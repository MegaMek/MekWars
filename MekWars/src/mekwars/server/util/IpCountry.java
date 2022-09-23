package server.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * <p>T�tulo:  IpCountry</p>
 * <p>Description: Search the name of the country from a ip </p>
 * <p> Need the file location with the knowed ips</p>
 * @author Northway, Geyrat Baliset.
 * @version Beta.1.3
 */

public class IpCountry {
  private FileOps file;
  private FileOps fileNames;
  private static int dim = 32;
  int iteracion =0;
  public IpCountry() {
    this.file = new FileOps ();
  }
  public IpCountry (String fileIps,String fileNames ){
    this ();
    this.file = new FileOps (fileIps);
    this.fileNames = new FileOps (fileNames);
  }

  private boolean [] ipToBytes (String ip){
    boolean [] ipResult = new boolean [dim];
    int pos = ip.indexOf(".");
    int [] ipVal = new int [4];
    int pos2 = ip.indexOf (".",pos+1);
    int pos3 = ip.indexOf (".",pos2+1);
    int pos4 = ip.length();
    int decVal =0;
    ipVal [0] = Integer.parseInt(ip.substring(0,pos));
    ipVal [1] = Integer.parseInt(ip.substring(pos+1,pos2));
    ipVal [2] = Integer.parseInt(ip.substring(pos2+1,pos3));
    ipVal [3] = Integer.parseInt(ip.substring(pos3+1,pos4));
    for (int i = ipResult.length-1; i >=0;i--){//binary conversion
      decVal = (int) StrictMath.pow(2, (((dim - i) - 1) % (dim/4)));
      ipResult [i]= (decVal & ipVal [i/8]) == decVal;
    }

    return ipResult;
  }

  public String seachIpCountry (String ipSource){
    try {
      boolean[] ip = ipToBytes(ipSource);
      boolean[] ipTable = ip.clone();// necesary to enter the while first time.
      boolean[] mask = {true};
      String country = "Unknown";
      boolean found = false;// if one of the countries matched the seach
      boolean thisOne = false;//if the current countr match the search.
      String cad ="";
      file.openFile();
      while (! file.eofFile() && //end of file of search so rare but possible.
             BoolMatrixOps.GreaterOrEqual (ip,ipTable) //ip < ipTable such the file is SHORTED no need to continue the search
             ) {
        cad = file.getTextFromFile();
        ipTable = getIp(cad);
        mask = getMask(cad);
        thisOne =BoolMatrixOps.equal(BoolMatrixOps.and(ip, mask), ipTable);
        found |= thisOne;
        if (thisOne) {
          country = getCountry(cad);
        }
      }
      file.closeFile();
      return largeName(country,fileNames);
    }
    catch (Exception ex) {
      return "Moon";
    }
  }
  private boolean [] getIp (String cad){
    try {
      return ipToBytes(cad.substring(cad.indexOf(":") + 1, cad.indexOf("/")));
    }
    catch (Exception ex) {
      return ipToBytes ("0.0.0.0");
    }
  }
  private boolean [] getMask (String cad){
    try {
      int pos = cad.indexOf("/");
      int mask = Integer.parseInt(cad.substring(pos + 1, cad.length()));
      boolean[] res = new boolean[dim];
      for (int i = 0; i < res.length; i++) {
        res[i] = i < mask;
      }
      return res;
    }
    catch (Exception ex) {
      return ipToBytes ("0.0.0.0");
    }
  }
  private String getCountry (String cad){
    try {
      int pos = cad.indexOf(":");
      return cad.substring(0, pos);
    }
    catch (Exception ex) {
      return "Moon";
    }
  }
  private String largeName (String shortName,FileOps file){
    String res = shortName;
    String current ="";
    String cad = "";
    file.openFile();
    try {
      while (!file.eofFile()) {
        cad = file.getTextFromFile();
        current = cad.substring(0, cad.indexOf(" ", 0));
        if (0 == current.compareTo(shortName)) {
          res = cad.substring(cad.indexOf(" ", 0), cad.length()).trim();
          break;
        }
      }
    }
    catch (Exception ex) {
      res="Moon";
    }
    finally {
      file.closeFile();
    }
    return res;
  }

}

class BoolMatrixOps {
  public BoolMatrixOps (){

  }
  public static boolean [] and (boolean [] a, boolean [] b){
    if (a.length != b.length){
      return null;
    }
    boolean [] r = new boolean [a.length];
    for (int i =0; i < r.length ;i++){
      r [i] =a [i] &&  b [i];
    }
    return r;
  }
  public static boolean  equal (boolean [] a, boolean [] b){
    if (a.length != b.length){
      return false;
    }
    boolean res = true;
    for (int i =0; i< a.length; i++){
      res &= a [i] == b [i];
    }
    return res;
  }
  public static boolean GreaterOrEqual (boolean [] a, boolean []b) throws java.lang.ArrayIndexOutOfBoundsException{
    if (a.length != b.length){
      throw new java.lang.ArrayIndexOutOfBoundsException();
     }
    boolean res = true;
    for (int i =0; i< a.length; i++){
      if  (a [i] != b [i]){
        res = a [i];
        break;
      }
    }
    return res;
  }
}
class FileOps {
  private File file;
  private FileInputStream fis;
  private BufferedReader br;
  public FileOps (){

  }
  public FileOps (String file) {
    this ();
    this.file = new File(file);
  }
  public int openFile () {
    try {
      fis = new FileInputStream(file);
      br = new BufferedReader (new InputStreamReader(fis));
      return 0;
    }
    catch (FileNotFoundException ex) {
      return 1;
    }
  }
  public int closeFile () {
    try {
      fis.close();
      br.close();
      return 0;
    }
    catch (IOException ex) {
      return 1;
    }
  }
  public String getTextFromFile () {

    try {
      return br.readLine();
    }
    catch (IOException ex) {
      return "";
    }
  }
  public boolean eofFile () {
    try {
      return !br.ready();
    }
    catch (IOException ex) {
      return true;
    }
  }
}