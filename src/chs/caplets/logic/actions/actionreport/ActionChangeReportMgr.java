package chs.caplets.logic.actions.actionreport;

import org.jetbrains.annotations.NotNull;

/**
 * action report manager - singleton class
 */
public class ActionChangeReportMgr
{

	private IActionChangeReporterFactory mFactory;

	private ActionChangeReportMgr()
	{
		mFactory = new ActionChangeReporterFactory();
	}

	public void setActionReporterFactory(IActionChangeReporterFactory factory)
	{
		mFactory = factory;
	}

	@NotNull public IMergeActionChangeReporter createMergeActionChangeReporter()
	{
		return mFactory.createMergeActionChangeReporter();
	}

	@NotNull public static ActionChangeReportMgr getInstance()
	{
		return InnerClass.instance;
	}

	private static class InnerClass
	{

		private static final ActionChangeReportMgr instance = new ActionChangeReportMgr();
	}
}
