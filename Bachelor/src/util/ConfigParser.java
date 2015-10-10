package util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ConfigParser {
	public static final int NO_EDGE = 0;
	public static final int LIST_EDGE = 1;
	public static final int TREE_EDGE = -1;
	private int n = -1;
	
	private Properties prop;

	public ConfigParser(String fileName) throws IOException {
		prop = new Properties();
		InputStream input = new FileInputStream(fileName);
		prop.load(input);
		input.close();
	}
	
	public String classPath(){
		return prop.getProperty("classPath");
	}
	
	public String mainClass(){
		return prop.getProperty("mainClass");
	}
	
	public String arguments(){
		return prop.getProperty("arguments");
	}
	
	// also sets n
	public long encodingNumber(){
		long w = Long.parseLong(prop.getProperty("encodingNumber"));
		if (n < 0){
			n = (int) Math.ceil(Math.log(w) / Math.log(2));
		}
		return w;
	}
	
	public String watermarkClass(){
		// TODO: regex-checking for letters, numbers and dot
		String fullClassName = prop.getProperty("watermarkClass");
//		int i = fullClassName.lastIndexOf(".");
//		String packageName = fullClassName.substring(0, i);
//		char first = fullClassName.charAt(i+1);
//		String truncatedName = fullClassName.substring(i+2,fullClassName.length());
//		if ((first >= 'a') && (first <= 'z')){
//			first = (char) (first - 32);
//		} else if ((first <= 'A') || (first >= 'Z')){
//			throw new IllegalStateException("The first character of the class name must be a letter.");
//		}
//		fullClassName = packageName+"."+first+truncatedName;
		return fullClassName;
	}
	
	public String markMethod(){
		return prop.getProperty("markMethod");
	}
	
	public int deleteEdgeType(){
		String deleteEdgeType = prop.getProperty("deleteEdgeType");
		if (deleteEdgeType.equalsIgnoreCase("tree")){
			return TREE_EDGE;
		} else if (deleteEdgeType.equalsIgnoreCase("list")){
			return LIST_EDGE;
		} else {
			return NO_EDGE;
		}
	}
	
	public int deleteEdgeNumber(){
		int deleteEdgeType = deleteEdgeType();
		if (deleteEdgeType == NO_EDGE){
			return -1;
		}
		String deleteEdgeNumber = prop.getProperty("deleteEdgeNumber");
		int result;
		try{
			result = Integer.parseInt(deleteEdgeNumber);
		} catch (NumberFormatException e){
			return -1;
		}
		if (n < 0){
			// this sets n
			encodingNumber();
		}
		
		boolean treeEdgeLegit = (deleteEdgeType == TREE_EDGE) && (result >= 1) && (result <= 2*n+1);
		boolean listEdgeLegit = (deleteEdgeType == LIST_EDGE) && (result >= 1) && (result <= 2*n+2);
		
		if ((treeEdgeLegit && (deleteEdgeType == TREE_EDGE)) || (listEdgeLegit && (deleteEdgeType == LIST_EDGE))){
			return result;
		}
		return -1;
	}
	
	public int[] flipEdgeNumbers(){
		if (n < 0){
			// this sets n
			encodingNumber();
		}
		List<Integer> flipEdgeNumbers = new ArrayList<Integer>();
		String numbers = prop.getProperty("flipEdgeNumbers");
		String[] numbersArray = numbers.split(" ");
		int flipEdgeNumber;
		for (String numberString : numbersArray){
			try{
				flipEdgeNumber = Integer.parseInt(numberString);
				if ((flipEdgeNumber >= 1) && (flipEdgeNumber <= 2*n+1)){
					flipEdgeNumbers.add(flipEdgeNumber);
				}
			} catch (NumberFormatException e) {
				// just do nothing
			}
		}
		int[] result = new int[flipEdgeNumbers.size()];
		int i = 0;
		for (Integer num : flipEdgeNumbers){
			result[i] = num;
			i++;
		}
		return result;
	}
}
