/*
 * Copyright 2006 Mentor Graphics Corporation
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
import chs.caplets.logic.actions.CreateInterconnectDeviceAction;
import chs.cof.parts.ILibraryDevice;
import chs.cof.parts.ILibraryObject;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner})
public class CreateInterconnectDeviceFromPartBrowserAction extends PartBrowserAction
{

	public CreateInterconnectDeviceFromPartBrowserAction()
	{
		super(ResourceMgr.getString(CreateInterconnectDeviceFromPartBrowserAction.class,
				"CreateInterconnectDeviceFromPartBrowserAction.name.decl"),
				ResourceMgr.getString(CreateInterconnectDeviceFromPartBrowserAction.class,
						"CreateInterconnectDeviceFromPartBrowserAction.shortDesc.decl"),
				ResourceMgr.getString(CreateInterconnectDeviceFromPartBrowserAction.class,
						"CreateInterconnectDeviceFromPartBrowserAction.longDesc.decl"),
				(int) ResourceMgr.getMnemonic(CreateInterconnectDeviceFromPartBrowserAction.class,
						"CreateInterconnectDeviceFromPartBrowserAction.mnemonic"),
				CHSImageLoader.loadImageIcon(CHSImages.INTERCONNECT_DEVICE_ICON_ENABLED));
	}

	public IAction getActionToPerform()
	{
		return CAFUtils.getInstance().getActiveCapletController().getAction(CreateInterconnectDeviceAction.class);
	}

	public boolean isApplicable(ILibraryObject libObj)
	{
		return (libObj instanceof ILibraryDevice) && libObj.getNumCavities() >= 1;
	}
}
