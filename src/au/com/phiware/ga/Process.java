package au.com.phiware.ga;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import au.com.phiware.util.concurrent.CloseableBlockingQueue;
import au.com.phiware.util.concurrent.QueueClosedException;

public abstract class Process<Ante extends Container, Post extends Container> {
	
	public abstract Post transform(Ante individual);

	public Callable<Post> transformer(final Ante individual) {
		try {
			final Process<Ante, Post> me = this;
			return new Callable<Post>() {
				public Post call() {
					try {
						return me.transform(individual);
					} catch (RuntimeException e) {
						throw e;
					} catch (Exception e) {
						throw new EvolutionTransformException(e);
					}
				}
			};
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new EvolutionTransformException(e);
		}
	}

	public void transformPopulation(
			final CloseableBlockingQueue<? extends Ante> in,
			final CloseableBlockingQueue<? super Post> out)
					throws EvolutionTransformException {
		ExecutorService executor = Executors.newCachedThreadPool();
		List<Future<Post>> results = new LinkedList<Future<Post>>();
		
		try {
			try {
				for (;;) {
					final Ante individual = in.take();
					Callable<Post> transformer = transformer(individual);
					
					if (transformer == null)
						throw new UnsupportedOperationException("No transformer");
					
					results.add(executor.submit(transformer));
				}
			} catch (QueueClosedException good) {}

			while (!results.isEmpty()) {
				Collections.sort(results, new Comparator<Future<Post>>() {
					public int compare(Future<Post> a, Future<Post> b) {
						boolean done = a.isDone();
						return done == b.isDone() ? 0 : (done ? -1 : 1);
					}
				});
				try {
					Iterator<Future<Post>> i = results.iterator();
					while (i.hasNext()) {
						try {
							Post individual = i.next().get(10, TimeUnit.MILLISECONDS);
							i.remove();
							if (individual != null)
								out.put(individual);
						} catch (ExecutionException e) {
							//i.remove();
							if (e.getCause() instanceof RuntimeException)
								throw (RuntimeException) e.getCause();
							else
								throw new RuntimeException(e);
						}
					}
				} catch (TimeoutException resort) {}
			}
		}
		catch (InterruptedException earlyExit) {
			@SuppressWarnings("unused")
			int dropCount = 0;
			
			executor.shutdown();
			try {
				// Use generous time out, since a second interrupt may kill it
				executor.awaitTermination(1, TimeUnit.MINUTES);
			} catch (InterruptedException impatient) {}
			
			if (!executor.isTerminated())
				dropCount = executor.shutdownNow().size();
			
			//dropCount += done count in results(?)
		}
		finally {
			out.close();
		}
	}
	
	static Class<?> actualPostType(
			Process<? extends Container, ? extends Container> process) {
		return actualProcessType(1, process);
	}
	static Class<?> actualAnteType(
			Process<? extends Container, ? extends Container> process) {
		return actualProcessType(0, process);
	}
	static Class<?> actualProcessType(int i,
			Process<? extends Container, ? extends Container> process) {
		@SuppressWarnings("rawtypes")
		Class<? extends Process> subclass = process.getClass();
		Type actualType = subclass;
		Class<?> actualClass = subclass;
		Type individualType;
		Class<?> individualClass = null;
		
		while (!Process.class.equals(actualClass)) {
			actualType = actualClass.getGenericSuperclass();
			assertTrue("Superclass should be a Class or ParameterizedType",
					actualType instanceof Class<?> || actualType instanceof ParameterizedType);
			
			if (actualType instanceof ParameterizedType) {
				Type rawType = ((ParameterizedType) actualType).getRawType();
				assertTrue("What? It doesn't return a Class??", rawType instanceof Class<?>);
				actualClass = (Class<?>) rawType;
			} else
				actualClass = (Class<?>) actualType;
		}
		individualType = ((ParameterizedType) actualType).getActualTypeArguments()[i];

		while (individualClass == null) {
			if (individualType instanceof GenericArrayType) {
				individualType = ((GenericArrayType) individualType).getGenericComponentType();
			} else if (individualType instanceof TypeVariable<?>) {
				Type[] bounds = ((TypeVariable<?>) individualType).getBounds();
				if (bounds.length > 0)
					individualType = bounds[0];
				else
					individualClass = Container.class;
			} else if (individualType instanceof ParameterizedType) {
				individualClass = (Class<?>) ((ParameterizedType) individualType).getRawType();
			} else if (individualType instanceof WildcardType) {
				individualClass = Container.class;
			} else
				individualClass = (Class<?>) individualType;
		}

		return individualClass;
	}
}
