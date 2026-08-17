/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2020-2023 Siemens
 */

package chs.caplets.logic.actions.layout.sync;

import chs.caf.CAFUtils;
import chs.caf.helpers.CAFSharedUpdater;
import chs.caplets.logic.actions.layout.sync.rules.CheckUnsavedDesignChangesRule;
import chs.caplets.logic.actions.layout.sync.rules.CleanupDeletedLayoutObjectRefRule;
import chs.caplets.logic.actions.layout.sync.rules.CollectSchematicObjectsToBeDeletedRule;
import chs.caplets.logic.actions.layout.sync.rules.DeleteDissociatedCableObjectsRule;
import chs.caplets.logic.actions.layout.sync.rules.DeleteDissociatedSchematicObjectsRule;
import chs.caplets.logic.actions.layout.sync.rules.DetermineDissociatedSourceObjectRefRule;
import chs.caplets.logic.actions.layout.sync.rules.DetermineExistingLayoutObjectsToSyncRule;
import chs.caplets.logic.actions.layout.sync.rules.DissociateDeletedSourceObjectsRule;
import chs.caplets.logic.actions.layout.sync.rules.IdentifyModifiedDesignsRule;
import chs.caplets.logic.actions.layout.sync.rules.PrepareForSchematicSyncRule;
import chs.caplets.logic.actions.layout.sync.rules.RecordReusableLayoutObjectsRule;
import chs.caplets.logic.actions.layout.sync.rules.RecordSourceDesignTimeStampsRule;
import chs.caplets.logic.actions.layout.sync.rules.ReplicateLogicalObjectsRule;
import chs.caplets.logic.actions.layout.sync.rules.ReportDuplicateNamedObjectsRule;
import chs.caplets.logic.actions.layout.sync.rules.SyncMissingSourceDesignsRule;
import chs.caplets.logic.actions.layout.sync.rules.SynchronizeDiagramsRule;
import chs.caplets.logic.actions.layout.sync.rules.UpdateSchemDevicePhysicalDimensionsRule;
import chs.caplets.logic.actions.layout.sync.rules.UpdateSourceDesignTimeStampsRule;
import chs.caplets.logic.actions.layout.sync.rules.ValidateAndFixLayoutDesignRule;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.ILayoutDesignMgr;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.ISourceObjectRef;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.RefreshHelper;
import chs.cof.project.IProject;
import chs.cof.topology.IBaseIntegrationDesign;
import chs.common.ICommandHelper;
import chs.common.IDesignDescriptor;
import chs.common.IReadOnlyTimeStampedObject;
import chs.common.IUID;
import chs.common.sync.AbstractBaseSync;
import chs.common.sync.AbstractFunctionalSyncReporter;
import chs.common.sync.IAssociatedDesignFilterProcessor;
import chs.common.sync.ISyncListener;
import chs.common.sync.ISyncRule;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import chs.utility.IReplicationListener;
import chs.utility.helpers.UtilsHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class AbstractLayoutDesignSync extends AbstractBaseSync<ILayoutLogicDesign>
{

	private static Collection<ISyncListener> syncListeners = new ArrayList<>();
	@NotNull private final ILayoutDesignSyncStateManager mLayoutDesignSyncStateMgr;
	@NotNull private final SyncConnectivityReplicationHelper mReplicationHandler;
	@NotNull private final SyncReconstructHierarchyProvider mHierarchyProvider;
	@NotNull private final SchematicSyncController mDiagramSyncController;

	/**
	 * @param theDesign Design that will be modified
	 * @param theCommandHelper Abstract CommandHelper to give access to framework implementation
	 */
	protected AbstractLayoutDesignSync(@NotNull ILayoutLogicDesign theDesign, @NotNull ICommandHelper theCommandHelper)
	{
		super(theDesign, theCommandHelper);
		mLayoutDesignSyncStateMgr = new LayoutDesignSyncStateManager();
		mReplicationHandler = new SyncConnectivityReplicationHelper(theDesign, this::isFiltered);
		mHierarchyProvider = new SyncReconstructHierarchyProvider();
		mDiagramSyncController = new SchematicSyncController(theDesign);
	}

	public static void addSyncListener(@NotNull ISyncListener listener)
	{
		syncListeners.add(listener);
	}

	public static void removeSyncListener(@NotNull ISyncListener listener)
	{
		syncListeners.remove(listener);
	}

	@NotNull public ILayoutLogicDesign getDesign()
	{
		return mDesign;
	}

	@NotNull protected ILayoutDesignMgr getLayoutDesignMgr()
	{
		return getDesign().getLayoutDesignMgr();
	}

	@NotNull @Override protected Collection<ISyncListener> getSyncListeners()
	{
		return syncListeners;
	}

	@Override
	@NotNull
	protected IAssociatedDesignFilterProcessor createAssocDesignFilterProcessor()
	{
		return new LayoutAssociateDesignFilterProcessor();
	}

	@Override protected void buildRules()
	{
		final List<ISyncRule<ILayoutLogicDesign>> rules = getRules();
		buildPreparatoryReadOnlyRules(rules);

		rules.add(new ReplicateLogicalObjectsRule(this));

		buildPostSyncEditRules(rules);
		rules.add(new UpdateSourceDesignTimeStampsRule(this));
		rules.add(new ReportDuplicateNamedObjectsRule(this));
	}

	private void buildPreparatoryReadOnlyRules(@NotNull List<ISyncRule<ILayoutLogicDesign>> rules)
	{
		rules.add(new SyncMissingSourceDesignsRule(this));
		doAddAssociateDesignsRules(rules);
		rules.add(new CheckUnsavedDesignChangesRule(this));
		rules.add(new IdentifyModifiedDesignsRule(this));
		rules.add(new RecordSourceDesignTimeStampsRule(this));
		rules.add(new CleanupDeletedLayoutObjectRefRule(this));
		rules.add(new DetermineDissociatedSourceObjectRefRule(this));
		rules.add(new DissociateDeletedSourceObjectsRule(this));
		rules.add(new DetermineExistingLayoutObjectsToSyncRule(this));
		rules.add(new CollectSchematicObjectsToBeDeletedRule(this));
		rules.add(new PrepareForSchematicSyncRule(this));
		rules.add(new RecordReusableLayoutObjectsRule(this));
	}

	private void buildPostSyncEditRules(@NotNull List<ISyncRule<ILayoutLogicDesign>> rules)
	{
		rules.add(new DeleteDissociatedSchematicObjectsRule(this));
		rules.add(new DeleteDissociatedCableObjectsRule(this));
		rules.add(new ValidateAndFixLayoutDesignRule(this));
		rules.add(new SynchronizeDiagramsRule(this));
		rules.add(new UpdateSchemDevicePhysicalDimensionsRule(this));
	}

	protected abstract void doAddAssociateDesignsRules(@NotNull List<ISyncRule<ILayoutLogicDesign>> rules);

	@NotNull protected abstract Collection<IDesignDescriptor> getEffectivelyAssociatedDesigns();

	@Override public boolean execute()
	{
		boolean result;
		try {
			UtilsHelper.transcribe("Successfully started sync on: " + mDesign.getFullName());
			refreshDesignsAndSharedObjects();
			result = doExecute();
		}
		finally {
			UtilsHelper.transcribe("Successfully completed sync on: " + mDesign.getFullName());
		}

		notifyListeners(mDesign);
		return result;
	}

	private void refreshDesignsAndSharedObjects()
	{
		refreshSharedObjects();
		refreshDesigns();
	}

	protected void refreshDesigns()
	{
		final Collection<IDesignDescriptor> associatedDesignDescritors = getEffectivelyAssociatedDesigns();
		if (!associatedDesignDescritors.isEmpty()) {
			Set<ILogicDesign> designContainersToRefresh = associatedDesignDescritors.stream()
					.map(designDescriptor -> designDescriptor.getDesignContainer())
					.map(d -> CommonUtils.cast(d, ILogicDesign.class))
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());
			RefreshHelper.batchRefreshDesigns(designContainersToRefresh);
		}
	}

	private void refreshSharedObjects()
	{
		IProject project = getDesign().getProject();
		// Refresh shared objects.
		if (project != null) {
			CAFSharedUpdater sr = new CAFSharedUpdater(project, CAFUtils.getInstance().getWindowMgr());
			sr.updateSharedMgrs();
		}
	}

	@Override
	protected boolean markModifiedDesignsUpToDate(@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		// Timestamps are updated by UpdateSourceDesignTimeStampsRule
		return false;
	}

	@NotNull @Override protected List<ISyncRule<ILayoutLogicDesign>> createRules()
	{
		return new ArrayList<ISyncRule<ILayoutLogicDesign>>();
	}

	@NotNull @Override protected AbstractFunctionalSyncReporter<ILayoutLogicDesign> createReporter()
	{
		return new LayoutDesignSyncBufferedReporter(mDesign, true);
	}

	@NotNull public ILayoutDesignSyncStateManager getSyncStateManager()
	{
		return mLayoutDesignSyncStateMgr;
	}

	public void recordExistingSourceToTarget(@NotNull IUID sourceDesignID)
	{
		for (ISourceObjectRef existingSourceObjectRef : getSyncStateManager()
				.getExistingSourceObjectRefs(sourceDesignID)) {

			final IUID sourceObjectUID = existingSourceObjectRef.getSourceObjectUID();
			if (sourceObjectUID != null) {
				final IUID layoutObjectUID = existingSourceObjectRef.getReferrerObjectUID();
				mReplicationHandler.recordExistingSourceToTarget(sourceObjectUID, layoutObjectUID);
				recordSourceChildToTargetChild(existingSourceObjectRef);
			}
		}
	}

	private void recordSourceChildToTargetChild(@NotNull ISourceObjectRef sourceObjectRef)
	{
		for (Map.Entry<IUID, IUID> childObjectMapping : sourceObjectRef.getChildObjectMappings()) {
			mReplicationHandler
					.recordExistingSourceToTarget(childObjectMapping.getKey(), childObjectMapping.getValue());
		}
	}

	public void replicateConnectivity(@NotNull ILogicDesign sourceLogicDesign)
	{
		mReplicationHandler.replicateConnectivity(sourceLogicDesign);
	}

	public void addReplicationListener(@NotNull IReplicationListener listener)
	{
		mReplicationHandler.addReplicationListener(listener);
	}

	public void removeReplicationListener(@NotNull IReplicationListener listener)
	{
		mReplicationHandler.removeReplicationListener(listener);
	}

	public void resetReplicator()
	{
		mReplicationHandler.resetReplicator();
	}

	@NotNull public SyncReconstructHierarchyProvider getReconstrutHierarchyProvider()
	{
		return mHierarchyProvider;
	}

	@Nullable public IUID getSourceObject(@NotNull IUID referrerObjUID)
	{
		return mReplicationHandler.getSourceObject(referrerObjUID);
	}

	public void recordReusedUIDs()
	{
		getSyncStateManager().recordReusedUIDs(mReplicationHandler.getUsedUIDs());
	}

	public void prepareForDiagramSync()
	{
		mDiagramSyncController.prepare();
	}

	public void runDiagramSync(@NotNull Set<IUID> unusedUIDs, @NotNull Consumer<IDiagramObject> callback)
	{
		mDiagramSyncController.run(unusedUIDs, callback);
	}

	public boolean hasSharedObjectChanged(@NotNull ISourceObjectRef sourceObjectRef)
	{
		final IUID sharedObjectUID = sourceObjectRef.getSharedObjectUID();
		if (sharedObjectUID != null) {
			final ISharedObject sharedObject = UIDMgr.getObjectOfType(sharedObjectUID, ISharedObject.class);
			if (sharedObject == null) {
				return true;
			}
			final IReadOnlyTimeStampedObject timeStampedObject =
					CommonUtils.cast(sharedObject, IReadOnlyTimeStampedObject.class);
			return timeStampedObject != null &&
					timeStampedObject.getTimeModified() != sourceObjectRef.getSharedRefTimestamp();
		}
		return false;
	}
}
