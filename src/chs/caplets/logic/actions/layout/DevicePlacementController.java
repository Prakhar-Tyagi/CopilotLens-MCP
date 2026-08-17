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
import chs.cof.draw.SnapStyle;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemOtherComponent;
import chs.common.IExtent;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.system.FactoryMgr;
import chs.utilities.BuildInfo;
import chs.utilities.ResourceMgr;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

/**
 * @author chandras on 19-10-2019.
 */
public class DevicePlacementController extends AbstractDevicePlacementController<IUndoableDevicePlacementConfig>
{

	@NotNull private final IDevicePlacementMarginControl m_deviceMarginControl;
	@NotNull private final IDevicePlacementValidityControl m_placementValidityControl;
	@NotNull private final LayoutComponentHighlightHelper m_mountingHighlight;
	@NotNull private final ExtentObjectAlignmentHighlightHelper m_alignGuide;

	public DevicePlacementController(@NotNull ISchemDiagram diagram, @NotNull IDynamicGfxService dynamicGfxService,
			@NotNull IDevicePlacementDataModel dataModel)
	{
		super(dynamicGfxService, dataModel, new UndoableDevicePlacementConfig(dataModel.getOneUnitCustomGap()));
		m_deviceMarginControl = new DevicePlacementMarginControl(diagram);
		m_placementValidityControl = new DevicePlacementValidityControl(diagram);
		final ISchemDiagram schemDiagram = dataModel.getDiagram();
		m_mountingHighlight = new LayoutComponentHighlightHelper(schemDiagram);
		m_alignGuide = new ExtentObjectAlignmentHighlightHelper(schemDiagram);
	}

	@Override public void setupNextAxis()
	{
		if (isAbuttingUnderProgress()) {
			return;
		}
		m_placementConfig.setupNextAxis();
		regenerateTransientGraphicsAtCurrentPlacementPoint();
	}

	@Override public void setupPrevAxis()
	{
		if (isAbuttingUnderProgress()) {
			return;
		}
		m_placementConfig.setupPrevAxis();
		regenerateTransientGraphicsAtCurrentPlacementPoint();
	}

	@Override public void incrementGroupCustomAdditionalGap()
	{
		final boolean multiItemPending = (m_dataModel.getPlacementInfos().size() - getCurrentPlacementIdx()) > 1;
		final int totalShift = m_placementConfig.incrementGroupCustomAdditionalGap();
		if (totalShift != 0 && multiItemPending) {
			handleAbuttingShift(totalShift);
			regenerateTransientGraphicsAtCurrentPlacementPoint();
		}
	}

	@Override public void decrementGroupCustomAdditionalGap()
	{
		final int totalShift = m_placementConfig.decrementGroupCustomAdditionalGap();
		final boolean multiItemPending = (m_dataModel.getPlacementInfos().size() - getCurrentPlacementIdx()) > 1;
		if (totalShift != 0 && multiItemPending) {
			handleAbuttingShift(totalShift);
			regenerateTransientGraphicsAtCurrentPlacementPoint();
		}
	}

	@Override public boolean isValidPlacement(@NotNull IDevice device, @NotNull IDevicePlacementItem placementItem)
	{
		final ISchemOtherComponent snappedMount = m_mountRailSnap != null ? m_mountRailSnap.getSnappedMount() : null;
		return m_placementValidityControl.isValidPlacement(device, placementItem, snappedMount);
	}

	@NotNull @Override public IExtent getMarginExtent(@NotNull IDevice device, @NotNull IExtent snappedExtent)
	{
		return m_deviceMarginControl.getMarginExtent(device, snappedExtent);
	}

	@Override public int getMargin(@NotNull IDevice device, @NotNull DeviceMarginSide side)
	{
		return m_deviceMarginControl.getMargin(device, side);
	}

	@Override public boolean hasPendingPlacements()
	{
		int currentPlacementIdx = m_placementConfig.getCurrentPlacementIdx();
		final List<IDevicePlacementInfo> placementInfos = m_dataModel.getPlacementInfos();
		return currentPlacementIdx >= 0 && currentPlacementIdx < placementInfos.size();
	}

	@Override public void mouseClicked(@NotNull Point currentMousePoint)
	{
		if (!hasPendingPlacements()) {
			return;
		}
		final DevicePlacementMode currentPlacementMode = m_placementConfig.getPlacementMode();
		Point snappedPt = getGridSnappedPoint(currentMousePoint);
		final int preCommitPlacementIdxcurrentPlacementIdx = m_placementConfig.getCurrentPlacementIdx();
		if (currentPlacementMode.equals(DevicePlacementMode.MANUAL)) {
			processCommitForManualPlacementMode(snappedPt);
		}
		else if (currentPlacementMode.equals(DevicePlacementMode.AUTO)) {
			processCommitForAutoPlacementMode(snappedPt);
		}
		else if (currentPlacementMode.equals(DevicePlacementMode.GROUP)) {
			processCommitForGroupPlacementMode(snappedPt);
		}
		final int postCommitPlacementIdx = m_placementConfig.getCurrentPlacementIdx();
		final List<IDevicePlacementInfo> placementInfos = m_dataModel.getPlacementInfos();
		final int itemCount = placementInfos.size();
		final int maxIdx = Math.min(itemCount, postCommitPlacementIdx);
		for (int idx = preCommitPlacementIdxcurrentPlacementIdx; idx < maxIdx; ++idx) {
			m_alignGuide.register(placementInfos.get(idx).getSnappedAbsExtentSqueezed());
		}
	}

	protected void processCommitForGroupPlacementMode(@NotNull Point snappedPt)
	{
		final List<IDevicePlacementInfo> placementInfos = m_dataModel.getPlacementInfos();
		final int currentPlacementIdx = m_placementConfig.getCurrentPlacementIdx();
		clearUnCommitedTransientGraphics();
		m_placementConfig.commit(snappedPt);
		Point nextItemPlacementPoint = new Point(snappedPt);
		final int itemCount = placementInfos.size();
		for (int idx = currentPlacementIdx; idx < itemCount; ++idx) {
			final IDevicePlacementInfo currentPlacementDeviceInfo = placementInfos.get(idx);
			currentPlacementDeviceInfo.commit(PlacementCommitType.GROUP, nextItemPlacementPoint, m_dynamicGfxService);

			final int nextIdx = idx + 1;
			if (nextIdx < itemCount) {
				final IDevicePlacementInfo nextPlacementDeviceInfo = placementInfos.get(nextIdx);
				computeNextItemPlacementPoint(currentPlacementDeviceInfo, nextPlacementDeviceInfo,
						nextItemPlacementPoint);
			}
		}
		m_placementConfig.setupPlacementIndex(itemCount);
		m_placementConfig.moveToNewPlacementPoint(null);
		if (itemCount > 0) {
			placementInfos.get(itemCount - 1).endCurrentCommitGroup();
		}
		regenerateTransientGraphicsAtCurrentPlacementPoint();
	}

	protected void processCommitForAutoPlacementMode(@NotNull Point snappedPt)
	{
		final List<IDevicePlacementInfo> placementInfos = m_dataModel.getPlacementInfos();
		final int currentPlacementIdx = m_placementConfig.getCurrentPlacementIdx();
		final Point currentPlacementPoint = m_placementConfig.getCurrentPlacementPoint();
		if (isAbuttingUnderProgress() && currentPlacementPoint != null) {
			final IDevicePlacementInfo currentPlacementDeviceInfo = placementInfos.get(currentPlacementIdx);
			currentPlacementDeviceInfo.commit(PlacementCommitType.AUTO, currentPlacementPoint, m_dynamicGfxService);
			m_placementConfig.commit(currentPlacementPoint);
			final int newPlacementIndex = currentPlacementIdx + 1;
			m_placementConfig.setupPlacementIndex(newPlacementIndex);
			if (newPlacementIndex >= placementInfos.size()) {
				m_placementConfig.moveToNewPlacementPoint(null);
				currentPlacementDeviceInfo.endCurrentCommitGroup();
			}
			else {
				final IDevicePlacementInfo nextPlacementDeviceInfo = placementInfos.get(newPlacementIndex);
				computeNextItemPlacementPoint(currentPlacementDeviceInfo, nextPlacementDeviceInfo,
						currentPlacementPoint);
				m_placementConfig.moveToNewPlacementPoint(currentPlacementPoint);
			}
		}
		else {
			clearUnCommitedTransientGraphics();
			final IDevicePlacementInfo currentPlacementDeviceInfo = placementInfos.get(currentPlacementIdx);
			currentPlacementDeviceInfo.commit(PlacementCommitType.AUTO, snappedPt, m_dynamicGfxService);
			m_placementConfig.commit(snappedPt);
			final int newPlacementIndex = currentPlacementIdx + 1;
			m_placementConfig.setupPlacementIndex(newPlacementIndex);
			if (newPlacementIndex >= placementInfos.size()) {
				m_placementConfig.moveToNewPlacementPoint(null);
				currentPlacementDeviceInfo.endCurrentCommitGroup();
			}
			else {
				final IDevicePlacementInfo nextPlacementDeviceInfo = placementInfos.get(newPlacementIndex);
				computeNextItemPlacementPoint(currentPlacementDeviceInfo, nextPlacementDeviceInfo, snappedPt);
				m_placementConfig.moveToNewPlacementPoint(snappedPt);
			}
		}
		regenerateTransientGraphicsAtCurrentPlacementPoint();
	}

	protected void processCommitForManualPlacementMode(@NotNull Point snappedPt)
	{
		final List<IDevicePlacementInfo> placementInfos = m_dataModel.getPlacementInfos();
		final int currentPlacementIdx = m_placementConfig.getCurrentPlacementIdx();
		clearUnCommitedTransientGraphics();
		final IDevicePlacementInfo currentPlacementDeviceInfo = placementInfos.get(currentPlacementIdx);
		currentPlacementDeviceInfo.commit(PlacementCommitType.MANUAL, snappedPt, m_dynamicGfxService);
		m_placementConfig.commit(snappedPt);
		final int newPlacementIndex = currentPlacementIdx + 1;
		m_placementConfig.setupPlacementIndex(newPlacementIndex);
		if (newPlacementIndex >= placementInfos.size()) {
			m_placementConfig.moveToNewPlacementPoint(null);
			currentPlacementDeviceInfo.endCurrentCommitGroup();
		}
		else {
			final IDevicePlacementInfo nextPlacementDeviceInfo = placementInfos.get(newPlacementIndex);
			computeNextItemPlacementPoint(currentPlacementDeviceInfo, nextPlacementDeviceInfo, snappedPt);
			m_placementConfig.moveToNewPlacementPoint(snappedPt);
		}
		regenerateTransientGraphicsAtCurrentPlacementPoint();
	}

	@Override @NotNull public Point getGridSnappedPoint(@NotNull Point currentMousePoint)
	{
		final IGrid grid = getGrid();
		final int snapX = grid.snap(currentMousePoint.x);
		final int snapY = grid.snap(currentMousePoint.y);
		final Point snappedPt = new Point(snapX, snapY);
		m_mountingHighlight.snapToCenterGridLine(snappedPt);
		return snappedPt;
	}

	@Override public void mouseMoved(@NotNull Point currentMousePoint)
	{
		final Point currentPlacementPoint = m_placementConfig.getCurrentPlacementPoint();
		Point snappedPt = getGridSnappedPoint(currentMousePoint);
		if (currentPlacementPoint != null && currentPlacementPoint.equals(snappedPt) || isAbuttingUnderProgress()
				|| !hasPendingPlacements()) {
			return;
		}
		m_mountRailSnap = m_mountingHighlight.determineMountRailSnap(snappedPt);
		m_placementConfig.moveToNewPlacementPoint(snappedPt);
		regenerateTransientGraphicsAtCurrentPlacementPoint();
	}

	@Override public void mouseDragged(@NotNull Point currentMousePoint)
	{

	}

	@Override public void mousePressed(@NotNull Point currentMousePoint)
	{

	}

	@Override public void mouseReleased(@NotNull Point currentMousePoint)
	{

	}

	@Override public boolean isAbuttingUnderProgress()
	{
		final DevicePlacementMode placementMode = m_placementConfig.getPlacementMode();
		final int currentPlacementIdx = m_placementConfig.getCurrentPlacementIdx();
		final Point currentPlacementPoint = m_placementConfig.getCurrentPlacementPoint();
		final List<IDevicePlacementInfo> placementInfos = m_dataModel.getPlacementInfos();
		if (placementMode.equals(DevicePlacementMode.AUTO) && currentPlacementIdx > 0
				&& currentPlacementIdx < placementInfos.size() && currentPlacementPoint != null) {
			final PlacementCommitType lastPlacementCommitType =
					placementInfos.get(currentPlacementIdx - 1).getPlacementCommitType();
			return PlacementCommitType.AUTO.equals(lastPlacementCommitType);
		}
		return false;
	}

	@Override public void handleAbuttingShift(int totalShift)
	{
		final Point currentPlacementPoint = m_placementConfig.getCurrentPlacementPoint();
		if (isAbuttingUnderProgress() && currentPlacementPoint != null) {
			PlacementAxisRotation axisRotation = getPlacementRotation();
			AffineTransform affineTransform = axisRotation.getTransform();
			final Point ptTransShift = new Point(totalShift, 0);
			affineTransform.transform(ptTransShift, ptTransShift);
			currentPlacementPoint.x += ptTransShift.x;
			currentPlacementPoint.y += ptTransShift.y;
			m_placementConfig.moveToNewPlacementPoint(currentPlacementPoint);
		}
	}

	@Override public void handleHorizontalJustification()
	{
		if (!isAbuttingUnderProgress()) {
			m_placementConfig.handleHorizontalJustification();
			regenerateTransientGraphicsAtCurrentPlacementPoint();
		}
	}

	@Override public void handleVerticalJustification()
	{
		if (!isAbuttingUnderProgress()) {
			m_placementConfig.handleVerticalJustification();
			regenerateTransientGraphicsAtCurrentPlacementPoint();
		}
	}

	@Override public void toggleOrderOfPlacement()
	{
		m_dataModel.reverseOrderOfPlacement(m_placementConfig.getCurrentPlacementIdx());
		regenerateTransientGraphicsAtCurrentPlacementPoint();
	}

	@Override public void toggleFlip()
	{
		final List<IDevicePlacementInfo> placementInfos = m_dataModel.getPlacementInfos();
		final int currentPlacementIdx = m_placementConfig.getCurrentPlacementIdx();
		for (int idx = currentPlacementIdx; idx < placementInfos.size(); ++idx) {
			final IDevicePlacementInfo currentPlacementDeviceInfo = placementInfos.get(idx);
			currentPlacementDeviceInfo.toggleFlip(m_dynamicGfxService);
		}
		regenerateTransientGraphicsAtCurrentPlacementPoint();
	}

	@Override public void toggleOriginAligned()
	{
		m_placementConfig.toggleOriginAligned();
		regenerateTransientGraphicsAtCurrentPlacementPoint();
	}

	@Override public void setupNextPlacementMode()
	{
		prepareForNextBatchOfPlacement();
		m_placementConfig.setupNextPlacementMode();
		regenerateTransientGraphicsAtCurrentPlacementPoint();
	}

	protected void prepareForNextBatchOfPlacement()
	{
		final int currentPlacementIdx = m_placementConfig.getCurrentPlacementIdx();
		final List<IDevicePlacementInfo> placementInfos = m_dataModel.getPlacementInfos();
		if (currentPlacementIdx > 0 && currentPlacementIdx <= placementInfos.size()) {
			placementInfos.get(currentPlacementIdx - 1).endCurrentCommitGroup();
		}
	}

	@Override public void setupAbutPlacementMode()
	{
		prepareForNextBatchOfPlacement();
		m_placementConfig.setupAbutPlacementMode();
		regenerateTransientGraphicsAtCurrentPlacementPoint();
	}

	@Override @NotNull public List<IDevicePlacementInfo> getCommitedDevices()
	{
		final List<IDevicePlacementInfo> commitedDevices = new ArrayList<>();
		final int currentPlacementIdx = m_placementConfig.getCurrentPlacementIdx();
		final List<IDevicePlacementInfo> placementInfos = m_dataModel.getPlacementInfos();
		for (int idx = 0; idx < currentPlacementIdx; ++idx) {
			commitedDevices.add(placementInfos.get(idx));
		}
		return commitedDevices;
	}

	@Override public boolean canUndo()
	{
		return m_placementConfig.canUndo();
	}

	@Override public void undo()
	{
		final List<IDevicePlacementInfo> placementInfos = m_dataModel.getPlacementInfos();
		final int preUndoPlacementIdx = m_placementConfig.getCurrentPlacementIdx();
		m_placementConfig.undo();
		final int postUndoPlacementIdx = m_placementConfig.getCurrentPlacementIdx();
		if (postUndoPlacementIdx >= 0 && preUndoPlacementIdx <= placementInfos.size()) {
			for (int idx = postUndoPlacementIdx; idx < preUndoPlacementIdx; ++idx) {
				final IDevicePlacementInfo placementInfo = placementInfos.get(idx);
				placementInfo.undoCommit();
			}
		}
		regenerateTransientGraphicsAtCurrentPlacementPoint();
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

	@Override public void handleNextSymbolSelection()
	{

	}

	@Override public void handlePreviousSymbolSelection()
	{

	}

	@Override @Nullable public String getTooltipText(boolean areAdvancedFeaturesEnabled)
	{
		if (!hasPendingPlacements()) {
			//nothing under progress.
			return null;
		}

		return areAdvancedFeaturesEnabled ? getAdvancedTooltipText() : getBasicTooltipText();
	}

	private String getAdvancedTooltipText()
	{
		final DevicePlacementMode currentPlacementMode = m_placementConfig.getPlacementMode();
		final DevicePlacementMode nextPlacementMode = currentPlacementMode.next();
		final boolean isAbutMode = DevicePlacementMode.AUTO.equals(currentPlacementMode);
		final String nextModeText = nextPlacementMode.toDisplayString();
		final String abutModeText = DevicePlacementMode.AUTO.toDisplayString();
		final String initModeText = IDevicePlacementConfig.INITIAL_PLACEMENT_MODE.toDisplayString();
		String modeDisplayTextForNext = isAbutMode ? HTMLHelper.italic(initModeText) : HTMLHelper.bold(nextModeText);
		String modeDisplayTextForAbut = isAbutMode ? HTMLHelper.bold(abutModeText) : HTMLHelper.italic(abutModeText);
		final int currentPlacementIdx = getCurrentPlacementIdx();
		final int totalCount = m_dataModel.getPlacementInfos().size();

		StringBuilder tooltipText = new StringBuilder("<html>");
		tooltipText.append(ResourceMgr.getString(DevicePlacementController.class,
				"DevicePlacementController.options.text.P", modeDisplayTextForNext));

		tooltipText.append("<br>");
		tooltipText.append(ResourceMgr.getString(DevicePlacementController.class,
				"DevicePlacementController.options.text.C", modeDisplayTextForAbut));

		tooltipText.append("<br>");
		tooltipText.append(ResourceMgr.getString(DevicePlacementController.class,
				"DevicePlacementController.options.text.S"));

		boolean multiItemPlacement = (totalCount - currentPlacementIdx) > 1;
		if (multiItemPlacement) {
			tooltipText.append("<br>");
			tooltipText.append(ResourceMgr.getString(DevicePlacementController.class,
					"DevicePlacementController.options.text.spacing"));
		}

		if (!isAbuttingUnderProgress()) {
			tooltipText.append("<br>");
			tooltipText.append(ResourceMgr.getString(DevicePlacementController.class,
					"DevicePlacementController.options.text.R"));
		}

		tooltipText.append("<br>");
		tooltipText.append(ResourceMgr.getString(DevicePlacementController.class,
				"DevicePlacementController.options.text.F"));

		if (multiItemPlacement) {
			tooltipText.append("<br>");
			tooltipText.append(ResourceMgr.getString(DevicePlacementController.class,
					"DevicePlacementController.options.text.O"));
		}

		if (!isAbuttingUnderProgress()) {
			tooltipText.append("<br>");
			tooltipText.append(ResourceMgr.getString(DevicePlacementController.class,
					"DevicePlacementController.options.text.Q"));

			if (BuildInfo.getBuildInfo().areDeveloperExtensionsEnabled()) {
				tooltipText.append("<br>");
				tooltipText.append(ResourceMgr.getString(DevicePlacementController.class,
						"DevicePlacementController.options.text.H"));
			}

			tooltipText.append("<br>");
			tooltipText.append(ResourceMgr.getString(DevicePlacementController.class,
					"DevicePlacementController.options.text.V"));
		}

		if (currentPlacementIdx > 0) {
			tooltipText.append("<br>");
			tooltipText.append(ResourceMgr.getString(DevicePlacementController.class,
					"DevicePlacementController.options.text.back"));
		}

		tooltipText.append("<br>");
		tooltipText.append(ResourceMgr.getString(DevicePlacementController.class,
				"DevicePlacementController.options.disable.advanced"));

		tooltipText.append("</html>");
		return tooltipText.toString();
	}

	private String getBasicTooltipText()
	{
		final int currentPlacementIdx = getCurrentPlacementIdx();
		final int totalCount = m_dataModel.getPlacementInfos().size();
		StringBuilder tooltipText = new StringBuilder("<html>");
		final String modeText = HTMLHelper.bold(m_placementConfig.getPlacementMode().next().toDisplayString());
		tooltipText.append(ResourceMgr.getString(DevicePlacementController.class,
				"DevicePlacementController.options.text.P", modeText));

		boolean multiItemPlacement = (totalCount - currentPlacementIdx) > 1;
		if (multiItemPlacement) {
			tooltipText.append("<br>");
			tooltipText.append(ResourceMgr.getString(DevicePlacementController.class,
					"DevicePlacementController.options.text.spacing"));
		}

		if (!isAbuttingUnderProgress()) {
			tooltipText.append("<br>");
			tooltipText.append(ResourceMgr.getString(DevicePlacementController.class,
					"DevicePlacementController.options.text.R"));
		}

		if (currentPlacementIdx > 0) {
			tooltipText.append("<br>");
			tooltipText.append(ResourceMgr.getString(DevicePlacementController.class,
					"DevicePlacementController.options.text.back"));
		}

		tooltipText.append("<br>");
		tooltipText.append(ResourceMgr.getString(DevicePlacementController.class,
				"DevicePlacementController.options.enable.advanced"));

		tooltipText.append("</html>");
		return tooltipText.toString();
	}

	@Override public boolean isSymbolPreviewMode()
	{
		return false;
	}

	protected void regenerateMountingHighlights(@Nullable Point currentPlacement_point)
	{
		if (currentPlacement_point != null) {
			m_mountingHighlight.diagnose(currentPlacement_point, m_otherTransientGfx::add);
		}

		final List<IDevicePlacementInfo> placementInfos = m_dataModel.getPlacementInfos();
		final int currentPlacementIdx = getCurrentPlacementIdx();
		if (currentPlacementIdx < placementInfos.size()) {
			final DevicePlacementMode currentPlacementMode = m_placementConfig.getPlacementMode();
			final IExtent winodwExtent = FactoryMgr.getCommonFactory()
					.constructExtent(placementInfos.get(currentPlacementIdx).getSnappedAbsExtentSqueezed());
			if (DevicePlacementMode.GROUP.equals(currentPlacementMode)) {
				for (int idx = currentPlacementIdx + 1; idx < placementInfos.size(); ++idx) {
					winodwExtent.addUnion(placementInfos.get(idx).getSnappedAbsExtentSqueezed());
				}
			}
			m_alignGuide.diagnose(winodwExtent, m_otherTransientGfx::add, SnapStyle.GRID_SNAP, false);
		}
	}

	protected void regenerateOtherTransientGraphics(@Nullable Point currentPlacement_point)
	{
		regenerateMountingHighlights(currentPlacement_point);

		regenerateGapDimensionTransients();

		regenerateWireTransients();
	}
}
