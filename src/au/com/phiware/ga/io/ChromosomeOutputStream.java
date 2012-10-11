package au.com.phiware.ga.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ChromosomeOutputStream extends FilterOutputStream {
	static int[] G = {
		0xD2, 0x55, 0x99, 0xE1
	/*	0b11100001,
		0b10011001,
		0b01010101,
		0b11010010  */
	//	0x87, 0x99, 0xAA, 0x4B
	/*	0b10000111,
		0b10011001,
		0b10101010,
		0b01001011  */
	};
	private int size = 1;
	
	public ChromosomeOutputStream(OutputStream out, int size) {
		super(out);
		this.size = size;
	}

	public ChromosomeOutputStream(OutputStream out) {
		this(out, 1);
	}

	@Override
	public void write(int b) throws IOException {
		int[] clear = {(b & 0xF0) >> 4, b & 0x0F};
		int[] coded = {0, 0};

		for (int i = 0; i < clear.length; i++)
			for (int x = 0, p = 1; x < G.length; x++, p <<= 1)
				if ((p & clear[i]) != 0)
					coded[i] ^= G[x];

		for (int i = 0; i < size; i++)
			out.write(coded[0]);
		for (int i = 0; i < size; i++)
			out.write(coded[1]);
	}
}
