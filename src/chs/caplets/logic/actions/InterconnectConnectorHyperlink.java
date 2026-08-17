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
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IConnectorPin;
import chs.cof.logical.cable.IInterconnectConnector;
import chs.cof.logical.cable.IInterconnectSourceInfo;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.ISchemDiagram;
import chs.utility.ui.IconUtils;

import javax.swing.Icon;

public class InterconnectConnectorHyperlink extends AbstractInterconnectParentHyperlink
{

	public InterconnectConnectorHyperlink(ISchemDiagram currentDiagram, IDesign design, ISchemDiagram diagram,
			ILogicObject lobj)
	{
		super(currentDiagram, design);
		IConnector child = null;
		if (lobj instanceof IConnector) {
			child = (IConnector) lobj;
		}
		else if (lobj instanceof IConnectorPin) {
			child = (IConnector) ((IConnectorPin) lobj).getOwner();
		}
		else {
			assert false;
		}
		IInterconnectSourceInfo isi = m_currentDesign.getInterconnectSourceInfo();
		m_sourceUID = isi.getSourceConnectorUID(child);
		init(diagram);
	}

	public Icon getIcon()
	{
		return IconUtils.getIcon(IInterconnectConnector.class);
	}
}
