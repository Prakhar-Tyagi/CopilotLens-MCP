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
import chs.caplets.logic.actions.CreateOverbraidAction;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.ILibraryWire;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner})
public class CreateOverbraidFromPartBrowserAction extends PartBrowserAction
{

	public CreateOverbraidFromPartBrowserAction()
	{
		super(ResourceMgr.getString(CreateOverbraidFromPartBrowserAction.class,
				"CreateOverbraidFromPartBrowserAction.name.decl"),
				ResourceMgr.getString(CreateOverbraidFromPartBrowserAction.class,
						"CreateOverbraidFromPartBrowserAction.shortDesc.decl"),
				ResourceMgr.getString(CreateOverbraidFromPartBrowserAction.class,
						"CreateOverbraidFromPartBrowserAction.longDesc.decl"),
				(int) ResourceMgr.getMnemonic(CreateOverbraidFromPartBrowserAction.class,
						"CreateOverbraidFromPartBrowserAction.mnemonic"),
				CHSImageLoader.loadImageIcon(CHSImages.OVERBRAID_ICON));
	}

	public IAction getActionToPerform()
	{
		return CAFUtils.getInstance().getActiveCapletController().getAction(CreateOverbraidAction.class);
	}

	public boolean isApplicable(ILibraryObject libObj)
	{
		return (libObj instanceof ILibraryWire);
	}
}
