package au.com.phiware.ga;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ChromosomeInputStream extends FilterInputStream {
	static final int[] H = {
		0x1, 0xF, 0x7, 0xB, 0x3, 0xD, 0x5, 0x9
	/*	0b0001,
		0b1111,
		0b0111,
		0b1011,
		0b0011,
		0b1101,
		0b0101,
		0b1001  */
	};
	static final int[] revH = {
	//	0b0001, 0b1111, 0b0111, 0b1011, 0b0011, 0b1101, 0b0101, 0b1001
		1 << 0, 1 << 4, 1 << 6, 1 << 2, 1 << 7, 1 << 3, 1 << 5, 1 << 1
	};
	private OutputStream syndrome;
	
	public ChromosomeInputStream(InputStream in, OutputStream err) {
		super(in);
		syndrome = err;
	}

	public int read()
	         throws IOException {
		int[] clear = {0, 0};
		byte[] s = {0, 0};
		byte[] coded = {0, 0};
		int x, p;
		
		if (in.read(coded) < 0)
			return -1;
		
		for (int i = 0; i < clear.length; i++) {
			for (x = 0, p = 1; x < H.length; x++, p <<= 1)
				if ((p & coded[i]) != 0)
					s[i] ^= H[x];
			if(s[i] != 0) {
				if ((s[i] & 1) == 1)
					coded[i] ^= revH[s[i] >>> 1];
				else
					continue;
			}
			clear[i] = ((coded[i] & 32) >>> 1 | coded[i] & 14) >>> 1;
		}
		
		syndrome.write(s);
		
		return clear[1] | (clear[0] << 4);
	}
	
	public int read(byte[] b,
            int off,
            int len)
     throws IOException {
		int read, i = off;
		while(i < len) {
			read = read();
			if (read < 0) break;
			b[i++] = (byte) read;
		}
		return i - off;
	}

	public OutputStream getSyndrome() {
		return syndrome;
	}
}
