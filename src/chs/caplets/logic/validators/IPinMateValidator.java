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
import chs.common.IDesignDescriptor;
import chs.ctf.caf.utils.IPinProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Validates swap/move operation for pin connections in manage connections.
 */
public interface IPinMateValidator
{

	@Nullable MoveSwapErrorCode validate(@NotNull IPinProxy srcPin,
			@NotNull IPinProxy targetPin, @Nullable IDesignDescriptor srcDesign,
			@Nullable IDesignDescriptor targetDesign, boolean isSwap);
}
