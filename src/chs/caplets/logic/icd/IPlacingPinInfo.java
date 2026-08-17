/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.icd;

import chs.common.ILocation;
import chs.utility.IDeviceICDSignalsContainer;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Provide the locations of the pins that are being placed
 */
public interface IPlacingPinInfo
{

	@NotNull Set<ILocation> getPlacingPinAbsoluteLocations(@NotNull IICDSignalSourceSchemPinlist currentSchemPinlist,
			@NotNull IDeviceICDSignalsContainer signalsContainer);
}
