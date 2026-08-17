/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.harness.propagate;

import chs.caf.CAFUtils;
import chs.caf.IOutputWindow;
import chs.caf.caplet.IModelChangeListener;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.action.IActionMgrListener;
import chs.caf.caplet.helpers.IHarnessEditingAction;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caplets.logic.actions.EditHarnessAction;
import chs.caplets.logic.actions.SmartEditAction;
import chs.cof.drawplus.IGfxView;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedDeviceConnector;
import chs.cof.logical.shared.ISharedObject;
import chs.cofUtils.logical.concurrency.PropertiesConcurrencyHelper;
import chs.common.IDeletedObject;
import chs.common.ISystemPreferenceMgr;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.UIDUtils;
import chs.system.UIDMgr;
import chs.utilities.AppInfo;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utilities.StringUtils;
import chs.utilities.suite.ApplicationSuiteInfo;
import chs.utility.attr.AttributeUtils;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.helpers.SystemPreferencesHelper;
import chs.utility.logic.LogicUtils;
import chs.utility.logic.autotagharness.HarnessDesignPropagator;
import chs.utility.logic.autotagharness.IHarnessDesignPropagatorContext;
import chs.utility.logic.autotagharness.PokeHomeHarnessDesignPropagator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 */
public class AutoPropagateHarnessController implements IAutoPropagateHarnessController, IActionMgrListener, IModelChangeListener
{

	private static IAutoPropagateHarnessController m_instance = new AutoPropagateHarnessController();
	private static IAutoPropagateHarnessController m_dummyInstance = new DummyAutoPropagateHarnessController();
	private Map<IUID, String> m_selectedObjects = new HashMap<>();
	private Map<IUID, String> m_preActionHarnessVals = new HashMap<>();
	private Map<IUID, IHarnessDesignPropagatorContext> m_preActionPokeHomeContexts = new HashMap<>();
	private boolean m_actionEditedLogicObjects = false;
	@Nullable private IAction m_activatedAction = null;

	private Set<ILogicObject> m_logicObjectsToPropagate = new HashSet<>();
	private Map<ISharedObject, ILogicObject> m_sharedObjectsToPropagate = new HashMap<>();
	private Set<ILogicObject> m_logicObjectsToSkip = new HashSet<>();
	private Map<ISharedObject, ILogicObject> m_sharedObjectsToSkip = new HashMap<>();
	private Set<ILogicObject> m_pokeHomes = new HashSet<>();
	@Nullable private String m_harness = null;
	@Nullable private IUID m_designUid = null;

	@NotNull public static IAutoPropagateHarnessController getInstance()
	{
		if(isAutoPropagateEnabled()){
			return m_instance;
		}
		return m_dummyInstance;
	}

	@NotNull private Set<IUID> determineCurrentSelectionUIDs()
	{
		ILogicDesign logicDesign = deletermineLogicDesign();
		if (logicDesign != null) {
			ISelectMgr activeSelectMgr = CAFUtils.getInstance().getActiveSelectMgr();
			if (activeSelectMgr != null) {
				return activeSelectMgr.getCurrentSelections().getSelectedUIDS();
			}
		}
		return Collections.emptySet();
	}

	@Nullable private ILogicDesign deletermineLogicDesign()
	{
		IGfxView gfxView = CommonUtils.cast(CAFUtils.getInstance().getActiveCapletView(), IGfxView.class);
		ISchemDiagram diagram = gfxView != null ? CommonUtils.cast(gfxView.getDiagram(), ISchemDiagram.class) : null;
		return diagram != null ? diagram.getDesign() : null;
	}

	protected void collectAdditionalAffedtedObjects(@NotNull Set<ILogicObject> selectedLogicObjects,
			@NotNull Set<ILogicObject> candidatesForHarnessEdit)
	{
		selectedLogicObjects.stream().filter(obj -> obj instanceof IAssembly)
				.forEach(obj -> PropertiesConcurrencyHelper
						.collectCandidatesForConnectorAssemlyEdit(obj, candidatesForHarnessEdit));

		selectedLogicObjects.stream().filter(obj -> obj instanceof IConnector)
				.forEach(obj -> PropertiesConcurrencyHelper
						.collectAdditionalLockForModularConnectorHarnessEdit((IConnector) obj, candidatesForHarnessEdit));
	}

	protected void populateSelectedLogicObjects(@NotNull Set<ILogicObject> selectedLogicObjects)
	{
		for (IUID selectedUID : determineCurrentSelectionUIDs()) {
			IUIDObject uidObject = UIDMgr.getObject(selectedUID);
			if (uidObject != null) {
				IUIDObject attrOrPropProvider = ReferenceHelper.reduceToLogicAttrOrPropProvider(uidObject);
				IUIDObject candidate = attrOrPropProvider != null ? attrOrPropProvider : uidObject;
				ILogicObject logicObject = ReferenceHelper.reduceToLogicObject(candidate);
				if (logicObject != null) {
					selectedLogicObjects.add(logicObject);
				}
			}
		}
	}

	private void collectPokeHomeInfo(@NotNull ILogicDesign logicDesign,
			@NotNull SetMap<? extends ILogicObject, ? extends ILogicObject> pokeHomes)
	{
		for (Map.Entry<? extends ILogicObject, ? extends Set<? extends ILogicObject>> entry : pokeHomes.entrySet()) {
			ILogicObject key = entry.getKey();
			IUID uid = key.getUID();
			IHarnessDesignPropagatorContext context =
					new HarnessDesignPropagator(logicDesign, Collections.singleton(key)).propagateAndUpdate();
			m_preActionHarnessVals.put(uid, key.getHarness());
			m_preActionPokeHomeContexts.put(uid, context);
		}
	}

	@Override public void activateEnded(IAction action, @Nullable IActionEnum status)
	{
		reset();
		m_activatedAction = action;
		if (!(action instanceof IHarnessEditingAction)) {
			return;
		}

		ILogicDesign logicDesign = deletermineLogicDesign();
		if (logicDesign == null || !logicDesign.isEditable() || !AppInfo.isLogic()) {
			return;
		}

		Set<ILogicObject> selectedLogicObjects = new HashSet<>();

		populateSelectedLogicObjects(selectedLogicObjects);

		//collect the whole modular/multicore tree??
		Set<ILogicObject> candidatesForHarnessEdit = new HashSet<>();
		candidatesForHarnessEdit.addAll(selectedLogicObjects);

		collectAdditionalAffedtedObjects(selectedLogicObjects, candidatesForHarnessEdit);

		candidatesForHarnessEdit.stream().forEach(obj -> m_selectedObjects.put(obj.getUID(), obj.getHarness()));
		IHarnessDesignPropagatorContext context =
				new HarnessDesignPropagator(logicDesign, candidatesForHarnessEdit).propagateAndUpdate();
		context.getPropagatedLogicObjects().stream().forEach(obj -> m_preActionHarnessVals.put(obj.getUID(), obj.getHarness()));

		collectPokeHomeInfo(logicDesign, context.getPokeHomeConductors());
		collectPokeHomeInfo(logicDesign, context.getPokeHomePinLists());
	}

	public static boolean isAutoPropagateEnabled()
	{
		if (ApplicationSuiteInfo.isElectricalSuite() || ApplicationSuiteInfo.isCapitalEssentialsSuite()) {
			//Auto Propagate is always enabled in Electrical & Capital Essential irrespective of system preference value.
			return true;
		}

		ISystemPreferenceMgr systemPrefMgr = SystemPreferencesHelper.getSystemPreferences();
		return systemPrefMgr == null || systemPrefMgr.isPropagateHarnessAttributeEnabled();
	}

	@Override public void terminateEnded(IAction action, boolean status)
	{
		if (!(action instanceof IHarnessEditingAction)) {
			if (m_actionEditedLogicObjects) {
				clearHarnessPropagateWindow();
			}
			return;
		}
		IHarnessEditingAction harnessEditingAction = (IHarnessEditingAction) action;
		if (harnessEditingAction.isAutoPropagateAction()) {
			return;
		}

		ILogicDesign logicDesign = deletermineLogicDesign();
		if (logicDesign == null || !logicDesign.isEditable() || !AppInfo.isLogic()) {
			return;
		}

		SetMap<String, ILogicObject> candidatesForHarnessEdit = SetMap.createShallowSetMap();

		boolean traverseAll = status && harnessEditingAction.shouldTraverseAllDuringAutoPropagate();
		populateCandidatesForEdit(candidatesForHarnessEdit, traverseAll);

		if (candidatesForHarnessEdit.size() == 1) {
			Map.Entry<String, Set<ILogicObject>> entry = candidatesForHarnessEdit.entrySet().iterator().next();
			String newHarnessVal = StringUtils.emptyIfBlank(entry.getKey());
			if (AttributeUtils.getHarnessNameTokens(newHarnessVal).size() != 1) {
				clearHarnessPropagateWindow();
				return;
			}
			m_harness = newHarnessVal;

			Set<ILogicObject> candidateObjects = entry.getValue();

			IHarnessDesignPropagatorContext context =
					new PokeHomeHarnessDesignPropagator(logicDesign, candidateObjects, m_preActionHarnessVals).propagateAndUpdate();

			Set<ILogicObject> logicObjects = new HashSet<>(context.getPropagatedLogicObjects());
			Map<ISharedObject, ILogicObject> sharedObjects = new HashMap<>(context.getPropagatedSharedObjects());
			SetMap<IConductor, IPinList> conductorPokeHomes = new SetMap<>(context.getPokeHomeConductors());
			SetMap<IPinList, IConductor> pinListPokeHomes = new SetMap<>(context.getPokeHomePinLists());

			logicObjects.removeAll(candidateObjects);
			conductorPokeHomes.removeAll(logicObjects.stream().filter(o -> o instanceof IConductor)
					.map(o -> (IConductor) o).collect(Collectors.toSet()));
			conductorPokeHomes.removeAll(candidateObjects.stream().filter(o -> o instanceof IConductor)
					.map(o -> (IConductor) o).collect(Collectors.toSet()));
			pinListPokeHomes.removeAll(logicObjects.stream().filter(o -> o instanceof IPinList)
					.map(o -> (IPinList) o).collect(Collectors.toSet()));
			pinListPokeHomes.removeAll(candidateObjects.stream().filter(o -> o instanceof IPinList)
					.map(o -> (IPinList) o).collect(Collectors.toSet()));
			m_pokeHomes.addAll(conductorPokeHomes.keySet());
			m_pokeHomes.addAll(pinListPokeHomes.keySet());

			List<IHarnessPropagateStatusMessage> messages = new ArrayList<>(logicObjects.size() * 2);

			collectObjectMessages(logicDesign, newHarnessVal, logicObjects, sharedObjects, messages);
			collectPokeHomeMessages(logicDesign, newHarnessVal, conductorPokeHomes, messages);
			collectPokeHomeMessages(logicDesign, newHarnessVal, pinListPokeHomes, messages);

			HarnessPropagateTableUtils.showMessages(messages, logicDesign.getUID());
		}
		reset();
	}

	private boolean shouldTraverseAll(@NotNull IAction action)
	{
		return (action instanceof EditHarnessAction || action instanceof SmartEditAction);
	}

	protected void collectObjectMessages(@NotNull ILogicDesign logicDesign, @NotNull String newHarnessVal,
			@NotNull Set<ILogicObject> logicObjects, @NotNull Map<ISharedObject, ILogicObject> sharedObjects,
			@NotNull List<IHarnessPropagateStatusMessage> messages)
	{
		Map<ILogicObject, IHarnessPropagateStatusMessage> logicMessages = new HashMap<>();

		for (ILogicObject logicObject : LogicUtils.getReducedLogicSet(logicObjects)) {
			if (!supportsLogicHarnessEdit(logicObject)) {
				continue;
			}
			String oldHarnessVal = StringUtils.emptyIfBlank(logicObject.getHarness());
			if (!StringUtils.areEqualOrBothNull(newHarnessVal, oldHarnessVal)) {

				HarnessPropagateMessageType messageType = LogicUtils.hasMultipleHarness(logicObject) ?
								HarnessPropagateMessageType.READY_TO_UPDATE_VARIANT_CONN : HarnessPropagateMessageType.READY_TO_UPDATE;

				HarnessPropagateStatusMessage
						logicStatusMessage = new HarnessPropagateStatusMessage(messageType, logicObject,
						oldHarnessVal, newHarnessVal, logicDesign, new HarnessPropagateStatusMessageGroup());

				if (messageType == HarnessPropagateMessageType.READY_TO_UPDATE_VARIANT_CONN) {
					logicStatusMessage.setupPropagationStatus(false);
				}

				if (logicObject.isShared()) {
					logicStatusMessage.setMessage(ResourceMgr.getString(HarnessPropagateMessageType.class,
							"HarnessPropagateMessageType.ReadyToUpdateShared.message"));
				}

				logicMessages.put(logicObject, logicStatusMessage);
				messages.add(logicStatusMessage);
			}
		}

		collectSharedMessages(logicDesign, newHarnessVal, sharedObjects, messages, logicMessages);
	}

	private void collectSharedMessages(@NotNull ILogicDesign logicDesign, @NotNull String newHarnessVal,
			@NotNull Map<ISharedObject, ILogicObject> sharedObjects, @NotNull List<IHarnessPropagateStatusMessage> messages,
			@NotNull Map<ILogicObject, IHarnessPropagateStatusMessage> logicMessages)
	{
		Set<ILogicObject> objectsProcessed = new HashSet<>();
		for (Map.Entry<ISharedObject, ILogicObject> item : sharedObjects.entrySet()) {
			ISharedObject sharedObject = item.getKey();
			ILogicObject logicObject = item.getValue();

			String oldHarnessVal = StringUtils.emptyIfBlank(sharedObject.getHarness());
			ILogicObject parentObject = LogicUtils.getParentMCObject(logicObject);

			if (!supportsSharedHarnessEdit(sharedObject) || !objectsProcessed.add(parentObject)) {
				continue;
			}

			IHarnessPropagateStatusMessage logicStatusMessage = logicMessages.get(parentObject);
			IHarnessPropagateStatusMessageGroup messageGroup = logicStatusMessage != null ? logicStatusMessage.getGroup() :
							new HarnessPropagateStatusMessageGroup();

			HarnessPropagateStatusMessage message =
					new HarnessPropagateStatusMessage(HarnessPropagateMessageType.SHARED_READY_TO_UPDATE,
							parentObject, oldHarnessVal, newHarnessVal, logicDesign, messageGroup);
			message.setupPropagationStatus(false);
			messages.add(message);
		}
	}

	private boolean supportsLogicHarnessEdit(@NotNull ILogicObject logicObject)
	{
		return logicObject.supportsEditHarnessAttribute() && !(logicObject instanceof IDeviceConnector);
	}

	private boolean supportsSharedHarnessEdit(@NotNull ISharedObject sharedObject)
	{
		return !(sharedObject instanceof ISharedDeviceConnector);
	}

	protected void populateCandidatesForEdit(@NotNull SetMap<String, ILogicObject> candidatesForHarnessEdit,
			boolean traverseAll)
	{
		for (Map.Entry<IUID, String> entry : m_selectedObjects.entrySet()) {
			ILogicObject logicObject = UIDMgr.getObjectOfType(entry.getKey(), ILogicObject.class);
			if (logicObject != null) {
				String newHarnessVal = StringUtils.emptyIfBlank(logicObject.getHarness());
				String oldHarnessVal = StringUtils.emptyIfBlank(entry.getValue());
				if (traverseAll || !StringUtils.areEqualOrBothNull(newHarnessVal, oldHarnessVal)) {
					candidatesForHarnessEdit.add(newHarnessVal, logicObject);
				}
			}
		}
	}

	protected void collectPokeHomeMessages(@NotNull ILogicDesign logicDesign, @NotNull String newHarnessVal,
			@NotNull SetMap<? extends ILogicObject, ? extends ILogicObject> pokeHomeSetMap,
			@NotNull List<IHarnessPropagateStatusMessage> messages)
	{
		for (Map.Entry<? extends ILogicObject, ? extends Set<? extends ILogicObject>> entry : pokeHomeSetMap
				.entrySet()) {
			ILogicObject logicObject = entry.getKey();
			String oldHarnessVal = StringUtils.emptyIfBlank(logicObject.getHarness());
			if (!StringUtils.areEqualOrBothNull(newHarnessVal, oldHarnessVal)) {
				HarnessPropagateStatusMessage pokeHomeMessage = new HarnessPropagateStatusMessage(
						HarnessPropagateMessageType.READY_TO_UPDATE_POKE_HOME, logicObject, oldHarnessVal,
						newHarnessVal, logicDesign, new HarnessPropagateStatusMessageGroup());
				pokeHomeMessage.setMessage(getPokeHomeMessage(logicObject, entry.getValue()));
				pokeHomeMessage.setupPropagationStatus(false);
				messages.add(pokeHomeMessage);
				if (logicObject.isShared()) {
					HarnessPropagateStatusMessage message =
							new HarnessPropagateStatusMessage(HarnessPropagateMessageType.SHARED_READY_TO_UPDATE,
									logicObject, oldHarnessVal, newHarnessVal, logicDesign, pokeHomeMessage.getGroup());
					message.setupPropagationStatus(false);
					messages.add(message);
				}
			}
		}
	}

	@NotNull private String getPokeHomeMessage(@NotNull ILogicObject logicObject, @NotNull Set<? extends ILogicObject> values)
	{
		if (m_preActionPokeHomeContexts.containsKey(logicObject.getUID())) {
			IHarnessDesignPropagatorContext context =
					m_preActionPokeHomeContexts.get(logicObject.getUID());

			List<ILogicObject> valueLists = values.stream().collect(Collectors.toList());
			Collections.sort(valueLists, new PokeHomeComparator());
			List<String> collect = valueLists.stream().map(o -> o.getName()).collect(Collectors.toList());
			String pokeHomesObjects = StringUtils.convertCollectionToString(collect, ",");

			List<ILogicObject> logicObjects = context.getPropagatedLogicObjects().stream().collect(Collectors.toList());
			logicObjects.remove(logicObject);
			Collections.sort(logicObjects, new PokeHomeComparator());
			List<String> nameList = logicObjects.stream().map(o -> o.getName()).collect(Collectors.toList());
			String affectedObjects = StringUtils.convertCollectionToString(nameList, ",");

			String conductors = logicObject instanceof IConductor ? logicObject.getName() : pokeHomesObjects;
			String pinlists = logicObject instanceof IConductor ? pokeHomesObjects : logicObject.getName();

			return ResourceMgr
					.getString(HarnessPropagateMessageType.class, "HarnessPropagateMessageType.PokeHomed.message",
							conductors, pinlists, affectedObjects);
		}
		return "";
	}

	public void clearHarnessPropagateWindow()
	{
		String propagate_harness = IAutoPropagateHarnessController.propagate_harness_tab;
		IOutputWindow outputWindow = CAFUtils.getInstance().getOutputWindow();
		Component pane = outputWindow.getPane(propagate_harness);
		if (pane instanceof HarnessPropagateTableWindow) {
			((HarnessPropagateTableWindow) pane).clearHarnessPropagateTable();
		}
		m_harness = null;
		m_designUid = null;
	}

	@Override @Nullable public PropagationInfo getPropagationInfo()
	{
		PropagationInfo propagationInfo = null;
		if (m_harness != null && m_designUid != null) {
			propagationInfo = new PropagationInfo(m_designUid, m_logicObjectsToPropagate, m_sharedObjectsToPropagate,
					m_logicObjectsToSkip, m_sharedObjectsToSkip, m_harness);
		}
		return propagationInfo;
	}

	@Override public void loadObjects(@NotNull IUID designUid, @NotNull HarnessUpdateStatusMessageTableModel tableModel, boolean propagateAll)
	{
		m_designUid = designUid;
		m_logicObjectsToPropagate.clear();
		m_sharedObjectsToPropagate.clear();
		m_logicObjectsToSkip.clear();
		m_sharedObjectsToSkip.clear();

		Collection<IHarnessPropagateStatusMessage> rows = tableModel.getRows();

		Set<ILogicObject> logicObjectsToPropagate = new HashSet<>();
		Set<ILogicObject> logicObjectsToSkip = new HashSet<>();

		for (IHarnessPropagateStatusMessage message : rows) {
			if (!message.isSharedRow() && message.isEditable()) {
				if (propagateAll || message.shouldPropgate()) {
					addLogicObject(logicObjectsToPropagate, message.getObjectId());
				}
				else {
					addLogicObject(logicObjectsToSkip, message.getObjectId());
				}
			}
		}

		Map<ISharedObject, ILogicObject> sharedObjectsToPropagate = new HashMap<>();
		Map<ISharedObject, ILogicObject> sharedObjectsToSkip = new HashMap<>();

		for (IHarnessPropagateStatusMessage message : rows) {
			if (message.isSharedRow() && message.isEditable()) {
				if (propagateAll || message.shouldPropgate()) {
					ILogicObject logicObject = UIDMgr.getObjectOfType(message.getObjectId(), ILogicObject.class);
					if (logicObject == null) {
						continue;
					}
					addSharedObject(logicObjectsToPropagate, sharedObjectsToPropagate, message.getObjectId(), !logicObjectsToSkip.contains(logicObject));
				}
				else {
					addSharedObject(logicObjectsToPropagate, sharedObjectsToSkip, message.getObjectId(), true);
				}
			}
		}

		Set<ILogicObject> toAdd = new HashSet<>();
		Map<ISharedObject, ILogicObject> toAddShared = new HashMap<>();
		for (ILogicObject pokeHome : m_pokeHomes) {
			if (logicObjectsToPropagate.contains(pokeHome)) {
				if (m_preActionPokeHomeContexts.containsKey(pokeHome.getUID())) {
					IHarnessDesignPropagatorContext context =
							m_preActionPokeHomeContexts.get(pokeHome.getUID());
					toAdd.addAll(context.getPropagatedLogicObjects());
					toAddShared.putAll(context.getPropagatedSharedObjects());
				}
			}
		}

		m_logicObjectsToPropagate.addAll(logicObjectsToPropagate);
		m_logicObjectsToPropagate.addAll(toAdd);
		m_sharedObjectsToPropagate.putAll(sharedObjectsToPropagate);
		m_sharedObjectsToPropagate.putAll(toAddShared);
		m_logicObjectsToSkip.addAll(logicObjectsToSkip);
		m_sharedObjectsToSkip.putAll(sharedObjectsToSkip);
		m_pokeHomes.clear();
		m_preActionPokeHomeContexts.clear();
	}

	private void addSharedObject(@NotNull Set<ILogicObject> logicObjectSet, @NotNull Map<ISharedObject, ILogicObject> sharedObjectSet,
			@NotNull IUID uid, boolean skipCheck)
	{
		ILogicObject logicObject = UIDMgr.getObjectOfType(uid, ILogicObject.class);
		if (logicObject != null) {
			if (skipCheck || logicObjectSet.contains(logicObject)) {
				ISharedObject sharedObject = logicObject.getSharedObject();
				if (sharedObject != null) {
					sharedObjectSet.put(sharedObject, logicObject);
				}
			}
		}
	}

	private void addLogicObject(@NotNull Set<ILogicObject> logicObjectSet, @NotNull IUID uid)
	{
		ILogicObject logicObject = UIDMgr.getObjectOfType(uid, ILogicObject.class);
		if (logicObject != null) {
			logicObjectSet.add(logicObject);
		}
	}

	private void reset()
	{
		m_selectedObjects.clear();
		m_preActionHarnessVals.clear();
		m_actionEditedLogicObjects = false;
		m_activatedAction = null;
	}

	@Override public void modelPreChanged(ModelChangeEvent e)
	{
	}

	@Override public void modelChanged(ModelChangeEvent e)
	{
		Set<IUIDObject> allObjects = new HashSet<>();
		allObjects.addAll(UIDUtils.convertToUIDObjectList(e.getChangedObjectsUIDs()));
		allObjects.addAll(UIDUtils.convertToUIDObjectList(e.getNewObjectsUIDs()));
		allObjects.addAll(UIDUtils.convertToUIDObjectList(e.getDeletedObjectsUIDs()));
		for (IUIDObject object : allObjects) {
			IUIDObject objectOfInterest =
					object instanceof IDeletedObject ? ((IDeletedObject) object).getOriginalObject() : object;
			if (objectOfInterest instanceof ILogicObject) {
				m_actionEditedLogicObjects = true;
				break;
			}
		}

		if (m_activatedAction == null && m_actionEditedLogicObjects) {
			clearHarnessPropagateWindow();
			m_actionEditedLogicObjects = false;
		}
	}
}
