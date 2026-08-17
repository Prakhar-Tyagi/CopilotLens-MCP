/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.layout;

import chs.cof.COFTypeEnum;
import chs.cof.draw.IColor;
import chs.cof.draw.IGrid;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.dimension.DimensionControl;
import chs.cof.drawplus.dimension.IDimensionControl;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.ILogicOtherComponent;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.LogicOtherComponentTypeEnum;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemOtherComponentParameterized;
import chs.cofUtils.logical.concurrency.IDiagramRepresentationUpdater;
import chs.cofUtils.logical.concurrency.LoadAndLockDiagramRepresentationUpdateStrategy;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.common.IExtent;
import chs.common.IObjectFilter;
import chs.common.IParameterized;
import chs.common.IUID;
import chs.common.IUnit;
import chs.common.attr.custom.CustomAttributeConstants;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.DiagramHelper;
import chs.utility.SymbolUtils;
import chs.utility.attr.custom.CustomAttributesControl;
import chs.utility.gfx.TransformGfxHelper;
import chs.utility.helpers.GridHelper;
import chs.utility.helpers.IReplicate;
import chs.utility.helpers.LogHelper;
import chs.utility.helpers.SchemPinListHelper;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Dynamic phsical length and width updater for layout diagram's parameterized other componenets and devices
 */
public class ParameterizedPhysicalDimensionUpdater
{

	private static ParameterizedPhysicalDimensionUpdater instance = null;

	@NotNull public static synchronized ParameterizedPhysicalDimensionUpdater getInstance()
	{
		if (instance == null) {
			instance = new ParameterizedPhysicalDimensionUpdater();
		}
		return instance;
	}

	public void updatePhysicalDimensions(@NotNull ILogicDesign design, @NotNull Collection<IUID> modifiedObjs)
	{
		for (IUID changedObjectsUID : modifiedObjs) {
			ILogicObject logicObject = UIDMgr.getObjectOfType(changedObjectsUID, ILogicObject.class);
			if (logicObject != null) {
				updateParamPhysicalDimension(design, logicObject);
			}
		}
	}

	private void updateParamPhysicalDimension(@NotNull ILogicDesign design, @NotNull ILogicObject logicObject)
	{
		updateParamPhysicalDimension(design, logicObject, this::logErrorMessage);
	}

	private void logErrorMessage(@NotNull String msg)
	{
		LogHelper.appMsgSafe(HTMLHelper.color(IColor.RED, msg));
	}

	public void updateParamPhysicalDimension(@NotNull ILogicDesign design, @NotNull ILogicObject logicObject,
			@NotNull Consumer<String> messageConsumer)
	{
		final IPhysicalDimensionUpdater dimensionUpdater = getPhysicalDimensionUpdater(logicObject);
		if (dimensionUpdater == null) {
			return;
		}
		IDiagramRepresentationUpdater updateStrategy = new LoadAndLockDiagramRepresentationUpdateStrategy(design)
				.getDiagramProcessor(Collections.singleton(logicObject));
		updateStrategy.processDiagrams((diagram) -> {
			dimensionUpdater.adjustPhysicalDimensionOnDiagram(diagram, messageConsumer);
		});
	}

	@Nullable private IPhysicalDimensionUpdater getPhysicalDimensionUpdater(@NotNull ILogicObject logicObject)
	{
		if (logicObject instanceof ILogicOtherComponent) {
			final ILogicOtherComponent otherComp = (ILogicOtherComponent) logicObject;
			final LogicOtherComponentTypeEnum type = otherComp.getType();
			if (type.isKindOfLengthWiseObject()) {
				return new NonElectricalLengthAdjustibleDimensionUpdater(otherComp);
			}
			else {
				return new NonElectricalFixedPhysicalDimensionUpdater(otherComp);
			}
		}
		if (logicObject instanceof IDevice) {
			return new SchemDevicePhysicalDimensionUpdater((IDevice) logicObject);
		}
		return null;
	}

	private interface IPhysicalDimensionUpdater
	{

		void adjustPhysicalDimensionOnDiagram(@NotNull ISchemDiagram diagram,
				@NotNull Consumer<String> messageConsumer);
	}

	private abstract static class AbstractPhysicalDimensionUpdater implements IPhysicalDimensionUpdater
	{

		@NotNull protected final ILogicObject mLogicObject;

		private AbstractPhysicalDimensionUpdater(@NotNull ILogicObject logicObject)
		{
			mLogicObject = logicObject;
		}

		@NotNull protected ILogicObject getLogicObject()
		{
			return mLogicObject;
		}

		@Override public void adjustPhysicalDimensionOnDiagram(@NotNull ISchemDiagram diagram,
				@NotNull Consumer<String> messageConsumer)
		{

			final Set<IDiagramObject> candidatesToProcess = diagram.getRepresentations(getLogicObject().getUID())
					.stream()
					.filter(dObj -> isParameterized(dObj))
					.collect(Collectors.toSet());

			if (candidatesToProcess.isEmpty()) {
				return;
			}

			final CustomAttributesControl control = new CustomAttributesControl(mLogicObject);
			final double width = Double.parseDouble(control.getCustomAttributeValueAsNonNullString(
					CustomAttributeConstants.LayCompWidth.getName()));
			final double length = Double.parseDouble(control.getCustomAttributeValueAsNonNullString(
					CustomAttributeConstants.LayCompLength.getName()));
			final int worldCordWidth = toWorldCoord(diagram, width);
			final int worldCordLength = toWorldCoord(diagram, length);
			for (IDiagramObject diagramObject : candidatesToProcess) {
				final String errorMsg = checkValidDimensionValues(worldCordWidth, worldCordLength, diagram.getGrid());
				if (StringUtils.isBlank(errorMsg)) {
					adjustObjectDimension(diagramObject, worldCordWidth, worldCordLength, diagram);
				}
				else {
					final ILogicDesign design = diagram.getDesign();
					final String name = mLogicObject.getName();
					final String diagramObjectLink =
							design != null ? HTMLHelper.link(design, diagramObject, name) : name;
					final StringBuffer sb = new StringBuffer();
					final String errorMsgPrefix = ResourceMgr.getString(ParameterizedPhysicalDimensionUpdater.class,
							"ParameterizedPhysicalDimensionUpdater.Parameterized.updateDimensions.prefix",
							getCofType(mLogicObject), diagramObjectLink);
					sb.append(errorMsgPrefix);
					sb.append(StringUtils.SPACE);
					sb.append(errorMsg);
					messageConsumer.accept(sb.toString());
				}
			}
		}

		private int toWorldCoord(@NotNull ISchemDiagram diagram, double physicalVal)
		{
			return GridHelper.toWorldCoordFromPhysicalVal(physicalVal, diagram.getGrid(), diagram);
		}

		@NotNull private String getCofType(@NotNull ILogicObject logicObject)
		{
			return StringUtils.toLowerCase(COFTypeEnum.getDisplayableTypeName(logicObject));
		}

		/* If dimensions are not valid, return the error message key, otherwise empty string*/
		@NotNull protected String checkValidDimensionValues(int worldCordWidth, int worldCordLength,
				@NotNull IGrid grid)
		{
			return worldCordLength > 0 && worldCordWidth > 0 ? StringUtils.EMPTY_STRING : ResourceMgr
					.getString(ParameterizedPhysicalDimensionUpdater.class,
							"ParameterizedPhysicalDimensionUpdater.Parameterized.invalidDimensions.fail");
		}

		protected void adjustObjectDimension(@NotNull IDiagramObject diagramObject, int worldCordWidth,
				int worldCordLength, @NotNull ISchemDiagram diagram)
		{
			final IDimensionControl dimensionControl = diagramObject.getDimensionControl();
			if (dimensionControl != null) {
				final boolean widthChanged = setupWidth(dimensionControl, worldCordWidth);
				final boolean lengthChanged = setupLength(dimensionControl, worldCordLength);
				if (widthChanged || lengthChanged) {
					diagramObject.updateDimension(dimensionControl);
				}
			}
		}

		protected abstract boolean isParameterized(@NotNull IDiagramObject diagramObject);

		protected abstract boolean setupLength(@NotNull IDimensionControl dimensionControl, int worldCordLength);

		protected abstract boolean setupWidth(@NotNull IDimensionControl dimensionControl, int worldCordWidth);
	}

	private static class NonElectricalLengthAdjustibleDimensionUpdater extends AbstractPhysicalDimensionUpdater
	{

		private NonElectricalLengthAdjustibleDimensionUpdater(@NotNull ILogicOtherComponent otherComponent)
		{
			super(otherComponent);
		}

		@Override protected boolean isParameterized(@NotNull IDiagramObject diagramObject)
		{
			return diagramObject instanceof ISchemOtherComponentParameterized;
		}

		@Override protected boolean setupLength(@NotNull IDimensionControl dimensionControl, int worldCordLength)
		{
			final Double actualLength = dimensionControl.getValue(DimensionControl.DimensionType.Width);
			if (actualLength == null || actualLength > worldCordLength) {
				dimensionControl.setEntity(DimensionControl.DimensionType.Width, worldCordLength);
				return true;
			}
			return false;
		}

		@Override protected boolean setupWidth(@NotNull IDimensionControl dimensionControl, int worldCordWidth)
		{
			final Double curValue = dimensionControl.getValue(DimensionControl.DimensionType.Height);
			if (curValue == null || worldCordWidth != CommonUtils.toInteger(curValue)) {
				dimensionControl.setEntity(DimensionControl.DimensionType.Height, worldCordWidth);
				return true;
			}
			return false;
		}
	}

	private abstract static class FixedPhysicalDimensionUpdater extends AbstractPhysicalDimensionUpdater
	{

		private FixedPhysicalDimensionUpdater(@NotNull ILogicObject logicObject)
		{
			super(logicObject);
		}

		@Override protected boolean setupLength(@NotNull IDimensionControl dimensionControl, int worldCordLength)
		{
			final Double curValue = dimensionControl.getValue(DimensionControl.DimensionType.Width);
			if (curValue == null || worldCordLength != CommonUtils.toInteger(curValue)) {
				dimensionControl.setEntity(DimensionControl.DimensionType.Width, worldCordLength);
				return true;
			}
			return false;
		}

		@Override protected boolean setupWidth(@NotNull IDimensionControl dimensionControl, int worldCordWidth)
		{
			final Double curValue = dimensionControl.getValue(DimensionControl.DimensionType.Height);
			if (curValue == null || worldCordWidth != CommonUtils.toInteger(curValue)) {
				dimensionControl.setEntity(DimensionControl.DimensionType.Height, worldCordWidth);
				return true;
			}
			return false;
		}
	}

	private static class NonElectricalFixedPhysicalDimensionUpdater extends FixedPhysicalDimensionUpdater
	{

		private NonElectricalFixedPhysicalDimensionUpdater(@NotNull ILogicOtherComponent logicObject)
		{
			super(logicObject);
		}

		@Override protected boolean isParameterized(@NotNull IDiagramObject diagramObject)
		{
			return diagramObject instanceof ISchemOtherComponentParameterized;
		}
	}

	private static class SchemDevicePhysicalDimensionUpdater extends FixedPhysicalDimensionUpdater
	{

		private SchemDevicePhysicalDimensionUpdater(@NotNull IDevice logicObject)
		{
			super(logicObject);
		}

		@NotNull @Override
		protected String checkValidDimensionValues(int worldCordWidth, int worldCordLength, @NotNull IGrid grid)
		{
			final String errorMsg = super.checkValidDimensionValues(worldCordWidth, worldCordLength, grid);
			if (!errorMsg.isEmpty()) {
				return errorMsg;
			}
			final int gridSpacing = grid.getGridSpacing();
			if (worldCordLength < gridSpacing || worldCordWidth < 2 * gridSpacing ||
					worldCordLength % gridSpacing != 0 || worldCordWidth % gridSpacing != 0) {
				final IUnit realMapping = grid.getRealMapping();
				return ResourceMgr.getString(ParameterizedPhysicalDimensionUpdater.class,
						"ParameterizedPhysicalDimensionUpdater.ParameterizedDevice.multipleOfGrid.fail",
						toDisplayUnit(realMapping, 1), toDisplayUnit(realMapping, 2));
			}
			return StringUtils.EMPTY_STRING;
		}

		@NotNull private String toDisplayUnit(@Nullable IUnit unit, double multiple)
		{
			if (unit == null) {
				return StringUtils.EMPTY_STRING;
			}
			final double value = unit.getValue() * multiple;
			final String unitType = unit.getType().toDisplayString();
			return Double.toString(value) + StringUtils.SPACE + unitType;
		}

		@NotNull private IExtent determineResizedParameterizedExtent(@NotNull final IParameterized parameterized,
				@NotNull final IGrid grid, int worldCordLength, int worldCordWidth)
		{
			//always consider there is border. because with < 1 border also the graphics
			//is hazy and selection box always considers one grid border.
			//we will extend the box in all the direction.
			//this is a placeholder. the placeholder should be such
			//that it will fit the actual one without overflowing.
			final int gridSpacing = grid.getGridSpacing();
			int expParamLength = Math.max(gridSpacing, IGrid.snapToCeilGrid(worldCordLength, grid));
			final int borderAdustment = CommonUtils.toInteger(2 * gridSpacing);
			int expParamWidth = Math.max(0, (IGrid.snapToCeilGrid(worldCordWidth, grid) - borderAdustment));
			final IExtent parameterizedExtent = parameterized.getExtent();
			final int currentLength = parameterizedExtent.getWidth();
			final int currentWidth = parameterizedExtent.getHeight();
			int newExtX = parameterizedExtent.getX() - grid.snap((expParamLength - currentLength) / 2);
			int newExtY = parameterizedExtent.getY() - grid.snap((expParamWidth - currentWidth) / 2);
			return FactoryMgr.getCommonFactory().constructExtent(newExtX, newExtY, expParamLength, expParamWidth);
		}

		@Override protected void adjustObjectDimension(@NotNull IDiagramObject diagramObject, int worldCordWidth,
				int worldCordLength, @NotNull ISchemDiagram diagram)
		{
			if (diagramObject instanceof chs.cof.logical.schem.IPinList) {
				final chs.cof.logical.schem.IPinList pinlist = (chs.cof.logical.schem.IPinList) diagramObject;
				final IParameterized parameterized = pinlist.getParameterized();
				final IObjectFilter<chs.cof.logical.schem.IPinList> devConnFilter =
						p -> !(p.getConnectivity() instanceof IDeviceConnector);
				if (parameterized != null && pinlist.getPins().isEmpty() &&
						SchemPinListHelper.getAttachedSchemPinLists(pinlist, devConnFilter).isEmpty()) {
					//if there are pins. we would need separate mechanism to resize them.
					IExtent deviceGridExtent = determineResizedParameterizedExtent(parameterized,
							diagram.getGrid(), worldCordLength, worldCordWidth);
					GeneratorParameters gp = DiagramHelper.createGeneratorParameters(diagram);
					Generator generator = Generator.getGenerator();
					//need to recreate parameterized with new extent otherwise undo doesn't work.
					IParameterized replicatedParameterized = (IParameterized) ((IReplicate) parameterized).replicate();
					replicatedParameterized.setExtent(deviceGridExtent);
					pinlist.setParameterized(replicatedParameterized);
					generator.generateDevice(pinlist, gp, Generator.REGENERATE_PROPERTIES);
					TransformGfxHelper.updateEditedObjectAnchors(pinlist);
				}
			}
		}

		@Override protected boolean isParameterized(@NotNull IDiagramObject diagramObject)
		{
			return !SymbolUtils.isSymbolInstance((IPinList) getLogicObject(),
					(chs.cof.logical.schem.IPinList) diagramObject);
		}
	}
}