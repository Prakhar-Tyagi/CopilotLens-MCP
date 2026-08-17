/*
 * Copyright 2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.partbrowser;

import chs.caf.CAFUtils;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.action.IAction;
import chs.caplets.logic.actions.CreateNoPinPlugConnectorAction;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
public class CreatePlugFromPartBrowserAction extends CreateConnectorFromPartBrowserAction
{

	public CreatePlugFromPartBrowserAction()
	{
		super(ResourceMgr.getString(CreatePlugFromPartBrowserAction.class,
				"CreatePlugFromPartBrowserAction.name.decl"),
				ResourceMgr.getString(CreatePlugFromPartBrowserAction.class,
						"CreatePlugFromPartBrowserAction.shortDesc.decl"),
				ResourceMgr.getString(CreatePlugFromPartBrowserAction.class,
						"CreatePlugFromPartBrowserAction.longDesc.decl"),
				(int) ResourceMgr.getMnemonic(CreatePlugFromPartBrowserAction.class,
						"CreatePlugFromPartBrowserAction.mnemonic"),
				CHSImageLoader.loadImageIcon(CHSImages.PLUG_CONNECTOR_ICON_ENABLED));
	}

	public IAction getActionToPerform()
	{
		return CAFUtils.getInstance().getActiveCapletController().getAction(CreateNoPinPlugConnectorAction.class);
	}
}
