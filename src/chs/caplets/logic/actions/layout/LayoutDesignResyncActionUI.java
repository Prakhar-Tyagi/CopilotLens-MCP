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
import chs.utilities.ui.IEllipsesToolTipAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.ImageIcon;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign})
public class LayoutDesignResyncActionUI extends ActionUI implements IEllipsesToolTipAction
{

	public LayoutDesignResyncActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		putValue(NAME,
				ResourceMgr.getStringForMenu(LayoutDesignResyncActionUI.class,
						"LayoutDesignResyncActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(LayoutDesignResyncActionUI.class,
						"LayoutDesignResyncActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(LayoutDesignResyncActionUI.class,
						"LayoutDesignResyncActionUI.longDesc.decl"));
		putValue(SMALL_ICON, getIcon());
		putValue(MNEMONIC_KEY,
				(int) ResourceMgr.getMnemonic(LayoutAssociateDesignsActionUI.class,
						"LayoutDesignResyncActionUI.mnemonic.text"));
	}

	@Override public String getActionClass()
	{
		return LayoutDesignResyncAction.class.getName();
	}

	private ImageIcon getIcon()
	{
		return CHSImageLoader.loadImageIcon(CHSImages.LAYOUT_SYNC_ACTIVE_ICON);
	}

	@Nullable @Override public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon(CHSImages.LAYOUT_SYNC_INACTIVE_ICON);
	}

	@Override @Nullable public String getToolTipText()
	{
		// SHORT_DESCRIPTION or disabled reason
		return getDefaultDynamicToolTipText();
	}

	@Override public boolean shouldAppendTooltipEllipses()
	{
		return false;
	}
}
