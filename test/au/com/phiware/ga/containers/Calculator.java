package au.com.phiware.ga.containers;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Deque;

import au.com.phiware.math.ring.ArithmeticFactory;
import au.com.phiware.math.ring.BitArithmetic;

public class Calculator<V extends Number> implements Ploid<Haploid<Calculator<V>>> {
	enum Operation {
		ZERO {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				stack.push(arithmetic.zero());
			}
		},
		ADD {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 1)
					stack.push(arithmetic.add(stack.pop(), stack.pop()));
			}
		},
		NEGATE {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 0)
					stack.push(arithmetic.negate(stack.pop()));
			}
		},
		SUBTRACT {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 1)
					stack.push(arithmetic.subtract(stack.pop(), stack.pop()));
			}
		},
		MULTIPLY {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 1)
					stack.push(arithmetic.multiply(stack.pop(), stack.pop()));
			}
		},
		POW {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 1)
					stack.push(arithmetic.pow(stack.pop(), stack.pop()));
			}
		},
		ONE {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				stack.push(arithmetic.one());
			}
		},
		MOD {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 1)
					stack.push(arithmetic.mod(stack.pop(), stack.pop()));
			}
		},
		OR {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 1)
					stack.push(arithmetic.or(stack.pop(), stack.pop()));
			}
		},
		AND {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 1)
					stack.push(arithmetic.and(stack.pop(), stack.pop()));
			}
		},
		NAND {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 1)
					stack.push(arithmetic.nand(stack.pop(), stack.pop()));
			}
		},
		XOR {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 1)
					stack.push(arithmetic.xor(stack.pop(), stack.pop()));
			}
		},
		NOT {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 0)
					stack.push(arithmetic.not(stack.pop()));
			}
		},
		MIN {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 1)
					stack.push(arithmetic.min(stack.pop(), stack.pop()));
			}
		},
		MAX {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 1)
					stack.push(arithmetic.max(stack.pop(), stack.pop()));
			}
		},
		CLEAR {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				stack.clear();
			}
		};
		
		public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {}
	}
	
	private BitArithmetic<V> arithmetic;

	public static <V extends Number> Calculator<V> newCalculator(Class<V> elementType) {
		try {
			Calculator<V> rv = new Calculator<V>();
			rv.arithmetic = ArithmeticFactory.getBitArithmetic(elementType);
			return rv;
		} catch (ClassNotFoundException e) {}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public Calculator() throws ClassNotFoundException {
		if (!Calculator.class.equals(this.getClass())) {
			ParameterizedType superType = (ParameterizedType) this.getClass().getGenericSuperclass();
			while (!Calculator.class.equals(superType.getRawType()))
				superType = (ParameterizedType) ((Class<?>) superType.getRawType()).getGenericSuperclass();
			Type[] actualType = superType.getActualTypeArguments();
			arithmetic = ArithmeticFactory.getBitArithmetic((Class<V>) actualType[0]);
		}
	}
	
	public static final int stepLimit = 1024;
	private Deque<V> stack = new ArrayDeque<V>();
	
	public int calculate(V target) throws IOException {
		int step = 0;
		
		while (!target.equals(stack.peek()) && step < stepLimit)
			nextInstruction().execute(arithmetic, stack);
		
		return step;
	}
	
	public V getResult() {
		return stack.peek();
	}
	
	private Operation[] instructions = new Operation[0x100];
	private int cursor = 0;

	private Operation nextInstruction() {
		if (cursor >= instructions.length)
			cursor = 0;
		return instructions[cursor++];
	}

	@Override
	public int getNumberOfParents() {
		return 2;
	}

	@Override
	public void writeGenome(DataOutput out) throws IOException {
		for (int i = 0; i < instructions.length;) {
			out.writeByte(
				  instructions[i++].ordinal() << 4
				| instructions[i++].ordinal()
			);
		}
	}

	@Override
	public void readGenome(DataInput in) throws IOException {
		Operation[] ops = Operation.values();
		for (int i = 0; i < instructions.length;) {
			byte b = in.readByte();
			instructions[i++] = ops[(b >>> 4) & 0xF];
			instructions[i++] = ops[b & 0xF];
		}
	}
}
