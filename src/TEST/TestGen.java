package TEST;

import taskGeneration.ITask;
import taskGeneration.ITaskGenerator;
import taskGeneration.TaskGenerator;
import taskGeneration.UUniFastDiscardTaskSetGen;

public class TestGen {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		 UUniFastDiscardTaskSetGen genTask ;
	     ITaskGenerator gen = new TaskGenerator();
		 genTask = new UUniFastDiscardTaskSetGen(gen, 15, .15, 0,100);
		 ITask[] tasks = genTask.generate();

	}

}
