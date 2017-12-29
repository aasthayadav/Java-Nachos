package jnachos.kern;
import jnachos.machine.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import jnachos.kern.Debug;
import jnachos.kern.sync.Lock;
import static org.junit.Assert.assertTrue;

public class NachosThread {
   
    public Thread threade;
	public static NachosThread currentThread() {
       assertTrue(currentThread != null);
        return currentThread;
    }

    public NachosThread() {
        if (currentThread != null) {
          
        	threade = new Thread();
        	
        	} 
        else {
            readyQueue = ThreadedKernel.scheduler.newThreadQueue(false);
           readyQueue.acquire(this);

            currentThread = this;
            
            threade=Thread.currentThread();
            name = "main";
            restoreState(); 
            createIdleThread();
        }
    }

    public NachosThread(Runnable target) {
        this();
        this.target = target;
    }

    public NachosThread setTarget(Runnable target) {
       assertTrue(status == statusNew);

        this.target = target;
        return this;
    }

    public NachosThread setName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return (name + " (#" + id + ")");
    }

    public int compareTo(Object o) {
        NachosThread thread = (NachosThread) o;

        if (id < thread.id)
            return -1;
        else if (id > thread.id)
            return 1;
        else
            return 0;
    }

    
    public void fork() {
      assertTrue(status == statusNew);
      assertTrue(target != null);

        Debug.debug(dbgThread, "Forking thread: " + toString() + " Runnable: " + target);

        boolean intStatus = jnachos.machine.Interrupt.setLevel(false);

        mThread = Thread.currentThread();

        ready();

    // jnachos.machine.Interrupt.restore(intStatus);
    }

    private void runThread() {
        begin();
        target.run();
        finish();
    }

    private void begin() {
       Debug.debug(dbgThread, "Beginning thread: " + toString());

       assertTrue(this == currentThread);

        restoreState();

        jnachos.machine.Interrupt.setLevel(true);
    }

    public static void finish() {
      Debug.debug(dbgThread, "Finishing thread: " + currentThread.toString());

        jnachos.machine.Interrupt.setLevel(false);

        assertTrue(toBeDestroyed == null);
        toBeDestroyed = currentThread;

        currentThread.status = statusFinished;

         currentThread.joinLock.acquire();
        currentThread.joinCondition.wakeAll();
         currentThread.joinLock.release();
      // currentThread.joinSemaphore.V();

        sleep();
    }

    public static void yield() {
     Debug.debug(dbgThread, "Yielding thread: " + currentThread.toString());

      assertTrue(currentThread.status == statusRunning);

        boolean intStatus = jnachos.machine.Interrupt.setLevel(false);

        currentThread.ready();

        runNextThread();

      //  jnachos.machine.Interrupt.(intStatus);
    }

    public static void sleep() {
      Debug.debug(dbgThread, "Sleeping thread: " + currentThread.toString());

       assertTrue(jnachos.machine.Interrupt.setLevel(false));

        if (currentThread.status != statusFinished)
            currentThread.status = statusBlocked;

        runNextThread();
    }

    public void ready() {
      Debug.debug(dbgThread, "Ready thread: " + toString());

        assertTrue(jnachos.machine.Interrupt.setLevel(false));
        assertTrue(status != statusReady);

        status = statusReady;
        if (this != idleThread)
            readyQueue.waitForAccess(this);

    }

    public void join() {
        Debug.debug(dbgThread, "Joining to thread: " + toString());

      assertTrue(this != currentThread);

        if (status != statusFinished){
             joinLock.acquire();
             joinCondition.sleep();
             joinLock.release();
           // joinSemaphore.P();
        }
    }

    private static void createIdleThread() {
       assertTrue(idleThread == null);

        idleThread = new NachosThread(new Runnable() {
            public void run() {
                while (true)
                    yield();
            }
        });
        idleThread.setName("idle");
        idleThread.fork();
    }

    private static void runNextThread() {
        NachosThread nextThread = readyQueue.nextThread();
        if (nextThread == null)
            nextThread = idleThread;

        nextThread.run();
    }

    private void run() {
        assert(jnachos.machine.Interrupt.setLevel(false));

      //  Machine.yield();

        currentThread.saveState();

        Debug.debug(dbgThread, "Switching from: " + currentThread.toString() + " to: " + toString());

        currentThread = this;

      //  tcb.contextSwitch();

        currentThread.restoreState();
    }

    protected void restoreState() {
       Debug.debug(dbgThread, "Running thread: " + currentThread.toString());

       assertTrue(jnachos.machine.Interrupt.setLevel(false));
        assertTrue(this == currentThread);
      // assertTrue(tcb == TCB.currentTCB());
        
        status = statusRunning;

        if (toBeDestroyed != null) {
       //     toBeDestroyed.tcb.destroy();
        	toBeDestroyed.threade.destroy();
         //   toBeDestroyed.tcb = null;
        	toBeDestroyed.threade =null;
            toBeDestroyed = null;
        }
    }

    protected void saveState() {
     assertTrue(jnachos.machine.Interrupt.setLevel(false));
     assertTrue(this == currentThread);
    }

    private static class PingTest implements Runnable {
        PingTest(int which) {
            this.which = which;
        }

        public void run() {
            for (int i = 0; i < 5; i++) {
                System.out.println("*** thread " + which + " looped " + i + " times");
                currentThread.yield();
            }
        }

        private int which;
    }

    private static void doPingTest() {
        System.out.println("[test:KThread] pingpong test started");
        new NachosThread(new PingTest(1)).setName("forked thread").fork();
        new PingTest(0).run();
        System.out.println("[test:KThread] pingpong test passed");
    }

    private static class JoinTestRec {
        JoinTestRec() {
            c = 0;
        }
        public int c;
    }

    private static class JoinModifier implements Runnable {
        JoinModifier(int id, JoinTestRec rec, Lock lock) {
            i = id;
            r = rec;
            l = lock;
        }

        public void run() {
            l.acquire();
            r.c++;
            System.out.println("*** modifier " + i + " modifies count to " + r.c);
            l.release();
        }

        private int i;
        private JoinTestRec r;
        private Lock l; 
    }

    private static void doMultipleModifierJoinTest(int n){
        System.out.println("[test:KThread] " + n + " modifier join test started");
        JoinTestRec r = new JoinTestRec();
        List<NachosThread> pool = new ArrayList<NachosThread>();
        Lock recLock = null;
        
        for (int i = 0; i < n; ++ i){
            NachosThread p = new NachosThread(new JoinModifier(i, r, recLock)).setName("modifier" + i);
            pool.add(p);
            p.fork();
        }
        for (int i = 0; i < n; ++ i)
            pool.get(i).join();
             assertTrue(r.c == n);
        System.out.println("[test:KThread] " + n + " modifier join test passed");
    }

    private static void doJoinMutipleTimesJoinTest(){
    	Lock lock = null;
        System.out.println("[test:KThread] multiple time join test started");
        JoinTestRec r = new JoinTestRec();
       NachosThread p = new NachosThread(new JoinModifier(0, r, lock)).setName("modifier");
        p.fork();
        p.join();
        p.join();
        assertTrue(r.c == 1);
        System.out.println("[test:KThread] multiple time join test passed");
    }

    private static void doJoinTest() {
        doMultipleModifierJoinTest(1);
        doMultipleModifierJoinTest(10);
        doJoinMutipleTimesJoinTest();
    }

   
    public static void selfTest() {
       Debug.debug(dbgThread, "Enter NachosThread.selfTest");

        doPingTest();
        doJoinTest();
    }

    
	private static final char dbgThread = 't';

    public Object schedulingState = null;

    private static final int statusNew = 0;
    private static final int statusReady = 1;
    private static final int statusRunning = 2;
    private static final int statusBlocked = 3;
    private static final int statusFinished = 4;
    private Lock joinLock = new Lock();
    private Condition joinCondition = new Condition(joinLock);
    private int id = numCreated++;

    private int status = statusNew;
    private String name = "(unnamed thread)";
    private Runnable target;
   
    private Thread pthread;
    private Semaphore joinSemaphore = new Semaphore(0);
    
    private static int numCreated = 0;
    private Thread mThread;

    private static ThreadQueue readyQueue = null;
    private static NachosThread currentThread = null;
    private static NachosThread toBeDestroyed = null;
    private static NachosThread idleThread = null;
}