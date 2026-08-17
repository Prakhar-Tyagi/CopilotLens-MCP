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
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.ICHSIterator;
import chs.common.IUID;
import chs.dataservices.DesignBlockUsageInfo;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.DiagramHelper;
import chs.utility.ui.IconUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class DesignBlockHyperlink extends AbstractLogicHyperlink
{

	private DesignBlockUsageInfo m_usage;
	private static final double CONFIDENCE = 0.95; // Wish this could be 1.0 :(

	protected DesignBlockHyperlink(@NotNull IDesign currentDesign, DesignBlockUsageInfo usage)
	{
		super(currentDesign);
		m_usage = usage;
	}

	public double getConfidence()
	{
		return CONFIDENCE;
	}

	public Icon getIcon()
	{
		return IconUtils.getBlockDeviceWithDesignIcon(IconUtils.ACTIVE);
	}

	@Nullable protected String getDesignName()
	{
		return m_usage.getDesignFullName();
	}

	protected String getDiagramName()
	{
		return m_usage.getDiagramName();
	}

	public IUID getDesignUID()
	{
		return m_usage.getDesignID();
	}

	@Nullable public IUID getDiagramUID()
	{
		return null;
	}

	@Nullable protected ISchemDiagram getDiagram()
	{
		String diagramName = getDiagramName();
		ILogicDesign design = getDesign();
		if (diagramName == null || StringUtils.isBlank(diagramName)) {
			ICHSIterator<ISchemDiagram> diagramItr = design.getDiagrams(true);
			return diagramItr.hasNext() ? diagramItr.getNext() : null;
		}
		else {
			return (ISchemDiagram) DiagramHelper.getDiagramByName(design, diagramName);
		}
	}

	public IDiagramObjectIterator getDiagramObjects()
	{
		return getDiagramObjects(m_usage.getSchemID());
	}

	public boolean hasDiagramObjects()
	{
		return true;
	}

	public boolean canOpenEmptyDiagram()
	{
		return true;
	}

	protected boolean isUnplacedUsage()
	{
		return m_usage.getSchemID() == null;
	}

	protected boolean isHome()
	{
		return m_usage.isHome();
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		boolean isUnplaced = isUnplacedUsage();
		boolean isHome = isHome();
		if (isHome) {
			sb.append("<html><b>");
		}
		else if (isUnplaced) {
			sb.append("<html><b><i>");
		}
		sb.append(super.toString());
		if (isHome) {
			String home = ResourceMgr.getString(AbstractLogicHyperlink.class, "Hyperlink.home.text");
			sb.append(" (").append(home).append(") </html></b>");
		}
		else if (isUnplaced) {
			String unplaced = ResourceMgr.getString(AbstractLogicHyperlink.class, "Hyperlink.unplaced.text");
			sb.append(" (").append(unplaced).append(") </html></b></i>");
		}
		return sb.toString();
	}
}

