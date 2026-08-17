/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration;

import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.utility.IMessageCollectorAndReporter;
import chs.utility.Replicator;
import org.jetbrains.annotations.Nullable;

/**
 * Uses {@link Replicator} to replicate connector and pin
 */
class MatedPinListReplicator implements IMatedPinListReplicator
{

	protected ISchemDiagram m_activeDiagram;
	protected IMessageCollectorAndReporter m_messageReporter;

	MatedPinListReplicator(ISchemDiagram activeDiagram, IMessageCollectorAndReporter messageReporter)
	{
		m_activeDiagram = activeDiagram;
		m_messageReporter = messageReporter;
	}

	@Nullable public IPinList replicatePinList(IPinList cablePinList)
	{
		Replicator replicator = new Replicator();
		IPinList replicatedPinList = replicator.replicatePinListConnectivity(cablePinList, true);
		//noinspection ConstantConditions
		m_activeDiagram.getDesign().getConnectivity().addPinList(replicatedPinList);
		return replicatedPinList;
	}

	@Nullable public IAbstractPin replicatePin(IAbstractPin cablePin, IPinList cablePinList,
			IPinList replicatedPinList)
	{
		Replicator replicator = new Replicator();
		IAbstractPin replicatedPin = (IAbstractPin) replicator.replicatePin(cablePinList, replicatedPinList, cablePin);
		return replicatedPin;
	}
}
