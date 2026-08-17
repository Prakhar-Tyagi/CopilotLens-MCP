/*
 * Copyright 2005-2008 Mentor Graphics Corporation
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
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.selection.SelectSet;

public class AddInterconnectShieldAction extends AddLibraryInnercoreShieldAction
{

	public AddInterconnectShieldAction(ICapletController controller, ISpecialSelectMgr libSelectMgr)
	{
		super(controller, libSelectMgr);
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

	@Override protected boolean checkCache()
	{
		return false;
	}

	public String getActionUIClass()
	{
		return AddInterconnectShieldActionUI.class.getName();
	}

	// We don't want to be on the regular context menu
	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
	}
}
