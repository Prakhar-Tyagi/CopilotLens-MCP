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
import chs.utilities.SetMap;
import chs.utilities.StringUtils;
import chs.utility.IDeviceICDSignalsContainer;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calculate the locations of the pins that are being placed
 */
public class GeneralPlacingPinInfo implements IPlacingPinInfo
{

	@Nullable private Map<String, String> mCavVsPinNames = null;
	@Nullable private SetMap<String, ILocation> mPinAbsLocations = null;

	public GeneralPlacingPinInfo(@Nullable List<Pair<ILocation, String>> pinAbsLocations,
			@Nullable Map<String, String> cavVsPinNames)
	{
		mCavVsPinNames = cavVsPinNames;
		if (pinAbsLocations != null) {
			mPinAbsLocations = SetMap.createShallowSetMap();
			for (Pair<ILocation, String> pinAbsLocation : pinAbsLocations) {
				mPinAbsLocations.add(StringUtils.nonNull(pinAbsLocation.getValue()), pinAbsLocation.getKey());
			}
		}
	}

	@Nullable private String derivePinNameKey(@NotNull IDeviceICDSignalsContainer signalsContainer)
	{
		return signalsContainer.getPinName();
	}

	@NotNull public Set<ILocation> getPlacingPinAbsoluteLocations(@NotNull IICDSignalSourceSchemPinlist currentSchemPinlist,
			@NotNull IDeviceICDSignalsContainer signalsContainer)
	{
		String pinNameKey = derivePinNameKey(signalsContainer);
		if (!StringUtils.isBlank(pinNameKey)) {
			if (mPinAbsLocations != null) {
				return mPinAbsLocations.pullReadOnlySafeSet(StringUtils.nonNull(pinNameKey));
			}
			if (mCavVsPinNames != null) {
				String pinNameToSearchDevicePinName = pinNameKey;
				String mappedConectivityPinName = mCavVsPinNames.get(pinNameToSearchDevicePinName);
				if (!StringUtils.isBlank(mappedConectivityPinName)) {
					pinNameToSearchDevicePinName = mappedConectivityPinName;
				}
				ILocation pinLocation = currentSchemPinlist.getPinLocation(pinNameToSearchDevicePinName);
				return pinLocation != null ? Collections.singleton(pinLocation) : Collections.emptySet();
			}
		}
		return Collections.emptySet();
	}
}
