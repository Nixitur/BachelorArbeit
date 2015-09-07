package util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigParser {
	public static final int NO_EDGE = 0;
	public static final int LIST_EDGE = 1;
	public static final int TREE_EDGE = -1;
	
	private Properties prop;

	public ConfigParser(String fileName) throws IOException {
		prop = new Properties();
		InputStream input = new FileInputStream(fileName);
		prop.load(input);
	}
	
	public String classPath(){
		return prop.getProperty("classPath");
	}
	
	public String mainClass(){
		return prop.getProperty("mainClass");
	}
	
	public String[] arguments(){
		return prop.getProperty("arguments").split(",");
	}
	
	public int encodingNumber(){
		return Integer.parseInt(prop.getProperty("encodingNumber"));
	}
	
	public String watermarkPackage(){
		return prop.getProperty("watermarkPackage");
	}
	
	public String markMethod(){
		return prop.getProperty("markMethod");
	}
	
	public int edgeType(){
		String edgeType = prop.getProperty("edgeType");
		if (edgeType.equalsIgnoreCase("tree")){
			return TREE_EDGE;
		} else if (edgeType.equalsIgnoreCase("list")){
			return LIST_EDGE;
		} else {
			return NO_EDGE;
		}
	}
	
	public int edgeNumber(){
		int edgeType = edgeType();
		if (edgeType == NO_EDGE){
			return -1;
		}
		String edgeNumber = prop.getProperty("edgeNumber");
		int result;
		try{
			result = Integer.parseInt(edgeNumber);
		} catch (NumberFormatException e){
			return -1;
		}
		int w = encodingNumber();
		// n is the bitlength of w
		int n = (int) Math.ceil(Math.log(w) / Math.log(2));
		
		boolean treeEdgeLegit = (edgeType == TREE_EDGE) && (result >= 1) && (result <= 2*n+1);
		boolean listEdgeLegit = (edgeType == LIST_EDGE) && (result >= 1) && (result <= 2*n+2);
		
		if (treeEdgeLegit || listEdgeLegit){
			return result;
		}
		return -1;
	}
}
