package edu.berkeley.gamesman.util;

import java.math.BigInteger;

public abstract class Task {

	private static TaskFactory factory;
	
	protected BigInteger completed,total;
	
	protected Task(){}
	
	public static Task beginTask(String name){
		if(factory != null)
			return factory.createTask(name);
		return new Task(){
			protected void begin() {}
			public void complete() {}
			public void update() {}
		};
	}
	
	public static void setTaskFactory(TaskFactory f){
		factory = f;
	}
	
	public void setProgress(long l){
		setProgress(BigInteger.valueOf(l));
	}
	
	public void setProgress(BigInteger p){
		completed = (p.compareTo(total) <= 0 ? p : total);
		update();
	}
	
	public void setTotal(long l){
		setTotal(BigInteger.valueOf(l));
	}
	
	public void setTotal(BigInteger t){
		total = t;
		begin();
	}
	
	protected abstract void begin();
	public abstract void update();
	public abstract void complete();
	
}
