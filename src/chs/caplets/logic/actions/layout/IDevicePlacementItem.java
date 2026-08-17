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

import chs.cof.logical.cable.IDevice;
import chs.cof.logical.schem.IPinList;
import chs.cof.symbol.ISymbolDef;
import chs.common.IExtent;
import chs.services.dynamicgfx.IDynamicGfxService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.function.BiConsumer;

/**
 * @author chandras on 19-10-2019.
 */
public interface IDevicePlacementItem
{

	void resetTransientGraphics(@NotNull IDynamicGfxService dynamicGfxService);

	void collectTransientGraphics(@NotNull IDynamicGfxService dynamicGfxService);

	void regenerateTransientGraphics(@NotNull Point placementPoint);

	int getPlacementItemSideGap(@NotNull DeviceMarginSide side);

	int getPlacementItemInset(@NotNull DeviceMarginSide side);

	void commit(@NotNull Point commitPoint);

	boolean isCommited();

	void undoCommit();

	@Nullable ISymbolDef getSymbol();

	@Nullable IPinList getSchematic();

	@NotNull IExtent getSnappedAbsExtent();

	@NotNull IExtent getSnappedAbsExtentSqueezed();

	@NotNull IExtent getSnappedExtent();

	boolean isSymbolPreviewMode();

	void collectSymbolPinInformation(@NotNull BiConsumer<String, Point> pinInfoCollector);

	void decorateTrailingItem(@NotNull DevicePlacementMode mode);

	void terminate(@NotNull IDevice device);

	void cleanup();

	void flipped(@NotNull IDynamicGfxService dynamicGfxService);
}
