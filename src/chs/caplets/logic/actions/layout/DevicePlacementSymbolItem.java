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

import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caplets.logic.actions.AddInstanceAction;
import chs.caplets.logic.actions.CreateParameterizedObjectAction;
import chs.cof.draw.GfxDimEnum;
import chs.cof.draw.HorizJustificationEnum;
import chs.cof.draw.IColor;
import chs.cof.draw.IDrawFactory;
import chs.cof.draw.IGrid;
import chs.cof.draw.IText;
import chs.cof.draw.ITransform;
import chs.cof.draw.IWritableGfxAttribute;
import chs.cof.draw.LineStyle;
import chs.cof.draw.LogicalGraphicSize;
import chs.cof.draw.VertJustificationEnum;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.schem.ILayoutLogicDiagram;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.parts.ILibraryGraphic;
import chs.cof.project.IProject;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.SymbolTypeEnum;
import chs.cofUtils.cmd.CommandListener;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.common.ICommandListener;
import chs.common.ICommonFactory;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.IParameterized;
import chs.common.IUIDObject;
import chs.common.cmd.replacesymbol.ReplaceInstanceSymbolCmd;
import chs.common.cmd.replacesymbol.ReplaceInstanceSymbolOptions;
import chs.common.cmd.replacesymbol.ReplaceInstanceSymbolParams;
import chs.common.geom.GeometryUtils;
import chs.services.dynamicgfx.DynamicGfxFactoryHelper;
import chs.services.dynamicgfx.DynamicRotationIndicator;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.IDynamicGfxFactory;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.system.FactoryMgr;
import chs.utilities.CHSConstants;
import chs.utilities.CommonUtils;
import chs.utility.DiagramHelper;
import chs.utility.SymbolUtils;
import chs.utility.helpers.CoordinateHelper;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.ExtentHelper;
import chs.utility.helpers.TextHelper;
import chs.utility.helpers.UtilsHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author chandras on 19-10-2019.
 */
public class DevicePlacementSymbolItem implements IDevicePlacementItem
{

	private static final Color m_marginColor = Color.GRAY;
	private static final Color m_prohibitionColor = Color.RED;
	private static final Color m_multiSymColor = Color.PINK;
	private static final Color m_singleSymColor = Color.GREEN;
	private static final Color m_commitColor = Color.CYAN;

	private static final double SQRT_TWO = Math.sqrt(2.0);
	@NotNull private IDevicePlacementInfo m_placementController;
	@Nullable private ISymbolDef m_symbol;
	@Nullable private IPinList m_schematic;
	@NotNull private IDeviceGridExtent m_snappedExtent;
	@NotNull private Map<DeviceDecorationObjectType, IDynamicGfx> m_additionalDynGfx =
			new EnumMap<>(DeviceDecorationObjectType.class);
	@Nullable private IDeviceCommitInfo m_commitInfo = null;

	public DevicePlacementSymbolItem(@NotNull ISymbolDef symbol,
			@NotNull IDevicePlacementInfo placementController)
	{
		m_placementController = placementController;
		m_symbol = symbol;
		final IProject project = m_placementController.getProject();
		final double scale = getScale(m_symbol);
		m_schematic = SymbolUtils.createSchematicInstance(project, m_symbol, null, scale, true);
		if (!SymbolUtils.isUnitScale(scale)) {
			SymbolUtils.adjustOffGridPinsToAGridPoint(m_schematic, getGrid());
		}
		m_snappedExtent = new DeviceGridExtent(m_schematic.getNoTextExtent(), getGrid());
		generateDynamicGraphics();
	}

	public DevicePlacementSymbolItem(int paramLength, int paramWidth,
			@NotNull IDevicePlacementInfo placementController)
	{
		m_placementController = placementController;
		m_symbol = null;
		m_schematic = null;
		final IGrid grid = placementController.getGrid();
		//this is a placeholder. the placeholder should be such
		//that it will fit the actual one without overflowing.
		final int gridSpacing = grid.getGridSpacing();
		int expectedLength = Math.max(gridSpacing, IGrid.snapToCeilGrid(paramLength, grid));
		int expectedWidth = Math.max(2 * gridSpacing, IGrid.snapToCeilGrid(paramWidth, grid));
		final int snappedX = grid.snap(-expectedLength / 2);
		final int snappedY = grid.snap(-expectedWidth / 2);
		final ICommonFactory commonFactory = FactoryMgr.getCommonFactory();
		final IExtent extent = commonFactory.constructExtent(snappedX, snappedY, paramLength, paramWidth);
		m_snappedExtent = new DeviceGridExtent(extent, getGrid());
		generateDynamicGraphics();
	}

	@NotNull private IGrid getGrid()
	{
		return m_placementController.getGrid();
	}

	private double getScale(@NotNull ISymbolDef symbol)
	{
		boolean shouldScale = symbol.getSymbolType() == SymbolTypeEnum.COMMENT ||
				m_placementController.getDiagram() instanceof ILayoutLogicDiagram;
		return shouldScale ? SymbolUtils.getSymbolScale(symbol, getGrid()) : 1;
	}

	private void generateDynamicGraphics()
	{
		IDynamicGfxFactory factory = new DynamicGfxFactoryHelper(FactoryMgr.getDrawFactory());
		m_additionalDynGfx.put(DeviceDecorationObjectType.MAIN_COMPOUND_CONTAINER,
				factory.constructCompound(FactoryMgr.getCommonFactory().constructLocation(0, 0)));

		final IExtent snappedExtent = getSnappedExtentSqueezed(m_snappedExtent.getSnappedExtent(), false);
		m_additionalDynGfx.put(DeviceDecorationObjectType.SYM_GRID_TRUE_BOUNDARY,
				factory.constructRectangle(snappedExtent, false));

		m_additionalDynGfx.put(DeviceDecorationObjectType.SYM_GRID_MARGIN_BOUNDARY,
				factory.constructRectangle(m_placementController.getMarginExtent(snappedExtent), false));

		final Point center = new Point(snappedExtent.getCenterX(), snappedExtent.getCenterY());
		final int rectMin = Math.min(snappedExtent.getHeight(), snappedExtent.getWidth());
		final int radius = Math.max(rectMin, CHSConstants.PIN_SPACING) / 2;
		m_additionalDynGfx.put(DeviceDecorationObjectType.PROHIBITION_CIRCLE,
				factory.constructCircle(center, new Point(0, 0), radius, false));

		int shift = CommonUtils.toInteger(radius / SQRT_TWO);
		final Point backSlashPt1 = new Point(center.x - shift, center.y + shift);
		final Point backSlashPt2 = new Point(center.x + shift, center.y - shift);
		m_additionalDynGfx.put(DeviceDecorationObjectType.PROHIBITION_BACK_SLASH,
				factory.constructLine(backSlashPt1, backSlashPt2, new Point(0, 0), false));

		final Point fwdSlashPt1 = new Point(center.x - shift, center.y - shift);
		final Point fwdSlashPt2 = new Point(center.x + shift, center.y + shift);
		m_additionalDynGfx.put(DeviceDecorationObjectType.PROHIBITION_FWD_SLASH,
				factory.constructLine(fwdSlashPt1, fwdSlashPt2, new Point(0, 0), false));

		final int transientTextSize =
				IBasicDevicePlacementControl.computeTransientTextSize(m_placementController.getDiagram());
		final int x1 = snappedExtent.getCenterX() - transientTextSize;
		final int x2 = snappedExtent.getCenterX() + transientTextSize;
		final IGrid grid = getGrid();
		final int y = snappedExtent.getTop() +
				CommonUtils.toInteger(IDeviceGridExtent.ALIGN_OVERLAP_TOLERANCE * grid.getGridSpacing());
		final String deviceName = m_placementController.getDeviceName();
		final String symbolName =
				m_symbol != null ? (m_symbol.getContainerLibrary().getName() + " / " + m_symbol.getName()) : "";
		final IDynamicGfx devNameText = factory.constructText(createNameText(deviceName), false, false);
		devNameText.setLocation(FactoryMgr.getCommonFactory().constructLocation(x1, y));
		m_additionalDynGfx.put(DeviceDecorationObjectType.DEVICE_NAME_TEXT, devNameText);
		final IDynamicGfx symNameText = factory.constructText(createNameText(symbolName), false, false);
		symNameText.setLocation(FactoryMgr.getCommonFactory().constructLocation(x2, y));
		m_additionalDynGfx.put(DeviceDecorationObjectType.SYMBOL_NAME_TEXT, symNameText);

		//hide markers.
		for (IDynamicGfx dynamicGfx : m_additionalDynGfx.values()) {
			dynamicGfx.hideMarkers();
		}

		setupAsSnappedBoundary(m_additionalDynGfx.get(DeviceDecorationObjectType.SYM_GRID_TRUE_BOUNDARY));
		setupAsMarginBoundary(m_additionalDynGfx.get(DeviceDecorationObjectType.SYM_GRID_MARGIN_BOUNDARY));
		setupAsProhibition(m_additionalDynGfx.get(DeviceDecorationObjectType.PROHIBITION_CIRCLE));
		setupAsProhibition(m_additionalDynGfx.get(DeviceDecorationObjectType.PROHIBITION_BACK_SLASH));
		setupAsProhibition(m_additionalDynGfx.get(DeviceDecorationObjectType.PROHIBITION_FWD_SLASH));
	}

	@NotNull private IExtent getSnappedExtentSqueezed(@NotNull final IExtent snappedExtent, boolean verticalSqueeze)
	{
		if (!m_placementController.isSymbolPreviewMode()) {
			//there might be some real space in no. of grids (max would be ~2 grid) between the items
			//which can be squeezed to have better abutment resolution. round-off to no. of grids.
			int currentItemLeftInset = getPlacementItemInset(DeviceMarginSide.LEFT);
			int currentItemRightInset = getPlacementItemInset(DeviceMarginSide.RIGHT);
			int currentItemBottomtInset = verticalSqueeze ? getPlacementItemInset(DeviceMarginSide.BOTTOM) : 0;
			int currentItemTopInset = verticalSqueeze ? getPlacementItemInset(DeviceMarginSide.TOP) : 0;
			int squeezedExtentX = snappedExtent.getX() + currentItemLeftInset;
			int squeezedExtentY = snappedExtent.getY() + currentItemBottomtInset;
			int squeezedExtentW = snappedExtent.getWidth() - currentItemLeftInset - currentItemRightInset;
			int squeezedExtentH = snappedExtent.getHeight() - currentItemBottomtInset - currentItemTopInset;
			return FactoryMgr.getCommonFactory().constructExtent(squeezedExtentX, squeezedExtentY,
					squeezedExtentW, squeezedExtentH);
		}
		return snappedExtent;
	}

	@NotNull private IText createNameText(String valueText)
	{
		int rotation = GeometryUtils.TWO_SEVENTY_DEGREES;
		final int transientTextSize =
				IBasicDevicePlacementControl.computeTransientTextSize(m_placementController.getDiagram());
		final IText nameText = TextHelper.createTextForCurrentLocale(0, 0, transientTextSize, rotation, valueText);
		nameText.setHorizontalJustification(HorizJustificationEnum.JustLeft);
		nameText.setVerticalJustification(VertJustificationEnum.JustCenter);
		return nameText;
	}

	private void setupAsProhibition(@Nullable IDynamicGfx dynamicGfx)
	{
		if (dynamicGfx != null) {
			final IDrawFactory drawFactory = FactoryMgr.getDrawFactory();
			IWritableGfxAttribute gfxAttr = drawFactory.createGfxAttribute();
			gfxAttr.setLineStyle(LineStyle.SOLID);
			gfxAttr.setGfxDimType(GfxDimEnum.DIM_NONE);
			gfxAttr.setThickness(new LogicalGraphicSize(2));
			final IColor color = drawFactory.constructColorRGB(m_prohibitionColor.getRGB());
			gfxAttr.setColor(color);
			dynamicGfx.setAttribute(gfxAttr);
		}
	}

	private void setupAsSnappedBoundary(@Nullable IDynamicGfx dynamicGfx)
	{
		if (dynamicGfx != null) {
			final boolean unselectedSymbolPreview =
					!m_placementController.isSelectedForSymbolChange() && m_placementController.isSymbolPreviewMode();
			final GfxDimEnum dimType = unselectedSymbolPreview ? GfxDimEnum.DIM_COLOR : GfxDimEnum.DIM_NONE;
			final int thickness = unselectedSymbolPreview ? 1 : 2;
			final IDrawFactory drawFactory = FactoryMgr.getDrawFactory();
			IWritableGfxAttribute gfxAttr = drawFactory.createGfxAttribute();
			gfxAttr.setLineStyle(LineStyle.SOLID);
			gfxAttr.setGfxDimType(dimType);
			gfxAttr.setThickness(new LogicalGraphicSize(thickness));
			gfxAttr.setColor(determineColorForTrueSnapBoundary());
			dynamicGfx.setAttribute(gfxAttr);
		}
	}

	private IColor determineColorForTrueSnapBoundary()
	{
		boolean multiSymbol = m_placementController.isMultiSymbol();
		Color awtColor = multiSymbol ? m_multiSymColor : m_singleSymColor;
		if (isCommited()) {
			awtColor = m_commitColor;
		}
		return FactoryMgr.getDrawFactory().constructColorRGB(awtColor.getRGB());
	}

	private void setupAsMarginBoundary(@Nullable IDynamicGfx dynamicGfx)
	{
		if (dynamicGfx != null) {
			final IDrawFactory drawFactory = FactoryMgr.getDrawFactory();
			IWritableGfxAttribute gfxAttr = drawFactory.createGfxAttribute();
			gfxAttr.setLineStyle(LineStyle.DOTTED);
			gfxAttr.setGfxDimType(GfxDimEnum.DIM_NONE);
			gfxAttr.setThickness(new LogicalGraphicSize(1));
			final IColor color = drawFactory.constructColorRGB(m_marginColor.getRGB());
			gfxAttr.setColor(color);
			dynamicGfx.setAttribute(gfxAttr);
		}
	}

	private void setupTrueSymbolGridBoundary()
	{
		final IDynamicGfx trueGridBoundary = m_additionalDynGfx.get(DeviceDecorationObjectType.SYM_GRID_TRUE_BOUNDARY);
		if (trueGridBoundary != null) {
			setupAsSnappedBoundary(trueGridBoundary);
		}
	}

	@Override @Nullable public ISymbolDef getSymbol()
	{
		return m_symbol;
	}

	@Override @Nullable public IPinList getSchematic()
	{
		return m_schematic;
	}

	@NotNull public IExtent getSnappedAbsExtent()
	{
		return computeAbsExtent(m_snappedExtent.getSnappedExtent());
	}

	@NotNull private IExtent computeAbsExtent(@NotNull IExtent snappedExtent)
	{
		final IDynamicGfx mainContainer = m_additionalDynGfx.get(DeviceDecorationObjectType.MAIN_COMPOUND_CONTAINER);
		if (mainContainer != null) {
			return ExtentHelper.getAbsExtent(mainContainer, snappedExtent);
		}
		return snappedExtent;
	}

	@NotNull public IExtent getSnappedAbsExtentSqueezed()
	{
		return computeAbsExtent(getSnappedExtentSqueezed(m_snappedExtent.getSnappedExtent(), true));
	}

	@NotNull public IExtent getSnappedExtent()
	{
		return m_snappedExtent.getSnappedExtent();
	}

	@Override public boolean isSymbolPreviewMode()
	{
		return m_placementController.isSymbolPreviewMode();
	}

	@Override public void collectSymbolPinInformation(@NotNull BiConsumer<String, Point> pinInfoCollector)
	{
		final IDynamicGfx mainContainer = m_additionalDynGfx.get(DeviceDecorationObjectType.MAIN_COMPOUND_CONTAINER);
		if (mainContainer != null && m_schematic != null) {
			for (IPin pin : m_schematic.getObjects(IPin.class)) {
				final ILocation location = pin.getLocation();
				final ILocation absGfxLocation =
						CoordinateHelper.getAbsGfxLocation(mainContainer, location.getX(), location.getY());
				pinInfoCollector.accept(pin.getConnectivity().getName(),
						new Point(absGfxLocation.getX(), absGfxLocation.getY()));
			}
		}
	}

	@Override public void decorateTrailingItem(@NotNull DevicePlacementMode mode)
	{
		final IDynamicGfx trueGridBoundary = m_additionalDynGfx.get(DeviceDecorationObjectType.SYM_GRID_TRUE_BOUNDARY);
		if (trueGridBoundary != null && !m_placementController.isSymbolPreviewMode()) {
			final IDrawFactory drawFactory = FactoryMgr.getDrawFactory();
			IWritableGfxAttribute gfxAttr = drawFactory.createGfxAttribute();
			gfxAttr.setLineStyle(LineStyle.SOLID);
			switch (mode) {
				case MANUAL:
					gfxAttr.setGfxDimType(GfxDimEnum.DIM_GRAY);
					gfxAttr.setThickness(new LogicalGraphicSize(1));
					break;
				case AUTO:
					gfxAttr.setGfxDimType(GfxDimEnum.DIM_COLOR);
					gfxAttr.setThickness(new LogicalGraphicSize(1));
					break;
				case GROUP:
					gfxAttr.setGfxDimType(GfxDimEnum.DIM_NONE);
					gfxAttr.setThickness(new LogicalGraphicSize(2));
					break;
			}
			gfxAttr.setColor(determineColorForTrueSnapBoundary());
			trueGridBoundary.setAttribute(gfxAttr);
		}
	}

	@Override public void terminate(@NotNull IDevice device)
	{
		final IDynamicGfx mainContainer = m_additionalDynGfx.get(DeviceDecorationObjectType.MAIN_COMPOUND_CONTAINER);
		final ISymbolDef symbolDef = m_symbol;
		final boolean pinsMatching =
				symbolDef == null || DeviceLayoutHelper.deviceHasAllSymbolPinsMatching(device, symbolDef);
		if (mainContainer != null && pinsMatching) {
			final ISchemDiagram diagram = m_placementController.getDiagram();
			final IExtent snappedExtent = m_snappedExtent.getSnappedExtent();
			final ILocation devLoc = mainContainer.getLocation();
			final int x = devLoc.getX();
			final int y = devLoc.getY();
			Point lbPt = new Point(x + snappedExtent.getLeft(), y + snappedExtent.getBottom());
			Point rtPt = new Point(x + snappedExtent.getRight(), y + snappedExtent.getTop());
			final IPinList schemDevice = CreateParameterizedObjectAction.createSchemPinList(diagram, device,
					lbPt, rtPt, false, new DynamicRotationIndicator(true), "device");
			//reset the location. the above method shifts the location adjusting border.
			schemDevice.setLocation(devLoc);
			schemDevice.setTransform(mainContainer.getTransform());
			if (symbolDef != null) {
				//need to create temporary schem pins otherwise
				//new pins are getting created on the connectivity.
				//replace symbol pin-mapping works on instance pins? LOGIC-11226
				for (IAbstractPin devicePin : device.getPins()) {
					IPin pin = FactoryMgr.getSchemFactory().createPin(FactoryMgr.createUID(), devicePin);
					schemDevice.addObject(pin);
					pin.setParent(schemDevice);
				}
				final ReplaceInstanceSymbolCmd replaceInstanceSymbolCmd =
						new ReplaceInstanceSymbolCmd(new CAFCommandHelper(), ConductorRouteAction.getInstance());
				ReplaceInstanceSymbolParams param = new ReplaceInstanceSymbolParams(diagram, schemDevice.getUID());
				param.setSymbolRef(UtilsHelper.getCHSUtils().getSymbolFactory().constructSymbolRef(symbolDef));
				// set the pin mapping and options via callback so clients of the command can reuse this code
				replaceInstanceSymbolCmd.initPinMapping(param, true, ILibraryGraphic.ContextType.ELECTRICAL);
				//LOGIC-11222. LOGIC-11233:we don't want attributes/properties from symbol?
				//otherwise its overriding the includeOnBom also.
				ReplaceInstanceSymbolOptions options = new ReplaceInstanceSymbolOptions(false, false,
						false, false, false, false, false);
				param.setOptions(options);
				//don't need output messages about replacement symbol.
				ICommandListener cmdListener = new CommandListener();
				replaceInstanceSymbolCmd.setCommandListener(cmdListener);
				replaceInstanceSymbolCmd.setParams(Collections.singletonList(param));
				replaceInstanceSymbolCmd.execute();
			}
			else {
				//take the new extent. this would have taken care of borders also.
				final IParameterized parameterized = schemDevice.getParameterized();
				assert parameterized != null;
				final IExtent parameterizedExtent = parameterized.getExtent();
				final int extWidth = parameterizedExtent.getWidth();
				final int extHheight = parameterizedExtent.getHeight();
				final ICommonFactory commonFactory = FactoryMgr.getCommonFactory();
				IParameterized params = commonFactory.createParameterized();
				Generator generator = Generator.getGenerator();
				GeneratorParameters gp = DiagramHelper.createGeneratorParameters(diagram);
				final IGrid grid = getGrid();
				final int snappedX = grid.snap(-extWidth / 2);
				final int snappedY = grid.snap(-extHheight / 2);
				params.setExtent(commonFactory.constructExtent(snappedX, snappedY, extWidth, extHheight));
				schemDevice.setParameterized(params);
				generator.generateDevice(schemDevice, gp, Generator.REGENERATE_PROPERTIES);
			}
			AddInstanceAction.completePostAction(schemDevice, diagram);
		}
	}

	@Override public void flipped(@NotNull IDynamicGfxService dynamicGfxService)
	{
		//since the decorations are being added as child to main container and main container handles flip.
		//we actually don't neeed to do anything extra here. things would work automatically.
	}

	@Override public void cleanup()
	{
		if (m_schematic != null) {
			Consumer<IUIDObject> clearObject = p -> {
				CreationDeletionHelper.getTheCreationHelper().removeCreationObject(p);
				p.delete();
			};
			//first delete schematic and then connectivity. store connectivity before deletion.
			final chs.cof.logical.cable.IPinList connDevice = m_schematic.getConnectivity();
			clearObject.accept(m_schematic);
			if (connDevice != null) {
				clearObject.accept(connDevice);
			}
		}
	}

	@Override public void resetTransientGraphics(@NotNull IDynamicGfxService dynamicGfxService)
	{
		final IDynamicGfx mainContainer = m_additionalDynGfx.get(DeviceDecorationObjectType.MAIN_COMPOUND_CONTAINER);
		if (mainContainer != null) {
			dynamicGfxService.removeTransientGfx(mainContainer);
		}
	}

	@Override public void collectTransientGraphics(@NotNull IDynamicGfxService dynamicGfxService)
	{
		final IDynamicGfx mainContainer = m_additionalDynGfx.get(DeviceDecorationObjectType.MAIN_COMPOUND_CONTAINER);
		if (mainContainer != null) {
			dynamicGfxService.addTransientGfx(mainContainer);
		}
	}

	@Override public void regenerateTransientGraphics(@NotNull Point placementPoint)
	{
		//first update the color depending upon the state of device.
		setupTrueSymbolGridBoundary();
		final IDynamicGfx mainContainer = m_additionalDynGfx.get(DeviceDecorationObjectType.MAIN_COMPOUND_CONTAINER);
		if (mainContainer != null) {
			mainContainer.removeAllObjects();
			if (m_schematic != null) {
				mainContainer.addObject(m_schematic, false);
			}
			applyLocationAndTransform(placementPoint, mainContainer);
			//update with decoration only after computation of final location and transform.
			//because the validity check might use that information in decision making.
			updateWithDecorations(mainContainer);
		}
	}

	protected void updateWithDecorations(IDynamicGfx mainContainer)
	{
		List<DeviceDecorationObjectType> decorations = new ArrayList<>(4);
		decorations.add(DeviceDecorationObjectType.SYM_GRID_TRUE_BOUNDARY);
		decorations.add(DeviceDecorationObjectType.DEVICE_NAME_TEXT);
		decorations.add(DeviceDecorationObjectType.SYMBOL_NAME_TEXT);
		if (!isCommited() && !isSymbolPreviewMode()) {
			final IExtent snappedExtent = m_snappedExtent.getSnappedExtent();
			if (!m_placementController.getMarginExtent(snappedExtent).isEquivalent(snappedExtent)) {
				decorations.add(DeviceDecorationObjectType.SYM_GRID_MARGIN_BOUNDARY);
			}
			if (!m_placementController.isValidPlacement(this)) {
				decorations.add(DeviceDecorationObjectType.PROHIBITION_CIRCLE);
				decorations.add(DeviceDecorationObjectType.PROHIBITION_FWD_SLASH);
				decorations.add(DeviceDecorationObjectType.PROHIBITION_BACK_SLASH);
			}
		}

		for (DeviceDecorationObjectType decoration : decorations) {
			final IDynamicGfx decDynGfx = m_additionalDynGfx.get(decoration);
			if (decDynGfx != null) {
				mainContainer.addObject(decDynGfx, false);
			}
		}
		mainContainer.updateDynamicExtent();
	}

	private void applyLocationAndTransform(@NotNull Point placementPoint, IDynamicGfx mainContainer)
	{
		HorizJustificationEnum hJust = m_placementController.getHorizontalJustification();
		VertJustificationEnum vJust = m_placementController.getVerticalJustification();
		PlacementAxisRotation axisRotation = m_placementController.getPlacementRotation();

		final int lMargin = m_snappedExtent.getLeft();
		final int rMargin = m_snappedExtent.getRight();
		final int tMargin = m_snappedExtent.getTop();
		final int bMargin = m_snappedExtent.getBottom();
		final int centerX = m_snappedExtent.getCenterX();
		final int centerY = m_snappedExtent.getCenterY();

		int relX = centerX;
		if (HorizJustificationEnum.JustLeft.equals(hJust)) {
			relX = lMargin;
		}
		else if (HorizJustificationEnum.JustRight.equals(hJust)) {
			relX = rMargin;
		}

		int relY = centerY;
		if (VertJustificationEnum.JustTop.equals(vJust)) {
			relY = tMargin;
		}
		else if (VertJustificationEnum.JustBottom.equals(vJust)) {
			relY = bMargin;
		}

		AffineTransform affineTransform = axisRotation.getTransform();
		final Point ptTransAnchor = new Point(relX, relY);
		if (m_placementController.isFlipped()) {
			//the flip is equivalent to rotate by 180 degree w.r.t true center of the grid box.
			//this is equivalent to rotate the anchor w.r.t true center by 180 degrees.
			//the true center would not necessarily be on grid (could be at half grid off).
			//however, this would ensure that the location of device is on grid.
			//this equation would take care of middle-center justification also.
			affineTransform.rotate(Math.PI);
			ptTransAnchor.x = lMargin + rMargin - relX;
			ptTransAnchor.y = tMargin + bMargin - relY;
		}
		affineTransform.transform(ptTransAnchor, ptTransAnchor);

		int absX = placementPoint.x - ptTransAnchor.x;
		int absY = placementPoint.y - ptTransAnchor.y;

		if (m_placementController.isOriginAligned()) {
			absX = placementPoint.x;
			absY = placementPoint.y;
		}

		mainContainer.setLocation(FactoryMgr.getCommonFactory().constructLocation(absX, absY));
		final ITransform transform = FactoryMgr.getDrawFactory().createTransform();
		transform.setFromAffineTransform(affineTransform);
		mainContainer.setTransform(transform);
	}

	@Override public int getPlacementItemSideGap(@NotNull DeviceMarginSide side)
	{
		return m_snappedExtent.getPlacementItemSideGap(side);
	}

	@Override public int getPlacementItemInset(@NotNull DeviceMarginSide side)
	{
		return m_snappedExtent.getPlacementItemInset(side);
	}

	@Override public void commit(@NotNull Point commitPoint)
	{
		final IDynamicGfx mainContainer = m_additionalDynGfx.get(DeviceDecorationObjectType.MAIN_COMPOUND_CONTAINER);
		if (mainContainer != null) {
			m_commitInfo = new DeviceCommitInfo(mainContainer.getLocation(), mainContainer.getTransform());
		}
		//this will remove some unwanted graphics.
		regenerateTransientGraphics(commitPoint);
	}

	@Override public boolean isCommited()
	{
		return m_commitInfo != null;
	}

	@Override public void undoCommit()
	{
		if (m_commitInfo != null) {
			final Point commitPoint = m_commitInfo.getLocation();
			m_commitInfo = null;
			//this will remove some unwanted graphics.
			regenerateTransientGraphics(commitPoint);
		}
	}
}
