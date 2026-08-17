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
import chs.cof.draw.IColor;
import chs.cof.draw.IDrawFactory;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGrid;
import chs.cof.draw.IPolyline;
import chs.cof.draw.IText;
import chs.cof.draw.IWritableGfxAttribute;
import chs.cof.draw.VertJustificationEnum;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.common.IExtent;
import chs.common.IUID;
import chs.common.geom.GeometryUtils;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.PreciseDecimalFormat;
import chs.utility.GfxUtils;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author chandras on 19-10-2019.
 */
public abstract class AbstractDevicePlacementController<T extends IBasicDevicePlacementConfig>
		implements IDevicePlacementController
{

	private static final Color TransientWireColor = Color.GRAY;
	private static final DecimalFormat GAP_DIM_FORMAT = new PreciseDecimalFormat("######.#####");
	@NotNull protected final IDynamicGfxService m_dynamicGfxService;
	@NotNull protected final T m_placementConfig;
	@NotNull protected final IDevicePlacementDataModel m_dataModel;
	@NotNull protected final List<IGfxObject> m_otherTransientGfx = new ArrayList<>();
	@Nullable protected IMountSnapInfo m_mountRailSnap = null;

	protected AbstractDevicePlacementController(@NotNull IDynamicGfxService dynamicGfxService,
			@NotNull IDevicePlacementDataModel dataModel, @NotNull T placementConfig)
	{
		m_dynamicGfxService = dynamicGfxService;
		m_dataModel = dataModel;
		m_placementConfig = placementConfig;
	}

	@Override public int getOneUnitCustomGap()
	{
		return m_placementConfig.getOneUnitCustomGap();
	}

	protected void regenerateTransientGraphicsAtCurrentPlacementPoint()
	{
		regenerateTransientGraphics(m_placementConfig.getCurrentPlacementPoint());
	}

	@NotNull @Override public ISchemDiagram getDiagram()
	{
		return m_dataModel.getDiagram();
	}

	@NotNull @Override public IGrid getGrid()
	{
		return m_dataModel.getGrid();
	}

	@NotNull public IProject getProject()
	{
		return m_dataModel.getProject();
	}

	@NotNull @Override public VertJustificationEnum getVerticalJustification()
	{
		return m_placementConfig.getVerticalJustification();
	}

	@NotNull @Override public HorizJustificationEnum getHorizontalJustification()
	{
		return m_placementConfig.getHorizontalJustification();
	}

	@NotNull @Override public PlacementAxisRotation getPlacementRotation()
	{
		return m_placementConfig.getPlacementRotation(m_mountRailSnap);
	}

	@Override public boolean isOriginAligned()
	{
		return m_placementConfig.isOriginAligned(m_mountRailSnap);
	}

	@Override public void clearUnCommitedTransientGraphics()
	{
		clearDeviceTransientGraphics();
		clearOtherTransientGraphics();
	}

	protected void clearDeviceTransientGraphics()
	{
		final List<IDevicePlacementInfo> placementInfos = m_dataModel.getPlacementInfos();
		final int currentPlacementIdx = m_placementConfig.getCurrentPlacementIdx();
		for (int idx = currentPlacementIdx; idx < placementInfos.size(); ++idx) {
			final IDevicePlacementInfo deviceInfo = placementInfos.get(idx);
			deviceInfo.deRegisterTransientGraphics(m_dynamicGfxService);
		}
	}

	private boolean shouldShowGapDimension(@NotNull PlacementCommitType currentType,
			@NotNull PlacementCommitType nextType)
	{
		final PlacementCommitType auto = PlacementCommitType.AUTO;
		final PlacementCommitType group = PlacementCommitType.GROUP;
		final PlacementCommitType autoEnd = PlacementCommitType.AUTO_END;
		final PlacementCommitType notCommited = PlacementCommitType.NOT_COMMITED;
		return (auto.equals(currentType) && auto.equals(nextType)) ||
				(auto.equals(currentType) && autoEnd.equals(nextType)) ||
				(auto.equals(currentType) && notCommited.equals(nextType)) ||
				(group.equals(currentType) && group.equals(nextType)) ||
				(notCommited.equals(currentType) && notCommited.equals(nextType));
	}

	@Override public void regenerateTransientGraphics(@Nullable Point currentPlacement_point)
	{
		regenerateDeviceTransientGraphics(currentPlacement_point);

		clearOtherTransientGraphics();

		regenerateOtherTransientGraphics(currentPlacement_point);

		appendOtherTransientsToDynamicGfx();
	}

	protected void regenerateOtherTransientGraphics(@Nullable Point currentPlacement_point)
	{
		regenerateWireTransients();
	}

	protected void appendOtherTransientsToDynamicGfx()
	{
		final IDynamicGfxService dynamicGfxService = m_dynamicGfxService;
		for (IGfxObject dimensionGfx : m_otherTransientGfx) {
			dynamicGfxService.addTransientGfx(dimensionGfx);
		}
	}

	protected void regenerateDeviceTransientGraphics(@Nullable Point currentPlacement_point)
	{
		if (currentPlacement_point != null) {
			final List<IDevicePlacementInfo> placementInfos = m_dataModel.getPlacementInfos();
			final IDynamicGfxService dynamicGfxService = m_dynamicGfxService;
			final int itemsCount = placementInfos.size();
			Point nextItemPlacementPoint = new Point(currentPlacement_point);
			final int currentPlacementIdx = m_placementConfig.getCurrentPlacementIdx();
			for (int idx = currentPlacementIdx; idx < itemsCount; ++idx) {
				final IDevicePlacementInfo currentPlacementDeviceInfo = placementInfos.get(idx);
				currentPlacementDeviceInfo.regenerateTransientGraphics(nextItemPlacementPoint, dynamicGfxService);
				if (idx > currentPlacementIdx) {
					currentPlacementDeviceInfo.decorateTrailingItem(m_placementConfig.getPlacementMode());
				}
				currentPlacementDeviceInfo.collectTransientGraphics(dynamicGfxService);

				final int nextIdx = idx + 1;
				if (nextIdx < itemsCount) {
					final IDevicePlacementInfo nextPlacementDeviceInfo = placementInfos.get(nextIdx);
					computeNextItemPlacementPoint(currentPlacementDeviceInfo, nextPlacementDeviceInfo,
							nextItemPlacementPoint);
				}
			}
		}
	}

	protected void regenerateGapDimensionTransients()
	{
		final List<IDevicePlacementInfo> placementInfos = m_dataModel.getPlacementInfos();
		final int itemsCount = placementInfos.size();
		Set<Pair<IDevicePlacementInfo, IDevicePlacementInfo>> candidatesForGapDimension = new HashSet<>();
		for (int idx = 1; idx < itemsCount; ++idx) {
			final IDevicePlacementInfo currentPlacementDeviceInfo = placementInfos.get(idx);
			final IDevicePlacementInfo prevPlacementDeviceInfo = placementInfos.get(idx - 1);
			if (shouldShowGapDimension(prevPlacementDeviceInfo.getPlacementCommitType(),
					currentPlacementDeviceInfo.getPlacementCommitType())) {
				candidatesForGapDimension.add(new Pair<>(prevPlacementDeviceInfo, currentPlacementDeviceInfo));
			}
		}
		for (Pair<IDevicePlacementInfo, IDevicePlacementInfo> pair : candidatesForGapDimension) {
			regenerateGapDimension(pair.getKey(), pair.getValue());
		}
	}

	protected void regenerateWireTransients()
	{
		final List<IDevicePlacementInfo> placementInfos = m_dataModel.getPlacementInfos();
		Map<IUID, Point> tempDevicePinLocations = new HashMap<>();
		for (IDevicePlacementInfo placementInfo : placementInfos) {
			placementInfo.collectSymbolPinInformation(tempDevicePinLocations::put);
		}

		final ILogicDesign design = m_dataModel.getDesign();
		final IConnectivity connectivity = design.getConnectivity();
		assert connectivity != null;
		for (IWireConductor wire : connectivity.getWireConductors()) {
			boolean shouldDrawWireGfx = false;
			for (IAbstractPin pin : wire.getPins()) {
				if (tempDevicePinLocations.get(pin.getUID()) != null) {
					shouldDrawWireGfx = true;
					break;
				}
			}

			if (shouldDrawWireGfx) {
				final IDrawFactory drawFactory = FactoryMgr.getDrawFactory();
				Set<Point> polylinePoints = new HashSet<>();
				for (IAbstractPin pin : wire.getPins()) {
					Point pinLoc = m_dataModel.getAbsoluteLocationForPin(pin);
					if (pinLoc != null) {
						polylinePoints.add(pinLoc);
					}
					else if (tempDevicePinLocations.containsKey(pin.getUID())) {
						pinLoc = tempDevicePinLocations.get(pin.getUID());
						if (pinLoc != null) {
							polylinePoints.add(pinLoc);
						}
					}
				}

				if (polylinePoints.size() > 1) {
					final IPolyline wireLine = drawFactory.constructPolyline(polylinePoints, false);
					final IColor wireColor = drawFactory.constructColorRGB(TransientWireColor.getRGB());
					final IWritableGfxAttribute wireAttr = drawFactory.constructAttribute(wireColor);
					wireLine.setAttribute(wireAttr);
					m_otherTransientGfx.add(wireLine);
				}
			}
		}
	}

	protected void clearOtherTransientGraphics()
	{
		for (IGfxObject dimensionGfx : m_otherTransientGfx) {
			m_dynamicGfxService.removeTransientGfx(dimensionGfx);
		}
		m_otherTransientGfx.clear();
	}

	private int computeMiddleValue(int v1, int v2, int v3, int v4)
	{
		List<Integer> values = new ArrayList<>(Arrays.asList(v1, v2, v3, v4));
		Collections.sort(values);
		return (values.get(1) + values.get(2)) / 2;
	}

	private void regenerateGapDimension(@NotNull IDevicePlacementInfo current, @NotNull IDevicePlacementInfo next)
	{
		final IExtent currentAbsExtent = current.getSnappedAbsExtentSqueezed();
		final IExtent nextAbsExtent = next.getSnappedAbsExtentSqueezed();
		if (currentAbsExtent.getRight() < nextAbsExtent.getLeft()) {
			final int y = computeMiddleValue(currentAbsExtent.getTop(), currentAbsExtent.getBottom(),
					nextAbsExtent.getTop(), nextAbsExtent.getBottom());
			Point startPt = new Point(currentAbsExtent.getRight(), y);
			Point endPt = new Point(nextAbsExtent.getLeft(), y);
			constructGapDimension(startPt, endPt);
		}
		else if (nextAbsExtent.getRight() < currentAbsExtent.getLeft()) {
			final int y = computeMiddleValue(currentAbsExtent.getTop(), currentAbsExtent.getBottom(),
					nextAbsExtent.getTop(), nextAbsExtent.getBottom());
			Point startPt = new Point(nextAbsExtent.getRight(), y);
			Point endPt = new Point(currentAbsExtent.getLeft(), y);
			constructGapDimension(startPt, endPt);
		}
		else if (currentAbsExtent.getTop() < nextAbsExtent.getBottom()) {
			final int x = computeMiddleValue(currentAbsExtent.getLeft(), currentAbsExtent.getRight(),
					nextAbsExtent.getLeft(), nextAbsExtent.getRight());
			Point startPt = new Point(x, currentAbsExtent.getTop());
			Point endPt = new Point(x, nextAbsExtent.getBottom());
			constructGapDimension(startPt, endPt);
		}
		else if (nextAbsExtent.getTop() < currentAbsExtent.getBottom()) {
			final int x = computeMiddleValue(currentAbsExtent.getLeft(), currentAbsExtent.getRight(),
					nextAbsExtent.getLeft(), nextAbsExtent.getRight());
			Point startPt = new Point(x, nextAbsExtent.getTop());
			Point endPt = new Point(x, currentAbsExtent.getBottom());
			constructGapDimension(startPt, endPt);
		}
	}

	private void constructGapDimension(@NotNull Point lowerPt, @NotNull Point higherPt)
	{
		final IDrawFactory drawFactory = FactoryMgr.getDrawFactory();
		final boolean horizontal = lowerPt.y == higherPt.y;
		final IGrid grid = getGrid();
		final ISchemDiagram diagram = getDiagram();
		final double realUnitVal = IGrid.convertToPhysicalUnitVal(lowerPt.distance(higherPt), grid, diagram);
		final String unitDisplay = diagram.getPhysicalMapping().getType().toDisplayString();
		final String valueText = GAP_DIM_FORMAT.format(realUnitVal) + " " + unitDisplay;
		int textLocX = (lowerPt.x + higherPt.x) / 2;
		int textLocY = (lowerPt.y + higherPt.y) / 2;
		int rotation = horizontal ? GeometryUtils.TWO_SEVENTY_DEGREES : 0;
		final int transientTextSize = IBasicDevicePlacementControl.computeTransientTextSize(getDiagram());
		final IText dimText = drawFactory.constructText(textLocX, textLocY, transientTextSize, rotation, valueText);
		dimText.setHorizontalJustification(HorizJustificationEnum.JustLeft);
		dimText.setVerticalJustification(VertJustificationEnum.JustCenter);
		final IColor textColor = drawFactory.constructColorRGB(128, 128, 0);
		dimText.setAttribute(drawFactory.constructAttribute(textColor));
		m_otherTransientGfx.add(dimText);
		GfxUtils.constructBiDirArrow(lowerPt, higherPt, m_otherTransientGfx);
	}

	@Override public void computeNextItemPlacementPoint(@NotNull IDevicePlacementInfo currentPlacementDeviceInfo,
			@NotNull IDevicePlacementInfo nextPlacementDeviceInfo, @NotNull Point nextItemPlacementPoint)
	{
		final HorizJustificationEnum hJust = m_placementConfig.getHorizontalJustification();
		int shiftX = currentPlacementDeviceInfo.getPlacementItemSideGap(DeviceMarginSide.RIGHT)
				+ nextPlacementDeviceInfo.getPlacementItemSideGap(DeviceMarginSide.LEFT);
		if (m_placementConfig.isOriginAligned(m_mountRailSnap)) {
			final IExtent currentItemSnappedExtent = currentPlacementDeviceInfo.getSnappedExtent();
			final IExtent nextItemSnappedExtent = nextPlacementDeviceInfo.getSnappedExtent();
			final int shiftCurrent = currentPlacementDeviceInfo.isFlipped() ? -currentItemSnappedExtent.getLeft() :
					currentItemSnappedExtent.getRight();
			final int shiftNext = nextPlacementDeviceInfo.isFlipped() ? -nextItemSnappedExtent.getRight() :
					nextItemSnappedExtent.getLeft();
			shiftX = shiftCurrent - shiftNext;
		}
		else if (HorizJustificationEnum.JustLeft.equals(hJust)) {
			shiftX = currentPlacementDeviceInfo.getPlacementItemSideGap(DeviceMarginSide.RIGHT)
					+ currentPlacementDeviceInfo.getPlacementItemSideGap(DeviceMarginSide.LEFT);
		}
		else if (HorizJustificationEnum.JustRight.equals(hJust)) {
			shiftX = nextPlacementDeviceInfo.getPlacementItemSideGap(DeviceMarginSide.RIGHT)
					+ nextPlacementDeviceInfo.getPlacementItemSideGap(DeviceMarginSide.LEFT);
		}

		shiftX -= getRealFreeSpaceBetweenItemsToBeSqueezed(currentPlacementDeviceInfo, nextPlacementDeviceInfo);

		//compute the min edge to egde gap to be maintained.
		int groupCustomGap = m_placementConfig.getGroupCustomGap();
		final int minEdgeToEdgeGap = Math.max(Math.max(currentPlacementDeviceInfo.getMargin(DeviceMarginSide.RIGHT),
				nextPlacementDeviceInfo.getMargin(DeviceMarginSide.LEFT)), groupCustomGap);
		PlacementAxisRotation axisRotation = getPlacementRotation();
		AffineTransform affineTransform = axisRotation.getTransform();
		final int totalShift = shiftX + minEdgeToEdgeGap;
		final Point ptTransShift = new Point(totalShift, 0);
		affineTransform.transform(ptTransShift, ptTransShift);
		nextItemPlacementPoint.x += ptTransShift.x;
		nextItemPlacementPoint.y += ptTransShift.y;
	}

	protected int getRealFreeSpaceBetweenItemsToBeSqueezed(
			@NotNull IDevicePlacementInfo currentPlacementDeviceInfo,
			@NotNull IDevicePlacementInfo nextPlacementDeviceInfo)
	{
		//there might be some real space in no. of grids (max would be ~2 grid) between the items
		//which can be squeezed to have better abutment resolution. round-off to no. of grids.
		int currentItemRightInset = currentPlacementDeviceInfo.getPlacementItemInset(
				currentPlacementDeviceInfo.isFlipped() ? DeviceMarginSide.LEFT : DeviceMarginSide.RIGHT);
		int nextItemLeftInset = nextPlacementDeviceInfo.getPlacementItemInset(
				nextPlacementDeviceInfo.isFlipped() ? DeviceMarginSide.RIGHT : DeviceMarginSide.LEFT);
		final int gridSpacing = getGrid().getGridSpacing();
		int freeSpaceCanBeSqueezed = CommonUtils.toInteger(IDeviceGridExtent.PLACE_OVERLAP_TOLERANCE * gridSpacing)
				+ currentItemRightInset + nextItemLeftInset;
		freeSpaceCanBeSqueezed = gridSpacing * (freeSpaceCanBeSqueezed / gridSpacing);
		return freeSpaceCanBeSqueezed;
	}

	@Override @NotNull public Point getGridSnappedPoint(@NotNull Point currentMousePoint)
	{
		final IGrid grid = getGrid();
		final int snapX = grid.snap(currentMousePoint.x);
		final int snapY = grid.snap(currentMousePoint.y);
		return new Point(snapX, snapY);
	}

	@Nullable @Override public Point getCurrentPlacementPoint()
	{
		return m_placementConfig.getCurrentPlacementPoint();
	}

	@Override public int getCurrentPlacementIdx()
	{
		return m_placementConfig.getCurrentPlacementIdx();
	}

	@Override public void moveToNewPlacementPoint(@Nullable Point currentPlacementPoint)
	{
		m_placementConfig.moveToNewPlacementPoint(currentPlacementPoint);
	}

	@Override public void setupPlacementIndex(int currentPlacementIndex)
	{
		m_placementConfig.setupPlacementIndex(currentPlacementIndex);
	}

	@NotNull @Override public IExtent computeTransientGraphicsBoundary()
	{
		final IExtent transGfxExtent = FactoryMgr.getCommonFactory().createExtent();
		final List<IDevicePlacementInfo> placementInfos = m_dataModel.getPlacementInfos();
		final int currentPlacementIdx = getCurrentPlacementIdx();
		for (int idx = currentPlacementIdx; idx < placementInfos.size(); ++idx) {
			final IDevicePlacementInfo devPlacementInfo = placementInfos.get(idx);
			transGfxExtent.addUnion(devPlacementInfo.getSnappedAbsExtent());
		}
		return transGfxExtent;
	}

	@Override public void terminate(boolean successful)
	{
		if (successful) {
			for (IDevicePlacementInfo commitedDevice : getCommitedDevices()) {
				commitedDevice.terminate();
			}
		}
		for (IDevicePlacementInfo placementInfo : m_dataModel.getPlacementInfos()) {
			placementInfo.cleanup();
		}
	}
}
