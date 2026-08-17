/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration;

import chs.cof.logical.cable.IPinList;

/**
 *
 */
public interface IShareIntoExecutor
{

	void doShareInto(IPinList replicatedPinList, IPinList cablePinList);
}
