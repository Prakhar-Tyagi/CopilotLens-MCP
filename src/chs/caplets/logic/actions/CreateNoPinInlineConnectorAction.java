/*
 * Copyright 2003-2008 Mentor Graphics Corporation
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

import java.awt.Cursor;
import java.awt.Point;

/**
 * Overrides {@link CreateInlineConnectorAction} to not add pins by default.
 *
 * @author Matt Boyd
 */
public class CreateNoPinInlineConnectorAction extends CreateInlineConnectorAction
{

	private static Cursor m_cursor = null;

	public CreateNoPinInlineConnectorAction(ICapletController controller)
	{
		super(controller);
		if (m_cursor == null) {
			m_cursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/cur_inline_nopin.gif", new Point(7, 7));
		}
	}

	protected boolean shouldAddPins()
	{
		return isCtrlDown();
	}

	public String getActionUIClass()
	{
		return CreateNoPinInlineConnectorActionUI.class.getName();
	}

	/**
	 * @see CreateInlineConnectorAction#getCursor()
	 */
	public Cursor getCursor()
	{
		return m_cursor;
	}
}