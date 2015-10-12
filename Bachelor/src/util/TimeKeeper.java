package util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

public class TimeKeeper {
	static HashMap<String,Double> nameToS = new HashMap<String,Double>();
	String name;
	long paused = 0;
	long startTimeNano;
	long pauseTime = -1;
	
	public TimeKeeper(String name){
		this.name = name.toUpperCase();
		startTimeNano = System.nanoTime();
	}
	
	public void pause(){
		if (pauseTime == -1){
			pauseTime = System.nanoTime();
		}
	}
	
	public void resume(){
		if (pauseTime != -1){
			long resumeTime = System.nanoTime();
			paused = paused + (resumeTime-pauseTime);
			pauseTime = -1;
		}
	}
	
	public void stop(){
		long endTimeNano = System.nanoTime();
		double timeTakenNano = ((double)(endTimeNano-startTimeNano-paused)) / (double)1000000;
		String s = "=== "+name+" TOOK "+timeTakenNano+" MS ===";
		nameToS.put(name, timeTakenNano);
		System.out.println(s);
	}
	
	public static void dump() throws IOException{
		for (String name : nameToS.keySet()){
			PrintWriter bw = new PrintWriter(new BufferedWriter(new FileWriter("logs/"+name+".txt",true)));
			bw.write(""+nameToS.get(name)+"\r\n");
			bw.close();
		}
	}
}
