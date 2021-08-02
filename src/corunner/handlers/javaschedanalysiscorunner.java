package corunner.handlers;

import java.awt.Component;
import java.awt.GridLayout;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;

//Q1: What is this?
//A1: It is a tool that performs timing analysis of software; specifically it performs schedulability analysis
//of tasks with co-runner dependent execution times. The tool can be used with Gurobi (an external linear
//programming solver) or without. Using the tool with Gurobi has the advantage that the tool is faster but it
//has the disadvantage that Gurobi needs to be installed (for academics: free academic license; for others:
//requires purchasing a license). If you have just started, it is recommended to use it without Gurobi.
//
//Q2: Using it with Gurobi, how do I compile it?
//A2: First, copy gurobi.jar to your current working directory.
//This can be achieved as follows:
// cp /home/ba/gurobi/gurobi910/linux64/lib/gurobi.jar .
//The gurobi directory may be different on your computer.
//Then, unzip the file gurobi.jar in your current working directory.
//This can be achieved as follows:
// jar xf gurobi.jar
//Then, make sure that you have the file javaschedanalysiscorunner.java in
//your current working directory. Then compile as follows:
// javac javaschedanalysiscorunner.java
//
//A2.1 On Eclipse on Windows
//Install Gurobi on windows -
//download the Gurobi Remote Services installer from the website (https://www.gurobi.com/documentation/9.1/remoteservices/windows_installation.html)
//(e.g., GurobiServer-9.1.2-win64.msi for Gurobi 9.1.2), double click to run the installer
//
//•	First, add the file gurobi.jar to the project:
//•	Select Project > Properties, select Java Build Path, and select Libraries.
//•	Add gurobi.jar as an External JAR; the file gurobi.jar is in the lib subdirectory of the Gurobi installation.
//•	Next, add the native libraries to the path for the Run Configuration:
//•	Select the Run Configuration and select the Environment tab.
//•	Set the system environment variable PATH to the absolute location of the bin subdirectory of the Gurobi installation.
//
//Q3: Using it with Gurobi, how do I run it?
//A3: First, compile it---follow the instructions in A2. Then type the following:
// java javaschedanalysiscorunner
//You can see one taskset there. If you want to run schedulability analysis,
//click the button "Options..." then click "Gurobi" in the radio button. Then click
//submit. Then click the button "Do schedulability analysis" Now, you see results.
//You can change the taskset parameters so that it describes the system you are
//interested in.
//
//Q4: Using it without Gurobi, how do I compile it?
//A4: First, make sure that you have the file javaschedanalysiscorunner.java in
//your current working directory. Then, comment out code that uses Gurobi.
//This can be achieved as follows:
// Before import gurobi.*; add //
// Go to the function solve_linearprogram_gurobi.
// Comment out this function (for example by putting // before each line
// Go to the function computereqlp
// Comment out the line
//   objv = solve_linearprogram_gurobi(initial_A, initial_b, initial_c);
//Then compile as follows:
// javac javaschedanalysiscorunner.java
//
//Q5: Using it without Gurobi, how do I run it?
//A5: First, compile it---follow the instructions in A4. Then type the following:
// java javaschedanalysiscorunner
//You can see one taskset there. If you want to run schedulability analysis,
//then click the button "Do schedulability analysis" Now, you see results.
//You can change the taskset parameters so that it describes the system you are
//interested in.
//
//Q6: If I want to use an external program to call this schedulability analysis, how to do it?
//A6: First, comment out GUI code. Specifically, comment out the following:
// comment out all the imports related to GUI (one example is import javax.swing.JFrame;)
// comment out all variables that reference GUI widgets in public class javaschedanalysiscorunner
//   (one example is static JLabel label1;)
// comment out the function createAndShowGUI()
// comment out the function main(String[] args)
//Then, look at the function setdefaulttaskset1. Copy that code to your own program.
//Then call doschedulabilitytesting(). It returns a boolean; true means schedulable,
//false means cannot guarantee schedulability.

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

// comment out if not using Gurobi
import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;


public class javaschedanalysiscorunner {
	public static final int USE_INBUILT_SOLVER = 0;
	public static final int USE_GUROBI_SOLVER = 1;

	public static int solver_to_use;
	public static class segment {
		public int taskindex;
		public int segmentindex;

		segment(int ti, int si) {
			this.taskindex = ti;
			this.segmentindex = si;
		}
	}

	public static class corunnerinfo {
		public ArrayList<segment> corset = new ArrayList<segment>();
		public double speed;
	}

	public static final int MAXNTASKS = 10;
	public static final int MAXNSEGMENTSPERTASK = 3;
	public static final int nrows_in_middlepanewithlotsoftext = 3 + (MAXNSEGMENTSPERTASK + 2) * MAXNTASKS;
	public static final int ncols_in_middlepanewithlotsoftext = 9;

	public static int ntasks;
	public static int nprocessors;
	public static ArrayList<Integer> priorities = new ArrayList<Integer>();
	public static ArrayList<Integer> proc = new ArrayList<Integer>();
	public static ArrayList<Double> periods = new ArrayList<Double>();
	public static ArrayList<Double> deadlines = new ArrayList<Double>();
	public static ArrayList<ArrayList<segment>> V = new ArrayList<ArrayList<segment>>();
	public static ArrayList<ArrayList<Double>> executiontimes = new ArrayList<ArrayList<Double>>();
	public static ArrayList<ArrayList<Double>> pd = new ArrayList<ArrayList<Double>>();
	public static ArrayList<ArrayList<ArrayList<corunnerinfo>>> CO = new ArrayList<ArrayList<ArrayList<corunnerinfo>>>();

	public static ArrayList<Double> t = new ArrayList<Double>();
	public static ArrayList<Double> newt = new ArrayList<Double>();

	public static JLabel label1;
	public static JLabel label2;
	public static JLabel label3;
	public static JPanel toppanel1;

	public static JLabel label4;
	public static JTextField textfieldnumberoftasks;
	public static JLabel label5;
	public static JTextField textfieldnumberofprocessors;
	public static JPanel toppanel2a;
	public static JLabel label6;
	public static JPanel toppanel2b;
	public static JPanel toppanel2;
	public static JPanel toppanel;

	public static JPanel panelwithlotsoftext;
	public static JPanel[][] panelholder;
	public static JComponent[][] visual_element_in_panelholder;

	public static JScrollPane scrollablepanel;
	public static JPanel middlepanel;
	public static JLabel labelemptyinbottompanel1;
	public static JPanel bottompanel1;
	public static JButton but1;
	public static JButton but2;
	public static JButton but3;
	public static JButton but4;
	public static JPanel bottompanel2;
	public static JPanel bottompanel;
	public static JPanel bigpanel;
	public static JFrame frame;

	public static double[] initial_c;
	public static double[][] initial_A;
	public static double[] initial_b;

	public static final double EPSILON = 1.0E-10;
	public static double[][] a;
	public static int m;
	public static int n;
	public static int[] basis;

//	public static final int USE_INBUILT_SOLVER = 0;
//	public static final int USE_GUROBI_SOLVER = 1;
// select which solver to use
	// public static int solver_to_use = USE_INBUILT_SOLVER;
//	public static int solver_to_use = USE_GUROBI_SOLVER;

	public static class Dialog_options extends JFrame {
		Dialog_options() {
			JRadioButton inbuiltsolverButton = new JRadioButton("Inbuilt");
			JRadioButton gurobiButton = new JRadioButton("Gurobi");
			if (solver_to_use == USE_INBUILT_SOLVER) {
				inbuiltsolverButton.setSelected(true);
				gurobiButton.setSelected(false);
			} else if (solver_to_use == USE_GUROBI_SOLVER) {
				inbuiltsolverButton.setSelected(false);
				gurobiButton.setSelected(true);
			} else {
				System.out.println("Error in constructor for Dialog_options");
				System.exit(-1);
			}
			ButtonGroup bgroup = new ButtonGroup();
			bgroup.add(inbuiltsolverButton);
			bgroup.add(gurobiButton);

			JPanel radioPanel = new JPanel();
			radioPanel.setLayout(new GridLayout(3, 1));
			radioPanel.add(inbuiltsolverButton);
			radioPanel.add(gurobiButton);

			radioPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Solver"));
			setContentPane(radioPanel);

			JButton submitButton = new JButton("Submit");
			submitButton.addActionListener(e -> {
				if (inbuiltsolverButton.isSelected()) {
					solver_to_use = USE_INBUILT_SOLVER;
				}
				if (gurobiButton.isSelected()) {
					solver_to_use = USE_GUROBI_SOLVER;
				}
				dispose();
			});
			add(submitButton);
			pack();
		}
	}

	public static Dialog_options dialog_options;

	public static void setpriorities(int taskindex, int priority_value) {
		while (priorities.size() < taskindex) {
			priorities.add(0);
		}
		priorities.set(taskindex - 1, priority_value);
	}

	public static void setproc(int taskindex, int proc_value) {
		while (proc.size() < taskindex) {
			proc.add(0);
		}
		proc.set(taskindex - 1, proc_value);
	}

	public static void setperiods(int taskindex, double periods_value) {
		while (periods.size() < taskindex) {
			periods.add(0.0);
		}
		periods.set(taskindex - 1, periods_value);
	}

	public static void setdeadlines(int taskindex, double deadlines_value) {
		while (deadlines.size() < taskindex) {
			deadlines.add(0.0);
		}
		deadlines.set(taskindex - 1, deadlines_value);
	}

	public static void addsegmenttoV(int taskindex, int taskindexforsegment, int segindexforsegment) {
		while (V.size() < taskindex) {
			V.add(new ArrayList<segment>());
		}
		ArrayList<segment> l = V.get(taskindex - 1);
		segment aseg = new segment(taskindexforsegment, segindexforsegment);
		l.add(aseg);
		V.set(taskindex - 1, l);
	}

	public static void setexecutiontime(int taskindex, int segmentindex, double value_of_executiontime) {
		while (executiontimes.size() < taskindex) {
			executiontimes.add(new ArrayList<Double>());
		}
		ArrayList<Double> l = executiontimes.get(taskindex - 1);
		while (l.size() < segmentindex) {
			l.add(-1.0);
		}
		l.set(segmentindex - 1, value_of_executiontime);
		executiontimes.set(taskindex - 1, l);
	}

	public static void setpd(int taskindex, int segmentindex, double value_of_pd) {
		while (pd.size() < taskindex) {
			pd.add(new ArrayList<Double>());
		}
		ArrayList<Double> l = pd.get(taskindex - 1);
		while (l.size() < segmentindex) {
			l.add(-1.0);
		}
		l.set(segmentindex - 1, value_of_pd);
		pd.set(taskindex - 1, l);
	}

	public static corunnerinfo createcorunnerinfo(int ti, int si, double v) {
		corunnerinfo corunnerinfo_var = new corunnerinfo();
		corunnerinfo_var.corset = new ArrayList<segment>();
		corunnerinfo_var.corset.clear();
		corunnerinfo_var.corset.add(new segment(ti, si));
		corunnerinfo_var.speed = v;
		return corunnerinfo_var;
	}

	public static ArrayList<corunnerinfo> createcorunnerinfolist_with_single_element(int ti1, int si1, double v1) {
		ArrayList<corunnerinfo> corunnerinfo_var_list = new ArrayList<corunnerinfo>();
		corunnerinfo_var_list.clear();
		corunnerinfo_var_list.add(createcorunnerinfo(ti1, si1, v1));
		return corunnerinfo_var_list;
	}

	public static ArrayList<corunnerinfo> createcorunnerinfolist_with_two_elements(int ti1, int si1, double v1, int ti2,
			int si2, double v2) {
		ArrayList<corunnerinfo> corunnerinfo_var_list = new ArrayList<corunnerinfo>();
		corunnerinfo_var_list.clear();
		corunnerinfo_var_list.add(createcorunnerinfo(ti1, si1, v1));
		corunnerinfo_var_list.add(createcorunnerinfo(ti2, si2, v2));
		return corunnerinfo_var_list;
	}

	public static ArrayList<corunnerinfo> createcorunnerinfolist_with_four_elements(int ti1, int si1, double v1,
			int ti2, int si2, double v2, int ti3, int si3, double v3, int ti4, int si4, double v4) {
		ArrayList<corunnerinfo> corunnerinfo_var_list = new ArrayList<corunnerinfo>();
		corunnerinfo_var_list.clear();
		corunnerinfo_var_list.add(createcorunnerinfo(ti1, si1, v1));
		corunnerinfo_var_list.add(createcorunnerinfo(ti2, si2, v2));
		corunnerinfo_var_list.add(createcorunnerinfo(ti3, si3, v3));
		corunnerinfo_var_list.add(createcorunnerinfo(ti4, si4, v4));
		return corunnerinfo_var_list;
	}

	public static void setCO(int taskindex, int segmentindex, ArrayList<corunnerinfo> list_of_corunnerinfo) {
		while (CO.size() < taskindex) {
			CO.add(new ArrayList<ArrayList<corunnerinfo>>());
		}
		ArrayList<ArrayList<corunnerinfo>> l = CO.get(taskindex - 1);
		while (l.size() < segmentindex) {
			l.add(new ArrayList<corunnerinfo>());
		}
		l.set(segmentindex - 1, list_of_corunnerinfo);
		CO.set(taskindex - 1, l);
	}

	public static void multiply_executiontimes(double factor) {
		for (int i = 1; i <= ntasks; i++) {
			for (int v = 1; v <= executiontimes.get(i - 1).size(); v++) {
				setexecutiontime(i, v, executiontimes.get(i - 1).get(v - 1) * factor);
			}
		}
	}

	public static void multiply_deadlines(double factor) {
		for (int i = 1; i <= ntasks; i++) {
			setdeadlines(i, deadlines.get(i - 1) * factor);
		}
	}

	public static void multiply_periods(double factor) {
		for (int i = 1; i <= ntasks; i++) {
			setperiods(i, periods.get(i - 1) * factor);
		}
	}

	public static void multiply_executiontimes_periods_deadlines(double factor) {
		multiply_executiontimes(factor);
		multiply_deadlines(factor);
		multiply_periods(factor);
	}

	public static void applybothmultipliers(double execmultiplier, double allmultiplier) {
		multiply_executiontimes(execmultiplier);
		multiply_executiontimes_periods_deadlines(allmultiplier);
	}

	public static void setdefaulttaskset1(double execmultiplier, double allmultiplier) {
		ntasks = 3;
		nprocessors = 2;
		priorities.clear();
		proc.clear();
		periods.clear();
		deadlines.clear();
		V.clear();
		executiontimes.clear();
		pd.clear();
		CO.clear();
		setpriorities(1, 3);
		setpriorities(2, 1);
		setpriorities(3, 2);
		setproc(1, 2);
		setproc(2, 2);
		setproc(3, 1);
		setperiods(1, 508.827494);
		setperiods(2, 2873.485391);
		setperiods(3, 231.911587);
		setdeadlines(1, 51.58222);
		setdeadlines(2, 1414.746314);
		setdeadlines(3, 123.257014);
		addsegmenttoV(1, 1, 1);
		addsegmenttoV(2, 2, 1);
		addsegmenttoV(3, 3, 1);
		setexecutiontime(1, 1, 0.508827);
		setexecutiontime(2, 1, 4.000429);
		setexecutiontime(3, 1, 4.074487);
		setpd(1, 1, 0.05);
		setpd(2, 1, 0.05);
		setpd(3, 1, 0.05);
		ArrayList<corunnerinfo> CO11 = createcorunnerinfolist_with_single_element(3, 1, 0.727217);
		ArrayList<corunnerinfo> CO21 = createcorunnerinfolist_with_single_element(3, 1, 0.616589);
		ArrayList<corunnerinfo> CO31 = createcorunnerinfolist_with_two_elements(1, 1, 0.965866, 2, 1, 0.915646);
		setCO(1, 1, CO11);
		setCO(2, 1, CO21);
		setCO(3, 1, CO31);
		applybothmultipliers(execmultiplier, allmultiplier);
	}

	public static void setdefaulttaskset2(double execmultiplier, double allmultiplier) {
		ntasks = 5;
		nprocessors = 2;
		priorities.clear();
		proc.clear();
		periods.clear();
		deadlines.clear();
		V.clear();
		executiontimes.clear();
		pd.clear();
		CO.clear();
		setpriorities(1, 3);
		setpriorities(2, 2);
		setpriorities(3, 3);
		setpriorities(4, 2);
		setpriorities(5, 1);
		setproc(1, 1);
		setproc(2, 1);
		setproc(3, 2);
		setproc(4, 2);
		setproc(5, 2);
		setperiods(1, 1.500);
		setperiods(2, 2.000);
		setperiods(3, 2.000);
		setperiods(4, 2.000);
		setperiods(5, 2.250);
		setdeadlines(1, 1.500);
		setdeadlines(2, 2.000);
		setdeadlines(3, 2.000);
		setdeadlines(4, 2.000);
		setdeadlines(5, 2.250);
		addsegmenttoV(1, 1, 1);
		addsegmenttoV(2, 2, 1);
		addsegmenttoV(3, 3, 1);
		addsegmenttoV(4, 4, 1);
		addsegmenttoV(5, 5, 1);
		addsegmenttoV(5, 5, 2);
		setexecutiontime(1, 1, 0.250);
		setexecutiontime(2, 1, 0.250);
		setexecutiontime(3, 1, 0.250);
		setexecutiontime(4, 1, 0.250);
		setexecutiontime(5, 1, 0.500);
		setexecutiontime(5, 2, 0.125);

		setpd(1, 1, 0.500);
		setpd(2, 1, 0.500);
		setpd(3, 1, 0.500);
		setpd(4, 1, 0.500);
		setpd(5, 1, 1.000);
		setpd(5, 2, 0.500);
		ArrayList<corunnerinfo> CO11 = createcorunnerinfolist_with_four_elements(3, 1, 1.000, 4, 1, 0.500, 5, 1, 1.000,
				5, 2, 1.000);
		ArrayList<corunnerinfo> CO21 = createcorunnerinfolist_with_four_elements(3, 1, 0.500, 4, 1, 1.000, 5, 1, 1.000,
				5, 2, 1.000);
		ArrayList<corunnerinfo> CO31 = createcorunnerinfolist_with_two_elements(1, 1, 1.000, 2, 1, 0.500);
		ArrayList<corunnerinfo> CO41 = createcorunnerinfolist_with_two_elements(1, 1, 0.500, 2, 1, 1.000);
		ArrayList<corunnerinfo> CO51 = createcorunnerinfolist_with_two_elements(1, 1, 1.000, 2, 1, 1.000);
		ArrayList<corunnerinfo> CO52 = createcorunnerinfolist_with_two_elements(1, 1, 0.500, 2, 1, 1.000);
		setCO(1, 1, CO11);
		setCO(2, 1, CO21);
		setCO(3, 1, CO31);
		setCO(4, 1, CO41);
		setCO(5, 1, CO51);
		setCO(5, 2, CO52);
		applybothmultipliers(execmultiplier, allmultiplier);
	}

	public static int gettaskindexfromrow(int row) {
		int localrow = row - 3;
		int taskindex = (localrow / (MAXNSEGMENTSPERTASK + 2)) + 1;
		return taskindex;
	}

	public static int getsegindexfromrow(int row) {
		int localrow = row - 3;
		int segindex = (localrow % (MAXNSEGMENTSPERTASK + 2));
		return segindex;
	}

	public static int get_row_given_task(int taskindex) {
		int localrow;
		int row;
		localrow = (taskindex - 1) * (MAXNSEGMENTSPERTASK + 2);
		row = localrow + 3;
		return row;
	}

	public static int get_row_for_period_given_task(int taskindex) {
		return get_row_given_task(taskindex);
	}

	public static int get_col_for_period_given_task(int taskindex) {
		return 1;
	}

	public static int get_row_for_deadline_given_task(int taskindex) {
		return get_row_given_task(taskindex);
	}

	public static int get_col_for_deadline_given_task(int taskindex) {
		return 2;
	}

	public static int get_row_for_nsegments_given_task(int taskindex) {
		return get_row_given_task(taskindex);
	}

	public static int get_col_for_nsegments_given_task(int taskindex) {
		return 3;
	}

	public static int get_row_for_priority_given_task(int taskindex) {
		return get_row_given_task(taskindex);
	}

	public static int get_col_for_priority_given_task(int taskindex) {
		return 4;
	}

	public static int get_row_for_proc_given_task(int taskindex) {
		return get_row_given_task(taskindex);
	}

	public static int get_col_for_proc_given_task(int taskindex) {
		return 5;
	}

	public static int get_row_for_executiontimes_given_task_and_seg(int taskindex, int segmentindex) {
		return get_row_given_task(taskindex) + segmentindex;
	}

	public static int get_col_for_executiontimes_given_task_and_seg(int taskindex, int segmentindex) {
		return 6;
	}

	public static int get_row_for_pd_given_task_and_seg(int taskindex, int segmentindex) {
		return get_row_given_task(taskindex) + segmentindex;
	}

	public static int get_col_for_pd_given_task_and_seg(int taskindex, int segmentindex) {
		return 7;
	}

	public static int get_row_for_CO_given_task_and_seg(int taskindex, int segmentindex) {
		return get_row_given_task(taskindex) + segmentindex;
	}

	public static int get_col_for_CO_given_task_and_seg(int taskindex, int segmentindex) {
		return 8;
	}

	public static String producestring_from_corunnerinfo_corset(ArrayList<segment> corset) {
		String newstr;
		newstr = "";
		newstr = newstr + "[";
		for (int iterator2 = 1; iterator2 <= corset.size(); iterator2++) {
			int tempi = corset.get(iterator2 - 1).taskindex;
			int tempv = corset.get(iterator2 - 1).segmentindex;
			newstr = newstr + "[" + Integer.toString(tempi) + "," + Integer.toString(tempv) + "]";
			if (iterator2 < corset.size()) {
				newstr = newstr + ",";
			}
		}
		newstr = newstr + "]";
		return newstr;
	}

	public static String producestring_from_corunnerinfo(corunnerinfo corinfo) {
		String newstr;
		newstr = "";
		newstr = newstr + "[";
		newstr = newstr + producestring_from_corunnerinfo_corset(corinfo.corset);
		newstr = newstr + ",";
		newstr = newstr + Double.toString(corinfo.speed);
		newstr = newstr + "]";
		return newstr;
	}

	public static String producestring_from_corunnerlist(ArrayList<corunnerinfo> COtaskseg) {
		String newstr;
		newstr = "";
		newstr = newstr + "[";
		for (int iterator1 = 1; iterator1 <= COtaskseg.size(); iterator1++) {
			newstr = newstr + producestring_from_corunnerinfo(COtaskseg.get(iterator1 - 1));
			if (iterator1 < COtaskseg.size()) {
				newstr = newstr + ",";
			}
		}
		newstr = newstr + "]";
		return newstr;
	}

	public static segment get_segment_from_string(String astr) {
		String local_astr;
		String astr2;
		String part1str;
		String part2str;
		int taskindex;
		int segindex;
		int delimiter_index;
		local_astr = astr.trim();
		if ((local_astr.charAt(0) == '[') && (local_astr.charAt(local_astr.length() - 1) == ']')) {
			astr2 = local_astr.substring(1, local_astr.length() - 1);
			delimiter_index = astr2.indexOf(",");
			part1str = astr2.substring(0, delimiter_index);
			part2str = astr2.substring(delimiter_index + 1, astr2.length());
			taskindex = Integer.parseInt(part1str);
			segindex = Integer.parseInt(part2str);
			return new segment(taskindex, segindex);
		} else {
			System.out.println("Error in get_segment_from_string. Did not see hard brackets.");
			return null;
		}
	}

	public static ArrayList<segment> getcorunnersetfromstring(String astr) {
		String local_astr;
		int delimiter_index;
		ArrayList<segment> segment_list = new ArrayList<segment>();
		segment_list.clear();
		String head_str;
		String remainder_str;
		local_astr = astr.trim();
		if ((local_astr.charAt(0) == '[') && (local_astr.charAt(local_astr.length() - 1) == ']')) {
			remainder_str = local_astr.substring(1, local_astr.length() - 1);
			delimiter_index = remainder_str.indexOf("],[");
			while (delimiter_index != -1) {
				head_str = remainder_str.substring(0, delimiter_index + 1);
				remainder_str = remainder_str.substring(delimiter_index + 2, remainder_str.length());
				segment a_segment = get_segment_from_string(head_str);
				segment_list.add(a_segment);
				delimiter_index = remainder_str.indexOf("],[");
			}
			segment a_segment = get_segment_from_string(remainder_str);
			segment_list.add(a_segment);
			return segment_list;
		} else {
			System.out.println("Error in getcorunnersetfromstring. Did not see hard brackets.");
			return null;
		}
	}

	public static corunnerinfo get_corunnerinfo_from_string(String astr) {
		String local_astr;
		String astr2;
		int separatorindex;
		corunnerinfo my_corunnerinfo = new corunnerinfo();
		local_astr = astr.trim();
		if ((local_astr.charAt(0) == '[') && (local_astr.charAt(local_astr.length() - 1) == ']')) {
			astr2 = local_astr.substring(1, local_astr.length() - 1);
			separatorindex = astr2.lastIndexOf(',');
			String corunnerstr = astr2.substring(0, separatorindex);
			String speedstr = astr2.substring(separatorindex + 1, astr2.length());
			my_corunnerinfo.corset = getcorunnersetfromstring(corunnerstr);
			my_corunnerinfo.speed = Double.parseDouble(speedstr);
			return my_corunnerinfo;
		} else {
			System.out.println("Error in get_corunnerinfo_from_string. Did not see hard brackets.");
			return null;
		}
	}

	public static ArrayList<corunnerinfo> get_corunnerlist_from_string(String astr) {
		int delimiter_index;
		ArrayList<corunnerinfo> corunnerinfo_list = new ArrayList<corunnerinfo>();
		corunnerinfo_list.clear();
		String local_astr;
		String head_str;
		String remainder_str;
		local_astr = astr.trim();
		if ((local_astr.charAt(0) == '[') && (local_astr.charAt(local_astr.length() - 1) == ']')) {
			remainder_str = local_astr.substring(1, local_astr.length() - 1);
			remainder_str = remainder_str.trim();
			delimiter_index = remainder_str.indexOf("],[");
			while (delimiter_index != -1) {
				head_str = remainder_str.substring(0, delimiter_index + 1);
				remainder_str = remainder_str.substring(delimiter_index + 2, remainder_str.length());
				corunnerinfo a_corunnerinfo = get_corunnerinfo_from_string(head_str);
				corunnerinfo_list.add(a_corunnerinfo);
				delimiter_index = remainder_str.indexOf("],[");
			}
			corunnerinfo a_corunnerinfo = get_corunnerinfo_from_string(remainder_str);
			corunnerinfo_list.add(a_corunnerinfo);
			return corunnerinfo_list;
		} else {
			System.out.println("Error in get_corunnerlist_from_string. Did not see hard brackets.");
			return null;
		}
	}

	public static void clearallTextFields() {
		for (int row = 0; row < nrows_in_middlepanewithlotsoftext; row++) {
			for (int col = 0; col < ncols_in_middlepanewithlotsoftext; col++) {
				if (visual_element_in_panelholder[row][col] instanceof JTextField) {
					((JTextField) visual_element_in_panelholder[row][col]).setText(" ");
				}
			}
		}
	}

	public static void setGUIcomponentsbasedontaskset() {
		int row;
		int col;
		textfieldnumberoftasks.setText(Integer.toString(ntasks));
		textfieldnumberofprocessors.setText(Integer.toString(nprocessors));
		clearallTextFields();
		for (int i = 1; i <= ntasks; i++) {
			row = get_row_for_period_given_task(i);
			col = get_col_for_period_given_task(i);
			((JTextField) visual_element_in_panelholder[row][col]).setText(Double.toString(periods.get(i - 1)));
		}
		for (int i = 1; i <= ntasks; i++) {
			row = get_row_for_deadline_given_task(i);
			col = get_col_for_deadline_given_task(i);
			((JTextField) visual_element_in_panelholder[row][col]).setText(Double.toString(deadlines.get(i - 1)));
		}
		for (int i = 1; i <= ntasks; i++) {
			row = get_row_for_nsegments_given_task(i);
			col = get_col_for_nsegments_given_task(i);
			((JTextField) visual_element_in_panelholder[row][col]).setText(Integer.toString(V.get(i - 1).size()));
		}
		for (int i = 1; i <= ntasks; i++) {
			row = get_row_for_priority_given_task(i);
			col = get_col_for_priority_given_task(i);
			((JTextField) visual_element_in_panelholder[row][col]).setText(Integer.toString(priorities.get(i - 1)));
		}
		for (int i = 1; i <= ntasks; i++) {
			row = get_row_for_proc_given_task(i);
			col = get_col_for_proc_given_task(i);
			((JTextField) visual_element_in_panelholder[row][col]).setText(Integer.toString(proc.get(i - 1)));
		}

		for (int i = 1; i <= ntasks; i++) {
			for (int v = 1; v <= V.get(i - 1).size(); v++) {
				row = get_row_for_executiontimes_given_task_and_seg(i, v);
				col = get_col_for_executiontimes_given_task_and_seg(i, v);
				((JTextField) visual_element_in_panelholder[row][col])
						.setText(Double.toString(executiontimes.get(i - 1).get(v - 1)));
			}
		}
		for (int i = 1; i <= ntasks; i++) {
			for (int v = 1; v <= V.get(i - 1).size(); v++) {
				row = get_row_for_pd_given_task_and_seg(i, v);
				col = get_col_for_pd_given_task_and_seg(i, v);
				((JTextField) visual_element_in_panelholder[row][col])
						.setText(Double.toString(pd.get(i - 1).get(v - 1)));
			}
		}
		for (int i = 1; i <= ntasks; i++) {
			for (int v = 1; v <= V.get(i - 1).size(); v++) {
				row = get_row_for_CO_given_task_and_seg(i, v);
				col = get_col_for_CO_given_task_and_seg(i, v);
				((JTextField) visual_element_in_panelholder[row][col])
						.setText(producestring_from_corunnerlist(CO.get(i - 1).get(v - 1)));
			}
		}
	}

	public static void setGUIcomponentstodefaulttaskset(double execmultiplier, double allmultiplier) {
		setdefaulttaskset2(execmultiplier, allmultiplier);
		setGUIcomponentsbasedontaskset();
	}

	public static void gettasksetfromGUIcomponent() {
		int row;
		int col;
		ntasks = Integer.parseInt(textfieldnumberoftasks.getText());
		nprocessors = Integer.parseInt(textfieldnumberofprocessors.getText());
		priorities.clear();
		proc.clear();
		periods.clear();
		deadlines.clear();
		V.clear();
		executiontimes.clear();
		pd.clear();
		CO.clear();
		for (int i = 1; i <= ntasks; i++) {
			row = get_row_for_period_given_task(i);
			col = get_col_for_period_given_task(i);
			setperiods(i, Double.parseDouble(((JTextField) visual_element_in_panelholder[row][col]).getText()));
		}
		for (int i = 1; i <= ntasks; i++) {
			row = get_row_for_deadline_given_task(i);
			col = get_col_for_deadline_given_task(i);
			setdeadlines(i, Double.parseDouble(((JTextField) visual_element_in_panelholder[row][col]).getText()));
		}
		for (int i = 1; i <= ntasks; i++) {
			row = get_row_for_nsegments_given_task(i);
			col = get_col_for_nsegments_given_task(i);
			for (int v = 1; v <= Integer
					.parseInt(((JTextField) visual_element_in_panelholder[row][col]).getText()); v++) {
				addsegmenttoV(i, i, v);
			}
		}
		for (int i = 1; i <= ntasks; i++) {
			row = get_row_for_priority_given_task(i);
			col = get_col_for_priority_given_task(i);
			setpriorities(i, Integer.parseInt(((JTextField) visual_element_in_panelholder[row][col]).getText()));
		}
		for (int i = 1; i <= ntasks; i++) {
			row = get_row_for_proc_given_task(i);
			col = get_col_for_proc_given_task(i);
			setproc(i, Integer.parseInt(((JTextField) visual_element_in_panelholder[row][col]).getText()));
		}
		for (int i = 1; i <= ntasks; i++) {
			for (int v = 1; v <= V.get(i - 1).size(); v++) {
				row = get_row_for_executiontimes_given_task_and_seg(i, v);
				col = get_col_for_executiontimes_given_task_and_seg(i, v);
				setexecutiontime(i, v,
						Double.parseDouble(((JTextField) visual_element_in_panelholder[row][col]).getText()));
			}
		}
		for (int i = 1; i <= ntasks; i++) {
			for (int v = 1; v <= V.get(i - 1).size(); v++) {
				row = get_row_for_pd_given_task_and_seg(i, v);
				col = get_col_for_pd_given_task_and_seg(i, v);
				setpd(i, v, Double.parseDouble(((JTextField) visual_element_in_panelholder[row][col]).getText()));
			}
		}
		for (int i = 1; i <= ntasks; i++) {
			for (int v = 1; v <= V.get(i - 1).size(); v++) {
				row = get_row_for_CO_given_task_and_seg(i, v);
				col = get_col_for_CO_given_task_and_seg(i, v);
				setCO(i, v,
						get_corunnerlist_from_string(((JTextField) visual_element_in_panelholder[row][col]).getText()));
			}
		}
	}

	public static double solve_linearprogram_inbuilt(double[][] A, double[] b, double[] c) {
		m = b.length;
		n = c.length;
		for (int i = 0; i < m; i++) {
			if (!(b[i] >= 0)) {
				throw new IllegalArgumentException("RHS must be nonnegative");
			}
		}
		a = new double[m + 1][n + m + 1];
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				a[i][j] = A[i][j];
			}
		}
		for (int i = 0; i < m; i++) {
			a[i][n + i] = 1.0;
		}
		for (int j = 0; j < n; j++) {
			a[m][j] = c[j];
		}
		for (int i = 0; i < m; i++) {
			a[i][m + n] = b[i];
		}
		basis = new int[m];
		for (int i = 0; i < m; i++) {
			basis[i] = n + i;
		}
		solve();
		assert check(A, b, c);
		return get_value_of_objective_function();
	}

	public static void solve() {
		int q;
		q = bland();
		while (q != -1) {
			int p = minRatioRule(q);
			if (p == -1) {
				throw new ArithmeticException("Linear program is unbounded");
			}
			pivot(p, q);
			basis[p] = q;
			q = bland();
		}
	}

	public static int bland() {
		for (int j = 0; j < m + n; j++) {
			if (a[m][j] > 0) {
				return j;
			}
		}
		return -1;
	}

	public static int minRatioRule(int q) {
		int p = -1;
		for (int i = 0; i < m; i++) {
			if (a[i][q] > EPSILON) {
				if (p == -1) {
					p = i;
				} else if ((a[i][m + n] / a[i][q]) < (a[p][m + n] / a[p][q])) {
					p = i;
				}
			}
		}
		return p;
	}

	public static void pivot(int p, int q) {
		for (int i = 0; i <= m; i++) {
			for (int j = 0; j <= m + n; j++) {
				if (i != p && j != q) {
					a[i][j] -= a[p][j] * a[i][q] / a[p][q];
				}
			}
		}
		for (int i = 0; i <= m; i++) {
			if (i != p) {
				a[i][q] = 0.0;
			}
		}
		for (int j = 0; j <= m + n; j++) {
			if (j != q) {
				a[p][j] /= a[p][q];
			}
		}
		a[p][q] = 1.0;
	}

	public static double get_value_of_objective_function() {
		return -a[m][m + n];
	}

	public static double[] primal() {
		double[] x = new double[n];
		for (int i = 0; i < m; i++) {
			if (basis[i] < n) {
				x[basis[i]] = a[i][m + n];
			}
		}
		return x;
	}

	public static double[] dual() {
		double[] y = new double[m];
		for (int i = 0; i < m; i++) {
			y[i] = -a[m][n + i];
		}
		return y;
	}

	public static boolean isPrimalFeasible(double[][] A, double[] b) {
		double[] x = primal();
		for (int j = 0; j < x.length; j++) {
			if (x[j] < 0.0) {
				return false;
			}
		}
		for (int i = 0; i < m; i++) {
			double sum = 0.0;
			for (int j = 0; j < n; j++) {
				sum += A[i][j] * x[j];
			}
			if (sum > b[i] + EPSILON) {
				return false;
			}
		}
		return true;
	}

	public static boolean isDualFeasible(double[][] A, double[] c) {
		double[] y = dual();
		for (int i = 0; i < y.length; i++) {
			if (y[i] < 0.0) {
				return false;
			}
		}
		for (int j = 0; j < n; j++) {
			double sum = 0.0;
			for (int i = 0; i < m; i++) {
				sum += A[i][j] * y[i];
			}
			if (sum < c[j] - EPSILON) {
				return false;
			}
		}
		return true;
	}

	public static boolean isOptimal(double[] b, double[] c) {
		double[] x = primal();
		double[] y = dual();
		double value = get_value_of_objective_function();

		double value1 = 0.0;
		for (int j = 0; j < x.length; j++) {
			value1 += c[j] * x[j];
		}
		double value2 = 0.0;
		for (int i = 0; i < y.length; i++) {
			value2 += y[i] * b[i];
		}
		if (Math.abs(value - value1) > EPSILON || Math.abs(value - value2) > EPSILON) {
			return false;
		}
		return true;
	}

	public static boolean check(double[][] A, double[] b, double[] c) {
		return isPrimalFeasible(A, b) && isDualFeasible(A, c) && isOptimal(b, c);
	}

	public static boolean isinhp(int iprime, int i) {
		return ((proc.get(iprime - 1) == proc.get(i - 1)) && (priorities.get(iprime - 1) > priorities.get(i - 1)));
	}

	public static ArrayList<Integer> listofhptasks(int i) {
		ArrayList<Integer> resultlist = new ArrayList<Integer>();
		resultlist.clear();
		for (int iprime = 1; iprime <= ntasks; iprime++) {
			if (isinhp(iprime, i)) {
				resultlist.add(iprime);
			}
		}
		return resultlist;
	}

	public static boolean isinhep(int iprime, int i) {
		return ((proc.get(iprime - 1) == proc.get(i - 1)) && (priorities.get(iprime - 1) >= priorities.get(i - 1)));
	}

	public static ArrayList<Integer> listofheptasks(int i) {
		ArrayList<Integer> resultlist = new ArrayList<Integer>();
		resultlist.clear();
		for (int iprime = 1; iprime <= ntasks; iprime++) {
			if (isinhep(iprime, i)) {
				resultlist.add(iprime);
			}
		}
		return resultlist;
	}

	public static boolean isintop(int iprime, int i) {
		return (proc.get(iprime - 1) != proc.get(i - 1));
	}

	public static ArrayList<Integer> listoftoptasks(int i) {
		ArrayList<Integer> resultlist = new ArrayList<Integer>();
		resultlist.clear();
		for (int iprime = 1; iprime <= ntasks; iprime++) {
			if (isintop(iprime, i)) {
				resultlist.add(iprime);
			}
		}
		return resultlist;
	}

	public static boolean segmentsetincludestask(ArrayList<segment> asegmentset, int iprime) {
		for (int myiterator = 0; myiterator < asegmentset.size(); myiterator++) {
			segment asegment = asegmentset.get(myiterator);
			if (asegment.taskindex == iprime) {
				return true;
			}
		}
		return false;
	}

	public static boolean segmentsetincludesaheptask(ArrayList<segment> asegmentset, int i) {
		for (int myiterator = 0; myiterator < listofheptasks(i).size(); myiterator++) {
			int iprime = listofheptasks(i).get(myiterator);
			if (segmentsetincludestask(asegmentset, iprime)) {
				return true;
			}
		}
		return false;
	}

	public static ArrayList<segment> getsegmentsonprocessor(int i, int p) {
		ArrayList<segment> temp = new ArrayList<segment>();
		temp.clear();
		for (int iprime = 1; iprime <= ntasks; iprime++) {
			if ((proc.get(iprime - 1) == p) && ((isinhep(iprime, i)) || (isintop(iprime, i)))) {
				for (int myiterator = 0; myiterator < V.get(iprime - 1).size(); myiterator++) {
					temp.add(V.get(iprime - 1).get(myiterator));
				}
			}
		}
		return temp;
	}

	public static ArrayList<segment> create_segmentlist_with_single_segment(segment aseg) {
		ArrayList<segment> temp = new ArrayList<segment>();
		temp.clear();
		temp.add(aseg);
		return temp;
	}

	public static ArrayList<segment> concatenate_two_segmentlists(ArrayList<segment> segmentlist1,
			ArrayList<segment> segmentlist2) {
		ArrayList<segment> resultlist = new ArrayList<segment>();
		resultlist.clear();
		for (int tempindex = 0; tempindex < segmentlist1.size(); tempindex++) {
			resultlist.add(segmentlist1.get(tempindex));
		}
		for (int tempindex = 0; tempindex < segmentlist2.size(); tempindex++) {
			resultlist.add(segmentlist2.get(tempindex));
		}
		return resultlist;
	}

	public static ArrayList<ArrayList<segment>> concatenate_two_lists_of_segmentlists(
			ArrayList<ArrayList<segment>> segmentlist1, ArrayList<ArrayList<segment>> segmentlist2) {
		ArrayList<ArrayList<segment>> resultlist = new ArrayList<ArrayList<segment>>();
		resultlist.clear();
		for (int tempindex = 0; tempindex < segmentlist1.size(); tempindex++) {
			resultlist.add(segmentlist1.get(tempindex));
		}
		for (int tempindex = 0; tempindex < segmentlist2.size(); tempindex++) {
			resultlist.add(segmentlist2.get(tempindex));
		}
		return resultlist;
	}

	public static ArrayList<ArrayList<segment>> generate_setofsegmentsets(int i, int p) {
		ArrayList<ArrayList<segment>> higherindexprocessorssegmentsetlist;
		ArrayList<segment> segmentsonprocessorlist;
		ArrayList<segment> a_segmentset_with_single_segment;
		ArrayList<segment> new_segmentset;
		ArrayList<ArrayList<segment>> resultlist = new ArrayList<ArrayList<segment>>();
		segmentsonprocessorlist = getsegmentsonprocessor(i, p);
		if (p == nprocessors) {
			for (int tempindex = 0; tempindex < segmentsonprocessorlist.size(); tempindex++) {
				a_segmentset_with_single_segment = create_segmentlist_with_single_segment(
						segmentsonprocessorlist.get(tempindex));
				resultlist.add(a_segmentset_with_single_segment);
			}
		} else {
			higherindexprocessorssegmentsetlist = generate_setofsegmentsets(i, p + 1);
			for (int tempindex = 0; tempindex < segmentsonprocessorlist.size(); tempindex++) {
				a_segmentset_with_single_segment = create_segmentlist_with_single_segment(
						segmentsonprocessorlist.get(tempindex));
				for (int tempindex2 = 0; tempindex2 < higherindexprocessorssegmentsetlist.size(); tempindex2++) {
					new_segmentset = concatenate_two_segmentlists(a_segmentset_with_single_segment,
							higherindexprocessorssegmentsetlist.get(tempindex2));
					resultlist.add(new_segmentset);
				}
			}
			for (int tempindex = 0; tempindex < segmentsonprocessorlist.size(); tempindex++) {
				a_segmentset_with_single_segment = create_segmentlist_with_single_segment(
						segmentsonprocessorlist.get(tempindex));
				resultlist.add(a_segmentset_with_single_segment);
			}
			resultlist = concatenate_two_lists_of_segmentlists(resultlist, higherindexprocessorssegmentsetlist);
		}
		return resultlist;
	}

	public static ArrayList<ArrayList<segment>> select_relevant_segmentsets(int i,
			ArrayList<ArrayList<segment>> alistof_segmentsets) {
		ArrayList<segment> asegmentset;
		ArrayList<ArrayList<segment>> resultlist = new ArrayList<ArrayList<segment>>();
		resultlist.clear();
		for (int tempindex = 0; tempindex < alistof_segmentsets.size(); tempindex++) {
			asegmentset = alistof_segmentsets.get(tempindex);
			if (segmentsetincludesaheptask(asegmentset, i)) {
				resultlist.add(asegmentset);
			}
		}
		return resultlist;
	}

	public static boolean match_segment(int taskindex, int segmentindex, segment asegment) {
		return ((asegment.taskindex == taskindex) && (asegment.segmentindex == segmentindex));
	}

	public static boolean match_segment(segment segment1, segment segment2) {
		return ((segment1.taskindex == segment2.taskindex) && (segment1.segmentindex == segment2.segmentindex));
	}

	public static boolean match_segmentset(ArrayList<segment> segmentset1, ArrayList<segment> segmentset2) {
		boolean foundflag;
		if (segmentset1.size() == segmentset2.size()) {
			for (int myiterator1 = 0; myiterator1 < segmentset1.size(); myiterator1++) {
				foundflag = false;
				for (int myiterator2 = 0; myiterator2 < segmentset2.size(); myiterator2++) {
					if (match_segment(segmentset1.get(myiterator1), segmentset2.get(myiterator2))) {
						foundflag = true;
					}
				}
				if (!foundflag) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	public static boolean segment_is_in_segmentset(int taskindex, int segindex, ArrayList<segment> asegmentset) {
		for (int myiterator = 0; myiterator < asegmentset.size(); myiterator++) {
			segment asegment = asegmentset.get(myiterator);
			if (match_segment(taskindex, segindex, asegment)) {
				return true;
			}
		}
		return false;
	}

	public static ArrayList<segment> obtaincorunnerset(int taskindex, int segindex, ArrayList<segment> asegmentset) {
		ArrayList<segment> resultlist = new ArrayList<segment>();
		resultlist.clear();
		for (int myiterator = 0; myiterator < asegmentset.size(); myiterator++) {
			segment asegment = asegmentset.get(myiterator);
			if (!match_segment(taskindex, segindex, asegment)) {
				resultlist.add(asegment);
			}
		}
		return resultlist;
	}

	public static void form_LP_instance_generate_initial_c(ArrayList<ArrayList<segment>> relevant_segmentsets) {
		initial_c = new double[relevant_segmentsets.size()];
		for (int tempindex = 0; tempindex < relevant_segmentsets.size(); tempindex++) {
			initial_c[tempindex] = 1.0;
		}
	}

	public static double getcoefficient(int taskindex, int segindex, ArrayList<segment> cor) {
		if (cor.size() == 0) {
			return 1.0;
		}
		for (int myiterator = 0; myiterator < CO.get(taskindex - 1).get(segindex - 1).size(); myiterator++) {
			corunnerinfo acorunnerinfo = CO.get(taskindex - 1).get(segindex - 1).get(myiterator);
			if (match_segmentset(acorunnerinfo.corset, cor)) {
				return acorunnerinfo.speed;
			}
		}
		return pd.get(taskindex - 1).get(segindex - 1);
	}

	public static void fill_row_in_initial_A(int iprime, int kprime, int constraint_counter,
			ArrayList<ArrayList<segment>> relevant_segmentsets) {
		for (int myiterator = 0; myiterator < relevant_segmentsets.size(); myiterator++) {
			ArrayList<segment> asegmentset = relevant_segmentsets.get(myiterator);
			if (segment_is_in_segmentset(iprime, kprime, asegmentset)) {
				initial_A[constraint_counter][myiterator] = getcoefficient(iprime, kprime,
						obtaincorunnerset(iprime, kprime, asegmentset));
			} else {
				initial_A[constraint_counter][myiterator] = 0.0;
			}
		}
	}

	public static double getminpw(int iprime, int kprime, ArrayList<ArrayList<segment>> relevant_segmentsets) {
		ArrayList<segment> asegmentset;
		double temppw;
		double minpw = -1.0;
		for (int myiterator = 0; myiterator < relevant_segmentsets.size(); myiterator++) {
			asegmentset = relevant_segmentsets.get(myiterator);
			if (segment_is_in_segmentset(iprime, kprime, asegmentset)) {
				temppw = getcoefficient(iprime, kprime, obtaincorunnerset(iprime, kprime, asegmentset));
				if (minpw == -1.0) {
					minpw = temppw;
				} else {
					if (minpw > temppw) {
						minpw = temppw;
					}
				}
			}
		}
		if (minpw == -1.0) {
			System.out.println("Error in getminpw. exist_a_minpw=false.");
			System.exit(-1);
		}
		return minpw;
	}

	public static double computeLST(int iprime, int kprime, ArrayList<ArrayList<segment>> relevant_segmentsets) {
		double mysum;
		mysum = 0.0;
		for (int kprimeprime = 1; kprimeprime < kprime; kprimeprime++) {
			mysum = mysum + executiontimes.get(iprime - 1).get(kprimeprime - 1)
					/ getminpw(iprime, kprimeprime, relevant_segmentsets);
		}
		return mysum;
	}

	public static double xUB(int iprime, int kprime, double t, ArrayList<ArrayList<segment>> relevant_segmentsets) {
		boolean flag;
		if (listofheptasks(iprime).size() == 1) {
			flag = true;
			for (int kprimeprime = 1; kprimeprime < kprime; kprimeprime++) {
				flag = flag && (getminpw(iprime, kprimeprime, relevant_segmentsets) > 0);
			}
		} else {
			flag = false;
		}
		if (flag) {
			return Math.ceil((t + computeLST(iprime, kprime, relevant_segmentsets)) / periods.get(iprime - 1))
					* executiontimes.get(iprime - 1).get(kprime - 1);
		} else {
			return Math.ceil((t + deadlines.get(iprime - 1)) / periods.get(iprime - 1))
					* executiontimes.get(iprime - 1).get(kprime - 1);
		}
	}

	public static void fill_element_in_initial_b_hep(int iprime, int kprime, int constraint_counter, double t,
			ArrayList<ArrayList<segment>> relevant_segmentsets) {
		initial_b[constraint_counter] = (Math.ceil(t / periods.get(iprime - 1)))
				* (executiontimes.get(iprime - 1).get(kprime - 1));
	}

	public static void fill_element_in_initial_b_top(int iprime, int kprime, int constraint_counter, double t,
			ArrayList<ArrayList<segment>> relevant_segmentsets) {
		initial_b[constraint_counter] = xUB(iprime, kprime, t, relevant_segmentsets);
	}

	public static int form_LP_instance_generate_initial_A_and_initial_b(int i, double t,
			ArrayList<ArrayList<segment>> relevant_segmentsets) {
		int constraint_counter;
		constraint_counter = 0;
		for (int myiterator1 = 0; myiterator1 < listofheptasks(i).size(); myiterator1++) {
			int iprime = listofheptasks(i).get(myiterator1);
			for (int myiterator2 = 0; myiterator2 < V.get(iprime - 1).size(); myiterator2++) {
				int kprime = V.get(iprime - 1).get(myiterator2).segmentindex;
				constraint_counter = constraint_counter + 1;
			}
		}
		for (int myiterator1 = 0; myiterator1 < listoftoptasks(i).size(); myiterator1++) {
			int iprime = listoftoptasks(i).get(myiterator1);
			for (int myiterator2 = 0; myiterator2 < V.get(iprime - 1).size(); myiterator2++) {
				int kprime = V.get(iprime - 1).get(myiterator2).segmentindex;
				constraint_counter = constraint_counter + 1;
			}
		}
		initial_A = new double[constraint_counter][relevant_segmentsets.size()];
		initial_b = new double[constraint_counter];
		constraint_counter = 0;
		for (int myiterator1 = 0; myiterator1 < listofheptasks(i).size(); myiterator1++) {
			int iprime = listofheptasks(i).get(myiterator1);
			for (int myiterator2 = 0; myiterator2 < V.get(iprime - 1).size(); myiterator2++) {
				int kprime = V.get(iprime - 1).get(myiterator2).segmentindex;
				fill_row_in_initial_A(iprime, kprime, constraint_counter, relevant_segmentsets);
				fill_element_in_initial_b_hep(iprime, kprime, constraint_counter, t, relevant_segmentsets);
				constraint_counter = constraint_counter + 1;
			}
		}
		for (int myiterator1 = 0; myiterator1 < listoftoptasks(i).size(); myiterator1++) {
			int iprime = listoftoptasks(i).get(myiterator1);
			for (int myiterator2 = 0; myiterator2 < V.get(iprime - 1).size(); myiterator2++) {
				int kprime = V.get(iprime - 1).get(myiterator2).segmentindex;
				fill_row_in_initial_A(iprime, kprime, constraint_counter, relevant_segmentsets);
				fill_element_in_initial_b_top(iprime, kprime, constraint_counter, t, relevant_segmentsets);
				constraint_counter = constraint_counter + 1;
			}
		}
		return constraint_counter;
	}

	// Solver using Gurobi
	//
	public static double solve_linearprogram_gurobi(double[][] A, double[] b, double[] c) {
		int optimstatus;
		double objval = -1.0;
		try {
			GRBEnv env = new GRBEnv();
			GRBModel model = new GRBModel(env);
			model.set(GRB.StringAttr.ModelName, "reqlp");
			GRBVar[] durationvariables = new GRBVar[c.length];
			for (int i = 0; i < c.length; i++) {
				durationvariables[i] = model.addVar(0, GRB.INFINITY, 1.0, GRB.CONTINUOUS, "dur_" + Integer.toString(i));
			}
			model.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);
			//
			for (int j = 0; j < b.length; j++) {
				GRBLinExpr ntot = new GRBLinExpr();
				for (int i = 0; i < c.length; i++) {
					ntot.addTerm(A[j][i], durationvariables[i]);
				}
				model.addConstr(ntot, GRB.LESS_EQUAL, b[j], "constr_" + Integer.toString(j));
			}
			//
			model.optimize();
			optimstatus = model.get(GRB.IntAttr.Status);
			if (optimstatus == GRB.Status.OPTIMAL) {
				objval = model.get(GRB.DoubleAttr.ObjVal);
			} else if (optimstatus == GRB.Status.INFEASIBLE) {
				System.out.println("Error in solve_linearprogram_gurobi. Model is infeasible");
				System.exit(-1);
			} else if (optimstatus == GRB.Status.UNBOUNDED) {
				System.out.println("Error in solve_linearprogram_gurobi. Model is unbounded");
				System.exit(-1);
			} else {
				System.out.println(
						"Error in solve_linearprogram_gurobi. Optimization was stopped with status = " + optimstatus);
				System.exit(-1);
			}
			model.dispose();
			env.dispose();
		} catch (GRBException e) {
			System.out.println(
					"Error in solve_linearprogram_gurobi. Error code: " + e.getErrorCode() + ". " + e.getMessage());
//			System.exit(-1);
		}
		return objval;
	}

	public static double computereqlp(int i, double t) {
		boolean solving_successfully;
		ArrayList<ArrayList<segment>> alistof_segmentsets;
		ArrayList<ArrayList<segment>> relevant_segmentsets;
		int n_constraints;
		double objval = -1.0;
		try {
			alistof_segmentsets = generate_setofsegmentsets(i, 1);
			relevant_segmentsets = select_relevant_segmentsets(i, alistof_segmentsets);
			form_LP_instance_generate_initial_c(relevant_segmentsets);
			n_constraints = form_LP_instance_generate_initial_A_and_initial_b(i, t, relevant_segmentsets);
			if (solver_to_use == USE_INBUILT_SOLVER) {
				System.out.println("Using Inbuilt solver");
				objval = solve_linearprogram_inbuilt(initial_A, initial_b, initial_c);
			} else if (solver_to_use == USE_GUROBI_SOLVER) {
				System.out.println("Using Gurobi solver");
				// add ; // to the following line to remove Gurobi from the build
				objval = solve_linearprogram_gurobi(initial_A, initial_b, initial_c);
				if (objval == -1.0) {
					System.out.println("Switching to built in solver ");
					solver_to_use = USE_INBUILT_SOLVER;
					System.out.println("Using Inbuilt solver");
					objval = solve_linearprogram_inbuilt(initial_A, initial_b, initial_c);
				}
			} else {
				System.out.println("Error in computereqlp. This solver is not supported.");
				System.exit(-1);
			}
		} catch (ArithmeticException e) {
			System.out.println("Error in computereqlp.");
			System.out.println(e);
			System.exit(-1);
		}
		return objval;
	}

	public static double compute_sum_of_execution_times(int i) {
		double sum;
		int v;
		sum = 0.0;
		for (v = 1; v <= executiontimes.get(i - 1).size(); v++) {
			sum = sum + executiontimes.get(i - 1).get(v - 1);
		}
		return sum;
	}

	public static void initialize_t_and_newt() {
		t.clear();
		for (int i = 1; i <= ntasks; i++) {
			t.add(0.0);
		}
		newt.clear();
		for (int i = 1; i <= ntasks; i++) {
			newt.add(0.0);
		}
	}

	public static boolean doschedulabilitytesting() {
		int i;
		boolean success;
		initialize_t_and_newt();
		success = true;
		i = 1;
		while ((i <= ntasks) && (success)) {
			t.set(i - 1, 0.0);
			newt.set(i - 1, compute_sum_of_execution_times(i));
			while ((newt.get(i - 1) > t.get(i - 1)) && (newt.get(i - 1) <= deadlines.get(i - 1))) {
				t.set(i - 1, newt.get(i - 1));
				newt.set(i - 1, computereqlp(i, t.get(i - 1)));
			}
			success = success && (newt.get(i - 1) <= t.get(i - 1)) && (newt.get(i - 1) <= deadlines.get(i - 1));
			i = i + 1;
		}
		if (success) {
			return true;
		} else {
			return false;
		}
	}

	public static void doschedulabilityanalysis() {
		boolean flag;
		Instant starttime = Instant.now();
		gettasksetfromGUIcomponent();
		flag = doschedulabilitytesting();
		if (flag) {
			String temps = "Upper bounds on the response times of task are as follows:\n";
			for (int i = 1; i <= ntasks; i++) {
				temps = temps + "  For task " + Integer.toString(i) + ": " + Double.toString(newt.get(i - 1)) + "\n";
			}
			String outputstr1 = "Taskset is schedulable";
			String outputstr2 = temps;
			Instant finishtime = Instant.now();
			long timeElapsed = Duration.between(starttime, finishtime).toMillis();
			String outputstr3 = Long.toString(timeElapsed);
			System.out.println(outputstr1 + " " + outputstr2 + " " + outputstr3);
			JOptionPane.showMessageDialog(frame, outputstr1 + "\n" + outputstr2);
		} else {
			String outputstr1 = "Cannot guarantee schedulability";
			String outputstr2 = "                                             ";
			Instant finishtime = Instant.now();
			long timeElapsed = Duration.between(starttime, finishtime).toMillis();
			String outputstr3 = Long.toString(timeElapsed);
			System.out.println(outputstr1 + " " + outputstr2 + " " + outputstr3);
			JOptionPane.showMessageDialog(frame, outputstr1 + "\n" + outputstr2);
		}
	}

	public static void genericdoschedulabilityanalysis() {
		doschedulabilityanalysis();
	}

	public static void dosetoptions() {
		dialog_options = new Dialog_options();
		dialog_options.setTitle("Options");
		dialog_options.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog_options.setLocationRelativeTo(null);
		dialog_options.setVisible(true);
	}

	public static void setoptions() {
		dosetoptions();
	}

	public static String choosefilename_load() {
		JFileChooser chooser = new JFileChooser(".");
		chooser.setSelectedFile(new File("taskset.txt"));
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Text files", "txt");
		chooser.setFileFilter(filter);
		int returnVal = chooser.showOpenDialog(frame);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			return chooser.getSelectedFile().getName();
		} else {
			return "";
		}
	}

	public static String choosefilename_save() {
		JFileChooser chooser = new JFileChooser(".");
		chooser.setSelectedFile(new File("taskset.txt"));
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Text files", "txt");
		chooser.setFileFilter(filter);
		int returnVal = chooser.showSaveDialog(frame);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			return chooser.getSelectedFile().getName();
		} else {
			return "";
		}
	}

	public static void doloadtasksetfromfile(String fn) {
		try {
			Scanner s = new Scanner(new File(fn));
			int dummy = s.nextInt();
			nprocessors = s.nextInt();
			ntasks = s.nextInt();
			System.out.println("ntasks = " + Integer.toString(ntasks));
			priorities.clear();
			proc.clear();
			periods.clear();
			deadlines.clear();
			V.clear();
			executiontimes.clear();
			pd.clear();
			CO.clear();
			for (int i = 1; i <= ntasks; i++) {
				int temp_taskindex = s.nextInt();
				if (temp_taskindex != i) {
					System.out.println("Error in doloadtasksetfromfile. FileNotFoundException.");
					System.exit(-1);
				}
				setpriorities(i, s.nextInt());
				setproc(i, s.nextInt());
				setperiods(i, s.nextDouble());
				setdeadlines(i, s.nextDouble());
				int nsegsoftask = s.nextInt();
				for (int v = 1; v <= nsegsoftask; v++) {
					int temp_segmentindex = s.nextInt();
					if (temp_segmentindex != v) {
						System.out.println("Error in doloadtasksetfromfile. FileNotFoundException.");
						System.exit(-1);
					}
					addsegmenttoV(i, i, v);
					setexecutiontime(i, v, s.nextDouble());
					setpd(i, v, s.nextDouble());
					int n_corspecifications = s.nextInt();
					ArrayList<corunnerinfo> COiv = new ArrayList<corunnerinfo>();
					COiv.clear();
					for (int corindex = 1; corindex <= n_corspecifications; corindex++) {
						corunnerinfo a_corunnerinfo = new corunnerinfo();
						a_corunnerinfo.corset.clear();
						int ns = s.nextInt();
						for (int si = 1; si <= ns; si++) {
							int temp1 = s.nextInt();
							int temp2 = s.nextInt();
							segment a_segment = new segment(temp1, temp2);
							a_corunnerinfo.corset.add(a_segment);
						}
						a_corunnerinfo.speed = s.nextDouble();
						COiv.add(a_corunnerinfo);
					}
					setCO(i, v, COiv);
				}
			}
			s.close();
		} catch (FileNotFoundException ex) {
			System.out.println("Error in doloadtasksetfromfile. FileNotFoundException.");
		}
	}

	public static void dosavetasksettofile(String fn) {
		try {
			File fout = new File(fn);
			FileOutputStream fos = new FileOutputStream(fout);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
			bw.write(Integer.toString(1));
			bw.newLine();
			bw.write(Integer.toString(nprocessors));
			bw.newLine();
			bw.write(Integer.toString(ntasks));
			bw.newLine();
			for (int i = 1; i <= ntasks; i++) {
				bw.write(Integer.toString(i));
				bw.newLine();
				bw.write(Integer.toString(priorities.get(i - 1)));
				bw.newLine();
				bw.write(Integer.toString(proc.get(i - 1)));
				bw.newLine();
				bw.write(Double.toString(periods.get(i - 1)) + " " + Double.toString(deadlines.get(i - 1)));
				bw.newLine();
				bw.write(Integer.toString(V.get(i - 1).size()));
				bw.newLine();
				for (int v = 1; v <= V.get(i - 1).size(); v++) {
					bw.write(Integer.toString(v));
					bw.newLine();
					bw.write(Double.toString(executiontimes.get(i - 1).get(v - 1)));
					bw.newLine();
					bw.write(Double.toString(pd.get(i - 1).get(v - 1)));
					bw.newLine();
					bw.write(Integer.toString(CO.get(i - 1).get(v - 1).size()));
					bw.newLine();
					for (int corindex = 1; corindex <= CO.get(i - 1).get(v - 1).size(); corindex++) {
						corunnerinfo a_corunnerinfo = CO.get(i - 1).get(v - 1).get(corindex - 1);
						bw.write(Integer.toString(a_corunnerinfo.corset.size()));
						bw.write(" ");
						for (int segindex = 1; segindex <= a_corunnerinfo.corset.size(); segindex++) {
							bw.write(Integer.toString(a_corunnerinfo.corset.get(segindex - 1).taskindex));
							bw.write(" ");
							bw.write(Integer.toString(a_corunnerinfo.corset.get(segindex - 1).segmentindex));
							bw.write(" ");
						}
						bw.write(Double.toString(a_corunnerinfo.speed));
						bw.newLine();
					}
				}
			}
			bw.close();
		} catch (FileNotFoundException ex) {
			System.out.println("Error in dosavetasksettofile. FileNotFoundException.");
		} catch (IOException ex) {
			System.out.println("Error in dosavetasksettofile. IOException.");
		}
	}

	public static void loadtasksetfromfile() {
		String fn = choosefilename_load();
		if (fn.length() >= 1) {
			doloadtasksetfromfile(fn);
			setGUIcomponentsbasedontaskset();
		} else {
			System.out.println("No file selected. Hence, no taskset is loaded.");
		}
	}

	public static void savetasksettofile() {
		String fn = choosefilename_save();
		if (fn.length() >= 1) {
			gettasksetfromGUIcomponent();
			dosavetasksettofile(fn);
		} else {
			System.out.println("No file selected. Hence, no taskset is saved.");
		}
	}

	public static void createAndShowGUI() {
		label1 = new JLabel(
				"This program implements the schedulability test in B. Andersson et al., Schedulability Analysis of Tasks with Co-Runner-Dependent Execution Times, ACM TECS, 2018.");
		label1.setAlignmentX(Component.CENTER_ALIGNMENT);
		label2 = new JLabel(" ");
		label2.setAlignmentX(Component.CENTER_ALIGNMENT);
		label3 = new JLabel(" ");
		label3.setAlignmentX(Component.CENTER_ALIGNMENT);
		toppanel1 = new JPanel();
		toppanel1.setLayout(new BoxLayout(toppanel1, BoxLayout.Y_AXIS));
		toppanel1.add(label1);
		toppanel1.add(label2);
		toppanel1.add(label3);

		label4 = new JLabel("Number of tasks");
		textfieldnumberoftasks = new JTextField(4);
		label5 = new JLabel("Number of processors");
		textfieldnumberofprocessors = new JTextField(4);
		toppanel2a = new JPanel();
		toppanel2a.add(label4);
		toppanel2a.add(textfieldnumberoftasks);
		toppanel2a.add(label5);
		toppanel2a.add(textfieldnumberofprocessors);

		label6 = new JLabel(" ");
		label6.setAlignmentX(Component.CENTER_ALIGNMENT);
		toppanel2b = new JPanel();
		toppanel2b.add(label6);

		toppanel2 = new JPanel();
		toppanel2.setLayout(new BoxLayout(toppanel2, BoxLayout.Y_AXIS));
		toppanel2.add(toppanel2a);
		toppanel2.add(toppanel2b);

		toppanel = new JPanel();
		toppanel.setLayout(new BoxLayout(toppanel, BoxLayout.Y_AXIS));
		toppanel.add(toppanel1);
		toppanel.add(toppanel2);

		panelwithlotsoftext = new JPanel();
		panelholder = new JPanel[nrows_in_middlepanewithlotsoftext][ncols_in_middlepanewithlotsoftext];
		visual_element_in_panelholder = new JComponent[nrows_in_middlepanewithlotsoftext][ncols_in_middlepanewithlotsoftext];
		panelwithlotsoftext
				.setLayout(new GridLayout(nrows_in_middlepanewithlotsoftext, ncols_in_middlepanewithlotsoftext));
		for (int row = 0; row < nrows_in_middlepanewithlotsoftext; row++) {
			for (int col = 0; col < ncols_in_middlepanewithlotsoftext; col++) {
				panelholder[row][col] = new JPanel();
				panelwithlotsoftext.add(panelholder[row][col]);
			}
		}
		for (int row = 0; row < nrows_in_middlepanewithlotsoftext; row++) {
			for (int col = 0; col < ncols_in_middlepanewithlotsoftext; col++) {
				if (row < 3) {
					if (row == 0) {
						if (col == 0) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 1) {
							visual_element_in_panelholder[row][col] = new JLabel("Minimum");
						}
						if (col == 2) {
							visual_element_in_panelholder[row][col] = new JLabel("Deadline");
						}
						if (col == 3) {
							visual_element_in_panelholder[row][col] = new JLabel("Number");
						}
						if (col == 4) {
							visual_element_in_panelholder[row][col] = new JLabel("Priority");
						}
						if (col == 5) {
							visual_element_in_panelholder[row][col] = new JLabel("Processor");
						}
						if (col == 6) {
							visual_element_in_panelholder[row][col] = new JLabel("Execution");
						}
						if (col == 7) {
							visual_element_in_panelholder[row][col] = new JLabel("Default");
						}
						if (col == 8) {
							visual_element_in_panelholder[row][col] = new JLabel("Co-runner");
						}
					}
					if (row == 1) {
						if (col == 0) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 1) {
							visual_element_in_panelholder[row][col] = new JLabel("inter-arrival");
						}
						if (col == 2) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 3) {
							visual_element_in_panelholder[row][col] = new JLabel("of");
						}
						if (col == 4) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 5) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 6) {
							visual_element_in_panelholder[row][col] = new JLabel("requirement");
						}
						if (col == 7) {
							visual_element_in_panelholder[row][col] = new JLabel("speed");
						}
						if (col == 8) {
							visual_element_in_panelholder[row][col] = new JLabel("specification");
						}
					}
					if (row == 2) {
						if (col == 0) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 1) {
							visual_element_in_panelholder[row][col] = new JLabel("time");
						}
						if (col == 2) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 3) {
							visual_element_in_panelholder[row][col] = new JLabel("segments");
						}
						if (col == 4) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 5) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 6) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 7) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 8) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
					}
				}
			}
		}
		for (int row = 0; row < nrows_in_middlepanewithlotsoftext; row++) {
			for (int col = 0; col < ncols_in_middlepanewithlotsoftext; col++) {
				if (row >= 3) {
					int taskindex = gettaskindexfromrow(row);
					int segindex = getsegindexfromrow(row);
					if (segindex == 0) {
						if (col == 0) {
							visual_element_in_panelholder[row][col] = new JLabel("Task " + Integer.toString(taskindex));
						}
						if (col == 1) {
							visual_element_in_panelholder[row][col] = new JTextField(5);
						}
						if (col == 2) {
							visual_element_in_panelholder[row][col] = new JTextField(5);
						}
						if (col == 3) {
							visual_element_in_panelholder[row][col] = new JTextField(5);
						}
						if (col == 4) {
							visual_element_in_panelholder[row][col] = new JTextField(5);
						}
						if (col == 5) {
							visual_element_in_panelholder[row][col] = new JTextField(5);
						}
						if (col == 6) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 7) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 8) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
					}
				}
			}
		}
		for (int row = 0; row < nrows_in_middlepanewithlotsoftext; row++) {
			for (int col = 0; col < ncols_in_middlepanewithlotsoftext; col++) {
				if (row >= 3) {
					int taskindex = gettaskindexfromrow(row);
					int segindex = getsegindexfromrow(row);
					if ((segindex >= 1) && (segindex <= MAXNSEGMENTSPERTASK)) {
						if (col == 0) {
							visual_element_in_panelholder[row][col] = new JLabel(
									"Segment " + Integer.toString(segindex));
						}
						if (col == 1) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 2) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 3) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 4) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 5) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 6) {
							visual_element_in_panelholder[row][col] = new JTextField(5);
						}
						if (col == 7) {
							visual_element_in_panelholder[row][col] = new JTextField(5);
						}
						if (col == 8) {
							visual_element_in_panelholder[row][col] = new JTextField(5);
						}
					}
				}
			}
		}
		for (int row = 0; row < nrows_in_middlepanewithlotsoftext; row++) {
			for (int col = 0; col < ncols_in_middlepanewithlotsoftext; col++) {
				if (row >= 3) {
					int taskindex = gettaskindexfromrow(row);
					int segindex = getsegindexfromrow(row);
					if (segindex == MAXNSEGMENTSPERTASK + 1) {
						if (col == 0) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 1) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 2) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 3) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 4) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 5) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 6) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 7) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
						if (col == 8) {
							visual_element_in_panelholder[row][col] = new JLabel(" ");
						}
					}
				}
			}
		}
		for (int row = 0; row < nrows_in_middlepanewithlotsoftext; row++) {
			for (int col = 0; col < ncols_in_middlepanewithlotsoftext; col++) {
				panelholder[row][col].add(visual_element_in_panelholder[row][col]);
			}
		}
		scrollablepanel = new JScrollPane(panelwithlotsoftext);
		scrollablepanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		middlepanel = new JPanel();
		middlepanel.setLayout(new BoxLayout(middlepanel, BoxLayout.Y_AXIS));
		middlepanel.add(scrollablepanel);

		labelemptyinbottompanel1 = new JLabel(" ");
		bottompanel1 = new JPanel();
		bottompanel1.add(labelemptyinbottompanel1);

		but1 = new JButton("Load taskset from file taskset.txt");
		but2 = new JButton("Save taskset to file taskset.txt");
		but3 = new JButton("Do schedulability analysis");
		but4 = new JButton("Options...");
		but1.addActionListener(e -> loadtasksetfromfile());
		but2.addActionListener(e -> savetasksettofile());
		but3.addActionListener(e -> genericdoschedulabilityanalysis());
		but4.addActionListener(e -> setoptions());
		bottompanel2 = new JPanel();
		bottompanel2.add(but1);
		bottompanel2.add(but2);
		bottompanel2.add(but3);
		bottompanel2.add(but4);

		bottompanel = new JPanel();
		bottompanel.setLayout(new BoxLayout(bottompanel, BoxLayout.Y_AXIS));
		bottompanel.add(bottompanel1);
		bottompanel.add(bottompanel2);

		bigpanel = new JPanel();
		bigpanel.setLayout(new BoxLayout(bigpanel, BoxLayout.Y_AXIS));
		bigpanel.add(toppanel);
		bigpanel.add(middlepanel);
		bigpanel.add(bottompanel);

		setGUIcomponentstodefaulttaskset(1.0, 1.0);

		frame = new JFrame();
		frame.add(bigpanel);
		frame.setSize(400, 300);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

//public static void main(String[] args) {
//javax.swing.SwingUtilities.invokeLater(new Runnable() {
//public void run() {
//  createAndShowGUI();
//}
//});
//}
}