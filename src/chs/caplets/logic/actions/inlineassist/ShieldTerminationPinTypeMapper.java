/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions.inlineassist;

import chs.caplets.logic.actions.PlaceInlineBackshellTerminationEnum;
import org.jetbrains.annotations.NotNull;

/**
 * Utility for mapping {@link PlaceInlineBackshellTerminationEnum} to {@link ShieldTerminationPinType}.
 */

public class ShieldTerminationPinTypeMapper
{

	@NotNull
	public ShieldTerminationPinType getShieldTerminationPinType(
			@NotNull PlaceInlineBackshellTerminationEnum terminationPinType)
	{
		switch (terminationPinType) {
			case BACKSHELL_TERMINATION:
				return ShieldTerminationPinType.BACKSHELL_PIN;
			case NO_TERMINATION:
			default:
				return ShieldTerminationPinType.CABLE_PIN;
		}
	}
}
