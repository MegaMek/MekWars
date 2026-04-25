/*
 * MekWars - Copyright (C) 2007 
 * 
 * Original author - Torren (torren@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

/*
 * Created on 10.30.2007
 *   
 */

package common.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadManager{
	
	private static ThreadManager instance = new ThreadManager();
	
	private ExecutorService executor;
	
	protected ThreadManager(){
		executor = Executors.newCachedThreadPool();
	}
	
	public static ThreadManager getInstance(){
		return instance;
	}
	
	public void runInThreadFromPool(Thread runnable){
		try{
			executor.execute(runnable);
		}catch(Exception ex ){
			MWLogger.errLog(ex);
		}
	}
	
	
	public void shutdown(){
		executor.shutdown();
	}
}