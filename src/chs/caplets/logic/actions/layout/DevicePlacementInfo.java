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
import chs.cof.draw.IGrid;
import chs.cof.draw.VertJustificationEnum;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.cof.symbol.ISymbolDef;
import chs.common.IExtent;
import chs.common.IUID;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.system.FactoryMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author chandras on 19-10-2019.
 */
public class DevicePlacementInfo implements IDevicePlacementInfo
{

	@NotNull private IDevicePlacementGraphicControl m_constraintsController;
	@NotNull private IDevice m_device;
	@NotNull private List<IDevicePlacementItem> m_schematics = new ArrayList<>();
	private int m_selectedIdx = 0;
	private PlacementCommitType m_placementCommitType = PlacementCommitType.NOT_COMMITED;
	private boolean m_flipped = false;
	private boolean m_isSelectedForSymbolChange = false;

	public DevicePlacementInfo(@NotNull IDevice device, @NotNull Collection<ISymbolDef> symbolDefs,
			@NotNull IDevicePlacementGraphicControl constraintsController)
	{
		m_constraintsController = constraintsController;
		m_device = device;
		for (ISymbolDef deviceSymbol : symbolDefs) {
			m_schematics.add(new DevicePlacementSymbolItem(deviceSymbol, this));
		}
	}

	public DevicePlacementInfo(@NotNull IDevice device, int paramLength, int paramWidth,
			@NotNull IDevicePlacementGraphicControl constraintsController)
	{
		m_constraintsController = constraintsController;
		m_device = device;
		m_schematics.add(new DevicePlacementSymbolItem(paramLength, paramWidth, this));
	}

	@Nullable private IDevicePlacementItem getPlacementSchemItem()
	{
		return (m_selectedIdx >= 0 && m_selectedIdx < m_schematics.size()) ? m_schematics.get(m_selectedIdx) : null;
	}

	@Override public void commit(@NotNull PlacementCommitType commitType, @NotNull Point snappedPt,
			@NotNull IDynamicGfxService dynamicGfxService)
	{
		m_placementCommitType = commitType;
		regenerateTransientGraphics(snappedPt, dynamicGfxService);
		final IDevicePlacementItem placementSchemItem = getPlacementSchemItem();
		if (placementSchemItem != null) {
			placementSchemItem.commit(snappedPt);
		}
		collectTransientGraphics(dynamicGfxService);
	}

	@NotNull @Override public PlacementCommitType getPlacementCommitType()
	{
		return m_placementCommitType;
	}

	@Override public void regenerateTransientGraphics(@NotNull Point placementPoint,
			@NotNull IDynamicGfxService dynamicGfxService)
	{
		deRegisterTransientGraphics(dynamicGfxService);
		for (IDevicePlacementItem schematic : m_schematics) {
			schematic.regenerateTransientGraphics(placementPoint);
		}
	}

	@Override public void deRegisterTransientGraphics(@NotNull IDynamicGfxService dynamicGfxService)
	{
		for (IDevicePlacementItem schematic : m_schematics) {
			schematic.resetTransientGraphics(dynamicGfxService);
		}
	}

	@Override public void collectTransientGraphics(@NotNull IDynamicGfxService dynamicGfxService)
	{
		deRegisterTransientGraphics(dynamicGfxService);
		final IDevicePlacementItem placementSchemItem = getPlacementSchemItem();
		if (placementSchemItem != null) {
			placementSchemItem.collectTransientGraphics(dynamicGfxService);
		}
	}

	@Override public int getPlacementItemSideGap(@NotNull DeviceMarginSide side)
	{
		final IDevicePlacementItem placementSchemItem = getPlacementSchemItem();
		if (placementSchemItem != null) {
			return placementSchemItem.getPlacementItemSideGap(side);
		}
		return 0;
	}

	@Override public int getPlacementItemInset(@NotNull DeviceMarginSide side)
	{
		final IDevicePlacementItem placementSchemItem = getPlacementSchemItem();
		if (placementSchemItem != null) {
			return placementSchemItem.getPlacementItemInset(side);
		}
		return 0;
	}

	@Override public boolean isMultiSymbol()
	{
		return m_schematics.size() > 1;
	}

	@NotNull @Override public IExtent getMarginExtent(@NotNull IExtent snappedExtent)
	{
		return m_constraintsController.getMarginExtent(m_device, snappedExtent);
	}

	@Override public boolean isValidPlacement(@NotNull IDevicePlacementItem placementItem)
	{
		return m_constraintsController.isValidPlacement(m_device, placementItem);
	}

	@Override public int getMargin(@NotNull DeviceMarginSide side)
	{
		return m_constraintsController.getMargin(m_device, side);
	}

	@NotNull @Override public ISchemDiagram getDiagram()
	{
		return m_constraintsController.getDiagram();
	}

	@NotNull @Override public IGrid getGrid()
	{
		return m_constraintsController.getGrid();
	}

	@NotNull @Override public IProject getProject()
	{
		return m_constraintsController.getProject();
	}

	@NotNull @Override public VertJustificationEnum getVerticalJustification()
	{
		return m_constraintsController.getVerticalJustification();
	}

	@NotNull @Override public HorizJustificationEnum getHorizontalJustification()
	{
		return m_constraintsController.getHorizontalJustification();
	}

	@NotNull @Override public PlacementAxisRotation getPlacementRotation()
	{
		return m_constraintsController.getPlacementRotation();
	}

	@Override public boolean isOriginAligned()
	{
		return m_constraintsController.isOriginAligned();
	}

	@Override public boolean isFlipped()
	{
		return m_flipped;
	}

	@Override public void toggleFlip(@NotNull IDynamicGfxService dynamicGfxService)
	{
		m_flipped = !m_flipped;
		m_schematics.forEach(item -> item.flipped(dynamicGfxService));
	}

	@Override public void toggleSelectionForSymbolChange()
	{
		m_isSelectedForSymbolChange = !m_isSelectedForSymbolChange;
	}

	@Override public void unSelect()
	{
		m_isSelectedForSymbolChange = false;
	}

	@Override public void chooseNextSymbol()
	{
		if (m_selectedIdx < (m_schematics.size() - 1)) {
			++m_selectedIdx;
		}
	}

	@Override public void choosePrevSymbol()
	{
		if (m_selectedIdx > 0) {
			--m_selectedIdx;
		}
	}

	@NotNull @Override public IExtent getSnappedAbsExtent()
	{
		final IDevicePlacementItem placementSchemItem = getPlacementSchemItem();
		if (placementSchemItem != null) {
			return placementSchemItem.getSnappedAbsExtent();
		}
		return FactoryMgr.getCommonFactory().createExtent();
	}

	@NotNull @Override public IExtent getSnappedAbsExtentSqueezed()
	{
		final IDevicePlacementItem placementSchemItem = getPlacementSchemItem();
		if (placementSchemItem != null) {
			return placementSchemItem.getSnappedAbsExtentSqueezed();
		}
		return FactoryMgr.getCommonFactory().createExtent();
	}

	@NotNull @Override public IExtent getSnappedExtent()
	{
		final IDevicePlacementItem placementSchemItem = getPlacementSchemItem();
		if (placementSchemItem != null) {
			return placementSchemItem.getSnappedExtent();
		}
		return FactoryMgr.getCommonFactory().createExtent();
	}

	@Override public boolean isSymbolPreviewMode()
	{
		return m_constraintsController.isSymbolPreviewMode();
	}

	@Override public boolean isSelectedForSymbolChange()
	{
		return m_isSelectedForSymbolChange;
	}

	@Override public void undoCommit()
	{
		m_placementCommitType = PlacementCommitType.NOT_COMMITED;
		for (IDevicePlacementItem schematic : m_schematics) {
			schematic.undoCommit();
		}
	}

	@Override public void endCurrentCommitGroup()
	{
		if (m_placementCommitType.equals(PlacementCommitType.AUTO)) {
			m_placementCommitType = PlacementCommitType.AUTO_END;
		}
	}

	@NotNull @Override public String getDeviceName()
	{
		return m_device.getName();
	}

	@Override public void collectSymbolPinInformation(@NotNull BiConsumer<IUID, Point> pinInfoCollector)
	{
		Map<String, Point> symbolPinLocations = new HashMap<>();
		final IDevicePlacementItem placementSchemItem = getPlacementSchemItem();
		if (placementSchemItem != null) {
			placementSchemItem.collectSymbolPinInformation(symbolPinLocations::put);
		}

		for (IAbstractPin pin : m_device.getPins()) {
			pinInfoCollector.accept(pin.getUID(), symbolPinLocations.get(pin.getName()));
		}
	}

	@Override public void decorateTrailingItem(@NotNull DevicePlacementMode mode)
	{
		final IDevicePlacementItem placementSchemItem = getPlacementSchemItem();
		if (placementSchemItem != null) {
			placementSchemItem.decorateTrailingItem(mode);
		}
	}

	@Override public void terminate()
	{
		final IDevicePlacementItem placementSchemItem = getPlacementSchemItem();
		if (placementSchemItem != null) {
			placementSchemItem.terminate(m_device);
		}
	}

	@Override public void cleanup()
	{
		for (IDevicePlacementItem placementItem : m_schematics) {
			placementItem.cleanup();
		}
	}
}
