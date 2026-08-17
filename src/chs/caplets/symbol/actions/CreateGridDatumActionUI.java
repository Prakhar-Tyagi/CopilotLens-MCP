/*
 * Copyright 2007-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.graphics.CreateRectangleActionUI;
import chs.caf.caplet.selection.SelectEvent;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;
import chs.utility.ui.IconUtils;

import javax.swing.Icon;

/**
 * Created by IntelliJ IDEA. User: amittal Date: Aug 3, 2007 Time: 2:02:57 PM To change this template use File |
 * Settings | File Templates.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalSymbolDesigner, Application.CapitalSymbolForCapture, Application.CapitalEssentialsSymbolDesigner,
				Application.XSCSymbol, Application.SEElectricalSymbol})
public class CreateGridDatumActionUI extends CreateRectangleActionUI
{

	/**
	 * Constructor for the CreateRectangleActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public CreateGridDatumActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public Icon getInactiveIcon()
	{
		return getIcon(IconUtils.INACTIVE);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		String name = ResourceMgr.getString(CreateGridDatumActionUI.class, "CreateGridDatumActionUI.name.decl");
		String shortDesc = ResourceMgr.getString(CreateGridDatumActionUI.class,
				"CreateGridDatumActionUI.shortDesc.decl");
		String longDesc = ResourceMgr.getString(CreateGridDatumActionUI.class, "CreateGridDatumActionUI.longDesc.decl");
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_drawrectangle_active.gif");
		Integer iMnemonic =
				(int) ResourceMgr.getMnemonic(CreateGridDatumActionUI.class, "CreateGridDatumActionUI.mnemonic");

		putValue(NAME, name);
		putValue(SHORT_DESCRIPTION, shortDesc);
		putValue(LONG_DESCRIPTION, longDesc);
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, iMnemonic);
	}

	private Icon getIcon(IconUtils type)
	{
		return CHSImageLoader.loadImageIcon(CHSImages.TRANSPARENT_ICON);
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return CreateGridDatumAction.class.getName();
	}

	public void selectionChanged(SelectEvent e)
	{
		updateUI();
	}
}
