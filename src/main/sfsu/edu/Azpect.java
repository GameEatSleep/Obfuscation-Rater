package main.sfsu.edu;


import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import java.lang.Runtime;
import java.lang.Thread;

public aspect Azpect {

 private int callDepth = 0;
	private int maxDepth = 0;

	private String currentClassName ="";

	@Pointcut("execution(* *(..))")
	public void whatToMatch (){}
	
	@Pointcut("execution(* Azpect.*(..))")
	public void whatNotToMatch(){}
	
	@Pointcut ("execution(* *.main(..))")
	protected void startOfMainMethod() {}

	@Pointcut("whatToMatch() && ! whatNotToMatch() && !startOfMainMethod()")
	protected void loggingOperation()
    {
    }
	
	@Before("loggingOperation()")
	public void logJoinPoint(ProceedingJoinPoint joinPoint) {
		printIndent();
		callDepth++;
		maxDepth = Math.max(maxDepth, callDepth);
	}
	
	private void checkClass(ProceedingJoinPoint joinPoint, String action) {
		String newClass = joinPoint.getSignature().getDeclaringTypeName();
		if (currentClassName.isEmpty()) {
			currentClassName = newClass;
		} else {
			//check if the classname changed
			if (currentClassName.equals(newClass)) {
				StringBuilder outputString = new StringBuilder();
				// If we are exiting, print the maxDepth, then reset it.
				if (action.equals("Exiting")) {
					resetCounters();
				}
				System.out.println(outputString);
			}
		}
	}

	private void resetCounters() {
		callDepth = 0;
	}

	@After("loggingOperation()")
	public void logExitPoint(ProceedingJoinPoint joinPoint) {
		callDepth--;
		printIndent();
		checkClass(joinPoint, "Exiting");		
	}	
	
	 protected void printIndent() {
		 if (callDepth!=0) {
			 for (int i = 0; i <= callDepth; i++) {
				 System.out.print("  ");
			 }
		 } 
	 }

	 //Needed to know when we're leaving.
	 @Before("startOfMainMethod()")
     public void logMainMethodStart()
	 {
		 Runtime.getRuntime().addShutdownHook(new Thread() {
			 public void run() { 
				 System.out.println("Max calldepth for " + currentClassName + ": " + maxDepth);;
			 }
		 });
     }
}

