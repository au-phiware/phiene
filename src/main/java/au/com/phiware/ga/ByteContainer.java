package au.com.phiware.ga;

import java.io.IOException;

/**
 *
 * @author Corin Lawson <corin@phiware.com.au>
 */
public interface ByteContainer extends Container {

	public abstract byte[] getGenome() throws IOException;

	public abstract void setGenome(byte[] genome);

}
