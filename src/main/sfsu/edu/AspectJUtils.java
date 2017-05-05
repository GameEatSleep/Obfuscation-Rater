package main.sfsu.edu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;

import org.aspectj.weaver.loadtime.Agent;

import com.sun.tools.attach.VirtualMachine;

public class AspectJUtils {

	public static boolean isAspectJAgentLoaded() {
	    try {
	      Agent.getInstrumentation();
	    } catch (NoClassDefFoundError e) {
	      return false;
	    } catch (UnsupportedOperationException e) {
	      return dynamicallyLoadAspectJAgent();
	    }
	    return true;
	  }

	  public static boolean dynamicallyLoadAspectJAgent() {
	    String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
	    int p = nameOfRunningVM.indexOf('@');
	    String pid = nameOfRunningVM.substring(0, p);
	    try {
	      VirtualMachine vm = VirtualMachine.attach(pid);
	      String jarFilePath = System.getProperty("AGENT_PATH");
	      vm.loadAgent(jarFilePath);
	      vm.detach();
	    } catch (Exception e) {
	      System.out.println("Failed to dynamically load");
	      System.out.println(e);
	      return false;
	    }
	    return true;
	  }
	  public static int exec(Class klass, String filePath) throws IOException, InterruptedException {
		  String currentDir = System.getProperty("user.dir");
		  String aspectJarPath = currentDir.concat("\\bin\\main\\sfsu\\edu\\aspect.jar");
		  String aspectRTPath = currentDir.concat("\\bin\\main\\sfsu\\edu\\aspectjrt.jar");
				  
		  ArrayList<String> paramsExecute = new ArrayList<String>();
		  paramsExecute.add("java");
		  paramsExecute.add("-cp");
		  paramsExecute.add(filePath+";" + aspectRTPath + ";" + aspectJarPath);
		  paramsExecute.add("-javaagent:bin/main/sfsu/edu/aspectjweaver.jar");
		  paramsExecute.add(klass.getName());
		  
		  ProcessBuilder builderExecute = new ProcessBuilder(paramsExecute);

//		  builderExecute.inheritIO();
		  Process process = builderExecute.start();

          // get output from the process
          BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
          String line;
          while ((line = in.readLine()) != null) {
              if (line.contains("Max calldepth for")) {
            	  //get the number after the colon
            	  String output = line.substring(line.indexOf(":") + 1).trim();
            	  System.out.println("Calldepth: " + output);
            	  DisplayDetails.cd=Integer.parseInt(output);
              }
          }
          process.waitFor();

          in.close();
		  return 0;
	  }
}
