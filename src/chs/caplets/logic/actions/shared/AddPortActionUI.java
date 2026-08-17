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
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.annotations.Application;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.INetConductor;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
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
 * Created by jamesmw User: jamesmw Date: 21-Jun-2007 Time: 13:50:42
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner})
public class AddPortActionUI extends ActionUI
{

	public AddPortActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return AddPortAction.class.getName();
	}

	public void updateUI()
	{
		if (getAction() != null) {
			IConductor conductor = ((AddPortAction) getAction()).getOperand();
			if (conductor instanceof INetConductor) {
				putValue(MNEMONIC_KEY,
						new Integer(
								ResourceMgr.getMnemonic(AddPortActionUI.class, "AddPortActionUI.net.mnemonic.decl")));
				putValue(NAME, ResourceMgr.getString(AddPortActionUI.class, "AddPortActionUI.net.name.decl"));
				putValue(SHORT_DESCRIPTION,
						ResourceMgr.getString(AddPortActionUI.class, "AddPortActionUI.net.shortDesc.decl"));
				putValue(LONG_DESCRIPTION,
						ResourceMgr.getString(AddPortActionUI.class, "AddPortActionUI.net.longDesc.decl"));
				putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_net_active.gif"));
			}
			else {
				putValue(MNEMONIC_KEY,
						new Integer(
								ResourceMgr.getMnemonic(AddPortActionUI.class, "AddPortActionUI.wire.mnemonic.decl")));
				putValue(NAME, ResourceMgr.getString(AddPortActionUI.class, "AddPortActionUI.wire.name.decl"));
				putValue(SHORT_DESCRIPTION,
						ResourceMgr.getString(AddPortActionUI.class, "AddPortActionUI.wire.shortDesc.decl"));
				putValue(LONG_DESCRIPTION,
						ResourceMgr.getString(AddPortActionUI.class, "AddPortActionUI.wire.longDesc.decl"));
				putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_wire_active.gif"));
			}
		}
	}

	public void setupUI()
	{
	}
}
