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
import chs.cof.logical.cable.IDevice;
import chs.common.IExtent;
import chs.system.FactoryMgr;
import chs.utilities.CHSConstants;
import org.jetbrains.annotations.NotNull;

/**
 * @author chandras on 19-10-2019.
 */
public class DeviceMargin implements IDeviceMargin
{

	private int m_lMargin = 0;
	private int m_rMargin = 0;
	private int m_tMargin = 0;
	private int m_bMargin = 0;

	public DeviceMargin(@NotNull IDevice device, @NotNull IGrid grid)
	{
		final int dummyMargin = 0;//IGrid.snapToCeilGrid(2 * CHSConstants.PIN_SPACING, grid);
		m_lMargin = dummyMargin;
		m_rMargin = dummyMargin;
		m_tMargin = dummyMargin;
		m_bMargin = dummyMargin;
	}

	@Override @NotNull public IExtent getMarginExtent(@NotNull IExtent extent)
	{
		final int x = extent.getX() - m_lMargin;
		final int y = extent.getY() - m_bMargin;
		final int w = extent.getWidth() + m_lMargin + m_rMargin;
		final int h = extent.getHeight() + m_bMargin + m_tMargin;
		return FactoryMgr.getCommonFactory().constructExtent(x, y, w, h);
	}

	@Override public int getMargin(@NotNull DeviceMarginSide side)
	{
		int margin = 0;
		switch (side) {
			case LEFT:
				margin = m_lMargin;
				break;
			case RIGHT:
				margin = m_rMargin;
				break;
			case TOP:
				margin = m_tMargin;
				break;
			case BOTTOM:
				margin = m_bMargin;
				break;
			default:
				break;
		}
		return margin;
	}
}
