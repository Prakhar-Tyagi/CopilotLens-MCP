/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign})
public class LayoutXDimensionActionUI extends ActionUI
{

	public LayoutXDimensionActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		putValue(NAME,
				ResourceMgr.getString(LayoutXDimensionActionUI.class, "LayoutXDimensionActionUI.putValue.action.name"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(LayoutXDimensionActionUI.class,
						"LayoutXDimensionActionUI.putValue.action.shortDescription"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(LayoutXDimensionActionUI.class,
						"LayoutXDimensionActionUI.putValue.action.longDescription"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon(CHSImages.LAYOUT_X_DIM_ACTIVE_ICON));
		putValue(MNEMONIC_KEY,
				(int) ResourceMgr
						.getMnemonic(LayoutXDimensionActionUI.class, "LayoutXDimensionActionUI.mnemonic.text"));
	}

	@Nullable @Override public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon(CHSImages.LAYOUT_X_DIM_INACTIVE_ICON);
	}

	@Override public String getActionClass()
	{
		return LayoutXDimensionAction.class.getName();
	}
}
