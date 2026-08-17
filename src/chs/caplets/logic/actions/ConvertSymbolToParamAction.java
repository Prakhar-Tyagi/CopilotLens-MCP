/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2002-2025 Siemens
 */
package chs.caplets.logic.actions;

import chs.ans.IObjectDescriptor;
import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.IOutputWindow;
import chs.caf.cafmain.actions.ApplyStyleOnDiagramObjectActionCmd;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.helpers.graphics.FlipActionUI;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionUtils;
import chs.caplets.logic.DeleteHelper;
import chs.caplets.logic.actions.ghc.ConnectivityGHCHelper;
import chs.caplets.logic.actions.ghc.DiagramWideConnectorGenerationContext;
import chs.cof.draw.IGrid;
import chs.cof.draw.ILine;
import chs.cof.drawplus.IAttributeText;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.ICrossReferenceable;
import chs.cof.drawplus.IJoint;
import chs.cof.drawplus.IPropText;
import chs.cof.library.IFootprintable;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.HarnessConnectorGenerationEnum;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.IFunction;
import chs.cof.logical.cable.IGroundDevice;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.footprint.connectivity.ConnectivityDeviceFootprintView;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IGenericSchemPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IPinPlaceholder;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.HarnessConnectorsGenerator;
import chs.cofUtils.parameterized.PinPlacementHelper;
import chs.common.Extent;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.IUIDObjectCollection;
import chs.common.Location;
import chs.common.Side;
import chs.services.dynamicgfx.DynamicRotationIndicator;
import chs.services.gfx.GfxView;
import chs.utilities.AlphaNumComparator;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.ListMap;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utility.DiagramHelper;
import chs.utility.Replicator;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.CoordinateHelper;
import chs.utility.helpers.ExtentHelper;
import chs.utility.helpers.ModularSchemPinListInfo;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.logic.ILogicModel;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

public class ConvertSymbolToParamAction extends ControllerActionRT implements ICtxMenuProvider
{

	private IPinList symDev = null;
	private IExtent nonTextExtent = null;
	private AbstractMap<Side, Set<IAbstractSchemPin>> pinsAtSide = new EnumMap<>(Side.class);
	private AbstractMap<Side, ArrayList<Point>> pinPlaceHoldersAtSide = new EnumMap<Side, ArrayList<Point>>(Side.class);
	private Map<IAbstractSchemPin, Point> pinLocations = new LinkedHashMap<>();
	private AffineTransform transform = null;
	private int pinspacing;
	private IExtent calculatedExtent;
	private MovePinHandler movePinHandler = null;
	private Set<IAbstractSchemPin> movedPins = new LinkedHashSet<>();
	private Set<Point> usedLocations = new LinkedHashSet<>();
	private Map<IAbstractSchemPin, Side> prevPinSides = new LinkedHashMap<>();
	private Set<IAbstractSchemPin> processedPins = new LinkedHashSet<>();
	private Set<IConductor> objectsToRoute = new LinkedHashSet<>();
	private Set<IPinList> attachedPinLists = new LinkedHashSet<>();
	private Map<IDevicePin, IAbstractSchemPin> pintoPinMap = new LinkedHashMap<>();
	private SetMap<IConnector, IDevicePin> pintoConnectorMap = new SetMap<>();
	private Map<IAbstractSchemPin, Point> boundaryPinMap = new LinkedHashMap<>();
	private Set<Point> reserevedPlaceHolders = new LinkedHashSet<>();
	private SetMap<IUID, ConnectionInfo> attachedConnectorDetails = null;

	public ConvertSymbolToParamAction(ICapletController controller)
	{
		super(controller);
	}

	@Override public IActionEnum onActivate(ActionEvent e)
	{
		IUIDObject object = getOperand(getController().getSelectMgr().getPreSelections());
		if (object == null) {
			return IActionEnum.eCanceled;
		}
		attachedConnectorDetails = new SetMap<>();
		Comparator<IAbstractSchemPin> pinComparator =
				(@NotNull IAbstractSchemPin o1, @NotNull IAbstractSchemPin o2) -> {
					return AlphaNumComparator.compare(getPinIdentifier(o1), getPinIdentifier(o2), false, true);
				};
		pinsAtSide.put(Side.TOP, new TreeSet<>(pinComparator));
		pinsAtSide.put(Side.BOTTOM, new TreeSet<>(pinComparator));
		pinsAtSide.put(Side.RIGHT, new TreeSet<>(pinComparator));
		pinsAtSide.put(Side.LEFT, new TreeSet<>(pinComparator));
		if (object instanceof IPinList pinList) {
			symDev = pinList;
			transform = symDev.getTransform().getAffineTransform();
			ICapletView view = CAFUtils.getInstance().getActiveCapletView();
			GfxView gview = (GfxView) view;
			ISchemDiagram diagram = (ISchemDiagram) gview.getSheet();
			IGrid grid = diagram.getGrid();
			pinspacing = grid.getGridSpacing();
			movePinHandler = getMovePinHandler();
			nonTextExtent = ExtentHelper.getPinExtent(symDev, null, false);
			nonTextExtent = ExtentHelper.getAbsExtent(symDev, nonTextExtent);
			int min = 2 * pinspacing;
			if (nonTextExtent.getWidth() == 0 || nonTextExtent.getHeight() == 0) {
				Side side = ExtentHelper.getSide(ExtentHelper.getAbsNonTextExtent(symDev), nonTextExtent);
				if (side.isTop()) {
					nonTextExtent.translate(0, -min);
				}
				else if (side.isRight()) {
					nonTextExtent.translate(-min, 0);
				}
			}
			if (nonTextExtent.getWidth() < min) {
				nonTextExtent.setWidth(min);
			}
			if (nonTextExtent.getHeight() < min) {
				nonTextExtent.setHeight(min);
			}
			determineIntialLocations(symDev);

			nonTextExtent.translate(-pinspacing, -pinspacing);

			nonTextExtent.setHeight(nonTextExtent.getHeight() + min);

			nonTextExtent.setWidth(nonTextExtent.getWidth() + min);
			calculatedExtent = calculateExtent();
			symDev.getAttachedPinListObjects().stream()
					.forEach(pl -> {
						if (!(pl.getConnectivity() instanceof IDeviceConnector)) {
							attachedPinLists.add(pl);
						}
					});
			if (!ConvertSymbolToParamHelper
					.determineMatedConnectorMaps(symDev, attachedPinLists, pintoPinMap, pintoConnectorMap)) {
				IOutputWindow outputWindow = CAFUtils.getInstance().getOutputWindow();
				outputWindow.sendMessage(HTMLHelper
								.color("red", ResourceMgr.getString(ConvertSymbolToParamAction.class,
										"ConvertSymbolToParamAction.Output.Message")),
						ResourceMgr.getString(ConvertSymbolToParamAction.class, "ConvertSymbolToParamAction.name.text"),
						false, true);
				return IActionEnum.eCanceled;
			}
		}
		return IActionEnum.eCompleted;
	}

	@NotNull private String getPinIdentifier(@NotNull IAbstractSchemPin pin)
	{
		if (pin instanceof IGenericSchemPin) {
			return ((IConnectivityRef) pin).getConnectivity().getName();
		}
		return pin.getUID().getString();
	}

	@NotNull protected MovePinHandler getMovePinHandler()
	{
		return new MovePinHandler(pinspacing, true, null);
	}

	private IExtent calculateExtent()
	{
		int vPins = Math.max(pinsAtSide.get(Side.LEFT).size(), pinsAtSide.get(Side.RIGHT).size());
		int hPins = Math.max(pinsAtSide.get(Side.TOP).size(), pinsAtSide.get(Side.BOTTOM).size());
		ISchemDiagram diagram = (ISchemDiagram) getModel(ILogicModel.class).getSheet();
		int spacing = diagram.getGrid().getGridSpacing();
		int w = (hPins + 1) * spacing;
		int h = (vPins + 1) * spacing;
		w = getRoundedToGrid(Math.max(w, nonTextExtent.getWidth()));
		h = getRoundedToGrid(Math.max(h, nonTextExtent.getHeight()));
		int x = getRoundedToGrid(nonTextExtent.getX());
		int y = getRoundedToGrid(nonTextExtent.getY());
		return new Extent(x, y, w, h);
	}

	protected int getRoundedToGrid(int value)
	{
		int x = value;
		x = ((int) Math.round((double) x / pinspacing)) * pinspacing;
		return x;
	}

	private void determineIntialLocations(IPinList dev)
	{
		IUIDObjectCollection<IAbstractSchemPin> pins = dev.getAllPins();
		pins.stream().forEach(determineIntialPinPosition());
	}

	@NotNull private Consumer<IAbstractSchemPin> determineIntialPinPosition()
	{
		return pin -> {
			IJoint joint = pin.getJoint();
			Set<ISegment> associations = new TreeSet<>(new Comparator<ISegment>()
			{
				@Override public int compare(ISegment o1, ISegment o2)
				{
					IObjectDescriptor cond1 = CommonUtils.cast(o1.getParent(), IObjectDescriptor.class);
					IObjectDescriptor cond2 = CommonUtils.cast(o2.getParent(), IObjectDescriptor.class);
					String name1 = cond1 != null ? cond1.getTaggableDisplayName() : "";
					String name2 = cond2 != null ? cond2.getTaggableDisplayName() : "";
					return AlphaNumComparator.compare(name1, name2, false);
				}
			});
			associations
					.addAll(joint != null ? joint.getAssociations(ISegment.class) : Collections.emptySet());
			Side pinSide = null;
			if (PinPlacementHelper.onBoundary(nonTextExtent, pin.getAbsLocation())) {
				ILocation pinAbsLocation = pin.getAbsLocation();
				Point point = new Point(pinAbsLocation.getX(), pinAbsLocation.getY());
				pinLocations.put(pin, point);
				if (ConvertSymbolToParamHelper.isCornerPin(nonTextExtent, pinAbsLocation)) {
					pinSide = getSide(ExtentHelper
							.getSideByNearestToBoundary(pinAbsLocation,
									ExtentHelper.getAbsNonTextExtent(symDev)));
				}
				else {
					pinSide = getSide(ExtentHelper.getSideByNearestToBoundary(pinAbsLocation, nonTextExtent));
				}
				pinsAtSide.get(pinSide).add(pin);
				if (!associations.isEmpty()) {
					objectsToRoute.add(associations.iterator().next().getConductor());
				}
				boundaryPinMap.put(pin, adjustAndCachePinLocation(pinSide, point));
				prevPinSides.put(pin, pinSide);
			}
			else if (!associations.isEmpty()) {
				ISegment segment = associations.iterator().next();
				objectsToRoute.add(segment.getConductor());
				Point intersection = findIntersection(segment);
				if (intersection != null) {
					pinLocations.put(pin, intersection);
					pinSide = getSide(
							ExtentHelper.getSideByNearestToBoundary(new Location(intersection), nonTextExtent));
					pinsAtSide.get(pinSide)
							.add(pin);
				}
				else {
					ILocation initLocation = pin.getAbsLocation();
					ILine line = segment.getLineHolder();
					ILocation otherEnd =
							new Location(line.getStartPoint().equals(initLocation) ? line.getEndPoint() :
									line.getStartPoint());
					pinSide = getSide(ExtentHelper.getSideByNearestToBoundary(otherEnd, nonTextExtent));
					pinsAtSide.get(pinSide).add(pin);
					pinLocations.put(pin, new Point(otherEnd.getX(), otherEnd.getY()));
				}
			}
			else {
				ILocation pinAbsLocation = pin.getAbsLocation();
				pinLocations.put(pin, new Point(pinAbsLocation.getX(), pinAbsLocation.getY()));
				pinSide = getSide(ExtentHelper.getSideByNearestToBoundary(pin.getAbsLocation(), nonTextExtent));
				pinsAtSide.get(pinSide)
						.add(pin);
			}
		};
	}

	private Point adjustAndCachePinLocation(Side pinSide, Point point)
	{
		if (pinSide.isLeft()) {
			point.translate(-pinspacing, 0);
		}
		if (pinSide.isRight()) {
			point.translate(pinspacing, 0);
		}
		if (pinSide.isTop()) {
			point.translate(0, pinspacing);
		}
		if (pinSide.isBottom()) {
			point.translate(0, -pinspacing);
		}
		return point;
	}

	@Nullable private Point findIntersection(ISegment segment)
	{
		IConductor conductor = CommonUtils.cast(segment.getParent(), IConductor.class);
		return ConvertSymbolToParamHelper.getSingleIntersectionPoint(nonTextExtent, conductor, transform);
	}

	private Side getSide(int sideByNearestToBoundary)
	{
		if (sideByNearestToBoundary == 1) {
			return Side.TOP;
		}
		if (sideByNearestToBoundary == 2) {
			return Side.RIGHT;
		}
		if (sideByNearestToBoundary == 4) {
			return Side.BOTTOM;
		}
		return Side.LEFT;
	}

	private int getSide(Side side)
	{
		if (side.isTop()) {
			return 1;
		}
		if (side.isRight()) {
			return 2;
		}
		if (side.isBottom()) {
			return 4;
		}
		return 8;
	}

	@Override public boolean onTerminate(boolean successful)
	{
		if (!successful) {
			return false;
		}
		assert symDev != null;
		IPinList schemPinList = CreateParameterizedObjectAction
				.createSchemPinList(symDev.getConnectivity(),
						new Point(calculatedExtent.getX(), calculatedExtent.getY()),
						new Point(calculatedExtent.getRight(), calculatedExtent.getTop()), false,
						new DynamicRotationIndicator(false), "device");
		Replicator replicator = new Replicator(Replicator.INSTANTIATE, true);
		//dts0101231014: ST161BashXSEEDSI4: 'Hide Cross-reference Text' does not persist after 'Convert to Parameterized'
		replicator.replicateCrossReferenceable(symDev, schemPinList);
		//dts0101230832ST161BashXSEEDSI4: Home Condition re-setting when Symbol is getting converted to Parameterized device
		schemPinList.setHome(symDev.isHome());


		Map<String, IPropText> propTexts = new LinkedHashMap<>();
		Map<String, IAttributeText> attributeTexts = new LinkedHashMap<>();

		cacheDiagramTexts(propTexts, attributeTexts);

		//if the symbol device is supplementary, then mark the schem pinlist also as supplementary
		if (symDev.isSupplementary()) {
			schemPinList.markAsSupplementary();
		}
		Set<ILocation> placeHolderLocations = new LinkedHashSet<>();
		schemPinList.getObjects(IPinPlaceholder.class).
				stream().
				forEach(placeHolder ->
				{
					placeHolderLocations.add(CoordinateHelper.getAbsGfxLocation(placeHolder, 0, 0));
				});
		pinPlaceHoldersAtSide.put(Side.LEFT, new ArrayList<>());
		pinPlaceHoldersAtSide.put(Side.TOP, new ArrayList<>());
		pinPlaceHoldersAtSide.put(Side.RIGHT, new ArrayList<>());
		pinPlaceHoldersAtSide.put(Side.BOTTOM, new ArrayList<>());

		processedPlaceHolders(schemPinList, placeHolderLocations);

		processBoundaryPins(schemPinList);
		prevPinSides.clear();

		moveToNewSchemPinList(schemPinList);

		SelectSet toRoute = new SelectSet();
		objectsToRoute.stream().
				forEach(obj -> toRoute.add(new
						Selection(obj), false));

		ISchemDiagram diagram = (ISchemDiagram) getModel(ILogicModel.class).getSheet();

		ConductorRouteAction.getInstance().
				addConductorsForRoute(objectsToRoute);
		ConductorRouteAction.getInstance().
				addPinListForRoute(schemPinList);
		ConductorRouteAction.getInstance().
				addPinListsForRoute(schemPinList.getAttachedPinListObjects());

		ApplyStyleOnDiagramObjectActionCmd cmd = new ApplyStyleOnDiagramObjectActionCmd();
		Set<IUIDObject> styleObjects = new LinkedHashSet<>();
		styleObjects.addAll(schemPinList.getObjects(IUIDObject.class));
		cmd.applyStyle(styleObjects, diagram, false);

		addmissingDiagramtexts(replicator, schemPinList, propTexts, attributeTexts);
		DeleteHelper.getInstance().

				delete(diagram, Collections.singleton(symDev), false);
		return true;
	}

	private void cachePreviousConnectorConnections()
	{
		for (IPinList schmPL : attachedPinLists) {
			for (IPin pin : schmPL.getPins()) {
				IUID connectivityUID = pin.getConnectivityUID();
				if (pin.getConnectivity() instanceof IBackshellTermination) {
					assert connectivityUID != null;
					attachedConnectorDetails.add(connectivityUID, new ConnectionInfo(pin.getAbsLocation(),
							CollectionUtils.getObjects(pin.getSegments(), ISegment.class)));
					continue;
				}
				IPin matchingPinForConnectorPin =
						ConnectionHelper.getMatchingPinForConnectorPin(pin, schmPL, IDevice.class);
				if (matchingPinForConnectorPin != null && connectivityUID != null &&
						matchingPinForConnectorPin.getParent() == symDev) {
					attachedConnectorDetails.add(connectivityUID, new ConnectionInfo(pin.getAbsLocation(),
							CollectionUtils.getObjects(pin.getSegments(), ISegment.class)));
				}
			}
		}
	}

	private void addmissingDiagramtexts(Replicator replicator, IPinList schemPinList, Map<String, IPropText> propTexts,
			Map<String, IAttributeText> attributeTexts)
	{
		schemPinList.getObjects(IPropText.class).stream().forEach(propText -> {
			propTexts.remove(propText.getPropertyName());
		});
		schemPinList.getObjects(IAttributeText.class).stream().forEach(attributeText -> {
			attributeTexts.remove(attributeText.getName());
		});
		propTexts.values().stream().forEach(propText -> schemPinList.addObject(replicator.replicate(propText)));
		attributeTexts.values().stream()
				.forEach(attributeText -> schemPinList.addObject(replicator.replicate(attributeText)));
	}

	protected void cacheDiagramTexts(Map<String, IPropText> propTexts,
			Map<String, IAttributeText> attributeTexts)
	{
		symDev.getObjects(IPropText.class).stream().forEach((prop) -> {
			propTexts.put(prop.getPropertyName(), prop);
		});
		symDev.getObjects(IAttributeText.class).stream().forEach(attr -> {
			attributeTexts.put(attr.getName(), attr);
		});
	}

	private void processBoundaryPins(IPinList pinlist)
	{
		for (IAbstractSchemPin pin : boundaryPinMap.keySet()) {
			Point point = boundaryPinMap.get(pin);
			Side side = prevPinSides.get(pin);
			Point locationOnNewSchem = locationOnNewSchem(pinlist, point, getSide(side));
			boundaryPinMap.replace(pin, locationOnNewSchem);
			reserevedPlaceHolders.add(locationOnNewSchem);
		}
	}

	private void processedPlaceHolders(IPinList schemPinList, Set<ILocation> placeHolderLocations)
	{
		IExtent extent = ExtentHelper.getAbsNonTextExtent(schemPinList);
		placeHolderLocations.stream().forEach(point -> {
			pinPlaceHoldersAtSide.get(getSide(ExtentHelper.getSideByNearestToBoundary(point, extent)))
					.add(new Point(point.getX(), point.getY()));
		});
		Comparator<Point> hCompartor = new Comparator<Point>()
		{
			@Override public int compare(Point o1, Point o2)
			{
				return o1.getX() > o2.getX() ? 1 : -1;
			}
		};
		Comparator<Point> vCompartor = new Comparator<Point>()
		{
			@Override public int compare(Point o1, Point o2)
			{
				return o1.getY() > o2.getY() ? 1 : -1;
			}
		};
		pinPlaceHoldersAtSide.get(Side.LEFT).sort(vCompartor);
		pinPlaceHoldersAtSide.get(Side.RIGHT).sort(vCompartor);
		pinPlaceHoldersAtSide.get(Side.TOP).sort(hCompartor);
		pinPlaceHoldersAtSide.get(Side.BOTTOM).sort(hCompartor);
	}

	@Override public String getActionUIClass()
	{
		return ConvertSymbolToParamActionUI.class.getName();
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (!getController().getCapletModel().isEditable()) {
			return;
		}
		if (selections.getSelectCount() == 1) {
			IUIDObject obj = getOperand(selections);
			if (!(obj instanceof IPinList)) {
				return;
			}
			IBaseDiagram activeDiagram = CAFUtils.getInstance().getActiveDiagram();
			if (activeDiagram != null && SelectionUtils.hasOtherDiagramSelection(selections, activeDiagram)) {
				return;
			}
			ILogicObject logicObject = ReferenceHelper.reduceToLogicObject(obj);
			if (isValidObjectType(logicObject)) {
				//condition #2 added for dts0101230950:ST161BashXSEEDSI4 : "Concert to Parameterized" and leads to "ClassCastException" exception if Symbols pins are connected
				if (((IPinList) obj).getParameterized() == null && !isAttachedToDevice((IPinList) obj)) {
					container.addAfter(new ActionEntry(getActionUI()), FlipActionUI.class);
				}
			}
		}
	}

	private boolean isValidObjectType(@Nullable ILogicObject logicObject)
	{
		return (logicObject instanceof IDevice || logicObject instanceof IFunction) &&
				!IGroundDevice.class.isInstance(logicObject);
	}

	private boolean isAttachedToDevice(IPinList pinList)
	{
		for (IPinList attachedPinList : pinList.getAttachedPinListObjects()) {
			if (attachedPinList.getConnectivity() instanceof IDevice) {
				return true;
			}
		}
		return false;
	}

	@Nullable protected IUIDObject getOperand(SelectSet selections)
	{
		SelectedUIDObjectIterator objectIterator = selections.getSelectedUIDObjects();
		if (objectIterator.hasNext()) {
			return objectIterator.getNext();
		}
		return null;
	}

	@Override public void populateActiveCtxMenu(ActionContainer container)
	{

	}

	public void moveToNewSchemPinList(IPinList destPinList)
	{
		cachePreviousConnectorConnections();
		movePinsFromSymbolToParmPinlist(destPinList);
		Generator generator = Generator.getGenerator();
		ISchemDiagram diagram = (ISchemDiagram) getModel(ILogicModel.class).getSheet();
		GeneratorParameters gp = DiagramHelper.createGeneratorParameters(diagram);
		ILogicDesign design = getModel(ILogicModel.class).getDesign();
		try {
			design.beginLocalEdit();
			pintoPinMap.values().stream().forEach(connectorPin -> {
				connectorPin.disconnectGraphically();
			});

			Collection<IUIDObject> toDelete = new ArrayList<IUIDObject>();
			attachedPinLists.stream().forEach(pl -> {
				if (!(pl.getConnectivity() instanceof IDeviceConnector)) {
					//LOGIC2017-784:delete the whole modular connector schematics.
					toDelete.addAll(new ModularSchemPinListInfo(pl).getCandidates());
				}
			});

			//LOGIC2017-347 :	CT171BashXQPE4: Exception on performing convert to parameterized action with auto-route signal enabled
			DeleteHelper.getInstance().delete(diagram, toDelete, false);
			//dts0101230154 ST161BashSEEDSI1: Convert to parameterized leads to exception in MU mode after removing or creating design wide instance of device in S2
			//CreationDeletionHelper.getTheCreationHelper().addDeletionObject(pl);//.delete();

			DiagramWideConnectorGenerationContext.setGenerationRestrictions(pintoConnectorMap, pintoPinMap);
			ConnectivityDeviceFootprintView.relaxForMu();
			generator.generateDevice(destPinList, gp);
			generator.regenerateSchemDeviceConnectors(destPinList, gp);
			ConnectivityGHCHelper ghcHelper =
					new ConnectivityGHCHelper(diagram, HarnessConnectorGenerationEnum.TypeManuallyGenerated)
					{
						public boolean isDeviceReadyToAllowGHC(IFootprintable device)
						{
							return true;
						}

						protected void tryIfObjectsAreEditable(@NotNull Set<IPinList> schemPinListsForGHC,
								ILogicDesign design)
						{

						}
					};
			ghcHelper.generateHarnessConnectorsForPinlist(destPinList);
			reConnectPreviousConnections(destPinList);
			design.endLocalEdit();
		}
		finally {
			DiagramWideConnectorGenerationContext.resetGenerationRestrictions();
			ConnectivityDeviceFootprintView.resetMuRelax();
		}
	}

	private void movePinsFromSymbolToParmPinlist(IPinList destPinList)
	{
		pinsAtSide.get(Side.LEFT).stream()
				.forEach(pin -> movetoNewSchem(pin, pinLocations.get(pin), destPinList, 8));
		pinsAtSide.get(Side.TOP).stream()
				.forEach(pin -> movetoNewSchem(pin, pinLocations.get(pin), destPinList, 1));
		pinsAtSide.get(Side.RIGHT).stream()
				.forEach(pin -> movetoNewSchem(pin, pinLocations.get(pin), destPinList, 2));
		pinsAtSide.get(Side.BOTTOM).stream()
				.forEach(pin -> movetoNewSchem(pin, pinLocations.get(pin), destPinList, 4));
	}

	private void reConnectPreviousConnections(IPinList destPinList)
	{
		for (IPinList attchedSchemPL : destPinList.getAttachedPinListObjects()) {
			for (IPin schemPin : attchedSchemPL.getPins()) {
				IUID connectivityUID = schemPin.getConnectivityUID();
				assert connectivityUID != null;
				for (ConnectionInfo connectionInfo : attachedConnectorDetails.getSet(connectivityUID)) {
					HarnessConnectorsGenerator
							.reConnectSegmentsWithNewSchmPin(schemPin, connectionInfo.getPrevPinLocation(),
									connectionInfo.getPrevSegments());
				}
			}
		}
	}

	@Nullable
	public ILogicDesign getDesign()
	{
		ICapletController controller = CAFUtils.getInstance().getActiveCapletController();
		if (controller != null) {
			ICapletModel model = controller.getCapletModel();
			if (model instanceof ILogicModel) {
				return ((ILogicModel) model).getDesign();
			}
		}
		return null;
	}

	private void movetoNewSchem(IAbstractSchemPin pin, Point point, IPinList destPinList, int side)
	{
		Point pinLoc = locationOnNewSchem(destPinList, point, side);
		pinLoc.setLocation(getRoundedToGrid(pinLoc.x), getRoundedToGrid(pinLoc.y));
		movePin(pin, pinLoc, destPinList, side);
	}

	private void movePin(IAbstractSchemPin pin, Point pinLoc, IPinList destPinList, int side)
	{
		Point pinDestLocation = pinLoc;
		List<Point> availPoints = new ArrayList<>();
		switch (side) {
			case 1:
				availPoints = pinPlaceHoldersAtSide.get(Side.TOP);
				break;
			case 2:
				availPoints = pinPlaceHoldersAtSide.get(Side.RIGHT);
				break;
			case 4:
				availPoints = pinPlaceHoldersAtSide.get(Side.BOTTOM);
				break;
			case 8:
				availPoints = pinPlaceHoldersAtSide.get(Side.LEFT);
				break;
			default:
				break;
		}
		if (usedLocations.contains(pinDestLocation) ||
				((reserevedPlaceHolders.contains(pinDestLocation) && !boundaryPinMap.containsKey(pin)))) {
			int index = availPoints.indexOf(pinDestLocation);
			index = ConvertSymbolToParamHelper.findClosetAvailPoint(index, availPoints, usedLocations);
			pinDestLocation = availPoints.get(index);
		}
		if (pin instanceof IPin iPin) {
			iPin.setSymbolPinUID(null);
		}
		movePinHandler.movePinToLocation(pin, pinDestLocation.x, pinDestLocation.y, null, destPinList, movedPins,
				new ListMap<>(), processedPins, prevPinSides, new LinkedHashMap<>(), true, true, null);
		usedLocations.add(pinDestLocation);
	}

	private Point locationOnNewSchem(IPinList destPinList, Point point, int side)
	{
		Point locOnSchem = point;
		List<Point> availLocationsOnSchem = collectAvailableLocations(side);
		IExtent absNoAndNonTextExtent = ExtentHelper.getAbsExtent(destPinList);
		int x = absNoAndNonTextExtent.getX();
		int y = absNoAndNonTextExtent.getY();
		int w = absNoAndNonTextExtent.getWidth();
		int h = absNoAndNonTextExtent.getHeight();
		AffineTransform affineTransform = destPinList.getTransform().getAffineTransform();
		affineTransform.transform(locOnSchem, locOnSchem);
		if (!PinPlacementHelper.onBoundary(absNoAndNonTextExtent, new Location(locOnSchem))) {
			Point src = new Point();
			Point dest = new Point();
			switch (side) {
				case 8:
					src.setLocation(x, y);
					dest.setLocation(x, y + h);
					locOnSchem = ConvertSymbolToParamHelper.getPointonSchem(src, dest, locOnSchem, pinspacing);
					locOnSchem.setLocation(x, locOnSchem.y);
					break;
				case 1:
					src.setLocation(x, y + h);
					dest.setLocation(x + w, y + h);
					locOnSchem = ConvertSymbolToParamHelper.getPointonSchem(src, dest, locOnSchem, pinspacing);
					locOnSchem.setLocation(locOnSchem.x, y + h);
					break;
				case 2:
					src.setLocation(x + w, y);
					dest.setLocation(x + w, y + h);
					locOnSchem = ConvertSymbolToParamHelper.getPointonSchem(src, dest, locOnSchem, pinspacing);
					locOnSchem.setLocation(x + w, locOnSchem.y);
					break;
				case 4:
					src.setLocation(x, y);
					dest.setLocation(x + w, y);
					locOnSchem = ConvertSymbolToParamHelper.getPointonSchem(src, dest, locOnSchem, pinspacing);
					locOnSchem.setLocation(locOnSchem.x, y);
					break;
				default:
					break;
			}
		}
		else if (!availLocationsOnSchem.contains(locOnSchem) && !availLocationsOnSchem.isEmpty()) {
			locOnSchem = ConvertSymbolToParamHelper.findNearestPlaceHolder(locOnSchem, availLocationsOnSchem);
		}
		return locOnSchem;
	}

	private List<Point> collectAvailableLocations(int side)
	{
		List<Point> availLocations = new ArrayList<>();
		switch (side) {
			case 1:
				availLocations = pinPlaceHoldersAtSide.get(Side.TOP);
				break;
			case 2:
				availLocations = pinPlaceHoldersAtSide.get(Side.RIGHT);
				break;
			case 4:
				availLocations = pinPlaceHoldersAtSide.get(Side.BOTTOM);
				break;
			case 8:
				availLocations = pinPlaceHoldersAtSide.get(Side.LEFT);
				break;
			default:
				break;
		}
		return availLocations;
	}

	public boolean onPostTerminate(boolean onTerminateResult)
	{
		cleanUp();
		return true;
	}

	private void cleanUp()
	{
		usedLocations.clear();
		pinsAtSide.clear();
		pinLocations.clear();
		movedPins.clear();
		processedPins.clear();
		pinPlaceHoldersAtSide.clear();
		attachedPinLists.clear();
		objectsToRoute.clear();
		prevPinSides.clear();
		pintoPinMap.clear();
		pintoConnectorMap.clear();
		boundaryPinMap.clear();
		reserevedPlaceHolders.clear();
		attachedConnectorDetails = null;
	}

	private static class ConnectionInfo
	{

		private ILocation prevPinLocation = null;
		private Set<ISegment> prevSegments = null;

		ConnectionInfo(ILocation pinLocation, Set<ISegment> connectedSegments)
		{
			prevPinLocation = pinLocation;
			prevSegments = connectedSegments;
		}

		protected ILocation getPrevPinLocation()
		{
			return prevPinLocation;
		}

		protected Set<ISegment> getPrevSegments()
		{
			return prevSegments;
		}
	}
}
