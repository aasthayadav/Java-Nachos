package jnachos.kern;
import jnachos.kern.NachosThread;


public abstract class ThreadQueue {
    
    public abstract void waitForAccess(NachosThread thread);

    
    public abstract NachosThread nextThread();

   
    public abstract void acquire(NachosThread thread);

    public abstract void print();
}