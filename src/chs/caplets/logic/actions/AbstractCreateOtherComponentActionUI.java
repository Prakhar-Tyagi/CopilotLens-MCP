/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions;

import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * @author chandras on 3-10-2019.
 */
public abstract class AbstractCreateOtherComponentActionUI extends ActionUI
{

	private final String m_sActionUIClass;

	protected AbstractCreateOtherComponentActionUI(ICaplet caplet, String sActionUIClass)
	{
		super(caplet);
		m_sActionUIClass = sActionUIClass;
	}

	public void setupUI()
	{
		Icon icon = getIcon();
		final Class<? extends AbstractCreateOtherComponentActionUI> aClass = getClass();
		final String simpleName = aClass.getSimpleName();
		putValue(NAME, ResourceMgr.getStringForMenu(aClass, simpleName + ".name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(aClass, simpleName + ".shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(aClass, simpleName + ".longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	protected Icon getIcon()
	{
		return CHSImageLoader.loadImageIcon(CHSImages.LAYOUT_OTHERCOMP_ACTIVE_ICON);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon(CHSImages.LAYOUT_OTHERCOMP_INACTIVE_ICON);
	}

	public String getActionClass()
	{
		return m_sActionUIClass;
	}
}

