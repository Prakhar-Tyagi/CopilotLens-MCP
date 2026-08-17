/*
 * Copyright 2005-2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.logical.IDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IUID;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utility.ui.IconUtils;

import javax.swing.Icon;
import java.util.Collections;

public class BlockDeviceHyperlink extends AbstractLogicHyperlink
{

	private IDesign m_design;
	private ISchemDiagram m_diagram;

	protected BlockDeviceHyperlink(ISchemDiagram currentDiagram, IDesign design, ISchemDiagram diagram)
	{
		super(currentDiagram);
		m_design = design;
		m_diagram = diagram;
	}

	public double getConfidence()
	{
		return 1.0;
	}

	public Icon getIcon()
	{
		return IconUtils.getIcon(m_design);
	}

	protected String getDesignName()
	{
		return m_design.getFullName();
	}

	protected String getDiagramName()
	{
		return m_diagram.getName();
	}

	public IUID getDesignUID()
	{
		return m_design.getUID();
	}

	public IUID getDiagramUID()
	{
		return m_diagram.getUID();
	}

	public IDiagramObjectIterator getDiagramObjects()
	{
		return FactoryMgr.getDrawPlusFactory().createDiagramObjectIterator(Collections.EMPTY_LIST);
	}

	public boolean hasDiagramObjects()
	{
		return true;
	}

	public boolean canOpenEmptyDiagram()
	{
		return true;
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<html><b>");
		sb.append(super.toString());
		String targetDesign = ResourceMgr.getString(AbstractLogicHyperlink.class, "Hyperlink.target.text");
		sb.append(" (").append(targetDesign).append(") </html></b>");
		return sb.toString();
	}
}

