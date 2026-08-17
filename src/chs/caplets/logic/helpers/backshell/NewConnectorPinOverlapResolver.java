/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.helpers.backshell;

import chs.cof.logical.schem.IPin;
import chs.utility.logic.PinUtils;
import org.jetbrains.annotations.NotNull;

/**
 * A new implementation of IBackshellPinOverlapResolver that resolves pin overlaps .
 * by transferring conductors from the overlapped pin to the backshell pin,
 * and then deletes the overlapped pin. This is used in the flow when a new connector is created and overlaps with the backshell pin,
 * in which case we want to keep the backshell pin and delete the newly created pin that overlaps with it.
 */
public class NewConnectorPinOverlapResolver extends DefaultBackshellPinOverlapResolver
{

	@Override public void resolveOverlappedPins(@NotNull IPin backshellPin, @NotNull IPin overlappedPin)
	{
		super.resolveOverlappedPins(backshellPin, overlappedPin);
		PinUtils.transferConductorConnections(overlappedPin.getConnectivity(), backshellPin.getConnectivity());
		transferSchemConductors(overlappedPin, backshellPin);
		deleteSchemPin(overlappedPin, true);
	}
}
