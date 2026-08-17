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
import chs.common.IUID;
import chs.services.dynamicgfx.IDynamicGfxService;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;
import java.util.function.BiConsumer;

/**
 * @author chandras on 19-10-2019.
 */
public interface IDevicePlacementInfo extends IBasicDevicePlacementControl
{

	boolean isValidPlacement(@NotNull IDevicePlacementItem placementItem);

	@NotNull IExtent getMarginExtent(@NotNull IExtent snappedExtent);

	int getMargin(@NotNull DeviceMarginSide side);

	void commit(@NotNull PlacementCommitType commitType, @NotNull Point snappedPt,
			@NotNull IDynamicGfxService dynamicGfxService);

	@NotNull PlacementCommitType getPlacementCommitType();

	void regenerateTransientGraphics(@NotNull Point placementPoint,
			@NotNull IDynamicGfxService dynamicGfxService);

	void deRegisterTransientGraphics(@NotNull IDynamicGfxService dynamicGfxService);

	void collectTransientGraphics(@NotNull IDynamicGfxService dynamicGfxService);

	int getPlacementItemSideGap(@NotNull DeviceMarginSide side);

	int getPlacementItemInset(@NotNull DeviceMarginSide side);

	boolean isMultiSymbol();

	boolean isFlipped();

	void toggleFlip(@NotNull IDynamicGfxService dynamicGfxService);

	void toggleSelectionForSymbolChange();

	void unSelect();

	void chooseNextSymbol();

	void choosePrevSymbol();

	@NotNull IExtent getSnappedAbsExtent();

	@NotNull IExtent getSnappedAbsExtentSqueezed();

	@NotNull IExtent getSnappedExtent();

	boolean isSymbolPreviewMode();

	boolean isSelectedForSymbolChange();

	void undoCommit();

	void endCurrentCommitGroup();

	@NotNull String getDeviceName();

	void collectSymbolPinInformation(@NotNull BiConsumer<IUID, Point> pinInfoCollector);

	void decorateTrailingItem(@NotNull DevicePlacementMode mode);

	void terminate();

	void cleanup();
}
