/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.cof.logical.shared.ISharedObject;
import chs.utilities.CollectionUtils;
import chs.utility.helpers.revisioning.SharedObjectRevisionHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Group to represent set of objects to be merged to single shared object
 */
public class ShareableObjectGroup implements IShareableObjectGroup
{

	@NotNull private final List<IObjectInfo> m_shareableObjectInfos;
	@NotNull private final ShareableEntityTypeEnum m_type;
	@Nullable private Set<ISharedObject> m_targetSharedObjects;
	@NotNull private IObjectInfo m_representativeObjectInfo;
	@NotNull private final Map<String, IObjectInfo> mObjectInfoUIDMap;

	public ShareableObjectGroup(@NotNull Set<? extends IObjectInfo> shareableObjectInfos)
	{
		if (shareableObjectInfos.isEmpty()) {
			throw new IllegalArgumentException("Group must have atleast one shareable object");
		}

		m_shareableObjectInfos = shareableObjectInfos.stream().sorted().collect(Collectors.toList());
		m_representativeObjectInfo = m_shareableObjectInfos.get(0);
		m_type = m_representativeObjectInfo.getType();
		m_targetSharedObjects = null;
		mObjectInfoUIDMap = new HashMap<>();
		m_shareableObjectInfos.stream().forEach(info -> mObjectInfoUIDMap.put(info.getUID(), info));
	}

	@Override
	@NotNull public ShareableEntityTypeEnum getType()
	{
		return m_type;
	}

	@Override
	@NotNull public List<IObjectInfo> getShareableObjectInfos()
	{
		return Collections.unmodifiableList(m_shareableObjectInfos);
	}

	@Override
	public void setCandidateTargetSharedObjects(@NotNull Set<ISharedObject> targetSharedObjects)
	{
		m_targetSharedObjects = new HashSet<>(targetSharedObjects);
	}

	@Override
	@NotNull public Set<ISharedObject> getTargetSharedObjects()
	{
		return CollectionUtils.getSafeSet(m_targetSharedObjects);
	}

	@Override
	public int getShareableObjectsNum()
	{
		return m_shareableObjectInfos.size();
	}

	@Override
	@NotNull public IObjectInfo getRepresentativeObjectInfo()
	{
		return m_representativeObjectInfo;
	}

	@Override
	public void setRepresentativeObjectInfo(@NotNull String representativeUID)
	{
		m_representativeObjectInfo = m_shareableObjectInfos.stream()
				.filter(objInfo -> objInfo.getUID().equals(representativeUID))
				.findFirst().orElse(m_representativeObjectInfo);
	}

	@Override
	@NotNull public IObjectInfo findObjectInfoByUID(String uid)
	{
		return mObjectInfoUIDMap.get(uid);
	}

	@Override
	@NotNull public ShareabilityStatus validate()
	{
		Set<ISharedObject> targetSharedObjects = getTargetSharedObjects();
		if (targetSharedObjects.size() > 1) {
			return SharedObjectRevisionHelper.areBaseIDsTheSame(targetSharedObjects) ?
					ShareabilityStatus.MULTIPLE_TARGET_SHARED_OBJECT_REVISIONS :
					ShareabilityStatus.MULTIPLE_TARGET_SHARED_OBJECTS;
		}
		if (targetSharedObjects.size() == 1 && targetSharedObjects.iterator().next().isFrozen()) {
			return ShareabilityStatus.FROZEN_TARGET_SHARED_OBJECT;
		}
		return ShareabilityStatus.VALID;
	}
}
