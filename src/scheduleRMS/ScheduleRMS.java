package scheduleRMS;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import energy.ParameterSetting;
import energy.SysClockFreq;
import platform.Energy;
import platform.Fault;
import platform.Processor;
import platform.ProcessorState;
import queue.ISortedJobQueue;
import queue.ISortedQueue;
import queue.SortedJobQueue;
import queue.SortedQueuePeriod;
import taskGeneration.FileTaskReaderTxt;
import taskGeneration.ITask;
import taskGeneration.IdleSlot;
import taskGeneration.Job;
import taskGeneration.SystemMetric;

public class ScheduleRMS {
	 public static final   double  CRITICAL_TIME= 1500;
	private double freq=1;
	
	/**
	 * @throws IOException
	 */
	/**
	 * @throws IOException
	 */
	public void schedule() throws IOException
	{
	String inputfilename= "IMPLICIT_TOT_SETS_100_MAX_P_100_PROC_1_13_08_2017_23_08";
    FileTaskReaderTxt reader = new FileTaskReaderTxt("D:/CODING/TASKSETS/uunifast/"+inputfilename+".txt"); // read taskset from file
    DateFormat dateFormat = new SimpleDateFormat("dd_MM_yyyy_HH_mm");
    Calendar cal = Calendar.getInstance();
    String date = dateFormat.format(cal.getTime());
  String filename= "D:/CODING/TEST/RMS/primary"+"_"+inputfilename+"_"+date+".txt";
    String filename1= "D:/CODING/TEST/RMS/spare"+"_"+inputfilename+"_"+date+".txt";
    String filename2= "D:/CODING/TEST/RMS/energy"+"_"+inputfilename+"_"+date+".txt";
    
     Writer writer = new FileWriter(filename);
     Writer writer1 = new FileWriter(filename1);
    Writer writer2 = new FileWriter(filename2);
    
    DecimalFormat twoDecimals = new DecimalFormat("#.##");  // upto 1 decimal points

    Energy energyConsumed = new Energy();
    Job[] current= new Job[2];  // FOR SAVING THE NEWLY INTIAlIZED JOB  FROM JOBQUEUE SO THAT IT 
	// IS VISIBLE OUTSIDE THE BLOCK
    
  ITask task;
    ITask[] set = null;
    double U_SUM;
    // final   long  CRITICAL_TIME= 4;
      
    // IDLE SLOTS QUEUE
    IdleSlot slot = new IdleSlot(); // idle slot
    List <IdleSlot> slots = new ArrayList<IdleSlot>();
    int total_no_tasksets=1;
     writer2.write("TASKSET UTILIZATION SYS_FREQ FREQ PRIMARY_ENERGY SPARE_ENERGY\n");
 
    SysClockFreq frequency = new SysClockFreq();
    
    while ((set = reader.nextTaskset()) != null)
    {
    	boolean primaryBusy=false;
    	boolean spareBusy= true;
    	boolean deadlineMissed = false;
    	Job lastExecutedJob= null;
        ProcessorState proc_state = null;
        
    	  int id = 0;  // idle slot id 
    	 long time=0 ;
    	     long spareIdleTime = 0,timeToNextPromotion=0, spareActiveTime = 0;
			long timeToNextArrival=0;
    	     long endTime = 0; // endtime of job
			long spareEndTime=0;
    	     long idle = 0;  // idle time counter for processor idle slots
    	     SchedulabilityCheck schedule = new SchedulabilityCheck();
    	
    	 Processor primary = new Processor();
    	 Processor spare = new Processor();
    	 
    	 spare.setBusy(false);
			spareBusy=false;
			spare.setProc_state(proc_state.SLEEP);
			
			primary.setBusy(false);
			primary.setProc_state(proc_state.SLEEP);
    	/*//LIST OF FREE PROCESSORS
			Comparator<Processor> comparator = new Comparator<Processor>() {
		    	 public int compare(Processor p1, Processor p2) {
					int cmp =  (int) (p1.getId()-p2.getId());
					return cmp;
				}
			  };
			
			  PriorityQueue<Processor> freeProcList = new PriorityQueue<Processor> (comparator); //LIST OF FREE PROCESSORS

    	ArrayList<Processor> no_of_proc = new ArrayList<Processor>(); //total processor list
			for(int i = 1;i<=2;i++)  // m is number of processors
			 {
				 Processor p = new Processor(i,false); // i gives the processor id value , false means processor is free
				 freeProcList.add(p);
				 no_of_proc.add(p);
			 }*/
    	
    	ISortedQueue queue = new SortedQueuePeriod ();
    	queue.addTasks(set);
    	ArrayList<ITask> taskset = new ArrayList<ITask>();
    	ArrayList<Job> completedJobs = new ArrayList<Job>();
    	taskset = queue.getSortedSet();
    	U_SUM= (SystemMetric.utilisation(taskset));
    	   //	total_no_tasks=total_no_tasks+ tasks.size();
    	prioritize(taskset);
    	
    	ParameterSetting ps = new ParameterSetting();
    	double set_fq = frequency.SysClockF(taskset), fq = 0;
    /*	if (set_fq>0 && set_fq<=0.5)
    		fq=0.50;
    	else if(set_fq>0.5 && set_fq<=.75)
    		fq=0.75;
    	else if (set_fq>0.75)
    		fq=1.0;*/
    	fq=set_fq;
    	System.out.println("frequency   " +fq);
    	ps.set_freq(taskset,Double.valueOf(twoDecimals.format(fq)));
    	boolean schedulability = schedule.worstCaseResp_TDA_RMS(taskset, fq);
    	  System.out.println(schedulability);
   
    	while(!schedulability)
       {
    	
    	
         fq=fq+0.01;

  	   System.out.println("frequency   " +fq);
     	ps.set_freq(taskset,Double.valueOf(twoDecimals.format(fq)));
        
    	   schedulability = schedule.worstCaseResp_TDA_RMS(taskset, fq);   
    	   System.out.println(schedulability);
        
       }
    	   ps.setResponseTime(taskset);    
    	ps.setPromotionTime(taskset);       //SET PROMOTION TIMES
    	
    	ArrayList<Integer> fault = new ArrayList<Integer>();
		Fault f = new Fault();
	//	fault = f.lamda_0(10000000);
    	
    	
    	long temp=0;
		ISortedJobQueue activeJobQ = new SortedJobQueue(); // dynamic jobqueue 
		TreeSet<Job> spareQueue = new TreeSet<Job>(new Comparator<Job>() {
	          @Override
	          public int compare(Job t1, Job t2) {
	                         
	              if( t1.getPromotionTime()!= t2.getPromotionTime())
	                  return (int)( t1.getPromotionTime()- t2.getPromotionTime());
	              
	              return (int) (t1.getPeriod() - t2.getPeriod());
	          }
	      }); 
		
		 long hyper = SystemMetric.hyperPeriod(taskset);  /////////////// HYPER PERIOD////////////
	    	System.out.println(" hyper  "+hyper);  

	       // if(hyper>100000000)
	        	hyper = 100000000;
			fault = f.lamda_F(hyper/1000, 0.42, fq, 2);        //////////////FAULT////////////
		
			
			
			Job j,  spareJob = null; //job
		TreeSet<Long> activationTimes = new TreeSet<Long>();
		long nextActivationTime=0;
		long executedTime=0;
    	// ACTIVATE ALL TASKS AT TIME 0 INITIALLY IN QUEUE  
		for(ITask t : taskset)  // activate all tasks at time 0
		{
					temp=0;
					j =  t.activateRMS_energy(time);
					j.setPriority(t.getPriority());
					spareJob = j.cloneJob();
					spareJob.setCompletionSuccess(false);
					activeJobQ.addJob(j);
					spareQueue.add(spareJob);
				//	System.out.println("time   "+time+"out  activeJobQ.first().getActivationDate()  "+activeJobQ.first().getActivationDate());
				    
					while (temp<=hyper)
					{
						
						temp+=t.getPeriod();
						activationTimes.add(temp);
					}
						
		}
		
	/*	Iterator itr = activationTimes.iterator();
		while(itr.hasNext())
			System.out.println("activationTimes   "+itr.next());
	  	*/
        writer.write("\n\nSCHEDULE\nTASK ID  JOBID  ARRIVAL  WCET DEADLINE  isPreempted STARTTIME ENDTIME  \n");
       writer1.write("\n\nSCHEDULE\nTASK ID  JOBID  ARRIVAL  WCET DEADLINE  isPreempted STARTTIME ENDTIME  \n");

        nextActivationTime=  activationTimes.pollFirst();
  //  System.out.println("nextActivationTime  "+nextActivationTime);
    	
       

        while(time<hyper)
    	{
    //		System.out.println("hyper  "+hyper+"  time  "+Double.valueOf(twoDecimals.format(time)));
    		
    		
    		if (!spareQueue.isEmpty() && spareBusy==false  )
    		{
    //			System.out.println("   time   "+time+"  spareBusy   "+spareBusy+ "  task  id  "+spareQueue.first().getTaskId());
    			if( time >= spareQueue.first().getPromotionTime() )
    			{
    				
    	//			System.out.println("  promotion time "+spareQueue.first().getPromotionTime()+"  completion  "+spareQueue.first().isCompletionSuccess() );
    				
    				spareJob= 	spareQueue.pollFirst();
    			//	System.out.println("spare job  "+ spareJob.getTaskId());
    				if (spareJob.isCompletionSuccess()==false)
    				{
    					
    					
    					spareBusy=true;
    					spare.setBusy(true);
    					spare.setProc_state(ProcessorState.ACTIVE);
    					 writer1.write(spareJob.getTaskId()+"\t  "+spareJob.getJobId()+"\t"+spareJob.getActivationDate()+
	               			  "\t"+spareJob.getRomainingTimeCost()+"\t"+spareJob.getAbsoluteDeadline()+"\t"+spareJob.isPreempted+"\t\t"+time+"\t");
	          			
    	  //  			System.out.println(" time  "+time+"  spareBusy   "+spareBusy+"  promotion time "+spareJob.getPromotionTime());
    					//spare.setActiveTime(spareActiveTime++);
    				spareEndTime = (long)time + spareJob.getRomainingTimeCost();
    				
    			/*System.out.println("  time "+time+"  job id "+spareJob.getJobId()+"  task id  "+spareJob.getTaskId()+
    					"  job completed  "+spareJob.isCompletionSuccess()+"   spareEndTime  "+spareEndTime);
    			*/}
    				}
    		}
    		
    		if ( (long)time == (long)spareEndTime && (time>0) )
    			{
    			
    			 writer1.write(time+"    endtime\n");
    	//		System.out.println("time   "+time  +" spare queue   "+spareQueue.size());
    	//		+"  task  "+spareQueue.first().getTaskId()+   					"  job  "+spareQueue.first().getJobId());
    			if(spareQueue.size() > 0)
    			{
    				timeToNextPromotion = spareQueue.first().getPromotionTime()- (long)time;
    		//	System.out.println("timeToNextPromotion   "+timeToNextPromotion+" time "+time+"  spareQueue.first().getPromotionTime()   "+spareQueue.first().getPromotionTime());
    			if( (timeToNextPromotion<=CRITICAL_TIME)&& !spareQueue.first().isCompletionSuccess())
    				spare.setProc_state(proc_state.IDLE);
    			else
    				spare.setProc_state(proc_state.SLEEP);
    			}
    			spare.setBusy(false);
    			spareBusy=false;
    			}
    		
    		if (spareBusy && spare.getProc_state()==ProcessorState.ACTIVE)
    		{
    			spare.activeTime++;
		//		System.out.println( "time   "+time+" active   "+spare.getActiveTime()+ " proc state  "+spare.getProc_state());
				
    		}
    		else if (!spareBusy && spare.getProc_state()==ProcessorState.SLEEP)
    		{
    			spare.sleepTime++;
    		}
    		else if (spare.getProc_state()==ProcessorState.IDLE)
    			spare.idleTime++;
    		
    		
    		if( (long)time== (long)nextActivationTime) // AFTER 0 TIME JOB ACTIVAIONS
			{
	
    			if (!activationTimes.isEmpty())
    			nextActivationTime=  activationTimes.pollFirst();
    		/*	else
    				break;*/
   		//    System.out.println("nextActivationTime  "+nextActivationTime);

    			for (ITask t : taskset) 
				{
					
					Job n = null;
					long activationTime;
					activationTime = t.getNextActivation(time-1);  //GET ACTIVATION TIME
					//System.out.println("  activationTime  "+activationTime);
					long temp1= (long) activationTime, temp2 =(long) time;
					if (temp1==temp2)
						n= t.activateRMS_energy(time);
					
					if (n!=null)
					{
						activeJobQ.addJob(n);  // add NEW job to queue
						// System.out.println("time   "+time+"  task  "+activeJobQ.first().getTaskId()+
				//				"  out  activeJobQ.first().getActivationDate()  "+activeJobQ.first().getActivationDate()
					//			+"  period  "+activeJobQ.first().getPeriod());
			    		// System.out.println(" size  "+activeJobQ.size());
 
						spareJob = n.cloneJob();
						spareJob.setCompletionSuccess(false);
						spareQueue.add(spareJob);	
						// System.out.println("spare size  "+spareQueue.size());
						 
						
						// System.out.println("spareJob  p time"+spareJob.getPromotionTime());
						
						
					}
				}
				
			} 
    		
    //		System.out.println("time   "+time+"out  activeJobQ.first().getActivationDate()  "+activeJobQ.first().getActivationDate());
    		//////////////////PREEMPTION
    		if(time>0 && !activeJobQ.isEmpty() && time==activeJobQ.first().getActivationDate() && current[0]!=null )
    		{
        		// System.out.println("activeJobQ.first().getActivationDate()  "+activeJobQ.first().getActivationDate());

    			if (activeJobQ.first().getPeriod()<current[0].getPeriod())
    			{
        			// System.out.println("preemption  ");

    				primaryBusy=false;
    			  writer.write("\t"+time+"\t preempted\n");
    				executedTime = time - current[0].getStartTime();
    				// System.out.println("time   "+time+"  executedTime  "+executedTime);


    				current[0].setRemainingTime(current[0].getRemainingTime()-executedTime);
    				if (current[0].getRemainingTime()>0)
    				activeJobQ.addJob(current[0]);
    				// System.out.println("preempted job  "+current[0].getTaskId()+" remaining time "+current[0].getRemainingTime()+ "   wcet "+
    		//			current[0].getRomainingTimeCost());
    			}
    		}
    		
    		
    		
    		if ((primaryBusy == false ) )// SELECT JOB FROM QUEUE ONLY if processor is free
	        	 {
	                	
	        		j = activeJobQ.pollFirst(); // get the job at the top of queue
	        		// QUEUE MAY BE EMPTY , SO CHECK IF IT IS  NOT NULL
	        		if (j!=null)      // if job in queue is null 
	        		{
	        			
	                	primary.setProc_state(proc_state.ACTIVE);
	        			
	                		
	        			
	                //	System.out.println("time   "+time+"   active   "+primary.getActiveTime());
	        			//  IDLE SLOTS RECORD
	                			if (idle!=0)
	                			{
	                			 writer.write("endtime  "+time+"\n");
	                				slot.setLength(idle);  // IF PROCESSOR IS IDLE FROM LONF TIME, RECORD LENGTH OF IDLESLOT
	                				IdleSlot cloneSlot = (IdleSlot) slot.cloneSlot(); // CLONE THE SLOT
	                				slots.add(cloneSlot); // ADD THE SLOT TO LIST OR QUEUE
	                			}
	                			//RE- INITIALIZE IDLE VARIABLE FOR IDLE SLOTS
	                			idle =0;   // if job on the queue is not null, initialize  processor idle VARIABLE to 0
	                			
	        			current[0]=j;  // TO MAKE IT VISIBLE OUTSIDE BLOCK
    			//	System.out.println("current[0]  "+current[0].getTaskId()+" start time "+(long)time);

	        			 writer.write(j.getTaskId()+"\t  "+j.getJobId()+"\t"+j.getActivationDate()+
	                		  "\t"+j.getRomainingTimeCost()+"\t"+j.getAbsoluteDeadline()+"\t"+j.isPreempted+"\t\t"+time+"\t");
	          			
	        			
	        				j.setStartTime(time);  // other wise start time is one less than current time 
        											// BCOZ START TIME IS EQUAL TO END OF LAST EXECUTED JOB
        				
	        			endTime =  (time+j.getRemainingTime());
	        		//	System.out.println("current[0]  "+current[0].getTaskId()+"   endTime  "+(long)endTime);
	        			   primaryBusy = true;   //set  processor busy
	        			   lastExecutedJob = j;    
	        		}
	        		else  // if no job in jobqueue
	        		{

		        		timeToNextArrival= nextActivationTime-lastExecutedJob.getEndTime(); 
		        	//	System.out.println("nextActivationTime  "+nextActivationTime+"  lastExecutedJob.getEndTime   "+lastExecutedJob.getEndTime());
		        	//	System.out.println("time   "+time+"timeToNextArrival   "+timeToNextArrival);
		        	
		        		if (timeToNextArrival<CRITICAL_TIME)
		        		{
	        			primary.setProc_state(proc_state.IDLE);
		        		primary.idleTime++;
		        		}
	        			else
	        			{
	        				primary.setProc_state(proc_state.SLEEP);
			        		primary.sleepTime++;
	        			}
		        			
	        			if (idle==0)  // if starting of idle slot
	        			{
	        				writer.write("\nIDLE SLOT");
	        				slot.setId(id++); // SET ID OF SLOT
	                        slot.setStartTime(time);// START TIME OF SLOT
	                        current[0] = null;
	                       writer.write("\tstart time\t"+time+"\t");
	                	}
	        			
	        			idle++; // IDLE SLOT LENGTH 
	        			
	        			slot.setEndTime(idle + slot.getStartTime()); // SET END TIME OF SLOT
	                 } //end else IDLE SLOTS
	               
	        	 }
    		
		//	System.out.println("out fault time  "+time+"  task  "+lastExecutedJob.getTaskId()+" job  "+lastExecutedJob.getJobId());

    		
    		/////////////////////////////FAULT INDUCTION
    	//	if(time == 			11000)
    	if ( fault.size()>0 )
    		{
		//	System.out.println("out fault time  "+time+"  task  "+lastExecutedJob.getTaskId()+" job  "+lastExecutedJob.getJobId());

    		if(time==fault.get(0)*1000)
    		
    			{
    				if (primary.getProc_state()==proc_state.ACTIVE )
    				{	
    			System.out.println("                       fault time  "+time+"                task  "+lastExecutedJob.getTaskId()+" job  "+lastExecutedJob.getJobId());
    				
    				lastExecutedJob.setCompletionSuccess(false);
    				}
    				fault.remove(0);
    			}
    	}
    	
    	
    			// CHECK DEADLINE MISS
    			Iterator<Job> it = activeJobQ.iterator();
				while (it.hasNext()) //CHECK FOR ALL ACTIVE JOBS
				{
					Job j1 = it.next();
					if (j1.getAbsoluteDeadline()<time) // IF TIME IS MORE THAN THE DEADLINE, ITS A MISSING DEADLINE
					{
						System.out.println("deadline missed  task id "+j1.getTaskId()+"job id " + j1.getJobId()+"  deadline time  "+j1.getAbsoluteDeadline()+"  time "+time);
						 writer.write("\ndeadline missed  task id "+j1.getTaskId()+"  deadline time  "+j1.getAbsoluteDeadline()+"  time "+time);
						deadlineMissed= true;
						
						/*	writer.close();
						System.exit(0);*/
					}
				}
    			
	        	//	System.out.println("hyper  "+hyper+"   time  "+Double.valueOf(twoDecimals.format((time)))+"  end time "+Double.valueOf(twoDecimals.format((endTime-1))));

					// IF NOW TIME IS EQUAL TO ENDTIME OF JOB
				
			//	double temp1 = Double.valueOf(twoDecimals.format(time)), temp2= Double.valueOf(twoDecimals.format(endTime-1));
		        	if ((long)time==(long)endTime-1) // if current time == endtime 
		        	{
		        //		System.out.println("                time  "+time+"  end time "+ (endTime-1));
		        		//	Job k =  executedList.get(noOfJobsExec-1);// get last executed job added to list or job at the top of executed list
		        		primaryBusy = false;  // set processor free
		        		lastExecutedJob.setEndTime(endTime);  // set endtime of job
		        		 writer.write(endTime+"    endtime\n");
		        	//	lastExecutedJob.setCompletionSuccess(true);
		        	//	completedJobs.add(lastExecutedJob);
		        		
		        		Iterator<Job> spareitr = spareQueue.iterator();
		        		while(spareitr.hasNext())
		        		{
		        			Job spar = spareitr.next();
		        			if (spar.getTaskId()==lastExecutedJob.getTaskId() && spar.getJobId()==lastExecutedJob.getJobId())
		        			{
		        				if (lastExecutedJob.isCompletionSuccess()==false)
		        					spar.setCompletionSuccess(false);
		        				else
		        				spar.setCompletionSuccess(true);
		        				break;
		        			}
		        		}
		        		
		        		
		        		
		    //     		System.out.println("hyper  "+hyper+"  time  "+time+"  busy "+busy);
		        	}
		        	
		        /*	if(activeJobQ.isEmpty())
		        	{
		        	
		        		timeToNextArrival= activationTimes.first()-lastExecutedJob.getEndTime(); 
		        		System.out.println("activationTimes.first()  "+activationTimes.first()+"  lastExecutedJob.getEndTime   "+lastExecutedJob.getEndTime());
		        		System.out.println("time   "+time+"timeToNextArrival   "+timeToNextArrival);
		        	
		        	}
		        		*/
		       if (primary.getProc_state()==proc_state.ACTIVE)
		    	   primary.activeTime++;
		        	
		        	
		        	if(!spareBusy)
		        	spareIdleTime++;
		        	
				
		    	time=time+1;
		    	if (deadlineMissed)
		    		break;
    	}
    	System.out.println("spareFreeTime   "+spareIdleTime+" active time "+spare.getActiveTime()+"  sleep "+spare.getSleepTime()+"  idle  "+spare.getIdleTime());
    	System.out.println("primary  active time "+primary.getActiveTime()+"  sleep "+primary.getSleepTime()+"  idle  "+primary.getIdleTime());
    	/*Iterator<Job> itr1 = spareQueue.iterator();
    	 while (itr1.hasNext())
    	 {
    		 
    		 j = itr1.next();
    		 System.out.println("task  "+j.getTaskId()+"  job  "+j.getJobId()+"   period   "+j.getPeriod()+"   prio   " +j.getPriority()
    		 +"  start time  "+j.getActivationDate()+"  promotion "+j.getPromotionTime());
    	 }*/
    
    	double primaryEnergy, spareEnergy;
    	primaryEnergy = energyConsumed.energyActive(primary.activeTime/1000, fq)+energyConsumed.energy_IDLE(primary.idleTime/1000)+energyConsumed.energySLEEP(primary.sleepTime/1000);
    	spareEnergy = energyConsumed.energyActive(spare.activeTime/1000, 1)+energyConsumed.energy_IDLE(spare.idleTime/1000)+energyConsumed.energySLEEP(spare.sleepTime/1000);
    	
    	System.out.println("primary  active energy"+energyConsumed.energyActive(primary.activeTime/1000, fq)+"  idle  "+energyConsumed.energy_IDLE(primary.idleTime/1000)
    	+" sleep  "+energyConsumed.energySLEEP(primary.sleepTime/1000));
    	System.out.println("spare  active energy "+energyConsumed.energyActive(spare.activeTime/1000, 1)+"  idle  "+energyConsumed.energy_IDLE(spare.idleTime/1000)
    	+" sleep  "+energyConsumed.energySLEEP(spare.sleepTime/1000));
    
    	
    	System.out.println("primaryEnergy   "+primaryEnergy +" spareEnergy  "+spareEnergy);
    
    	 writer2.write(total_no_tasksets++ + " "+Double.valueOf(twoDecimals.format(U_SUM))+" "+Double.valueOf(twoDecimals.format(set_fq))+" "
    	    +" "+ Double.valueOf(twoDecimals.format(fq))+" " +Double.valueOf(twoDecimals.format(primaryEnergy))+" "+Double.valueOf(twoDecimals.format(spareEnergy))+"\n");
    System.out.println("   tasksets  "+total_no_tasksets);
    
    }
    
     writer.close();
    writer1.close();
     writer2.close();
    System.out.println("success");
	}
	
	public static void prioritize(ArrayList<ITask> taskset)
	{
		int priority =1;
				
		for(ITask t : taskset)
		{
			t.setPriority(priority++);
			
		}
		
//		return taskset;
		
	}
	
}
	

	