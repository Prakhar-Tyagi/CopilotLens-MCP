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
import chs.caplets.logic.actions.CreateWireAction;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.ILibraryWire;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
public class CreateWireFromPartBrowserAction extends PartBrowserAction
{

	public CreateWireFromPartBrowserAction()
	{
		super(ResourceMgr.getString(CreateWireFromPartBrowserAction.class,
				"CreateWireFromPartBrowserAction.name.decl"),
				ResourceMgr.getString(CreateWireFromPartBrowserAction.class,
						"CreateWireFromPartBrowserAction.shortDesc.decl"),
				ResourceMgr.getString(CreateWireFromPartBrowserAction.class,
						"CreateWireFromPartBrowserAction.longDesc.decl"),
				(int) ResourceMgr.getMnemonic(CreateWireFromPartBrowserAction.class,
						"CreateWireFromPartBrowserAction.mnemonic"),
				CHSImageLoader.loadImageIcon(CHSImages.WIRE_ICON_ENABLED));
	}

	public IAction getActionToPerform()
	{
		return CAFUtils.getInstance().getActiveCapletController().getAction(CreateWireAction.class);
	}

	public boolean isApplicable(ILibraryObject libObj)
	{
		return (libObj instanceof ILibraryWire);
	}
}
