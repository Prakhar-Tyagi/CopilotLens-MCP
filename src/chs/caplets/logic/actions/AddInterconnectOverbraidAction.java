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

import chs.caf.ActionEntry;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.ISpecialSelection;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caplets.logic.Model;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IInterconnectMember;
import chs.cof.logical.cable.IInterconnectToDoItem;
import chs.cof.logical.cable.IOverbraid;
import chs.cof.logical.cable.IShieldBody;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.parts.ILibraryWire;
import chs.cofUtils.parameterized.IndicatorHelper;
import chs.common.IUIDObject;
import chs.system.FactoryMgr;
import chs.utility.logic.LogicObjectUtils;

import java.awt.event.ActionEvent;

public class AddInterconnectOverbraidAction extends ControllerActionRT
{

	ISpecialSelection m_libSelectMgr;
	private IDesign m_design = null;
	private IConnectivity m_connectivity = null;
	private ISchemDiagram m_diagram = null;
	private IInterconnectToDoItem m_icxToDoItem;

	public AddInterconnectOverbraidAction(ICapletController controller, ISpecialSelectMgr libSelectMgr)
	{
		super(controller);
		Model model = (Model) controller.getCapletModel();
		m_design = model.getDesign();
		m_connectivity = m_design.getConnectivity();
		m_diagram = model.getDiagram();
		m_libSelectMgr = libSelectMgr;
		if (getActionUI() != null) {
			libSelectMgr.contextMenuAddAction(new ActionEntry(getActionUI())
			{
				public boolean shouldDisplay()
				{
					return isEnabled();
				}
			});
		}
	}

	public String getActionUIClass()
	{
		return AddInterconnectOverbraidActionUI.class.getName();
	}

	public boolean isEnabled()
	{
		return getController().getCapletModel().isEditable() && getOperand() && super.isEnabled();
	}

	private boolean getOperand()
	{
		if (m_libSelectMgr.getSelectedObjects().getSize() == 1) {
			IUIDObject uidObj = m_libSelectMgr.getSelectedObjects().getNext();
			if (uidObj instanceof IInterconnectToDoItem
					&& ((IInterconnectToDoItem) uidObj).getPartClass() == IInterconnectMember.TYPE_OVERBRAID
					&& ((IInterconnectToDoItem) uidObj).getLibraryObject() instanceof ILibraryWire) {
				m_icxToDoItem = (IInterconnectToDoItem) uidObj;
				return true;
			}
		}
		return false;
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eCompleted;
	}

	protected boolean onTerminate(boolean successful)
	{
		IOverbraid overbraid = FactoryMgr.getCableFactory().createOverbraid(FactoryMgr.getCommonFactory().createUID());
		overbraid.assignLibraryPart(m_icxToDoItem.getLibraryObject());
		IShieldBody cableShieldBody =
				LogicObjectUtils.createCableShieldBody(false, IndicatorHelper.getDefaultOverbraidIndicatorType(),
						overbraid);
		overbraid.setShieldBody(cableShieldBody);
		cableShieldBody.setConnectivity(m_connectivity);

		m_design.getInterconnectSourceInfo().addConductorDerivation(m_icxToDoItem, overbraid);
		overbraid.setHarness(m_design.getInterconnectSourceInfo().getHarness(m_icxToDoItem));
		m_connectivity.addMulticore(overbraid);

		return true;
	}
}
