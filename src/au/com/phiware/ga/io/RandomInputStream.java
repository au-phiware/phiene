package au.com.phiware.ga.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.util.Random;

public class RandomInputStream extends FilterInputStream {
	private Random random;
	
	public RandomInputStream() {
		this(new Random());
	}
	public RandomInputStream(Random random) {
		super(null);
		this.random = random;
	}

	public int read() throws IOException {
		return random.nextInt(0x100);
	}
	
	private transient byte[] bytes;
	private transient int cursor = 0;
	public int read(byte[] b, int offset, int length) throws IOException {
		if (bytes == null) {
			bytes = new byte[0x100];
			random.nextBytes(bytes);
			cursor = 0;
		}
			
		for (int i = 0; i < length;) {
			for (; cursor < bytes.length && i < length; i++)
				b[offset + i] = bytes[cursor++];
			if (cursor >= bytes.length) {
				random.nextBytes(bytes);
				cursor = 0;
			}
		}
		return length;
	}
}
