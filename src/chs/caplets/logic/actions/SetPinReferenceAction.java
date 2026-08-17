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
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDeviceOwned;
import chs.cof.logical.cable.IDeviceOwnedConnector;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.IPin;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class SetPinReferenceAction extends ControllerActionRT implements ICtxMenuProvider
{

	private List m_pins;

	public SetPinReferenceAction(ICapletController controller)
	{
		super(controller);
		m_pins = null;
	}

	/* (non-Javadoc)
		 * @see chs.caf.caplet.helpers.ActionRT#onActivate(java.awt.event.ActionEvent)
		 */
	protected IActionEnum onActivate(ActionEvent e)
	{

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

			Iterator iter = m_pins.iterator();
			while (iter.hasNext()) {
				IPin pin = (IPin) iter.next();

				// No need to set if already is a reference pin
				if (pin.isReference()) {
					continue;
				}

				// Can't change the reference of a connected pin!
				if (isConnected(pin)) {
					IOutputWindow m_outputWindow = CAFUtils.getInstance().getOutputWindow();
					m_outputWindow.sendApplicationMessage(
							ResourceMgr.getString(SetPinReferenceAction.class,
									"SetPinReferenceAction.reference.warning",
									pin.getConnectivity().getName()));
					continue;
				}

				pin.setReference(true);
			}
		}

		return successful;
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

		// Return true on first unconnected shared pin in select list
		SelectSet selections = getController().getSelectMgr().getPreSelections();
		for (IDiagramObject obj : LogicMultiUserSelectionFilter.getValidDiagramObjectOperands(selections)) {
			if ((obj instanceof IPin)) {
				IPin thePin = (IPin) obj;

				if (isConnected(thePin)) {
					// If the pin is connected to something - can't make it reference.
					return false;
				}
				if (!thePin.getConnectivity().getOwner().canHaveReferencePin()) {
					return false;
				}

				isEnabled |= !thePin.isReference();
			}
		}

		return isEnabled && super.isEnabled();
	}

	private boolean isConnected(IPin pin)
	{
		if (connectedPinCheckNotNeeded(pin.getConnectivity().getOwner())) {
			return !pin.getConductors().isEmpty();
		}
		return pin.isConnectedToSomething();
	}

	private boolean connectedPinCheckNotNeeded(@Nullable IPinList owner)
	{
		if (owner instanceof IDeviceOwnedConnector) {
			return ((IDeviceOwned) owner).getOwner() == null;
		}
		return owner instanceof IConnector;
	}

	/* (non-Javadoc)
	 * @see chs.caf.caplet.action.IAction#getActionUIClass()
	 */
	public String getActionUIClass()
	{
		return SetPinReferenceActionUI.class.getName();
	}
}
