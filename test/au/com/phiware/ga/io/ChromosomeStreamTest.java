package au.com.phiware.ga.io;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ChromosomeStreamTest {
	private static final String[] stringIn = {
		   "0",
		"1000",
		 "100",
		"1100",
		  "10",
		"1010",
		 "110",
		"1110",
		   "1",
		"1001",
		 "101",
		"1101",
		  "11",
		"1011",
		 "111",
		"1111"
	};
	private static final int[] byteIn = new int[stringIn.length];
	private static final String[] stringOut = {
		"0",        "0",
		"0", "11100001",
		"0", "10011001",
		"0",  "1111000",
		"0",  "1010101",
		"0", "10110100",
		"0", "11001100",
		"0",   "101101",
		"0", "11010010",
		"0",   "110011",
		"0",  "1001011",
		"0", "10101010",
		"0", "10000111",
		"0",  "1100110",
		"0",    "11110",
		"0", "11111111"
	};
	private static final byte[] byteOut = new byte[stringOut.length];
	private static final byte[] syndrome = new byte[] {
		0x1, 0xF, 0x7, 0xB, 0x3, 0xD, 0x5, 0x9,
		0xE, 0x6, 0xA, 0x2, 0xC, 0x4, 0x8,
		0x8, 0x4, 0xC, 0x2, 0xA, 0x6,
		0xC, 0x4, 0xA, 0x2, 0xE,
		0x8, 0x6, 0xE, 0x2,
		0xE, 0x6, 0xA,
		0x8, 0x4,
		0xC
	};
	private static final String[] stringErr = {
		       "1",
		      "10",
		     "100",
		    "1000",
		   "10000",
		  "100000",
		 "1000000",
		"10000000",
		      "11",
		     "101",
		    "1001",
		   "10001",
		  "100001",
		 "1000001",
		"10000001",
		     "110",
		    "1010",
		   "10010",
		  "100010",
		 "1000010",
		"10000010",
		    "1100",
		   "10100",
		  "100100",
		 "1000100",
		"10000100",
		   "11000",
		  "101000",
		 "1001000",
		"10001000",
		  "110000",
		 "1010000",
		"10010000",
		 "1100000",
		"10100000",
		"11000000"
	};
	private static final byte[] byteErr = new byte[stringErr.length];

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		for (int i = 0; i < byteIn.length; i++)
			byteIn[i] = Integer.parseInt(stringIn[i], 2);
		for (int i = 0; i < byteOut.length; i++)
			byteOut[i] = (byte) Integer.parseInt(stringOut[i], 2);
		for (int i = 0; i < byteErr.length; i++)
			byteErr[i] = (byte) Integer.parseInt(stringErr[i], 2);
	}

	private ByteArrayOutputStream bytes;
	private ChromosomeOutputStream out;
	private ByteArrayOutputStream errors;
	
	@Before public void setUp() {
		bytes = new ByteArrayOutputStream();
		out = new ChromosomeOutputStream(bytes);
		errors = new ByteArrayOutputStream();
	}
	@After public void tearDown() {
		bytes = null;
		out = null;
		errors = null;
	}

	@Test
	public void testOutput() throws IOException {
		byte[] byteArray;
		String[] strings = new String[byteOut.length];
		
		for (int b : byteIn )
			out.write(b);
		
		byteArray = bytes.toByteArray();
		for (int i = 0; i < byteArray.length; i++)
			strings[i] = Integer.toBinaryString(byteArray[i] & 0xFF);

		assertArrayEquals(stringOut, strings);
	}

	@Test
	public void testInput() throws IOException {
		ChromosomeInputStream in = new ChromosomeInputStream(new ByteArrayInputStream(byteOut), errors);
		byte[] byteArray = new byte[byteIn.length];
		String[] strings = new String[byteIn.length];
		
		in.read(byteArray);
		
		for (int i = 0; i < byteArray.length; i++)
			strings[i] = Integer.toBinaryString(byteArray[i] & 0xFF);

		assertArrayEquals(stringIn, strings);
	}

	@Test
	public void testError() throws IOException {
		byte[] byteArray = new byte[byteOut.length];
		String[] strings = new String[byteIn.length];
		int i;

		for(int j = 0; j < byteErr.length; j++) {
			for(i = 0; i < byteArray.length; i++)
				byteArray[i] = (byte) (byteOut[i] ^ byteErr[j]);
			
			ChromosomeInputStream in = new ChromosomeInputStream(new ByteArrayInputStream(byteArray), errors);
			for (i = 0; i < strings.length; i++) {
				int x = in.read();
				if (Integer.bitCount(byteErr[j] & 0xFF) == 1)
					assertTrue("Should correct "+Integer.toBinaryString(byteArray[i])+" to "+stringIn[i]+" not "+Integer.toBinaryString(x), x == byteIn[i]);
				else
					assertTrue("Should zero out double bit error, "+Integer.toBinaryString(byteErr[j]), x == 0);
			}
	
			byteArray = errors.toByteArray();
			for (i = 0; i < byteArray.length; i++)
				assertTrue("Expected " + Integer.toBinaryString(syndrome[j]) + " but got " + Integer.toBinaryString(byteArray[i]) + " for error, " + Integer.toBinaryString(byteErr[j]) + " at "+stringOut[i], syndrome[j] == byteArray[i]);
			errors.reset();
		}
	}
}
