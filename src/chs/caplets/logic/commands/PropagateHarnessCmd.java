/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.commands;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.logic.actions.PropagateHarnessContext;
import chs.caplets.logic.harness.propagate.HarnessPropagateTableUtils;
import chs.caplets.logic.harness.propagate.IHarnessPropagateStatusMessage;
import chs.caplets.shared.ForeignDesignChangesHandler;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.IPropagateHarnessCmd;
import chs.cof.logical.IPropagationInfo;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPinListMgr;
import chs.cof.project.IProject;
import chs.cofUtils.cmd.CHSCommand;
import chs.common.ICommandHelper;
import chs.common.IGuard;
import chs.common.IObjectFilter;
import chs.common.NullGuard;
import chs.common.PreferenceContext;
import chs.dataservices.CapitalDataServices;
import chs.system.UIDMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utilities.StringUtils;
import chs.utilities.permission.PermissionEnum;
import chs.utilities.ui.messaging.Choice;
import chs.utilities.ui.messaging.Question;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.logic.LogicUtils;
import chs.utility.logic.autotagharness.DESIGN_LOCK_RESULT;
import chs.utility.logic.autotagharness.DesignInfoPropagator;
import chs.utility.logic.autotagharness.HarnessDesignPropagator;
import chs.utility.logic.autotagharness.HarnessUpdater;
import chs.utility.logic.autotagharness.IHarnessDesignPropagatorContext;
import chs.utility.logic.autotagharness.IHarnessUpdateReport;
import chs.utility.logic.autotagharness.IHarnessUpdater;
import chs.utility.logic.autotagharness.ILogicObjectInfo;
import chs.utility.logic.autotagharness.LogicConnectivityInfo;
import chs.utility.logic.autotagharness.LogicConnectivityInfoProvider;
import chs.utility.logic.autotagharness.SharedInfoCache;
import chs.utility.security.PermissionHelper;
import chs.utility.ui.progress.IProgress;
import chs.utility.ui.progress.ProgressGroup;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Command to update harness to all connected objects across designs
 */

public class PropagateHarnessCmd extends CHSCommand implements IPropagateHarnessCmd
{

	private IProject project;
	private ILogicDesign currentDesign;
	private IPropagationInfo m_propagationInfo;
	private PropagateHarnessContext m_context;

	private IProgress m_progress;
	private IProgress m_findProgress;
	private IProgress m_propagateProgress;

	private SetMap<ILogicDesign, ISharedObject> designVsSharedObj;
	private SharedInfoCache sharedInfoCache;

	private SetMap<ILogicDesign, ILogicObject> m_loadedDesignSet;
	private SetMap<ILogicDesign, String> m_unloadedDesignSet;

	private static final String FINDING_CONNECTED_OBJECTS =
			ResourceMgr.getString(PropagateHarnessCmd.class, "PropagateHarnessCmd.Progress.FindingConnectedObjects");
	private static final String UPDATING_DESIGNS =
			ResourceMgr.getString(PropagateHarnessCmd.class, "PropagateHarnessCmd.Progress.UpdatingDesigns");
	private LogicConnectivityInfoProvider logicConnectivityInfoProvider;

	public PropagateHarnessCmd(@NotNull ICommandHelper commandHelper, @NotNull ILogicDesign design, @NotNull
			IPropagationInfo propagationInfo)
	{
		super(commandHelper);
		logicConnectivityInfoProvider = getLogicConnectivityInfoProvider();
		m_propagationInfo = propagationInfo;
		currentDesign = design;
		m_context = createPropagateHarnessContext();
		project = m_context.getProject();
		init();

		ProgressGroup progressGroup = new ProgressGroup(StringUtils.EMPTY_STRING);
		m_findProgress = progressGroup.createChild(0, 0, FINDING_CONNECTED_OBJECTS);
		m_propagateProgress = progressGroup.createChild(0, 1, UPDATING_DESIGNS);
		m_progress = progressGroup;
	}

	@NotNull protected LogicConnectivityInfoProvider getLogicConnectivityInfoProvider()
	{
		return new LogicConnectivityInfoProvider();
	}

	private void init()
	{
		m_unloadedDesignSet = new SetMap<>();
		designVsSharedObj = new SetMap<>();
		m_loadedDesignSet = new SetMap<>();
		sharedInfoCache = new SharedInfoCache();
	}

	@NotNull private PropagateHarnessContext createPropagateHarnessContext()
	{
		return new PropagateHarnessContext(currentDesign, getCommandHelper());
	}

	@Override public boolean prepare()
	{
		String contentRoot = m_propagationInfo.getSharedObjects().isEmpty() ?
				(containsSharedObjects() ? "PropagateHarnessCmd.Warning" : null) : "PropagateHarnessCmd.Shared.Warning";

		if (contentRoot == null) {
			return true;
		}

		ResourceBasedMessageContent content =
				new ResourceBasedMessageContent(PropagateHarnessCmd.class, contentRoot);
		final Choice propagate = new Choice(getClass(), "PropagateHarnessCmd.Warning.choice.propagate");
		final Choice cancel = new Choice(getClass(), "PropagateHarnessCmd.Warning.choice.cancel");
		Choice choice = Question.show(content, propagate, cancel);

		return choice != cancel;
	}

	private boolean containsSharedObjects()
	{
		for (ILogicObject logicObject : m_propagationInfo.getLogicObjects()) {
			if (logicObject.isShared()) {
				return true;
			}
		}
		return false;
	}

	@Override protected boolean doExecute()
	{
		refreshManagers();

		Set<ISharedObject> accessibleSharedObjects = getAccessibleSharedObjects();
		for (ILogicObject logicObject : m_propagationInfo.getLogicObjects()) {
			m_loadedDesignSet.add(currentDesign, logicObject);
		}

		for (Map.Entry<ISharedObject, ILogicObject> entry : m_propagationInfo.getSharedObjects().entrySet()) {
			m_loadedDesignSet.add(currentDesign, entry.getValue());
		}

		Set<ISharedObject> sharedObjectsToSkip = new HashSet<>();
		sharedObjectsToSkip.addAll(m_propagationInfo.getSharedObjectsToSkip().keySet());
		for (ISharedObject sharedObject : sharedObjectsToSkip) {
			designVsSharedObj.add(currentDesign, sharedObject);
		}

		ICapletView capletView = CAFUtils.getInstance().getActiveCapletView();
		try {
			if (capletView != null) {
				capletView.lock();
			}
			Set<ILogicDesign> designsToProcess = new HashSet<>();
			if (!accessibleSharedObjects.isEmpty()) {
				designVsSharedObj.put(currentDesign, accessibleSharedObjects);
				populateDesignUsageMap(accessibleSharedObjects, designsToProcess);
				designsToProcess.remove(currentDesign);
			}

			m_findProgress.increment("");

			Set<ISharedObject> allCollectedSharedObjects = new HashSet<>();
			Set<ISharedObject> sharedObjectsInDesign = new HashSet<>(accessibleSharedObjects);
			while (!designsToProcess.isEmpty()) {
				allCollectedSharedObjects.addAll(sharedObjectsInDesign);
				Set<ISharedObject> sharedObjectsCollected = new HashSet<>();

				for (ILogicDesign logicDesign : designsToProcess) {

					DESIGN_LOCK_RESULT lockResult = m_context.lockDesign(logicDesign);
					if (lockResult.isFailed()) {
						designVsSharedObj.remove(logicDesign);
					}
					if (logicDesign.isLocked() || logicDesign == currentDesign) {
						processDesign(sharedObjectsCollected, logicDesign, sharedObjectsInDesign);
					}
				}

				sharedObjectsCollected.removeAll(allCollectedSharedObjects);
				sharedObjectsCollected.removeAll(sharedObjectsToSkip);
				sharedObjectsInDesign.clear();
				sharedObjectsInDesign.addAll(sharedObjectsCollected);

				//determine unprocessed designs for the next round processing.
				designsToProcess.clear();
				if (!sharedObjectsInDesign.isEmpty()) {
					populateDesignUsageMap(sharedObjectsInDesign, designsToProcess);
				}
			}

			propagateDesigns();
		}
		catch (UserSessionException e) {
			e.printStackTrace();
			return false;
		}
		finally {
			if (capletView != null) {
				capletView.unlock();
			}
			m_context.unlockDesigns();
		}

		return true;
	}

	@NotNull private Set<ISharedObject> getAccessibleSharedObjects()
	{
		Set<ISharedObject> accessibleSharedObjects = new HashSet<>();

		if (PermissionHelper.hasPermission(PermissionEnum.SHARED_OBJECTS)) {
			Set<ISharedObject> sharedObjects = m_propagationInfo.getSharedObjects().keySet();
			accessibleSharedObjects.addAll(sharedObjects.stream()
					.filter(o -> sharedInfoCache.isSharedObjectAvailable(o, currentDesign)).collect(Collectors.toSet()));
		}

		return accessibleSharedObjects;
	}

	private void refreshManagers()
	{
		ISharedPinListMgr sharedPinListMgr = project.getSharedPinListMgr();
		if (sharedPinListMgr != null) {
			sharedPinListMgr.refresh();
		}
		ISharedConductorMgr sharedConductorMgr = project.getSharedConductorMgr();
		if (sharedConductorMgr != null) {
			sharedConductorMgr.refresh();
		}
	}

	private void propagateDesigns() throws UserSessionException
	{
		Set<ILogicDesign> designsToProcess = new HashSet<>();
		designsToProcess.addAll(designVsSharedObj.keySet());
		designsToProcess.add(currentDesign);

		int range = designsToProcess.size();
		m_propagateProgress.setRange(range);

		List<IHarnessPropagateStatusMessage> messages = new ArrayList<>();
		Set<ISharedObject> updatedSharedObjects = new HashSet<>();
		Set<ISharedObject> failedSharedObjects = new HashSet<>();

		for (ILogicDesign design : designsToProcess) {
			m_propagateProgress.increment(design.getName());
			if (m_progress.isCancelled()) {
				break;
			}

			boolean inMemory = isLoadedInMemory(design);

			try (IGuard ignored = getUndoableGuardForDesign(design)) {

				Set<ILogicObject> logicObjs = getLogicObjects(design);

				IHarnessUpdater harnessUpdater = getHarnessUpdater(design, logicObjs);
				IHarnessUpdateReport updateReport = harnessUpdater.updateHarness();
				populateSharedResult(failedSharedObjects, updatedSharedObjects, updateReport);
				HarnessPropagateTableUtils.updateStatusMessages(messages, updateReport);

				if (!m_propagationInfo.getSharedObjects().isEmpty() && design != currentDesign) {
					getCommandHelper().saveDesign(design);
					getCommandHelper().setDesignModifiedFlag(design, false);
				}
				else {
					getCommandHelper().setDesignModifiedFlag(design, true);
				}
			}

			finally {
				m_context.unlockDesign(design);
				if (!inMemory) {
					unloadFromMemory(design);
				}
			}
		}

		showPropagationResult(messages, updatedSharedObjects, failedSharedObjects);
	}

	@NotNull protected HarnessUpdater getHarnessUpdater(@NotNull ILogicDesign design, @NotNull Set<ILogicObject> logicObjs)
	{
		return new HarnessUpdater(design, logicObjs, m_propagationInfo.getHarness(),
				design.isUnderConcurrentEdit(), sharedInfoCache);
	}

	private void showPropagationResult(@NotNull List<IHarnessPropagateStatusMessage> messages,
			@NotNull Set<ISharedObject> updatedSharedObjects, @NotNull Set<ISharedObject> failedSharedObjects)
	{
		HarnessPropagateTableUtils
				.updateStatusMessagesForShared(messages, designVsSharedObj, failedSharedObjects, updatedSharedObjects,
						sharedInfoCache, m_propagationInfo.getHarness());

		Map<ISharedObject, ILogicObject> sharedObjects = new HashMap<>();
		Map<ILogicObject, IHarnessPropagateStatusMessage> logicMessages = new HashMap<>();

		HarnessPropagateTableUtils.updateStatusMessagesForNonSelectedRows(messages, currentDesign,
				m_propagationInfo.getLogicObjectsToSkip(), m_propagationInfo.getHarness(), sharedObjects, logicMessages);

		sharedObjects.putAll(m_propagationInfo.getSharedObjectsToSkip());
		HarnessPropagateTableUtils.updateSkippedSharedMessages(messages, sharedObjects, currentDesign,
				m_propagationInfo.getHarness(), logicMessages);

		HarnessPropagateTableUtils.showMessages(messages, currentDesign.getUID());
	}

	@NotNull private IGuard getUndoableGuardForDesign(@NotNull ILogicDesign design)
	{
		ICapletController controller = CAFUtils.getInstance().getControllerForDesign(design);
		ICapletController activeController = CAFUtils.getInstance().getActiveCapletController();

		if (controller != null && controller == activeController) {
			return new NullGuard();
		}
		return new ForeignDesignChangesHandler.UndoIdlerForForeignDesignChanges(controller);
	}

	protected void unloadFromMemory(@NotNull ILogicDesign design)
	{
		design.unloadFromMemory();
	}

	private void populateSharedResult(Set<ISharedObject> failedSharedObjects, Set<ISharedObject> updatedSharedObjects,
			IHarnessUpdateReport updateReport)
	{
		for (ISharedObject sharedObject : updateReport.getFailedSharedObjects()) {
			failedSharedObjects.add(sharedObject);
		}
		for (ISharedObject sharedObject : updateReport.getEditedSharedObjects()) {
			updatedSharedObjects.add(sharedObject);
		}
	}

	@NotNull private Set<ILogicObject> getLogicObjects(ILogicDesign design)
	{
		//load connectivity
		design.getConnectivity();

		if (m_loadedDesignSet.contains(design)) {
			Set<ILogicObject> loadedDesignLogicObjs = m_loadedDesignSet.get(design);
			if (design == currentDesign) {
				loadedDesignLogicObjs.removeAll(m_propagationInfo.getLogicObjectsToSkip());
			}
			return LogicUtils.getReducedLogicSet(loadedDesignLogicObjs);
		}

		Set<ILogicObject> logicObjs = new HashSet<>();
		if (m_unloadedDesignSet.contains(design)) {
			Set<String> uids = m_unloadedDesignSet.get(design);
			for (String uid : uids) {
				ILogicObject object = UIDMgr.getObjectOfType(uid, ILogicObject.class);
				if (object != null) {
					logicObjs.add(object);
				}
			}
		}
		return LogicUtils.getReducedLogicSet(logicObjs);
	}

	private void processDesign(Set<ISharedObject> sharedObjectsCollected, ILogicDesign logicDesign,
			Set<ISharedObject> sharedObjectsInDesign)
	{
		Set<ISharedObject> designSharedObjs = new HashSet<>();
		boolean loaded = isLoadedInMemory(logicDesign);

		if (loaded) {
			processLoadedConnectivity(logicDesign, sharedObjectsInDesign, designSharedObjs);
		}
		else {
			processUnloadedConnectivity(logicDesign, sharedObjectsInDesign, designSharedObjs);
		}

		sharedObjectsCollected.addAll(designSharedObjs);
		designSharedObjs.forEach(obj -> designVsSharedObj.add(logicDesign, obj));
	}

	protected boolean isLoadedInMemory(@NotNull ILogicDesign logicDesign)
	{
		return logicDesign.isLoadedInMemory();
	}

	private void processUnloadedConnectivity(ILogicDesign logicDesign, Set<ISharedObject> sharedObjectsInDesign,
			Set<ISharedObject> designSharedObjs)
	{
		LogicConnectivityInfo logicConnectivity = logicConnectivityInfoProvider
				.getUnloadedLogicConnectivity(logicDesign, project.getPreferences().getEnableImplicitPokeHome(
						PreferenceContext.LOGIC));

		Set<ILogicObjectInfo> logicObjs = logicConnectivity.findLogicObjects(sharedObjectsInDesign);

		DesignInfoPropagator infoPropagator = new DesignInfoPropagator(logicDesign, logicObjs,
				(objectInfo, design) -> sharedInfoCache.isSharedObjectAvailable(objectInfo.getSharedUID(), design) &&
						!LogicUtils.hasMultipleHarness(objectInfo.getUID()));
		Set<ILogicObjectInfo> propagateResult = infoPropagator.propagate();

		propagateResult.forEach(o -> m_unloadedDesignSet.add(logicDesign, o.getUID()));

		for (ILogicObjectInfo objectInfo : propagateResult) {

			String sharedUID = objectInfo.getSharedUID();
			ISharedObject sharedObject = UIDMgr.getObjectOfType(sharedUID, ISharedObject.class);

			if (sharedObject != null) {
				designSharedObjs.add(sharedObject);
			}
		}
	}

	private void processLoadedConnectivity(ILogicDesign logicDesign, Set<ISharedObject> sharedObjectsInDesign,
			Set<ISharedObject> designSharedObjs)
	{
		IConnectivity connectivity = logicDesign.getConnectivity();

		if (connectivity != null) {

			IObjectFilter<ILogicObject> filter = logicObject -> {
				ISharedObject sharedObject = logicObject.getSharedObject();
				return sharedObject != null && sharedObjectsInDesign.contains(sharedObject);
			};
			Set<ILogicObject> logicObjs =
					CollectionUtils.getFilteredCollection(connectivity.getObjects(), filter, HashSet::new);

			HarnessDesignPropagator designPropagator = new HarnessDesignPropagator(logicDesign, logicObjs,
					(uidObj, design) -> sharedInfoCache.isSharedObjectAvailable(uidObj, design) &&
							!LogicUtils.hasMultipleHarness(uidObj));
			IHarnessDesignPropagatorContext designContext = designPropagator.propagateAndUpdate();

			Set<ILogicObject> propagatedSet = designContext.getPropagatedLogicObjects();
			propagatedSet.forEach(o -> m_loadedDesignSet.add(logicDesign, o));
			designSharedObjs.addAll(designContext.getPropagatedSharedObjects().keySet());
		}
	}

	protected void populateDesignUsageMap(Set<ISharedObject> sharedObjects,
			Set<ILogicDesign> designsToProcess)
	{
		CapitalDataServices.getDataServices()
				.getSharedObjectUsages(m_context.getProject(), m_context.getDesignsInScope(), sharedObjects)
				.keySet().stream().filter(design -> m_context.isDesignInScope(design))
				.filter(design -> m_context.isDesignEditable(design))
				.filter(design -> m_context.isDesignSafe(design))
				.forEach(design -> designsToProcess.add(design));
	}

	@NotNull @Override public IProgress getProgress()
	{
		return m_progress;
	}
}
