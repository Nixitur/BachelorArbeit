package example;

public abstract class ExampleAbstract {
	void methodInSuperClass(int j){
		if (j == 1){
			j = j+1;
		}
		Marker.mark("superclass");
	}
	abstract void abstractMethod();
}
