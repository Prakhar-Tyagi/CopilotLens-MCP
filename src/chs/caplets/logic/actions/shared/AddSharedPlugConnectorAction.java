/*
 * Copyright 2002-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.utilities.ResourceMgr;

import java.awt.Cursor;
import java.awt.Point;

/**
 * This class exists for typing only.
 */
public class AddSharedPlugConnectorAction extends AddSharedJackConnectorAction
{

	private static Cursor m_plugConnectorCursor = null;

	public AddSharedPlugConnectorAction(ICapletController controller, ISpecialSelectMgr sharedSelectMgr)
	{
		super(controller, sharedSelectMgr);
		setSubType(getType());
		if (m_plugConnectorCursor == null) {
			m_plugConnectorCursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/cur_connector.gif", new Point(8, 8));
		}
	}

	protected PinListTypeEnum getType()
	{
		return PLUG_CONNECTOR;
	}

	protected IDynamicGfx constructDynGfx(Point ref_point)
	{
		IDynamicGfx gfx = super.constructDynGfx(ref_point);

		// We derive from AddsharedJackConnector so we inherit it's behaviour of setting up a jack style
		// rotation indicator. We must now reset that back to a plug style indicator.
		getRotationIndicator().setIsJackStyle(false);
		return gfx;
	}

	/**
	 * Gets the ActionUIClass attribute of the CreateCircleAction object
	 *
	 * @return The ActionUIClass value
	 */
	public String getActionUIClass()
	{
		return AddSharedPlugConnectorActionUI.class.getName();
	}

	public Cursor getCursor()
	{
		return m_plugConnectorCursor;
	}

	/**
	 * Set the status text for this action
	 */
	public String getStatusbarText()
	{
		switch (getState()) {
			case STATE_PARAM:
				return ResourceMgr.getString(AddSharedPlugConnectorAction.class,
						"AddSharedPlugConnectorAction.StatusBar.text");
			case STATE_PINS:
				return m_addPinActionHelper.getStatusbarText();
			default:
				return null;
		}
	}
}
