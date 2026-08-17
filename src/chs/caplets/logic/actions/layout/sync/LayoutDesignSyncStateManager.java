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
import chs.cof.logical.ISourceObjectRef;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.common.IUID;
import chs.utilities.ListSet;
import chs.utilities.MapMap;
import chs.utilities.SetMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LayoutDesignSyncStateManager implements ILayoutDesignSyncStateManager
{

	@NotNull private final Set<ISourceObjectRef> mSourceObjectRefsToBeRemoved = new HashSet<>();
	@NotNull private final List<IUID> mObjUIDsToBeDeleted = new ListSet<>();
	@NotNull private final Set<IUID> mToBeReusedUIDs = new HashSet<>();
	@NotNull private final SetMap<IUID, IUID> mDiagramToSchemsForDeletion = new SetMap<>();
	@NotNull private final MapMap<IUID, IUID, ISourceObjectRef> mExistingSourceObjectRefMap = new MapMap<>();
	@NotNull private final SetMap<IUID, IUID> mExistingLayoutObjToSourceUIDs = new SetMap<>();
	@NotNull private final Map<IUID, Long> mSourceDesignTimeStamps = new HashMap<>();
	@NotNull private Set<ILogicDesign> mDesignsToSyncFrom = new HashSet<>();

	@Override public void recordObjectToBeDeleted(@NotNull IUID uidToBeDeleted)
	{
		mObjUIDsToBeDeleted.add(uidToBeDeleted);
	}

	@NotNull @Override public Collection<IUID> getObjectUIDsToBeDeleted()
	{
		return Collections.unmodifiableList(mObjUIDsToBeDeleted);
	}

	@Override
	public void recordExistingSourceObjectRef(@NotNull ISourceObjectRef existingSourceObjectRef)
	{
		final IUID referrerObjectUID = existingSourceObjectRef.getReferrerObjectUID();
		final IUID srcDesignUID = existingSourceObjectRef.getSourceDesignUID();
		final IUID sourceObjectUID = existingSourceObjectRef.getSourceObjectUID();
		if (sourceObjectUID != null && srcDesignUID != null) {
			mExistingSourceObjectRefMap.put(srcDesignUID, sourceObjectUID, existingSourceObjectRef);
			mExistingLayoutObjToSourceUIDs.add(referrerObjectUID, sourceObjectUID);
		}
		assert sourceObjectUID != null && srcDesignUID != null;
	}

	@Nullable
	public ISourceObjectRef getExistingSourceObjectRef(@NotNull IUID sourceDesignUID, @NotNull IUID sourceObjectUID)
	{
		return mExistingSourceObjectRefMap.get(sourceDesignUID, sourceObjectUID);
	}

	@NotNull public Collection<ISourceObjectRef> getExistingSourceObjectRefs(@NotNull IUID sourceDesignUID)
	{
		final Map<IUID, ISourceObjectRef> sourceObjRefsMap = mExistingSourceObjectRefMap.pullMap(sourceDesignUID);
		if (sourceObjRefsMap == null || sourceObjRefsMap.isEmpty()) {
			return Collections.emptySet();
		}
		return sourceObjRefsMap.values();
	}

	@Override public void recordSourceDesignTimeStamp(@NotNull IUID designUID, long timeModified)
	{
		mSourceDesignTimeStamps.put(designUID, timeModified);
	}

	@Nullable @Override public Long getSourceDesignTimeStamp(@Nullable IUID sourceDesignUID)
	{
		if (sourceDesignUID != null) {
			return mSourceDesignTimeStamps.get(sourceDesignUID);
		}
		return null;
	}

	@Override public void recordSourceObjectRefToBeRemoved(@NotNull ISourceObjectRef sourceObjectRef)
	{
		mSourceObjectRefsToBeRemoved.add(sourceObjectRef);
	}

	@NotNull @Override public Set<ISourceObjectRef> getSourceObjectRefsToBeRemoved()
	{
		return Collections.unmodifiableSet(mSourceObjectRefsToBeRemoved);
	}

	@NotNull public Collection<IUID> getExistingLayoutObjects()
	{
		return Collections.unmodifiableSet(mExistingLayoutObjToSourceUIDs.keySet());
	}

	@Override public void recordSchemsToBeDeleted(@NotNull List<IDesignSharedUsage> usages)
	{
		for (IDesignSharedUsage usage : usages) {
			final IUID diagramUID = usage.getDiagramUID();
			final IUID diagramObjectUID = usage.getDiagramObjectUID();
			mDiagramToSchemsForDeletion.add(diagramUID, diagramObjectUID);
		}
	}

	@NotNull @Override public Set<IUID> getDiagramsToProcessForSchemDeletion()
	{
		return Collections.unmodifiableSet(mDiagramToSchemsForDeletion.keySet());
	}

	@NotNull @Override public Set<IUID> getSchemsForDeletion(@NotNull IUID diagramUID)
	{
		return mDiagramToSchemsForDeletion.pullReadOnlySafeSet(diagramUID);
	}

	public void recordUIDToBeReused(@NotNull IUID reconstructedObject)
	{
		mToBeReusedUIDs.add(reconstructedObject);
	}

	@Override public void recordReusedUIDs(@NotNull Set<IUID> usedUIDs)
	{
		mToBeReusedUIDs.removeAll(usedUIDs);
	}

	@NotNull @Override public Set<IUID> getUnusedUIDs()
	{
		return Collections.unmodifiableSet(mToBeReusedUIDs);
	}

	@NotNull @Override public Set<ILogicDesign> getDesignsToSyncFrom()
	{
		return Collections.unmodifiableSet(mDesignsToSyncFrom);
	}

	@Override public void setDesignsToSyncFrom(@NotNull Set<ILogicDesign> designsToSyncFrom)
	{
		mDesignsToSyncFrom.clear();
		mDesignsToSyncFrom.addAll(designsToSyncFrom);
	}
}
