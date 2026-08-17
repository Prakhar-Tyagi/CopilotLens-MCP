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

import chs.common.IExtent;
import org.jetbrains.annotations.NotNull;

/**
 * @author chandras on 19-10-2019.
 */
public interface IDeviceGridExtent
{

	double ALIGN_OVERLAP_TOLERANCE = 0.10;
	double PLACE_OVERLAP_TOLERANCE = 0.10;

	int getPlacementItemSideGap(@NotNull DeviceMarginSide side);

	int getPlacementItemInset(@NotNull DeviceMarginSide side);

	@NotNull IExtent getSnappedExtent();

	int getCenterX();

	int getCenterY();

	int getLeft();

	int getRight();

	int getTop();

	int getBottom();
}
