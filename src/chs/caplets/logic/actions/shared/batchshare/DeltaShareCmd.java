/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023-2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.logic.actions.shared.autoshare.AutoShareParams;
import chs.caplets.logic.commands.DeltaBulkAutoShareIntoCmd;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IRevisionedSharedObject;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedObjectMgr;
import chs.cof.logical.shared.ISharedObjectsFinder;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinListMgr;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.logical.shared.SharedObjectsFinder;
import chs.cof.project.IProject;
import chs.cofUtils.cmd.CHSCommand;
import chs.common.IDesignAbstraction;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.ctf.ui.form.sharedobjectrevisioning.SharedObjectRevisioningDialogHelper;
import chs.utilities.CommonUtils;
import chs.utilities.ListSet;
import chs.utilities.StringUtils;
import chs.utility.IMessageReporterWithContext;
import chs.utility.helpers.revisioning.FunctionModuleCodeForSharedCopyService;
import chs.utility.helpers.revisioning.SharedObjectRevisionHelper;
import chs.utility.ui.progress.ProgressGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cmd for trying to share/shareInto objects added by delta.
 */
public class DeltaShareCmd extends AbstractDeltaShareCmd
{

	@NotNull private ISharedObjectsFinder m_sharedObjectsFinder;
	//Objects added to the design using add delta, could be shared into if:
	//1. Target project contains a shared object with the same name and type
	//2. Source object is already shared
	@NotNull private Map<ILogicObject, ISharedObject> m_targetToSourceShared;
	//Objects that should be shared as they exist in multiple designs.
	@NotNull private Collection<ILogicObject> m_objectToBeShared;

	//Set with all the objects that are newly created
	@NotNull private Set<ISharedObject> m_newlyCreatedSharedObjs;
	@Nullable private IDesignAbstraction designAbstraction;

	public DeltaShareCmd(@NotNull IProject project, @NotNull ILogicDesign design,
			@NotNull ISharedObjectsFinder sharedObjectsFinder,
			@NotNull Map<ILogicObject, ISharedObject> targetToSourceShared,
			@NotNull Collection<ILogicObject> objectsToBeShared)
	{
		super(project, new HashSet<>(Set.of(design)), getObjectTypes(targetToSourceShared.keySet()));

		m_sharedObjectsFinder = sharedObjectsFinder;
		m_targetToSourceShared = targetToSourceShared;
		m_objectToBeShared = objectsToBeShared;
		m_newlyCreatedSharedObjs = new HashSet<>();
		setProgress(new ProgressGroup(StringUtils.EMPTY_STRING));
		designAbstraction = design.getDesignAbstraction();
	}

	@NotNull private static Collection<ShareableEntityTypeEnum> getObjectTypes(Collection<ILogicObject> objs)
	{
		return objs.stream()
				.map(SharedObjectsFinder::getShareableEntityType)
				.filter(type -> type != null)
				.collect(Collectors.toCollection(HashSet::new));
	}

	/**
	 * Executes the sharing process for delta operations.
	 * <p>
	 * If no target-to-source shared mapping exists, attempts to share objects by finding similar
	 * shared objects in the target. Otherwise, tries to match existing shared objects, clone source
	 * shared objects when needed, or create new shared objects when this is the first target design
	 * receiving them.
	 * </p>
	 * <p>
	 * Performs share/share-into operations accordingly and refreshes the shared objects view.
	 * </p>
	 */
	@Override protected void doExecute()
	{
		Set<ILogicObject> objectsToBeShared = new HashSet<>();
		Map<IUIDObject, ISharedObject> objectsToBeSharedInto = new HashMap<>();

		// If no target-to-source shared mapping exists, We will share objectsToBeSharedInto after finding similar objects in target.
		if (m_targetToSourceShared.isEmpty()) {
			if (!m_objectToBeShared.isEmpty()) {
				for (ILogicObject objectToBeSharedInto : m_objectToBeShared) {
					ISharedObject sharedObject = m_sharedObjectsFinder.findSharedObjectSimilarTo(objectToBeSharedInto);
					if (sharedObject != null) {
						objectsToBeSharedInto.put(objectToBeSharedInto, sharedObject);
					}
				}
			}
		}
		// If there is target-to-source shared mapping,
		// First We try to find matching shared objects in target by similarity if we failed we clone the src shared object then share into
		else {
			Map<ILogicObject, ISharedObject> targetToObjectsToBeClonedMap = new HashMap<>();
			for (ILogicObject targetObject : m_targetToSourceShared.keySet()) {
				ISharedObject sharedObject = m_sharedObjectsFinder.findSharedObjectSimilarTo(targetObject);
				//Check if the target project already contains shared object similar to targetObject, share it into the shared object.
				if (sharedObject != null) {
					objectsToBeSharedInto.put(targetObject, sharedObject);
					continue;
				}
				//Collect source shared objects if available to be cloned later.
				ISharedObject srcSharedObject = m_targetToSourceShared.get(targetObject);
				if (srcSharedObject != null) {
					targetToObjectsToBeClonedMap.put(targetObject, srcSharedObject);
					continue;
				}
				//The below condition will be true addedObject will be added to multiple target designs
				//and the current design is the first one.
				if (m_objectToBeShared.contains(targetObject)) {
					objectsToBeShared.add(targetObject);
				}
			}
			//Clone all source objects and add new objects to be be shared into
			if (!targetToObjectsToBeClonedMap.isEmpty()) {
				SharedObjectCloner sharedObjectCloner = getCloner();
				Map<IUIDObject, ISharedObject> targetToClonedObjectsMap =
						sharedObjectCloner.cloneAll(targetToObjectsToBeClonedMap);
				objectsToBeSharedInto.putAll(targetToClonedObjectsMap);
			}
		}

		ILogicDesign design = m_designs.iterator().next();
		Set<IUID> modifiedSharedObjectsUIDs = new HashSet<>();
		if (!objectsToBeShared.isEmpty()) {
			doShare(design, objectsToBeShared, m_reporter);
			modifiedSharedObjectsUIDs.addAll(
					objectsToBeShared.stream().map(ILogicObject::getSharedObjectUID).collect(Collectors.toSet()));
		}
		if (!objectsToBeSharedInto.isEmpty()) {
			doShareInto(design, objectsToBeSharedInto, m_reporter);
			modifiedSharedObjectsUIDs.addAll(
					objectsToBeSharedInto.values().stream().map(ISharedObject::getUID).collect(Collectors.toSet()));
		}
		refreshSharedTab(modifiedSharedObjectsUIDs);
	}

	@NotNull protected SharedObjectCloner getCloner()
	{
		return new SharedObjectCloner();
	}

	@NotNull protected CHSCommand getAutoShareIntoCmd(@NotNull ILogicDesign design,
			@NotNull IMessageReporterWithContext reporter,
			@NotNull Map<IUIDObject, ISharedObject> objectsToBeSharedInto, @Nullable ISchemDiagram diagram,
			@NotNull AutoShareParams params)
	{
		return new DeltaBulkAutoShareIntoCmd(objectsToBeSharedInto, design, diagram, reporter, true, true, params,
				m_newlyCreatedSharedObjs);
	}

	protected class SharedObjectCloner
	{

		private ListSet<ISharedObjectMgr> sharedObjectManagers = new ListSet<ISharedObjectMgr>();

		SharedObjectCloner()
		{
			sharedObjectManagers.add(m_project.getSharedPinListMgr());
			sharedObjectManagers.add(m_project.getSharedConductorMgr());
		}

		@NotNull private Map<IUIDObject, ISharedObject> cloneAll(
				Map<ILogicObject, ISharedObject> newObjectsToObjectsToBeClonedMap)
		{
			Map<IUIDObject, ISharedObject> objectsToBeSharedInto = new HashMap<>();

			try {
				lockManagers();
				refreshManagers();
				for (ILogicObject targetObject : newObjectsToObjectsToBeClonedMap.keySet()) {
					ISharedObject sourceSharedObject = newObjectsToObjectsToBeClonedMap.get(targetObject);
					ISharedObject targetSharedObject = cloneAndSave(sourceSharedObject);
					if (targetSharedObject != null) {
						//record instance to be shared into
						objectsToBeSharedInto.put(targetObject, targetSharedObject);
						m_newlyCreatedSharedObjs.add(targetSharedObject);
					}
				}
			}
			finally {
				unlockManagers();
			}
			return objectsToBeSharedInto;
		}

		@Nullable protected ISharedObject cloneAndSave(ISharedObject sourceSharedObject)
		{
			//clone
			List<IRevisionedSharedObject> newSharedObjects = clone(sourceSharedObject);
			if (newSharedObjects == null) {
				return null;
			}
			//save
			return save(newSharedObjects);
		}

		@Nullable private List<IRevisionedSharedObject> clone(@NotNull ISharedObject sourceSharedObject)
		{
			if (sourceSharedObject instanceof ISharedPinList) {
				PinListTypeEnum pinListType = ((ISharedPinList) sourceSharedObject).getType();
				if (pinListType == PinListTypeEnum.TypeInlineJack) {
					return null;
				}
				if (pinListType == PinListTypeEnum.TypeInlinePlug && sourceSharedObject instanceof ISharedConnector) {
					ISharedConnector oldPlug = (ISharedConnector) sourceSharedObject;
					IRevisionedSharedObject oldJack = oldPlug.getMate();
					IRevisionedSharedObject newPlug = cloneSingleObject(oldPlug);
					assert oldJack != null;
					IRevisionedSharedObject newJack = cloneSingleObject(oldJack);
					SharedObjectRevisionHelper.mateConnectors(oldPlug, (ISharedConnector) newPlug,
							(ISharedConnector) oldJack, (ISharedConnector) newJack, sharedPin -> sharedPin.getName());
					return List.of(newPlug, newJack);
				}
			}
			if (sourceSharedObject instanceof IRevisionedSharedObject) {
				return List.of(cloneSingleObject(sourceSharedObject));
			}
			return null;
		}

		@NotNull private IRevisionedSharedObject cloneSingleObject(@NotNull ISharedObject srcSharedObject)
		{
			IRevisionedSharedObject srcRevSharedObject = (IRevisionedSharedObject) srcSharedObject;
			String name = srcSharedObject.getName();
			String description = srcSharedObject.getShortDescription();
			String revision = srcRevSharedObject.getRevision();
			IRevisionedSharedObject newRevision;
			try {
				FunctionModuleCodeForSharedCopyService.getInstance().registerDoNotCopyModuleCodeForShared();
				newRevision = SharedObjectRevisionHelper.createCopyOf(
						srcRevSharedObject, name, description, revision, null, new HashMap<>(), designAbstraction);
			}
			finally {
				FunctionModuleCodeForSharedCopyService.getInstance().registerCopyModuleCodeForShared();
			}
			refreshRevisionAndValidate(newRevision);

			return newRevision;
		}

		@Nullable protected ISharedObject save(@NotNull List<IRevisionedSharedObject> newSharedObjects)
		{
			try {
				SharedObjectRevisionHelper.save(newSharedObjects, Objects.requireNonNull(m_project));
				for (var newSharedObject : newSharedObjects) {
					LockUpdateHelper.flushAndUnlockSharedObject(newSharedObject);
				}
			}
			catch (UserSessionException e) {
				return null;
			}
			return newSharedObjects.get(0);
		}

		private void refreshRevisionAndValidate(IRevisionedSharedObject sharedObject)
		{
			if (sharedObject != null) {
				ISharedPinList sharedPinList = CommonUtils.cast(sharedObject, ISharedPinList.class);
				ISharedConductor sharedConductor = CommonUtils.cast(sharedObject, ISharedConductor.class);
				ISharedMulticore sharedMulticore = CommonUtils.cast(sharedObject, ISharedMulticore.class);
				if ((sharedPinList != null)) {
					ISharedPinListMgr sharedPinListMgr = m_project.getSharedPinListMgr();
					sharedPinListMgr.refreshAllRevisionsOfSharedObject(sharedPinList);
				}
				else if (sharedConductor != null) {
					ISharedConductorMgr sharedConductorMgr = m_project.getSharedConductorMgr();
					sharedConductorMgr.refreshAllRevisionsOfSharedObject(sharedConductor);
				}
				else if (sharedMulticore != null) {
					ISharedConductorMgr sharedConductorMgr = m_project.getSharedConductorMgr();
					sharedConductorMgr.refreshAllRevisionsOfSharedObject(sharedMulticore);
				}
			}
		}

		private boolean lockManagers()
		{
			try {
				return SharedObjectRevisioningDialogHelper.lockManagers(sharedObjectManagers);
			}
			catch (RuntimeException e) {
				return false;
			}
		}

		private void unlockManagers()
		{
			try {
				SharedObjectRevisioningDialogHelper.unlockManagers(sharedObjectManagers);
			}
			catch (RuntimeException e) {
				return;
			}
		}

		private void refreshManagers()
		{
			try {
				SharedObjectRevisioningDialogHelper.refreshManager(sharedObjectManagers);
			}
			catch (RuntimeException e) {
				return;
			}
		}
	}
}