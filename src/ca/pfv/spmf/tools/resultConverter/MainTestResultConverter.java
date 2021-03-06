package ca.pfv.spmf.tools.resultConverter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import ca.pfv.spmf.tools.dataset_converter.Formats;
import ca.pfv.spmf.tools.dataset_converter.TransactionDatabaseConverter;

/**
 * Example of how to convert items in a result from integer to string by using
 * metadata in an input file.
 */
public class MainTestResultConverter {
	
	public static void main(String [] arg) throws IOException{
		
		String inputDB = fileToPath("example.txt");
//		String inputResult = fileToPath("association_rules.txt");
		String inputResult = fileToPath("frequent_itemsets.txt");
		String outputFile = "C://patterns//result_converted.txt";
		
		try{
			ResultConverter converter = new ResultConverter();
			converter.convert(inputDB, inputResult, outputFile);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	

	public static String fileToPath(String filename) throws UnsupportedEncodingException{
		URL url = MainTestResultConverter.class.getResource(filename);
		 return java.net.URLDecoder.decode(url.getPath(),"UTF-8");
	}
}
