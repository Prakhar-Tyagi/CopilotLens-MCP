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

/**
 * @author chandras on 19-10-2019.
 */
public interface IBasicDevicePlacementConfig
{

	void setupNextAxis();

	void setupPrevAxis();

	int getOneUnitCustomGap();

	int incrementGroupCustomAdditionalGap();

	int decrementGroupCustomAdditionalGap();

	@NotNull VertJustificationEnum getVerticalJustification();

	@NotNull HorizJustificationEnum getHorizontalJustification();

	@NotNull PlacementAxisRotation getPlacementRotation(@Nullable IMountSnapInfo mountRailSnap);

	void handleHorizontalJustification();

	void handleVerticalJustification();

	void setupNextPlacementMode();

	void setupAbutPlacementMode();

	void moveToNewPlacementPoint(@Nullable Point currentPlacementPoint);

	void commit(@NotNull Point currentPlacementLocation);

	@Nullable Point getCurrentPlacementPoint();

	int getCurrentPlacementIdx();

	@NotNull DevicePlacementMode getPlacementMode();

	int getGroupCustomGap();

	void setupPlacementIndex(int currentPlacementIndex);

	boolean isOriginAligned(@Nullable IMountSnapInfo mountSnapInfo);

	void toggleOriginAligned();
}
