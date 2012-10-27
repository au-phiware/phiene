package au.com.phiware.ga;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.phiware.util.concurrent.CloseableBlockingQueue;
import au.com.phiware.util.concurrent.QueueClosedException;

public abstract class AbstractProcess<Ante extends Container, Post extends Container> implements Process<Ante, Post> {
	public final static Logger logger = LoggerFactory.getLogger("au.com.phiware.ga.Process");
	private static int threadPoolSize = 3;
	protected ExecutorService sharedExecutor;
	private Queue<ExecutorService> executorPool = new ConcurrentLinkedQueue<ExecutorService>();
	
	public abstract Post transform(Ante individual);
	
	public Callable<Post> transformer(final CloseableBlockingQueue<? extends Ante> in)
			throws InterruptedException, TimeoutException {
		final Ante individual = in.take();
		return new Callable<Post>() {
			@SuppressWarnings("unchecked")
			public Post call() {
				try {
					LoggerFactory.getLogger("au.com.phiware.ga.Process."+getShortName()).debug("transforming {}...", individual);
					Post rv = transform(individual);
					Genomes.logTransform(rv, individual);
					return rv;
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new TransformException(e);
				}
			}
		};
	}
	
	protected List<Future<Post>> submitTransformers(final CloseableBlockingQueue<? extends Ante> in, final ExecutorService executor) throws InterruptedException {
		List<Future<Post>> results = new LinkedList<Future<Post>>();
		try {
			while (!in.isEmpty() || !in.isClosed()) {
				Callable<Post> transformer = transformer(in);
				
				if (transformer == null)
					throw new UnsupportedOperationException("Transformer expected");
				
				if (transformer != nullTransformer)
					results.add(executor.submit(transformer));
			}
		}
		catch (QueueClosedException expected) {}
		catch (TimeoutException ok) {}
		return results;
	}
	
	@Override
	public void transformPopulation(
			final CloseableBlockingQueue<? extends Ante> in,
			final CloseableBlockingQueue<? super Post> out)
					throws TransformException {
		ExecutorService executor = takeSharedExecutor();
		
		try {
			drainFutures(submitTransformers(in, executor), out);
		} catch (InterruptedException earlyExit) {
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
		} finally {
			giveExecutor(executor);
		}
	}
	
	public static int getThreadPoolSize() {
		return threadPoolSize;
	}

	public static void setThreadPoolSize(int threadPoolSize) {
		AbstractProcess.threadPoolSize = threadPoolSize;
	}
	
	public ExecutorService newExecutor() {
		return newExecutor(getThreadPoolSize());
	}
	
	public ExecutorService newExecutor(int poolSize) {
		if (poolSize > 0)
			return Executors.newFixedThreadPool(poolSize, new DefaultThreadFactory());
		else
			return Executors.newCachedThreadPool(new DefaultThreadFactory());
	}
	public ExecutorService takeSharedExecutor() {
		if (sharedExecutor == null)
			sharedExecutor = takeExecutor();
		return sharedExecutor;
	}
	public ExecutorService takeExecutor() {
		ExecutorService executor = executorPool.poll();
		if (executor == null)
			executor = newExecutor();
		return executor;
	}
	public void giveExecutor(ExecutorService executor) {
		if (executor != null) {
			if (executor.isShutdown()) {
				if (sharedExecutor == executor)
					sharedExecutor = null;
			} else if (!executorPool.offer(executor))
				executor.shutdown(); // Can't take it
		}
	}
	public void shutdown() {
		sharedExecutor.shutdown();
		sharedExecutor = null;
		Iterator<ExecutorService> i = executorPool.iterator();
		while (i.hasNext()) {
			((ExecutorService) i.next()).shutdown();
			i.remove();
		}
	}
	private final AtomicInteger poolNumber = new AtomicInteger(1);
    class DefaultThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        DefaultThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null)? s.getThreadGroup() :
                                 Thread.currentThread().getThreadGroup();
            namePrefix = getShortName() + "-" + poolNumber.getAndIncrement() + "-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                                  namePrefix + threadNumber.getAndIncrement(),
                                  0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
    private static final AtomicInteger procNumber = new AtomicInteger(1);
    private final int myProcNumber = procNumber.getAndIncrement();
	public String getShortName() {
		return "Proc" + myProcNumber;
	}

	public final Callable<Post> nullTransformer = new Callable<Post>() {
		public Post call() {
			return null;
		}
	};

	private static final Comparator<? super Future<?>> FUTURE_COMPARATOR = new Comparator<Future<?>>() {
		public int compare(Future<?> a, Future<?> b) {
			boolean done = a.isDone();
			return done == b.isDone() ? 0 : (done ? -1 : 1);
		}
	};
	protected void drainFutures(List<Future<Post>> results, final CloseableBlockingQueue<? super Post> out)
			throws InterruptedException {
		while (!results.isEmpty()) {
			Collections.sort(results, FUTURE_COMPARATOR);
			try {
				Iterator<Future<Post>> i = results.iterator();
				while (i.hasNext()) {
					try {
						Post t = i.next().get(5, TimeUnit.MILLISECONDS);
						i.remove();
						if (out != null && t != null) {
							//logger.debug("Transformed to {}", t);
							out.put(t);
						}
					} catch (ExecutionException e) {
						//i.remove();
						if (e.getCause() instanceof RuntimeException)
							throw (RuntimeException) e.getCause();
						else
							throw new RuntimeException(e);
					}
				}
			} catch (TimeoutException sortAgain) {}
		}
	}

	protected static void drainFutures(Collection<Future<?>> results)
			throws InterruptedException {
		drainFutures(new ArrayList<Future<?>>(results));
	}

	protected static void drainFutures(List<Future<?>> results)
			throws InterruptedException {
		while (!results.isEmpty()) {
			Collections.sort(results, FUTURE_COMPARATOR);
			try {
				Iterator<Future<?>> i = results.iterator();
				while (i.hasNext()) {
					try {
						i.next().get(5, TimeUnit.MILLISECONDS);
						i.remove();
					} catch (ExecutionException e) {
						//i.remove();
						if (e.getCause() instanceof RuntimeException)
							throw (RuntimeException) e.getCause();
						else
							throw new RuntimeException(e);
					}
				}
			} catch (TimeoutException sortAgain) {}
		}
	}

	public static Class<?> actualPostType(Process<?, ?> process) {
		return actualProcessType(1, process);
	}
	public static Class<?> actualAnteType(Process<?, ?> process) {
		return actualProcessType(0, process);
	}
	private static Type parameterVariable(Type type, int i) {
		Type var = null;

		if (type instanceof ParameterizedType) {
			ParameterizedType paramType = (ParameterizedType) type;
			Type rawType = paramType.getRawType();
			assert rawType instanceof Class<?>;
			
			if (Process.class.equals(rawType))
				return paramType.getActualTypeArguments()[i];
			
			if (!Process.class.isAssignableFrom((Class<?>)rawType))
				return null;

			@SuppressWarnings("unchecked")
			Class<? extends Process<?, ?>> rawClass = (Class<? extends Process<?, ?>>) rawType;

			TypeVariable<?>[] params = rawClass.getTypeParameters();
			Type[] actualParams = paramType.getActualTypeArguments();

			Type superType = rawClass.getGenericSuperclass();
			var = parameterVariable(superType, i);

			if (var == null)
				for (Type ifType : rawClass.getGenericInterfaces()) {
					var = parameterVariable(ifType, i);
					if (var != null)
						break;
				}
			if (var != null && var instanceof TypeVariable) {
				String name = ((TypeVariable<?>) var).getName();
				for (int j = 0; j < params.length; j++)
					if (name.equals(params[j].getName()))
						return actualParams[j];
			}
		} else if (type instanceof Class) {
			Class<?> rawClass = (Class<?>) type;

			if (!Process.class.isAssignableFrom((Class<?>)rawClass))
				return null;

			Type superType = rawClass.getGenericSuperclass();
			var = parameterVariable(superType, i);

			if (var == null)
				for (Type ifType : rawClass.getGenericInterfaces()) {
					var = parameterVariable(ifType, i);
					if (var != null)
						break;
				}
		}
		return var;
	}
	private static Class<?> actualProcessType(int i, Process<?, ?> process) {
		Type individualType;
		Class<?> individualClass = null;

		individualType = parameterVariable(process.getClass(), i);

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
