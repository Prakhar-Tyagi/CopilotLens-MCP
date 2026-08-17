/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.validators;

import chs.caplets.logic.MoveSwapErrorCode;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.shared.ISharedBackshellTermination;
import chs.cof.parts.ILibraryObject;
import chs.common.IDesignDescriptor;
import chs.ctf.caf.utils.IPinProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * validates move/swap for backshell termination pin.
 * Prevents moving or swapping backshell termination pins with non-backshell pins.
 * If both pins are backshell terminations, the operation is allowed.
 */
public class BackshellTerminationValidator implements IPinMateValidator
{

	@Nullable
	public MoveSwapErrorCode validate(@NotNull IPinProxy srcPin,
			@NotNull IPinProxy targetPin,
			@Nullable IDesignDescriptor srcDesign, @Nullable IDesignDescriptor targetDesign, boolean isSwap)
	{
		boolean isSrcBackshellTermination = isBackshellTermination(srcPin);
		boolean isTargetBackshellTermination = isBackshellTermination(targetPin);

		if (isSrcBackshellTermination && isTargetBackshellTermination) {
			return MoveSwapErrorCode.NoError;
		}
		if (isSrcBackshellTermination || isTargetBackshellTermination) {
			return isSwap ? MoveSwapErrorCode.SwapBackshellPin :
					MoveSwapErrorCode.MoveBackshellPin;
		}
		return null;
	}

	private boolean isBackshellTermination(IPinProxy pin)
	{
		if (pin.getSharedPin() != null) {
			return pin.getSharedPin() instanceof ISharedBackshellTermination;
		}
		if (pin.getCablePin() != null) {
			return pin.getCablePin() instanceof IBackshellTermination;
		}
		if (pin.getLibraryCavity() != null) {
			return pin.getLibraryCavity().getOwner().getGroupName().equals(ILibraryObject.GroupType.BACKSHELL);
		}
		return false;
	}
}
