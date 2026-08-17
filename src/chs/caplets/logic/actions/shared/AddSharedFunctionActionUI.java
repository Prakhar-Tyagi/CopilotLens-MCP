/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

@ApplicationSpecification(includeIn = {Application.ArtisanFunction})
public class AddSharedFunctionActionUI extends ActionUI
{

	public AddSharedFunctionActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		Integer iMnemonic =
				(int) ResourceMgr.getMnemonic(AddSharedDeviceActionUI.class, "AddSharedFunctionActionUI.mnemonic");
		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getString(AddSharedFunctionActionUI.class, "AddSharedFunctionActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(AddSharedFunctionActionUI.class, "AddSharedFunctionActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddSharedFunctionActionUI.class, "AddSharedFunctionActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon(CHSImages.SHARED_FUNCTION_ACTIVE_ICON));
	}

	@Override public String getActionClass()
	{
		return AddSharedFunctionAction.class.getName();
	}
}
