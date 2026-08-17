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
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IUID;

public abstract class AbstractInterconnectHyperlink extends AbstractLogicHyperlink
{

	protected IDesign m_linkDesign;
	protected ISchemDiagram m_linkDiagram;

	protected AbstractInterconnectHyperlink(ISchemDiagram currentDiagram, IDesign design)
	{
		this(currentDiagram, design, null);
	}

	protected AbstractInterconnectHyperlink(ISchemDiagram currentDiagram, IDesign design, ISchemDiagram diagram)
	{
		super(currentDiagram);
		m_linkDesign = design;
		m_linkDiagram = diagram;
	}

	public ILogicDesign getDesign()
	{
		return (ILogicDesign)m_linkDesign;
	}

	public IUID getDesignUID()
	{
		return m_linkDesign.getUID();
	}

	public String getDesignName()
	{
		return m_linkDesign.getName();
	}

	protected ISchemDiagram getDiagram()
	{
		return m_linkDiagram;
	}

	public IUID getDiagramUID()
	{
		if (m_linkDiagram != null) {
			return m_linkDiagram.getUID();
		}
		else {
			return null;
		}
	}

	protected String getDiagramName()
	{
		if (m_linkDiagram != null) {
			return m_linkDiagram.getName();
		}
		else {
			return null;
		}
	}

	public boolean hasDiagramObjects()
	{
		// assume if there's something on the diagram.  Dealing with the ICX info requires the diagram to be loaded.
		return true;
	}
}
