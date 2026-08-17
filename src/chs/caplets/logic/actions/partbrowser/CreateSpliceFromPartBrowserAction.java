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
import chs.caplets.logic.actions.AddSpliceFromLibraryPartAction;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.ILibrarySolderSleeve;
import chs.cof.parts.ILibrarySplice;
import chs.cof.parts.ILibraryUltrasonicWeld;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
public class CreateSpliceFromPartBrowserAction extends PartBrowserAction
{

	public CreateSpliceFromPartBrowserAction()
	{
		super(ResourceMgr.getString(CreateSpliceFromPartBrowserAction.class,
				"CreateSpliceFromPartBrowserAction.name.decl"),
				ResourceMgr.getString(CreateSpliceFromPartBrowserAction.class,
						"CreateSpliceFromPartBrowserAction.shortDesc.decl"),
				ResourceMgr.getString(CreateSpliceFromPartBrowserAction.class,
						"CreateSpliceFromPartBrowserAction.longDesc.decl"),
				(int) ResourceMgr.getMnemonic(CreateSpliceFromPartBrowserAction.class,
						"CreateSpliceFromPartBrowserAction.mnemonic"),
				CHSImageLoader.loadImageIcon(CHSImages.SPLICE_ICON_ENABLED));
	}

	public IAction getActionToPerform()
	{
		return CAFUtils.getInstance().getActiveCapletController().getAction(AddSpliceFromLibraryPartAction.class);
	}

	public boolean isApplicable(ILibraryObject libObj)
	{
		return (libObj instanceof ILibraryUltrasonicWeld || libObj instanceof ILibrarySolderSleeve ||
				libObj instanceof ILibrarySplice);
	}
}
