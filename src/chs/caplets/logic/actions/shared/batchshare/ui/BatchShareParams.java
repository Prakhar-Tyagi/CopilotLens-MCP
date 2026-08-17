/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.caplets.logic.actions.shared.batchshare.IEntityShareCriteria;
import chs.caplets.logic.actions.shared.batchshare.IObjectInfo;
import chs.caplets.logic.actions.shared.batchshare.IShareableObjectGroup;
import chs.caplets.logic.actions.shared.batchshare.ShareabilityStatus;
import chs.caplets.logic.actions.shared.batchshare.ShareableObjectGroup;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class BatchShareParams implements IBatchShareParams
{

	private Map<IShareableObjectGroup, IBatchShareGroup> mObjectMap;

	public BatchShareParams(@NotNull Set<IShareableObjectGroup> shareableObjectGroups,
			@NotNull Set<IEntityShareCriteria> entitiesShareCriteria)
	{
		mObjectMap = new HashMap<>();
		shareableObjectGroups.stream().forEach(group -> {
			mObjectMap.put(group, new BatchShareGroup(group, entitiesShareCriteria));
		});
	}

	@Override @NotNull public Collection<IBatchShareRow> getData()
	{
		Set<IBatchShareRow> data = new HashSet<>();
		for (IBatchShareGroup grp : mObjectMap.values()) {
			data.addAll(grp.getBatchShareElements());
		}
		return data;
	}

	@Override @NotNull public Set<IShareableObjectGroup> retrieveObjectsFromUserSelection()
	{
		Set<IShareableObjectGroup> result = new HashSet<>();
		for (IShareableObjectGroup inputGroup : mObjectMap.keySet()) {
			if (inputGroup.validate() != ShareabilityStatus.VALID) {
				continue;
			}
			collectValidGroupElements(inputGroup, result);
		}
		return result;
	}

	private void collectValidGroupElements(IShareableObjectGroup inputGroup, Set<IShareableObjectGroup> result)
	{
		HashSet<IObjectInfo> modifiedObjectInfo = new HashSet<>();
		String anchorElementUID = null;

		IBatchShareGroup tableGroup = mObjectMap.get(inputGroup);
		for (IBatchShareRow rowElement : tableGroup.getBatchShareElements()) {
			if (rowElement.isSelected()) {
				modifiedObjectInfo.add(inputGroup.findObjectInfoByUID(rowElement.getObjectUID().getString()));
				if (rowElement.getAction() == Action.SHARE) {
					anchorElementUID = rowElement.getObjectUID().getString();
				}
			}
		}
		if (!modifiedObjectInfo.isEmpty()) {
			IShareableObjectGroup modifiedGroup = new ShareableObjectGroup(modifiedObjectInfo);
			if (anchorElementUID != null) {
				modifiedGroup.setRepresentativeObjectInfo(anchorElementUID);
			}
			modifiedGroup.setCandidateTargetSharedObjects(inputGroup.getTargetSharedObjects());
			result.add(modifiedGroup);
		}
	}
}
