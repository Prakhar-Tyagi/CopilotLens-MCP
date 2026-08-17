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
import chs.utilities.AppInfo;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.IEllipsesToolTipAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.ImageIcon;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign})
public class LayoutAssociateDesignsActionUI extends ActionUI implements IEllipsesToolTipAction
{

	public LayoutAssociateDesignsActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		boolean capitalDerivative = AppInfo.isCapitalDerivative();
		putValue(NAME,
				ResourceMgr.getStringForMenu(LayoutAssociateDesignsActionUI.class,
						capitalDerivative ? "LayoutAssociateDesignsActionUI.name.derivative.decl" :
								"LayoutAssociateDesignsActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(LayoutAssociateDesignsActionUI.class,
						capitalDerivative ? "LayoutAssociateDesignsActionUI.shortDesc.derivative.decl" :
								"LayoutAssociateDesignsActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(LayoutAssociateDesignsActionUI.class,
						capitalDerivative ? "LayoutAssociateDesignsActionUI.longDesc.derivative.decl" :
								"LayoutAssociateDesignsActionUI.longDesc.decl"));
		putValue(MNEMONIC_KEY,
				(int) ResourceMgr.getMnemonic(LayoutAssociateDesignsActionUI.class,
						"LayoutAssociateDesignsActionUI.mnemonic.text"));
		putValue(SMALL_ICON, getIcon());
	}

	@Override public String getActionClass()
	{
		return LayoutAssociateDesignsAction.class.getName();
	}

	protected ImageIcon getIcon()
	{
		return CHSImageLoader.loadImageIcon(CHSImages.LAYOUT_ASSOCIATE_ACTIVE_ICON);
	}

	@Nullable @Override public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon(CHSImages.LAYOUT_ASSOCIATE_INACTIVE_ICON);
	}

	@Override @Nullable public String getToolTipText()
	{
		// SHORT_DESCRIPTION or disabled reason
		return getDefaultDynamicToolTipText();
	}

	@Override public boolean shouldAppendTooltipEllipses()
	{
		return isEnabled();
	}
}
