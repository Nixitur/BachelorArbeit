package util;

import java.util.Date;

public class TimeKeeper {
	String name;
	long startTime;
	
	public TimeKeeper(String name){
		this.name = name.toUpperCase();
		Date startDate = new Date();
		startTime = startDate.getTime();
	}
	
	public void stop(){
		Date endDate = new Date();
		long endTime = endDate.getTime();
		System.out.println("=== "+name+" TOOK "+(endTime-startTime)+" MS ===");
	}
}
