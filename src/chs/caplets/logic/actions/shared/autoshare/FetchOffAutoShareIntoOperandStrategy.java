package chs.caplets.logic.actions.shared.autoshare;

import chs.caplets.logic.actions.shared.AbstractShareActionOperandStrategy;
import chs.caplets.logic.actions.shared.OperandShareabilityStatus;

import java.util.EnumSet;

public class FetchOffAutoShareIntoOperandStrategy extends AbstractShareActionOperandStrategy
{

	@Override protected EnumSet<OperandShareabilityStatus> getSkippableShareStatuses()
	{
		return EnumSet.of(OperandShareabilityStatus.NonShareable);
	}

	@Override protected EnumSet<OperandShareabilityStatus> getSuccessShareStatuses()
	{
		return EnumSet.of(OperandShareabilityStatus.Shareable, OperandShareabilityStatus.PartialPlacedMulticore);
	}
}
