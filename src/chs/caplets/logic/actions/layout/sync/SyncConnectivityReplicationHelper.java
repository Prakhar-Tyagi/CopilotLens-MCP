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

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.DesignContent;
import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.IReadOnlyDesignContent;
import chs.cofUtils.BasicDesignStitchSession;
import chs.cofUtils.IDesignStitchSession;
import chs.common.IObjectFilter;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.utilities.Environment;
import chs.utility.logic.StitchedConnectivityReplicationHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class SyncConnectivityReplicationHelper extends StitchedConnectivityReplicationHelper<SyncConnectivityReplicator>
{

	@NotNull private IObjectFilter<IUIDObject> mConnectivityFilter;
	private static boolean mShouldReplicateHighways = false;

	public SyncConnectivityReplicationHelper(@NotNull ILogicDesign targetDesign,
			@NotNull IObjectFilter<IUIDObject> connectivityFilter)
	{
		super(targetDesign, new BasicDesignStitchSession());
		mConnectivityFilter = connectivityFilter;
	}

	public static void toggleHighwayReplication(boolean isHWReplicationEnabled)
	{
		if (Environment.isUnitTest()) {
			// Highway replication is currently disabled in production code. Might be needed in future!
			mShouldReplicateHighways = isHWReplicationEnabled;
		}
	}

	@NotNull @Override protected IReadOnlyDesignContent createDesignContent(IConnectivity srcConnectivity)
	{
		return new DesignContent(srcConnectivity);
	}

	@NotNull @Override
	protected SyncConnectivityReplicator createReplicator(@NotNull IDesignStitchSession stitchSession)
	{
		return new SyncConnectivityReplicator(stitchSession);
	}

	@Override protected void replicateDesignContent(@NotNull IReadOnlyDesignContent information)
	{
		replicateCablePinlists(information.getPinLists(true, false));
		replicateConductors(information.getConductors());
		replicateMulticores(information.getMulticores(false));
		if (mShouldReplicateHighways) {
			replicateHighways(information.getHighways());
		}
		replicateAssemblies(information.getAssemblies());
	}

	private void replicateHighways(@NotNull Set<IHighway> highways)
	{
		for (IHighway highway : highways) {
			if (!isFiltered(highway)) {
				final IHighway replicatedHighway = mReplicator.replicateHighway(highway);
				objectReplicated(highway, replicatedHighway);
			}
		}
	}

	private void replicateAssemblies(@NotNull Set<IAssembly> assemblies)
	{
		for (IAssembly assembly : assemblies) {
			if (!isFiltered(assembly)) {
				final IAssembly replicatedConductor = mReplicator.replicateAssembly(assembly);
				objectReplicated(assembly, replicatedConductor);
			}
		}
	}

	public void recordExistingSourceToTarget(@NotNull IUID sourceObjeUID, @NotNull IUID layoutObjectUID)
	{
		mReplicator.recordExistingSourceToTargetUID(sourceObjeUID, layoutObjectUID);
	}

	@NotNull public Set<IUID> getUsedUIDs()
	{
		return mReplicator.getUsedUIDs();
	}

	@Nullable public IUID getSourceObject(@NotNull IUID referrerObjUID)
	{
		return mReplicator.getOldObjectUID(referrerObjUID);
	}

	private void replicateMulticores(@NotNull Set<IMulticore> multicores)
	{
		for (IMulticore multicore : multicores) {
			if (!isFiltered(multicore)) {
				final IMulticore replicatedConductor =
						mReplicator.replicateMulticoreOrOverbraid(multicore, true, true, null);
				objectReplicated(multicore, replicatedConductor);
			}
		}
	}

	private void replicateConductors(@NotNull Set<IConductor> conductors)
	{
		for (IConductor conductor : conductors) {
			if (!isFiltered(conductor)) {
				final IConductor replicatedConductor = mReplicator.replicateConductor(conductor);
				objectReplicated(conductor, replicatedConductor);
			}
		}
	}

	private void replicateCablePinlists(@NotNull Set<IPinList> pinLists)
	{
		for (IPinList pinList : pinLists) {
			if (!isFiltered(pinList)) {
				final IPinList replicatedPinlist =
						mReplicator.replicatePinListConnectivity(pinList, false, false, true);
				if (replicatedPinlist != null) {
					objectReplicated(pinList, replicatedPinlist);
				}
			}
		}
	}

	private boolean isFiltered(@Nullable IUIDObject uidObect)
	{
		if (uidObect == null) {
			return true;
		}
		return mConnectivityFilter.accept(uidObect);
	}
}
