/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.inlineassist;

import org.jetbrains.annotations.NotNull;

/**
 * This interface defines methods to be implemented by classes responsible for terminating the shield conductor
 * on inlines.
 */
public interface IShieldConnector
{

	/**
	 * Connect the shield termination conductor to the inline half pin.
	 *
	 * @param terminationParams the inline half shield termination parameters
	 */
	public void connectShield(@NotNull InlineHalfShieldTerminationParams terminationParams);
}
