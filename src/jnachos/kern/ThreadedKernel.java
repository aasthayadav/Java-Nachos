package jnachos.kern;
import jnachos.machine.*;
import jnachos.filesystem.*;


public class ThreadedKernel extends Kernel {
	
	public ThreadedKernel() {
		super();
	}

	public void initialize(String[] args) {
		
		NachosFileSystem nfs ;
	
		String schedulerName = "ThreadedKernel.scheduler";
		scheduler = (Scheduler) scheduler;
		String fileSystemName = "ThreadedKernel.fileSystem";
		if (fileSystemName != null)
		{
		fileSystem = (NachosFileSystem) fileSystem;	
		}
		/*else if (nfs != null)
		{
		
		 	fileSystem = (NachosFileSystem) fileSystem;
		}*/
		
		else
			fileSystem = null;

		// start threading
		new NachosThread(null);

     	//	alarm = new Alarm();
		jnachos.machine.Interrupt.setLevel(true);
	}

	public void selfTest() {
		NachosThread.selfTest();
	//	Semaphore.selfTest();
		//SynchList.selfTest();
	/*	if (Machine.bank() != null) {
			ElevatorBank.selfTest();
		} */
	}

	public void run() {
	}

	
	public void terminate() {
	//	Machine.halt();
	}

	public static Scheduler scheduler = null;
	
	//public static Alarm alarm = null;
	
	public static NachosFileSystem fileSystem = null;

	
}