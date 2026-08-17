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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.Stack;

/**
 * @author chandras on 19-10-2019.
 */
public class UndoableDevicePlacementConfig implements IUndoableDevicePlacementConfig
{

	@NotNull private Stack<IDevicePlacementConfig> m_configs = new Stack<>();

	public UndoableDevicePlacementConfig(int unit_placement_gap)
	{
		m_configs.push(new DevicePlacementConfig(unit_placement_gap));
	}

	@NotNull private IDevicePlacementConfig currentConfig()
	{
		return m_configs.peek();
	}

	@Override public boolean canUndo()
	{
		return m_configs.size() > 1;
	}

	@Override public void undo()
	{
		if (m_configs.size() > 1) {
			m_configs.pop();
		}
	}

	public void commit(@NotNull Point currentPlacementLocation)
	{
		//first commit and then push a clone of that.
		final IDevicePlacementConfig currentConfig = currentConfig();
		currentConfig.commit(currentPlacementLocation);
		m_configs.push(currentConfig.duplicate());
	}

	@Nullable @Override public Point getCurrentPlacementPoint()
	{
		return currentConfig().getCurrentPlacementPoint();
	}

	@Override public int getCurrentPlacementIdx()
	{
		return currentConfig().getCurrentPlacementIdx();
	}

	@NotNull @Override public DevicePlacementMode getPlacementMode()
	{
		return currentConfig().getPlacementMode();
	}

	@Override public int getGroupCustomGap()
	{
		return currentConfig().getGroupCustomGap();
	}

	@Override public void setupPlacementIndex(int currentPlacementIndex)
	{
		currentConfig().setupPlacementIndex(currentPlacementIndex);
	}

	@Override public boolean isOriginAligned(@Nullable IMountSnapInfo mountSnapInfo)
	{
		return currentConfig().isOriginAligned(mountSnapInfo);
	}

	@Override public void toggleOriginAligned()
	{
		currentConfig().toggleOriginAligned();
	}

	public void setupNextAxis()
	{
		currentConfig().setupNextAxis();
	}

	public void setupPrevAxis()
	{
		currentConfig().setupPrevAxis();
	}

	@Override public int getOneUnitCustomGap()
	{
		return currentConfig().getOneUnitCustomGap();
	}

	@Override public int incrementGroupCustomAdditionalGap()
	{
		return currentConfig().incrementGroupCustomAdditionalGap();
	}

	@Override public int decrementGroupCustomAdditionalGap()
	{
		return currentConfig().decrementGroupCustomAdditionalGap();
	}

	@NotNull @Override public VertJustificationEnum getVerticalJustification()
	{
		return currentConfig().getVerticalJustification();
	}

	@NotNull @Override public HorizJustificationEnum getHorizontalJustification()
	{
		return currentConfig().getHorizontalJustification();
	}

	@NotNull @Override public PlacementAxisRotation getPlacementRotation(@Nullable IMountSnapInfo mountRailSnap)
	{
		return currentConfig().getPlacementRotation(mountRailSnap);
	}

	@Override public void handleHorizontalJustification()
	{
		currentConfig().handleHorizontalJustification();
	}

	@Override public void handleVerticalJustification()
	{
		currentConfig().handleVerticalJustification();
	}

	@Override public void setupNextPlacementMode()
	{
		currentConfig().setupNextPlacementMode();
	}

	@Override public void setupAbutPlacementMode()
	{
		currentConfig().setupAbutPlacementMode();
	}

	@Override public void moveToNewPlacementPoint(@Nullable Point currentPlacementPoint)
	{
		currentConfig().moveToNewPlacementPoint(currentPlacementPoint);
	}
}
