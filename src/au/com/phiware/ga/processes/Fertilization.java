package au.com.phiware.ga.processes;

import static au.com.phiware.ga.containers.Ploids.getParents;
import static au.com.phiware.ga.containers.Ploids.setParents;

import java.io.FilterInputStream;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import au.com.phiware.ga.Genomes;
import au.com.phiware.ga.TransformException;
import au.com.phiware.ga.Transmission;
import au.com.phiware.ga.containers.Haploid;
import au.com.phiware.ga.containers.Ploid;
import au.com.phiware.ga.io.ChromosomeInputStream;
import au.com.phiware.util.concurrent.CloseableBlockingQueue;

public abstract class Fertilization<Parent extends Haploid<? extends Individual>, Individual extends Ploid<Parent>>
		extends SegregableProcess<Parent, Individual>
		implements Transmission<Parent, Individual> {
	Class<Individual> actualType;
	
	@SuppressWarnings("unchecked")
	public Individual transform() {
		try {
			if (this.actualType == null) {
				ParameterizedType superType = (ParameterizedType) this.getClass().getGenericSuperclass();
				while (!Fertilization.class.equals(superType.getRawType()))
					superType = (ParameterizedType) ((Class<?>) superType.getRawType()).getGenericSuperclass();
				Type actualType = superType.getActualTypeArguments()[1];
				if (actualType instanceof ParameterizedType)
					actualType = ((ParameterizedType) actualType).getRawType();
				this.actualType = (Class<Individual>) actualType;
			}
			return this.actualType.newInstance();
		} catch (Exception e) {}
		return null;
	}

	@Override
	public Callable<Individual> transformer(final CloseableBlockingQueue<? extends Parent> in)
			throws InterruptedException {
		final Individual newBorn = transform();
		final int size = newBorn.getNumberOfParents();
		final List<Parent> gametes = new ArrayList<Parent>(size);

		if (drain(in, gametes, size) < size)
			return nullTransformer;

		return new Callable<Individual>() {
			public Individual call() {
				final byte[][] heritage = new byte[size][];
				
				try {
					setParents(newBorn, gametes);
                    
                    for (int i = 0; i < size; i++)
                            heritage[i] = Genomes.getGenomeBytes(gametes.get(i));
                    
                    newBorn.readGenome(new Genomes.DataInputStream(
                    	new ChromosomeInputStream(
                    		new FilterInputStream(null) {
	                            private int cursor = 0;
	                            public int read() throws IOException {
	                                    byte b = heritage[cursor % size][cursor / size];
	                                    cursor++;
	                                    return b & 0xFF;
	                            }
	                            public int read(byte[] a, int off, int len)
	                                           throws IOException {
	                            	int i = 0;
	                            	byte b;
	                            	while (i < len && off + i < a.length) {
	                                    b = heritage[cursor % size][cursor / size];
	                                    cursor++;
	                                    a[off + i++] = (byte) (b & 0xFF);
	                            	}
	                            	return i;
	                            }
                    		},
                    		size
                    	)
                    ));

                    Genomes.logTransform(newBorn, gametes);
                    return newBorn;
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new TransformException(e);
				}
			}
		};
	}

	@Override
	public boolean shouldSegregate(Parent individual, CloseableBlockingQueue<Parent> in)
			throws InterruptedException {
		Individual parent, inlaw;

		parent = individual.getParent();
		if (parent != null) {
			List<Parent> parents = getParents(parent);
			for (Parent peer : in) {
				if ((inlaw = peer.getParent()) != null) {
					if (parent == inlaw || !Collections.disjoint(parents, getParents(inlaw)))
						return false;
				}
			}
		}

		return true;
	}
	
	@Override
	public final Individual transform(Parent individual) {
		return null;
	}
	
	public String getShortName() {
		return "Fert"+super.getShortName();
	}
}
