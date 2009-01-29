package edu.berkeley.gamesman.util.threading;

import edu.berkeley.gamesman.util.Util;

public final class Barrier {

	int total = 0, waiting = 0;
	boolean broken = false;
	
	public Barrier(){
		
	}
	
	public synchronized void enter(){
		total++;
	}
	
	public synchronized void exit(){
		total--;
	}
	
	public synchronized void sync(){
		if(total == 1)
			return;
		if(broken){
			//Util.debug("Thread came to broken barrier, waiting for reset...");
			while(broken) 
				try{
					this.wait();
				}catch (InterruptedException e) {}
			//Util.debug("Someone reset the barrier, yay");
		}
		
		if(waiting+1 == total){
			Util.debug("Breaking the barrier!");
			broken = true;
			this.notifyAll();
		}else{
			waiting++;
			//Util.debug("Waiting for barrier...");
			while(!broken)
				try{
					this.wait();
				}catch(InterruptedException e){}
			waiting--;
			if(waiting == 0){
				broken = false;
				//Util.debug("Last thread left, resetting barrier");
				this.notifyAll();
			}
		}
	}
	
}
