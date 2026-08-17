/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.common.IUIDObject;

import java.util.Map;

/**
 *  Abstract base class for checking multicore to shared multicore mappings.
 */
public abstract class AbstractSharedMulticoreMappingChecker implements ISharedMulticoreMappingChecker
{
	public final boolean mapShields(IMulticore multicore, ISharedMulticore sharedMulticore,
			Map<IUIDObject, ISharedObject> mapping)
	{
		IShieldConductor shield = multicore.getShield();
		ISharedConductor sharedShield = sharedMulticore.getShield();

		// Delegate "shield == null" behavior to subclass
		if (shield == null) {
			return handleNoMulticoreShield(multicore, sharedShield);
		}

		// Shared shield missing
		if (sharedShield == null) {
			return false;
		}

		// Both shields exist
		if (mapping != null) {
			mapping.put(shield, sharedShield);
		}
		return true;
	}

	/**
	 * Subclasses decide what to do when multicore has no shield.
	 */
	protected abstract boolean handleNoMulticoreShield(IMulticore multicore, ISharedConductor sharedShield);
}
