package chs.caplets.logic.actions.shared;

import org.jetbrains.annotations.NotNull;

public interface IShareOperandStrategy
{

	boolean isShareable(@NotNull OperandShareabilityStatus shareabilityStatus);

	boolean isSkippable(@NotNull OperandShareabilityStatus shareabilityStatus);

	boolean isError(@NotNull OperandShareabilityStatus shareabilityStatus);
}
