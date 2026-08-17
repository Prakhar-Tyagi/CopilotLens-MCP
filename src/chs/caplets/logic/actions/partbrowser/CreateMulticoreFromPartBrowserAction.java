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
import chs.caplets.logic.actions.AddLibraryMulticoreAction;
import chs.cof.parts.ILibraryMulticore;
import chs.cof.parts.ILibraryObject;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
public class CreateMulticoreFromPartBrowserAction extends PartBrowserAction
{

	public CreateMulticoreFromPartBrowserAction()
	{
		super(ResourceMgr.getString(CreateMulticoreFromPartBrowserAction.class,
				"CreateMulticoreFromPartBrowserAction.name.decl"),
				ResourceMgr.getString(CreateMulticoreFromPartBrowserAction.class,
						"CreateMulticoreFromPartBrowserAction.shortDesc.decl"),
				ResourceMgr.getString(CreateMulticoreFromPartBrowserAction.class,
						"CreateMulticoreFromPartBrowserAction.longDesc.decl"),
				(int) ResourceMgr.getMnemonic(CreateMulticoreFromPartBrowserAction.class,
						"CreateMulticoreFromPartBrowserAction.mnemonic"),
				CHSImageLoader.loadImageIcon(CHSImages.MULTICORE_ICON_ENABLED));
	}

	public IAction getActionToPerform()
	{
		return CAFUtils.getInstance().getActiveCapletController().getAction(AddLibraryMulticoreAction.class);
	}

	public boolean isApplicable(ILibraryObject libObj)
	{
		return (libObj instanceof ILibraryMulticore);
	}
}
