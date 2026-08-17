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
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedPinList;
import chs.common.IDesignDescriptor;
import chs.ctf.caf.utils.IPinProxy;
import chs.utilities.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Pin mate validator for shared pins
 */
public class SharedPinMateValidator implements IPinMateValidator
{
	@Nullable
	public MoveSwapErrorCode validate(@NotNull IPinProxy srcProxyPin, @NotNull IPinProxy targetProxyPin,
			@Nullable IDesignDescriptor srcDesign, @Nullable IDesignDescriptor targetDesign, boolean isSwap)
	{
		if (srcProxyPin.getSharedPin() != null && targetProxyPin.getSharedPin() != null) {
			ISharedPinList srcPinlist = srcProxyPin.getSharedPin().getOwner();
			if (srcPinlist instanceof ISharedConnector && ((ISharedConnector) srcPinlist).isInlineHalf()) {
				return MoveSwapErrorCode.NoError;
			}
			else {
				final boolean areMatesEqual = StringUtils.areEqualOrBothNull(
						srcProxyPin.getAttribute(srcDesign, IPinProxy.MATED_PIN_OWNER_ID),
						targetProxyPin.getAttribute(targetDesign, IPinProxy.MATED_PIN_OWNER_ID));
				return areMatesEqual ? MoveSwapErrorCode.NoError : MoveSwapErrorCode.PinsWithDifferentMates;
			}
		}
		return null;
	}
}
