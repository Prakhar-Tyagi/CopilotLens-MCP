/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.IOutputWindow;
import chs.caf.OutputWindowWrapper;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.commands.BatchUpdateFromDictionaryCmd;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IFunctionBaseConductor;
import chs.cof.logical.cable.IFunctionConductor;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.project.objectinfo.names.INameTemplate;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.utilities.CommonUtils;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.logic.LogicUtils;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Action to update function signals,messages to network signals,messages using dictionary name and revision
 */
public class UpdateDictionaryAction extends ControllerActionRT implements ICtxMenuProvider
{

	private UpdateDictionaryActionHelper m_dictionaryHelper;
	private Set<IFunctionBaseConductor> selectedObjects;
	private IOutputWindow m_outputWindow;

	public UpdateDictionaryAction(@NotNull ICapletController controller)
	{
		super(controller);
		selectedObjects = new HashSet<>();
	}

	@NotNull private Map<IFunctionBaseConductor, Pair<INameTemplate, INameTemplate>> populateSelectionObjectMap()
	{
		m_dictionaryHelper.refreshDictionary();
		Map<IFunctionBaseConductor, Pair<INameTemplate, INameTemplate>> hashMap = new HashMap<>();
		for (IFunctionBaseConductor conductor : selectedObjects) {
			Pair<INameTemplate, INameTemplate> matchedNameTemplates =
					m_dictionaryHelper.getNameTemplate(conductor);
			if (matchedNameTemplates != null) {
				hashMap.put(conductor, matchedNameTemplates);
			}
		}
		return hashMap;
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		selectedObjects.clear();
		Stream<ILogicObject> objectStream =
				selections.getSelectedUIDS().stream().map(ReferenceHelper::reduceToLogicObject)
						.filter(Objects::nonNull);
		objectStream.forEach(iLogicObject -> {
			if (isActionApplicableForConductor(iLogicObject)) {
				selectedObjects.add((IFunctionBaseConductor) iLogicObject);
			}
		});
		if (!selectedObjects.isEmpty() && getActionUI() != null) {
			container.add(new ActionEntry(getActionUI(), null));
		}
	}

	public boolean isActionApplicableForConductor(@NotNull ILogicObject conductor)
	{
		boolean isApplicable =
				conductor instanceof IFunctionBaseConductor && !(conductor instanceof IFunctionConductor &&
						((IFunctionConductor) conductor).isAssociatedMessageSignal());
		return isApplicable;
	}

	@Override public void populateActiveCtxMenu(ActionContainer container)
	{

	}

	@NotNull @Override public String getActionUIClass()
	{
		return UpdateDictionaryActionUI.class.getName();
	}

	@NotNull @Override protected IActionEnum onActivate(ActionEvent e)
	{
		m_outputWindow = new OutputWindowWrapper(CAFUtils.getInstance().getOutputWindow());
		m_dictionaryHelper = getUpdateDictionaryActionHelper();
		return IActionEnum.eCompleted;
	}

	@NotNull protected UpdateDictionaryActionHelper getUpdateDictionaryActionHelper()
	{
		return new UpdateDictionaryActionHelper(m_outputWindow);
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		boolean sharedObjectsModified = false;
		Set<ISharedObject> sharedObjects=new HashSet<>();
		boolean isMultiSelection = selectedObjects.size() > 1;
		for (Map.Entry<IFunctionBaseConductor, Pair<INameTemplate, INameTemplate>> eligibleObjectEntry : populateSelectionObjectMap()
				.entrySet()) {
			IFunctionBaseConductor obj = eligibleObjectEntry.getKey();
			Pair<INameTemplate, INameTemplate> nameTemplatePair = eligibleObjectEntry.getValue();

			if (obj.isShared()) {
				ISharedConductor sharedConductor = CommonUtils.cast(obj.getSharedObject(), ISharedConductor.class);
				if (sharedConductor != null) {
					if (sharedConductor.isFrozen()) {
						ILogicDesign logicDesign = obj.getLogicDesign();
						if (logicDesign != null) {
							String type = sharedConductor.isSignal() ?
									ResourceMgr.getString(BatchUpdateFromDictionaryCmd.class,
											"UpdateDictionaryAction.SignalObjectType") :
									ResourceMgr.getString(BatchUpdateFromDictionaryCmd.class,
											"UpdateDictionaryAction.MessageObjectType");
							String message = ResourceMgr.getString(UpdateICDAction.class,
									"UpdateDictionaryAction.output.isFrozen", type, HTMLHelper.link(logicDesign, obj));
							m_outputWindow.sendMessage(message, m_dictionaryHelper.getOutputTabName(), true);
						}
						continue;
					}
					boolean lockSuccess = false;
					try {
						lockSuccess = m_dictionaryHelper.lockSharedConductor(obj, nameTemplatePair, sharedConductor,
								!isMultiSelection);
					}
					finally {
						if (lockSuccess) {
							sharedObjects.add(sharedConductor);
							LockUpdateHelper.flushAndUnlockSharedObject(sharedConductor);
							sharedObjectsModified = true;
						}
					}
				}
			}
			else {
				m_dictionaryHelper.applyUpdateAction(obj, nameTemplatePair);
			}
		}
		CreationDeletionHelper.getTheCreationHelper().processObjects();
		if (sharedObjectsModified) {
			LogicUtils.fireChangeEvent(sharedObjects);
			// Editing of shared objects is not undoable
			getController().getUndoableContainer().endEdit();
			getController().clearUndoQueue();
		}
		return true;
	}
}
