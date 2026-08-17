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

import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedUsage;
import chs.cof.symbol.IZoneIdentifier;
import chs.common.IUID;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utility.ui.IconUtils;

import javax.swing.Icon;

public class UsageHyperlink extends AbstractLogicHyperlink
{

	protected ISharedUsage m_usage;
	protected String m_text;
	private static final double CONFIDENCE = 0.95; // Wish this could be 1.0 :(

	UsageHyperlink(ISchemDiagram currentDiagram, ISharedUsage usage)
	{
		super(currentDiagram);
		m_usage = usage;
		m_text = genUsageText();
	}

	public double getConfidence()
	{
		return CONFIDENCE;
	}

	public Icon getIcon()
	{
		if (m_usage.getSharedObject() != null) {
			return IconUtils.getIcon(m_usage.getSharedObject());
		}
		else {
			return IconUtils.getIcon(FactoryMgr.getUIDObject(m_usage.getLogicObjectUID()));
		}
	}

	protected String getDesignName()
	{
		return m_usage.getDesignName();
	}

	protected String getDiagramName()
	{
		return m_usage.getDiagramName();
	}

	public IUID getDesignUID()
	{
		return m_usage.getDesignUID();
	}

	public IUID getDiagramUID()
	{
		return m_usage.getDiagramUID();
	}

	public IDiagramObjectIterator getDiagramObjects()
	{
		return getDiagramObjects(m_usage.getDiagramObjectUID());
	}

	public boolean hasDiagramObjects()
	{
		return m_usage.getDiagramObjectUID() != null;
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		if (m_usage.isHome()) {
			sb.append("<html><b>");
		}
		sb.append(m_text);
		if (m_usage.isHome()) {
			String home = ResourceMgr.getString(AbstractLogicHyperlink.class, "Hyperlink.home.text");
			sb.append(" (").append(home).append(") </html></b>");
		}
		return sb.toString();
	}

	// TODO - This should be changed to use the cross-reference preferences.
	private String genUsageText()
	{
		String text = super.toString();
		IZoneIdentifier sz = m_usage.getZoneKey();
		if (sz != null) {
			StringBuilder buf = new StringBuilder(text);
			buf.append(' ');
			buf.append(sz.getName());
			text = buf.toString();
		}
		return text;
	}
}

