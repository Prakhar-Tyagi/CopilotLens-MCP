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
import chs.services.dynamicgfx.IDynamicGfx;

import java.awt.Cursor;
import java.awt.Point;

/**
 * @author Matt Boyd
 */
public class CreateJackConnectorAction extends CreateConnectorAction
{

	private static Cursor m_jackCursor = null;

	/**
	 * Constructor for CreatePlugConnectorAction.
	 *
	 * @param controller
	 */
	public CreateJackConnectorAction(ICapletController controller)
	{
		super(controller);
		if (m_jackCursor == null) {
			m_jackCursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/cur_connector_jack.gif", new Point(7, 7));
		}

		setSubType(JACK_CONNECTOR);
	}

	/**
	 * Gets the ActionUIClass attribute of the CreateCircleAction object
	 *
	 * @return The ActionUIClass value
	 */
	public String getActionUIClass()
	{
		return CreateJackConnectorActionUI.class.getName();
	}

	protected IDynamicGfx constructDynGfx(Point ref_point)
	{
		IDynamicGfx gfx = super.constructDynGfx(ref_point);
		getRotationIndicator().setIsJackStyle(true);
		return gfx;
	}

	/**
	 * @see chs.caplets.logic.actions.CreateConnectorAction#getCursor()
	 */
	public Cursor getCursor()
	{
		return m_jackCursor;
	}
}