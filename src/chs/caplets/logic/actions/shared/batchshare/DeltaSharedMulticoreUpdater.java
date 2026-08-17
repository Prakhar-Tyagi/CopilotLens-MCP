/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.SharedPinListHelper;
import chs.common.IUIDObject;
import chs.system.FactoryMgr;
import chs.utility.attr.AttributeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Mapping checker that also handles expansion of shared multicores during delta batch share.
 * When the local multicore has conductors that don't exist in the shared multicore,
 * this checker expands the shared multicore to include them and records the mapping.
 */
public class DeltaSharedMulticoreUpdater extends AbstractSharedMulticoreMappingChecker
{
	@NotNull private final ISharedConductorMgr m_sharedConductorMgr;

	public DeltaSharedMulticoreUpdater(@NotNull ISharedConductorMgr sharedConductorMgr)
	{
		m_sharedConductorMgr = sharedConductorMgr;
	}

	/**
	 * Always returns true — allows the local multicore to have more conductors than the shared multicore,
	 * since unmatched conductors will be expanded via {@link #handleUnmatchedConductor}.
	 */
	@Override public boolean matchChildren(IMulticore multicore, ISharedMulticore sharedMulticore)
	{
		return true;
	}

	@Override protected boolean handleNoMulticoreShield(IMulticore multicore, ISharedConductor sharedShield)
	{
		return true;
	}

	/**
	 * Expands the shared multicore by creating a new shared conductor for the unmatched local conductor,
	 * so that the mapping is populated correctly before being passed to BulkAutoShareIntoCmd.
	 */
	@Override @Nullable public ISharedConductor handleUnmatchedConductor(@NotNull IConductor conductor,
			@NotNull ISharedMulticore sharedMulticore, @Nullable Map<IUIDObject, ISharedObject> mapping)
	{
		ISharedConductor sharedCond = FactoryMgr.getSharedFactory()
				.createSharedConductor(FactoryMgr.createUID());
		sharedCond.setName(conductor.getName());
		sharedCond.setType(conductor.getType());
		sharedCond.setRevision(sharedMulticore.getRevision());
		AttributeUtils.copyAttributes(conductor, sharedCond);
		SharedPinListHelper.copyProperties(conductor, sharedCond);
		m_sharedConductorMgr.addSharedConductor(sharedCond);
		sharedMulticore.addConductor(sharedCond);
		// Persist the newly created shared conductor to the database with its parent (shared conductor manager).
		// Without this, the SHAREDCONDUCTORMANAGER_ID column will be null, causing a ConstraintViolationException.
		sharedCond.flushNew(m_sharedConductorMgr.getObjType(), m_sharedConductorMgr);
		if (mapping != null) {
			mapping.put(conductor, sharedCond);
		}
		return sharedCond;
	}
}


