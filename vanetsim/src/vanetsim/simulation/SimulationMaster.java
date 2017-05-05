package vanetsim.simulation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import vanetsim.ErrorLog;
import vanetsim.VanetSimStart;
import vanetsim.gui.Renderer;
import vanetsim.gui.controlpanels.ReportingControlPanel;
import vanetsim.gui.helpers.PrivacyLogWriter;
import vanetsim.gui.helpers.ReRenderManager;
import vanetsim.localization.Messages;
import vanetsim.map.Map;
import vanetsim.map.Region;
import vanetsim.scenario.KnownVehiclesList;
import vanetsim.scenario.KnownRSUsList;
import vanetsim.scenario.Scenario;
import vanetsim.scenario.Vehicle;
import vanetsim.scenario.events.EventList;

/**
 * This thread delegates the simulation processing to subthreads and then calls a
 * repaint on the drawing area.
 */
public final class SimulationMaster extends Thread{

	/** How much time passes in one step (in milliseconds). 40ms results in a smooth animation with 25fps. */
	public static final int TIME_PER_STEP = 40;
	
	/** The list with all events */
	private static final EventList eventList_ = EventList.getInstance();
	
	/** a flag to indicate if the performance should be logged */
	private static boolean logPerformance_ = true;
	
	/** a variable to save the start time */
	private static long startTime = 0;
	
	/** Indicates if this simulation should run. If this flag is updated to false the current simulation step 
	 * is finished and afterwards the simulation stops */
	private volatile boolean running_ = false;

	/** The do one step. */
	private volatile boolean doOneStep_ = false;
	
	/** If the mode to jump to a specific time is enabled or not. */
	private volatile boolean jumpTimeMode_ = false;
	
	/** The time, one step should have in realtime. Decrease to get a faster simulation, increase to get a slower simulation. */
	private volatile int targetStepTime_ = TIME_PER_STEP;
	
	/** A target time to jump to */
	private volatile int jumpTimeTarget_ = -1;

	/** An array holding all worker threads. */
	private WorkerThread[] workers_ = null;

	/** Synchronization barrier for the start of the working threads. */
	private CyclicBarrier barrierStart_ = null;
	
	/** Synchronization barrier for the worker threads. */
	private CyclicBarrier barrierDuringWork_ = null;

	/** Synchronization barrier for the end of one step in the working process. */
	private CyclicBarrier barrierFinish_ = null;
	
	/** GUI disabled or enabled */
	private boolean guiEnabled = true;
	
	/** Flag to log silent period header once */
	private boolean logSilentPeriodHeader_ = true;
	/**
	 * Instantiates a new simulation master.
	 */
	public SimulationMaster(){
	}

	/**
	 * Method to let this thread start delegating work to subthreads. Work in the main function is resumed, the
	 * subthreads (workers) will wake up again and the Renderer is notified to get active again.
	 */  
	public synchronized void startThread(){
		// write silent period log header
		if(Vehicle.isSilentPeriodsOn() && logSilentPeriodHeader_) {
			logSilentPeriodHeader_ = false;
			PrivacyLogWriter.log("Silent Period:Duration:" + Vehicle.getTIME_OF_SILENT_PERIODS() + ":Frequency:" + Vehicle.getTIME_BETWEEN_SILENT_PERIODS());
		}
		
		
		
		Renderer.getInstance().notifySimulationRunning(true);
		ErrorLog.log(Messages.getString("SimulationMaster.simulationStarted"), 2, SimulationMaster.class.getName(), "startThread", null); //$NON-NLS-1$ //$NON-NLS-2$
		Renderer.getInstance().ReRender(true, false);
		running_ = true;		
	}

	/**
	 * Method to let this thread stop delegating work to subthreads. Work in the main function is suspended, the
	 * subthreads (workers) will go to sleep and the Renderer is notified to get inactive.
	 */  
	public synchronized void stopThread(){
		if(running_) ErrorLog.log(Messages.getString("SimulationMaster.simulationStopped"), 2, SimulationMaster.class.getName(), "stopThread", null); //$NON-NLS-1$ //$NON-NLS-2$
		running_ = false;
		if ((Map.getInstance().getReadyState() == false || Scenario.getInstance().getReadyState() == false) && workers_ != null){
			//wait till all workers get to the start barrier
			while(barrierStart_.getParties() - barrierStart_.getNumberWaiting() != 1){
				try{
					sleep(1);
				} catch (Exception e){}
			}
			//now interrupt the first one. The first will exit with an InterruptedException, all other workers will exit through a BrokenBarrierException
			workers_[0].interrupt();
		
			workers_ = null;
		}		
		Renderer.getInstance().notifySimulationRunning(false);
	}
	
	/**
	 * Allows to jump to a specific time. While this mode is active, no display and statistics update 
	 * is done.
	 * 
	 * @param time	the target time in milliseconds
	 */
	public void jumpToTime(int time){
		jumpTimeMode_ = true;
		jumpTimeTarget_ = time;
		if(!Renderer.getInstance().isConsoleStart())VanetSimStart.setProgressBar(true);
		startThread();
	}
	
	/**
	 * Sets the target step time. Decrease to get a faster simulation, increase to get a slower.
	 * 
	 * @param time
	 */
	public void setTargetStepTime(int time){
		if(time > 0) targetStepTime_ = time;
	}

	/**
	 * Proceed one single step forward.
	 */
	public void doOneStep(){
		if(!running_){
			Renderer.getInstance().notifySimulationRunning(true);
			doOneStep_ = true;
		}
	}

	/**
	 * Function to set up the worker threads with their corresponding regions. Each thread gets an equal amount of regions
	 * (some might get one more because of rounding). The amount of items in a region is not used as a method to improve equality
	 * between threads as this value might change over time (moving vehicles!).
	 * This function expects to get 2x the amount of CPU cores as threads so that the negative effects of an unequal allocation
	 * (some threads finishing faster as others => unused cpu power) are reduced.
	 * 
	 * @param timePerStep	the time per step in milliseconds
	 * @param threads		the amount of threads that shall be created
	 * 
	 * @return the worker thread array
	 */
	public WorkerThread[] createWorkers(int timePerStep, int threads){
		ArrayList<WorkerThread> tmpWorkers = new ArrayList<WorkerThread>();
		WorkerThread tmpWorker = null;
		Region[][] regions = Map.getInstance().getRegions();
		ArrayList<Region> tmpRegions = new ArrayList<Region>();
		long regionCountX = Map.getInstance().getRegionCountX();
		long regionCountY = Map.getInstance().getRegionCountY();		
		double regionsPerThread = regionCountX * regionCountY / (double)threads;		
		long count = 0;
		double target = regionsPerThread;
		threads = 0;	// reset to 0, perhaps we're getting more/less because of rounding so we calculate this later!

		for(int i = 0; i < regionCountX; ++i){
			for(int j = 0; j < regionCountY; ++j){
				++count;
				tmpRegions.add(regions[i][j]);
				if(count >= Math.round(target)){					
					try{
						tmpWorker = new WorkerThread(tmpRegions.toArray(new Region[0]), timePerStep);			
						++threads;
						tmpWorkers.add(tmpWorker);
						tmpWorker.start();
					} catch (Exception e){
						ErrorLog.log(Messages.getString("SimulationMaster.errorWorkerThread"), 7, SimulationMaster.class.getName(), "createWorkers", e); //$NON-NLS-1$ //$NON-NLS-2$
					}	
					tmpRegions = new ArrayList<Region>();
					target += regionsPerThread;
				}
			}
		}
		if(tmpRegions.size() > 0){	// remaining items, normally this should never happen!
			ErrorLog.log(Messages.getString("SimulationMaster.regionsRemained"), 6, SimulationMaster.class.getName(), "createWorkers", null); //$NON-NLS-1$ //$NON-NLS-2$
			try{
				tmpWorker = new WorkerThread(tmpRegions.toArray(new Region[0]), timePerStep);			
				++threads;
				tmpWorkers.add(tmpWorker);
				tmpWorker.start();
			} catch (Exception e){
				ErrorLog.log(Messages.getString("SimulationMaster.errorAddingRemainingRegions"), 7, SimulationMaster.class.getName(), "createWorkers", e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		barrierStart_ = new CyclicBarrier(threads + 1);
		barrierDuringWork_ = new CyclicBarrier(threads);
		barrierFinish_ = new CyclicBarrier(threads + 1);
		Iterator<WorkerThread> iterator = tmpWorkers.iterator();
		while(iterator.hasNext() ) { 
			iterator.next().setBarriers(barrierStart_, barrierDuringWork_, barrierFinish_);
		}
		return tmpWorkers.toArray(new WorkerThread[0]);
	}


	/**
	 * The main method for the simulation master initializes the worker threads, manages them and 
	 * initiates the render process and statistics updates.
	 */
	public void run() {
		setName("SimulationMaster"); //$NON-NLS-1$
		int time, threads;
		long renderTime;
		Renderer renderer = Renderer.getInstance();
		CyclicBarrier barrierRender = new CyclicBarrier(2);
		renderer.setBarrierForSimulationMaster(barrierRender);
		ReportingControlPanel statsPanel = null;
		if(!Renderer.getInstance().isConsoleStart()) statsPanel = VanetSimStart.getMainControlPanel().getReportingPanel();
		long timeOld = 0;
		long timeNew = 0;
		long timeDistance = 0;
		boolean consoleStart = Renderer.getInstance().isConsoleStart();

		
		while(true){
			try{
				if(running_ || doOneStep_){
					renderTime = System.nanoTime();
					barrierRender.reset();
					
					while(workers_ == null){
						if (Map.getInstance().getReadyState() == true && Scenario.getInstance().getReadyState() == true){	// wait until map is ready
							if(Runtime.getRuntime().availableProcessors() < 2) threads = 1;	// on single processor systems or if system reports wrong (smaller 1) amount of CPUs => fallback to 1 CPU and 1 thread
							else threads = Runtime.getRuntime().availableProcessors() * 2;		// on multiprocessor systems use double the amount of threads to use ressources more efficiently
							long max_heap = Runtime.getRuntime().maxMemory()/1048576;		// Heap memory in MB
							ErrorLog.log(Messages.getString("SimulationMaster.preparingSimulation") + threads + Messages.getString("SimulationMaster.threadsDetected") + max_heap + Messages.getString("SimulationMaster.heapMemory"), 3, SimulationMaster.class.getName(), "run", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
							// Prepare multiple worker threads to gain advantage of multi-core processors
							workers_ = createWorkers(TIME_PER_STEP, threads);	
							
							if(Renderer.getInstance().isConsoleStart()){
								Renderer.getInstance().setMapZoom(0.4999999999);
								VanetSimStart.getMainControlPanel().getSimulatePanel().setZoomValue((int)Math.round(Math.log(Renderer.getInstance().getMapZoom()*1000)*50));
								ReRenderManager.getInstance().doReRender();
							}
							
							
						} else {
							sleep(50);
						}
					}					
					time = renderer.getTimePassed() + TIME_PER_STEP;

					//process events
					eventList_.processEvents(time);	

					// (re)start the working threads
					barrierStart_.await();

					// wait for all working threads to finish to prevent drawing an inconsistent state!
					barrierFinish_.await();	

					// Rendering itself can't be multithreaded and thus must be done here and not in the workers!
					KnownVehiclesList.setTimePassed(time);
					KnownRSUsList.setTimePassed(time);
					renderer.setTimePassed(time);						


					
					if(!jumpTimeMode_){
						
						renderer.ReRender(false, true);
	
						statsPanel.checkUpdates(TIME_PER_STEP);
						
						// wait until rendering has completed
						Thread.yield();
						barrierRender.await(3, TimeUnit.SECONDS);
	
						// wait so that we get near the desired frames per second (no waiting if processing power wasn't enough!)
						renderTime = ((System.nanoTime() - renderTime)/1000000);
						if(renderTime > 0) renderTime = targetStepTime_ - renderTime;
						else renderTime = targetStepTime_ + renderTime;	//nanoTime might overflow
						if(renderTime > 0 && renderTime <= targetStepTime_){
							sleep(renderTime);
						}
					} else {
						
						if(consoleStart && time%10000 == 0){
							timeNew = System.currentTimeMillis();
							timeDistance = timeNew-timeOld;
							System.out.println("Time:" + timeDistance);
							timeOld = timeNew;
							System.out.println(time);

						}
						
						if(time >= jumpTimeTarget_){
							jumpTimeTarget_ = -1;
							jumpTimeMode_ = false;
							stopThread();
							if(consoleStart){
								System.out.println("Time:" + new Date());
								System.out.println(Messages.getString("ConsoleStart.SimulationEnded"));
								//if(logPerformance_)writeAnyTextToFile("timeForCalculation:" + (System.currentTimeMillis()-startTime) + "\n*********\n", System.getProperty("user.dir") + "/performance.log", true);

								System.exit(0);
							}
							if(!consoleStart){
								VanetSimStart.setProgressBar(false);
								renderer.ReRender(false, true);	
								statsPanel.checkUpdates(TIME_PER_STEP);
							}
						}
					}
					if(doOneStep_){
						doOneStep_ = false;
						renderer.notifySimulationRunning(false);
					}
				} else {
					sleep(50);
				}
			} catch (Exception e){};
		}
	}

	/**
	 * Returns if a simulation is currently running or not.
	 * 
	 * @return <code>true</code> if a simulation is running, else <code>false</code>
	 */
	public boolean isSimulationRunning(){
		return running_;
	}

	public boolean isGuiEnabled() {
		return guiEnabled;
	}

	public void setGuiEnabled(boolean guiEnabled) {
		this.guiEnabled = guiEnabled;
	}
	
	/**
	 * Writes any data to file
	 */
	
	public static void writeAnyTextToFile(String anyText, String fileName, boolean append){
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(fileName, append));
			
			out.write(anyText);
			out.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();		
		}
	}

	public static boolean isLogPerformance_() {
		return logPerformance_;
	}

	public static long getStartTime() {
		return startTime;
	}

	public static  void setStartTime(long theStartTime) {
		startTime = theStartTime;
	}
}