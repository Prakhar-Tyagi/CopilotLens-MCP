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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ILayoutDesignSyncStateManager
{

	void recordObjectToBeDeleted(@NotNull IUID uidToBeDeleted);

	@NotNull Collection<IUID> getObjectUIDsToBeDeleted();

	void recordExistingSourceObjectRef(@NotNull ISourceObjectRef existingSourceObjectRef);

	void recordSourceObjectRefToBeRemoved(@NotNull ISourceObjectRef sourceObjectRef);

	@NotNull Set<ISourceObjectRef> getSourceObjectRefsToBeRemoved();

	@NotNull Collection<IUID> getExistingLayoutObjects();

	void recordSchemsToBeDeleted(@NotNull List<IDesignSharedUsage> usages);

	@NotNull Set<IUID> getDiagramsToProcessForSchemDeletion();

	@NotNull Set<IUID> getSchemsForDeletion(@NotNull IUID diagramUID);

	@Nullable ISourceObjectRef getExistingSourceObjectRef(@NotNull IUID sourceDesignUID, @NotNull IUID sourceObjectUID);

	@NotNull Collection<ISourceObjectRef> getExistingSourceObjectRefs(@NotNull IUID sourceDesignUID);

	void recordSourceDesignTimeStamp(@NotNull IUID designUID, long timeModified);

	@Nullable Long getSourceDesignTimeStamp(@Nullable IUID sourceDesignUID);

	void recordUIDToBeReused(@NotNull IUID reconstructedObject);

	void recordReusedUIDs(@NotNull Set<IUID> usedUIDs);

	@NotNull Set<IUID> getUnusedUIDs();

	@NotNull Set<ILogicDesign> getDesignsToSyncFrom();

	void setDesignsToSyncFrom(@NotNull Set<ILogicDesign> designsToSyncFrom);
}
