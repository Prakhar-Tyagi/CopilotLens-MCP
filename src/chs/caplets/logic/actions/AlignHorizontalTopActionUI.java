/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Aug 2, 2004 Time: 1:29:13 PM
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
public class AlignHorizontalTopActionUI extends ActionUI
{

	public AlignHorizontalTopActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return AlignHorizontalTopAction.class.getName();
	}

	public void setupUI()
	{
		putValue(MNEMONIC_KEY, new Integer(
				ResourceMgr.getMnemonic(AlignHorizontalTopActionUI.class, "AlignHorizontalTopActionUI.mnemonic.decl")));
		putValue(NAME, ResourceMgr.getString(AlignHorizontalTopActionUI.class, "AlignHorizontalTopActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(AlignHorizontalTopActionUI.class, "AlignHorizontalTopActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AlignHorizontalTopActionUI.class, "AlignHorizontalTopActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));
	}
}
