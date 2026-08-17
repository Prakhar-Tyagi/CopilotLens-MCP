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
import chs.cof.logical.cable.IOverbraid;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedMulticore;

/**
 *  Default implementation of AbstractSharedMulticoreMappingChecker
 */
public class DefaultSharedMulticoreMappingChecker extends AbstractSharedMulticoreMappingChecker
{
	@Override public boolean matchChildren(IMulticore multicore, ISharedMulticore sharedMulticore)
	{
		return multicore.getNumConductors() == sharedMulticore.getNumConductors() &&
				multicore.getNumMulticores() == sharedMulticore.getNumMulticores();
	}

	@Override protected boolean handleNoMulticoreShield(IMulticore multicore, ISharedConductor sharedShield)
	{
		return sharedShield == null || IOverbraid.class.isInstance(multicore);
	}
}
