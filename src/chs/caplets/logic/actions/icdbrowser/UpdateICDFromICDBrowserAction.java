/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.logic.actions.icdbrowser;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IAction;
import chs.caplets.logic.actions.UpdateICDAction;
import chs.caplets.logic.actions.UpdateICDActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
public class UpdateICDFromICDBrowserAction extends AbstactICDBrowserAction
{

	public UpdateICDFromICDBrowserAction()
	{
		putValue(MNEMONIC_KEY, (int) 'U');
		putValue(NAME,
				ResourceMgr.getString(UpdateICDActionUI.class, "UpdateICDActionUI.name"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(UpdateICDActionUI.class, "UpdateICDActionUI.short"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(UpdateICDActionUI.class, "UpdateICDActionUI.longDesc"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_device_active.gif"));
	}

	@Nullable protected IAction getAction(@NotNull ICapletController controller)
	{
		return controller.getAction(UpdateICDAction.class);
	}
}
