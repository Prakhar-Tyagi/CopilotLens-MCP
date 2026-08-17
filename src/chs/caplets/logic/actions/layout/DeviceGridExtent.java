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

import chs.cof.draw.IGrid;
import chs.common.IExtent;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author chandras on 19-10-2019.
 */
public class DeviceGridExtent implements IDeviceGridExtent
{

	private int m_l;
	private int m_r;
	private int m_t;
	private int m_b;
	private int m_cx;
	private int m_cy;
	@NotNull private final IExtent m_noTextExtent;

	public DeviceGridExtent(@NotNull final IExtent noTextExtent, @NotNull final IGrid grid)
	{
		m_cx = grid.snap(noTextExtent.getCenterX());
		m_cy = grid.snap(noTextExtent.getCenterY());
		m_l = IGrid.snapToFloorGrid(noTextExtent.getLeft(), grid);
		m_b = IGrid.snapToFloorGrid(noTextExtent.getBottom(), grid);
		m_r = IGrid.snapToCeilGrid(noTextExtent.getRight(), grid);
		m_t = IGrid.snapToCeilGrid(noTextExtent.getTop(), grid);

		final int gridSpacing = grid.getGridSpacing();
		final int a_tolerance = CommonUtils.toInteger(ALIGN_OVERLAP_TOLERANCE * gridSpacing);
		final int p_tolerance = CommonUtils.toInteger(PLACE_OVERLAP_TOLERANCE * gridSpacing);
		int freeSpaceCanBeSqueezed = a_tolerance + m_t - noTextExtent.getTop();
		freeSpaceCanBeSqueezed = gridSpacing * (freeSpaceCanBeSqueezed / gridSpacing);
		m_t -= freeSpaceCanBeSqueezed;

		freeSpaceCanBeSqueezed = p_tolerance + m_r - noTextExtent.getRight();
		freeSpaceCanBeSqueezed = gridSpacing * (freeSpaceCanBeSqueezed / gridSpacing);
		m_r -= freeSpaceCanBeSqueezed;

		freeSpaceCanBeSqueezed = a_tolerance - m_b + noTextExtent.getBottom();
		freeSpaceCanBeSqueezed = gridSpacing * (freeSpaceCanBeSqueezed / gridSpacing);
		m_b += freeSpaceCanBeSqueezed;

		freeSpaceCanBeSqueezed = p_tolerance - m_l + noTextExtent.getLeft();
		freeSpaceCanBeSqueezed = gridSpacing * (freeSpaceCanBeSqueezed / gridSpacing);
		m_l += freeSpaceCanBeSqueezed;
		m_noTextExtent = noTextExtent;
	}

	@NotNull public IExtent getSnappedExtent()
	{
		return FactoryMgr.getCommonFactory().constructExtent(m_l, m_b, Math.abs(m_r - m_l), Math.abs(m_t - m_b));
	}

	public int getCenterX()
	{
		return m_cx;
	}

	public int getCenterY()
	{
		return m_cy;
	}

	public int getLeft()
	{
		return m_l;
	}

	public int getRight()
	{
		return m_r;
	}

	public int getTop()
	{
		return m_t;
	}

	public int getBottom()
	{
		return m_b;
	}

	@Override public int getPlacementItemSideGap(@NotNull DeviceMarginSide side)
	{
		int margin = 0;
		final int centerX = getCenterX();
		final int centerY = getCenterY();
		switch (side) {
			case LEFT:
				margin = centerX - getLeft();
				break;
			case RIGHT:
				margin = getRight() - centerX;
				break;
			case TOP:
				margin = getTop() - centerY;
				break;
			case BOTTOM:
				margin = centerY - getBottom();
				break;
			default:
				break;
		}
		return Math.abs(margin);
	}

	@Override public int getPlacementItemInset(@NotNull DeviceMarginSide side)
	{
		int inset = 0;
		switch (side) {
			case LEFT:
				inset = m_noTextExtent.getLeft() - getLeft();
				break;
			case RIGHT:
				inset = getRight() - m_noTextExtent.getRight();
				break;
			case TOP:
				inset = getTop() - m_noTextExtent.getTop();
				break;
			case BOTTOM:
				inset = m_noTextExtent.getBottom() - getBottom();
				break;
			default:
				break;
		}
		//return raw value. this would be negative if spilling outside the grid box due to tolerance squeeze.
		return inset;
	}
}
