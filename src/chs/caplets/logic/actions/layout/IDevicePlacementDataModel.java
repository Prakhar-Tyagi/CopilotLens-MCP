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

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.List;

/**
 * @author chandras on 19-10-2019.
 */
public interface IDevicePlacementDataModel extends IDevicePlacementGraphicControl
{

	void setupOerands(@NotNull List<IDevicePlacementInfo> operands);

	void setupController(@NotNull IDevicePlacementController placementController);

	@NotNull List<IDevicePlacementInfo> getPlacementInfos();

	void reverseOrderOfPlacement(int startPlacementIdx);

	@Nullable Point getAbsoluteLocationForPin(@NotNull IAbstractPin pin);

	@NotNull ILogicDesign getDesign();

	int getOneUnitCustomGap();
}
