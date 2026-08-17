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
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.List;

/**
 * @author chandras on 19-10-2019.
 */
public interface IDevicePlacementController extends IDevicePlacementGraphicControl
{

	void setupNextAxis();

	void setupPrevAxis();

	int getOneUnitCustomGap();

	void incrementGroupCustomAdditionalGap();

	void decrementGroupCustomAdditionalGap();

	boolean hasPendingPlacements();

	void mouseClicked(@NotNull Point currentMousePoint);

	void clearUnCommitedTransientGraphics();

	void mouseMoved(@NotNull Point currentMousePoint);

	void mouseDragged(@NotNull Point currentMousePoint);

	void mousePressed(@NotNull Point currentMousePoint);

	void mouseReleased(@NotNull Point currentMousePoint);

	boolean isAbuttingUnderProgress();

	void handleAbuttingShift(int totalShift);

	void regenerateTransientGraphics(@Nullable Point currentPlacement_point);

	void computeNextItemPlacementPoint(@NotNull IDevicePlacementInfo currentPlacementDeviceInfo,
			@NotNull IDevicePlacementInfo nextPlacementDeviceInfo, @NotNull Point nextItemPlacementPoint);

	@NotNull Point getGridSnappedPoint(@NotNull Point currentMousePoint);

	void handleHorizontalJustification();

	void handleVerticalJustification();

	void toggleOrderOfPlacement();

	void toggleFlip();

	void toggleOriginAligned();

	void setupNextPlacementMode();

	void setupAbutPlacementMode();

	@NotNull List<IDevicePlacementInfo> getCommitedDevices();

	boolean canUndo();

	void undo();

	@Nullable Point getCurrentPlacementPoint();

	int getCurrentPlacementIdx();

	void moveToNewPlacementPoint(@Nullable Point currentPlacementPoint);

	void setupPlacementIndex(int currentPlacementIndex);

	void beginProcessing();

	void endProcessing();

	void handleNextSymbolSelection();

	void handlePreviousSymbolSelection();

	@NotNull IExtent computeTransientGraphicsBoundary();

	@Nullable String getTooltipText(boolean areAdvancedFeaturesEnabled);

	void terminate(boolean successful);
}
