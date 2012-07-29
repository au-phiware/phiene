package au.com.phiware.ga;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import au.com.phiware.math.bankers.Bankers;

public final class Genomes {
	private static Map<Container, byte[]> genomes = Collections.synchronizedMap(new WeakHashMap<Container, byte[]>());
	
	static Bankers<Integer> byteBankers;
	static Bankers<Integer> shortBankers;
	static Bankers<Integer> integerBankers;
	static Bankers<Long>    longBankers;

	static {
		try {
			byteBankers    = new Bankers<Integer>(8){};
			shortBankers   = new Bankers<Integer>(16){};
			integerBankers = new Bankers<Integer>(32){};
			longBankers    = new Bankers<Long>(64){};
		} catch (ClassNotFoundException ignored) {}
	}
	
	public static class DataOutputStream extends FilterOutputStream implements DataOutput {
		private final java.io.DataOutputStream dataOut;

		public DataOutputStream(OutputStream out) {
			super(new java.io.DataOutputStream(out));
			dataOut = (java.io.DataOutputStream) this.out;
		}

		@Override
		public void write(int b)
		           throws IOException {
			dataOut.write(byteBankers.to(b));
		}

		@Override
		public void writeBoolean(boolean v) throws IOException {
			dataOut.writeBoolean(v);
		}

		@Override
		public void writeByte(int v) throws IOException {
			dataOut.writeByte(byteBankers.to(v));
		}

		@Override
		public void writeBytes(String s) throws IOException {
			int len = s.length();
			for (int i = 0 ; i < len ; i++)
				writeByte((byte)s.charAt(i));
		}

		@Override
		public void writeChar(int v) throws IOException {
			dataOut.writeChar(shortBankers.to(v));
		}

		@Override
		public void writeChars(String s) throws IOException {
			int len = s.length();
			for (int i = 0 ; i < len ; i++)
				writeChar(s.charAt(i));
		}

		@Override
		public void writeDouble(double v) throws IOException {
			writeLong(Double.doubleToLongBits(v));
		}

		@Override
		public void writeFloat(float v) throws IOException {
			writeInt(Float.floatToIntBits(v));
		}

		@Override
		public void writeInt(int v) throws IOException {
			dataOut.writeInt(integerBankers.to(v));
		}

		@Override
		public void writeLong(long v) throws IOException {
			dataOut.writeLong(longBankers.to(v));
		}

		@Override
		public void writeShort(int v) throws IOException {
			dataOut.writeShort(shortBankers.to(v));
		}

		@Override
		public void writeUTF(String v) throws IOException {
			writeUTF(v, this);
		}

	    static int writeUTF(String str, DataOutput out) throws IOException {
	        int strlen = str.length();
	        int utflen = 0;
	        int c, count = 0;

	        /* use charAt instead of copying String to char array */
	        for (int i = 0; i < strlen; i++) {
	            c = str.charAt(i);
	            if ((c >= 0x0001) && (c <= 0x007F)) {
	                utflen++;
	            } else if (c > 0x07FF) {
	                utflen += 3;
	            } else {
	                utflen += 2;
	            }
	        }

	        if (utflen > 65535)
	            throw new UTFDataFormatException(
	                "encoded string too long: " + utflen + " bytes");

	        byte[] bytearr = new byte[utflen+2];

	        bytearr[count++] = byteBankers.to((utflen >>> 8) & 0xFF).byteValue();
	        bytearr[count++] = byteBankers.to((utflen >>> 0) & 0xFF).byteValue();

	        int i=0;
	        for (i=0; i<strlen; i++) {
	           c = str.charAt(i);
	           if (!((c >= 0x0001) && (c <= 0x007F))) break;
               bytearr[count++] = byteBankers.to(c).byteValue();
	        }

	        for (;i < strlen; i++){
	            c = str.charAt(i);
	            if ((c >= 0x0001) && (c <= 0x007F)) {
	                bytearr[count++] = byteBankers.to(c).byteValue();

	            } else if (c > 0x07FF) {
	                bytearr[count++] = byteBankers.to(0xE0 | ((c >> 12) & 0x0F)).byteValue();
	                bytearr[count++] = byteBankers.to(0x80 | ((c >>  6) & 0x3F)).byteValue();
	                bytearr[count++] = byteBankers.to(0x80 | ((c >>  0) & 0x3F)).byteValue();
	            } else {
	                bytearr[count++] = byteBankers.to(0xC0 | ((c >>  6) & 0x1F)).byteValue();
	                bytearr[count++] = byteBankers.to(0x80 | ((c >>  0) & 0x3F)).byteValue();
	            }
	        }
	        out.write(bytearr, 0, utflen+2);
	        return utflen + 2;
	    }
	}
	
	public static class DataInputStream extends FilterInputStream implements DataInput {
		private final java.io.DataInputStream dataIn;

		public DataInputStream(InputStream in) {
			super(new java.io.DataInputStream(in));
			dataIn = (java.io.DataInputStream) this.in;
		}

		@Override
		public boolean readBoolean() throws IOException {
	        int b = in.read();
	        if (b < 0)
	            throw new EOFException();
	        return (b & 1) == 1;
		}

		@Override
		public byte readByte() throws IOException {
			return byteBankers.from((int) dataIn.readByte() & 0xFF).byteValue();
		}

		@Override
		public char readChar() throws IOException {
			return (char) shortBankers.from((int) dataIn.readChar() & 0xFFFF).intValue();
		}

		@Override
		public double readDouble() throws IOException {
			return Double.longBitsToDouble(readLong());
		}

		@Override
		public float readFloat() throws IOException {
			return Float.intBitsToFloat(readInt());
		}

		@Override
		public void readFully(byte[] b) throws IOException {
	        readFully(b, 0, b.length);
		}

		@Override
		public void readFully(byte b[], int off, int len) throws IOException {
	        if (len < 0)
	            throw new IndexOutOfBoundsException();
	        int n = 0;
	        while (n < len) {
	            int count = in.read(b, off + n, len - n);
	            if (count < 0)
	                throw new EOFException();
	            for (int i = 0; i < len - n; i++)
	            	b[off + n + i] = byteBankers.from((int) b[off + n + i] & 0xFF).byteValue();
	            n += count;
	        }
	    }

		@Override
		public int readInt() throws IOException {
			return integerBankers.from(dataIn.readInt());
		}

		@Override
		public String readLine() throws IOException {
			return null;
		}

		@Override
		public long readLong() throws IOException {
			return longBankers.from(dataIn.readLong());
		}

		@Override
		public short readShort() throws IOException {
			return (short) shortBankers.from((int) dataIn.readShort() & 0xFFFF).intValue();
		}

		@Override
		public int readUnsignedByte() throws IOException {
			return byteBankers.from(dataIn.readUnsignedByte()).intValue();
		}

		@Override
		public int readUnsignedShort() throws IOException {
			return shortBankers.from(dataIn.readUnsignedShort()).intValue();
		}

		@Override
		public int skipBytes(int n) throws IOException {
			return dataIn.skipBytes(n);
		}

		@Override
		public String readUTF() throws IOException {
	        return readUTF(this);
		}

	    public final static String readUTF(DataInput in) throws IOException {
	        int utflen = in.readUnsignedShort();
	        byte[] bytearr = null;
	        char[] chararr = null;
            bytearr = new byte[utflen];
            chararr = new char[utflen];

	        int c, char2, char3;
	        int count = 0;
	        int chararr_count=0;

	        in.readFully(bytearr, 0, utflen);

	        while (count < utflen) {
	            c = (int) bytearr[count] & 0xff;
	            if (c > 127) break;
	            count++;
	            chararr[chararr_count++]=(char)c;
	        }

	        while (count < utflen) {
	            c = (int) bytearr[count] & 0xff;
	            switch (c >> 4) {
	                case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
	                    /* 0xxxxxxx*/
	                    count++;
	                    chararr[chararr_count++]=(char)c;
	                    break;
	                case 12: case 13:
	                    /* 110x xxxx   10xx xxxx*/
	                    count += 2;
	                    if (count > utflen)
	                        throw new UTFDataFormatException(
	                            "malformed input: partial character at end");
	                    char2 = (int) bytearr[count-1];
	                    if ((char2 & 0xC0) != 0x80)
	                        throw new UTFDataFormatException(
	                            "malformed input around byte " + count);
	                    chararr[chararr_count++]=(char)(((c & 0x1F) << 6) |
	                                                    (char2 & 0x3F));
	                    break;
	                case 14:
	                    /* 1110 xxxx  10xx xxxx  10xx xxxx */
	                    count += 3;
	                    if (count > utflen)
	                        throw new UTFDataFormatException(
	                            "malformed input: partial character at end");
	                    char2 = (int) bytearr[count-2];
	                    char3 = (int) bytearr[count-1];
	                    if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
	                        throw new UTFDataFormatException(
	                            "malformed input around byte " + (count-1));
	                    chararr[chararr_count++]=(char)(((c     & 0x0F) << 12) |
	                                                    ((char2 & 0x3F) << 6)  |
	                                                    ((char3 & 0x3F) << 0));
	                    break;
	                default:
	                    /* 10xx xxxx,  1111 xxxx */
	                    throw new UTFDataFormatException(
	                        "malformed input around byte " + count);
	            }
	        }
	        // The number of chars produced may be less than utflen
	        return new String(chararr, 0, chararr_count);
	    }
	}
	
	public static void setGenomeBytes(Container individual, byte[] bytes) throws IOException {
		if (individual instanceof ByteContainer)
			((ByteContainer) individual).setGenome(bytes);
		else {
			if (bytes != null)
				individual.readGenome(new DataInputStream(new ByteArrayInputStream(bytes)));
			genomes.put(individual, bytes.clone());
		}
	}

	public static byte[] getGenomeBytes(Container individual) throws IOException {
		if (individual instanceof ByteContainer)
			return ((ByteContainer) individual).getGenome();
		
		byte[] rv = genomes.get(individual);
		if (rv != null)
			return rv.clone(); //TODO: Should this return a copy or not?
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		individual.writeGenome(out);
		out.close();
		genomes.put(individual, rv = bytes.toByteArray());
		return rv;
	}

	public static <Filter extends FilterOutputStream> byte[] getGenomeBytes(Container individual, Class<Filter> filter) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		OutputStream filtered = bytes;
		try {
			filtered = filter.getConstructor(OutputStream.class).newInstance(bytes);
		} catch (Exception e) {
			e.printStackTrace();
		}
		DataOutputStream out = new DataOutputStream(filtered);
		individual.writeGenome(out);
		out.close();
		byte[] rv = null;
		rv = bytes.toByteArray();
		genomes.put(individual, rv);
		return rv;
	}

	/*
	 * Returns null is specified filter does not define a public no-arg constructor or public constructor that accepts an OutputStream.
	 */
	public static OutputStream[] getGenomeFilters(Container individual, Class<? extends FilterOutputStream>... filters) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		FilterOutputStream filtered = null;
		List<OutputStream> filterList = new ArrayList<OutputStream>(filters.length + 2);
		
		try {
			filterList.add(bytes);
			for (Class<? extends FilterOutputStream> filter : filters) {
				try {
					filtered = filter.getConstructor(OutputStream.class).newInstance(filtered == null ? bytes : filtered);
				} catch (Exception e) {
					bytes = null;
					filterList.clear();
					filtered = filter.getConstructor().newInstance();
				}
				filterList.add(filtered);
			}
		} catch (Exception x) {
			return null;
		}
		DataOutputStream out = new DataOutputStream(filtered);
		filterList.add(out);
		individual.writeGenome(out);
		out.close();
		byte[] rv = null;
		if (bytes != null)
			rv = bytes.toByteArray();
		genomes.put(individual, rv);
		OutputStream[] a = new OutputStream[filterList.size()];
		return filterList.toArray(a);
	}
}
