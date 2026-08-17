package chs.caplets.logic.actions.shared;

import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class BaseShareActionOperandStrategy extends AbstractShareActionOperandStrategy
{

	private static BaseShareActionOperandStrategy mInstance = null;

	private BaseShareActionOperandStrategy()
	{

	}

	@NotNull public static synchronized BaseShareActionOperandStrategy getInstance()
	{
		if (mInstance == null) {
			mInstance = new BaseShareActionOperandStrategy();
		}
		return mInstance;
	}

	@Override protected EnumSet<OperandShareabilityStatus> getSkippableShareStatuses()
	{
		return EnumSet.noneOf(OperandShareabilityStatus.class);
	}

	@Override protected EnumSet<OperandShareabilityStatus> getSuccessShareStatuses()
	{
		return EnumSet.of(OperandShareabilityStatus.Shareable, OperandShareabilityStatus.PartialPlacedMulticore);
	}
}
