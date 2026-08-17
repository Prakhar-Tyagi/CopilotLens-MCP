/*
 * Copyright 2004-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.utility.placement.IPlacementDirector;

import java.awt.event.ActionEvent;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Jul 30, 2004 Time: 2:16:16 PM
 */
public abstract class AbstractPlacementAction extends ControllerActionRT
{

	private IPlacementDirector m_pDir;
	private Collection m_operands;

	protected AbstractPlacementAction(ICapletController controller)
	{
		super(controller);
		//noinspection AbstractMethodCallInConstructor
		m_pDir = createPlacementDirector();
		m_operands = null;
	}

	abstract protected IPlacementDirector createPlacementDirector();

	final protected IPlacementDirector getPlacementDirector()
	{
		return m_pDir;
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		if (m_operands != null) {
			m_pDir.clear();
			m_pDir.addAll(m_operands);
			return IActionEnum.eCompleted;
		}
		else {
			return IActionEnum.eCanceled;
		}
	}

	protected boolean onTerminate(boolean successful)
	{
		if (successful) {
//			m_pDir.consolidate();
			m_pDir.place();
		}
		return successful;
	}

	public boolean isEnabled()
	{
		m_operands = m_pDir.accept(getController().getSelectMgr().getPreSelections().getUIDObjects());
		return m_operands != null && super.isEnabled();
	}
}
