/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2020-2026 Siemens
 */

package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.IConductorRouteAction;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.DeleteHelper;
import chs.caplets.shared.Finder;
import chs.caplets.shared.ISelectedAreaCoordinates;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGrid;
import chs.cof.draw.IRectangle;
import chs.cof.draw.Rectangle;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IDecorative;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IGfxView;
import chs.cof.drawplus.IPropertiedGraphic;
import chs.cof.logical.cable.IBaseDevice;
import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IInlineJackConnector;
import chs.cof.logical.cable.IInlinePlugConnector;
import chs.cof.logical.cable.IJackConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IPlugConnector;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.ILogicSegmentContainer;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.PinListGenerationParams;
import chs.cofUtils.parameterized.PinSideCalculator;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.IParameterized;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.Location;
import chs.common.Side;
import chs.system.FactoryMgr;
import chs.utilities.ListMap;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utility.DiagramHelper;
import chs.utility.ResizeHelper;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.CoordinateHelper;
import chs.utility.helpers.ExtentHelper;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.logic.LogicUtils;
import chs.utility.preferences.PreferenceSetHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static chs.cof.draw.IGrid.GRID_SIZE;

/**
 * Utility to join schem pinlists
 */
public class JoinPinlistsHelper
{

	@Nullable protected ObjectsToBeUsedInJoin m_stitchResult;
	@Nullable protected IExtent m_stitcedPinlistExtent;
	@Nullable protected Map<IAbstractSchemPin, LocationOnMergedPinlist> m_locationsOfPins;
	@Nullable protected Map<IPinList, LocationAndExtentOfPinlistOnMergedPinlist> m_locationOfAttachedPinlists;
	@Nullable private Map<IPropertiedGraphic, LocationOnMergedPinlist> m_locationOfAttachedGraphics;
	private IGrid m_grid;
	@Nullable private ISchemDiagram m_diagram;
	private GeneratorParameters m_generatorParameters;
	@Nullable private IGfxView m_view;
	private MovePinHandler movePinHandler;
	private String joinFailureMessage;
	private Collection<IPinList> m_selectedSchemObjects;
	private boolean setHomeConditionInAnchor = false;
	@NotNull private Consumer<String> m_feedbackConsumer;

	public JoinPinlistsHelper()
	{
		m_feedbackConsumer = feedback -> {
		};
	}

	public JoinPinlistsHelper(@NotNull Consumer<String> feedbackConsumer)
	{
		m_feedbackConsumer = feedbackConsumer;
	}

	@Nullable public static IExtent getOriginalExtent(IPinList pinList, int spacing)
	{
		IParameterized parameterized = pinList.getParameterized();
		if (parameterized == null) {
			return null;
		}

		int bottomOffset = spacing;
		int topOffset = spacing;
		if (pinList.getConnectivity() instanceof IBaseDevice) {
			PinSideCalculator pinSideCalculator = PinSideCalculator.createRelative(pinList);
			for (IAbstractSchemPin pin : pinList.getAllPins()) {
				if (pinSideCalculator.getSide(pin) == Side.BOTTOM) {
					bottomOffset = 0;
				}
				if (pinSideCalculator.getSide(pin) == Side.TOP) {
					topOffset = 0;
				}
			}
		}

		IExtent parameterizedExtent = parameterized.getExtent();
		IExtent requiredExtent = FactoryMgr.getCommonFactory().constructExtent(parameterizedExtent);
		requiredExtent.invalidate();
		requiredExtent
				.setBounds(parameterizedExtent.getX(), requiredExtent.getY() - bottomOffset,
						parameterizedExtent.getWidth(),
						requiredExtent.getHeight() + topOffset + bottomOffset);

		return ExtentHelper.getAbsExtent(pinList, requiredExtent);
	}

	@NotNull public static String getType(@Nullable IPinList pinlist)
	{
		if (pinlist == null) {
			return "";
		}

		chs.cof.logical.cable.IPinList connectivity = pinlist.getConnectivity();
		if (connectivity instanceof IDevice) {
			return "device";
		}
		if (connectivity instanceof IBlockDevice) {
			return "block";
		}
		if (connectivity instanceof IPlugConnector) {
			return "plug";
		}
		if (connectivity instanceof IJackConnector) {
			return "jack";
		}
		return "";
	}

	private void reportFailure(String errorMessage)
	{
		m_feedbackConsumer.accept(errorMessage);
		//m_output.sendMessage(errorMessage, m_outputTabName, true);
		joinFailureMessage = errorMessage;
	}

	public String getJoinFailureMessage()
	{
		return joinFailureMessage;
	}

	public boolean completeEdits()
	{
		IPinList anchorPinlist = null;
		if (m_stitchResult != null) {
			anchorPinlist = m_stitchResult.getAnchorObject();
		}

		if (anchorPinlist != null) {
			Collection<IAbstractSchemPin> pinsOfAnchor =
					new HashSet<IAbstractSchemPin>(anchorPinlist.getAllPins().getCollection());
			Collection<IPinList> attachedPinlistsOfAnchor =
					new HashSet<IPinList>(anchorPinlist.getAttachedPinListObjects());

			if (setHomeConditionInAnchor) {
				ToggleHomeAction.toggleHomeForShareableDiagramObject(true, anchorPinlist);
			}

			applyEditsOnAnchorPinlist(anchorPinlist);

			moveAssociatedGraphicsToNewLocation(anchorPinlist);

			Set<IAbstractSchemPin> pinsActuallyMoved = movePinsToNewLocation(anchorPinlist, pinsOfAnchor);

			if (m_stitchResult instanceof InlinePlugJackToBeUsedInJoin) {
				InlinePlugJackToBeUsedInJoin inlinePlugJackUsedInStitc =
						(InlinePlugJackToBeUsedInJoin) m_stitchResult;
				inlinePlugJackUsedInStitc.moveConnectedPins(pinsActuallyMoved, anchorPinlist,
						anchorPinlist.getAttachedPinListObjects().iterator().next(), movePinHandler);
			}
			else {

				if (m_locationOfAttachedPinlists != null) {
					for (IPinList attachedPinlist : m_locationOfAttachedPinlists.keySet()) {
						if (!attachedPinlistsOfAnchor.contains(attachedPinlist)) {
							ILocation location = m_locationOfAttachedPinlists.get(attachedPinlist).getLocation();
							for (IPinList anchorPinlistInstance : attachedPinlist.getAttachedPinListObjects()) {
								ConnectionHelper.disconnect(anchorPinlistInstance);
							}
							ILocation newAttachedPinlistLocation = new Location(location);

							attachedPinlist.setLocation(newAttachedPinlistLocation);

							ConnectionHelper.connectPinLists(anchorPinlist, attachedPinlist, false, m_grid);
							for (IAbstractSchemPin pin : attachedPinlist.getAllPins(false)) {
								movePinHandler
										.moveJoint(pin, pin.getAbsLocation().getX(),
												pin.getAbsLocation().getY());
							}
						}
					}
				}
			}

			for (IPinList anchorPinlistInstance : m_stitchResult.getAnchorSchemInstances()) {
				if (anchorPinlistInstance == anchorPinlist) {
					continue;
				}
				DeleteHelper.getInstance()
						.delete(m_diagram, Collections.singleton(anchorPinlistInstance), false);
			}

			regenerateGfxForAffectedPinlists(Collections.singleton(anchorPinlist));
			//SP2002 - dts0101401615:Inline halves are getting disassociated schematically
			if (anchorPinlist.getConnectivity() instanceof IGenericInlineConnector) {
				regenerateGfxForAffectedPinlists(anchorPinlist.getAttachedPinListObjects(IPinList.EXCLUDE_MODULAR));
			}
			SelectSet objects = new SelectSet();
			if (m_view != null) {
				Finder finder = new Finder(m_view.getGfxContext(), objects, m_stitcedPinlistExtent, true)
				{
					@Override protected boolean excludeFromSelection(IGfxObject obj)
					{
						return !(obj instanceof IPinList);
					}
				};
				finder.visitRoot(m_diagram, 0, 0);
			}

			IConductorRouteAction routeAction = ConductorRouteAction.getInstance();
			routeAction.addPinListsForRoute(objects.getSelectedObjects(IPinList.class));
			routeAction.addConductorsForRoute(objects.getSelectedObjects(ILogicSegmentContainer.class));
			routeAction.addPinsForRoute(objects.getSelectedObjects(IAbstractSchemPin.class));
		}
		return true;
	}

	protected Set<IAbstractSchemPin> movePinsToNewLocation(IPinList anchorPinlist,
			Collection<IAbstractSchemPin> pinsOfAnchor)
	{
		Collection<IPinList> affectedPinlists = new HashSet<IPinList>();
		Set<IAbstractSchemPin> pinsMoved = new HashSet<IAbstractSchemPin>();
		if (m_locationsOfPins != null) {
			for (IAbstractSchemPin pin : m_locationsOfPins.keySet()) {
				if (pinsOfAnchor.contains(pin)) {
					continue;
				}

				ILocation location = m_locationsOfPins.get(pin).getLocation();
				IDiagramObject owner = pin.getParent();
				if (owner instanceof IPinList) {
					affectedPinlists.add((IPinList) owner);
				}
				if (m_stitchResult != null) {
					m_stitchResult.preMovePin(pin);
				}
				Collection<IPinList> attachedPinListObjects = null;
				if (owner instanceof IPinList) {
					anchorPinlist.removeAttachedObject((IPinList) owner);
					attachedPinListObjects = ((IPinList) owner).getAttachedPinListObjects();
				}

				for (Iterator<IPinList> itr = Objects.requireNonNull(attachedPinListObjects).iterator();
						itr.hasNext(); ) {
					IPinList attachedPinList = itr.next();
					if (attachedPinList.getConnectivity() instanceof IDeviceConnector) {
						// Device connectors will be regenerated in movePinToLocation() method
						// So, old instances are invalid
						itr.remove();
					}
				}
				Set<IAbstractSchemPin> modifiedPins = new HashSet<IAbstractSchemPin>();
				ListMap<IAbstractSchemPin, IAbstractSchemPin> pinMateMap =
						new ListMap<IAbstractSchemPin, IAbstractSchemPin>();
				Set<IAbstractSchemPin> processedDevicePins = new HashSet<IAbstractSchemPin>();
				Map<IAbstractSchemPin, Side> pinSidesBeforeMove = new HashMap<IAbstractSchemPin, Side>();
				Map<IPin, String> newConnPinNames = new HashMap<IPin, String>();

				movePinHandler.movePinToLocation(pin, location.getX(), location.getY(), null, anchorPinlist,
						modifiedPins,
						pinMateMap, processedDevicePins, pinSidesBeforeMove, newConnPinNames, true);
				pinsMoved.add(pin);
			}
		}
		affectedPinlists.add(anchorPinlist);
		regenerateGfxForAffectedPinlists(affectedPinlists);
		return pinsMoved;
	}

	private void moveAssociatedGraphicsToNewLocation(IPinList anchorPinlist)
	{
		Collection<IPropertiedGraphic> propertiedGraphicsOfAnchor =
				anchorPinlist.getObjects(IPropertiedGraphic.class);
		if (m_locationOfAttachedGraphics != null) {
			for (IPropertiedGraphic propertiedGraphic : m_locationOfAttachedGraphics.keySet()) {
				ILocation newLocationOfGraphic = m_locationOfAttachedGraphics.get(propertiedGraphic).getLocation();

				if (!propertiedGraphicsOfAnchor.contains(propertiedGraphic)) {
					IDiagramObject parentObject = propertiedGraphic.getParent();
					if (parentObject instanceof ICompoundObject) {
						((ICompoundObject) parentObject).removeObject(propertiedGraphic);
					}
					anchorPinlist.addObject(propertiedGraphic);
				}

				ILocation location =
						CoordinateHelper.getRelLocation(propertiedGraphic, newLocationOfGraphic.getX(),
								newLocationOfGraphic.getY());
				ILocation currentLocation = propertiedGraphic.getLocation();

				propertiedGraphic.move(location.getX() - currentLocation.getX(),
						location.getY() - currentLocation.getY() + m_grid.getGridSpacing());
			}
		}
	}

	protected void applyEditsOnAnchorPinlist(IPinList anchorPinlist)
	{

		int locationYOffset = 0;
		int locationXOffset = 0;
		if (m_stitchResult != null) {
			if (m_stitchResult.isVerticalPlacementOfPins(m_stitchResult.getAnchorObject())) {
				if (!(anchorPinlist.getConnectivity() instanceof IBaseDevice)) {
					locationYOffset = m_grid.getGridSpacing();
				}
			}
			else if (m_stitchResult.isHorizontalPlacementOfPins(m_stitchResult.getAnchorObject())) {
				if (!(anchorPinlist.getConnectivity() instanceof IBaseDevice)) {
					locationXOffset = m_grid.getGridSpacing();
				}
			}
		}

		ILocation newLowerLocation = new Location(
				(m_stitcedPinlistExtent != null ? m_stitcedPinlistExtent.getX() : 0) + locationXOffset,
				(m_stitcedPinlistExtent != null ? m_stitcedPinlistExtent.getY() : 0) + locationYOffset);
		ILocation newUpperLocation = new Location(
				(m_stitcedPinlistExtent != null ? m_stitcedPinlistExtent.getRight() : 0) - locationXOffset,
				(m_stitcedPinlistExtent != null ? m_stitcedPinlistExtent.getTop() : 0) - locationYOffset);
		IExtent extentOfAnchorPinlist = ExtentHelper.getAbsExtent(anchorPinlist);

		int originalX = extentOfAnchorPinlist.getX();
		int originalY = extentOfAnchorPinlist.getY();

		int newLowerLocationX = newLowerLocation.getX();
		int newLowerLocationY = newLowerLocation.getY();

		IExtent initialExtent = LogicUtils.getResizableInitialExtent(anchorPinlist);
		IExtent newExtent = FactoryMgr.getCommonFactory().constructExtent(initialExtent);
		if (((newLowerLocationX - originalX) != 0) || ((newLowerLocationY - originalY) != 0)) {
			IRectangle rectangle = new Rectangle(0, 0, 1,
					1);
			ResizePinlistCornerAdjustCalc
					.adjustLowerLeft(new Point(newLowerLocationX - originalX, newLowerLocationY - originalY),
							newExtent);
			ResizeHelper.applyExtentToRectangle(newExtent, anchorPinlist.getTransform(), rectangle);
			GeneratorParameters genParams =
					new GeneratorParameters(m_diagram.getGrid(), PreferenceSetHelper.getStyleSet(m_diagram));
			ResizeHelper resizeHelper = new ResizeHelper(anchorPinlist, genParams);
			resizeHelper.resizePinList(rectangle, m_grid, initialExtent);
		}
		initialExtent = LogicUtils.getResizableInitialExtent(anchorPinlist);
		if (((newUpperLocation.getX() - extentOfAnchorPinlist.getRight()) != 0) ||
				((newUpperLocation.getY() - extentOfAnchorPinlist.getTop()) != 0)) {

			newExtent = FactoryMgr.getCommonFactory().constructExtent(initialExtent);
			IRectangle rectangle = new Rectangle(0, 0, 1,
					1);
			ResizePinlistCornerAdjustCalc.adjustUpperRight(
					(new Point(newUpperLocation.getX() - extentOfAnchorPinlist.getRight(),
							newUpperLocation.getY() - extentOfAnchorPinlist.getTop())),
					newExtent);
			ResizeHelper.applyExtentToRectangle(newExtent, anchorPinlist.getTransform(), rectangle);
			GeneratorParameters genParams =
					new GeneratorParameters(m_diagram.getGrid(), PreferenceSetHelper.getStyleSet(m_diagram));
			ResizeHelper resizeHelper = new ResizeHelper(anchorPinlist, genParams);
			resizeHelper.resizePinList(rectangle, m_grid, initialExtent);
		}
	}

	public LogicObjectAcceptance checkValidLogicObjectForStitch(IUIDObject uidObject)
	{
		ILogicObject logicObject = ReferenceHelper.reduceToLogicObject(uidObject.getUID());
		if (logicObject == null) {
			return LogicObjectAcceptance.IGNORE;
		}

		if (logicObject instanceof ISplice) {
			//if a splice is in the selection then this will be unrelated selection for stich of a pinlist.
			return LogicObjectAcceptance.FAILED;
		}
		if (logicObject instanceof IConductor) {

			return LogicObjectAcceptance.FAILED;
		}
		if (logicObject instanceof IConnector) {
			IConnector connector = (IConnector) logicObject;
			if (connector.isRingTerminal()) {
				return LogicObjectAcceptance.FAILED;
			}
		}
		if (checkValidConnectivityObject(logicObject)) {
			return LogicObjectAcceptance.ACCEPT;
		}

		return LogicObjectAcceptance.IGNORE;
	}

	protected boolean createAndValidateLocationOfAnchorPins()
	{
		if (m_stitchResult != null) {
			m_locationsOfPins = getLocationOfAnchorPins(m_stitchResult, m_stitcedPinlistExtent,
					m_stitchResult.isHorizontalPlacementOfPins(m_stitchResult.getAnchorObject()));
		}
		if (m_locationsOfPins == null) {
			return false;
		}

		if (areOverlappingPinsPresent(m_locationsOfPins)) {
			reportFailure(ResourceMgr.getString(JoinPinlistsAction.class, "JoinPinlistsAction.OverlappingPins"));
			return false;
		}
		boolean supportsPinsOnSingleSide = false;
		if (m_stitchResult != null) {
			supportsPinsOnSingleSide =
					Objects.requireNonNull(m_stitchResult.getAnchorObject()).getConnectivity() instanceof IConnector;
		}
		if (!arePinsOnAcceptableSideAndBorderOfPinlist(supportsPinsOnSingleSide, m_locationsOfPins,
				m_stitcedPinlistExtent)) {
			reportFailure("All the pins are not on border");
			return false;
		}
		return true;
	}

	protected boolean createAndValidateLocationOfAttachedPinlists()
	{
		if (m_locationsOfPins == null && m_stitchResult != null) {
			m_locationsOfPins = getLocationOfAnchorPins(m_stitchResult, m_stitcedPinlistExtent,
					m_stitchResult.isHorizontalPlacementOfPins(m_stitchResult.getAnchorObject()));
		}
		IPinList anchor = m_stitchResult != null ? m_stitchResult.getAnchorObject() : null;
		assert m_stitchResult != null;
		m_locationOfAttachedPinlists = getLocationOfAttachedPinlists(m_stitchResult, m_stitcedPinlistExtent);
		if (m_locationOfAttachedPinlists == null) {
			return false;
		}

		if (!m_stitchResult.areAttachedPinlistsOfCorrectSize(m_locationOfAttachedPinlists)) {
			reportFailure(m_stitchResult.getErrorMessage());
			return false;
		}
		String checkOverlappingPinsAndAttachedObjects =
				areOverlappingPinsAndAttachedObjectsIncompatible(m_locationOfAttachedPinlists, m_locationsOfPins,
						anchor);
		if (!checkOverlappingPinsAndAttachedObjects.isEmpty()) {
			reportFailure(checkOverlappingPinsAndAttachedObjects);
			return false;
		}

		return true;
	}

	public boolean isStitchPossibleOnPinlistsSelected(Collection<IPinList> selectedObjects)
	{
		if (!initialize(selectedObjects)) {
			return false;
		}
		String anchorCanBeCreatedWithExtents =
				m_stitchResult != null ? m_stitchResult.verifyAnchorCanBeCreatedOnTheExtent(m_stitcedPinlistExtent) :
						"";
		if (!anchorCanBeCreatedWithExtents.isEmpty()) {
			reportFailure(anchorCanBeCreatedWithExtents);
			return false;
		}
		if (!createAndValidateLocationOfAnchorPins()) {
			return false;
		}

		if (m_stitchResult != null && m_stitchResult.areAttachedPinlistsHandledInResize()) {
			m_locationOfAttachedPinlists = Collections.emptyMap();
		}
		else {
			if (!createAndValidateLocationOfAttachedPinlists()) {
				return false;
			}
		}

		m_locationOfAttachedGraphics = getLocationOfAttachedAssociatedgraphics(m_stitchResult, m_stitcedPinlistExtent,
				m_stitchResult != null ? m_stitchResult.isHorizontalPlacementOfPins(m_stitchResult.getAnchorObject()) :
						false);
		return true;
	}

	protected boolean initialize(Collection<IPinList> selectedObjects)
	{
		m_selectedSchemObjects = selectedObjects;
		if (!getDiagramDetails(selectedObjects)) {
			return false;
		}

		m_stitchResult = getResultObjectForStitch(selectedObjects);
		if (m_stitchResult == null) {
			return false;
		}
		if (!m_stitchResult.getErrorMessage().isEmpty()) {
			reportFailure(m_stitchResult.getErrorMessage());
			return false;
		}

		movePinHandler = new MovePinHandler(m_grid.getGridSpacing(), true, null);
		IPinList anchorPinlist = m_stitchResult.getAnchorObject();

		if (!m_stitchResult.getErrorMessage().isEmpty()) {
			reportFailure(m_stitchResult.getErrorMessage());
			return false;
		}

		if (anchorPinlist == null) {
			reportFailure(ResourceMgr.getString(JoinPinlistsAction.class, "JoinPinlistAction.noObjectToJoin"));
			return false;
		}

		if (!m_stitchResult.verifyPinlistPairs()) {
			reportFailure(m_stitchResult.getErrorMessage());
			return false;
		}

		m_stitcedPinlistExtent = m_stitchResult.determineExtentOfNewStitchedObject();
		if (m_stitcedPinlistExtent == null) {
			reportFailure(m_stitchResult.getErrorMessage());
			return false;
		}

		setHomeConditionInAnchor = doesAnyObjectHasHomeConditionTrue(selectedObjects);

		return true;
	}

	private boolean getDiagramDetails(Collection<IPinList> selectedObjects)
	{
		IBaseDiagram diagram = getDiagram();
		if (diagram instanceof ISchemDiagram) {
			m_diagram = (ISchemDiagram) diagram;
		}
		for (IPinList aPinListInSelection : selectedObjects) {
			ISchemDiagram diagramOfSelectedObject = DiagramHelper.getDiagram(aPinListInSelection);
			if (m_diagram != diagramOfSelectedObject) {
				reportFailure(ResourceMgr.getString(JoinPinlistsAction.class, "JoinPinlistAction.NotOnActiveDiagram"));
				m_diagram = null;
				return false;
			}
		}
		if (m_diagram == null) {
			reportFailure(ResourceMgr.getString(JoinPinlistsAction.class, "JoinPinlistAction.NotOnActiveDiagram"));
			return false;
		}
		m_grid = m_diagram.getGrid();
		m_view = (IGfxView) (CAFUtils.getInstance().getViewForDiagram(m_diagram));
		m_generatorParameters = DiagramHelper.createGeneratorParameters(m_diagram);
		return true;
	}

	@Nullable protected IBaseDiagram getDiagram()
	{
		return CAFUtils.getInstance().getActiveDiagram();
	}

	protected boolean areOverlappingPinsPresent(Map<IAbstractSchemPin, LocationOnMergedPinlist> locationsOfPins)
	{
		Set<String> locationVals = new HashSet<>();
		for (LocationOnMergedPinlist location : locationsOfPins.values()) {

			if (!locationVals.add(location.getLocation().toString())) {
				return true;
			}
		}
		return false;
	}

	private boolean arePinsOnAcceptableSideAndBorderOfPinlist(boolean supportsPinOnSingleSide,
			final Map<IAbstractSchemPin, LocationOnMergedPinlist> locationOfPins,
			IExtent newExtent)
	{

		Side currentSide = null;
		for (LocationOnMergedPinlist locationOfAPin : locationOfPins.values()) {
			if (locationOfAPin.getSide() == Side.BOTTOM || locationOfAPin.getSide() == Side.TOP) {
				if (locationOfAPin.getLocation().getY() != newExtent.getY() &&
						locationOfAPin.getLocation().getY() != newExtent.getTop()) {
					return false;
				}
			}
			else if (locationOfAPin.getSide() == Side.LEFT || locationOfAPin.getSide() == Side.RIGHT) {
				if (locationOfAPin.getLocation().getX() != newExtent.getX() &&
						locationOfAPin.getLocation().getX() != newExtent.getRight()) {
					return false;
				}
			}
			if (supportsPinOnSingleSide) {
				if (currentSide == null) {
					currentSide = locationOfAPin.getSide();
				}
				else if (currentSide != locationOfAPin.getSide()) {
					return false;
				}
			}
		}
		return true;
	}

	private String areOverlappingPinsAndAttachedObjectsIncompatible(
			final Map<IPinList, LocationAndExtentOfPinlistOnMergedPinlist> attachedPinlistLocation,
			@Nullable final Map<IAbstractSchemPin, LocationOnMergedPinlist> locationOfPins, @Nullable IPinList anchor)

	{
		List<IPinList> leftPinlists = new ArrayList<>();
		List<IPinList> rightPinlists = new ArrayList<IPinList>();
		List<IPinList> topPinlists = new ArrayList<>();
		List<IPinList> bottomPinlists = new ArrayList<IPinList>();

		for (IPinList pinList : attachedPinlistLocation.keySet()) {
			LocationAndExtentOfPinlistOnMergedPinlist locationOfPinlistOnMergedPinlist =
					attachedPinlistLocation.get(pinList);
			if (locationOfPinlistOnMergedPinlist.getSide() == Side.LEFT) {
				leftPinlists.add(pinList);
			}
			else if (locationOfPinlistOnMergedPinlist.getSide() == Side.RIGHT) {
				rightPinlists.add(pinList);
			}
			else if (locationOfPinlistOnMergedPinlist.getSide() == Side.TOP) {
				topPinlists.add(pinList);
			}
			else if (locationOfPinlistOnMergedPinlist.getSide() == Side.BOTTOM) {
				bottomPinlists.add(pinList);
			}
		}
		List<IAbstractSchemPin> topPins = new ArrayList<IAbstractSchemPin>();
		List<IAbstractSchemPin> bottomPins = new ArrayList<IAbstractSchemPin>();
		List<IAbstractSchemPin> leftPins = new ArrayList<IAbstractSchemPin>();
		List<IAbstractSchemPin> rightPins = new ArrayList<IAbstractSchemPin>();
		if (locationOfPins != null) {
			for (IAbstractSchemPin pin : locationOfPins.keySet()) {
				LocationOnMergedPinlist locationOnMergedPinlist = locationOfPins.get(pin);
				if (locationOnMergedPinlist.getSide() == Side.LEFT) {
					leftPins.add(pin);
				}
				else if (locationOnMergedPinlist.getSide() == Side.RIGHT) {
					rightPins.add(pin);
				}
				else if (locationOnMergedPinlist.getSide() == Side.TOP) {
					topPins.add(pin);
				}
				else if (locationOnMergedPinlist.getSide() == Side.BOTTOM) {
					bottomPins.add(pin);
				}
			}
		}
		Comparator<IPinList> verticalComparator = new Comparator<IPinList>()
		{

			@Override public int compare(IPinList o1, IPinList o2)
			{
				int o1Y = attachedPinlistLocation.get(o1).getExtent().getY();
				int o2Y = attachedPinlistLocation.get(o2).getExtent().getY();
				return o1Y - o2Y;
			}
		};
		Comparator<IAbstractSchemPin> verticalPinComparator = new Comparator<IAbstractSchemPin>()
		{

			@Override public int compare(IAbstractSchemPin o1, IAbstractSchemPin o2)
			{
				int o1Y = 0;
				int o2Y = 0;
				if (locationOfPins != null) {
					o1Y = locationOfPins.get(o1).getLocation().getY();
					o2Y = locationOfPins.get(o2).getLocation().getY();
				}

				return o1Y - o2Y;
			}
		};
		Comparator<IPinList> horizontalComparator = new Comparator<IPinList>()
		{

			@Override public int compare(IPinList o1, IPinList o2)
			{
				int o1Y = attachedPinlistLocation.get(o1).getExtent().getX();
				int o2Y = attachedPinlistLocation.get(o2).getExtent().getX();
				return o1Y - o2Y;
			}
		};
		Comparator<IAbstractSchemPin> horizontalPinComparator = new Comparator<IAbstractSchemPin>()
		{

			@Override public int compare(IAbstractSchemPin o1, IAbstractSchemPin o2)
			{
				int o1Y = 0;
				int o2Y = 0;
				if (locationOfPins != null) {
					o1Y = locationOfPins.get(o1).getLocation().getX();
					o2Y = locationOfPins.get(o2).getLocation().getX();
				}

				return o1Y - o2Y;
			}
		};
		Map<List<IPinList>, List<IAbstractSchemPin>> verticalPinlistsGroup =
				new HashMap<List<IPinList>, List<IAbstractSchemPin>>(2);
		Map<List<IPinList>, List<IAbstractSchemPin>> horizontalPinlistsGroup =
				new HashMap<List<IPinList>, List<IAbstractSchemPin>>(2);

		if (m_stitchResult != null && m_stitchResult.arePinsOnBothSidesOfAnchor()) {
			verticalPinlistsGroup.put(leftPinlists, leftPins);
			verticalPinlistsGroup.put(rightPinlists, rightPins);
			horizontalPinlistsGroup.put(topPinlists, topPins);
			horizontalPinlistsGroup.put(bottomPinlists, bottomPins);
		}
		else {
			updateMapWithCorrespondingPinsAndPinlists(leftPinlists, rightPinlists, leftPins, rightPins,
					verticalPinlistsGroup);
			updateMapWithCorrespondingPinsAndPinlists(topPinlists, bottomPinlists, topPins, bottomPins,
					horizontalPinlistsGroup);
		}

		String errorMessage =
				validatePinlistsAndPinsOnASide(attachedPinlistLocation, locationOfPins, verticalComparator,
						verticalPinComparator,
						verticalPinlistsGroup, true);
		if (!errorMessage.isEmpty()) {
			return errorMessage;
		}
		errorMessage = validatePinlistsAndPinsOnASide(attachedPinlistLocation, locationOfPins, horizontalComparator,
				horizontalPinComparator,
				horizontalPinlistsGroup, false);

		return errorMessage;
	}

	private String validatePinlistsAndPinsOnASide(
			Map<IPinList, LocationAndExtentOfPinlistOnMergedPinlist> attachedPinlistLocation,
			@Nullable Map<IAbstractSchemPin, LocationOnMergedPinlist> locationOfPins,
			Comparator<IPinList> verticalComparator, Comparator<IAbstractSchemPin> verticalPinComparator,
			Map<List<IPinList>, List<IAbstractSchemPin>> verticalPinlistsGroup, boolean isVertical)
	{

		for (List<IPinList> attachedPinlistsOnASide : verticalPinlistsGroup.keySet()) {
			List<IAbstractSchemPin> pinsOnMatchingSide = verticalPinlistsGroup.get(attachedPinlistsOnASide);
			Collections.sort(attachedPinlistsOnASide, verticalComparator);
			Collections.sort(pinsOnMatchingSide, verticalPinComparator);

			if (isVertical) {
				String overalppingAttachedPinlist =
						verifyVerticalAttachedPinLists(attachedPinlistLocation, attachedPinlistsOnASide);
				if (!overalppingAttachedPinlist.isEmpty()) {

					return overalppingAttachedPinlist;
				}
				String compatibilityOfAnchorPinsAndAttachedPinlists =
						areVerticalPinAndAttachedPinListAtLocationIncompatible(attachedPinlistLocation,
								locationOfPins,
								attachedPinlistsOnASide,
								pinsOnMatchingSide);
				if (!compatibilityOfAnchorPinsAndAttachedPinlists.isEmpty()) {
					return compatibilityOfAnchorPinsAndAttachedPinlists;
				}
			}
			else {
				String overlappingAttachedPinList =
						verifyHorizontalAttachedPinLists(attachedPinlistLocation, attachedPinlistsOnASide);
				if (!overlappingAttachedPinList.isEmpty()) {
					return overlappingAttachedPinList;
				}
				String compatibilityOfAnchorPinsAndAttachedPinlists =
						areHorizontalPinAndAttachedPinListAtLocationIncompatible(attachedPinlistLocation,
								locationOfPins,
								attachedPinlistsOnASide,
								pinsOnMatchingSide);
				if (!compatibilityOfAnchorPinsAndAttachedPinlists.isEmpty()) {
					return compatibilityOfAnchorPinsAndAttachedPinlists;
				}
			}
		}
		return "";
	}

	private void updateMapWithCorrespondingPinsAndPinlists(List<IPinList> leftPinlists, List<IPinList> rightPinlists,
			List<IAbstractSchemPin> leftPins, List<IAbstractSchemPin> rightPins,
			Map<List<IPinList>, List<IAbstractSchemPin>> verticalPinlistsGroup)
	{
		List<IPinList> verticalAttachedPinlists =
				new ArrayList<IPinList>(leftPinlists.size() + rightPinlists.size());
		verticalAttachedPinlists.addAll(leftPinlists);
		verticalAttachedPinlists.addAll(rightPinlists);
		List<IAbstractSchemPin> verticalPins = new ArrayList<IAbstractSchemPin>(leftPins.size() + rightPins.size());
		verticalPins.addAll(leftPins);
		verticalPins.addAll(rightPins);
		verticalPinlistsGroup.put(verticalAttachedPinlists, verticalPins);
	}

	/**
	 * For all the attached pinLists on the left and right side of the to be joined pinList
	 * the overlap of the attached pinLists is calculated.
	 * If the pinLists are already overlapping (which are identified by the same attached pinList) then ignore them.
	 * However if there is a any additional overlap then the join will fail.
	 *
	 * @param attachedPinListLocation the location and extent of all attached pinLists
	 * @param sortedOnY               attached pinLists sorted by Y
	 * @return the join failure message, empty if success
	 */
	@NotNull private String verifyVerticalAttachedPinLists(
			Map<IPinList, LocationAndExtentOfPinlistOnMergedPinlist> attachedPinListLocation,
			Collection<IPinList> sortedOnY)
	{
		Pair<IPinList, Integer> previousPinListTopY = null;
		for (IPinList thisPinList : sortedOnY) {
			IExtent extentOfPinListOnMergedPinList =
					getExtentOfPinListOnMergedPinList(attachedPinListLocation, thisPinList);
			int thisPinListBottomY = extentOfPinListOnMergedPinList.getY();
			int thisPinListTopY = extentOfPinListOnMergedPinList.getTop();

			if (previousPinListTopY != null) {
				int previousTopYVal = previousPinListTopY.getSecond();
				if (!arePinListsAttachedToSamePinList(attachedPinListLocation, previousPinListTopY.getFirst(),
						thisPinList) &&
						isThisPinListOverlappingWithPreviousPinList(thisPinListBottomY, previousTopYVal)) {
					return ResourceMgr
							.getString(JoinPinlistsAction.class,
									"JoinPinlistAction.OveralppingAttachedPinlist",
									getType(thisPinList));
				}
				if (thisPinListTopY > previousTopYVal) {
					previousPinListTopY = new Pair<>(thisPinList, thisPinListTopY);
				}
				continue;
			}
			previousPinListTopY = new Pair<>(thisPinList, thisPinListTopY);
		}
		return "";
	}

	/**
	 * For all the attached pinLists on the top and bottom side of the to be joined pinList
	 * the overlap of the attached pinLists is calculated.
	 * If the pinLists are already overlapping (which are identified by the same attached pinList) then ignore them.
	 * However if there is a any additional overlap then the join will fail.
	 *
	 * @param attachedPinListLocation the location and extent of all attached pinLists
	 * @param sortedOnX               attached pinLists sorted by X
	 * @return the join failure message, empty if success
	 */
	@NotNull private String verifyHorizontalAttachedPinLists(
			Map<IPinList, LocationAndExtentOfPinlistOnMergedPinlist> attachedPinListLocation,
			Collection<IPinList> sortedOnX)
	{
		Pair<IPinList, Integer> previousPinListRightX = null;
		for (IPinList thisPinList : sortedOnX) {
			IExtent extentOfPinListOnMergedPinList =
					getExtentOfPinListOnMergedPinList(attachedPinListLocation, thisPinList);
			int thisPinListLeftX = extentOfPinListOnMergedPinList.getX();
			int thisPinListRightX = extentOfPinListOnMergedPinList.getRight();

			if (previousPinListRightX != null) {
				int previousRightXVal = previousPinListRightX.getSecond();
				if (!arePinListsAttachedToSamePinList(attachedPinListLocation,
						previousPinListRightX.getFirst(), thisPinList) &&
						isThisPinListOverlappingWithPreviousPinList(thisPinListLeftX, previousRightXVal)) {
					return ResourceMgr
							.getString(JoinPinlistsAction.class,
									"JoinPinlistAction.OveralppingAttachedPinlist",
									getType(thisPinList));
				}
				if (thisPinListRightX > previousRightXVal) {
					previousPinListRightX = new Pair<>(thisPinList, thisPinListRightX);
				}
				continue;
			}
			previousPinListRightX = new Pair<>(thisPinList, thisPinListRightX);
		}
		return "";
	}

	private IExtent getExtentOfPinListOnMergedPinList(
			Map<IPinList, LocationAndExtentOfPinlistOnMergedPinlist> attachedPinListLocation, @NotNull IPinList pinList)
	{
		assert attachedPinListLocation != null;
		LocationAndExtentOfPinlistOnMergedPinlist locAndExtentOfThisPLOnMergedPL =
				attachedPinListLocation.get(pinList);
		return locAndExtentOfThisPLOnMergedPL.getExtent();
	}

	private static boolean arePinListsAttachedToSamePinList(
			Map<IPinList, LocationAndExtentOfPinlistOnMergedPinlist> attachedPinListLocation,
			@NotNull IPinList previousPinList, @NotNull IPinList thisPinList)
	{
		IUID previousPinListOriginalAttachedPinList = attachedPinListLocation.get(previousPinList)
				.getOriginalAttachedPinList();
		IUID thisPinListOriginalAttachedPinList = attachedPinListLocation.get(thisPinList)
				.getOriginalAttachedPinList();
		return Objects.equals(thisPinListOriginalAttachedPinList, previousPinListOriginalAttachedPinList);
	}

	/**
	 * Iterate over the schemPins of the to be joined pinList from lowest value to top value and
	 * make sure that there are no attached connectors overlapping the pin that it does not mate with.
	 * If the data already has such connectors then ignore them for overlap calculation.
	 * Join will not additionally create any such overlaps.
	 *
	 * @param thisPinListCornerExtentVal     thisPinList corner extent value
	 * @param previousPinListCornerExtentVal previousPinList corner extent value
	 * @return are pinLists overlapping
	 */
	private boolean isThisPinListOverlappingWithPreviousPinList(int thisPinListCornerExtentVal,
			int previousPinListCornerExtentVal)
	{
		return (previousPinListCornerExtentVal - (m_diagram != null ? m_diagram.getGrid().getGridSpacing() : IGrid.GRID_SIZE)) >
				thisPinListCornerExtentVal;
	}

	@NotNull private String areVerticalPinAndAttachedPinListAtLocationIncompatible(
			Map<IPinList, LocationAndExtentOfPinlistOnMergedPinlist> attachedPinListLocation,
			Map<IAbstractSchemPin, LocationOnMergedPinlist> locationOfPins,
			List<IPinList> pinListsOnOneOfVerticalSide,
			List<IAbstractSchemPin> pinsOnOneOfVerticalSide)
	{
		if (pinListsOnOneOfVerticalSide.isEmpty() || pinsOnOneOfVerticalSide.isEmpty()) {
			return "";
		}
		Iterator<IAbstractSchemPin> leftPinIter = pinsOnOneOfVerticalSide.iterator();
		Iterator<IPinList> leftPinListIter = pinListsOnOneOfVerticalSide.iterator();
		IAbstractSchemPin pin = leftPinIter.next();
		IPinList pinList = leftPinListIter.next();
		while (pin != null && pinList != null) {
			IPinList anchorPinListInstance = getAnchorPinListInstance(pin);

			assert locationOfPins != null;
			if (isPinYGreaterThanPinListY(attachedPinListLocation, locationOfPins, pin, pinList) &&
					isPinYLesserThanPinListTop(attachedPinListLocation, locationOfPins, pin, pinList)) {
				assert m_stitchResult != null;
				if (!arePinParentAndPinListAttachedToSamePinList(attachedPinListLocation, pin, pinList) &&
						!m_stitchResult.validatePinAndAttachedPinlist(pin, pinList)) {
					return ResourceMgr
							.getString(JoinPinlistsAction.class,
									"JoinPinlistAction.OveralppingPinsAndAttachedPinlist",
									getType(anchorPinListInstance), getType(pinList));
				}
				pin = leftPinIter.hasNext() ? leftPinIter.next() : null;
			}
			else if (!isPinYGreaterThanPinListY(attachedPinListLocation, locationOfPins, pin, pinList)) {
				pin = leftPinIter.hasNext() ? leftPinIter.next() : null;
			}
			else {
				pinList = leftPinListIter.hasNext() ? leftPinListIter.next() : null;
			}
		}
		return "";
	}

	@NotNull private String areHorizontalPinAndAttachedPinListAtLocationIncompatible(
			Map<IPinList, LocationAndExtentOfPinlistOnMergedPinlist> attachedPinListLocation,
			Map<IAbstractSchemPin, LocationOnMergedPinlist> locationOfPins,
			List<IPinList> pinListsOnOneOfHorizontalSide,
			List<IAbstractSchemPin> pinsOnOneOfHorizontalSide)
	{
		if (pinListsOnOneOfHorizontalSide.isEmpty() || pinsOnOneOfHorizontalSide.isEmpty()) {
			return "";
		}
		Iterator<IAbstractSchemPin> horizontalPinIter = pinsOnOneOfHorizontalSide.iterator();
		Iterator<IPinList> horizontalPinListIter = pinListsOnOneOfHorizontalSide.iterator();
		IAbstractSchemPin pin = horizontalPinIter.next();
		IPinList pinList = horizontalPinListIter.next();
		while (pin != null && pinList != null) {
			IPinList anchorPinListInstance = getAnchorPinListInstance(pin);
			assert locationOfPins != null;
			if (isPinXGreaterThanPinListX(attachedPinListLocation, locationOfPins, pin, pinList) &&
					isPinXLesserThanPinListRight(attachedPinListLocation, locationOfPins, pin, pinList)) {
				assert m_stitchResult != null;
				if (!arePinParentAndPinListAttachedToSamePinList(attachedPinListLocation, pin, pinList) &&
						!m_stitchResult.validatePinAndAttachedPinlist(pin, pinList)) {
					return ResourceMgr
							.getString(JoinPinlistsAction.class,
									"JoinPinlistAction.OveralppingPinsAndAttachedPinlist",
									getType(anchorPinListInstance), getType(pinList));
				}
				pin = horizontalPinIter.hasNext() ? horizontalPinIter.next() : null;
			}
			else if (!isPinXGreaterThanPinListX(attachedPinListLocation, locationOfPins, pin, pinList)) {
				pin = horizontalPinIter.hasNext() ? horizontalPinIter.next() : null;
			}
			else {
				pinList = horizontalPinListIter.hasNext() ? horizontalPinListIter.next() : null;
			}
		}
		return "";
	}

	@Nullable private static IPinList getAnchorPinListInstance(@NotNull IAbstractSchemPin pin)
	{
		IDiagramObject diagramObject = pin.getParent();
		return (diagramObject instanceof IPinList) ? (IPinList) diagramObject : null;
	}

	private static boolean arePinParentAndPinListAttachedToSamePinList(
			Map<IPinList, LocationAndExtentOfPinlistOnMergedPinlist> attachedPinListLocation,
			@NotNull IAbstractSchemPin pin, @NotNull IPinList pinList)
	{
		IDiagramObject pinOwner = pin.getParent();
		assert pinOwner != null;
		return pinOwner.getUID().equals(attachedPinListLocation.get(pinList).getOriginalAttachedPinList());
	}

	private static boolean isPinYGreaterThanPinListY(
			Map<IPinList, LocationAndExtentOfPinlistOnMergedPinlist> attachedPinListLocation,
			Map<IAbstractSchemPin, LocationOnMergedPinlist> locationOfPins, @NotNull IAbstractSchemPin pin,
			@NotNull IPinList pinList)
	{
		return locationOfPins.get(pin).getLocation().getY() >
				attachedPinListLocation.get(pinList).getExtent().getY();
	}

	private static boolean isPinYLesserThanPinListTop(
			Map<IPinList, LocationAndExtentOfPinlistOnMergedPinlist> attachedPinListLocation,
			Map<IAbstractSchemPin, LocationOnMergedPinlist> locationOfPins, @NotNull IAbstractSchemPin pin,
			@NotNull IPinList pinList)
	{
		return locationOfPins.get(pin).getLocation().getY() <
				attachedPinListLocation.get(pinList).getExtent().getTop();
	}

	private static boolean isPinXGreaterThanPinListX(
			Map<IPinList, LocationAndExtentOfPinlistOnMergedPinlist> attachedPinListLocation,
			Map<IAbstractSchemPin, LocationOnMergedPinlist> locationOfPins, @NotNull IAbstractSchemPin pin,
			@NotNull IPinList pinList)
	{
		return locationOfPins.get(pin).getLocation().getX() >
				attachedPinListLocation.get(pinList).getExtent().getX();
	}

	private static boolean isPinXLesserThanPinListRight(
			Map<IPinList, LocationAndExtentOfPinlistOnMergedPinlist> attachedPinListLocation,
			Map<IAbstractSchemPin, LocationOnMergedPinlist> locationOfPins, @NotNull IAbstractSchemPin pin,
			@NotNull IPinList pinList)
	{
		return locationOfPins.get(pin).getLocation().getX() <
				attachedPinListLocation.get(pinList).getExtent().getRight();
	}

	@Nullable
	protected Map<IAbstractSchemPin, LocationOnMergedPinlist> getLocationOfAnchorPins(ObjectsToBeUsedInJoin result,
			IExtent newExtent, boolean isHorizontalPinPlacement)
	{
		return getLocationOfAnObjectTypeInUnion(newExtent, getSortedSchemInstances(result, isHorizontalPinPlacement),
				isHorizontalPinPlacement,
				IAbstractSchemPin.class, false);
	}

	@Nullable private Collection<IPinList> getSortedSchemInstances(ObjectsToBeUsedInJoin result,
			boolean isHorizontalPinPlacement)
	{
		List<Pair<IPinList, IPinList>> sortedSchemInstances;
		if (isHorizontalPinPlacement) {
			sortedSchemInstances = result.sortPinlistInstancesOnX();
		}
		else {
			sortedSchemInstances = result.sortPinlistInstancesOnY();
		}
		if (sortedSchemInstances == null) {
			return null;
		}

		Collection<IPinList> anchorSchemInstances = new ArrayList<IPinList>(sortedSchemInstances.size());
		for (Pair<IPinList, IPinList> pairOfPinlist : sortedSchemInstances) {
			anchorSchemInstances.add(result.getInstanceOfAnchorObject(pairOfPinlist));
		}
		return anchorSchemInstances;
	}

	@Nullable private Map<IPropertiedGraphic, LocationOnMergedPinlist> getLocationOfAttachedAssociatedgraphics(
			ObjectsToBeUsedInJoin result,
			IExtent newExtent, boolean isHorizontalPinPlacement)
	{

		return getLocationOfAnObjectTypeInUnion(newExtent, getSortedSchemInstances(result, isHorizontalPinPlacement),
				isHorizontalPinPlacement,
				IPropertiedGraphic.class, true);
	}

	@Nullable private <T extends IDiagramObject> Map<T, LocationOnMergedPinlist> getLocationOfAnObjectTypeInUnion(
			IExtent newExtent, @Nullable Collection<IPinList> anchorSchemInstances,
			boolean isHorizontalPlacement, Class<T> type, boolean originReference)
	{
		if (anchorSchemInstances == null) {
			return null;
		}
		Map<T, LocationOnMergedPinlist> locationOfPin = new HashMap<T, LocationOnMergedPinlist>();
		Set<IPinList> pinlistInstances = new HashSet<>(anchorSchemInstances.size());
		for (IPinList pinList : anchorSchemInstances) {
			if (pinlistInstances.contains(pinList)) {
				continue;
			}
			pinlistInstances.add(pinList);
			IExtent ownerExtent = getOriginalExtent(pinList, m_generatorParameters.getSpacing());

			int xOffsetForPin = 0;
			int xOffsetForAttachedObject = 0;
			int yOffsetForPin = 0;
			int yOffsetForAttachedObject = 0;
			if (ownerExtent != null) {
				if (isHorizontalPlacement) {
					xOffsetForPin = Math.abs(ownerExtent.getX() - newExtent.getX());
					yOffsetForAttachedObject = newExtent.getY() - ownerExtent.getY();
				}
				else {
					yOffsetForPin = Math.abs(ownerExtent.getY() - newExtent.getY());
					xOffsetForAttachedObject = newExtent.getX() - ownerExtent.getX();
				}
			}
			PinSideCalculator sideCalculator = null;
			if (!originReference) {
				sideCalculator = PinSideCalculator.createAbsolute(pinList);
			}

			for (T aPin : pinList.getObjects(type)) {

				//				Side side = pinList.getExtent().getSide(aPin.getAbsLocation());
				LocationOnMergedPinlist newLocation;
				if (aPin instanceof IDecorative) { //if the object is through styling then ignore it.
					continue;
				}
				if (originReference) {
					newLocation =
							getExtentOnMergedExtentForAssociatedGraphics(yOffsetForAttachedObject,
									xOffsetForAttachedObject, pinList, aPin);
				}
				else {
					newLocation =
							getLocationOnMergedExtent(newExtent, yOffsetForPin, xOffsetForPin, pinList, aPin, 0,
									0, sideCalculator.getSide(aPin));
				}

				locationOfPin.put(aPin, newLocation);
			}
		}
		return locationOfPin;
	}

	private Side getSide(IPinList anchorPinlistInstance, IDiagramObject diagramObject)
	{
		IExtent anchorpinListInstanceExtent = getOriginalExtent(anchorPinlistInstance, m_grid.getGridSpacing());
		ILocation diagramObjectLocation = diagramObject.getAbsLocation();

		return Side.getSide(anchorpinListInstanceExtent, diagramObjectLocation);
	}

	private LocationAndExtentOfPinlistOnMergedPinlist getExtentOfPinlistOnMergedExtent(IExtent newExtent, int yOffset,
			int xOffset, IPinList anchorPinListInstance,
			IPinList diagramObject,
			int diagramObjectWidth, int diagramObjectHeight, Side sideOfAttachedPinlist)
	{
		IExtent diagramObjectExtent = getOriginalExtent(diagramObject, m_grid.getGridSpacing());
		PinlistExtentOnMergedExtentParams params =
				new PinlistExtentOnMergedExtentParams(diagramObjectExtent);
		LocationOnMergedPinlist locationOnMergedPinlist =
				getExtentAndLocationOnMergedExtent(newExtent, yOffset, xOffset, anchorPinListInstance, diagramObject,
						diagramObjectWidth, diagramObjectHeight,
						sideOfAttachedPinlist, params);
		return new LocationAndExtentOfPinlistOnMergedPinlist(locationOnMergedPinlist.getLocation(),
				locationOnMergedPinlist.getSide(), params.getNewExtentOnMergedExtent(), anchorPinListInstance.getUID());
	}

	private LocationOnMergedPinlist getLocationOnMergedExtent(IExtent newExtent, int yOffset, int xOffset,
			IPinList anchorPinListInstance,
			IDiagramObject diagramObject,
			int diagramObjectWidth, int diagramObjectHeight, Side side)
	{
		return getExtentAndLocationOnMergedExtent(newExtent, yOffset, xOffset, anchorPinListInstance, diagramObject,
				diagramObjectWidth, diagramObjectHeight, side, null);
	}

	private LocationOnMergedPinlist getExtentAndLocationOnMergedExtent(IExtent newExtent, int yOffset, int xOffset,
			IPinList anchorPinListInstance,
			IDiagramObject diagramObject,
			int diagramObjectWidth, int diagramObjectHeight,
			Side side, @Nullable PinlistExtentOnMergedExtentParams attachedPinlistExtentParams)
	{
		IExtent anchorpinListInstanceExtent = getOriginalExtent(anchorPinListInstance, m_grid.getGridSpacing());
		ILocation diagramObjectLocation = diagramObject.getAbsLocation();

		IExtent diagramObjectExtent =
				attachedPinlistExtentParams != null ? attachedPinlistExtentParams.getCurrentExtent() : null;

		ILocation diagramObjectAbsLocation = diagramObject.getAbsLocation();
		int originalLocationX = diagramObjectAbsLocation.getX() -
				(anchorpinListInstanceExtent != null ? anchorpinListInstanceExtent.getX() : 0);
		int originalLocationY = diagramObjectAbsLocation.getY() -
				(anchorpinListInstanceExtent != null ? anchorpinListInstanceExtent.getY() : 0);
		IExtent attachedPinlistExtent = null;
		if (diagramObjectExtent != null) {
			attachedPinlistExtent = FactoryMgr.getCommonFactory().createExtent();
			attachedPinlistExtent.invalidate();
		}

		ILocation newLocation = null;
		if (side == Side.BOTTOM) {
			int bottomOffset = 0;
			if ((anchorpinListInstanceExtent != null ? anchorpinListInstanceExtent.getY() : 0) !=
					diagramObjectLocation.getY()) {
				bottomOffset = diagramObjectHeight;
			}
			newLocation = new Location(newExtent.getX() + originalLocationX + xOffset, newExtent.getY() - bottomOffset);
			if (diagramObjectExtent != null) {
				attachedPinlistExtent.setY(newExtent.getY() - bottomOffset);
				attachedPinlistExtent
						.setX(newExtent.getX() + (diagramObjectExtent.getX() -
								(anchorpinListInstanceExtent != null ? anchorpinListInstanceExtent.getX() : 0)) +
								xOffset);
				attachedPinlistExtent.setHeight(diagramObjectExtent.getHeight());
				attachedPinlistExtent.setWidth(diagramObjectExtent.getWidth());
			}
		}
		else if (side == Side.LEFT) {
			int leftOffset = 0;
			if ((anchorpinListInstanceExtent != null ? anchorpinListInstanceExtent.getX() : 0) !=
					diagramObjectLocation.getX()) {
				leftOffset = diagramObjectWidth;
			}
			newLocation =
					new Location(newExtent.getX() - leftOffset, newExtent.getY() + originalLocationY + yOffset);
			if (diagramObjectExtent != null) {
				attachedPinlistExtent.setX(newExtent.getX() - leftOffset);
				attachedPinlistExtent
						.setY(newExtent.getY() + (diagramObjectExtent.getY() -
								(anchorpinListInstanceExtent != null ? anchorpinListInstanceExtent.getY() : 0)) +
								yOffset);
				attachedPinlistExtent.setHeight(diagramObjectExtent.getHeight());
				attachedPinlistExtent.setWidth(diagramObjectExtent.getWidth());
			}
		}
		else if (side == Side.TOP) {
			int topOffset = 0;
			if ((anchorpinListInstanceExtent != null ? anchorpinListInstanceExtent.getTop() : 0) !=
					diagramObjectLocation.getY()) {
				topOffset = diagramObjectHeight;
			}
			newLocation = new Location(newExtent.getX() + originalLocationX + xOffset, newExtent.getTop() + topOffset);

			if (diagramObjectExtent != null) {
				attachedPinlistExtent.setY(newExtent.getTop() + topOffset);
				attachedPinlistExtent
						.setX(newExtent.getX() +
								(diagramObjectExtent.getX() -
										(anchorpinListInstanceExtent != null ? anchorpinListInstanceExtent.getX() :
												0)) +
								xOffset);
				attachedPinlistExtent.setHeight(diagramObjectExtent.getHeight());
				attachedPinlistExtent.setWidth(diagramObjectExtent.getWidth());
			}
		}
		else if (side == Side.RIGHT) {
			int rightOffset = 0;
			if ((anchorpinListInstanceExtent != null ? anchorpinListInstanceExtent.getRight() : 0) !=
					diagramObjectLocation.getX()) {
				rightOffset = diagramObjectWidth;
			}
			newLocation =
					new Location(newExtent.getRight() + rightOffset, newExtent.getY() + originalLocationY + yOffset);
			if (diagramObjectExtent != null) {
				attachedPinlistExtent.setX(newExtent.getX() + rightOffset);
				attachedPinlistExtent
						.setY(newExtent.getY() +
								(diagramObjectExtent.getY() -
										(anchorpinListInstanceExtent != null ? anchorpinListInstanceExtent.getY() :
												0)) +
								yOffset);
				attachedPinlistExtent.setHeight(diagramObjectExtent.getHeight());
				attachedPinlistExtent.setWidth(diagramObjectExtent.getWidth());
			}
		}
		if (attachedPinlistExtentParams != null) {
			attachedPinlistExtentParams.setNewExtentOnMergeExtent(attachedPinlistExtent);
		}

		return new LocationOnMergedPinlist(newLocation, side);
	}

	private LocationOnMergedPinlist getExtentOnMergedExtentForAssociatedGraphics(int yOffset,
			int xOffset,
			IPinList anchorPinListInstance,
			IDiagramObject diagramObject)
	{

		Side side = getSide(anchorPinListInstance, diagramObject);

		ILocation diagramObjectAbsLocation = diagramObject.getAbsLocation();
		ILocation newLocation =
				new Location(diagramObjectAbsLocation.getX() + xOffset, diagramObjectAbsLocation.getY() + yOffset);
		return new LocationOnMergedPinlist(newLocation, side);
	}

	@Nullable
	private Map<IPinList, LocationAndExtentOfPinlistOnMergedPinlist> getLocationOfAttachedPinlists(
			ObjectsToBeUsedInJoin result,
			IExtent newExtent)
	{
		boolean isHorizontalPinPlacement = result.isHorizontalPlacementOfPins(result.getAnchorObject());
		boolean isVerticalPinPlacement = result.isVerticalPlacementOfPins(result.getAnchorObject());

		if (isHorizontalPinPlacement && isVerticalPinPlacement) {
			return null;
		}
		List<Pair<IPinList, IPinList>> sortedSchemInstances;
		if (isHorizontalPinPlacement) {
			sortedSchemInstances = result.sortPinlistInstancesOnX();

			return getAttachedPinlistLocationInUnion(result, newExtent,
					sortedSchemInstances, true);
		}
		if (isVerticalPinPlacement) {
			sortedSchemInstances = result.sortPinlistInstancesOnY();

			return getAttachedPinlistLocationInUnion(result, newExtent,
					sortedSchemInstances, false);
		}
		return null;
	}

	//	Map<IPinList, ILocation> getLocationOfAttachedPinlist(ObjectsToBeUsedInJoin result, IExtent newExtent)
//	{
//		Map<IPinList, ILocation> locationOfAttachedPinlists = new HashMap<IPinList, ILocation>();
//		for (IPinList pinList : result.getAttachedSchemInstances()) {
//			int originalX = pinList.getLocation().getX();
//			int originalY = pinList.getLocation().getY();
//		}
//
//	}

	private Map<IPinList, LocationAndExtentOfPinlistOnMergedPinlist> getAttachedPinlistLocationInUnion(
			ObjectsToBeUsedInJoin result, IExtent newExtent,
			List<Pair<IPinList, IPinList>> sortedSchemInstances,
			boolean isHorizontalPlacement)
	{
		Map<IPinList, LocationAndExtentOfPinlistOnMergedPinlist> locationOfAttachedPinlist =
				new HashMap<IPinList, LocationAndExtentOfPinlistOnMergedPinlist>();

		for (Pair<IPinList, IPinList> pinListPair : sortedSchemInstances) {

			IPinList anchorPinlistInstance = result.getInstanceOfAnchorObject(pinListPair);
			IPinList attachedPinlist = pinListPair.getSecond();
			if (anchorPinlistInstance == pinListPair.getSecond()) {
				attachedPinlist = pinListPair.getFirst();
			}

			if (attachedPinlist == null) {

				continue;
			}

			IExtent anchorPinlistInstanceExtent =
					getOriginalExtent(anchorPinlistInstance, m_generatorParameters.getSpacing());

			IExtent attachedPinlistExtent = getOriginalExtent(attachedPinlist, m_generatorParameters.getSpacing());

			int yOffset = 0;
			int xOffset = 0;
			if (isHorizontalPlacement) {
				xOffset = Math.abs(
						(anchorPinlistInstanceExtent != null ? anchorPinlistInstanceExtent.getX() : 0) - newExtent.getX());
			}
			else {
				yOffset = Math.abs(
						(anchorPinlistInstanceExtent != null ? anchorPinlistInstanceExtent.getY() : 0) - newExtent.getY());
			}

			LocationAndExtentOfPinlistOnMergedPinlist newLocation =
					getExtentOfPinlistOnMergedExtent(newExtent, yOffset, xOffset, anchorPinlistInstance,
							attachedPinlist,
							attachedPinlistExtent != null ? attachedPinlistExtent.getWidth() : 0,
							attachedPinlistExtent != null ? attachedPinlistExtent.getHeight() : 0,
							result.getSideOfAttachedPinlist(attachedPinlist));
			locationOfAttachedPinlist.put(attachedPinlist, newLocation);
		}
		return locationOfAttachedPinlist;
	}

	private boolean isSelectedPinlistIntersectsArea(int lowerX, int lowerY, int upperX, int upperY, IPinList pinList)
	{
		IExtent pinlistExtent = ExtentHelper.getAbsExtent(pinList);
		return (lowerX <= pinlistExtent.getX() && upperX >= pinlistExtent.getX() ||
				pinlistExtent.getX() <= lowerX && pinlistExtent.getRight() >= lowerX) &&
				(lowerY <= pinlistExtent.getY() && upperY >= pinlistExtent.getY() ||
						pinlistExtent.getY() <= lowerY && pinlistExtent.getTop() >= lowerY);
	}

	@Nullable IPinList getFirstPinlistInSelectedArea(Point startPoint, Point endPoint,
			Collection<IPinList> selectedPinlists)
	{

		final boolean ascend_x = endPoint.x > startPoint.x;
		final boolean ascend_y = endPoint.y > startPoint.y;
		int lowerY = (startPoint.y > endPoint.y ? endPoint.y : startPoint.y);
		int lowerX = (startPoint.x > endPoint.x ? endPoint.x : startPoint.x);
		int upperY = (startPoint.y < endPoint.y ? endPoint.y : startPoint.y);
		int upperX = (startPoint.x < endPoint.x ? endPoint.x : startPoint.x);

		Iterator<IPinList> iter = selectedPinlists.iterator();
		IPinList firstSelection = null;
		IExtent firstSelectionExtent = null;
		while (iter.hasNext()) {
			firstSelection = iter.next();
			firstSelectionExtent = ExtentHelper.getAbsExtent(firstSelection);
			isSelectedPinlistIntersectsArea(lowerX, lowerY, upperX, upperY, firstSelection);

			if (checkValidLogicObjectForStitch(firstSelection) == LogicObjectAcceptance.FAILED) {
				continue;
			}
			if (!isSelectedPinlistIntersectsArea(lowerX, lowerY, upperX, upperY, firstSelection)) {
				continue;
			}
			break;
		}

		while (iter.hasNext()) {
			IPinList aPinlist = iter.next();
			if (checkValidLogicObjectForStitch(aPinlist) == LogicObjectAcceptance.FAILED) {
				continue;
			}
			if (!isSelectedPinlistIntersectsArea(lowerX, lowerY, upperX, upperY, aPinlist)) {
				continue;
			}
			IExtent aPinlistExtent = ExtentHelper.getAbsExtent(aPinlist);

			if (ascend_y) {
				if (aPinlistExtent.getY() < firstSelectionExtent.getY()) {
					firstSelectionExtent = aPinlistExtent;
					firstSelection = aPinlist;
				}
			}
			else {
				if (aPinlistExtent.getTop() > firstSelectionExtent.getTop()) {
					firstSelectionExtent = aPinlistExtent;
					firstSelection = aPinlist;
				}
			}
			if (aPinlistExtent.getY() == firstSelectionExtent.getY()) {
				if (ascend_x) {
					if (aPinlistExtent.getX() < firstSelectionExtent.getX()) {
						firstSelection = aPinlist;
						firstSelectionExtent = aPinlistExtent;
					}
				}
				else {
					if (aPinlistExtent.getRight() > firstSelectionExtent.getRight()) {
						firstSelectionExtent = aPinlistExtent;
						firstSelection = aPinlist;
					}
				}
			}
		}
		return firstSelection;
	}

	@Nullable protected ObjectsToBeUsedInJoin getResultObjectForStitch(
			Collection<IPinList> selectedSchemObjects)
	{

		if (m_diagram == null) {
			if (!getDiagramDetails(selectedSchemObjects)) {
				return null;
			}
		}
		Collection<IPinList> orderedSchemObjects = selectedSchemObjects;

		ObjectsToBeUsedInJoin result = null;
		Collection<IPinList> pinlistsAlreadyVisited = new HashSet<IPinList>();
		if (m_view instanceof ISelectedAreaCoordinates) {
			Pair<Point, Point> areaSelected = ((ISelectedAreaCoordinates) m_view).getStartAndEndOfAreaSelection();
			if (areaSelected != null) {
				IPinList firstSelection =
						getFirstPinlistInSelectedArea(areaSelected.getFirst(), areaSelected.getSecond(),
								orderedSchemObjects);
				result = addPinlistsToResultObject(Collections.singleton(firstSelection), result,
						pinlistsAlreadyVisited);
				if (result == null) {
					return null;
				}
			}
		}

		result = addPinlistsToResultObject(selectedSchemObjects, result, pinlistsAlreadyVisited);

		if (result != null) {
			result.addMissingMatedPairs();
		}
		return result;
	}

	@Nullable
	private ObjectsToBeUsedInJoin addPinlistsToResultObject(@NotNull Collection<IPinList> selectedSchemObjects,
			@Nullable ObjectsToBeUsedInJoin currentObject, @NotNull Collection<IPinList> pinlistsAlreadyVisited)
	{
		ObjectsToBeUsedInJoin result = currentObject;
		for (IPinList aPinlist : selectedSchemObjects) {
			if (checkValidLogicObjectForStitch(aPinlist) == LogicObjectAcceptance.FAILED) {
				continue;
			}

			chs.cof.logical.cable.IPinList connectivityObject = aPinlist.getConnectivity();
			if (connectivityObject != null) {
				if (result == null) {
					result = createStitchObjectUsingConnectivityObject(aPinlist, m_selectedSchemObjects);
				}

				if (result != null) {
					Collection<IPinList> currentPinlistInstances =
							populateResultObjectWithSchemInstances(aPinlist, result, pinlistsAlreadyVisited);
					if (currentPinlistInstances == null) {
						reportFailure(result.getErrorMessage());
						return null;
					}
					pinlistsAlreadyVisited.addAll(currentPinlistInstances);
				}
			}
		}
		return result;
	}

	private boolean checkValidConnectivityObject(ILogicObject connectivityObject)
	{

		return connectivityObject instanceof IInlineJackConnector ||
				connectivityObject instanceof IInlinePlugConnector ||
				connectivityObject instanceof IBaseDevice || (connectivityObject instanceof IJackConnector &&
				!(connectivityObject instanceof IDeviceConnector)) || connectivityObject instanceof IPlugConnector;
	}

	private ObjectsToBeUsedInJoin createStitchObjectUsingConnectivityObject(IPinList aPinlist,
			Collection<IPinList> selectedSchemObjects)
	{
		ObjectsToBeUsedInJoin result = null;
		chs.cof.logical.cable.IPinList connectivityObject = aPinlist.getConnectivity();
		if (connectivityObject instanceof IInlineJackConnector ||
				connectivityObject instanceof IInlinePlugConnector) {
			IConnector currentInline = (IConnector) connectivityObject;
			Iterator<IConnector> iter = currentInline.getMates().iterator();
			IConnector mateInline = iter.hasNext() ? iter.next() : null;

			result = new InlinePlugJackToBeUsedInJoin(currentInline, mateInline, m_grid.getGridSpacing());
		}
		else if (connectivityObject instanceof IBaseDevice) {
			IBaseDevice baseDevice = (IBaseDevice) connectivityObject;

			result = new BaseDeviceToBeUsedInJoin(baseDevice, m_grid.getGridSpacing());
		}
		else if (connectivityObject instanceof IJackConnector &&
				!(connectivityObject instanceof IDeviceConnector)) {
			IJackConnector jackConnector = (IJackConnector) connectivityObject;

			for (IPinList attachedPinList : aPinlist.getAttachedPinListObjects()) {
				if (attachedPinList.getConnectivity() instanceof IPlugConnector) {
					result = new PlugJackToBeUsedInJoin(
							(IPlugConnector) (attachedPinList.getConnectivity()), jackConnector,
							m_grid.getGridSpacing(), selectedSchemObjects);
					break;
				}
			}
			if (result == null) {
				result = new PlugJackToBeUsedInJoin(null, jackConnector, m_grid.getGridSpacing(), selectedSchemObjects);
			}
		}

		else if (connectivityObject instanceof IPlugConnector) {
			IPlugConnector plugConnector = (IPlugConnector) connectivityObject;
			for (IPinList attachedPinList : aPinlist.getAttachedPinListObjects()) {
				if (attachedPinList.getConnectivity() instanceof IJackConnector) {
					result = new PlugJackToBeUsedInJoin(
							plugConnector, (IJackConnector) (attachedPinList.getConnectivity()),
							m_grid.getGridSpacing(), selectedSchemObjects);
					break;
				}
				else if (attachedPinList.getConnectivity() instanceof IBaseDevice) {
					result =
							new BaseDeviceToBeUsedInJoin(
									((IBaseDevice) attachedPinList.getConnectivity()),
									m_grid.getGridSpacing());
					break;
				}
			}
			if (result == null) {
				result = new PlugJackToBeUsedInJoin(plugConnector, null, m_grid.getGridSpacing(),
						selectedSchemObjects);
			}
		}
		return result;
	}

	@Nullable private Set<IPinList> populateResultObjectWithSchemInstances(IPinList aPinlist,
			ObjectsToBeUsedInJoin result, Collection<IPinList> pinlistsAlreadyVisited)
	{

		Set<IPinList> objectsAddedForCurrent = new HashSet<>();
		if (aPinlist.getConnectivity() instanceof IDeviceConnector) {
			return objectsAddedForCurrent;
		}
		for (IPinList attachedPinlist : aPinlist.getAttachedPinListObjects()) {

			if (attachedPinlist.getConnectivity() instanceof IDeviceConnector) {
				continue;
			}
			if (pinlistsAlreadyVisited.contains(attachedPinlist) && pinlistsAlreadyVisited.contains(aPinlist)) {
				objectsAddedForCurrent.add(attachedPinlist);
				continue;
			}
			if (!addSchemInstance(aPinlist, result, attachedPinlist)) {
				return null;
			}
			objectsAddedForCurrent.add(attachedPinlist);
		}
		if (objectsAddedForCurrent.isEmpty()) {
			if (!addSchemInstance(aPinlist, result, null)) {
				return null;
			}
		}
		objectsAddedForCurrent.add(aPinlist);
		return objectsAddedForCurrent;
	}

	protected boolean addSchemInstance(IPinList aPinlist, ObjectsToBeUsedInJoin result,
			@Nullable IPinList attachedPinlist)
	{
		return result.addSchemInstances(aPinlist, attachedPinlist);
	}

	private void regenerateGfxForAffectedPinlists(Collection<IPinList> affectedPinlists)
	{
		Generator generator = Generator.getGenerator();
		GeneratorParameters generatorParameters = DiagramHelper.createGeneratorParameters(m_diagram);

		GeneratorParameters gp = new GeneratorParameters(generatorParameters);
		gp.setNewObject(false);
		for (IPinList pinlist : affectedPinlists) {
			generator.generate(PinListGenerationParams.getInstance(pinlist, gp,
					Generator.NOREGENERATE_PROPERTIES, true));
		}
	}

	public boolean hasValidOperand()
	{
		return m_stitchResult != null;
	}

	public enum LogicObjectAcceptance
	{
		IGNORE,
		FAILED,
		ACCEPT,
	}

	private boolean doesAnyObjectHasHomeConditionTrue(Collection<IPinList> selectedObjects)
	{
		for (IPinList selectedObject : selectedObjects) {
			if (selectedObject.isHome()) {
				return true;
			}
		}
		return false;
	}

	private static class ResizePinlistCornerAdjustCalc
	{

		private static void adjustLowerLeft(Point relDelta, IExtent ext)
		{

			ext.setX(ext.getX() + relDelta.x);
			ext.setY(ext.getY() + relDelta.y);
			ext.setHeight(ext.getHeight() - relDelta.y);
			ext.setWidth(ext.getWidth() - relDelta.x);
		}

		private static void adjustUpperRight(Point relDelta, IExtent ext)
		{
			ext.setWidth(ext.getWidth() + relDelta.x);
			ext.setHeight(ext.getHeight() + relDelta.y);
		}
	}

	protected static class LocationOnMergedPinlist
	{

		private Side side;
		private ILocation location;

		LocationOnMergedPinlist(ILocation location, Side side)
		{
			this.side = side;
			this.location = location;
		}

		Side getSide()
		{
			return side;
		}

		ILocation getLocation()
		{
			return location;
		}
	}

	protected static class LocationAndExtentOfPinlistOnMergedPinlist extends LocationOnMergedPinlist
	{

		private IExtent extent;

		/**
		 * pinList attached to before JoinPinListAction performed
		 */
		private IUID originalAttachedPinList;

		LocationAndExtentOfPinlistOnMergedPinlist(ILocation location, Side side, IExtent extent,
				IUID originalAttachedPinList)
		{
			super(location, side);
			this.extent = extent;
			this.originalAttachedPinList = originalAttachedPinList;
		}

		IExtent getExtent()
		{
			return extent;
		}

		public IUID getOriginalAttachedPinList()
		{
			return originalAttachedPinList;
		}
	}

	private static class PinlistExtentOnMergedExtentParams
	{

		private IExtent currentExtent;
		private IExtent newExtent;

		PinlistExtentOnMergedExtentParams(IExtent currentExtent)
		{
			this.currentExtent = currentExtent;
		}

		IExtent getCurrentExtent()
		{
			return currentExtent;
		}

		IExtent getNewExtentOnMergedExtent()
		{
			return newExtent;
		}

		void setNewExtentOnMergeExtent(IExtent newExtent)
		{
			this.newExtent = newExtent;
		}
	}
}
