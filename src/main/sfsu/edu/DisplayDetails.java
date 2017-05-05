package main.sfsu.edu;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

public class DisplayDetails {
	static int cd=0;
	static Vector<Integer> OriginalFileVals = new Vector<Integer>();
	static Vector<Integer> ObfFileVals = new Vector<Integer>();
	static Vector<Integer> Diffmetrics = new Vector<Integer>();
	static String CmdArg = "";
	static boolean isFirstRun = true;

	public static void main(String[] args) {
		//Required for weaving at loadtime.
		URL url = DisplayDetails.class.getClassLoader().getResource("main/sfsu/edu/aspectjweaver.jar");
	    File f = new File (url.getPath());
		System.setProperty( "AGENT_PATH", f.getAbsolutePath() );
		AspectJUtils.isAspectJAgentLoaded();
		
		Scanner scanner = null;
		if (args.length != 0)
			CmdArg = args[0];
		try {
			System.out.println("*******Commmand Options*********");	System.out.println();
			System.out.println("Enter 'm' to compare difference in methods ");
			System.out.println("Enter 's' to compare difference in size ");
			System.out.println("Enter 'f' to compare difference in fields ");
			System.out.println("Enter 'a' to compare difference in attributes ");
			System.out.println("Default is overall obfuscation rating");
			System.out.println();System.out.println();
			scanner = new Scanner(System.in);
			String check = "y";
			String newArg = "";
			while (!check.equalsIgnoreCase("N")) {
				System.out.print("Enter path to file : ");
				String input = scanner.next();
				System.out.println("-----------------------\n");

				System.out.println("*******Analyzing*********");
				getFiles(input);
				System.out.println();
				System.out.println();
				System.out.print("Do you wish to analyze another obfuscator directory? : Y/N ");
				check = scanner.next();
				System.out.println();
				if (!check.equalsIgnoreCase("n")) {
					System.out.print("Please enter a command option; press any other key for char key for default  ");
					newArg = scanner.next();
				}
				System.out.println();
				if (newArg != null) {
					if (newArg.equalsIgnoreCase("a"))

						CmdArg = "a";
					else if (newArg.equalsIgnoreCase("m"))
						CmdArg = "m";
					else if (newArg.equalsIgnoreCase("s"))
						CmdArg = "s";
					else if (newArg.equalsIgnoreCase("f"))
						CmdArg = "f";
					else
						CmdArg = "";
				}
				OriginalFileVals.removeAllElements();
				ObfFileVals.removeAllElements();
				Diffmetrics.removeAllElements();
				isFirstRun = true;
			}
			System.out.println();
			System.out.println("*******Done*********");
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (scanner != null) {
				scanner.close();
			}
		}
	}

	private static void getFiles(String input) throws IOException {

		// Create a file pointing to the string input
		FolderTraverse fw = new FolderTraverse();
		fw.walk(input);
		if (fw.foundFiles.isEmpty()) {
			System.out.println("No Files Found!");
			return;
		}

		// Now let's collect the files together
		for (Map.Entry<String, File> entry : fw.foundFiles.entrySet()) {
			if (!entry.getKey().contains("obf")) {
				File f1 = entry.getValue();
				File f2 = fw.foundFiles.get(entry.getKey() + "obf");
				try {
					System.out.println("Testing calldepth of " + f1);
					execMethod(f1);
				String newFileName = FileUtils.getFileNameWithoutExtension(f1) + FileUtils.getExtensionWithoutFileName(f1);
	           String tempFolderPath = f1.getParentFile().getPath().concat("\\obftmp");
	           File tempFolder = new File(tempFolderPath);
	           if (!tempFolder.exists()) {
	        	   tempFolder.mkdir();
	           }	        	   
	           String newFilePath = f1.getParentFile()+"\\obftmp\\"+newFileName;
	           File tempFile = new File(newFilePath);
	           FileUtils.copyFileUsingFileStreams(f2,  tempFile);
	           execMethod(tempFile);
	           //we're done - delete the temp folder & its contents so we don't leave any traces behind
	           FileUtils.delete(tempFolder);
				} catch (Exception e) {
					e.printStackTrace();
				} 
				readFile(f1);
				readFile(f2);
				// clear vectors here cause we just compared the two
				OriginalFileVals.removeAllElements();
				ObfFileVals.removeAllElements();
				Diffmetrics.removeAllElements();
				cd=0;
				isFirstRun = true;
			}
		}
	}
	private static void execMethod(File f1)
			throws MalformedURLException, ClassNotFoundException, IOException, InterruptedException {
		String filePath = f1.getAbsolutePath().
			     substring(0,f1.getAbsolutePath().lastIndexOf(File.separator));
		File ammar = new File(filePath);
		URL url = ammar.toURL();
		URL[] cp = new URL[]{url};
		URLClassLoader urlcl = new URLClassLoader(cp);
		String fileName = FileUtils.getFileNameWithoutExtension(f1);
		Class clazz = urlcl.loadClass(fileName);
		int value = AspectJUtils.exec(clazz, filePath);
	}
	private static void readFile(File file) {
		int TMACount = 0, MCount = 0, FCount = 0, AttCount = 0, BCount = 0, Alen = 0;

		FileInputStream fis;
		try {
			fis = new FileInputStream(file);

			/* Parse the class */
			ClassParser parser = new ClassParser(fis, "DirExplorer.class");
			JavaClass javaClass = parser.parse();
			for (Attribute A : javaClass.getAttributes()) {
				AttCount++;
			}			
			for (Field field : javaClass.getFields()) {
				FCount++;
			}
			for (byte Byte : javaClass.getBytes()) {
				BCount++;
			}
			for (Method method : javaClass.getMethods()) {				
				for (Attribute MA : method.getAttributes()) {
					Alen += MA.getLength();
					TMACount++;
				}
				MCount++;
			}
			String Cname = javaClass.getClassName();
			System.out.println();
			System.out.println();
			String fileNameOnly = file.getName().replaceFirst("[.][^.]+$", "");
			if (fileNameOnly.endsWith("obf".toLowerCase())) {
				ObfFileVals.add(BCount);
				ObfFileVals.add(MCount);
				ObfFileVals.add(FCount);
				ObfFileVals.add(TMACount);
				ObfFileVals.add(Alen);
				ObfFileVals.add(cd);				
			} else {
				OriginalFileVals.add(BCount);
				OriginalFileVals.add(MCount);
				OriginalFileVals.add(FCount);
				OriginalFileVals.add(TMACount);
				OriginalFileVals.add(Alen);
				OriginalFileVals.add(cd);
			}
			if (isFirstRun) {
				isFirstRun = false;
				if (fileNameOnly.endsWith("obf".toLowerCase())) {
					fileNameOnly = fileNameOnly.substring(0, fileNameOnly.length() - 3);
				}
				System.out.println("Now Analyzing: " + fileNameOnly);
			} else {
				int diff = 0;
				for (int i = 0; i < 6; i++) {
					diff = Math.abs(OriginalFileVals.elementAt(i) - ObfFileVals.elementAt(i));
					Diffmetrics.add(diff);
				}
			}
			if (Diffmetrics.size() != 0 && Diffmetrics != null) {
				if (CmdArg.equalsIgnoreCase("m")) {
					System.out.println(
							"Number of Methods modified in: " + Cname + ".class  == " + Diffmetrics.elementAt(1));
				}
				else if (CmdArg.equalsIgnoreCase("s")) {
					System.out.println("Difference in size between obfuscated and original " + Cname + ".class== "
							+ Diffmetrics.elementAt(0) + " bytes");
				}
				else if (CmdArg.equalsIgnoreCase("f")) {
					System.out.println("Number of fields modified by obfuscation in: " + Cname + ".class  == "
							+ Diffmetrics.elementAt(2));
				}
				else if (CmdArg.equalsIgnoreCase("a")) {
					System.out.println("Total Attributes modified by obfuscation in: " + Cname + ".class  == "
							+ Diffmetrics.elementAt(3));
				}
				 else {
					double rating;
					double size = 0, mets = 0, fld = 0, atts = 0, cp = 0, al = 0, dyn = 0;
					// File Size
					if (OriginalFileVals.elementAt(0) >= (3 * ObfFileVals.elementAt(0)))
						size = 10;
					else if (OriginalFileVals.elementAt(0) >= (2 * ObfFileVals.elementAt(0)))
						size = 7.5;
					else if (OriginalFileVals.elementAt(0) >= (1.5 * ObfFileVals.elementAt(0)))
						size = 5;
					else if (OriginalFileVals.elementAt(0) > ObfFileVals.elementAt(0))
						size = 2;
					// Methods Difference
					if (OriginalFileVals.elementAt(1) >= (3 * ObfFileVals.elementAt(1)))
						mets = 10;
					else if (OriginalFileVals.elementAt(1) >= (2 * ObfFileVals.elementAt(1)))
						mets = 7.5;
					else if (OriginalFileVals.elementAt(1) >= (1.5 * ObfFileVals.elementAt(1)))
						mets = 5;
					else if (OriginalFileVals.elementAt(1) > ObfFileVals.elementAt(1))
						mets = 2;
					// Fields Difference
					if (OriginalFileVals.elementAt(2) >= (3 * ObfFileVals.elementAt(2)))
						fld = 10;
					else if (OriginalFileVals.elementAt(2) >= (2 * ObfFileVals.elementAt(2)))
						fld = 7.5;
					else if (OriginalFileVals.elementAt(2) >= (1.5 * ObfFileVals.elementAt(2)))
						fld = 5;
					else if (OriginalFileVals.elementAt(2) > ObfFileVals.elementAt(2))
						fld = 2;
					// Method Attributes Difference
					if (OriginalFileVals.elementAt(3) >= (3 * ObfFileVals.elementAt(3)))
						atts = 10;
					else if (OriginalFileVals.elementAt(3) >= (2 * ObfFileVals.elementAt(3)))
						atts = 7.5;
					else if (OriginalFileVals.elementAt(3) >= (1.5 * ObfFileVals.elementAt(3)))
						atts = 5;
					else if (OriginalFileVals.elementAt(3) > ObfFileVals.elementAt(3))
						atts = 2;
					// Dynamic, call depth Difference
					if (OriginalFileVals.elementAt(5) > (2 * ObfFileVals.elementAt(5)))
						dyn = 10;
					else if (OriginalFileVals.elementAt(5) < (2 * ObfFileVals.elementAt(5)))
						dyn = 10;
					else if (OriginalFileVals.elementAt(5) > ( ObfFileVals.elementAt(5)))
						dyn = 7.5;
					else if (OriginalFileVals.elementAt(5) < ObfFileVals.elementAt(5))
						dyn = 7.5;
					
					rating = ((cd + fld + mets + atts + size + dyn) / 6);
					System.out.println("Obfuscation Rating  == " + rating);
				}
			} else {		}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}