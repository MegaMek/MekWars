package common.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class MekwarsFileReader {
	FileInputStream fis;
	BufferedReader dis;
	
	public MekwarsFileReader(String filename) throws FileNotFoundException {
        fis = new FileInputStream(new File(filename));
        dis = new BufferedReader(new InputStreamReader(fis));
	}
	
	public MekwarsFileReader(File file) throws FileNotFoundException {
        fis = new FileInputStream(file);
        dis = new BufferedReader(new InputStreamReader(fis));
	}
	
	public boolean ready() throws IOException{
		return dis.ready();
	}
	
	public String readLine() throws IOException{
		return dis.readLine();
	}
	
	public void close() throws IOException{
		dis.close();
		fis.close();
	}
	

}
