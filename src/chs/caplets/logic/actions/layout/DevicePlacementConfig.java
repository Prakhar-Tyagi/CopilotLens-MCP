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
import chs.cof.draw.VertJustificationEnum;
import chs.utilities.CHSConstants;
import chs.utilities.WrappingRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;

/**
 * @author chandras on 19-10-2019.
 */
public class DevicePlacementConfig implements IDevicePlacementConfig, Cloneable
{

	@NotNull private DevicePlacementMode m_placementMode = INITIAL_PLACEMENT_MODE;
	@NotNull private HorizJustificationEnum m_hJust = HorizJustificationEnum.JustLeft;
	@NotNull private VertJustificationEnum m_vJust = VertJustificationEnum.JustCenter;
	@NotNull private PlacementAxisRotation m_axisRotation = PlacementAxisRotation.Zero;
	@Nullable private Point m_currentPlacement_point = new Point();
	private int m_group_custom_gap = 0; //in terms of grid.
	private int m_placementIdx = 0;
	private boolean m_originAligned = true;
	private final int m_unit_placement_gap;

	public DevicePlacementConfig(int unit_placement_gap)
	{
		m_unit_placement_gap = unit_placement_gap;
	}

	@Override public void setupNextAxis()
	{
		m_axisRotation = m_axisRotation.next();
	}

	@Override public void setupPrevAxis()
	{
		m_axisRotation = m_axisRotation.prev();
	}

	@Override public int getOneUnitCustomGap()
	{
		return Math.max(1, m_unit_placement_gap) * CHSConstants.PIN_SPACING;
	}

	@Override public int incrementGroupCustomAdditionalGap()
	{
		final int oldCustomGap = m_group_custom_gap;
		m_group_custom_gap += getOneUnitCustomGap();
		return (m_group_custom_gap - oldCustomGap);
	}

	@Override public int decrementGroupCustomAdditionalGap()
	{
		final int oldCustomGap = m_group_custom_gap;
		m_group_custom_gap = Math.max(0, (m_group_custom_gap - getOneUnitCustomGap()));
		return (m_group_custom_gap - oldCustomGap);
	}

	@NotNull @Override public VertJustificationEnum getVerticalJustification()
	{
		return m_vJust;
	}

	@NotNull @Override public HorizJustificationEnum getHorizontalJustification()
	{
		return m_hJust;
	}

	@NotNull @Override public PlacementAxisRotation getPlacementRotation(@Nullable IMountSnapInfo mountRailSnap)
	{
		if (mountRailSnap == null) {
			return m_axisRotation;
		}
		if (mountRailSnap.isHorizontal()) {
			if (PlacementAxisRotation.Ninety.equals(m_axisRotation)) {
				return PlacementAxisRotation.OneEighty;
			}
			else if (PlacementAxisRotation.TwoSeventy.equals(m_axisRotation)) {
				return PlacementAxisRotation.Zero;
			}
		}
		else {
			if (PlacementAxisRotation.Zero.equals(m_axisRotation)) {
				return PlacementAxisRotation.Ninety;
			}
			else if (PlacementAxisRotation.OneEighty.equals(m_axisRotation)) {
				return PlacementAxisRotation.TwoSeventy;
			}
		}
		return m_axisRotation;
	}

	@Override public void handleHorizontalJustification()
	{
		m_hJust = m_hJust.getNext();
	}

	@Override public void handleVerticalJustification()
	{
		m_vJust = m_vJust.getNext();
	}

	@Override public void setupNextPlacementMode()
	{
		m_placementMode = m_placementMode.next();
	}

	@Override public void setupAbutPlacementMode()
	{
		m_placementMode = DevicePlacementMode.AUTO;
	}

	@NotNull @Override public IDevicePlacementConfig duplicate()
	{
		try {
			DevicePlacementConfig devicePlacementConfig = (DevicePlacementConfig) super.clone();
			if (m_currentPlacement_point != null) {
				devicePlacementConfig.m_currentPlacement_point = new Point(m_currentPlacement_point);
			}
			return devicePlacementConfig;
		}
		catch (CloneNotSupportedException cnse) {
			throw new WrappingRuntimeException(cnse);
		}
	}

	@Override public void moveToNewPlacementPoint(@Nullable Point currentPlacementPoint)
	{
		m_currentPlacement_point = currentPlacementPoint != null ? new Point(currentPlacementPoint) : null;
	}

	@Override public void commit(@NotNull Point currentPlacementLocation)
	{
		m_currentPlacement_point = new Point(currentPlacementLocation);
	}

	@Nullable @Override public Point getCurrentPlacementPoint()
	{
		return m_currentPlacement_point;
	}

	@Override public int getCurrentPlacementIdx()
	{
		return m_placementIdx;
	}

	@NotNull @Override public DevicePlacementMode getPlacementMode()
	{
		return m_placementMode;
	}

	@Override public int getGroupCustomGap()
	{
		return m_group_custom_gap;
	}

	@Override public void setupPlacementIndex(int currentPlacementIndex)
	{
		m_placementIdx = currentPlacementIndex;
	}

	@Override public boolean isOriginAligned(@Nullable IMountSnapInfo mountSnapInfo)
	{
		return m_originAligned || mountSnapInfo != null;
	}

	@Override public void toggleOriginAligned()
	{
		m_originAligned = !m_originAligned;
	}
}
