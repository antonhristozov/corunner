package corunner.handlers;

public class corunnerhandlerINT extends corunnerhandler {

	// select which solver to use - this handler will request the internal solver

	public corunnerhandlerINT() {
		solver_to_use = USE_INBUILT_SOLVER;
	}

}
