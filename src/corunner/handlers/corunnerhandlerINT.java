package corunner.handlers;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.osate.aadl2.Aadl2Package;
import org.osate.aadl2.ComponentCategory;
import org.osate.aadl2.Element;
import org.osate.aadl2.IntegerLiteral;
import org.osate.aadl2.ListValue;
import org.osate.aadl2.Mode;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.Property;
import org.osate.aadl2.PropertyExpression;
import org.osate.aadl2.RealLiteral;
import org.osate.aadl2.StringLiteral;
import org.osate.aadl2.contrib.aadlproject.TimeUnits;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.InstanceObject;
import org.osate.aadl2.instance.InstanceReferenceValue;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.modelsupport.scoping.Aadl2GlobalScopeUtil;
import org.osate.aadl2.properties.PropertyNotPresentException;
import org.osate.pluginsupport.properties.CodeGenUtil;
import org.osate.pluginsupport.properties.IntegerRangeWithUnits;
import org.osate.pluginsupport.properties.IntegerWithUnits;
import org.osate.ui.handlers.AaxlReadOnlyHandlerAsJob;

import corunner.handlers.javaschedanalysiscorunner.corunnerinfo;
import corunner.handlers.javaschedanalysiscorunner.segment;

public class corunnerhandlerINT extends AaxlReadOnlyHandlerAsJob {
	private String fileName = "";
	ExecutionEvent event;

	// select which solver to use - this handler will request the internal solver

	static final int USE_INBUILT_SOLVER = 0;
	static final int USE_GUROBI_SOLVER = 1;
	static final int solver_to_use = USE_INBUILT_SOLVER;

	String thread_names[];

	class TaskIdStringandId {
		String TaskIdString;
		int Id;

		TaskIdStringandId(String TaskIdString, int Id) {
			this.TaskIdString = TaskIdString;
			this.Id = Id;
		}
	}

	static int getIdfromTaskIdString(ArrayList<TaskIdStringandId> aTaskIdStringandIdList, String aTaskIdString) {
		for (int temp = 0; temp < aTaskIdStringandIdList.size(); temp++) {
			if (aTaskIdStringandIdList.get(temp).TaskIdString.equals(aTaskIdString)) {
				return aTaskIdStringandIdList.get(temp).Id;
			}
		}
		System.out.println("An error in getIdfromTaskIdString");
		System.exit(-1);
		return (-1);
	}

	static String getTaskIdStringfromId(ArrayList<TaskIdStringandId> aTaskIdStringandIdList, int anId) {
		for (int temp = 0; temp < aTaskIdStringandIdList.size(); temp++) {
			if (aTaskIdStringandIdList.get(temp).Id == anId) {
				return aTaskIdStringandIdList.get(temp).TaskIdString;
			}
		}
		System.out.println("An error in getTaskIdStringfromId");
		System.exit(-1);
		return "";
	}

	@Override
	public Object execute(ExecutionEvent event) {
		try {
			super.execute(event);
			this.event = event;
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String getActionName() {
		return "Schedulability Analysis of Tasks with Co-runner Dependent Execution Times";
	}

	@Override
	public void doAaxlAction(IProgressMonitor monitor, Element root) {
		System.out.println("In start of doAaxAction");
//		System.exit(-1);
		SystemInstance si = (SystemInstance) root;
		final List<ComponentInstance> allThreads = si.getAllComponentInstances()
				.stream()
				.filter(comp -> (comp.getCategory() == ComponentCategory.THREAD))
				.collect(Collectors.toList());
		javaschedanalysiscorunner.ntasks = allThreads.size();
		final List<ComponentInstance> allProcessors = si.getAllComponentInstances()
				.stream()
				.filter(comp -> (comp.getCategory() == ComponentCategory.PROCESSOR))// //
				.collect(Collectors.toList());
		javaschedanalysiscorunner.nprocessors = allProcessors.size();

		System.out.println("number of tasks");
		System.out.println(javaschedanalysiscorunner.ntasks);
		System.out.println("number of processors");
		System.out.println(javaschedanalysiscorunner.nprocessors);
		System.out.println("solver to use");
		System.out.println(javaschedanalysiscorunner.solver_to_use);
//		System.exit(-1);

		thread_names = new String[javaschedanalysiscorunner.ntasks];

		javaschedanalysiscorunner.solver_to_use = solver_to_use;
		System.out.println("solver to use");
		System.out.println(javaschedanalysiscorunner.solver_to_use);

		javaschedanalysiscorunner.priorities.clear();
		javaschedanalysiscorunner.proc.clear();
		javaschedanalysiscorunner.periods.clear();
		javaschedanalysiscorunner.deadlines.clear();
		javaschedanalysiscorunner.V.clear();
		javaschedanalysiscorunner.executiontimes.clear();
		javaschedanalysiscorunner.pd.clear();
		javaschedanalysiscorunner.CO.clear();
		System.out.println("parameters cleared");
//		System.exit(-1);

		ArrayList<TaskIdStringandId> theTaskIdStringandIdList = new ArrayList<TaskIdStringandId>();
		theTaskIdStringandIdList.clear();
		Optional<String> OptionalTaskidString;
		String TaskidString;

		System.out.println("Start OptionalTaskidString fetch");
//		System.exit(-1);

		for (int i = 1; i <= javaschedanalysiscorunner.ntasks; i++) {
			System.out.println("Inside OptionalTaskidString fetch");
//			System.exit(-1);
			OptionalTaskidString = getTaskidString(allThreads.get(i - 1));
			System.out.println("OptionalTaskidString");
			System.out.println(OptionalTaskidString);
//			System.exit(-1);
			if (!OptionalTaskidString.isPresent()) {
				System.out.println("For task " + i + ", there is no taskid string given.");
			} else {
				TaskidString = OptionalTaskidString.get();
				System.out.println("TaskidString");
				System.out.println(TaskidString);
				thread_names[i - 1] = new String(TaskidString);
//				System.exit(-1);
				theTaskIdStringandIdList.add(new TaskIdStringandId(TaskidString, i));
			}
		}

		OptionalLong OptionalPriority;
		for (int i = 1; i <= javaschedanalysiscorunner.ntasks; i++) {
			OptionalPriority = getPriority(allThreads.get(i - 1));
			if (!OptionalPriority.isPresent()) {
				System.out.println("For task " + i + ", there is no priority given.");
			} else {
				javaschedanalysiscorunner.setpriorities(i, (int) OptionalPriority.getAsLong());
				System.out.println("setpriorities done");
//				System.exit(-1);
			}
		}

		Optional<List<InstanceObject>> OptionalListInstanceObject;
		List<InstanceObject> ListInstanceObject;
		for (int i = 1; i <= javaschedanalysiscorunner.ntasks; i++) {
			OptionalListInstanceObject = getActualProcessorBinding(allThreads.get(i - 1));
			if (!OptionalListInstanceObject.isPresent()) {
				System.out.println("For task " + i + ", there is no assignment-to-processor given.");
			} else {
				ListInstanceObject = OptionalListInstanceObject.get();
				if (ListInstanceObject.size() == 1) {
					int p;
					int iter_p;
					p = -1;
					for (iter_p = 1; iter_p <= javaschedanalysiscorunner.nprocessors; iter_p++) {
						if (ListInstanceObject.get(0) == allProcessors.get(iter_p - 1)) {
							if (p == -1) {
								p = iter_p;
							} else {
								System.out.print("Error because there was at least two matching processors.");
								System.exit(-1);
								return;
							}
						}
					}
					javaschedanalysiscorunner.setproc(i, p);
					System.out.println("setprocessors done");
//					System.exit(-1);
				} else {
					System.out.print(
							"Error when looking at assignment to processors. ListInstanceObject was different from one.");
					System.exit(-1);
					return;
				}
			}
		}

		Optional<IntegerWithUnits<TimeUnits>> OptionalIntegerWithUnitsTimeUnits;
		IntegerWithUnits<TimeUnits> IntegerWithUnitsTimeUnits;
		for (int i = 1; i <= javaschedanalysiscorunner.ntasks; i++) {
			OptionalIntegerWithUnitsTimeUnits = getPeriod(allThreads.get(i - 1));
			if (!OptionalIntegerWithUnitsTimeUnits.isPresent()) {
				System.out.println("For task " + i + ", there is no period given.");
			} else {
				IntegerWithUnitsTimeUnits = OptionalIntegerWithUnitsTimeUnits.get();
				javaschedanalysiscorunner.setperiods(i, IntegerWithUnitsTimeUnits.getValue());
				System.out.println("periods set");
//				System.exit(-1);
			}
		}

		for (int i = 1; i <= javaschedanalysiscorunner.ntasks; i++) {
			OptionalIntegerWithUnitsTimeUnits = getDeadline(allThreads.get(i - 1));
			if (!OptionalIntegerWithUnitsTimeUnits.isPresent()) {
				System.out.println("For task " + i + ", there is no deadline given.");
			} else {
				IntegerWithUnitsTimeUnits = OptionalIntegerWithUnitsTimeUnits.get();
				javaschedanalysiscorunner.setdeadlines(i, IntegerWithUnitsTimeUnits.getValue());
				System.out.println("deadlines set");
//				System.exit(-1);
			}
		}

		for (int i = 1; i <= javaschedanalysiscorunner.ntasks; i++) {
			javaschedanalysiscorunner.addsegmenttoV(i, i, 1);
		}

		Optional<IntegerRangeWithUnits<TimeUnits>> OptionalIntegerRangeWithUnitsTimeUnits;
		IntegerRangeWithUnits<TimeUnits> IntegerRangeWithUnitsTimeUnits;
		IntegerWithUnits<TimeUnits> IntegerWithUnitsTimeUnitsmin;
		IntegerWithUnits<TimeUnits> IntegerWithUnitsTimeUnitsmax;
		for (int i = 1; i <= javaschedanalysiscorunner.ntasks; i++) {
			OptionalIntegerRangeWithUnitsTimeUnits = getExecutiontime(allThreads.get(i - 1));
			if (!OptionalIntegerRangeWithUnitsTimeUnits.isPresent()) {
				System.out.println("For task " + i + ", there is no execution time given.");
			} else {
				IntegerRangeWithUnitsTimeUnits = OptionalIntegerRangeWithUnitsTimeUnits.get();
				IntegerWithUnitsTimeUnitsmin = IntegerRangeWithUnitsTimeUnits.getMinimum();
				IntegerWithUnitsTimeUnitsmax = IntegerRangeWithUnitsTimeUnits.getMaximum();
				javaschedanalysiscorunner.setexecutiontime(i, 1, IntegerWithUnitsTimeUnitsmax.getValue());
				System.out.println("execution time set");
//				System.exit(-1);
			}
		}

		OptionalDouble OptionalPd;
		for (int i = 1; i <= javaschedanalysiscorunner.ntasks; i++) {
			OptionalPd = getpd(allThreads.get(i - 1));
			if (!OptionalPd.isPresent()) {
				System.out.println("For task " + i + ", there is no pd given.");
			} else {
				javaschedanalysiscorunner.setpd(i, 1, OptionalPd.getAsDouble());
			}
		}

		ArrayList<corunnerinfo> COiv;
		Optional<String> OptionalCOString;
		String COString;
		for (int i = 1; i <= javaschedanalysiscorunner.ntasks; i++) {
			OptionalCOString = getCOString(allThreads.get(i - 1));
			if (!OptionalCOString.isPresent()) {
				System.out.println("For task " + i + ", there is no CO given.");
			} else {
				COString = OptionalCOString.get();
				COiv = getCOiv_from_COString(COString, theTaskIdStringandIdList);
				javaschedanalysiscorunner.setCO(i, 1, COiv);
			}
		}

		Boolean flag;
		Instant starttime = Instant.now();
		flag = javaschedanalysiscorunner.doschedulabilitytesting();
		if (flag) {
			String temps = "Upper bounds on the response times of task are as follows:\n";
			for (int i = 1; i <= javaschedanalysiscorunner.ntasks; i++) {
				// temps = temps + " For task " + Integer.toString(i) + ": "
				// + Double.toString(javaschedanalysiscorunner.newt.get(i - 1)) + "\n";
				temps = temps + "  For " + thread_names[i - 1] + ": "
						+ Double.toString(javaschedanalysiscorunner.newt.get(i - 1)) + "\n";
			}
			String outputstr1 = "Taskset is schedulable";
			String outputstr2 = temps;
			Instant finishtime = Instant.now();
			long timeElapsed = Duration.between(starttime, finishtime).toMillis();
			String outputstr3 = Long.toString(timeElapsed);
			System.out.println(outputstr1 + " " + outputstr2 + " " + outputstr3);
			JOptionPane.showMessageDialog(null, outputstr1 + "\n" + outputstr2);
		} else {
			String outputstr1 = "Cannot guarantee schedulability";
			String outputstr2 = "                                             ";
			Instant finishtime = Instant.now();
			long timeElapsed = Duration.between(starttime, finishtime).toMillis();
			String outputstr3 = Long.toString(timeElapsed);
			System.out.println(outputstr1 + " " + outputstr2 + " " + outputstr3);
			JOptionPane.showMessageDialog(null, outputstr1 + "\n" + outputstr2);
		}
	}

	public static OptionalLong getPriority(NamedElement lookupContext) {
		return getPriority(lookupContext, Optional.empty());
	}

	public static OptionalLong getPriority(NamedElement lookupContext, Mode mode) {
		return getPriority(lookupContext, Optional.of(mode));
	}

	public static OptionalLong getPriority(NamedElement lookupContext, Optional<Mode> mode) {
		String name = "Thread_Properties::Priority";
		Property property = Aadl2GlobalScopeUtil.get(lookupContext, Aadl2Package.eINSTANCE.getProperty(), name);
		try {
			PropertyExpression value = CodeGenUtil.lookupProperty(property, lookupContext, mode);
			PropertyExpression resolved = CodeGenUtil.resolveNamedValue(value, lookupContext, mode);
			return OptionalLong.of(((IntegerLiteral) resolved).getValue());
		} catch (PropertyNotPresentException e) {
			return OptionalLong.empty();
		}
	}

	public static Optional<List<InstanceObject>> getActualProcessorBinding(NamedElement lookupContext) {
		return getActualProcessorBinding(lookupContext, Optional.empty());
	}

	public static Optional<List<InstanceObject>> getActualProcessorBinding(NamedElement lookupContext, Mode mode) {
		return getActualProcessorBinding(lookupContext, Optional.of(mode));
	}

	public static Optional<List<InstanceObject>> getActualProcessorBinding(NamedElement lookupContext,
			Optional<Mode> mode) {
		String name = "Actual_Processor_Binding";
		Property property = Aadl2GlobalScopeUtil.get(lookupContext, Aadl2Package.eINSTANCE.getProperty(), name);
		try {
			PropertyExpression value = CodeGenUtil.lookupProperty(property, lookupContext, mode);
			PropertyExpression resolved = CodeGenUtil.resolveNamedValue(value, lookupContext, mode);
			return Optional.of(((ListValue) resolved).getOwnedListElements().stream().map(element1 -> {
				PropertyExpression resolved1 = CodeGenUtil.resolveNamedValue(element1, lookupContext, mode);
				return ((InstanceReferenceValue) resolved1).getReferencedInstanceObject();
			}).collect(Collectors.toList()));
		} catch (PropertyNotPresentException e) {
			return Optional.empty();
		}
	}

	public static Optional<IntegerWithUnits<TimeUnits>> getPeriod(NamedElement lookupContext) {
		return getPeriod(lookupContext, Optional.empty());
	}

	public static Optional<IntegerWithUnits<TimeUnits>> getPeriod(NamedElement lookupContext, Mode mode) {
		return getPeriod(lookupContext, Optional.of(mode));
	}

	public static Optional<IntegerWithUnits<TimeUnits>> getPeriod(NamedElement lookupContext, Optional<Mode> mode) {
		String name = "Timing_Properties::Period";
		Property property = Aadl2GlobalScopeUtil.get(lookupContext, Aadl2Package.eINSTANCE.getProperty(), name);
		try {
			PropertyExpression value = CodeGenUtil.lookupProperty(property, lookupContext, mode);
			PropertyExpression resolved = CodeGenUtil.resolveNamedValue(value, lookupContext, mode);
			return Optional.of(new IntegerWithUnits<>(resolved, TimeUnits.class));
		} catch (PropertyNotPresentException e) {
			return Optional.empty();
		}
	}

	public static Optional<IntegerWithUnits<TimeUnits>> getDeadline(NamedElement lookupContext) {
		return getDeadline(lookupContext, Optional.empty());
	}

	public static Optional<IntegerWithUnits<TimeUnits>> getDeadline(NamedElement lookupContext, Mode mode) {
		return getDeadline(lookupContext, Optional.of(mode));
	}

	public static Optional<IntegerWithUnits<TimeUnits>> getDeadline(NamedElement lookupContext, Optional<Mode> mode) {
		String name = "Timing_Properties::Deadline";
		Property property = Aadl2GlobalScopeUtil.get(lookupContext, Aadl2Package.eINSTANCE.getProperty(), name);
		try {
			PropertyExpression value = CodeGenUtil.lookupProperty(property, lookupContext, mode);
			PropertyExpression resolved = CodeGenUtil.resolveNamedValue(value, lookupContext, mode);
			return Optional.of(new IntegerWithUnits<>(resolved, TimeUnits.class));
		} catch (PropertyNotPresentException e) {
			return Optional.empty();
		}
	}

	public static Optional<IntegerRangeWithUnits<TimeUnits>> getExecutiontime(NamedElement lookupContext) {
		return getExecutiontime(lookupContext, Optional.empty());
	}

	public static Optional<IntegerRangeWithUnits<TimeUnits>> getExecutiontime(NamedElement lookupContext, Mode mode) {
		return getExecutiontime(lookupContext, Optional.of(mode));
	}

	public static Optional<IntegerRangeWithUnits<TimeUnits>> getExecutiontime(NamedElement lookupContext,
			Optional<Mode> mode) {
		String name = "Timing_Properties::Compute_Execution_Time";
		Property property = Aadl2GlobalScopeUtil.get(lookupContext, Aadl2Package.eINSTANCE.getProperty(), name);
		try {
			PropertyExpression value = CodeGenUtil.lookupProperty(property, lookupContext, mode);
			PropertyExpression resolved = CodeGenUtil.resolveNamedValue(value, lookupContext, mode);
			return Optional.of(new IntegerRangeWithUnits<>(resolved, TimeUnits.class, lookupContext, mode));
		} catch (PropertyNotPresentException e) {
			return Optional.empty();
		}
	}

	public static Optional<String> getTaskidString(NamedElement lookupContext) {
		System.out.println("In public getTaskidString");
//		System.exit(-1);
		return getTaskidString(lookupContext, Optional.empty());
	}

	public static Optional<String> getTaskidString(NamedElement lookupContext, Mode mode) {
		System.out.println("In public getTaskidString - lookup context");
//		System.exit(-1);
		return getTaskidString(lookupContext, Optional.of(mode));
	}

	public static Optional<String> getTaskidString(NamedElement lookupContext, Optional<Mode> mode) {
		System.out.println("In public getTaskidString - LookupContext, optional");
		System.out.println(lookupContext);
//		System.exit(-1);
		String name = "corunnerinfopropertyset::taskid";
		System.out.println("name");
		System.out.println(name);
		System.out.println("context");
		System.out.println(lookupContext);
		System.out.println("eINSTANCE");
		System.out.println(Aadl2Package.eINSTANCE.getProperty());
//		System.exit(-1);
		Property property = Aadl2GlobalScopeUtil.get(lookupContext, Aadl2Package.eINSTANCE.getProperty(), name);
		try {
			System.out.println("property");
			System.out.println(property);
//			System.exit(-1);
			PropertyExpression value = CodeGenUtil.lookupProperty(property, lookupContext, mode);
			System.out.println("value");
			System.out.println(value);
//			System.exit(-1);
			PropertyExpression resolved = CodeGenUtil.resolveNamedValue(value, lookupContext, mode);
			System.out.println("resolved");
			System.out.println(resolved);
//			System.exit(-1);
			return Optional.of(((StringLiteral) resolved).getValue());
		} catch (PropertyNotPresentException e) {
			return Optional.empty();
		}
	}

	public static OptionalDouble getpd(NamedElement lookupContext) {
		return getpd(lookupContext, Optional.empty());
	}

	public static OptionalDouble getpd(NamedElement lookupContext, Mode mode) {
		return getpd(lookupContext, Optional.of(mode));
	}

	public static OptionalDouble getpd(NamedElement lookupContext, Optional<Mode> mode) {
		String name = "corunnerinfopropertyset::pd";
		Property property = Aadl2GlobalScopeUtil.get(lookupContext, Aadl2Package.eINSTANCE.getProperty(), name);
		try {
			PropertyExpression value = CodeGenUtil.lookupProperty(property, lookupContext, mode);
			PropertyExpression resolved = CodeGenUtil.resolveNamedValue(value, lookupContext, mode);
			return OptionalDouble.of(((RealLiteral) resolved).getValue());
		} catch (PropertyNotPresentException e) {
			return OptionalDouble.empty();
		}
	}

	public static Optional<String> getCOString(NamedElement lookupContext) {
		return getCOString(lookupContext, Optional.empty());
	}

	public static Optional<String> getCOString(NamedElement lookupContext, Mode mode) {
		return getCOString(lookupContext, Optional.of(mode));
	}

	public static Optional<String> getCOString(NamedElement lookupContext, Optional<Mode> mode) {
		String name = "corunnerinfopropertyset::corunnerstring";
		Property property = Aadl2GlobalScopeUtil.get(lookupContext, Aadl2Package.eINSTANCE.getProperty(), name);
		try {
			PropertyExpression value = CodeGenUtil.lookupProperty(property, lookupContext, mode);
			PropertyExpression resolved = CodeGenUtil.resolveNamedValue(value, lookupContext, mode);
			return Optional.of(((StringLiteral) resolved).getValue());
		} catch (PropertyNotPresentException e) {
			return Optional.empty();
		}
	}

	public static segment get_segment_based_on_string(String s, ArrayList<TaskIdStringandId> theTaskIdStringandIdList) {
		int id;
		String aTaskIdString = s.trim();
		id = getIdfromTaskIdString(theTaskIdStringandIdList, aTaskIdString);
		segment asegment = new segment(id, 1);
		return asegment;
	}

	public static int getindexOf_given_startindex(String currentstring, char c, int startindex) {
		if (startindex >= 0) {
			int remain_index = currentstring.substring(startindex, currentstring.length()).indexOf(c);
			if (remain_index != -1) {
				return startindex + remain_index;
			} else {
				return -1;
			}
		} else {
			System.out.println("Error in getindexOf_given_startindex. startindex is negative.");
			return -1;
		}
	}

	public static void consider_substring_for_processing_segment(String currentstring, int prev_index, int index,
			ArrayList<segment> corset, ArrayList<TaskIdStringandId> theTaskIdStringandIdList) {
		String substring1;
		substring1 = currentstring.substring(prev_index + 1, index);
		segment mysegment = get_segment_based_on_string(substring1, theTaskIdStringandIdList);
		corset.add(mysegment);
	}

	public static ArrayList<segment> get_corset_based_on_string(String s,
			ArrayList<TaskIdStringandId> theTaskIdStringandIdList) {
		int index;
		int prev_index;
		ArrayList<segment> corset =  new ArrayList<segment>();
		corset.clear();
		String s2 = s.trim();
		if ((s2.charAt(0) == '[') && (s2.charAt(s2.length() - 1) == ']')) {
			String currentstring = (s2.substring(1, s2.length() - 1)).trim();
			prev_index = -1;
			index = getindexOf_given_startindex(currentstring, ',', prev_index + 1);
			while (index != -1) {
				consider_substring_for_processing_segment(currentstring, prev_index, index, corset,
						theTaskIdStringandIdList);
				prev_index = index;
				index = getindexOf_given_startindex(currentstring, ',', prev_index + 1);
			}
			consider_substring_for_processing_segment(currentstring, prev_index, currentstring.length(), corset,
					theTaskIdStringandIdList);
		} else {
			System.out.print("Error in get_corset_based_on_string. Did not find brackets.");
			System.exit(-1);
			return null;
		}
		return corset;
	}

	public static corunnerinfo get_corunnerinfo_based_on_string(String s,
			ArrayList<TaskIdStringandId> theTaskIdStringandIdList) {
		int index;
		corunnerinfo mycorunnerinfo = new corunnerinfo();
		String s2 = s.trim();
		if ((s2.charAt(0) == '[') && (s2.charAt(s2.length() - 1) == ']')) {
			String currentstring = (s2.substring(1, s2.length() - 1)).trim();
			index = currentstring.lastIndexOf(',');
			if (index != -1) {
				String substring1;
				String substring2;
				substring1 = currentstring.substring(0, index);
				substring2 = currentstring.substring(index + 1, currentstring.length());
				mycorunnerinfo.corset = get_corset_based_on_string(substring1, theTaskIdStringandIdList);
				mycorunnerinfo.speed = Double.parseDouble(substring2);
			} else {
				System.out.print("Error in get_corunnerinfo_based_on_string. Did not find comma.");
				System.exit(-1);
				return null;
			}
		} else {
			System.out.print("Error in get_corunnerinfo_based_on_string. Did not find brackets.");
			System.exit(-1);
			return null;
		}
		return mycorunnerinfo;
	}

	public static Boolean is_character_surrounded_by(String s, int index, char l, char r) {
		String substring1;
		String substring2;
		substring1 = s.substring(0, index);
		substring2 = s.substring(index + 1, s.length());
		String substring1_tr;
		String substring2_tr;
		substring1_tr = substring1.trim();
		substring2_tr = substring2.trim();
		return ((substring2_tr.charAt(0) == r) && (substring1_tr.charAt(substring1_tr.length() - 1) == l));
	}

	public static int getindex_that_separates_corunnerinfo_string_given_startindex(String s, int startindex) {
		int index;
		index = getindexOf_given_startindex(s, ',', startindex);
		while (index != -1) {
			if (is_character_surrounded_by(s, index, ']', '[')) {
				return index;
			}
			index = getindexOf_given_startindex(s, ',', index + 1);
		}
		return -1;
	}

	public static void consider_substring_for_processing_corunnerinfo(String currentstring, int prev_index, int index,
			ArrayList<corunnerinfo> COiv, ArrayList<TaskIdStringandId> theTaskIdStringandIdList) {
		String substring_for_one_corunnerinfo;
		corunnerinfo mycorunnerinfo;
		substring_for_one_corunnerinfo = currentstring.substring(prev_index + 1, index);
		mycorunnerinfo = get_corunnerinfo_based_on_string(substring_for_one_corunnerinfo, theTaskIdStringandIdList);
		COiv.add(mycorunnerinfo);
	}

	public static ArrayList<corunnerinfo> getCOiv_from_COString(String s,
			ArrayList<TaskIdStringandId> theTaskIdStringandIdList) {
		int index; int prev_index;
		String s2;
		String currentstring;
		ArrayList<corunnerinfo> COiv;
		COiv = new ArrayList<corunnerinfo>();
		COiv.clear();
		s2 = s.trim();
		if ((s2.charAt(0) == '[') && (s2.charAt(s2.length() - 1) == ']')) {
			currentstring = (s2.substring(1, s2.length() - 1)).trim();
			prev_index = -1;
			index = getindex_that_separates_corunnerinfo_string_given_startindex(currentstring, prev_index+1);
			while (index!=-1) {
				consider_substring_for_processing_corunnerinfo(currentstring, prev_index, index, COiv,
						theTaskIdStringandIdList);
				prev_index = index;
				index = getindex_that_separates_corunnerinfo_string_given_startindex(currentstring, prev_index+1);
			}
			consider_substring_for_processing_corunnerinfo(currentstring, prev_index, currentstring.length(), COiv,
					theTaskIdStringandIdList);
		} else {
			System.out.print("Error in getCOiv_from_COString. Did not find brackets.");
			System.exit(-1);
			return null;
		}
		return COiv;
	}

}
