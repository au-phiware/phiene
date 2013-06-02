package au.com.phiware.ga;

import static org.junit.Assert.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;

public class EnvironmentTest {
	abstract class TestContainer implements Container {
		String string = super.toString();
		TestContainer() {}
		TestContainer(Container x) {string = x.toString()+"->"+name(this.getClass());}
		public String toString() {return string;}
		@Override public void writeGenome(DataOutput out) throws IOException {}
		@Override public void readGenome(DataInput in) throws IOException {}
	}
	class A extends TestContainer {A(){} A(Container x) {super(x);}}
	class B extends TestContainer {B(){} B(Container x) {super(x);}}
	class AC extends A {AC(){} AC(Container x) {super(x);}}
	class AD extends A {AD(){} AD(Container x) {super(x);}}
	class BC extends B {BC(){} BC(Container x) {super(x);}}
	class BD extends B {BD(){} BD(Container x) {super(x);}}
	class ACE extends AC {ACE(){} ACE(Container x) {super(x);}}
	class ACF extends AC {ACF(){} ACF(Container x) {super(x);}}
	class ADE extends AD {ADE(){} ADE(Container x) {super(x);}}
	class ADF extends AD {ADF(){} ADF(Container x) {super(x);}}
	class BCE extends BC {BCE(){} BCE(Container x) {super(x);}}
	class BCF extends BC {BCF(){} BCF(Container x) {super(x);}}
	class BDE extends BD {BDE(){} BDE(Container x) {super(x);}}
	class BDF extends BD {BDF(){} BDF(Container x) {super(x);}}
	
	class ACE2B extends AbstractProcess<ACE, B>{
		@Override public B transform(ACE ace) {return new B(ace);}
	}
	class A2B extends AbstractProcess<A, B>{
		@Override public B transform(A a) {return new B(a);}
	}
	class A2BC extends AbstractProcess<A, BC>{
		@Override public BC transform(A a) {return new BC(a);}
	}
	class A2ACE extends AbstractProcess<A, ACE>{
		@Override public ACE transform(A a) {return new ACE(a);}
	}
	class B2A extends AbstractProcess<B, A>{
		@Override public A transform(B b) {return new A(b);}
	}
	class BD2A extends AbstractProcess<BD, A>{
		@Override public A transform(BD bd) {return new A(bd);}
	}
	class BDF2A extends AbstractProcess<BDF, A>{
		@Override public A transform(BDF bdf) {return new A(bdf);}
	}
	class BC2ACE extends AbstractProcess<BC, ACE>{
		@Override public ACE transform(BC bc) {return new ACE(bc);}
	}
	class AD2ACE extends AbstractProcess<AD, ACE>{
		@Override public ACE transform(AD ad) {return new ACE(ad);}
	}
	class ADF2ACE extends AbstractProcess<ADF, ACE>{
		@Override public ACE transform(ADF adf) {return new ACE(adf);}
	}
	class B2ACE extends AbstractProcess<B, ACE>{
		@Override public ACE transform(B b) {return new ACE(b);}
	}
	class B2AD extends AbstractProcess<B, AD>{
		@Override public AD transform(B b) {return new AD(b);}
	}
	class B2ADE extends AbstractProcess<B, ADE>{
		@Override public ADE transform(B b) {return new ADE(b);}
	}
	class B2ADF extends AbstractProcess<B, ADF>{
		@Override public ADF transform(B b) {return new ADF(b);}
	}

	private TestContainer test;

	@Before public void setUp() {
		test = new TestContainer(){};
	}
	@After public void tearDown() {
		test = null;
	}
	
	@Test
	@SuppressWarnings("rawtypes")
	public void testIndividualUpperBound() {
		assertEquals(Container.class, individualUpperBound(new Environment<A>(new A2B())));
		assertEquals(Container.class, individualUpperBound(new Environment<AC>(new A2B())));
		assertEquals(Container.class, individualUpperBound(newEnvironment(A.class)));
		assertEquals(Container.class, individualUpperBound(new Environment()));
		assertEquals(A.class, individualUpperBound(new Environment<A>(new A2B()){}));
	}
	private <T extends Container> Environment<T> newEnvironment(Class<T> type) {
		return new Environment<T>(new AbstractProcess<T, T>(){
			@Override public T transform(T x) {return x;}
		});
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testCompatibleProcesses() throws Throwable {
		AC[] pop = new AC[1];
		AC ac = new AC(test);
		
		Environment<AC> environment = new Environment<AC>(new A2BC(), ac);
		environment.appendProcess(new AD2ACE(), new B2ADF());
		assertThat("Should have processes.", 3, is(environment.getProcesses().size()));

		environment.evolve();
		pop = environment.getPopulation().toArray(pop);
		assertThat("Population should be singular", 2, is(pop.length));
		assertEquals("Population should transform", ACE.class, (pop[0] == ac ? pop[1] : pop[0]).getClass());
	}

	@Test(expected=ClassCastException.class)
	@SuppressWarnings("unchecked")
	public void testIncompatibleProcesses() throws Throwable {
		Environment<AC> environment = new Environment<AC>(new A2BC());
		environment.appendProcess(new ADF2ACE(), new B2AD());
	}

	private Class<?> individualUpperBound(Environment<?> subtype) {
		@SuppressWarnings("rawtypes")
		Class<? extends Environment> subclass = subtype.getClass();
		Type actualType = subclass;
		Class<?> actualClass = subclass;
		Type individualType;
		Class<?> individualClass = null;
		
		if (Environment.class.equals(actualClass)) {
			TypeVariable<?>[] parameters = actualClass.getTypeParameters();
			individualType = parameters[0];
			//individualClass = Container.class;
		} else {
			while (!Environment.class.equals(actualClass)) {
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
			individualType = ((ParameterizedType) actualType).getActualTypeArguments()[0];
		}

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

	private static String name(Class<?> t) {
		String name = t.getName();
		int innerClassSeparatorIndex = name.indexOf("$");
		if (innerClassSeparatorIndex < 0)
			innerClassSeparatorIndex = name.lastIndexOf(".");
		name = name.substring(++innerClassSeparatorIndex);
		name.replaceAll("^[0-9]", "");
		return name;
	}
}
