/*
 * Copyright 2002-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.shared.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.SelectActionHelper;
import chs.caf.caplet.helpers.SelectHelper;
import chs.caf.caplet.selection.ISelectClient;
import chs.caplets.logic.LogicSelectHelper;
import org.jetbrains.annotations.NotNull;

import java.awt.Cursor;

public class SelectAction extends SelectActionHelper
{

	protected SelectHelper m_selectHelper = null;
	protected ISelectClient m_selectClient = null;

	private static Cursor m_selectCursor = CAFUtils.getInstance().loadCursor(Cursor.DEFAULT_CURSOR);

	public SelectAction(ICapletController controller)
	{
		super(controller);
		m_selectClient = getInitSelectClient();
		initSelectHelper();
	}

	protected void initSelectHelper()
	{
		// Create a selector to do all of the work.
		m_selectHelper = new LogicSelectHelper(getController(),
				getController().getSelectMgr().getPreSelections(),
				getEventDistributor(),
				m_selectClient);
	}

	public ISelectClient getSelectClient()
	{
		return m_selectClient;
	}

	protected ISelectClient getInitSelectClient()
	{
		return new SelectActionClient(this, getController());
	}

	public String getActionUIClass()
	{
		return SelectActionUI.class.getName();
	}

	/**
	 * Is there an active manipulator in the select helper?
	 *
	 * @return boolean, true if a manipulator is active...
	 */
	public boolean isManipulatorActive()
	{
		return m_selectHelper != null && m_selectHelper.isManipulatorActive();
	}

	/**
	 * Return the cursor for this action
	 */
	public Cursor getCursor()
	{
		return m_selectCursor;
	}

	@NotNull @Override protected SelectHelper getSelectHelper()
	{
		return m_selectHelper;
	}
}
