/*
 * Copyright 2002-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.utilities.ResourceMgr;

import java.awt.Cursor;
import java.awt.Point;

public class CreateNoPinDeviceAction extends CreateDeviceAction
{

	private static Cursor m_cursor = null;

	public CreateNoPinDeviceAction(ICapletController controller)
	{
		super(controller);
		if (m_cursor == null) {
			m_cursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/cur_device_nopin.gif", new Point(7, 7));
		}
	}

	protected boolean shouldAddPins()
	{
		return isCtrlDown();
	}

	/**
	 * Gets the ActionUIClass attribute of the CreateCircleAction object
	 *
	 * @return The ActionUIClass value
	 */
	public String getActionUIClass()
	{
		return CreateNoPinDeviceActionUI.class.getName();
	}

	public String getStatusbarText()
	{
		return ResourceMgr
				.getString(CreateNoPinDeviceAction.class, "CreateParameterizedObjectAction.StatusBar.NoPins.text");
	}

	/**
	 * @see chs.caplets.logic.actions.CreateInlineConnectorAction#getCursor()
	 */
	public Cursor getCursor()
	{
		return m_cursor;
	}
}
