/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.creation.CreateByMultipointAction;
import chs.cof.draw.HorizJustificationEnum;
import chs.cof.draw.IColor;
import chs.cof.draw.IDrawFactory;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGrid;
import chs.cof.draw.ILine;
import chs.cof.draw.IWritableGfxAttribute;
import chs.cof.draw.LineStyle;
import chs.cof.draw.VertJustificationEnum;
import chs.cof.drawplus.IAnchorable;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.dimension.IDimensionable;
import chs.cof.harness.diagram.DimensionType;
import chs.cof.logical.schem.ILayoutXYDimension;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.GeneratorStyle;
import chs.common.DirectionEnum;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.IParameterized;
import chs.common.IProjectPreferenceMgr;
import chs.common.IUnit;
import chs.common.Location;
import chs.common.configurations.IEntityConfiguration;
import chs.common.preferencesets.IPreferenceSet;
import chs.common.preferencesets.IReadOnlyStyleSetInfoHolder;
import chs.common.preferencesets.ObjectTypeEnum;
import chs.common.styles.IBaseAxisDimensionShapeStyle;
import chs.common.styles.IShapeStyle;
import chs.common.styles.IStyleXMLKeys;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.IDynamicGfxFactory;
import chs.services.dynamicgfx.IDynamicGfxMediator;
import chs.services.dynamicgfx.IDynamicSnap;
import chs.services.dynamicgfx.ISmartPoint;
import chs.system.FactoryMgr;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.DiagramHelper;
import chs.utility.GfxObjectUtils;
import chs.utility.GfxUtils;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.CoordinateHelper;
import chs.utility.helpers.ExtentHelper;
import chs.utility.helpers.GridHelper;
import chs.utility.preferences.PreferenceSetHelper;
import chs.utility.topology.utils.CalcUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class AbstractAxisDimensionAction extends CreateByMultipointAction
{

	private static final int STATE_START_DIMENSIONABLE = 0;
	private static final int STATE_OTHER_DIMENSIONABLE = 1;
	private static final int STATE_PLACE_OR_CONTINUE = 2;
	private int baselineOffset = 0;
	@NotNull private Cursor mDimensionCursor;
	@NotNull private List<IGfxObject> mTransientAxisDimension = new ArrayList<>(2);
	@NotNull private DimensionMode mDimensionMode = DimensionMode.Continued;

	private enum DimensionMode
	{
		Continued,
		Baseline
	}

	protected AbstractAxisDimensionAction(ICapletController controller)
	{
		super(controller, false, false);
		mDimensionCursor = CAFUtils.getInstance()
				.loadCursorDirect(getClass(), "chs/images/app/cur_dimension.gif", new Point(7, 7));
	}

	private void initAction()
	{
		mDimensionMode = DimensionMode.Continued;
		mTransientAxisDimension.clear();
		final IProject currentProject = getCurrentProject();
		if (currentProject != null) {
			final IProjectPreferenceMgr preferences = currentProject.getPreferences();
			final double layoutBaseDimensionGap = preferences.getLayoutBaseDimensionGap();
			baselineOffset = Math.toIntExact(Math.round(layoutBaseDimensionGap * IGrid.GRID_SIZE));
		}
	}

	@Override public void mouseMoved(MouseEvent e)
	{
		super.mouseMoved(e);
		updateTransientGraphics();
	}

	@Override public void mouseReleased(MouseEvent e)
	{
		if (e.getClickCount() == 1 && getSnapHelper().getLastSnapped() == null) {
			backup();
		}
		else {
			final int currentState = getCurrentState();
			if (currentState == STATE_OTHER_DIMENSIONABLE) {
				onSelectStartDimensionable(e.isControlDown());
			}
		}
		updateStatusbarText();
		super.mouseReleased(e);
	}

	protected boolean shouldShowMouseSnap()
	{
		return false;
	}

	private void onSelectStartDimensionable(boolean isControlDown)
	{
		mDimensionMode = isControlDown ? DimensionMode.Baseline : DimensionMode.Continued;
	}

	@Override public String getStatusbarText()
	{
		final int currentState = getCurrentState();
		if (currentState == STATE_START_DIMENSIONABLE) {
			return ResourceMgr
					.getString(AbstractAxisDimensionAction.class, "AbstractAxisDimensionAction.status.selectStart");
		}
		if (currentState == STATE_OTHER_DIMENSIONABLE) {
			return ResourceMgr
					.getString(AbstractAxisDimensionAction.class, "AbstractAxisDimensionAction.status.selectOtherEnd");
		}
		if (currentState >= STATE_PLACE_OR_CONTINUE) {
			return ResourceMgr
					.getString(AbstractAxisDimensionAction.class, "AbstractAxisDimensionAction.status.placeOrContinue");
		}
		return StringUtils.EMPTY_STRING;
	}

	private int getCurrentState()
	{
		return m_point_list != null ? m_point_list.size() : 0;
	}

	@Override protected boolean allowDuplicateMediators(IDynamicSnap dynSnap)
	{
		return true;
	}

	@Override public boolean checkSnap(@Nullable IDynamicSnap dynSnap)
	{
		if (dynSnap == null) {
			return false;
		}
		if (!hasDimensionableMediator(dynSnap)) {
			return false;
		}
		return super.checkSnap(dynSnap);
	}

	private boolean hasDimensionableMediator(@NotNull IDynamicSnap dynSnap)
	{
		return getDimensionable(dynSnap) != null;
	}

	@Override protected boolean validPointList(Collection<ISmartPoint> col)
	{
		if (!super.validPointList(col)) {
			return false;
		}
		return getSnapHelper().getAllSnapped().size() >= m_point_list.size();
	}

	@Nullable @Override protected IGfxObject constructDisplayObject(List<ISmartPoint> point_list)
	{
		return null;
	}

	protected List<? extends IGfxObject> constructDisplayObjects(List<ISmartPoint> point_list)
	{
		final Map<Point, IDynamicSnap> dynamicSnapMap = new HashMap<>();
		for (Pair<IDynamicSnap, Integer> snapIntegerPair : getSnapHelper().getAllSnapped()) {
			final IDynamicSnap dynamicSnap = snapIntegerPair.getFirst();
			dynamicSnapMap.put(new Point(dynamicSnap.getPoint()), dynamicSnap);
		}
		List<IGfxObject> displayObjects = new ArrayList<IGfxObject>();
		for (int i = 0; i < point_list.size() - 1; i++) {
			final int startIndex = mDimensionMode == DimensionMode.Baseline ? 0 : i;
			final Point firstPoint = point_list.get(startIndex).getAbsoluteLocation();
			final Point secondPoint = point_list.get(i + 1).getAbsoluteLocation();
			int offsetSum = mDimensionMode == DimensionMode.Baseline ? i * baselineOffset : 0;
			final IDimensionable startHook = getDimensionable(dynamicSnapMap.get(firstPoint));
			final IDimensionable endHook = getDimensionable(dynamicSnapMap.get(secondPoint));
			if (startHook != null && endHook != null) {
				final IGfxObject gfxObject =
						constructDisplayObject(firstPoint, secondPoint, startHook, endHook, offsetSum);
				if (gfxObject != null) {
					displayObjects.add(gfxObject);
				}
			}
		}

		return displayObjects;
	}

	@Nullable private IDimensionable getDimensionable(@Nullable IDynamicSnap dynamicSnap)
	{
		if (dynamicSnap == null) {
			return null;
		}
		Iterator<IDynamicGfxMediator> iter = dynamicSnap.getMediators();
		while (iter.hasNext()) {
			IDynamicGfxMediator mediator = iter.next();
			if (mediator instanceof IDimensionable) {
				return (IDimensionable) mediator;
			}
		}
		return null;
	}

	@Nullable private IGfxObject constructDisplayObject(@NotNull Point startPoint, @NotNull Point endPoint,
			@NotNull IDimensionable startDimensionable, @NotNull IDimensionable endDimensionable, int offsetOffset)
	{
		final ISchemDiagram diagram = (ISchemDiagram) getModel().getSheet();
		final ILayoutXYDimension xyDimension =
				createXYDimension(startPoint, endPoint, startDimensionable, endDimensionable, diagram, offsetOffset);

		GeneratorParameters gp = DiagramHelper.createGeneratorParameters(diagram);
		gp.setNewObject(true);

		boolean dimensionGenerated =
				Generator.getGenerator().generateLayoutDimension(xyDimension, gp, Generator.REGENERATE_PROPERTIES);

		if (!dimensionGenerated) {
			xyDimension.delete();
			diagram.removeObject(xyDimension);
			return null;
		}
		return xyDimension;
	}

	@NotNull private ILayoutXYDimension createXYDimension(@NotNull Point startPoint, @NotNull Point endPoint,
			@NotNull IDimensionable startDimensionable, @NotNull IDimensionable endDimensionable,
			@NotNull ISchemDiagram diagram, int offsetOffset)
	{
		final Location startLocation = new Location(startPoint);
		final Location endLocation = new Location(endPoint);
		final Point offsetPoint = m_current_point.getAbsoluteLocation();
		final Pair<Integer, DirectionEnum> offsetDirectionPair =
				calculateOffset(startLocation, endLocation, offsetPoint);
		int offset = offsetDirectionPair.getFirst();
		offset += offsetOffset;
		final ILayoutXYDimension logicXYDimension =
				createLogicXYDimension(startPoint, endPoint, startDimensionable, endDimensionable, offset, diagram,
						getDimensionType());
		DirectionEnum direction = offsetDirectionPair.getSecond();
		logicXYDimension.setDirection(direction);
		return logicXYDimension;
	}

	@NotNull private ILayoutXYDimension createLogicXYDimension(@NotNull Point startPoint, @NotNull Point endPoint,
			@NotNull IDimensionable startHook, @NotNull IDimensionable endHook,
			double offset, ISchemDiagram diagram, DimensionType type)
	{

		final Pair<Double, Double> startProportionalOffsets = determineProportionalOffsets(startPoint, startHook);
		final Pair<Double, Double> endProportionalOffsets = determineProportionalOffsets(endPoint, endHook);

		final ILayoutXYDimension xyDimension = FactoryMgr.getSchemFactory()
				.createXYDimension(FactoryMgr.createUID(), startHook, endHook, type, offset,
						startProportionalOffsets.getFirst(), startProportionalOffsets.getSecond(),
						endProportionalOffsets.getFirst(), endProportionalOffsets.getSecond());

		IParameterized params = FactoryMgr.getCommonFactory().createParameterized();

		Generator generator = Generator.getGenerator();
		GeneratorStyle gs = generator.getStyle();
		gs.addDefaults(params, IStyleXMLKeys.LAYOUT_AXIS_DIMENSION);
		xyDimension.setParameterized(params);

		diagram.addObject(xyDimension);
		xyDimension.initDELOffsets();
		return xyDimension;
	}

	@NotNull private Pair<Double, Double> determineProportionalOffsets(@NotNull Point point,
			@NotNull IDimensionable dimensionable)
	{
		final ILocation relPoint = CoordinateHelper.getRelativeLocation(dimensionable, point.x, point.y);
		final ILocation absLeftBottom = dimensionable
				.getAbsAnchorableLocation(HorizJustificationEnum.JustLeft, VertJustificationEnum.JustBottom);
		final ILocation relLeftBottom =
				CoordinateHelper.getRelativeLocation(dimensionable, absLeftBottom.getX(), absLeftBottom.getY());
		final IExtent startHookExt = ExtentHelper.getFilteredeExtent(dimensionable, IAnchorable.NODECORATIVE_FILTER);
		double startX = startHookExt.getWidth() == 0 ? 0 :
				((double) (relPoint.getX() - relLeftBottom.getX())) / startHookExt.getWidth();
		double startY = startHookExt.getHeight() == 0 ? 0 :
				((double) (relPoint.getY() - relLeftBottom.getY())) / startHookExt.getHeight();
		return new Pair<>(startX, startY);
	}

	@Override protected IDynamicGfx constructDynGfx(Point ref_point)
	{
		Collection<Point> vec = Collections.singleton(ref_point);
		final IDynamicGfx dynamicGfx =
				getDynamicGfxService().getFactory().constructPolyline(vec, new Point(0, 0), true, false);
		dynamicGfx.setMarkedVisible(false);
		return dynamicGfx;
	}

	@Override protected Class<?> snappingSource()
	{
		return ILayoutXYDimension.class;
	}

	@Override public Cursor getCursor()
	{
		return mDimensionCursor;
	}

	@Override public IActionEnum onActivate(ActionEvent e)
	{
		initAction();
		final IActionEnum actionEnum = super.onActivate(e);
		return actionEnum;
	}

	private void updateTransientGraphics()
	{
		clearTransientGraphic();
		if (m_point_list.isEmpty()) {
			return;
		}
		final List<ISmartPoint> tempPointList = new ArrayList<>(m_point_list);
		final boolean mouseAtValidSnap = m_snapHelper.getLastSnapped() != null;
		if (mouseAtValidSnap) {
			tempPointList.add(m_current_point);
		}
		for (int i = 0; i < tempPointList.size() - 1; i++) {
			final Point firstPoint =
					mDimensionMode == DimensionMode.Baseline ? tempPointList.get(0).getAbsoluteLocation() :
							tempPointList.get(i).getAbsoluteLocation();
			final Point secondPoint = tempPointList.get(i + 1).getAbsoluteLocation();
			final int offsetSum = mDimensionMode == DimensionMode.Baseline ? i * baselineOffset : 0;
			final List<IGfxObject> transientGfx =
					createTransientDimension(firstPoint, secondPoint, m_current_point.getAbsoluteLocation(), offsetSum);
			addTransientGfx(transientGfx);
		}
		if ((tempPointList.size() < 2 && !mouseAtValidSnap)) {
			final int size = tempPointList.size();
			final IDynamicGfxFactory factory = getDynamicGfxService().getFactory();
			final IDynamicGfx dottedLine = factory
					.constructLine(tempPointList.get(size - 1).getAbsoluteLocation(),
							m_current_point.getAbsoluteLocation(), new Point(0, 0), false);
			final IWritableGfxAttribute lineAttr =
					FactoryMgr.getDrawFactory().constructGfxAttribute(dottedLine.getAttribute());
			lineAttr.setLineStyle(LineStyle.DASHED);
			dottedLine.setAttribute(lineAttr);
			addTransientGfx(dottedLine);
		}
		invalidateTransientView();
	}

	private void addTransientGfx(@NotNull List<IGfxObject> transientGfx)
	{
		for (IGfxObject gfx : transientGfx) {
			addTransientGfx(gfx);
		}
	}

	private void addTransientGfx(@NotNull IGfxObject gfx)
	{
		final IDrawFactory drawFactory = FactoryMgr.getDrawFactory();
		final IWritableGfxAttribute attr = drawFactory.constructGfxAttribute(gfx.getAttribute());
		final Color magenta = Color.MAGENTA;
		final IColor color = drawFactory.constructColorRGB(magenta.getRed(), magenta.getGreen(), magenta.getBlue());
		attr.setColor(color);
		attr.setThickness(drawFactory.constructGraphicSize(1, null));
		attr.setLineStyle(LineStyle.SOLID);
		gfx.setAttribute(attr);
		mTransientAxisDimension.add(gfx);
		getDynamicGfxService().addTransientGfx(gfx);
	}

	@NotNull
	private List<IGfxObject> createTransientDimension(@NotNull Point firstPoint, @NotNull Point secondPoint,
			@NotNull Point offsetPoint, int offsetOffset)
	{
		final Location startLocation = new Location(firstPoint);
		final Location endLocation = new Location(secondPoint);
		final Pair<Integer, DirectionEnum> offsetDirectionPair =
				calculateOffset(startLocation, endLocation, offsetPoint);
		final DirectionEnum direction = offsetDirectionPair.getSecond();
		int offset = offsetDirectionPair.getFirst() + offsetOffset;
		offset = direction == DirectionEnum.ANTI_CLOCK_WISE ? offset : -offset;

		final IBaseDiagram baseDiagram = getBaseDiagram();
		if (baseDiagram instanceof ISchemDiagram) {
			double valueToToggleArrowGfx = 0;
			final IPreferenceSet preferenceSet = ((IReadOnlyStyleSetInfoHolder) baseDiagram).getPreferenceSet();
			if (preferenceSet != null) {
				final IEntityConfiguration entityConfiguration =
						preferenceSet.getEntityConfiguration(ObjectTypeEnum.LayoutAxisDimension);
				if (entityConfiguration != null) {
					final IShapeStyle defaultShapeStyle =
							PreferenceSetHelper.getDefaultShapeStyle(entityConfiguration.getConditionalShapeStyle());
					if (defaultShapeStyle instanceof IBaseAxisDimensionShapeStyle) {
						double styleSetvalueToToggleArrowGfx =
								((IBaseAxisDimensionShapeStyle) defaultShapeStyle).getValueToToggleArrowGfx();
						IUnit unit = FactoryMgr.getCommonFactory().createUnit();
						unit.setType(preferenceSet.getDistanceUnit());
						unit.setValue(styleSetvalueToToggleArrowGfx);
						IUnit diagramUnit = baseDiagram.gridSpacingSizeInRealWorld();
						GridHelper.convert(unit, diagramUnit.getType());
						valueToToggleArrowGfx = unit.getValue();
					}
				}
			}
			double distance = getDistance(firstPoint, secondPoint);
			final double length = CalcUtils.convertGraphicalToPhysicalLength(baseDiagram, distance);

			return getAxisDimensionTransientGfx(startLocation, endLocation, getRefAxisPoint(startLocation),
					getRefAxisPoint(endLocation), length, valueToToggleArrowGfx, offset);
		}
		return Collections.emptyList();
	}

	@NotNull private Pair<Integer, DirectionEnum> calculateOffset(@NotNull ILocation startLocation,
			@NotNull ILocation endLocation, @NotNull Point currPoint)
	{
		final ILocation refAxisStart = getRefAxisPoint(startLocation);
		final ILocation refAxisEnd = getRefAxisPoint(endLocation);
		final ILocation currLocation = new Location(currPoint);

		final ILocation shiftedRefAxisEnd = FactoryMgr.getCommonFactory()
				.constructLocation(refAxisEnd.getX() + startLocation.getX() - refAxisStart.getX(),
						refAxisEnd.getY() + startLocation.getY() - refAxisStart.getY());

		final ILocation projectedPoint =
				GfxUtils.getProjectedPointOnAnInfiniteLine(startLocation, shiftedRefAxisEnd, currLocation);

		double curOffset = currLocation.distance(projectedPoint);

		ILocation perpVector = FactoryMgr.getCommonFactory()
				.constructLocation(startLocation.getY() - shiftedRefAxisEnd.getY(),
						shiftedRefAxisEnd.getX() - startLocation.getX());
		ILocation projectedVector = FactoryMgr.getCommonFactory()
				.constructLocation(currLocation.getX() - projectedPoint.getX(),
						currLocation.getY() - projectedPoint.getY());

		DirectionEnum direction =
				CoordinateHelper.sameQuadrant(perpVector, projectedVector) ? DirectionEnum.ANTI_CLOCK_WISE :
						DirectionEnum.CLOCK_WISE;
		return new Pair<>(Math.toIntExact(Math.round(curOffset)), direction);
	}

	@NotNull protected abstract ILocation getRefAxisPoint(@NotNull ILocation loc);

	protected abstract double getDistance(@NotNull Point firstPoint, @NotNull Point secondPoint);

	@NotNull protected abstract DimensionType getDimensionType();

	private void clearTransientGraphic()
	{
		for (IGfxObject snapGfx : mTransientAxisDimension) {
			getDynamicGfxService().removeTransientGfx(snapGfx);
		}
		mTransientAxisDimension.clear();
	}

	private void invalidateTransientView()
	{
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eTransient);
		}
	}

	@NotNull private List<IGfxObject> getAxisDimensionTransientGfx(@NotNull ILocation startLocation,
			@NotNull ILocation endLocation, @NotNull ILocation refVecSt, @NotNull ILocation refVecEnd,
			final double dimensionlength, final double valueToToggleArrowGfx, final double offset)
	{
		final Pair<ILocation, ILocation> axisDimensionEndPoints =
				Generator.calculateAxisDimensionEndPoints(startLocation, endLocation, refVecSt, refVecEnd, offset);
		ILocation dimensionLineStartPoint = axisDimensionEndPoints.getFirst();
		ILocation dimensionLineEndPoint = axisDimensionEndPoints.getSecond();

		if (GfxObjectUtils.compareLocation(dimensionLineStartPoint, dimensionLineEndPoint) == 0) {
			return Collections.emptyList();
		}
		final ILine startExtLine = FactoryMgr.getDrawFactory()
				.constructLine(startLocation.getX(), startLocation.getY(), dimensionLineStartPoint.getX(),
						dimensionLineStartPoint.getY());

		final ILine endExtLine = FactoryMgr.getDrawFactory()
				.constructLine(endLocation.getX(), endLocation.getY(), dimensionLineEndPoint.getX(),
						dimensionLineEndPoint.getY());

		final List<IGfxObject> transientGfxObjects = new ArrayList<>();
		transientGfxObjects.add(startExtLine);
		transientGfxObjects.add(endExtLine);

		final List<IGfxObject> gfxGroup =
				Generator.generateDimensionLineGfx(dimensionLineStartPoint, dimensionLineEndPoint, dimensionlength,
						valueToToggleArrowGfx, false);
		transientGfxObjects.addAll(gfxGroup);
		return transientGfxObjects;
	}
}
