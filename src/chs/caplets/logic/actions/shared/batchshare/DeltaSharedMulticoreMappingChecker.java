/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedMulticore;

/**
 *  implementation of AbstractSharedMulticoreMappingChecker in case of apply delta action
 *  here we allow sharing multicore into shared multicore with equal or greater number of conductors and multicore
 *  and allow sharing into shared multicore without shield.
 *  When the local multicore has conductors that don't exist in the shared multicore,
 *  the shared multicore is expanded to include them.
 */
public class DeltaSharedMulticoreMappingChecker extends AbstractSharedMulticoreMappingChecker
{
	@Override public boolean matchChildren(IMulticore multicore, ISharedMulticore sharedMulticore)
	{
		return multicore.getNumConductors() <= sharedMulticore.getNumConductors() &&
				multicore.getNumMulticores() <= sharedMulticore.getNumMulticores();
	}

	@Override protected boolean handleNoMulticoreShield(IMulticore multicore, ISharedConductor sharedShield)
	{
		return true;
	}
}
