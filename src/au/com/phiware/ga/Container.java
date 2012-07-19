package au.com.phiware.ga;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author Corin Lawson <me@corinlawson.com.au>
 *
 */
public interface Container {
	void writeGenome(DataOutput out)
            throws IOException;
	
	void readGenome(DataInput in)
            throws IOException;
}