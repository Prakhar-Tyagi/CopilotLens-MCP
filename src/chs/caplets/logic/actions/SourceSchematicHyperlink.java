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

import chs.caf.helpers.GfxViewHelper;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.drawplus.IDrawPlusFactory;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IUID;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.ResourceMgr;
import chs.utility.ui.IconUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SourceSchematicHyperlink extends AbstractLogicHyperlink
{

	@NotNull private ILogicDesign m_design;
	@NotNull private IUID m_sourceObjectUID;

	protected SourceSchematicHyperlink(@NotNull ISchemDiagram currentDiagram, @NotNull ILogicDesign design,
			@NotNull IUID sourceObjectUID)
	{
		super(currentDiagram);
		m_design = design;
		//LOGIC-11232:View Related Items action works only after Application restart
		m_sourceObjectUID = sourceObjectUID.getPersistentUID();
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
		return null;
	}

	public IUID getDesignUID()
	{
		return m_design.getUID();
	}

	@Nullable public IUID getDiagramUID()
	{
		return null;
	}

	@Nullable private ISchemDiagram determineCandidateDiagram()
	{
		m_design.getConnectivity(); //ensure logic object is loaded.
		final ILogicObject sourceObject = UIDMgr.getObjectOfType(m_sourceObjectUID, ILogicObject.class);
		if (sourceObject != null) {
			List<ISchemDiagram> diagrams =
					new ArrayList<>(m_design.getDesignWideUsageMgr().getUsageDiagrams(sourceObject));
			diagrams.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
			if (!diagrams.isEmpty()) {
				return diagrams.iterator().next();
			}
		}
		return null;
	}

	public IDiagramObjectIterator getDiagramObjects()
	{
		ISchemDiagram diagram = determineCandidateDiagram();
		final IDrawPlusFactory factory = FactoryMgr.getDrawPlusFactory();
		if (diagram != null) {
			return factory.createDiagramObjectIterator(diagram.getRepresentationsCollection(m_sourceObjectUID));
		}
		return factory.createDiagramObjectIterator(Collections.EMPTY_LIST);
	}

	@Override @Nullable public GfxView getView()
	{
		ISchemDiagram diagram = determineCandidateDiagram();
		return diagram != null ? GfxViewHelper.openLogicDiagram(diagram) : null;
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
		sb.append(" (").append(targetDesign).append(")</b></html>");
		return sb.toString();
	}
}

