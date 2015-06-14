package embedding;

import java.io.IOException;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.util.BCELifier;

/**
 * Merely a test class to see how BCEL works
 * @author Kaspar
 *
 */
public class BCELize {
	public static void main(String[] args){
		try {
			ClassParser parser = new ClassParser("example/Nodexample.class");
			JavaClass clazz = parser.parse();
			BCELifier bcel = new BCELifier(clazz,System.out);
			bcel.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
