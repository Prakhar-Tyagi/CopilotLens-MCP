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
import chs.cof.draw.IRectangle;
import chs.cof.logical.cable.IDevice;
import chs.common.IExtent;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author chandras on 19-10-2019.
 */
public class DeviceSymbolController extends AbstractDevicePlacementController<IDevicePlacementConfig>
{

	@Nullable private Point m_startDragEvent;
	@Nullable private Point m_endDragEvent;
	@Nullable private IRectangle m_selectRect;

	public DeviceSymbolController(@NotNull IDynamicGfxService dynamicGfxService,
			@NotNull IDevicePlacementDataModel dataModel)
	{
		super(dynamicGfxService, dataModel, new DevicePlacementConfig(dataModel.getOneUnitCustomGap()));
	}

	private void discardSelectArea()
	{
		if (m_selectRect != null) {
			m_dynamicGfxService.removeTransientGfx(m_selectRect);
			m_selectRect = null;
		}
	}

	@Override public void beginProcessing()
	{
		m_dataModel.setupController(this);
		regenerateTransientGraphicsAtCurrentPlacementPoint();
	}

	@Override public void endProcessing()
	{
		clearUnCommitedTransientGraphics();
	}

	@Override public void setupNextAxis()
	{

	}

	@Override public void setupPrevAxis()
	{

	}

	@Override public void incrementGroupCustomAdditionalGap()
	{

	}

	@Override public void decrementGroupCustomAdditionalGap()
	{

	}

	@Override public boolean isValidPlacement(@NotNull IDevice device, @NotNull IDevicePlacementItem placementItem)
	{
		return true;
	}

	@NotNull @Override public IExtent getMarginExtent(@NotNull IDevice device, @NotNull IExtent snappedExtent)
	{
		return snappedExtent;
	}

	@Override public int getMargin(@NotNull IDevice device, @NotNull DeviceMarginSide side)
	{
		return getOneUnitCustomGap();
	}

	@Override public boolean hasPendingPlacements()
	{
		return true;
	}

	@Override public void mouseClicked(@NotNull Point currentMousePoint)
	{
		final int currentPlacementIdx = getCurrentPlacementIdx();
		final List<IDevicePlacementInfo> placementInfos = m_dataModel.getPlacementInfos();
		for (int idx = currentPlacementIdx; idx < placementInfos.size(); ++idx) {
			final IDevicePlacementInfo devPlacementInfo = placementInfos.get(idx);
			IExtent snappedExtent = devPlacementInfo.getSnappedAbsExtent();
			if (snappedExtent.containsCoord(currentMousePoint)) {
				processNewSymbolNodeSelection(devPlacementInfo);
				regenerateTransientGraphicsAtCurrentPlacementPoint();
				break;
			}
		}
	}

	protected void processNewSymbolNodeSelection(IDevicePlacementInfo devPlacementInfo)
	{
		devPlacementInfo.toggleSelectionForSymbolChange();
	}

	@Override public void mouseMoved(@NotNull Point currentMousePoint)
	{

	}

	private Point snapToDragGrid(@NotNull Point pt)
	{
		//wanted some consistency in graphics for UTs :):)
		final int precision = IGrid.GRID_SIZE / 20;
		return new Point(IGrid.snap(pt.x, precision), IGrid.snap(pt.y, precision));
	}

	@Override public void mouseDragged(@NotNull Point currentMousePoint)
	{
		final Point pointSnappedForDrag = snapToDragGrid(currentMousePoint);
		if (m_startDragEvent == null) {
			m_startDragEvent = new Point(pointSnappedForDrag);
		}
		discardSelectArea();
		assert m_startDragEvent != null;
		Point stPt = m_startDragEvent;
		Point endPt = pointSnappedForDrag;
		m_selectRect = FactoryMgr.getDrawFactory().constructRectangle(stPt.x, stPt.y, endPt.x, endPt.y);
		assert m_selectRect != null;
		m_dynamicGfxService.addTransientGfx(m_selectRect);
	}

	@Override public void mousePressed(@NotNull Point currentMousePoint)
	{
		m_startDragEvent = null;
		m_endDragEvent = null;
		discardSelectArea();
	}

	@Override public void mouseReleased(@NotNull Point currentMousePoint)
	{
		final Point pointSnappedForDrag = snapToDragGrid(currentMousePoint);
		if (m_startDragEvent != null) {
			m_endDragEvent = new Point(pointSnappedForDrag);
			processEndOfDrag();
		}
		discardSelectArea();
	}

	private void processEndOfDrag()
	{
		if (m_startDragEvent != null && m_endDragEvent != null) {
			final int currentPlacementIdx = getCurrentPlacementIdx();
			final List<IDevicePlacementInfo> placementInfos = m_dataModel.getPlacementInfos();
			final int x = Math.min(m_startDragEvent.x, m_endDragEvent.x);
			final int y = Math.min(m_startDragEvent.y, m_endDragEvent.y);
			final int w = Math.abs(m_startDragEvent.x - m_endDragEvent.x);
			final int h = Math.abs(m_startDragEvent.y - m_endDragEvent.y);
			final IExtent dragBox = FactoryMgr.getCommonFactory().constructExtent(x, y, w, h);
			for (int idx = currentPlacementIdx; idx < placementInfos.size(); ++idx) {
				final IDevicePlacementInfo devPlacementInfo = placementInfos.get(idx);
				IExtent snappedExtent = devPlacementInfo.getSnappedAbsExtent();
				if (snappedExtent.intersects(dragBox)) {
					processNewSymbolNodeSelection(devPlacementInfo);
				}
			}
			regenerateTransientGraphicsAtCurrentPlacementPoint();
		}
	}

	@Override public boolean isAbuttingUnderProgress()
	{
		return false;
	}

	@Override public void handleAbuttingShift(int totalShift)
	{

	}

	@Override public void handleHorizontalJustification()
	{

	}

	@Override public void handleVerticalJustification()
	{

	}

	@Override public void toggleOrderOfPlacement()
	{

	}

	private void executeOnSelectedDevices(Consumer<IDevicePlacementInfo> consumer)
	{
		for (IDevicePlacementInfo selectedDevice : m_dataModel.getPlacementInfos()) {
			if (selectedDevice.isSelectedForSymbolChange()) {
				consumer.accept(selectedDevice);
			}
		}
	}

	@Override public void toggleFlip()
	{
		executeOnSelectedDevices(iDevicePlacementInfo -> iDevicePlacementInfo.toggleFlip(m_dynamicGfxService));
		regenerateTransientGraphicsAtCurrentPlacementPoint();
	}

	@Override public void toggleOriginAligned()
	{

	}

	@Override public void setupNextPlacementMode()
	{

	}

	@Override public void setupAbutPlacementMode()
	{

	}

	@Override @NotNull public List<IDevicePlacementInfo> getCommitedDevices()
	{
		return Collections.emptyList();
	}

	@Override public boolean canUndo()
	{
		return false;
	}

	@Override public void undo()
	{

	}

	@Override public void handleNextSymbolSelection()
	{
		executeOnSelectedDevices(IDevicePlacementInfo::chooseNextSymbol);
		regenerateTransientGraphicsAtCurrentPlacementPoint();
	}

	@Override public void handlePreviousSymbolSelection()
	{
		executeOnSelectedDevices(IDevicePlacementInfo::choosePrevSymbol);
		regenerateTransientGraphicsAtCurrentPlacementPoint();
	}

	@Override @Nullable public String getTooltipText(boolean areAdvancedFeaturesEnabled)
	{
		if (!hasPendingPlacements()) {
			//nothing under progress.
			return null;
		}

		String modeText = ResourceMgr.getString(DevicePlacementController.class,
				"DeviceSymbolController.hint.symbol.select");

		StringBuilder tooltipText = new StringBuilder("<html>");
		tooltipText.append(modeText);

		tooltipText.append("<br>");
		tooltipText.append(ResourceMgr.getString(DevicePlacementController.class,
				"DeviceSymbolController.options.text.P"));

		tooltipText.append("<br>");
		tooltipText.append(ResourceMgr.getString(DevicePlacementController.class,
				"DeviceSymbolController.options.text.F"));

		final List<IDevicePlacementInfo> placementInfos = m_dataModel.getPlacementInfos();
		final int size = placementInfos.size();
		final int currentPlacementIdx = getCurrentPlacementIdx();
		boolean isMultiSymbol = false;
		for (int i = currentPlacementIdx; i < size; ++i) {
			if (placementInfos.get(i).isMultiSymbol()) {
				isMultiSymbol = true;
				break;
			}
		}

		if (isMultiSymbol) {
			tooltipText.append("<br>");
			tooltipText.append(ResourceMgr.getString(DevicePlacementController.class,
					"DeviceSymbolController.options.text.1"));

			tooltipText.append("<br>");
			tooltipText.append(ResourceMgr.getString(DevicePlacementController.class,
					"DeviceSymbolController.options.text.2"));
		}

		tooltipText.append("</html>");
		return tooltipText.toString();
	}

	@Override public boolean isSymbolPreviewMode()
	{
		return true;
	}

	protected int getRealFreeSpaceBetweenItemsToBeSqueezed(
			@NotNull IDevicePlacementInfo currentPlacementDeviceInfo,
			@NotNull IDevicePlacementInfo nextPlacementDeviceInfo)
	{
		return 0;
	}
}
