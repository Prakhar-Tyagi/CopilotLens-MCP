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
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.IUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author chandras on 19-10-2019.
 */
public class DevicePlacementDataModel implements IDevicePlacementDataModel
{

	@NotNull private final ISchemDiagram m_diagram;
	@NotNull private IDevicePlacementGraphicControl m_placementController;
	@NotNull private final List<IDevicePlacementInfo> m_devicesBeingPlaced = new ArrayList<>();
	@NotNull private final Map<IUID, Point> m_pinAbsLocations = new HashMap<>();

	public DevicePlacementDataModel(@NotNull ISchemDiagram diagram)
	{
		m_diagram = diagram;
		m_placementController = new BasicDevicePlacementControl(diagram);
		for (IPinList pinList : diagram.getPinLists()) {
			if (pinList.getSymbolDef() != null) {
				for (IPin pin : pinList.getPins()) {
					final ILocation absLoc = pin.getAbsLocation();
					m_pinAbsLocations.put(pin.getConnectivityUID(), new Point(absLoc.getX(), absLoc.getY()));
				}
			}
		}
	}

	@Override public void setupOerands(@NotNull List<IDevicePlacementInfo> operands)
	{
		m_devicesBeingPlaced.addAll(operands);
	}

	@Override public void setupController(@NotNull IDevicePlacementController placementController)
	{
		m_placementController = placementController;
		m_devicesBeingPlaced.forEach(IDevicePlacementInfo::unSelect);
	}

	@NotNull @Override public List<IDevicePlacementInfo> getPlacementInfos()
	{
		return Collections.unmodifiableList(m_devicesBeingPlaced);
	}

	@Override public void reverseOrderOfPlacement(int startPlacementIdx)
	{
		final List<IDevicePlacementInfo> placementInfos = m_devicesBeingPlaced;
		int leftEnd = startPlacementIdx;
		int rightEnd = placementInfos.size() - 1;
		while (leftEnd < rightEnd) {
			final IDevicePlacementInfo rightSideElement = placementInfos.get(rightEnd);
			final IDevicePlacementInfo leftSideElement = placementInfos.get(leftEnd);
			placementInfos.set(leftEnd, rightSideElement);
			placementInfos.set(rightEnd, leftSideElement);
			++leftEnd;
			--rightEnd;
		}
	}

	@Nullable @Override public Point getAbsoluteLocationForPin(@NotNull IAbstractPin pin)
	{
		return m_pinAbsLocations.get(pin.getUID());
	}

	@NotNull @Override public ILogicDesign getDesign()
	{
		final ILogicDesign design = m_diagram.getDesign();
		assert design != null;
		return design;
	}

	@Override public int getOneUnitCustomGap()
	{
		return getProject().getPreferences().getPanelLayoutUnitGap();
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
		return m_placementController.getVerticalJustification();
	}

	@NotNull @Override public HorizJustificationEnum getHorizontalJustification()
	{
		return m_placementController.getHorizontalJustification();
	}

	@NotNull @Override public PlacementAxisRotation getPlacementRotation()
	{
		return m_placementController.getPlacementRotation();
	}

	@Override public boolean isOriginAligned()
	{
		return m_placementController.isOriginAligned();
	}

	@NotNull @Override public IExtent getMarginExtent(@NotNull IDevice device, @NotNull IExtent snappedExtent)
	{
		return m_placementController.getMarginExtent(device, snappedExtent);
	}

	@Override public int getMargin(@NotNull IDevice device, @NotNull DeviceMarginSide side)
	{
		return m_placementController.getMargin(device, side);
	}

	@Override public boolean isValidPlacement(@NotNull IDevice device, @NotNull IDevicePlacementItem placementItem)
	{
		return m_placementController.isValidPlacement(device, placementItem);
	}

	@Override public boolean isSymbolPreviewMode()
	{
		return m_placementController.isSymbolPreviewMode();
	}
}
