package au.com.phiware.ga.processes;

import java.io.FilterInputStream;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import au.com.phiware.ga.AbstractProcess;
import au.com.phiware.ga.Genomes;
import au.com.phiware.ga.TransformException;
import au.com.phiware.ga.Transmission;
import au.com.phiware.ga.containers.Haploid;
import au.com.phiware.ga.containers.Polyploid;
import au.com.phiware.ga.io.ChromosomeInputStream;
import au.com.phiware.util.concurrent.CloseableBlockingQueue;

public abstract class Fertilization<Parent extends Haploid<Individual>, Individual extends Polyploid<Parent>> extends AbstractProcess<Parent, Individual> implements Transmission<Parent, Individual> {
	public Individual transform() {
		try {
			ParameterizedType superType = (ParameterizedType) this.getClass().getGenericSuperclass();
			while (!Fertilization.class.equals(superType.getRawType()))
				superType = (ParameterizedType) ((Class<?>) superType.getRawType()).getGenericSuperclass();
			Type[] actualType = superType.getActualTypeArguments();
			@SuppressWarnings("unchecked")
			Class<Individual> type = (Class<Individual>) actualType[1];
			return type.newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public <Gamete extends Parent> Callable<Individual> transformer(final CloseableBlockingQueue<Gamete> in)
			throws InterruptedException {
		final Gamete p = in.take();

		return new Callable<Individual>() {
			public Individual call() {
				Individual newBorn = transform();
				try {
					final int size =  newBorn.getNumberOfParents();
					ArrayList<Gamete> gametes = new ArrayList<Gamete>(size);
					List<Parent> granparents = new ArrayList<Parent>();
					List<Individual> parents = new ArrayList<Individual>();
					Set<Gamete> family = new HashSet<Gamete>();;
					
					Individual parent, inlaw;
					Gamete mate = p;
					
					in.preventClose();
					try {
						while (gametes.size() < size) {
							gametes.add(mate);
							parent = mate.getParent();
							if (parent != null) {
								parents.add(parent);
								granparents.addAll(parent.getParents());
							}
							while ((inlaw = (mate = in.take()).getParent()) != null && (
										parents.contains(inlaw)
										|| !Collections.disjoint(granparents, inlaw.getParents())
									))
								family.add(mate);
						}
						for (Gamete m : family)
							in.put(m);
					} finally {
						in.permitClose();
					}
					
					newBorn.setParents(gametes);
	
					final byte[][] heritage = new byte[size][];
					
					for (int i = 0; i < size; i++)
						heritage[i] = Genomes.getGenomeBytes(gametes.get(i));
					
					newBorn.readGenome(new Genomes.DataInputStream(new ChromosomeInputStream(new FilterInputStream(null) {
						private int cursor = 0;
						public int read()
						         throws IOException {
							byte b = heritage[cursor % size][cursor / size];
							cursor++;
							return b & 0xFF;
						}
					}, size)));
				} catch (InterruptedException earlyExit) {
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new TransformException(e);
				}

				return newBorn;
			}
		};
	}

	@Override
	public final Individual transform(Parent individual) {
		return null;
	}
}
