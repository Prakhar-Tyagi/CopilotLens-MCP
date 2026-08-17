/*
 * Copyright 2006-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.IPin;
import chs.common.ILocation;
import chs.utility.GfxObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.jetbrains.annotations.Nullable;

public class ConnectByPinAction extends ConnectAction
{

	public ConnectByPinAction(ICapletController controller)
	{
		super(controller);
	}

	public String getActionUIClass()
	{
		return ConnectByPinActionUI.class.getName();
	}

	@Nullable protected Class<? extends IConductor> getConductorClass()
	{
		return null;
	}

	protected boolean allPinsAreFromSamePinlist(@NotNull Set<IPin> pins)
	{
		if (pins.size() > 1) {
			final IPinList parent = getCablePinlist(pins.iterator().next());
			return pins.stream().allMatch(pin -> getCablePinlist(pin) == parent);
		}
		return true;
	}

	@Nullable private static IPinList getCablePinlist(@NotNull IPin pin)
	{
		return pin.getConnectivity().getOwner();
	}

	@Override protected Set<ILocation> getFinalizedLocations(@NotNull Map<ILocation, Set<IPin>> inputPins)
	{
		final Set<ILocation> frozenPairs = new TreeSet<>(GfxObjectUtils.getLocationComparator());
		frozenPairs.addAll(inputPins.keySet());
		for (Map.Entry<ILocation, Set<IPin>> entry : inputPins.entrySet()) {
			if (!isValidInputPinsEntry(entry)) {
				frozenPairs.remove(entry.getKey());
			}
		}
		return frozenPairs;
	}
}
