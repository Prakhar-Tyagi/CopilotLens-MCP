/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.IOutputWindow;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.cof.logical.cable.IBaseDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IInlineJackConnector;
import chs.cof.logical.cable.IInlinePlugConnector;
import chs.cof.logical.cable.IJackConnector;
import chs.cof.logical.cable.INonInlineJackConnector;
import chs.cof.logical.cable.INonInlinePlugConnector;
import chs.cof.logical.cable.IPlugConnector;
import chs.cof.logical.schem.IPinList;
import chs.common.IParameterized;
import chs.common.IUIDObject;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.ModularSchemPinListInfo;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashSet;

public class JoinPinlistsAction extends ControllerActionRT implements ICtxMenuProvider
{

	private IOutputWindow m_output;
	private String m_outputTabName;
	@NotNull private JoinPinlistsHelper m_joinPinlistsHelper;

	public JoinPinlistsAction(ICapletController controller)
	{
		super(controller);
		m_output = CAFUtils.getInstance().getOutputWindow();
		m_outputTabName = ResourceMgr.getString(JoinPinlistsAction.class, "JoinPinlistsAction.output.tabname");
		m_joinPinlistsHelper = createJoinPinlistsHelper();
	}

	@NotNull protected JoinPinlistsHelper createJoinPinlistsHelper()
	{
		return new JoinPinlistsHelper(this::reportErrorMessage);
	}

	protected void reportErrorMessage(String errorMessage)
	{
		m_output.sendMessage(errorMessage, m_outputTabName, true);
	}

	@Override protected IActionEnum onActivate(ActionEvent e)
	{
		m_output.clearPane(m_outputTabName);
		if (!m_joinPinlistsHelper.isStitchPossibleOnPinlistsSelected(
				getController().getSelectMgr().getPreSelections().getSelectedObjects(IPinList.class))) {
			return IActionEnum.eCanceled;
		}

		return IActionEnum.eCompleted;
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		if (successful) {
			if (m_joinPinlistsHelper.hasValidOperand()) {
				return m_joinPinlistsHelper.completeEdits();
			}
			return true;
		}
		return false;
	}

	@Override public String getActionUIClass()
	{
		return JoinPinlistsActionUI.class.getName();
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		//make sure all the selected objects are of the same type
		if (checkValidSelection(selections).isEmpty()) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	@Override public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	public boolean isEnabled()
	{

		m_disabledReason = "";
		if (!(getController().getCapletModel().isEditable() && super.isEnabled())) {
			return false;
		}
		m_disabledReason = checkValidSelection(getController().getSelectMgr().getPreSelections());
		if (!m_disabledReason.isEmpty()) {
			return false;
		}
		return true;
	}

	protected String checkValidSelection(SelectSet selectionSet)
	{

		if (selectionSet.getSelectedObjects(IPinList.class).isEmpty()) {
			return ResourceMgr.getString(JoinPinlistsAction.class, "JoinPinlistAction.notenabled.PinlistsRequired");
		}

		IBaseDevice baseDeviceInSelection = null;
		IPlugConnector plugConnectorInSelection = null;
		IJackConnector jackConnectorInSelection = null;
		IInlinePlugConnector inlinePlugConnector = null;
		IInlineJackConnector inlineJackConnector = null;
		boolean plugConnectorInSelectionHasAttachedObjects = false;
		boolean jackConnectorInSelectionHasAttachedObjects = false;
		Collection<IPinList> slectedSchemObjects = new HashSet<IPinList>();
		for (SelectedUIDObjectIterator iter = selectionSet.getSelectedUIDObjects();
				iter.hasNext(); ) {

			IUIDObject uidObject = iter.getNext();

			if (uidObject instanceof IPinList) {
				if (m_joinPinlistsHelper.checkValidLogicObjectForStitch(uidObject) ==
						JoinPinlistsHelper.LogicObjectAcceptance.FAILED) {
					continue;
				}

				IPinList pinList = (IPinList) uidObject;
				IParameterized parameterized = pinList.getParameterized();
				if (parameterized == null) {
					return ResourceMgr.getString(JoinPinlistsAction.class, "JoinPinlistAction.NonParameterized",
							JoinPinlistsHelper.getType(pinList));
				}
				if (pinList.getConnectivity() instanceof IDeviceConnector) {
					continue;
				}
				if (pinList.getConnectivity() instanceof INonInlinePlugConnector ||
						pinList.getConnectivity() instanceof INonInlineJackConnector) {
					if (hasEmbeddedModularPinLists(pinList)) {
						continue;
					}
				}
				if (pinList.getConnectivity() instanceof IBaseDevice) {
					IBaseDevice currentBaseDevice = (IBaseDevice) pinList.getConnectivity();
					if (baseDeviceInSelection != null && currentBaseDevice != baseDeviceInSelection) {
						return ResourceMgr
								.getString(JoinPinlistsAction.class, "JoinPinlistAction.notenabled.NonUniqueInstances");
					}
					baseDeviceInSelection = currentBaseDevice;
				}
				else if (pinList.getConnectivity() instanceof IInlinePlugConnector) {
					IInlinePlugConnector currentInlinePlug = (IInlinePlugConnector) pinList.getConnectivity();
					if (inlinePlugConnector != null && currentInlinePlug != inlinePlugConnector) {
						return ResourceMgr
								.getString(JoinPinlistsAction.class,
										"JoinPinlistAction.notenabled.NonUniqueInstances");
					}
					inlinePlugConnector = currentInlinePlug;
				}
				else if (pinList.getConnectivity() instanceof IInlineJackConnector) {
					IInlineJackConnector currentInlineJack = (IInlineJackConnector) pinList.getConnectivity();
					if (inlineJackConnector != null && currentInlineJack != inlineJackConnector) {
						return ResourceMgr
								.getString(JoinPinlistsAction.class,
										"JoinPinlistAction.notenabled.NonUniqueInstances");
					}
					inlineJackConnector = currentInlineJack;
				}
				else if (pinList.getConnectivity() instanceof IPlugConnector) {
					IPlugConnector currentPlugConnector = (IPlugConnector) pinList.getConnectivity();
					if (plugConnectorInSelection != null && currentPlugConnector != plugConnectorInSelection) {
						if (pinList.getAttachedPinListObjects().isEmpty() ||
								!plugConnectorInSelectionHasAttachedObjects) {

							return ResourceMgr
									.getString(JoinPinlistsAction.class,
											"JoinPinlistAction.notenabled.NonUniqueInstances");
						}
					}
					plugConnectorInSelectionHasAttachedObjects = !pinList.getAttachedPinListObjects().isEmpty();
					plugConnectorInSelection = currentPlugConnector;
				}
				else if (pinList.getConnectivity() instanceof IJackConnector) {
					IJackConnector currentJackConnector = (IJackConnector) pinList.getConnectivity();
					if (jackConnectorInSelection != null && jackConnectorInSelection != currentJackConnector) {
						if (pinList.getAttachedPinListObjects().isEmpty() ||
								!jackConnectorInSelectionHasAttachedObjects) {

							return ResourceMgr
									.getString(JoinPinlistsAction.class,
											"JoinPinlistAction.notenabled.NonUniqueInstances");
						}
					}
					jackConnectorInSelectionHasAttachedObjects = !pinList.getAttachedPinListObjects().isEmpty();
					jackConnectorInSelection = currentJackConnector;
				}
				slectedSchemObjects.add((IPinList) uidObject);
			}
		}
		if (slectedSchemObjects.size() <= 1) {

			return ResourceMgr.getString(JoinPinlistsAction.class, "JoinPinlistAction.notenabled.TwoPinlistsRequired");
		}

		return "";
	}

	private boolean hasEmbeddedModularPinLists(IPinList pinList)
	{
		ModularSchemPinListInfo schemPinListInfo = new ModularSchemPinListInfo(pinList, false);
		return schemPinListInfo.getCandidates().stream().filter(o -> o != pinList).count() > 0;
	}
}