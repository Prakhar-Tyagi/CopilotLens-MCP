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
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Group to represent set of objects to be merged to single shared object
 */
public interface IShareableObjectGroup
{

	@NotNull ShareableEntityTypeEnum getType();

	@NotNull List<IObjectInfo> getShareableObjectInfos();

	void setCandidateTargetSharedObjects(@NotNull Set<ISharedObject> targetSharedObjects);

	@NotNull Set<ISharedObject> getTargetSharedObjects();

	int getShareableObjectsNum();

	@NotNull IObjectInfo getRepresentativeObjectInfo();

	void setRepresentativeObjectInfo(@NotNull String representativeUID);

	@NotNull IObjectInfo findObjectInfoByUID(String uid);

	@NotNull ShareabilityStatus validate();
}
