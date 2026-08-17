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
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.ILibraryOther;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;

/**
 * @author chandras on 3-10-2019.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
public class CreateOtherComponentOnlyWithPartFromPartBrowserAction
		extends CreateLayoutComponentOnlyWithPartFromPartBrowserAction
{

	public CreateOtherComponentOnlyWithPartFromPartBrowserAction()
	{
		super(ResourceMgr.getString(CreateOtherComponentOnlyWithPartFromPartBrowserAction.class,
				"CreateOtherComponentOnlyWithPartFromPartBrowserAction.name.decl"),
				ResourceMgr.getString(CreateOtherComponentOnlyWithPartFromPartBrowserAction.class,
						"CreateOtherComponentOnlyWithPartFromPartBrowserAction.shortDesc.decl"),
				ResourceMgr.getString(CreateOtherComponentOnlyWithPartFromPartBrowserAction.class,
						"CreateOtherComponentOnlyWithPartFromPartBrowserAction.longDesc.decl"),
				(int) ResourceMgr.getMnemonic(CreateOtherComponentOnlyWithPartFromPartBrowserAction.class,
						"CreateOtherComponentOnlyWithPartFromPartBrowserAction.mnemonic"),
				CHSImageLoader.loadImageIcon(CHSImages.LAYOUT_OTHERCOMP_ACTIVE_ICON));
	}

	public boolean isApplicable(ILibraryObject libObj)
	{
		return libObj instanceof ILibraryOther;
	}
}
