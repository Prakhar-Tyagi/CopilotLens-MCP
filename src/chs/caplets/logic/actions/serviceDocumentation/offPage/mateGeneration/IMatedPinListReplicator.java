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
import org.jetbrains.annotations.Nullable;

/**
 * interface to replicate connector and pin
 */
public interface IMatedPinListReplicator
{

	@Nullable IPinList replicatePinList(IPinList cablePinList);

	@Nullable IAbstractPin replicatePin(IAbstractPin cablePin, IPinList cablePinList, IPinList replicatedPinList);
}
