/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout;

import chs.cof.draw.HorizJustificationEnum;
import chs.cof.draw.IGrid;
import chs.cof.draw.VertJustificationEnum;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.common.IExtent;
import org.jetbrains.annotations.NotNull;

/**
 * @author chandras on 20-10-2019.
 */
public class BasicDevicePlacementControl implements IDevicePlacementGraphicControl
{

	@NotNull private final ISchemDiagram m_diagram;

	public BasicDevicePlacementControl(@NotNull ISchemDiagram diagram)
	{
		m_diagram = diagram;
	}

	@NotNull @Override public ISchemDiagram getDiagram()
	{
		return m_diagram;
	}

	@NotNull @Override public IGrid getGrid()
	{
		return m_diagram.getGrid();
	}

	@NotNull @Override public IProject getProject()
	{
		return m_diagram.getProject();
	}

	@NotNull @Override public VertJustificationEnum getVerticalJustification()
	{
		return VertJustificationEnum.JustCenter;
	}

	@NotNull @Override public HorizJustificationEnum getHorizontalJustification()
	{
		return HorizJustificationEnum.JustMiddle;
	}

	@NotNull @Override public PlacementAxisRotation getPlacementRotation()
	{
		return PlacementAxisRotation.Zero;
	}

	@Override public boolean isOriginAligned()
	{
		return false;
	}

	@NotNull @Override public IExtent getMarginExtent(@NotNull IDevice device, @NotNull IExtent snappedExtent)
	{
		return snappedExtent;
	}

	@Override public int getMargin(@NotNull IDevice device, @NotNull DeviceMarginSide side)
	{
		return 0;
	}

	@Override public boolean isValidPlacement(@NotNull IDevice device, @NotNull IDevicePlacementItem placementItem)
	{
		return true;
	}

	@Override public boolean isSymbolPreviewMode()
	{
		return false;
	}
}
