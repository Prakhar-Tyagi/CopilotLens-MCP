/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.icd;

import chs.cof.icd.IICDAssociatedSignal;
import chs.cof.logical.schem.IPin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Represent the information about the pin that is being placed and the ICD signals that are to be generated at the pin
 */
public class PlacingPinRouteInfo
{

	private IPin placingDevPin;
	private IPin placingPin;
	private Collection<IICDAssociatedSignal> associatedSignals;

	PlacingPinRouteInfo(@NotNull IPin placingDevPin, @NotNull IPin placingPin,
			@NotNull Collection<IICDAssociatedSignal> associatedSignals)
	{
		this.placingDevPin = placingDevPin;
		this.placingPin = placingPin;
		this.associatedSignals = associatedSignals;
	}

	@NotNull public IPin getPlacingDevPin()
	{
		return placingDevPin;
	}

	@NotNull public IPin getPlacingPin()
	{
		return placingPin;
	}

	@NotNull public Collection<IICDAssociatedSignal> getAssociatedSignals()
	{
		return associatedSignals;
	}
}
