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
import chs.caplets.logic.actions.PlaceAssemblyTreeAction;
import chs.cof.parts.ILibraryAssembly;
import chs.cof.parts.ILibraryObject;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner,Application.CapitalEssentialsDesign,Application.SEElectricalDesign})
public class CreateAssemblyFromPartBrowserAction extends PartBrowserAction
{

	public CreateAssemblyFromPartBrowserAction()
	{
		super(ResourceMgr.getString(CreateAssemblyFromPartBrowserAction.class,
				"CreateAssemblyFromPartBrowserAction.name.decl"),
				ResourceMgr.getString(CreateAssemblyFromPartBrowserAction.class,
						"CreateAssemblyFromPartBrowserAction.shortDesc.decl"),
				ResourceMgr.getString(CreateAssemblyFromPartBrowserAction.class,
						"CreateAssemblyFromPartBrowserAction.longDesc.decl"),
				(int) ResourceMgr.getMnemonic(CreateAssemblyFromPartBrowserAction.class,
						"CreateAssemblyFromPartBrowserAction.mnemonic"),
				CHSImageLoader.loadImageIcon(CHSImages.ASSEMBLY_ICON));
	}

	public IAction getActionToPerform()
	{
		return CAFUtils.getInstance().getActiveCapletController().getAction(PlaceAssemblyTreeAction.class);
	}

	public boolean isApplicable(ILibraryObject libObj)
	{
		return libObj instanceof ILibraryAssembly;
	}
}
