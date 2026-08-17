/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.partbrowser;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.cof.parts.ILibraryChannel;
import chs.cof.parts.ILibraryObject;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;

/**
 * @author chandras on 3-10-2019.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
public class CreateDuctWithPartAndSymbolFromPartBrowserAction
		extends CreateLayoutComponentWithPartAndSymbolFromPartBrowserAction
{

	public CreateDuctWithPartAndSymbolFromPartBrowserAction()
	{
		super(ResourceMgr.getString(CreateDuctWithPartAndSymbolFromPartBrowserAction.class,
				"CreateDuctWithPartAndSymbolFromPartBrowserAction.name.decl"),
				ResourceMgr.getString(CreateDuctWithPartAndSymbolFromPartBrowserAction.class,
						"CreateDuctWithPartAndSymbolFromPartBrowserAction.shortDesc.decl"),
				ResourceMgr.getString(CreateDuctWithPartAndSymbolFromPartBrowserAction.class,
						"CreateDuctWithPartAndSymbolFromPartBrowserAction.longDesc.decl"),
				(int) ResourceMgr.getMnemonic(CreateDuctWithPartAndSymbolFromPartBrowserAction.class,
						"CreateDuctWithPartAndSymbolFromPartBrowserAction.mnemonic"),
				CHSImageLoader.loadImageIcon(CHSImages.LAYOUT_DUCT_ACTIVE_ICON));
	}

	public boolean isApplicable(ILibraryObject libObj)
	{
		return libObj instanceof ILibraryChannel;
	}
}
