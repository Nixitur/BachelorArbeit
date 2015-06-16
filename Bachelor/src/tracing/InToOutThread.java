package tracing;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Instances of this class are threads that continually write data from an input stream to an output stream.
 * @author Kaspar
 *
 */
public class InToOutThread extends Thread {

	private int BUFFER_SIZE = 2048;
	private InputStreamReader inReader;
	private OutputStreamWriter outWriter;
	
	/**
	 * Creates a new InToOutThread.
	 * @param in The input stream that is to be read.
	 * @param out The output stream that is to be written to.
	 */
	public InToOutThread(InputStream in, OutputStream out) {
		inReader = new InputStreamReader(in);
		outWriter = new OutputStreamWriter(out);
	}
	
	@Override
	/**
	 * Runs this thread, continually writing from the input stream to the output stream.
	 */
	public void run(){
		try {
			char[] buf = new char[BUFFER_SIZE];
			int d;
			while ((d = inReader.read(buf,0,BUFFER_SIZE)) >= 0){
				outWriter.write(buf, 0, d);
			}
			outWriter.flush();
		} catch (IOException e) {
			System.err.println("I/O Error - " + e);
		}
	}
}
