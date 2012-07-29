package au.com.phiware.ga;

import java.io.IOException;

public interface ByteContainer extends Container {

	public abstract byte[] getGenome() throws IOException;

	public abstract void setGenome(byte[] genome);

}