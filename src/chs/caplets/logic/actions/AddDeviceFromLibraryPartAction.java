/*
 * Copyright 2006-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.browser.PartBrowserActionHelper;
import chs.cof.parts.partselector.ILibraryPartSelection;

/**
 * Adds a device from a library part.
 * <p/>
 * If the library part has a symbol, an instance with that symbol is added.  If the library part has no symbol, a
 * parameterized device is added and the user is prompted to add pins based on those that are on the part.
 */
public class AddDeviceFromLibraryPartAction extends AbstractAddDeviceFromLibraryPartAction
{

	public AddDeviceFromLibraryPartAction(ICapletController controller)
	{
		super(controller);
	}

	protected ILibraryPartSelection getPartSelection()
	{
		return PartBrowserActionHelper.getSelectedBrowserPart();
	}

	public String getActionUIClass()
	{
		return AddDeviceFromLibraryPartActionUI.class.getName();
	}
}

