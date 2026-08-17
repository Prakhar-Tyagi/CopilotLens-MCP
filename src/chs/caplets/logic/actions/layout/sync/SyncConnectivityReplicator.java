/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout.sync;

import chs.cofUtils.IDesignStitchSession;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.utility.logic.StitchedConnectivityReplicator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class SyncConnectivityReplicator extends StitchedConnectivityReplicator
{

	@NotNull private final Set<IUID> mUsedUIDs;

	public SyncConnectivityReplicator(@NotNull IDesignStitchSession stitchedSession)
	{
		super(stitchedSession);
		mUsedUIDs = new HashSet<>();
	}

	public void recordExistingSourceToTargetUID(@NotNull IUID oldUID, @NotNull IUID newUID)
	{
		m_uidMaps.put(oldUID, newUID);
	}

	@Override public IUID getNewUID(IUID oldUID)
	{
		final IUID newUID = super.getNewUID(oldUID);
		mUsedUIDs.add(newUID);
		return newUID;
	}

	@Nullable @Override public IUIDObject getNewObject(@NotNull IUID oldUID)
	{
		final IUIDObject newObject = super.getNewObject(oldUID);
		if (newObject != null) {
			mUsedUIDs.add(newObject.getUID());
		}
		return newObject;
	}

	@NotNull public Set<IUID> getUsedUIDs()
	{
		return mUsedUIDs;
	}

	@Override public void reset()
	{
		super.reset();
		mUsedUIDs.clear();
	}
}
