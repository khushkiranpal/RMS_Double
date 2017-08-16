package energy;

import java.text.DecimalFormat;
import java.util.ArrayList;

import org.apache.commons.math3.distribution.NormalDistribution;

import taskGeneration.ITask;

public class ParameterSetting {
	 DecimalFormat twoDecimals = new DecimalFormat("#.##");
	public void set_freq(ArrayList<ITask> taskset,double frequency)
	{
		for (ITask t: taskset)
		{
			t.setFrequency(frequency);
			t.setWcet( Double.valueOf(twoDecimals.format((double)t.getWcet()/frequency)));

			/*t.setWcet( Double.valueOf(twoDecimals.format((double)t.getWcet()/frequency))*100);
			t.setWCET_orginal(t.getWCET_orginal()*100);
			t.setPeriod(t.getPeriod()*100);
			t.setDeadline(t.getDeadline()*100);*/
		//	System.out.println("  wcet  "+t.getWcet());
		}
	}
	
	public void setBCET(ArrayList<ITask> taskset, double ratio)
	{
		for (ITask t: taskset)
		{
			t.setBCET( Double.valueOf(twoDecimals.format(t.getWcet()*ratio)));
		}
	}
	
	public void setACET(ArrayList<ITask> taskset)
	{
		double mean , variance , standardDev, acet;
	//	NormalDistribution normal = 	new NormalDistribution();
		for (ITask t: taskset)
		{
			mean = (t.getWcet()+t.getBCET())/2;
			variance = (t.getWcet()-t.getBCET())/6;
			standardDev = Math.sqrt(variance);
			NormalDistribution normal = 	new NormalDistribution(mean, standardDev);
			acet = normal.sample();
			t.setACET( Double.valueOf(twoDecimals.format(acet)));
		}
		
	}
	
	/**
	 * @param taskset
	 */
	public void setResponseTime(ArrayList<ITask> taskset)
	{
		for(ITask t:taskset)
      {
	//	System.out.println("task i "+t.getId()+" wcet  "+t.getWcet());
            double w=t.getWcet(),w1=w-1;
            while(w != w1)
            {
                w1 = w;
                w =t.getWCET_orginal();
                for(int i=0; taskset.get(i) != t; i++)
                {
                	w += (int) (Math.ceil((double) w1/taskset.get(i).getPeriod())*taskset.get(i).getWCET_orginal());
          //     	 System.out.println("task j "+taskset.get(i).getId()+"response time  "+w);
                }
            }
            if( w > t.getDeadline())
             t.setResponseTime(0);
            else
            	t.setResponseTime(w);
      //    System.out.println("response time  "+w);
        }
	/*	for (ITask t : taskset)
		{
			System.out.println("task i "+t.getId()+" wcet  "+t.getWcet()+"  response  "+t.getResponseTime());
			

		}*/
	}
	
	public void setPromotionTime(ArrayList<ITask> taskset)
	{
		for (ITask t : taskset)
		{
		    t.setSlack(t.getDeadline()-t.getResponseTime());
		}
		
		}
	}
	

