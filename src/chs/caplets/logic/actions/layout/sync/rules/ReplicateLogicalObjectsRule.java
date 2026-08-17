/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout.sync.rules;

import chs.caplets.logic.actions.layout.sync.AbstractLayoutDesignSync;
import chs.caplets.logic.actions.layout.sync.ILayoutDesignSyncStateManager;
import chs.caplets.logic.actions.layout.sync.SyncReconstructHierarchyProvider;
import chs.cof.COFTypeEnum;
import chs.cof.logical.ILayoutDesignMgr;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.ISourceDesignRef;
import chs.cof.logical.ISourceObjectRef;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.shared.ISharedObject;
import chs.common.IReadOnlyTimeStampedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.sync.AbstractFunctionalSyncReporter;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.ListMap;
import chs.utilities.Pair;
import chs.utilities.StringUtils;
import chs.utilities.stream.StreamUtils;
import chs.utility.IReplicationListener;
import chs.utility.helpers.ReferenceHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ReplicateLogicalObjectsRule extends AbstractLayoutDesignSyncRule implements IReplicationListener
{

	@Nullable private ReplicationHandler mHandler;

	public ReplicateLogicalObjectsRule(@NotNull AbstractLayoutDesignSync sync)
	{
		super(sync);
	}

	@NotNull @Override protected String getMessageSourceResourceName()
	{
		return "ReplicateLogicalObjectsRule";
	}

	@Override protected boolean doExecute(@NotNull ILayoutLogicDesign design,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		mHandler = new ReplicationHandler(design, reporter);
		final ILayoutDesignMgr layoutDesignMgr = design.getLayoutDesignMgr();

		final AbstractLayoutDesignSync sync = getSync();
		sync.addReplicationListener(this);
		final Set<ILogicDesign> designsToSyncFrom = sync.getSyncStateManager().getDesignsToSyncFrom();
		for (ISourceDesignRef sourceDesignRef : layoutDesignMgr.getSystemLogicDesignRefs()) {
			mHandler.setCurrentSourceDesignRef(sourceDesignRef);
			final ILogicDesign logicDesign = sourceDesignRef.getReferencedDesign();
			if (logicDesign != null && designsToSyncFrom.contains(logicDesign)) {
				sync.recordExistingSourceToTarget(logicDesign.getUID());
				sync.replicateConnectivity(logicDesign);
				sync.recordReusedUIDs();
				sync.resetReplicator();
				reporter.reportSyncChangesMade("ReplicateLogicalObjectsRule.completedReplication",
						logicDesign.getFullName());
			}
			mHandler.reportNewlyCreatedObjects();
			mHandler.reset();
		}
		sync.removeReplicationListener(this);
		return true;
	}

	@Override public void objectReplicated(@NotNull IUIDObject originalObject, @NotNull IUIDObject replicaObject)
	{
		final ILayoutDesignSyncStateManager syncStateMgr = getSync().getSyncStateManager();
		final Pair<ISourceObjectRef, Boolean> sourceObjectRefPair =
				getOrCreateSourceObjectRef(originalObject, replicaObject);
		if (sourceObjectRefPair != null) {
			final ISourceObjectRef sourceObjectRef = sourceObjectRefPair.getFirst();
			final boolean isNew = sourceObjectRefPair.getSecond();
			updateSharedReference(originalObject, sourceObjectRef);
			updateChildObjectReferences(sourceObjectRef, replicaObject);
			syncStateMgr.recordExistingSourceObjectRef(sourceObjectRef);
			if (mHandler != null) {
				mHandler.recordReplicatedObject(replicaObject, sourceObjectRef, isNew);
			}
		}
	}

	private void updateChildObjectReferences(@NotNull ISourceObjectRef sourceObjectRef,
			@NotNull IUIDObject replicaObject)
	{
		sourceObjectRef.clearChildMappings();
		final SyncReconstructHierarchyProvider reconstrutHierarchyProvider = getSync().getReconstrutHierarchyProvider();
		final List<IUID> allChildrenInHierarchy = reconstrutHierarchyProvider.getAllChildrenInHierarchy(replicaObject);
		for (IUID replicaChildUID : allChildrenInHierarchy) {
			final IUID sourceObjUID = getSync().getSourceObject(replicaChildUID);
			if (sourceObjUID != null) {
				sourceObjectRef.addSourceChildObjectMapping(sourceObjUID, replicaChildUID);
			}
		}
	}

	private void updateSharedReference(@NotNull IUIDObject sourceObject, @NotNull ISourceObjectRef sourceObjectRef)
	{
		final ILogicObject logicObject = CommonUtils.cast(sourceObject, ILogicObject.class);
		if (logicObject != null) {
			final ISharedObject sharedObject = logicObject.getSharedObject();
			sourceObjectRef.setSourceSharedObject(sharedObject);
			final IReadOnlyTimeStampedObject timeStamped =
					CommonUtils.cast(sharedObject, IReadOnlyTimeStampedObject.class);
			if (timeStamped != null) {
				sourceObjectRef.setTimestamp(timeStamped.getTimeModified());
			}
		}
	}

	@Nullable private Pair<ISourceObjectRef, Boolean> getOrCreateSourceObjectRef(@NotNull IUIDObject sourceObject,
			@NotNull IUIDObject referrerObject)
	{
		final ISourceDesignRef sourceDesignRef = mHandler != null ? mHandler.getCurrentSourceDesignRef() : null;
		if (sourceDesignRef != null) {
			final IUID sourceDesignUID = sourceDesignRef.getReferencedDesignUID();
			if (sourceDesignUID != null) {
				final ILayoutDesignSyncStateManager syncChangeHolder = getSync().getSyncStateManager();
				final ISourceObjectRef existingSourceObjectRef =
						syncChangeHolder.getExistingSourceObjectRef(sourceDesignUID, sourceObject.getUID());
				if (existingSourceObjectRef != null) {
					return new Pair<>(existingSourceObjectRef, false);
				}
			}

			final ISourceObjectRef newSourceObjectRef = FactoryMgr.getLogicalFactory()
					.createSourceObjectRef(referrerObject, sourceObject, sourceDesignRef);
			return new Pair<>(newSourceObjectRef, true);
		}
		return null;
	}

	private static class ReplicationHandler
	{

		@NotNull private final ILayoutLogicDesign mTargetDesign;
		@NotNull private final AbstractFunctionalSyncReporter<ILayoutLogicDesign> mReporter;
		@Nullable private ISourceDesignRef mCurrentSourceDesignRef;
		@NotNull private ListMap<String, IUID> mNewObjects = new ListMap<>();

		private ReplicationHandler(@NotNull ILayoutLogicDesign design,
				@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
		{
			mTargetDesign = design;
			mReporter = reporter;
		}

		private void reset()
		{
			setCurrentSourceDesignRef(null);
			mNewObjects.clear();
		}

		private void setCurrentSourceDesignRef(@Nullable ISourceDesignRef sourceDesignRef)
		{
			mCurrentSourceDesignRef = sourceDesignRef;
		}

		private void recordReplicatedObject(@NotNull IUIDObject replicaObject,
				@NotNull ISourceObjectRef sourceObjectRef, boolean isNew)
		{
			mTargetDesign.getLayoutDesignMgr().addSourceObjectReference(sourceObjectRef);
			if (isNew) {
				final String type = getObjectType(replicaObject);
				final ILogicObject logicObject = ReferenceHelper.reduceToLogicObject(replicaObject);
				if (logicObject != null && !(logicObject instanceof IDeviceConnector)) {
					mNewObjects.add(type, logicObject.getUID());
				}
			}
		}

		@NotNull private String getObjectType(@NotNull IUIDObject iuidObject)
		{
			return COFTypeEnum.getDisplayableTypeName(iuidObject);
		}

		@Nullable private ISourceDesignRef getCurrentSourceDesignRef()
		{
			return mCurrentSourceDesignRef;
		}

		@Nullable private ILogicDesign getCurrentSourceDesign()
		{
			return mCurrentSourceDesignRef != null ? mCurrentSourceDesignRef.getReferencedDesign() : null;
		}

		private void reportNewlyCreatedObjects()
		{
			for (Map.Entry<String, List<IUID>> newObjectsEntry : mNewObjects.entrySet()) {
				final String type = newObjectsEntry.getKey();
				final String newObjectNames = newObjectsEntry.getValue().stream()
						.map(uid -> ReferenceHelper.reduceToLogicObject(uid))
						.filter(StreamUtils::notNull)
						.map(lobj -> lobj.getName())
						.sorted()
						.collect(Collectors.joining(StringUtils.COMMA_SPACE));
				final ILogicDesign currentSourceDesign = getCurrentSourceDesign();
				final String designName =
						currentSourceDesign != null ? currentSourceDesign.getFullName() : StringUtils.BLANK;
				mReporter.reportSyncChangesMade("ReplicateLogicalObjectsRule.newObjectAdded",
						StringUtils.toLowerCase(type), designName, newObjectNames);
			}
		}
	}
}
