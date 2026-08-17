/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IInterconnectConductor;
import chs.cof.logical.cable.IInterconnectSourceInfo;
import chs.cof.logical.cable.IInterconnectToDoItem;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.schem.ISchemDiagram;
import chs.utility.ui.IconUtils;

import javax.swing.Icon;

public class InterconnectConductorHyperlink extends AbstractInterconnectParentHyperlink
{

	InterconnectConductorHyperlink(ISchemDiagram currentDiagram, IDesign design, ISchemDiagram diagram,
			ILogicObject logicObject)
	{
		super(currentDiagram, design);
		IInterconnectToDoItem item = null;
		IInterconnectSourceInfo isi = m_currentDesign.getInterconnectSourceInfo();
		if (logicObject instanceof IConductor) {
			item = isi.getToDoItem((IConductor) logicObject);
		}
		else if (logicObject instanceof IMulticore) {
			item = isi.getToDoItem((IMulticore) logicObject);
		}
		if (item != null) {
			m_sourceUID = item.getInterconnectUID();
		}
		init(diagram);
	}

	public Icon getIcon()
	{
		return IconUtils.getIcon(IInterconnectConductor.class);
	}
}
