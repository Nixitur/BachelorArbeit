package example;
import java.awt.event.*;
import javax.swing.*;
/**
 * An example class to test tracing and embedding.
 * @author Kaspar
 *
 */
public class Example extends ExampleAbstract implements ActionListener, ExampleFace{
    public static void q(String i){
        Marker.mark(i);
    }
    public static void p(int i){
    	Marker.mark(i);
        if (i < 3) q("0"+i);
        if (i < 0) d(i);
    }
    public static void d(int i){
    	Marker.mark(i);
    }
    public void instanceMethod(){
    	Marker.mark("instanceMethod");
    	PrivExample p = new PrivExample();
    	p.privMethod();
    }
    public void actionPerformed(ActionEvent e){
    	Marker.mark();
    	abstractMethod();
    	OtherExample.otherClassMethod();
        q(e.getActionCommand());
        interfaceMethod();
    }
    public static void main(String[] args){
        p(1);
        p(args.length+3);
        JFrame f = new JFrame();
        JButton b = new JButton("Test");
        b.addActionListener(new Example());
        b.setActionCommand(args[0]);
        f.getContentPane().add(b);
        f.setLocationRelativeTo(null);
        f.pack(); f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Example e = new Example();
        e.instanceMethod();
    }

    @Override
    /**
     * For checking if it works right with interface methods.
     */
    public void interfaceMethod(){
    	Marker.mark("interface");
    }
    
	@Override
	/**
	 * For checking if it works right with abstract methods and methods in a superclass.
	 */
	void abstractMethod() {
		Marker.mark("abstract");
		methodInSuperClass();
	}
    
	private class PrivExample{
		public void privMethod(){
			Marker.mark("privateClass");
		}		
	}
    
}