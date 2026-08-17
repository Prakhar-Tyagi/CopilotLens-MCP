/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.CAFUtils;
import chs.caf.IBasicDrawingActivityHandler;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.helpers.NewOtherComponentInputResult;
import chs.caf.caplet.helpers.browser.PartBrowserActionHelper;
import chs.caf.caplet.helpers.creation.SnapPoint;
import chs.caf.caplet.helpers.snapping.SnapHelper;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.Model;
import chs.caplets.logic.OtherComponentActionHelper;
import chs.caplets.logic.actions.layout.DeviceLayoutHelper;
import chs.caplets.logic.actions.layout.ExtentObjectAlignmentHighlightHelper;
import chs.caplets.logic.actions.layout.IComponentPhysicalDetails;
import chs.caplets.logic.actions.layout.PlacementAxisRotation;
import chs.cof.draw.IColor;
import chs.cof.draw.IDrawFactory;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGfxObjectPhysicalStateDisplayControl;
import chs.cof.draw.IGrid;
import chs.cof.draw.ITransform;
import chs.cof.draw.SnapStyle;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IPropertiedCommentSymbol;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.ILogicOtherComponent;
import chs.cof.logical.cable.IPanelLayoutDuctComponent;
import chs.cof.logical.cable.IPanelLayoutRailComponent;
import chs.cof.logical.cable.LogicOtherComponentTypeEnum;
import chs.cof.logical.cable.PanelLayoutDuctComponentType;
import chs.cof.logical.cable.PanelLayoutLengthComponentType;
import chs.cof.logical.cable.PanelLayoutOtherComponentType;
import chs.cof.logical.cable.PanelLayoutRailComponentType;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemFactory;
import chs.cof.logical.schem.ISchemOtherComponentParameterized;
import chs.cof.logical.schem.ISchemOtherComponentSymbolled;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.project.IProject;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.SymbolSubTypeEnum;
import chs.cof.symbol.SymbolTypeEnum;
import chs.cofUtils.SymbolGraphicUtils;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.GeneratorStyle;
import chs.cofUtils.parameterized.LengthWiseOtherComponentDimensionAdjustment;
import chs.common.ICommonFactory;
import chs.common.IExtent;
import chs.common.IParameterized;
import chs.common.UnitTypeEnum;
import chs.common.attr.custom.CustomAttributeConstants;
import chs.common.geom.GeometryUtils;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.ListSet;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.DiagramHelper;
import chs.utility.GfxUtils;
import chs.utility.SymbolUtils;
import chs.utility.attr.custom.CustomAttributesControl;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.ExtentHelper;
import chs.utility.helpers.GridHelper;
import chs.utility.helpers.LibraryHelper;
import chs.utility.helpers.LogHelper;
import chs.utility.helpers.PanelLayoutHelper;
import chs.utility.logic.ILogicModel;
import chs.utility.logic.LogicUtils;
import chs.utility.ui.HTMLHelper;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author chandras on 3-10-2019.
 */
public abstract class AbstractCreateOtherComponentAction extends ControllerActionRT
		implements IBasicDrawingActivityHandler
{

	private final String m_sActionClass;
	@Nullable protected NewOtherComponentInputResult m_newOtherComponentInputResult;
	private IOtherComponentPlacementControl m_otherCompPlacementControl = null;
	private Point m_currMousePoint = null;

	protected AbstractCreateOtherComponentAction(ICapletController controller, String sActionUIClass)
	{
		super(controller);
		m_sActionClass = sActionUIClass;
	}

	@Override public IDynamicGfxService getDynamicGfxService()
	{
		return getModel().getDynamicGfxService();
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{

	}

	@Override public void populateActiveCtxMenu(ActionContainer container)
	{

	}

	@Override protected boolean checkCache()
	{
		return false;
	}

	@NotNull protected ILogicDesign getLogicDesign()
	{
		return ((ILogicModel) getController().getCapletModel()).getDesign();
	}

	@Override public boolean isEnabled()
	{
		//disbale action on read-only design.
		return getController().getCapletModel().isEditable() && super.isEnabled();
	}

	@Nullable protected NewOtherComponentInputResult getNewOtherComponentInputResult()
	{
		final LogicOtherComponentTypeEnum selectedComponentType = determineDefaultComponentType();
		final String name = ""; //we want indexed naming for other components i.e mount/duct/other.
		return new NewOtherComponentInputResult(name, selectedComponentType);
	}

	@Nullable protected IConnectivity getDesignConnectivity()
	{
		return getLogicDesign().getConnectivity();
	}

	public String getActionUIClass()
	{
		return m_sActionClass;
	}

	protected abstract boolean shouldInitPhysicalDimensionAttributes();

	protected void cleanComment()
	{
		if (m_otherCompPlacementControl != null) {
			m_otherCompPlacementControl.cleanup();
		}
	}

	@NotNull protected LogicOtherComponentTypeEnum determineDefaultComponentType()
	{
		//here we can determine using some heuristics on typecode of library component.
		final ILibraryObject selectedLibraryPart = acquireSelectedLibraryPart();
		if (selectedLibraryPart != null) {
			final ILibraryObject.GroupType typeCode = selectedLibraryPart.getGroupName();
			if (ILibraryObject.GroupType.CHANNEL.equals(typeCode)) {
				return LogicOtherComponentTypeEnum.DUCT;
			}
			if (ILibraryObject.GroupType.MOUNT.equals(typeCode)) {
				return LogicOtherComponentTypeEnum.RAIL;
			}
		}
		else {
			final List<ISymbolDef> symbolDefs = acquireSelectedSymbols();
			if (!symbolDefs.isEmpty()) {
				final ISymbolDef selectedSymbol = symbolDefs.iterator().next();
				final SymbolSubTypeEnum symbolSubType = selectedSymbol.getSymbolSubType();
				if (SymbolSubTypeEnum.DUCT.equals(symbolSubType)) {
					return LogicOtherComponentTypeEnum.DUCT;
				}
				if (SymbolSubTypeEnum.MOUNT.equals(symbolSubType)) {
					return LogicOtherComponentTypeEnum.RAIL;
				}
			}
		}
		return LogicOtherComponentTypeEnum.GENERIC;
	}

	@NotNull protected Pair<IComponentPhysicalDetails, Boolean> determineParameterized()
	{
		final ILibraryObject selectedLibraryPart = acquireSelectedLibraryPart();
		return (selectedLibraryPart != null) ?
				new Pair<>(DeviceLayoutHelper.getComponentPhysicalDetails(selectedLibraryPart), false) :
				new Pair<>(DeviceLayoutHelper.getBlankComponentPhysicalDetails(), true);
	}

	@NotNull protected final List<ISymbolDef> getSymbolDefsFromPart(@Nullable ILibraryObject libraryObject)
	{
		Map<String, ISymbolDef> partSymbols =
				SymbolUtils.getAssociatedLibrarySymbols(libraryObject, this::isValidSymbolType);
		final ListSet<ISymbolDef> result = new ListSet<>(8);
		if (partSymbols != null) {
			result.addAll(partSymbols.values());
		}
		return Collections.unmodifiableList(result);
	}

	@Nullable protected PanelLayoutLengthComponentType getLengthWiseSubComponentType(
			@NotNull LogicOtherComponentTypeEnum type)
	{
		String subTypeAsXML = type.getDefaultComponentTypeAsXML();
		final ILibraryObject selectedLibraryPart = CommonUtils.cast(acquireSelectedLibraryPart(),
				ILibraryObject.class);
		if (selectedLibraryPart != null) {
			final String panelType = PanelLayoutHelper.getOverriddenPanelType(selectedLibraryPart, getBaseDiagram());
			subTypeAsXML = StringUtils.isBlank(panelType) ? "" : panelType;
		}
		return LengthWiseOtherComponentDimensionAdjustment.determineDefaultLengthWiseComponentType(type, subTypeAsXML);
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		final List<ISymbolDef> selectedSymbols = acquireSelectedSymbols();
		m_newOtherComponentInputResult = isPartSelectionValid() ? getNewOtherComponentInputResult() : null;
		if (m_newOtherComponentInputResult == null) {
			return IActionEnum.eCanceled;
		}
		final LogicOtherComponentTypeEnum type = m_newOtherComponentInputResult.getType();
		if (selectedSymbols.isEmpty()) {
			final Pair<IComponentPhysicalDetails, Boolean> parameterized = determineParameterized();
			final IBaseDiagram diagram = getBaseDiagram();
			assert diagram != null;
			IComponentPhysicalDetails componentPhysicalDetails = parameterized.getKey();
			final int length = componentPhysicalDetails.getLength(diagram);
			final int width = componentPhysicalDetails.getWidth(diagram);
			final PanelLayoutLengthComponentType subComponentType = getLengthWiseSubComponentType(type);
			if (length > 0 && width > 0) {
				if (type.isKindOfLengthWiseObject()) {
					if (hasMinimumRequiredLength(length, subComponentType, diagram)) {
						m_otherCompPlacementControl =
								new ConstrainedDynamicParameterizedPlacementControl(type, subComponentType, length,
										width);
					}
					else {
						displayMessageInOutputWindow(HTMLHelper.color(IColor.RED, ResourceMgr.getString(
								AbstractCreateOtherComponentAction.class,
								"CreateOtherComponentAction.Parameterized.minimumLengthError")));
					}
				}
				else {
					m_otherCompPlacementControl = new StaticParameterizedPlacementControl(type, subComponentType,
							length, width);
				}
			}
			else if (parameterized.getValue()) {
				m_otherCompPlacementControl = new DynamicParameterizedPlacementControl(type, subComponentType);
			}
			else {
				displayMessageInOutputWindow(HTMLHelper.color(IColor.RED, ResourceMgr.getString(
						AbstractCreateOtherComponentAction.class,
						"CreateOtherComponentAction.Parameterized.invalidStatus")));
			}
		}
		else {
			m_otherCompPlacementControl =
					new OtherComponentSymbolledPlacementControl(type, getController(), selectedSymbols);
		}
		return m_otherCompPlacementControl != null ? IActionEnum.eActivated : IActionEnum.eCanceled;
	}

	private boolean hasMinimumRequiredLength(int length, @Nullable PanelLayoutLengthComponentType subComponentType,
			@NotNull IBaseDiagram diagram)
	{
		boolean hasMinimumRequiredLength = true;
		if (subComponentType != null) {
			final int unitLength =
					toWorldCoordFromPhysical(subComponentType.getGlyphLength(), subComponentType.getUnit(), diagram);
			hasMinimumRequiredLength = length >= unitLength;
		}
		return hasMinimumRequiredLength;
	}

	protected boolean isValidSymbolType(@NotNull ISymbolDef symbol)
	{
		return SymbolUtils.isValidSymbolType(symbol, new SymbolTypeEnum[]{SymbolTypeEnum.COMMENT});
	}

	protected List<ISymbolDef> acquireSelectedSymbols()
	{
		final ISymbolDef activeSymbol = CommonUtils.cast(SymbolGraphicUtils.validateSymbol(
				CAFUtils.getInstance().getCHSSystem().getSymbolLibraryMgr().getActiveSymbol()), ISymbolDef.class);
		if (activeSymbol != null && isValidSymbolType(activeSymbol)) {
			return Arrays.asList(activeSymbol);
		}
		return Collections.emptyList();
	}

	@Nullable protected ILibraryObject acquireSelectedLibraryPart()
	{
		ILibraryPartSelection libPartSelection = getLibraryPartSelection();
		final Object selLibPart = libPartSelection != null ? libPartSelection.getSelectedObject() : null;
		return LibraryHelper.getLayoutComponentLibObject(selLibPart);
	}

	private boolean isPartSelectionValid()
	{
		final ILibraryObject libraryOtherComp = acquireSelectedLibraryPart();
		if (libraryOtherComp == null) {
			return true;
		}
		IProject project = getLogicDesign().getProject();
		boolean isValid = false;
		if (project != null) {
			if (LibraryHelper.isPartUsableForProject(project, libraryOtherComp)) {
				isValid = true;
			}
			else {
				final String message = getMessageForLibraryOtherComponentWhichIsNotCurrent(libraryOtherComp);
				displayMessageInOutputWindow(message);
			}
		}
		return isValid;
	}

	private void displayMessageInOutputWindow(String message)
	{
		LogHelper.appMsgSafe(message);
	}

	private String getMessageForLibraryOtherComponentWhichIsNotCurrent(ILibraryObject libraryOther)
	{
		final String key = "CreateOtherComponentAction.OtherComponent.invalidStatus";
		return HTMLHelper.color(IColor.RED,
				ResourceMgr.getString(AbstractCreateOtherComponentAction.class, key, libraryOther.getPartNumber()));
	}

	@Nullable protected ILibraryPartSelection getLibraryPartSelection()
	{
		return PartBrowserActionHelper.getCurrentSelectedBrowserPart();
	}

	protected boolean onTerminate(boolean successful)
	{
		final IConnectivity connectivity = getDesignConnectivity();
		final boolean status = successful && connectivity != null && m_newOtherComponentInputResult != null &&
				m_otherCompPlacementControl != null;
		final IDynamicGfxService dynamicGfxService = getModel().getDynamicGfxService();
		try {
			if (status) {
				final ILogicOtherComponent logicOtherComponent =
						constructConnectivityComponent(connectivity, m_newOtherComponentInputResult);
				if (shouldInitPhysicalDimensionAttributes()) {
					m_otherCompPlacementControl.updatePhysicalDimensionAttributes(logicOtherComponent);
				}
				m_otherCompPlacementControl.terminate(logicOtherComponent, dynamicGfxService);
			}
		}
		finally {
			getController().getSelectMgr().removeSelectSet();
			dynamicGfxService.removeAllDynamicGfx();
			dynamicGfxService.removeAllTransientGfx();
			updateTransientView();
			reset();
		}
		return status;
	}

	@NotNull protected ILogicOtherComponent constructConnectivityComponent(@NotNull IConnectivity connectivity,
			@NotNull NewOtherComponentInputResult newOtherComponentInputResult)
	{
		final LogicOtherComponentTypeEnum compType = newOtherComponentInputResult.getType();
		String defaultSubType = compType.getDefaultComponentTypeAsXML();
		final String name = newOtherComponentInputResult.getName();
		final ILibraryObject libraryOtherComp = acquireSelectedLibraryPart();
		ILogicOtherComponent logicOtherComponent = OtherComponentActionHelper
				.createLogicOtherComponent(libraryOtherComp, connectivity, name, compType, defaultSubType);
		if (libraryOtherComp != null) {
			String overriddenPanelType = PanelLayoutHelper.getOverriddenPanelType(libraryOtherComp, getBaseDiagram());
			if (overriddenPanelType != null) {
				if (logicOtherComponent instanceof IPanelLayoutRailComponent) {
					((IPanelLayoutRailComponent) logicOtherComponent)
							.setComponentType(PanelLayoutRailComponentType.fromXML(overriddenPanelType));
				}
				else if (logicOtherComponent instanceof IPanelLayoutDuctComponent) {
					((IPanelLayoutDuctComponent) logicOtherComponent)
							.setComponentType(PanelLayoutDuctComponentType.fromXML(overriddenPanelType));
				}
			}
		}
		return logicOtherComponent;
	}

	@SuppressWarnings("ConstantConditions")
	private void reset()
	{
		cleanComment();
		m_newOtherComponentInputResult = null;
		m_otherCompPlacementControl = null;
	}

	protected void terminateAction(boolean success)
	{
		getController().getActionMgr().terminateActiveAction(success);
	}

	public SnapPoint snapWorldPoint(MouseEvent e, Point worldPt)
	{
		final boolean shouldSnapToGrid =
				m_otherCompPlacementControl != null && m_otherCompPlacementControl.shouldSnapToGrid();
		final Point snapPoint = snapPoint(getMouseWorldPoint(e), shouldSnapToGrid);
		final Point otherCompSnapPoint = m_otherCompPlacementControl != null ?
				m_otherCompPlacementControl.getConstrainedSnapPoint(snapPoint) : snapPoint;
		return SnapPoint.toSnapPoint(otherCompSnapPoint, worldPt, true);
	}

	@Override public void keyTyped(KeyEvent e)
	{

	}

	@Override public void keyPressed(KeyEvent e)
	{
		final IDynamicGfxService dynamicGfxService = getModel().getDynamicGfxService();
		if (m_otherCompPlacementControl.processKeyPressed(e, dynamicGfxService)) {
			updateTransientView();
		}
	}

	@Override public void keyReleased(KeyEvent e)
	{

	}

	@Override public void mouseClicked(MouseEvent e)
	{
		m_currMousePoint = getMouseWorldPoint(e);
		if (e.getClickCount() > 1 && m_otherCompPlacementControl != null) {
			terminateAction(m_otherCompPlacementControl.isReadyForTermination());
		}
		else if (m_otherCompPlacementControl != null) {
			m_otherCompPlacementControl.mouseClicked(m_currMousePoint);
			final boolean readyForTermination = m_otherCompPlacementControl.isReadyForTermination();
			if (readyForTermination) {
				terminateAction(true);
			}
		}
	}

	@Override public void mousePressed(MouseEvent e)
	{

	}

	@Override public void mouseReleased(MouseEvent e)
	{

	}

	@Override public void mouseEntered(MouseEvent e)
	{

	}

	@Override public void mouseExited(MouseEvent e)
	{

	}

	@Override public void mouseDragged(MouseEvent e)
	{

	}

	@Override public void mouseMoved(MouseEvent e)
	{
		//
		// Keep the location around...
		//
		m_currMousePoint = getMouseWorldPoint(e);
		final IDynamicGfxService dynamicGfxService = getModel().getDynamicGfxService();
		m_otherCompPlacementControl.mouseMoved(m_currMousePoint, dynamicGfxService);
		updateTransientView();
	}

	private void updateTransientView()
	{
		GfxView view = (GfxView) CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.refreshPhysicalStateInformation();
			view.invalidate(IViewInvalidationEnum.eTransient);
		}
	}

	@NotNull protected static Point snapPoint(@NotNull Point mousePoint, boolean shouldSnapToGrid)
	{
		final IBaseDiagram diagram = getBaseDiagram();
		final Point snappedPt = new Point(mousePoint);
		if (diagram != null) {
			final IGrid grid = determineSnapStyle(shouldSnapToGrid).getSnapGrid(diagram);
			if (grid != null) {
				snappedPt.setLocation(grid.snap(mousePoint.x), grid.snap(mousePoint.y));
			}
		}
		return snappedPt;
	}

	@NotNull private static SnapStyle determineSnapStyle(boolean shouldSnapToGrid)
	{
		if (shouldSnapToGrid) {
			return SnapStyle.GRID_SNAP;
		}
		if (SnapHelper.isDrawingGridSnapEnabled()) {
			return SnapStyle.SUB_GRID_SNAP;
		}
		return SnapStyle.NO_SNAP;
	}

	@NotNull protected Model getModel()
	{
		return (Model) getCapletModel();
	}

	private static int toWorldCoordFromPhysical(double val, @NotNull UnitTypeEnum unit,
			@NotNull IBaseDiagram diagram)
	{
		final double physicalDepth = GridHelper.convert(val, unit, diagram.getGrid().getRealMapping().getType());
		return GridHelper.toWorldCoordFromPhysicalVal(physicalDepth, diagram.getGrid(), diagram);
	}

	private static class OtherComponentPlacementItem
	{

		private final ISymbolDef m_symbol;
		private IPropertiedCommentSymbol m_placementObject;

		private OtherComponentPlacementItem(@NotNull ICapletController controller, @NotNull ISymbolDef symbol)
		{
			m_symbol = symbol;
			final IBaseDiagram baseDiagram = getBaseDiagram();
			assert baseDiagram != null;
			m_placementObject = OtherComponentActionHelper.getPlacementObject(controller, symbol, baseDiagram,
					FactoryMgr.getSchemFactory()::createOtherComponentSymbolled);
		}

		@SuppressWarnings("ConstantConditions")
		public void cleanup()
		{
			if (m_placementObject != null) {
				deleteObject(m_placementObject);
				m_placementObject = null;
			}
		}

		public IExtent getAbsoluteExtent()
		{
			return ExtentHelper.getAbsExtentExcludingText(m_placementObject);
		}

		public void mouseMoved(@NotNull Point currSnappedPt)
		{
			final ICommonFactory commonFactory = FactoryMgr.getCommonFactory();
			m_placementObject.setLocation(commonFactory.constructLocation(currSnappedPt.x, currSnappedPt.y));
		}

		public void registerTransientGraphics(@NotNull IDynamicGfxService dynamicGfxService)
		{
			dynamicGfxService.addTransientGfx(m_placementObject);
		}

		public void deRegisterTransientGraphics(@NotNull IDynamicGfxService dynamicGfxService)
		{
			dynamicGfxService.removeTransientGfx(m_placementObject);
		}

		public void setupNextAxis(Point currSnappedPt)
		{
			m_placementObject.rotate(GeometryUtils.NINETY_DEGREES, currSnappedPt.x, currSnappedPt.y, 0, 0);
		}

		public void setupPrevAxis(Point currSnappedPt)
		{
			m_placementObject.rotate(GeometryUtils.TWO_SEVENTY_DEGREES, currSnappedPt.x, currSnappedPt.y, 0, 0);
		}

		@SuppressWarnings("ConstantConditions")
		public void terminate(@NotNull ILogicOtherComponent logicOtherComponent)
		{
			if (m_placementObject != null) {
				boolean home = !LogicUtils.hasUsage(logicOtherComponent);
				final ISchemOtherComponentSymbolled componentSymbolled =
						(ISchemOtherComponentSymbolled) m_placementObject;
				componentSymbolled.setConnectivity(logicOtherComponent);
				IBaseDiagram baseDiagram = getBaseDiagram();
				assert baseDiagram != null;
				OtherComponentActionHelper.placeCommentSymbolAsComponent(m_symbol, null, m_placementObject,
						logicOtherComponent, baseDiagram, SymbolUtils.getSymbolScale(m_symbol, baseDiagram.getGrid()));
				componentSymbolled.setHome(home);
				componentSymbolled.regenerateDiagramObject(true);
				m_placementObject = null;
			}
		}
	}

	private interface IOtherComponentPlacementControl
	{

		void cleanup();

		void mouseMoved(@NotNull Point mousePoint, @NotNull IDynamicGfxService dynamicGfxService);

		boolean processKeyPressed(KeyEvent e, @NotNull IDynamicGfxService dynamicGfxService);

		void terminate(@NotNull ILogicOtherComponent otherComponent, @NotNull IDynamicGfxService dynamicGfxService);

		boolean isReadyForTermination();

		void mouseClicked(Point currMousePoint);

		void informGraphicPhysicalState(@NotNull IGfxObjectPhysicalStateDisplayControl control);

		boolean shouldSnapToGrid();

		@NotNull Point getConstrainedSnapPoint(@NotNull Point snapPoint);

		void updatePhysicalDimensionAttributes(@NotNull ILogicOtherComponent logicOtherComponent);
	}

	private abstract static class OtherComponentPlacementControl implements IOtherComponentPlacementControl
	{

		protected Map<Integer, Consumer<Integer>> m_keyHandlers = new HashMap<>(8);
		protected Point m_currSnappedPt;
		protected final LogicOtherComponentTypeEnum m_componentType;
		protected final ExtentObjectAlignmentHighlightHelper m_alignGuide;
		protected final List<IGfxObject> m_transientAlignLines = new ArrayList<>(6);

		protected OtherComponentPlacementControl(LogicOtherComponentTypeEnum type)
		{
			m_componentType = type;
			final ISchemDiagram schemDiagram = CommonUtils.cast(getBaseDiagram(), ISchemDiagram.class);
			m_alignGuide = (schemDiagram != null) ? new ExtentObjectAlignmentHighlightHelper(schemDiagram) : null;
		}

		public boolean shouldSnapToGrid()
		{
			return m_componentType.isKindOfLengthWiseObject();
		}

		@NotNull public Point getConstrainedSnapPoint(@NotNull Point snapPoint)
		{
			return snapPoint;
		}

		@Override public void cleanup()
		{
			m_keyHandlers.clear();
			m_transientAlignLines.clear();
		}

		protected void deRegisterAlignmentLines(@NotNull IDynamicGfxService dynamicGfxService)
		{
			m_transientAlignLines.forEach(dynamicGfxService::removeTransientGfx);
			m_transientAlignLines.clear();
		}

		protected void registerAlignmentLines(@NotNull IDynamicGfxService dynamicGfxService)
		{
			m_transientAlignLines.forEach(dynamicGfxService::addTransientGfx);
		}

		protected void computeAlignmentGuides(IExtent absExtent)
		{
			if (m_alignGuide != null) {
				final SnapStyle snapStyle = determineSnapStyle(shouldSnapToGrid());
				m_alignGuide.diagnose(absExtent, m_transientAlignLines::add, snapStyle, false);
			}
		}

		@Override public void updatePhysicalDimensionAttributes(@NotNull ILogicOtherComponent logicOtherComponent)
		{
			final ISchemDiagram schemDiagram = CommonUtils.cast(getBaseDiagram(), ISchemDiagram.class);
			if (schemDiagram == null) {
				return;
			}
			CustomAttributesControl attributesControl = new CustomAttributesControl(logicOtherComponent);
			final IGrid grid = schemDiagram.getGrid();
			final double length = getPlacedComponentWorldLength();
			final double width = getPlacedComponentWorldWidth();

			final double physicalLength =
					GridHelper.toPhysicalCoordFromWorld(CommonUtils.toInteger(length), grid, schemDiagram);
			final double physicalWidth =
					GridHelper.toPhysicalCoordFromWorld(CommonUtils.toInteger(width), grid, schemDiagram);
			if (physicalLength > 0) {
				attributesControl.updateAttribute(CustomAttributeConstants.LayCompLength.getName(), physicalLength);
			}
			if (physicalWidth > 0) {
				attributesControl.updateAttribute(CustomAttributeConstants.LayCompWidth.getName(), physicalWidth);
			}

			final double depth = getPlacedComponentWorldDepth(schemDiagram);
			final double physicalDepth =
					GridHelper.toPhysicalCoordFromWorld(CommonUtils.toInteger(depth), grid, schemDiagram);
			if (physicalDepth > 0) {
				attributesControl.updateAttribute(CustomAttributeConstants.LayCompDepth.getName(), physicalDepth);
			}
		}

		protected abstract double getPlacedComponentWorldDepth(@NotNull ISchemDiagram diagram);

		protected abstract double getPlacedComponentWorldWidth();

		protected abstract double getPlacedComponentWorldLength();
	}

	private static class OtherComponentSymbolledPlacementControl extends OtherComponentPlacementControl
	{

		private final List<OtherComponentPlacementItem> m_items = new ArrayList<>();
		private int m_currentIdx = 0;

		private OtherComponentSymbolledPlacementControl(LogicOtherComponentTypeEnum type,
				@NotNull ICapletController controller,
				@NotNull List<ISymbolDef> symbols)
		{
			super(type);
			symbols.forEach(s -> m_items.add(new OtherComponentPlacementItem(controller, s)));

			m_keyHandlers.put(KeyEvent.VK_T, (t) -> {
				setupPrevAxis();
			});

			m_keyHandlers.put(KeyEvent.VK_R, (t) -> {
				setupNextAxis();
			});

			m_keyHandlers.put(KeyEvent.VK_S, (t) -> {
				setupPlacementIndex();
			});
		}

		private void setupPlacementIndex()
		{
			final int size = m_items.size();
			if (size > 0) {
				m_currentIdx = (m_currentIdx + 1) % size;
			}
		}

		private void setupNextAxis()
		{
			if (m_currSnappedPt != null) {
				m_items.forEach(s -> s.setupNextAxis(m_currSnappedPt));
			}
		}

		private void setupPrevAxis()
		{
			if (m_currSnappedPt != null) {
				m_items.forEach(s -> s.setupPrevAxis(m_currSnappedPt));
			}
		}

		@Override public void cleanup()
		{
			super.cleanup();
			m_items.forEach(OtherComponentPlacementItem::cleanup);
		}

		@Override protected double getPlacedComponentWorldDepth(@NotNull ISchemDiagram diagram)
		{
			return 0;
		}

		@Override protected double getPlacedComponentWorldWidth()
		{
			return 0;
		}

		@Override protected double getPlacedComponentWorldLength()
		{
			return 0;
		}

		@Override public void mouseMoved(@NotNull Point mousePoint, @NotNull IDynamicGfxService dynamicGfxService)
		{
			final Point snapPoint = snapPoint(mousePoint, shouldSnapToGrid());
			if (m_currSnappedPt != null && snapPoint.equals(m_currSnappedPt)) {
				return;
			}
			m_currSnappedPt = snapPoint;
			//move all the candidates so that when we change the current index the candidate will be at right position.
			m_items.forEach(s -> s.mouseMoved(m_currSnappedPt));
			deRegisterTransientGraphics(dynamicGfxService);
			registerTransientGraphics(dynamicGfxService);
		}

		protected void deRegisterTransientGraphics(@NotNull IDynamicGfxService dynamicGfxService)
		{
			m_items.forEach(s -> s.deRegisterTransientGraphics(dynamicGfxService));
			deRegisterAlignmentLines(dynamicGfxService);
		}

		protected void registerTransientGraphics(@NotNull IDynamicGfxService dynamicGfxService)
		{
			if (m_currentIdx >= 0 && m_currentIdx < m_items.size()) {
				final OtherComponentPlacementItem currentPlacementItem = m_items.get(m_currentIdx);
				currentPlacementItem.registerTransientGraphics(dynamicGfxService);
				computeAlignmentGuides(currentPlacementItem.getAbsoluteExtent());
				registerAlignmentLines(dynamicGfxService);
			}
		}

		@Override public boolean processKeyPressed(KeyEvent e, @NotNull IDynamicGfxService dynamicGfxService)
		{
			int keyCode = e.getKeyCode();
			Consumer<Integer> keyHandler = m_keyHandlers.get(keyCode);
			if (keyHandler != null) {
				keyHandler.accept(keyCode);
				deRegisterTransientGraphics(dynamicGfxService);
				registerTransientGraphics(dynamicGfxService);
				return true;
			}
			return false;
		}

		@Override public void terminate(@NotNull ILogicOtherComponent otherComponent,
				@NotNull IDynamicGfxService dynamicGfxService)
		{
			deRegisterTransientGraphics(dynamicGfxService);
			if (m_currentIdx >= 0 && m_currentIdx < m_items.size()) {
				final OtherComponentPlacementItem currentPlacementItem = m_items.get(m_currentIdx);
				currentPlacementItem.terminate(otherComponent);
			}
		}

		@Override public boolean isReadyForTermination()
		{
			return true;
		}

		@Override public void mouseClicked(Point currMousePoint)
		{
			final Point snapPoint = snapPoint(currMousePoint, shouldSnapToGrid());
			m_currSnappedPt = snapPoint;
		}

		@Override public void informGraphicPhysicalState(@NotNull IGfxObjectPhysicalStateDisplayControl control)
		{

		}
	}

	private abstract static class ParameterizedPlacementControl extends OtherComponentPlacementControl
	{

		@Nullable protected final PanelLayoutLengthComponentType m_compSubType;
		protected List<IGfxObject> m_transientParams = new ArrayList<>(6);

		protected ParameterizedPlacementControl(LogicOtherComponentTypeEnum type,
				@Nullable PanelLayoutLengthComponentType compSubType)
		{
			super(type);
			m_compSubType = compSubType;
		}

		protected void doUpdateTransientGraphics(final Point pt1, final Point pt2, boolean horizontalAxis)
		{
			int centerX = (pt1.x + pt2.x) / 2;
			int centerY = (pt1.y + pt2.y) / 2;
			m_transientParams.add(FactoryMgr.getDrawFactory().constructRectangle(pt1.x, pt1.y, pt2.x, pt2.y));
			if (horizontalAxis) {
				final int gridSize = Math.min(IGrid.GRID_SIZE, Math.abs(pt1.x - pt2.x) / 2);
				GfxUtils.constructBiDirArrow(new Point(centerX - gridSize, centerY),
						new Point(centerX + gridSize, centerY), m_transientParams);
			}
			else {
				final int gridSize = Math.min(IGrid.GRID_SIZE, Math.abs(pt1.y - pt2.y) / 2);
				GfxUtils.constructBiDirArrow(new Point(centerX, centerY - gridSize),
						new Point(centerX, centerY + gridSize), m_transientParams);
			}
			// Lower left is an untransformed location
			Point lowerLeft = GfxUtils.calculateLowerLeft(pt1, pt2);
			int height = Math.abs(pt2.y - pt1.y);
			int width = Math.abs(pt2.x - pt1.x);
			final ICommonFactory commonFactory = FactoryMgr.getCommonFactory();
			computeAlignmentGuides(commonFactory.constructExtent(lowerLeft.x, lowerLeft.y, width, height));
		}

		@Override public void cleanup()
		{
			super.cleanup();
			m_transientParams.clear();
		}

		protected abstract void updateTransientGraphics();

		@Override public void mouseMoved(@NotNull Point mousePoint, @NotNull IDynamicGfxService dynamicGfxService)
		{
			final Point snapPoint = snapPoint(mousePoint, shouldSnapToGrid());
			if (m_currSnappedPt != null && snapPoint.equals(m_currSnappedPt)) {
				return;
			}
			deRegisterTransientGraphics(dynamicGfxService);
			m_currSnappedPt = snapPoint;
			updateTransientGraphics();
			registerTransientGraphics(dynamicGfxService);
		}

		@Override public boolean processKeyPressed(KeyEvent e, @NotNull IDynamicGfxService dynamicGfxService)
		{
			int keyCode = e.getKeyCode();
			Consumer<Integer> keyHandler = m_keyHandlers.get(keyCode);
			if (keyHandler != null) {
				deRegisterTransientGraphics(dynamicGfxService);
				keyHandler.accept(keyCode);
				updateTransientGraphics();
				registerTransientGraphics(dynamicGfxService);
				return true;
			}
			return false;
		}

		private void registerTransientGraphics(@NotNull IDynamicGfxService dynamicGfxService)
		{
			m_transientParams.forEach(dynamicGfxService::addTransientGfx);
			registerAlignmentLines(dynamicGfxService);
		}

		private void deRegisterTransientGraphics(@NotNull IDynamicGfxService dynamicGfxService)
		{
			m_transientParams.forEach(dynamicGfxService::removeTransientGfx);
			deRegisterAlignmentLines(dynamicGfxService);
		}

		protected void doGenerateParameterizedComponent(@NotNull ILogicOtherComponent otherComponent,
				ISchemDiagram schemDiagram, int x, int y, int w, int h, ITransform transform)
		{
			boolean home = !LogicUtils.hasUsage(otherComponent);
			final ISchemFactory schemFactory = FactoryMgr.getSchemFactory();
			final ICommonFactory commonFactory = FactoryMgr.getCommonFactory();
			final ISchemOtherComponentParameterized otherComponentParameterized =
					schemFactory.createOtherComponentParameterized(FactoryMgr.createUID(), otherComponent);
			otherComponentParameterized.setLocation(commonFactory.constructLocation(x, y));
			otherComponentParameterized.setTransform(transform);
			final IParameterized parameterized = commonFactory.createParameterized();
			parameterized.setExtent(commonFactory.constructExtent(0, -h / 2, w, h));
			loadParameterizedDefaults(parameterized);
			otherComponentParameterized.setParameterized(parameterized);
			schemDiagram.addObject(otherComponentParameterized);
			//must add to diagram before calling generate otherwise it won't generate.
			//the generator needs physical unit to regenerate the graphics.
			GeneratorParameters paramOptions = DiagramHelper.createGeneratorParameters(schemDiagram);
			Generator.getGenerator().generateOtherComponent(otherComponentParameterized, paramOptions);
			otherComponentParameterized.extentChanged(otherComponentParameterized);
			otherComponentParameterized.setHome(home);
			otherComponentParameterized.regenerateDiagramObject(true);
		}

		private void loadParameterizedDefaults(@NotNull IParameterized parameterized)
		{
			if (LogicOtherComponentTypeEnum.GENERIC.equals(m_componentType)) {
				//
				// Get the generator, add the defaults, and go!
				//
				GeneratorStyle gs = Generator.getGenerator().getStyle();
				if (gs != null) {
					gs.addDefaults(parameterized, PanelLayoutOtherComponentType.PARAM_REF);
				}
			}
		}

		protected void doInformGraphicPhysicalState(@NotNull IGfxObjectPhysicalStateDisplayControl control,
				Point pt1, Point pt2, boolean horizontal)
		{
			final Pair<Double, Double> widthHeightPair = determineWidthHeightPair(pt1, pt2, horizontal);
			control.processRectangularShape(widthHeightPair.getKey(), widthHeightPair.getValue());
		}

		@NotNull protected Pair<Double, Double> determineWidthHeightPair(Point pt1, Point pt2, boolean horizontal)
		{
			double w = horizontal ? pt1.distance(pt2.x, pt1.y) : pt1.distance(pt1.x, pt2.y);
			double h = horizontal ? pt1.distance(pt1.x, pt2.y) : pt1.distance(pt2.x, pt1.y);
			return new Pair<>(w, h);
		}

		@Override protected double getPlacedComponentWorldDepth(@NotNull ISchemDiagram diagram)
		{
			if (m_compSubType != null) {
				return toWorldCoordFromPhysical(m_compSubType.getGlyphDepth(), m_compSubType.getUnit(), diagram);
			}
			return 0;
		}
	}

	private static class StaticParameterizedPlacementControl extends ParameterizedPlacementControl
	{

		private PlacementAxisRotation m_placementAxis = PlacementAxisRotation.Zero;
		private int m_paramWidth;
		private int m_paramHeight;

		private StaticParameterizedPlacementControl(LogicOtherComponentTypeEnum type,
				@Nullable PanelLayoutLengthComponentType compSubType, int paramWidth, int paramHeight)
		{
			super(type, compSubType);
			m_paramWidth = paramWidth;
			m_paramHeight = paramHeight;
			m_keyHandlers.put(KeyEvent.VK_R, (t) -> {
				m_placementAxis = m_placementAxis.next();
			});
			m_keyHandlers.put(KeyEvent.VK_T, (t) -> {
				m_placementAxis = m_placementAxis.prev();
			});
		}

		protected void updateTransientGraphics()
		{
			m_transientParams.clear();
			if (m_currSnappedPt != null) {
				Point pt1 = new Point();
				Point pt2 = new Point();
				computeCorners(pt1, pt2);
				doUpdateTransientGraphics(pt1, pt2, !m_placementAxis.isYAxis());
			}
		}

		private void computeCorners(Point pt1, Point pt2)
		{
			switch (m_placementAxis) {
				case Zero:
					pt1.setLocation(m_currSnappedPt.x, m_currSnappedPt.y + m_paramHeight / 2);
					pt2.setLocation(m_currSnappedPt.x + m_paramWidth, m_currSnappedPt.y - m_paramHeight / 2);
					break;
				case Ninety:
					pt1.setLocation(m_currSnappedPt.x - m_paramHeight / 2, m_currSnappedPt.y);
					pt2.setLocation(m_currSnappedPt.x + m_paramHeight / 2, m_currSnappedPt.y - m_paramWidth);
					break;
				case OneEighty:
					pt1.setLocation(m_currSnappedPt.x - m_paramWidth, m_currSnappedPt.y + m_paramHeight / 2);
					pt2.setLocation(m_currSnappedPt.x, m_currSnappedPt.y - m_paramHeight / 2);
					break;
				case TwoSeventy:
					pt1.setLocation(m_currSnappedPt.x - m_paramHeight / 2, m_currSnappedPt.y + m_paramWidth);
					pt2.setLocation(m_currSnappedPt.x + m_paramHeight / 2, m_currSnappedPt.y);
					break;
			}
		}

		@Override public void terminate(@NotNull ILogicOtherComponent otherComponent,
				@NotNull IDynamicGfxService dynamicGfxService)
		{
			final ISchemDiagram schemDiagram = CommonUtils.cast(getBaseDiagram(), ISchemDiagram.class);
			if (m_currSnappedPt != null && schemDiagram != null) {
				int x = m_currSnappedPt.x;
				int y = m_currSnappedPt.y;
				int w = m_paramWidth;
				int h = m_paramHeight;
				if (!m_componentType.isKindOfLengthWiseObject()) {
					final int gridSpacing = schemDiagram.getGrid().getGridSpacing();
					h = Math.max(0, h - 2 * gridSpacing);
					w = Math.max(0, w - gridSpacing) + gridSpacing;
				}
				final IDrawFactory drawFactory = FactoryMgr.getDrawFactory();
				final ITransform transform = drawFactory.createTransform();
				transform.setFromAffineTransform(m_placementAxis.getTransform());
				doGenerateParameterizedComponent(otherComponent, schemDiagram, x, y, w, h, transform);
			}
		}

		@Override public boolean isReadyForTermination()
		{
			return true;
		}

		@Override public void mouseClicked(Point currMousePoint)
		{
			final Point snapPoint = snapPoint(currMousePoint, shouldSnapToGrid());
			m_currSnappedPt = snapPoint;
		}

		public void informGraphicPhysicalState(@NotNull IGfxObjectPhysicalStateDisplayControl control)
		{
			if (m_currSnappedPt != null) {
				Point pt1 = new Point();
				Point pt2 = new Point();
				computeCorners(pt1, pt2);
				doInformGraphicPhysicalState(control, pt1, pt2, !m_placementAxis.isYAxis());
			}
		}

		@Override protected double getPlacedComponentWorldWidth()
		{
			return m_paramHeight;
		}

		@Override protected double getPlacedComponentWorldLength()
		{
			return m_paramWidth;
		}
	}

	private abstract static class AbstractDynamicParameterizedPlacementControl extends ParameterizedPlacementControl
	{

		private ListSet<Point> m_SnappedPts = new ListSet<>(2);
		private boolean m_horizontalAxis = true;
		private double placedLength = 0;
		private double placedWidth = 0;

		protected AbstractDynamicParameterizedPlacementControl(LogicOtherComponentTypeEnum type,
				@Nullable PanelLayoutLengthComponentType compSubType)
		{
			super(type, compSubType);
			if (!shouldDetermineAxisByMouseMovement(type)) {
				m_keyHandlers.put(KeyEvent.VK_R, (t) -> {
					toggleFlip();
				});
				m_keyHandlers.put(KeyEvent.VK_T, (t) -> {
					toggleFlip();
				});
			}
		}

		private boolean shouldDetermineAxisByMouseMovement(LogicOtherComponentTypeEnum type)
		{
			return type.isKindOfLengthWiseObject();
		}

		public boolean shouldSnapToGrid()
		{
			return super.shouldSnapToGrid() && m_SnappedPts.isEmpty();
		}

		private void toggleFlip()
		{
			m_horizontalAxis = !m_horizontalAxis;
		}

		protected void updateTransientGraphics()
		{
			m_transientParams.clear();
			if (!m_SnappedPts.isEmpty() && m_currSnappedPt != null) {
				Point pt1 = new Point(m_SnappedPts.get(0));
				Point pt2 = new Point(m_currSnappedPt);
				determineAxisByMouseMovement(pt1, pt2);
				constrainShapeBasedOnComponentType(pt1, pt2);
				recordLengthAndWidth(pt1, pt2);
				doUpdateTransientGraphics(pt1, pt2, m_horizontalAxis);
			}
		}

		private void recordLengthAndWidth(Point pt1, Point pt2)
		{
			final Pair<Double, Double> widthHeightPair = determineWidthHeightPair(pt1, pt2, m_horizontalAxis);
			placedLength = widthHeightPair.getKey();
			placedWidth = widthHeightPair.getValue();
		}

		private void determineAxisByMouseMovement(Point pt1, Point pt2)
		{
			if (shouldDetermineAxisByMouseMovement(m_componentType)) {
				m_horizontalAxis = Math.abs(pt1.x - pt2.x) >= Math.abs(pt1.y - pt2.y);
			}
		}

		protected abstract int adjustWidth(int width, @Nullable LengthWiseOtherComponentDimensionAdjustment adjuster);

		protected abstract int adjustHeight(int height, boolean isLogicalSubtype,
				@Nullable LengthWiseOtherComponentDimensionAdjustment adjuster);

		private void constrainShapeBasedOnComponentType(Point firstPt, Point currPt)
		{
			final IBaseDiagram phyDiagram = getBaseDiagram();
			PanelLayoutLengthComponentType compSubType = m_compSubType;
			if (compSubType != null && phyDiagram != null) {
				final LengthWiseOtherComponentDimensionAdjustment adjuster =
						LengthWiseOtherComponentDimensionAdjustment.getAdjuster(phyDiagram, compSubType);
				if (m_horizontalAxis) {
					int w = Math.abs(firstPt.x - currPt.x);
					int h = Math.abs(firstPt.y - currPt.y);
					w = adjustWidth(w, adjuster);
					h = adjustHeight(h, compSubType.isLogical(), adjuster);
					final int firstX = firstPt.x;
					final int firstY = firstPt.y + ((firstPt.y > currPt.y) ? h / 2 : -h / 2);
					final int secondX = firstPt.x + ((firstPt.x < currPt.x) ? w : -w);
					final int secondY = firstPt.y + ((firstPt.y > currPt.y) ? -h / 2 : h / 2);
					firstPt.setLocation(firstX, firstY);
					currPt.setLocation(secondX, secondY);
				}
				else {
					int w = Math.abs(firstPt.y - currPt.y);
					int h = Math.abs(firstPt.x - currPt.x);
					w = adjustWidth(w, adjuster);
					h = adjustHeight(h, compSubType.isLogical(), adjuster);
					final int firstX = firstPt.x + ((firstPt.x < currPt.x) ? -h / 2 : h / 2);
					final int firstY = firstPt.y;
					final int secondX = firstPt.x + ((firstPt.x < currPt.x) ? h / 2 : -h / 2);
					final int secondY = firstPt.y + ((firstPt.y < currPt.y) ? w : -w);
					firstPt.setLocation(firstX, firstY);
					currPt.setLocation(secondX, secondY);
				}
			}
		}

		@Override public void cleanup()
		{
			super.cleanup();
			m_SnappedPts.clear();
		}

		@Override public void terminate(@NotNull ILogicOtherComponent otherComponent,
				@NotNull IDynamicGfxService dynamicGfxService)
		{
			final ISchemDiagram schemDiagram = CommonUtils.cast(getBaseDiagram(), ISchemDiagram.class);
			if (m_SnappedPts.size() > 1 && schemDiagram != null) {
				Point pt1 = new Point(m_SnappedPts.get(0));
				Point pt2 = new Point(m_SnappedPts.get(1));
				constrainShapeBasedOnComponentType(pt1, pt2);
				int x = m_horizontalAxis ? Math.min(pt1.x, pt2.x) : (pt1.x + pt2.x) / 2;
				int y = m_horizontalAxis ? (pt1.y + pt2.y) / 2 : Math.max(pt1.y, pt2.y);
				int w = Math.abs(m_horizontalAxis ? (pt1.x - pt2.x) : (pt1.y - pt2.y));
				int h = Math.abs(m_horizontalAxis ? (pt1.y - pt2.y) : (pt1.x - pt2.x));
				if (!m_componentType.isKindOfLengthWiseObject()) {
					final int gridSpacing = schemDiagram.getGrid().getGridSpacing();
					h = Math.max(0, h - 2 * gridSpacing);
					w = Math.max(0, w - gridSpacing) + gridSpacing;
				}
				final IDrawFactory drawFactory = FactoryMgr.getDrawFactory();
				final ITransform transform = drawFactory.createTransform();
				if (!m_horizontalAxis) {
					transform.rotate(-Math.PI / 2);
				}
				doGenerateParameterizedComponent(otherComponent, schemDiagram, x, y, w, h, transform);
			}
		}

		@Override public boolean isReadyForTermination()
		{
			return m_SnappedPts.size() > 1;
		}

		@Override public void mouseClicked(Point currMousePoint)
		{
			final Point snapPoint = snapPoint(currMousePoint, shouldSnapToGrid());
			m_currSnappedPt = snapPoint;
			m_SnappedPts.add(snapPoint);
		}

		public void informGraphicPhysicalState(@NotNull IGfxObjectPhysicalStateDisplayControl control)
		{
			if (!m_SnappedPts.isEmpty() && m_currSnappedPt != null) {
				Point refPoint = m_SnappedPts.get(0);
				if (refPoint != null) {
					Point pt1 = new Point(refPoint);
					Point pt2 = new Point(m_currSnappedPt);
					constrainShapeBasedOnComponentType(pt1, pt2);
					doInformGraphicPhysicalState(control, pt1, pt2, m_horizontalAxis);
				}
			}
		}

		@NotNull public Point getConstrainedSnapPoint(@NotNull Point snapPoint)
		{
			if (!m_SnappedPts.isEmpty() && m_currSnappedPt != null) {
				Point refPoint = m_SnappedPts.get(0);
				if (refPoint != null) {
					Point result = new Point(snapPoint);
					constrainShapeBasedOnComponentType(new Point(refPoint), result);
					return result;
				}
			}
			return snapPoint;
		}

		@Override protected double getPlacedComponentWorldLength()
		{
			return placedLength;
		}

		@Override protected double getPlacedComponentWorldWidth()
		{
			return placedWidth;
		}
	}

	private static class DynamicParameterizedPlacementControl extends AbstractDynamicParameterizedPlacementControl
	{

		private DynamicParameterizedPlacementControl(LogicOtherComponentTypeEnum type,
				@Nullable PanelLayoutLengthComponentType compSubType)
		{
			super(type, compSubType);
		}

		@Override protected int adjustWidth(int width, @Nullable LengthWiseOtherComponentDimensionAdjustment adjuster)
		{
			if (adjuster != null) {
				return adjuster.getAdjustedWidthWithMinimumBound(width);
			}
			return width;
		}

		@Override protected int adjustHeight(int height, boolean isLogicalSubtype,
				@Nullable LengthWiseOtherComponentDimensionAdjustment adjuster)
		{
			if (adjuster != null && !isLogicalSubtype) {
				return adjuster.getUnitWidth();
			}
			return 2 * height;
		}
	}

	private static class ConstrainedDynamicParameterizedPlacementControl
			extends AbstractDynamicParameterizedPlacementControl
	{

		private int m_paramWidth;
		private int m_paramHeight;

		private ConstrainedDynamicParameterizedPlacementControl(LogicOtherComponentTypeEnum type,
				@Nullable PanelLayoutLengthComponentType compSubType, int paramWidth, int paramHeight)
		{
			super(type, compSubType);
			m_paramWidth = paramWidth;
			m_paramHeight = paramHeight;
		}

		@Override protected int adjustWidth(int width, @Nullable LengthWiseOtherComponentDimensionAdjustment adjuster)
		{
			if (adjuster != null) {
				return adjuster.getAdjustedWidthWithinBounds(width, m_paramWidth);
			}
			return Math.min(width, m_paramWidth);
		}

		@Override protected int adjustHeight(int height, boolean isLogicalSubtype,
				@Nullable LengthWiseOtherComponentDimensionAdjustment adjuster)
		{
			return m_paramHeight;
		}
	}

	public void informGraphicPhysicalState(@NotNull IGfxObjectPhysicalStateDisplayControl control)
	{
		m_otherCompPlacementControl.informGraphicPhysicalState(control);
	}
}