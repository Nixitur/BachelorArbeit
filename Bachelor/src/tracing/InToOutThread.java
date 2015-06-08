package tracing;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class InToOutThread extends Thread {

	private int BUFFER_SIZE = 2048;
	private InputStreamReader inReader;
	private OutputStreamWriter outWriter;
	public InToOutThread(InputStream in, OutputStream out) {
		inReader = new InputStreamReader(in);
		outWriter = new OutputStreamWriter(out);
	}
	
	@Override
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
