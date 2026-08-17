/*
 * Copyright 2006-2008 Mentor Graphics Corporation
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
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;
import chs.utility.ui.IconUtils;

import javax.swing.Icon;
import java.awt.event.KeyEvent;

@ApplicationSpecification(
		includeIn = {Application.CapitalSymbolDesigner, Application.CapitalSymbolForCapture, Application.CapitalEssentialsSymbolDesigner,
				Application.XSCSymbol, Application.SEElectricalSymbol})
public class AddEngineeringDatumActionUI extends ActionUI
{

	//Related entity type just wraps three enums, so it's is okay to hold on to this in action ui.
	private String m_type;

	public AddEngineeringDatumActionUI(ICaplet caplet, String type)
	{
		super(caplet, type);
		m_type = type;
		setupUI();
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		String ret = m_type;
		if (ret != null) {
			Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_datum_generic.gif");
			//Integer iMnemonic = (int) ResourceMgr.getMnemonic(AddEngineeringDatumActionUI.class, "AddEngineeringDatumActionUI.mnemonic." + getActionUIInstanceName());

			putValue(NAME, ResourceMgr.getString(AddEngineeringDatumActionUI.class,
					"AddEngineeringDatumActionUI.name.decl." + getActionUIInstanceName()));
			putValue(SHORT_DESCRIPTION, ResourceMgr.getString(AddEngineeringDatumActionUI.class,
					"AddEngineeringDatumActionUI.shortDesc.decl." + getActionUIInstanceName()));
			putValue(LONG_DESCRIPTION, ResourceMgr.getString(AddEngineeringDatumActionUI.class,
					"AddEngineeringDatumActionUI.longDesc.decl." + getActionUIInstanceName()));
			putValue(SMALL_ICON, icon);
			int key;
			switch (getActionUIInstanceName().charAt(0)) {
				case 'F':
					key = KeyEvent.VK_F;
					break;
				case 'B':
					key = KeyEvent.VK_B;
					break;
				default:
					key = KeyEvent.VK_T;
					break;
			}
			putValue(MNEMONIC_KEY, Integer.valueOf(key));
		}
	}

	public Icon getInactiveIcon()
	{
		return getIcon(IconUtils.INACTIVE);
	}

	private Icon getIcon(IconUtils type)
	{
		Icon icon = CHSImageLoader.loadImageIcon(CHSImages.TRANSPARENT_ICON);
		return icon;
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return AddEngineeringDatumAction.class.getName();
	}
}

