/*
 * Copyright 2007-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.caplet.selection.ISelectListener;
import chs.caf.caplet.selection.SelectEvent;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.annotations.Application;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IWireConductor;
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

/**
 * Created by jamesmw User: jamesmw Date: 22-Jun-2007 Time: 17:58:07
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner})
public class UnassignPortActionUI extends ActionUI implements ISelectListener
{

	public UnassignPortActionUI(ICaplet caplet)
	{
		super(caplet);
		getCaplet().getFIB().getAppActionMgr().addSelectListener(this, true);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		putValue(MNEMONIC_KEY,
				new Integer(ResourceMgr.getMnemonic(UnassignPortActionUI.class, "UnassignPortActionUI.mnemonic.decl")));
		putValue(NAME, ResourceMgr.getString(UnassignPortActionUI.class, "UnassignPortActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(UnassignPortActionUI.class, "UnassignPortActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(UnassignPortActionUI.class, "UnassignPortActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	public void updateUI()
	{
		if (getAction() != null) {
			IConductor conductor = ((UnassignPortAction) getAction()).getOperand();
			if (conductor instanceof INetConductor) {
				putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_net_active.gif"));
			}
			else if (conductor instanceof IWireConductor) {
				putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_wire_active.gif"));
			}
		}
	}

	public boolean isEnabled()
	{
		return super.isEnabled();
	}

	public String getActionClass()
	{
		return UnassignPortAction.class
				.getName();  //To change body of implemented methods use File | Settings | File Templates.
	}

	public void selectionChanged(SelectEvent e)
	{
		updateUI();
	}
}
