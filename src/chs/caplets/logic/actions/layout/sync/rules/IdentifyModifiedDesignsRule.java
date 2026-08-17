/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2020-2023 Siemens
 */

package chs.caplets.logic.actions.layout.sync.rules;

import chs.caplets.logic.actions.layout.sync.AbstractLayoutDesignSync;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.ISourceDesignRef;
import chs.cof.logical.ISourceObjectRef;
import chs.cof.logical.shared.ISharedObject;
import chs.common.DesignUtils;
import chs.common.IUID;
import chs.common.sync.AbstractFunctionalSyncReporter;
import chs.common.sync.FunctionalSyncHelper;
import chs.system.UIDMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.SetMap;
import chs.utility.logic.DesignSharedObjectHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class IdentifyModifiedDesignsRule extends AbstractLayoutDesignSyncRule
{

	public IdentifyModifiedDesignsRule(@NotNull AbstractLayoutDesignSync sync)
	{
		super(sync);
	}

	@NotNull @Override protected String getMessageSourceResourceName()
	{
		return "IdentifyModifiedDesignsRule";
	}

	@Override protected boolean doExecute(@NotNull ILayoutLogicDesign design,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		final Set<ILogicDesign> designsToSyncFrom = new HashSet<>();
		final Set<ILogicDesign> designsThatChangedSinceLastSync = getDesignsThatChangedSinceLastSync(design);
		designsToSyncFrom.addAll(designsThatChangedSinceLastSync);
		designsToSyncFrom
				.addAll(getDesignsToSyncFromDueToSharedObjConnections(design, designsThatChangedSinceLastSync));
		getSync().getSyncStateManager().setDesignsToSyncFrom(designsToSyncFrom);
		for (ISourceDesignRef systemLogicDesignRef : design.getLayoutDesignMgr().getSystemLogicDesignRefs()) {
			final ILogicDesign referencedDesign = systemLogicDesignRef.getReferencedDesign();
			if (referencedDesign != null && !designsToSyncFrom.contains(referencedDesign)) {
				reporter.reportMessage("IdentifyModifiedDesignsRule.noChangeInDesign", referencedDesign.getFullName());
			}
		}
		return true;
	}

	@NotNull private Set<ILogicDesign> getDesignsThatChangedSinceLastSync(@NotNull ILayoutLogicDesign design)
	{
		final Set<ILogicDesign> designsToSyncFrom = new HashSet<>();
		for (ISourceDesignRef systemLogicDesignRef : design.getLayoutDesignMgr().getSystemLogicDesignRefs()) {
			final ILogicDesign referencedDesign = systemLogicDesignRef.getReferencedDesign();
			if (referencedDesign != null) {
				if (FunctionalSyncHelper.isDesignModified(referencedDesign, systemLogicDesignRef)) {
					designsToSyncFrom.add(referencedDesign);
				}
			}
		}
		return designsToSyncFrom;
	}

	@NotNull private Set<ILogicDesign> getDesignsToSyncFromDueToSharedObjConnections(@NotNull ILayoutLogicDesign design,
			@NotNull Set<ILogicDesign> changedDesigns)
	{
		SetMap<IUID, IUID> designToSharedEdges = buildDesignToSharedConnectionEdgeMap(design);
		if (designToSharedEdges.isEmpty()) {
			return Collections.emptySet();
		}
		final Set<ILogicDesign> designsToSyncFrom = new HashSet<>();
		for (ILogicDesign changedDesign : changedDesigns) {
			designsToSyncFrom
					.addAll(getAffectedDesignsDueToSharedConnections(changedDesign.getUID(), designToSharedEdges));
		}
		final Set<IUID> modifiedSharedObjectUIDs = getModifiedSharedObjectUIDs(design);
		for (IUID modifiedSharedObjectUID : modifiedSharedObjectUIDs) {
			designsToSyncFrom
					.addAll(getAffectedDesignsDueToSharedConnections(modifiedSharedObjectUID, designToSharedEdges));
		}
		return designsToSyncFrom;
	}

	@NotNull private SetMap<IUID, IUID> buildDesignToSharedConnectionEdgeMap(@NotNull ILayoutLogicDesign design)
	{
		SetMap<IUID, IUID> sharedToDesignEdge = new SetMap<>();
		for (ISourceDesignRef sourceDesignRef : design.getLayoutDesignMgr().getSystemLogicDesignRefs()) {
			final ILogicDesign referencedDesign = sourceDesignRef.getReferencedDesign();
			if (referencedDesign != null) {
				Set<ISharedObject> allSharedObjects = getSharedObjectsUsedInDesign(referencedDesign);
				for (ISharedObject sharedObject : allSharedObjects) {
					sharedToDesignEdge.add(sharedObject.getUID(), referencedDesign.getUID());
					sharedToDesignEdge.add(referencedDesign.getUID(), sharedObject.getUID());
				}
			}
		}
		return sharedToDesignEdge;
	}

	@NotNull private Set<ISharedObject> getSharedObjectsUsedInDesign(@NotNull ILogicDesign referencedDesign)
	{
		Set<ISharedObject> allSharedObjects = new HashSet<>();
		CollectionUtils.add(DesignSharedObjectHelper.getSharedPinLists(referencedDesign, false), allSharedObjects);
		CollectionUtils.add(DesignSharedObjectHelper.getSharedMulticores(referencedDesign, false), allSharedObjects);
		CollectionUtils
				.add(DesignSharedObjectHelper.getSharedConductors(referencedDesign, false, false), allSharedObjects);
		CollectionUtils.add(DesignSharedObjectHelper.getSharedHighways(referencedDesign, false), allSharedObjects);
		CollectionUtils.add(DesignSharedObjectHelper.getSharedSingleLines(referencedDesign, false), allSharedObjects);
		return allSharedObjects;
	}

	@NotNull private Set<IUID> getModifiedSharedObjectUIDs(@NotNull ILayoutLogicDesign design)
	{
		Set<IUID> changedSharedObjects = new HashSet<>();
		for (ISourceObjectRef sourceObjectRef : design.getLayoutDesignMgr().getAllSourceObjectRefs()) {
			if (getSync().hasSharedObjectChanged(sourceObjectRef)) {
				final IUID sharedObjectUID = sourceObjectRef.getSharedObjectUID();
				changedSharedObjects.add(sharedObjectUID);
			}
		}
		return changedSharedObjects;
	}

	@NotNull private Set<ILogicDesign> getAffectedDesignsDueToSharedConnections(@NotNull IUID startObject,
			SetMap<IUID, IUID> designSharedEdgeMap)
	{

		final Collection<IUID> affectedObjects =
				CollectionUtils.findConnectedObjectsThroughFlooding(startObject, designSharedEdgeMap);
		return affectedObjects.stream()
				.filter(uid -> UIDMgr.getDesignDescriptor(uid) != null)
				.map(uid -> DesignUtils.getDesign(uid, ILogicDesign.class))
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}
}
