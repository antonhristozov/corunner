package corunner.handlers;

public class corunnerhandlerGRB extends corunnerhandler {

	// select which solver to use - this handler will request the internal solver

	public corunnerhandlerGRB() {
		solver_to_use = USE_GUROBI_SOLVER;
	}
}
