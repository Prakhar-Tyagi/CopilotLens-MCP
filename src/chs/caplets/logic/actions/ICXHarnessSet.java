/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConductorIterator;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IInterconnectConductor;
import chs.cof.logical.cable.IInterconnectObject;
import chs.cof.logical.cable.IPinList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public class ICXHarnessSet extends HashSet
{

	public ICXHarnessSet(IInterconnectConductor icx)
	{
		try {
			accumulateFromConductor(icx);
		}
		catch (RuntimeException ex) {
			clear();
			throw (ex);
		}
	}

	public ICXHarnessSet(IConnector icxConnector)
	{
		try {
			accumulateFromConnector(icxConnector);
			if (size() == 1) {
				clear();
			}
		}
		catch (RuntimeException ex) {
			clear();
			throw (ex);
		}
	}

	private void accumulateFromConductor(IInterconnectConductor icx)
	{
		Collection connectors = getConnectors(icx);
		add(icx);
		for (Iterator itr = connectors.iterator(); itr.hasNext();) {
			IConnector icxConnector = (IConnector) itr.next();
			if (!contains(icxConnector)) {
				accumulateFromConnector(icxConnector);
			}
		}
	}

	private static Collection getConnectors(IInterconnectConductor icx)
	{

		// Can't deal with anything other than 2 pins
		if (icx.getNumPins() < 2) {
			throw new UnderconnectedInterconnectException(icx);
		}
		else if (icx.getNumPins() > 2) {
			throw new OverconnectedInterconnectException(icx);
		}

		Collection connectors = new ArrayList(2);
		for (IAbstractPinIterator itr = icx.getPins(); itr.hasNext();) {
			IAbstractPin pin = itr.getNext();
			IPinList icxConnector = pin.getOwner();
			if (icxConnector instanceof IConnector && icxConnector instanceof IInterconnectObject) {
				connectors.add(icxConnector);
			}
		}
		return connectors;
	}

	private void accumulateFromConnector(IConnector icxConnector)
	{
		assert icxConnector.getNumPins() == 1;
		add(icxConnector);
		IAbstractPinIterator pinItr = icxConnector.getPins();
		if (pinItr.hasNext()) {
			IAbstractPin pin = pinItr.getNext();
			for (IConductorIterator condItr = pin.getConductors(); condItr.hasNext();) {
				IConductor icxConductor = condItr.getNext();
				if (icxConductor instanceof IInterconnectConductor && !contains(icxConductor)) {
					accumulateFromConductor((IInterconnectConductor) icxConductor);
				}
			}
		}
	}

	public static class OverconnectedInterconnectException extends RuntimeException
	{

		OverconnectedInterconnectException(IInterconnectConductor icxConductor)
		{
			super(icxConductor.getName());
		}
	}

	public static class UnderconnectedInterconnectException extends RuntimeException
	{

		UnderconnectedInterconnectException(IInterconnectConductor icxConductor)
		{
			super(icxConductor.getName());
		}
	}
}
