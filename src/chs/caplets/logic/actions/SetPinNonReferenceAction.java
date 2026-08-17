/*
 * Copyright 2005-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.IOutputWindow;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDeviceOwned;
import chs.cof.logical.cable.IDeviceOwnedConnector;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.shared.CommonInSharedPinDBReservationAndDesignScope;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinReservationView;
import chs.cof.logical.shared.SharedPinHelper;
import chs.common.IProjectPreferenceMgr;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utility.ProjectHelper;
import chs.utility.logic.PinUtils;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class SetPinNonReferenceAction extends ControllerActionRT implements ICtxMenuProvider
{

	private enum UnreferenceState
	{
		IS_USED_AND_DISALLOW_DUPLICATION,
		CAN_NOT_BE_RESERVED,
		OK_TO_UNREFERNCE,
	}

	private List m_pins;

	public SetPinNonReferenceAction(ICapletController controller)
	{
		super(controller);
		m_pins = null;
	}

	/* (non-Javadoc)
		 * @see chs.caf.caplet.helpers.ActionRT#onActivate(java.awt.event.ActionEvent)
		 */
	protected IActionEnum onActivate(ActionEvent e)
	{
		SelectSet preSelections = getController().getSelectMgr().getPreSelections();
		m_pins = new Vector();
		for (IDiagramObject obj : LogicMultiUserSelectionFilter
				.getValidDiagramObjectOperands(getController().getSelectMgr().getPreSelections())) {

			if ((obj instanceof IPin)) {
				m_pins.add(obj);
			}
		}
		return IActionEnum.eCompleted;
	}

	/* (non-Javadoc)
	 * @see chs.caf.caplet.helpers.ActionRT#onTerminate(boolean)
	 */
	protected boolean onTerminate(boolean successful)
	{
		if (successful) {
			Set<ISharedPin> nonReferenceUsage = new HashSet<ISharedPin>();
			IOutputWindow m_outputWindow = CAFUtils.getInstance().getOutputWindow();
			Iterator iter = m_pins.iterator();
			while (iter.hasNext()) {
				IPin pin = (IPin) iter.next();
				IAbstractPin apin = pin.getConnectivity();
				ILogicDesign design = apin.getOwner().getLogicDesign();

				if (pin.isReference()) {
					UnreferenceState state = UnreferenceState.OK_TO_UNREFERNCE;
					if (apin.isShared()) {
						ISharedPin shpin = apin.getSharedPin();
						state = getUnreferenceState(shpin, design, nonReferenceUsage.contains(shpin));
					}
					else if (design != null) {
						state = PinUtils.isPinPlaceableInDesign(apin, design, false) ?
								UnreferenceState.OK_TO_UNREFERNCE : UnreferenceState.IS_USED_AND_DISALLOW_DUPLICATION;
					}
					switch (state) {
						case OK_TO_UNREFERNCE: {
							pin.setReference(false);
							if (apin.isShared()) {
								nonReferenceUsage.add((ISharedPin) pin.getSharedObject());
							}
							break;
						}
						case CAN_NOT_BE_RESERVED: {
							m_outputWindow.sendApplicationMessage(
									ResourceMgr.getString(SetPinReferenceAction.class,
											"SetPinNonReferenceAction.notReserved.warning", apin.getName()));
							break;
						}
						case IS_USED_AND_DISALLOW_DUPLICATION: {
							m_outputWindow.sendApplicationMessage(
									ResourceMgr.getString(SetPinReferenceAction.class,
											"SetPinNonReferenceAction.used.warning", apin.getName()));
							break;
						}
						default:
							break;
					}
				}
			}

			nonReferenceUsage.clear();
		}

		return successful;
	}

	private UnreferenceState getUnreferenceState(ISharedPin shpin, ILogicDesign design, boolean wasProcessed)
	{
		if (wasProcessed || SharedPinHelper.isUsed(shpin, design)) {
			IProjectPreferenceMgr preferences = ProjectHelper.getProjectPreferences(shpin.getProject());
			if (preferences.getDisallowLogicPinDuplication()) {
				return UnreferenceState.IS_USED_AND_DISALLOW_DUPLICATION;
			}
		}
		ISharedPinReservationView pinview =
				FactoryMgr.getCommonFactory().constructSharedPinReservationView(shpin.getOwner());

		CommonInSharedPinDBReservationAndDesignScope commonInSharedPinDBReservationAndDesignScope =
				SharedPinHelper.createSharedPinDBReservationScope(design, pinview.getSharedPinDBReservations(shpin), shpin);

		if (!SharedPinHelper.isAvailable(shpin,  commonInSharedPinDBReservationAndDesignScope)) {
			return UnreferenceState.CAN_NOT_BE_RESERVED;
		}
		return UnreferenceState.OK_TO_UNREFERNCE;
	}

	/* (non-Javadoc)
			  * @see chs.caf.ICtxMenuProvider#populateCtxMenu(chs.caf.ActionContainer, chs.caf.caplet.selection.SelectSet)
			  */
	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (isEnabled()) {
			setFullNamedContextMenu(container);
		}
	}

	/* (non-Javadoc)
	 * @see chs.caf.ICtxMenuProvider#populateActiveCtxMenu(chs.caf.ActionContainer)
	 */
	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	/* (non-Javadoc)
	 * @see chs.caf.caplet.action.IAction#isEnabled()
	 */
	public boolean isEnabled()
	{
		if (!getController().getCapletModel().isEditable()) {
			return false;
		}

		boolean isEnabled = false;

		for (IDiagramObject obj : LogicMultiUserSelectionFilter
				.getValidDiagramObjectOperands(getController().getSelectMgr().getPreSelections())) {
			if ((obj instanceof IPin)) {
				IPin pin = (IPin) obj;
				IAbstractPin apin = pin.getConnectivity();
				ILogicDesign design = apin.getOwner().getLogicDesign();

				// If this reference pin is connected to something - can't make it non-reference. (inline exempted)
				if (needsConnectionCheck(apin) && pin.isConnectedToSomething()) {
					return false;
				}

				ISharedPin shpin = apin.getSharedPin();

				if (shpin != null) {
					if (!SharedPinHelper.isAvailable(shpin, design)) {
						continue;
					}
				}
				else if (design != null && !PinUtils.isPinPlaceableInDesign(apin, design, false)) {
					continue;
				}

				isEnabled |= pin.isReference();
			}
		}

		return isEnabled && super.isEnabled();
	}

	private boolean needsConnectionCheck(IAbstractPin apin)
	{
		IPinList owner = apin.getOwner();
		if (owner instanceof IGenericInlineConnector) {
			return false;
		}
		if (owner instanceof IConnector) {
			return owner instanceof IDeviceOwnedConnector && ((IDeviceOwned) owner).getOwner() != null;
		}
		return true;
	}

	/* (non-Javadoc)
		 * @see chs.caf.caplet.action.IAction#getActionUIClass()
		 */
	public String getActionUIClass()
	{
		return SetPinNonReferenceActionUI.class.getName();
	}
}