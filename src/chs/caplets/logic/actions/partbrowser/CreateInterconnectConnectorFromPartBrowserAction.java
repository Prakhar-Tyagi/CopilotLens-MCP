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
import chs.caf.cafmain.actions.partbrowser.PartBrowserAction;
import chs.caf.caplet.action.IAction;
import chs.caplets.logic.actions.CreateInterconnectConnectorAction;
import chs.cof.parts.ILibraryBaseConnector;
import chs.cof.parts.ILibraryObject;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner})
public class CreateInterconnectConnectorFromPartBrowserAction extends PartBrowserAction
{

	public CreateInterconnectConnectorFromPartBrowserAction()
	{
		super(ResourceMgr.getString(CreateInterconnectConnectorFromPartBrowserAction.class,
				"CreateInterconnectConnectorFromPartBrowserAction.name.decl"),
				ResourceMgr.getString(CreateInterconnectConnectorFromPartBrowserAction.class,
						"CreateInterconnectConnectorFromPartBrowserAction.shortDesc.decl"),
				ResourceMgr.getString(CreateInterconnectConnectorFromPartBrowserAction.class,
						"CreateInterconnectConnectorFromPartBrowserAction.longDesc.decl"),
				(int) ResourceMgr.getMnemonic(CreateInterconnectConnectorFromPartBrowserAction.class,
						"CreateInterconnectConnectorFromPartBrowserAction.mnemonic"),
				CHSImageLoader.loadImageIcon(CHSImages.INTERCONNECT_CONNECTOR_ICON_ENABLED));
	}

	public IAction getActionToPerform()
	{
		return CAFUtils.getInstance().getActiveCapletController().getAction(
				CreateInterconnectConnectorAction.class.getName());
	}

	public boolean isApplicable(ILibraryObject libObj)
	{
		return (libObj instanceof ILibraryBaseConnector) && libObj.getNumCavities() >= 1;
	}
}
