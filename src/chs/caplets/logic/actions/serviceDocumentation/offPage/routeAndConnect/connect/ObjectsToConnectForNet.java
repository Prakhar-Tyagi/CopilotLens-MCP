/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.connect;

import chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.ISchemObjectsConnector;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IPin;

import java.util.Iterator;

public class ObjectsToConnectForNet extends ObjectsToConnectForConductor
{

	boolean foundObjectsToConnect()
	{
		return false;
	}

	public boolean connect(ISchemObjectsConnector schemObjectsConnector)
	{
		int scSize = m_segmentContainers.size();
		int pinSize = m_pins.size();
		if (scSize == 0 && pinSize == 0) {
			return true;
		}
		Iterator<IConductor> scIterator = m_segmentContainers.iterator();
		Iterator<IPin> pinIterator = m_pins.iterator();
		//connect the net conductor, pin pairs
		while (scIterator.hasNext() && pinIterator.hasNext()) {
			IConductor schem1 = scIterator.next();
			IPin pin = pinIterator.next();
			connectSegmentContainerAndPin(schem1, pin, schemObjectsConnector);
		}
		if (scSize > pinSize) {
			//only left with one or more segments to connect
			while (scIterator.hasNext()) {
				IConductor schem1 = scIterator.next();
				if (scIterator.hasNext()) {
					IConductor schem2 = scIterator.next();
					connectSegmentContainers(schem1, schem2, schemObjectsConnector);
				}
			}
		}
		else {
			//only left with one or more pins to connect
			while (pinIterator.hasNext()) {
				IPin pin1 = pinIterator.next();
				if (pinIterator.hasNext()) {
					IPin pin2 = pinIterator.next();
					schemObjectsConnector.connectSchemPins(pin1, pin2);
				}
			}
		}
		return true;
	}
}
