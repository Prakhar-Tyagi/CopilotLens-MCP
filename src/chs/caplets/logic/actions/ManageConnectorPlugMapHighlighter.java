/*
 * Copyright 2016 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.ctf.caf.utils.IPinProxy;
import chs.utility.ui.IPinConnectionChangeInfoProvider;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ManageConnectorPlugMapHighlighter implements IPinConnectionChangeInfoProvider
{

	private Collection<String> pinsWithCondutors;
	private Map<String, Integer> activePins = new HashMap<>();
	private Set<String> modifiedPins = new HashSet<>();

	public ManageConnectorPlugMapHighlighter(ManageConnectorPinSelections manageConnectorPinSelections,
			Collection<String> pinsWithConductors)
	{

		pinsWithCondutors = pinsWithConductors;
		for (IPinProxy aPinProxy : manageConnectorPinSelections.getPins()) {
			if (aPinProxy.getCablePin() != null && aPinProxy.getCablePin().getNumConductors() > 0) {
				activePins.put(aPinProxy.getName(), 1);
			}
			else {
				activePins.put(aPinProxy.getName(), 0);
			}
		}
	}

	public void updateActivePins(String oldValue, String newValue)
	{
		modifiedPins.add(oldValue);
		modifiedPins.add(newValue);

		Integer currentPinCount = activePins.get(newValue);
		Integer originalPinCount = activePins.get(oldValue);

		activePins.put(oldValue, originalPinCount > 0 ? originalPinCount - 1 : originalPinCount);
		activePins.put(newValue, currentPinCount + 1);
	}

	@Override public boolean isConnectedToNewConductor(String pin)
	{
		if (modifiedPins.contains(pin)) {
			return activePins.get(pin) > 0;
		}
		return false;
	}

	@Override public boolean isDisConnectedFromOldConductor(String pin)
	{
		Integer pinCount = activePins.get(pin);
		return pinCount != null && pinCount == 0 && pinsWithCondutors.contains(pin);
	}
}