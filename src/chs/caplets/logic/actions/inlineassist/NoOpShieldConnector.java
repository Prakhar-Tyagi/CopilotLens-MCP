/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.inlineassist;

import org.jetbrains.annotations.NotNull;

/**
 * This implements {@link IShieldConnector} to skip terminating the shield on inline half
 */
class NoOpShieldConnector implements IShieldConnector
{

	@Override public void connectShield(@NotNull InlineHalfShieldTerminationParams terminationParams)
	{
	}
}
