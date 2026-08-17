package chs.caplets.logic.actions.shared;

import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public abstract class AbstractShareActionOperandStrategy implements IShareOperandStrategy
{

	protected abstract EnumSet<OperandShareabilityStatus> getSkippableShareStatuses();

	protected abstract EnumSet<OperandShareabilityStatus> getSuccessShareStatuses();

	private EnumSet<OperandShareabilityStatus> getErrorShareStatuses()
	{
		EnumSet<OperandShareabilityStatus> nonErrorStatuses = EnumSet.copyOf(getSkippableShareStatuses());
		nonErrorStatuses.addAll(getSuccessShareStatuses());
		return EnumSet.complementOf(nonErrorStatuses);
	}

	@Override public boolean isShareable(@NotNull OperandShareabilityStatus shareabilityStatus)
	{
		return getSuccessShareStatuses().contains(shareabilityStatus);
	}

	@Override public boolean isSkippable(@NotNull OperandShareabilityStatus shareabilityStatus)
	{
		return getSkippableShareStatuses().contains(shareabilityStatus);
	}

	@Override public boolean isError(@NotNull OperandShareabilityStatus shareabilityStatus)
	{
		return getErrorShareStatuses().contains(shareabilityStatus);
	}
}
