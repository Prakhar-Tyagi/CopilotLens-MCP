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
import chs.caplets.logic.actions.CreateICDFromPlacedICDAction;
import chs.caplets.logic.actions.CreateICDFromPlacedICDActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_ALLOWED)
public class CreatePlacedICDFromICDBrowserAction extends AbstactICDBrowserAction
{

	public CreatePlacedICDFromICDBrowserAction()
	{
		putValue(NAME, ResourceMgr.getStringForMenu(
				CreateICDFromPlacedICDActionUI.class, "CreateICDFromPlacedICDActionUI.name.text"));
		putValue(SHORT_DESCRIPTION, ResourceMgr
				.getString(CreateICDFromPlacedICDActionUI.class, "CreateICDFromPlacedICDActionUI.shortDesc.text"));
		putValue(LONG_DESCRIPTION, ResourceMgr
				.getString(CreateICDFromPlacedICDActionUI.class, "CreateICDFromPlacedICDActionUI.longDesc.text"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_device_active.gif"));
	}
	
	@Nullable @Override protected IAction getAction(@NotNull ICapletController controller)
	{
		return controller.getAction(CreateICDFromPlacedICDAction.class);
	}

}
