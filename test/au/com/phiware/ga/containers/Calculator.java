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
		CLEAR {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				stack.clear();
			}
			@Override public char code() { return '∅'; }
		},
		MIN {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 1) {
					V a = stack.pop(), b = stack.pop();
					stack.push(arithmetic.min(b, a));
			}
			}
			@Override public char code() { return '<'; }
		},
		MAX {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 1) {
					V a = stack.pop(), b = stack.pop();
					stack.push(arithmetic.max(b, a));
				}
			}
			@Override public char code() { return '>'; }
		},
		OR {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 1) {
					V a = stack.pop(), b = stack.pop();
					stack.push(arithmetic.or(b, a));
				}
			}
			@Override public char code() { return '⋁'; }
		},
		AND {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 1) {
					V a = stack.pop(), b = stack.pop();
					stack.push(arithmetic.and(b, a));
				}
			}
			@Override public char code() { return '⋀'; }
		},
		NAND {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 1) {
					V a = stack.pop(), b = stack.pop();
					stack.push(arithmetic.nand(b, a));
				}
			}
			@Override public char code() { return '⊼'; }
		},
		XOR {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 1) {
					V a = stack.pop(), b = stack.pop();
					stack.push(arithmetic.xor(b, a));
				}
			}
			@Override public char code() { return '⊻'; }
		},
		POW {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 1) {
					V a = stack.pop(), b = stack.pop();
					stack.push(arithmetic.pow(b, a));
				}
			}
			@Override public char code() { return 'ⁿ'; }
		},
		MOD {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 1 && !stack.peek().equals(arithmetic.zero())) {
					V a = stack.pop(), b = stack.pop();
					stack.push(arithmetic.mod(b, a));
				}
			}
			@Override public char code() { return '÷'; }
		},
		NOT {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 0)
					stack.push(arithmetic.not(stack.pop()));
			}
			@Override public char code() { return '¬'; }
		},
		NEGATE {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 0)
					stack.push(arithmetic.negate(stack.pop()));
			}
			@Override public char code() { return '⌐'; }
		},
		SUBTRACT {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 1) {
					V a = stack.pop(), b = stack.pop();
					stack.push(arithmetic.subtract(b, a));
				}
			}
			@Override public char code() { return '-'; }
		},
		MULTIPLY {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 1) {
					V a = stack.pop(), b = stack.pop();
					stack.push(arithmetic.multiply(b, a));
				}
			}
			@Override public char code() { return '×'; }
		},
		ADD {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				if (stack.size() > 1) {
					V a = stack.pop(), b = stack.pop();
					stack.push(arithmetic.add(b, a));
				}
			}
			@Override public char code() { return '+'; }
		},
		ONE {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				stack.push(arithmetic.one());
			}
			@Override public char code() { return '1'; }
		},
		ZERO {
			@Override public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {
				stack.push(arithmetic.zero());
			}
			@Override public char code() { return '0'; }
		};
		
		public <V extends Number> void execute(BitArithmetic<V> arithmetic, Deque<V> stack) {}
		
		public char code() {
			return (char) ('0' + this.ordinal());
		}
	}
	
	private BitArithmetic<V> arithmetic;

	public static <V extends Number> Calculator<V> newCalculator(Class<V> elementType) {
		try {
			return new Calculator<V>(ArithmeticFactory.getBitArithmetic(elementType));
		} catch (ClassNotFoundException e) {}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	protected Calculator() throws ClassNotFoundException {
		if (!Calculator.class.equals(this.getClass())) {
			ParameterizedType superType = (ParameterizedType) this.getClass().getGenericSuperclass();
			while (!Calculator.class.equals(superType.getRawType()))
				superType = (ParameterizedType) ((Class<?>) superType.getRawType()).getGenericSuperclass();
			Type[] actualType = superType.getActualTypeArguments();
			arithmetic = ArithmeticFactory.getBitArithmetic((Class<V>) actualType[0]);
		}
	}
	
	 public Calculator(BitArithmetic<V> bitArithmetic) {
		 arithmetic = bitArithmetic;
	 }
	
	public static final int stepLimit = 999;
	private Deque<V> stack = new ArrayDeque<V>();
	private transient int score;
	private transient V lastTarget;
	
	public int calculate(V target) throws IOException {
		int step = 0;
		
		while (!target.equals(stack.peek()) && stepLimit > step++)
			nextInstruction().execute(arithmetic, stack);
		
		if (target.equals(stack.peek())) {
			lastTarget = target;
			score = step;
		}
		
		return step;
	}
	
	public V getResult() {
		return stack.peek();
	}
	
	private Operation[] instructions = new Operation[0x200];
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

	private transient String instructionString;
	@Override
	public String toString() {
		if (instructionString == null) {
			char[] instructionCode = new char[instructions.length];
			int i = 0;
			for (Operation op : instructions)
				instructionCode[i++] = op.code();
			instructionString = new String(instructionCode);
		}
		return String.format("%s%s",instructionString, (lastTarget == null ? "" : ("["+lastTarget+"^"+score+"]")));
	}
}
