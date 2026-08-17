package chs.caplets.logic.actions.actionreport;

import org.jetbrains.annotations.NotNull;

/**
 * factory class
 */
public class ActionChangeReporterFactory implements IActionChangeReporterFactory
{

	@NotNull @Override public IMergeActionChangeReporter createMergeActionChangeReporter()
	{
		return new MergeActionChangeDisplayHelper();
	}
}
