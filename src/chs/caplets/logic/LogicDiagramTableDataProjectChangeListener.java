/*
 * Copyright 2006-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.logic;

import chs.caf.CAFUtils;
import chs.caf.IProjectChangeListener;
import chs.caf.ProjectChangeEvent;
import chs.caf.caplet.helpers.UndoableContainerIdler;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;

public class LogicDiagramTableDataProjectChangeListener implements IProjectChangeListener
{

	private IDesign m_design;

	public LogicDiagramTableDataProjectChangeListener(IDesign design)
	{
		m_design = design;
	}

	public void projectChanged(ProjectChangeEvent e)
	{
		if (m_design instanceof ILogicDesign && m_design.getProject() != null &&
				(e.getChangeType() == ProjectChangeEvent.PROJECT_EDITED ||
						e.getChangeType() == ProjectChangeEvent.PROJECT_CHILD_DELETED)) {
			CAFUtils.getInstance().setTempUndoableContainer(UndoableContainerIdler.instance());
			ILogicDesign lDesign = (ILogicDesign) m_design;
			lDesign.fireTableDataChanged();
			CAFUtils.getInstance().clearTempUndoableContainer();
		}
	}
}
