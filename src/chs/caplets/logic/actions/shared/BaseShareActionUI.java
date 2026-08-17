/*
 * Copyright 2006-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.caf.IFIB;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.caplet.selection.ISelectListener;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caf.caplet.selection.SelectEvent;
import chs.caf.caplet.selection.SelectSet;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IFunction;
import chs.cof.logical.cable.IFunctionConductor;
import chs.cof.logical.cable.IFunctionMessage;
import chs.cof.logical.cable.IInlineJackConnector;
import chs.cof.logical.cable.IInlinePlugConnector;
import chs.cof.logical.cable.IJackConnector;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IOverbraid;
import chs.cof.logical.cable.IPlugConnector;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IPinList;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;
/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */

public class BaseShareActionUI extends ActionUI implements ISelectListener
{

	protected String m_defaultName;
	protected String m_defaultShortDesc;
	protected String m_defualtLongDesc;
	protected String m_deviceShortDesc;
	protected String m_deviceLongDesc;
	protected String m_functionShortDesc;
	protected String m_functionLongDesc;
	protected String m_plugShortDesc;
	protected String m_plugLongDesc;
	protected String m_jackShortDesc;
	protected String m_jackLongDesc;
	protected String m_inlineShortDesc;
	protected String m_inlineLongDesc;
	protected String m_spliceShortDesc;
	protected String m_spliceLongDesc;
	protected String m_condGroupShortDesc;
	protected String m_condGroupLongDesc;
	protected String m_multicoreShortDesc;
	protected String m_multicoreLongDesc;
	protected String m_overbraidShortDesc;
	protected String m_overbraidLongDesc;
	protected String m_conductorShortDesc;
	protected String m_conductorLongDesc;
	protected String m_functionConductorShortDesc;
	protected String m_functionConductorLongDesc;
	protected String m_functionMessageShortDesc;
	protected String m_functionMessageLongDesc;

	/**
	 * Builds up all the appropriate strings - depending on the calling class - share or unshare.
	 *
	 * @param caplet
	 * @param subclass The calling class - ShareActioUI or UnshareActionUI
	 */
	protected BaseShareActionUI(ICaplet caplet, Class<?> subclass)
	{
		super(caplet);
		m_defaultName = ResourceMgr.getStringForMenu(subclass, subclass.getSimpleName() + ".name.decl");
		m_defaultShortDesc = ResourceMgr.getString(subclass, subclass.getSimpleName() + ".longDesc.decl");
		m_defualtLongDesc = ResourceMgr.getString(subclass, subclass.getSimpleName() + ".shortDesc.decl");

		m_deviceShortDesc = ResourceMgr.getString(subclass, subclass.getSimpleName() + ".Device.longDesc.decl");
		m_deviceLongDesc = ResourceMgr.getString(subclass, subclass.getSimpleName() + ".Device.shortDesc.decl");

		m_functionShortDesc= ResourceMgr.getString(subclass, subclass.getSimpleName() + ".Function.longDesc.decl");
		m_functionLongDesc = ResourceMgr.getString(subclass, subclass.getSimpleName() + ".Function.shortDesc.decl");

		m_plugShortDesc = ResourceMgr.getString(subclass, subclass.getSimpleName() + ".Plug.longDesc.decl");
		m_plugLongDesc = ResourceMgr.getString(subclass, subclass.getSimpleName() + ".Plug.shortDesc.decl");

		m_jackShortDesc = ResourceMgr.getString(subclass, subclass.getSimpleName() + ".Jack.longDesc.decl");
		m_jackLongDesc = ResourceMgr.getString(subclass, subclass.getSimpleName() + ".Jack.shortDesc.decl");

		m_inlineShortDesc = ResourceMgr.getString(subclass, subclass.getSimpleName() + ".Inline.longDesc.decl");
		m_inlineLongDesc = ResourceMgr.getString(subclass, subclass.getSimpleName() + ".Inline.shortDesc.decl");

		m_spliceShortDesc = ResourceMgr.getString(subclass, subclass.getSimpleName() + ".Splice.longDesc.decl");
		m_spliceLongDesc = ResourceMgr.getString(subclass, subclass.getSimpleName() + ".Splice.shortDesc.decl");

		m_condGroupShortDesc = ResourceMgr.getString(subclass, subclass.getSimpleName() + ".CondGroup.longDesc.decl");
		m_condGroupLongDesc = ResourceMgr.getString(subclass, subclass.getSimpleName() + ".CondGroup.shortDesc.decl");

		m_multicoreShortDesc = ResourceMgr.getString(subclass, subclass.getSimpleName() + ".Multicore.longDesc.decl");
		m_multicoreLongDesc = ResourceMgr.getString(subclass, subclass.getSimpleName() + ".Multicore.shortDesc.decl");

		m_overbraidShortDesc = ResourceMgr.getString(subclass, subclass.getSimpleName() + ".Overbraid.longDesc.decl");
		m_overbraidLongDesc = ResourceMgr.getString(subclass, subclass.getSimpleName() + ".Overbraid.shortDesc.decl");

		m_conductorShortDesc = ResourceMgr.getString(subclass, subclass.getSimpleName() + ".Conductor.longDesc.decl");
		m_conductorLongDesc = ResourceMgr.getString(subclass, subclass.getSimpleName() + ".Conductor.shortDesc.decl");

		m_functionConductorShortDesc =
				ResourceMgr.getString(subclass, subclass.getSimpleName() + ".FunctionConductor.longDesc.decl");
		m_functionConductorLongDesc =
				ResourceMgr.getString(subclass, subclass.getSimpleName() + ".FunctionConductor.shortDesc.decl");
		m_functionMessageShortDesc =
				ResourceMgr.getString(subclass, subclass.getSimpleName() + ".FunctionMessage.longDesc.decl");
		m_functionMessageLongDesc =
				ResourceMgr.getString(subclass, subclass.getSimpleName() + ".FunctionMessage.shortDesc.decl");
	}

	public void selectionChanged(SelectEvent e)
	{
		setTextFromOperand();
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		putValue(NAME, m_defaultName);
		putValue(SHORT_DESCRIPTION, m_defaultShortDesc);
		putValue(LONG_DESCRIPTION, m_defualtLongDesc);
		putValue(SMALL_ICON, icon);
	}

	public boolean isEnabled()
	{
		if (getFIB().isTaskActive(IFIB.TASK_SAVE)) {
			return false;
		}
		return super.isEnabled();
	}

	public String getActionClass()
	{
		return BaseShareAction.class.getName();
	}

	public void updateUI()
	{
		super.updateUI();
		setTextFromOperand();
	}

	/**
	 * Sets the appropriate text for the operation depending on the selection objects.
	 */
	private void setTextFromOperand()
	{
		putValue(NAME, m_defaultName);
		putValue(SHORT_DESCRIPTION, m_defaultShortDesc);
		putValue(LONG_DESCRIPTION, m_defualtLongDesc);
		ISelectMgr selMgr = CAFUtils.getInstance().getActiveSelectMgr();
		if (selMgr == null) {
			return;
		}
		SelectSet selection = selMgr.getPreSelections();
		if (selection == null) {
			return;
		}
		BaseShareActionOperands operands = BaseShareAction.getOperands(selection);
		if (operands != null) {
			//dts0100809894:we may have share action as disabled and tooltip should be set by disabled
			//action. somehow for placed objects with unplaced children the tooltip is being overridden
			//for share action we may have overhead of determining unplaced objects. so we need to do
			//the enabled check only if selection is there and correct operands are selected.
			IAction action = getAction();
			if (action == null || !action.isEnabled()) {
				return;
			}
			if (operands.target instanceof IPinList) {
				chs.cof.logical.cable.IPinList cpl = ((IPinList) operands.target).getConnectivity();
				if (cpl instanceof IDevice) {
					putValue(SHORT_DESCRIPTION, m_deviceShortDesc);
					putValue(LONG_DESCRIPTION, m_deviceLongDesc);
				}
				else if (cpl instanceof IFunction) {
					putValue(SHORT_DESCRIPTION,m_functionShortDesc);
					putValue(LONG_DESCRIPTION,m_functionLongDesc);
				}
				else if (cpl instanceof IInlineJackConnector
						|| cpl instanceof IInlinePlugConnector) {
					putValue(SHORT_DESCRIPTION, m_inlineShortDesc);
					putValue(LONG_DESCRIPTION, m_inlineLongDesc);
				}
				else if (cpl instanceof IPlugConnector) {
					putValue(SHORT_DESCRIPTION, m_plugShortDesc);
					putValue(LONG_DESCRIPTION, m_plugLongDesc);
				}
				else if (cpl instanceof IJackConnector) {
					putValue(SHORT_DESCRIPTION, m_jackShortDesc);
					putValue(LONG_DESCRIPTION, m_jackLongDesc);
				}
				else if (cpl instanceof ISplice) {
					putValue(SHORT_DESCRIPTION, m_spliceShortDesc);
					putValue(LONG_DESCRIPTION, m_spliceLongDesc);
				}
			}
			else if (operands.target instanceof IMulticore) {
				IMulticore mc = (IMulticore) operands.target;
				if (mc instanceof IOverbraid) {
					putValue(SHORT_DESCRIPTION, m_overbraidShortDesc);
					putValue(LONG_DESCRIPTION, m_overbraidLongDesc);
				}
				else if (!mc.isPartAssigned()) {
					putValue(SHORT_DESCRIPTION, m_condGroupShortDesc);
					putValue(LONG_DESCRIPTION, m_condGroupLongDesc);
				}
				else {
					putValue(SHORT_DESCRIPTION, m_multicoreShortDesc);
					putValue(LONG_DESCRIPTION, m_multicoreLongDesc);
				}
			}
			else if (operands.target instanceof IConductor) {
				setConductorDescriptions((IConductor)operands.target);
			}
		}
	}

	private void setConductorDescriptions(IConductor conductor)
	{
		if (conductor.getConnectivity() instanceof IFunctionConductor) {
			putValue(SHORT_DESCRIPTION, m_functionConductorShortDesc);
			putValue(LONG_DESCRIPTION, m_functionConductorLongDesc);
		}
		else if (conductor.getConnectivity() instanceof IFunctionMessage) {
			putValue(SHORT_DESCRIPTION, m_functionMessageShortDesc);
			putValue(LONG_DESCRIPTION, m_functionMessageLongDesc);
		}
		else {
			putValue(SHORT_DESCRIPTION, m_conductorShortDesc);
			putValue(LONG_DESCRIPTION, m_conductorLongDesc);
		}
	}
}
