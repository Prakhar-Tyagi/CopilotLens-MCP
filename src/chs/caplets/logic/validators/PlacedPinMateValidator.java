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
import chs.cof.logical.cable.IAbstractPin;
import chs.common.IDesignDescriptor;
import chs.ctf.caf.utils.IPinProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Pin mate validator for placed pins.
 */
public class PlacedPinMateValidator implements IPinMateValidator
{

	@Nullable
	public MoveSwapErrorCode validate(@NotNull IPinProxy srcProxyPin,
			@NotNull IPinProxy targetProxyPin,
			@Nullable IDesignDescriptor srcDesign, @Nullable IDesignDescriptor targetDesign, boolean isSwap)
	{
		if (srcProxyPin.getCablePin() != null && targetProxyPin.getCablePin() != null) {
			IAbstractPin srcPin = srcProxyPin.getCablePin();
			IAbstractPin targetPin = targetProxyPin.getCablePin();
			if (srcPin.getConnectedPins().size() == 1 && targetPin.getConnectedPins().size() == 1) {
				return srcPin.getConnectedPins().iterator().next().getOwner() ==
						targetPin.getConnectedPins().iterator().next().getOwner() ?
						MoveSwapErrorCode.NoError :
						MoveSwapErrorCode.PinsWithDifferentMates;
			}
			else {
				return srcPin.getConnectedPins().isEmpty() && targetPin.getConnectedPins().isEmpty() ?
						MoveSwapErrorCode.NoError :
						MoveSwapErrorCode.PinsWithDifferentMates;
			}
		}
		return null;
	}
}
