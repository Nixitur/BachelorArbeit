package example;

/**
 * An example class to test if the tracing and embedding works correctly when there's trace points in multiple classes.
 * @author Kaspar
 *
 */
public class OtherExample {

	public static void otherClassMethod(){
		Marker.mark("otherClass");
	}

}
