/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2004-2025 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.helpers.UndoableContainerIdler;
import chs.caplets.logic.Model;
import chs.caplets.logic.icd.ICDPlacementHelper;
import chs.caplets.logic.shared.AddSharedPinDialog;
import chs.cof.COFTypeEnum;
import chs.cof.draw.HorizJustificationEnum;
import chs.cof.draw.IDrawFactory;
import chs.cof.draw.IFillPattern;
import chs.cof.draw.IGfxContext;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGrid;
import chs.cof.draw.ISheet;
import chs.cof.draw.IText;
import chs.cof.draw.ITransform;
import chs.cof.draw.IWritableGfxAttribute;
import chs.cof.draw.VertJustificationEnum;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.icd.IDeviceICD;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.IFunction;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IHarnessPlugConnector;
import chs.cof.logical.cable.IInternalPin;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IPinPlaceholder;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinReservationView;
import chs.cof.logical.shared.LogicalDesignCacheGuard;
import chs.cof.logical.shared.SharedPinHelper;
import chs.cof.logical.shared.SharedPinListHelper;
import chs.cof.parts.partselector.IICDSelection;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cofUtils.parameterized.AddPinPlacementConstraints;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.PinPlacementConstraints;
import chs.cofUtils.parameterized.PinPlacementConstraintsHolder;
import chs.cofUtils.parameterized.PinPlacementHelper;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.IObjectFilter;
import chs.common.IUIDObject;
import chs.common.RefreshStatusEnum;
import chs.common.Side;
import chs.common.geom.GeometryUtils;
import chs.ctf.caf.ui.PinPlaceOptionsParams;
import chs.ctf.caf.utils.IBlockPinProxy;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.CHSConstants;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.ListMap;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.GfxUtils;
import chs.utility.ICDUtils;
import chs.utility.gfx.IDrawingComponentOwner;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.ConnectorHelper;
import chs.utility.helpers.CoordinateHelper;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.DesignSharedUsageHelper;
import chs.utility.helpers.ModularSchemPinListInfo;
import chs.utility.helpers.PinListHelper;
import chs.utility.helpers.PropertyCopier;
import chs.utility.helpers.TextHelper;
import chs.utility.helpers.TransformHelper;
import chs.utility.ui.SharedPinListEditUtils;
import chs.view.assist.IPinInfo;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.ToolTipManager;
import javax.swing.event.MouseInputAdapter;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
/**
 * Abstract class for pin creation/placement related actions
 */
public abstract class AbstractPinActionHelper extends MouseInputAdapter
{

	private static final String STACK_PIN_NAME = "Stack";
	private static final int TOOLTIP_WIDTH = 300;
	private static final int TOOLTIP_HEIGHT = 100;
	private int group_placement_pin_sep = 1;
	private boolean m_isUnderReversePlacement = false;
	private boolean m_isUnderStrictOverlapCheck = true;
	private boolean m_isUnderReversePinOrder = false;
	private Map<Integer, Consumer<Integer>> m_keyHandlers = new HashMap<>(8);

	protected enum PlacementMode
	{
		INDIVIDUAL {
			@Override
			public boolean canChange()
			{
				return true;
			}
		},
		STACK {
			@Override
			public boolean canChange()
			{
				return true;
			}
		},
		GROUP {
			@Override
			public boolean canChange()
			{
				return true;
			}
		};

		public abstract boolean canChange();

		public boolean isIndividual()
		{
			return this == INDIVIDUAL;
		}

		public boolean isGroup()
		{
			return this == GROUP;
		}

		public boolean isStack()
		{
			return this == STACK;
		}
	}

	private PlacementMode placementMode = PlacementMode.INDIVIDUAL;
	private ControllerActionRT m_action;
	protected ICapletController m_controller;

	protected GeneratorParameters m_genParams;
	protected Generator m_generator;

	private IDynamicGfxService m_dynamics; // A handle to our dynamic graphics service for convenience.

	private Map<String, IGfxObject> m_dummyPinMap;
	private Map<String, IText> m_dummyPinNameMap;

	protected Point m_currValidPoint;
	protected Point m_currMousePoint;
	private Set<Point> placedPoints = new HashSet<>();
	private PinPlacementValidityEvaluator m_placementValidityEvaluator;
	// TODO jacobt : m_existingConnectivity could contain either String, IAbstractPin, IPinProxy, or perhaps ISharedPin
	// probably need to make it a List<IPinProxy>
	@NotNull private List<IPinProxy> m_existingConnectivity = new ArrayList<>();
	private List<AddPinArgs> m_pinsToAdd = new ArrayList<AddPinArgs>();
	private PinPlacementController m_pinPlacementController;
	protected List<IGfxObject> prevTransientGraphics = new ArrayList<>();

	private static Cursor m_addPinValidCursor = null;
	private static Cursor m_addPinInvalidCursor = null;
	private boolean m_useBoundaryExtensions;
	protected boolean m_requirePlacement;
	protected String m_currentPin;
	protected Preemmies m_preemies = new Preemmies();
	private List<IDynamicGfx> m_transientGraphics = new ArrayList<IDynamicGfx>();

	//must be null. this will trigger rebuild.
	@Nullable private ListMap<IPinList, AddPinArgs> m_pinsDistributed = null;

	protected AbstractPinActionHelper(ControllerActionRT action, boolean requirePlacement,
			boolean useBoundaryExtensions)
	{
		m_pinPlacementController = createPinPlacementController();
		m_action = action;
		m_controller = m_action.getController();

		m_dynamics = getModel().getDynamicGfxService();
		m_generator = Generator.getGenerator();
		m_useBoundaryExtensions = useBoundaryExtensions;
		m_requirePlacement = requirePlacement;
		createAddPinCursors();
		m_dummyPinMap = new LinkedHashMap<>();
		m_dummyPinNameMap = new LinkedHashMap<>();

		m_keyHandlers.put(KeyEvent.VK_R, (t) -> {
			m_isUnderReversePlacement = !m_isUnderReversePlacement;
		});
		Consumer<Integer> consumer_space_incr = (t) -> {
			++group_placement_pin_sep;
		};
		m_keyHandlers.put(KeyEvent.VK_1, consumer_space_incr);
		m_keyHandlers.put(KeyEvent.VK_NUMPAD1, consumer_space_incr);
		Consumer<Integer> consumer_space_decr = (t) -> {
			if (group_placement_pin_sep > 1) {
				--group_placement_pin_sep;
			}
		};
		m_keyHandlers.put(KeyEvent.VK_2, consumer_space_decr);
		m_keyHandlers.put(KeyEvent.VK_NUMPAD2, consumer_space_decr);
		Consumer<Integer> consumer_overlap_check = (t) -> {
			m_isUnderStrictOverlapCheck = !m_isUnderStrictOverlapCheck;
		};
		m_keyHandlers.put(KeyEvent.VK_3, consumer_overlap_check);
		m_keyHandlers.put(KeyEvent.VK_NUMPAD3, consumer_overlap_check);
		m_keyHandlers.put(KeyEvent.VK_P, (t) -> {
			m_isUnderReversePinOrder = !m_isUnderReversePinOrder;
		});
	}

	@NotNull protected PinPlacementController createPinPlacementController()
	{
		return new PinPlacementController();
	}

	@NotNull protected IPinPlacementController getPinPlacementController()
	{
		return m_pinPlacementController;
	}

	protected void pushPlacementPins(List<IPinProxy> objects)
	{
		m_existingConnectivity.addAll(objects);
	}

	protected IPinProxy popPlacementPin()
	{
		return m_existingConnectivity.remove(0);
	}

	protected boolean hasPinsToPlace()
	{
		return !m_existingConnectivity.isEmpty();
	}

	@NotNull protected List<AddPinArgs> getPinsCommitedToPlace(@NotNull IPinList pinList)
	{
		return getPlacingPinArgsDistributed().pullReadOnlySafeList(pinList);
	}

	@NotNull private ListMap<IPinList, AddPinArgs> getPlacingPinArgsDistributed()
	{
		ensurePlacingPinArgsDistributed();
		assert m_pinsDistributed != null;
		return m_pinsDistributed;
	}

	protected final void ensurePlacingPinArgsDistributed()
	{
		if (m_pinsDistributed == null) {
			m_pinsDistributed = new ListMap<>();
			distributeAddPinArgsToPinLists(m_pinsDistributed);
			addPinArgsDistributed();
		}
	}

	protected void addPinArgsDistributed()
	{
		//nothing to do..
	}

	protected String getIconForIvalidLocation()
	{
		return "chs/images/app/cur_cantaddpin.gif";
	}

	protected String getAddPinCursorIcon()
	{
		return "chs/images/app/cur_pin.gif";
	}

	protected void createAddPinCursors()
	{
		if (m_addPinValidCursor == null) {
			//noinspection AssignmentToStaticFieldFromInstanceMethod,NonThreadSafeLazyInitialization
			m_addPinValidCursor = CAFUtils.getInstance()
					.loadCursor(m_controller.getCaplet(), getAddPinCursorIcon(), new Point(7, 7));
			//noinspection AssignmentToStaticFieldFromInstanceMethod
			m_addPinInvalidCursor = CAFUtils.getInstance()
					.loadCursor(m_controller.getCaplet(), getIconForIvalidLocation(), new Point(7, 7));
		}
	}

	protected Model getModel()
	{
		return (Model) m_controller.getCapletModel();
	}

	protected boolean setUp(IPinList pinList, @Nullable Collection<? extends IPinProxy> existingConnectivity,
			boolean bUseBoundaryExtensions)
	{
		m_useBoundaryExtensions = bUseBoundaryExtensions;
		return setUp(pinList, existingConnectivity);
	}

	protected void removeObjectFromAlreadyOccupiedPlaces(@NotNull IDiagramObject diagramObject)
	{
		ILocation absLocation = diagramObject.getAbsLocation();
		Point pt = new Point(absLocation.getX(), absLocation.getY());
		if (placedPoints.remove(pt)) {
			resetPlacementValidityEvaluator();
		}
	}

	private void resetPlacementValidityEvaluator()
	{
		IPinList anchor = m_pinPlacementController.getAnchor();
		IExtent anchorExtent = m_pinPlacementController.getAnchorExtent();
		m_placementValidityEvaluator = new PinPlacementValidityEvaluator(anchor, anchorExtent);
		m_placementValidityEvaluator.placed(placedPoints);
	}

	protected void addObjectToAlreadyOccupiedPlaces(@NotNull IDiagramObject diagramObject)
	{
		ILocation absLocation = diagramObject.getAbsLocation();
		Point pt = new Point(absLocation.getX(), absLocation.getY());
		if (placedPoints.add(pt)) {
			resetPlacementValidityEvaluator();
		}
	}

	@NotNull public Set<IPinList> getEditedPinLists(@NotNull IPinList pinList)
	{
		Set<IPinList> result = new HashSet<>(getPlacingPinArgsDistributed().keySet());
		result.add(pinList);
		return Collections.unmodifiableSet(result);
	}

	protected boolean shouldConsiderDistributionOfPinsOnMultiplePinLists(@NotNull IPinList reference)
	{
		//we might come here when we create empty pinlist for example during shared pinlist flows.
		boolean isForNewCablePinCreation = true;
		for (AddPinArgs addPinArgs : m_pinsToAdd) {
			if (!addPinArgs.isForNewCablePinCreation()) {
				isForNewCablePinCreation = false;
				break;
			}
		}
		if (isForNewCablePinCreation) {
			//single new pin creation case.
			return false;
		}
		chs.cof.logical.cable.IPinList connectivity = reference.getConnectivity();
		return connectivity instanceof IConnector && !((IConnector) connectivity).isInline();
	}

	private void distributeAddPinArgsToPinLists(@NotNull ListMap<IPinList, AddPinArgs> pinsDistributed)
	{
		//we might come here when we create empty pinlist for example during shared pinlist flows.
		if (m_pinsToAdd.isEmpty()) {
			return;
		}
		IPinList rootSchemPinList = m_pinPlacementController.getAnchor();
		if (!shouldConsiderDistributionOfPinsOnMultiplePinLists(rootSchemPinList)) {
			pinsDistributed.addAll(rootSchemPinList, m_pinsToAdd);
			return;
		}

		for (AddPinArgs addPinArgs : m_pinsToAdd) {
			assert addPinArgs.getCablePinlist() != null : "Cable pinlist not set on AddPinArgs!!!";
		}

		ConnectorHelper.distributeAddPinArgsToPinLists(rootSchemPinList, getModel().getDiagram(),
				m_pinsToAdd, (p, a) -> pinsDistributed.add(p, (AddPinArgs) a));
	}

	protected boolean setUp(IPinList pinList, @Nullable Collection<? extends IPinProxy> existingConnectivity)
	{
		m_currMousePoint = null;
		group_placement_pin_sep = 1;
		m_isUnderReversePlacement = false;
		m_isUnderReversePinOrder = false;
		m_isUnderStrictOverlapCheck = true;

		final ISchemDiagram diagram = getModel().getDiagram();

		m_existingConnectivity.clear();
		m_currentPin = null;
		m_pinsToAdd.clear();
		m_preemies.clear();
		placedPoints.clear();
		m_genParams = new GeneratorParameters(diagram.getGrid().getGridSpacing());

		Frame owner = CAFUtils.getInstance().getDialogFrame();
		boolean nullInitialization = false;
		Collection<IPinProxy> pinProxiesSelected = new ArrayList<>();
		chs.cof.logical.cable.IPinList cablePinList = pinList.getConnectivity();
		boolean requiresSetupForSharedPins = false;
		if (existingConnectivity != null && !existingConnectivity.isEmpty()) {
			// adding with the specified connectivity pins
			pinProxiesSelected.addAll(existingConnectivity);
		}
		else if (cablePinList.isShared()) {
			requiresSetupForSharedPins = true;
		}
		else {
			// Default case - add one pin, creating connectivity and schem
			nullInitialization = true;
		}

		//need to setup the controller before setting up shared pins.
		//because that call will require anchor determination.
		setupPinPlacementController(pinList, nullInitialization);

		if (requiresSetupForSharedPins) {
			// adding shared pins - handle with different dialog for now
			pinProxiesSelected = setupSharedPins(owner);
			if (pinProxiesSelected == null) {
				return false;
			}
		}

		if (nullInitialization) {
			m_existingConnectivity.add(null);
		}
		else {
			m_existingConnectivity.addAll(SharedPinListEditUtils.createAndAddCablePins(cablePinList,
					pinProxiesSelected, isReference()));
		}

		IPinList anchor = m_pinPlacementController.getAnchor();
		IExtent anchorExtent = m_pinPlacementController.getAnchorExtent();
		m_placementValidityEvaluator = new PinPlacementValidityEvaluator(anchor, anchorExtent);
		for (IAbstractSchemPin pin : m_pinPlacementController.getAllPins()) {
			ILocation absLocation = pin.getAbsLocation();
			Point pt = new Point(absLocation.getX(), absLocation.getY());
			placedPoints.add(pt); //this is must to handle existing pins.
		}
		m_placementValidityEvaluator.placed(placedPoints);

		m_dummyPinMap = new LinkedHashMap<>();
		for (IPinProxy first : m_existingConnectivity) {
			String dummyPinName = getObjectName(first);
			IDrawFactory drawFact = FactoryMgr.getDrawFactory();
			IGfxObject dummyPin = drawFact.constructRectangle(0, 0, 0, 0);
			m_dummyPinMap.put(dummyPinName, dummyPin);

			int gridSpacing = getModel().getDiagram().getGrid().getGridSpacing();
			IText dummyPinNameText = TextHelper.createTextForCurrentLocale(0, 0, (3 * gridSpacing) / 4, 0, "");
			dummyPinNameText.setHorizontalJustification(HorizJustificationEnum.JustMiddle);
			dummyPinNameText.setVerticalJustification(VertJustificationEnum.JustBottom);
			dummyPinNameText.setString(dummyPinName);
			m_dummyPinNameMap.put(dummyPinName, dummyPinNameText);
		}

		GfxView view = (GfxView) CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eFull);
		}
		if (isGroupPlacement() && m_existingConnectivity.isEmpty()) {
			placementMode = PlacementMode.INDIVIDUAL;
		}

		ISheet sheet = view.getSheet();
		IGfxContext context = view.getGfxContext();
		//
		// Clean up the dynamics first.
		//
		cleanupConstraintGraphics();

		m_pinPlacementController.registerPinPlacementConstraints(
				getPinPlacementConstraintsHolder(anchor, m_useBoundaryExtensions, sheet, context));

		setupNextPin();

		m_preemies.collectPreemies();

		return true;
	}

	protected final void setupPinPlacementController(@NotNull IPinList pinList, boolean ignoreModularity)
	{
		m_pinPlacementController.setup(pinList, ignoreModularity);
	}

	protected final void transferPreemiesToCDH()
	{
		m_preemies.transferPreemiesToCDH();
	}

	private static class Preemmies
	{

		private boolean m_pendingForTransfer = false; //to support multiple call of transfer.
		private Set<IUIDObject> m_elements = new HashSet<IUIDObject>();

		public void clear()
		{
			m_elements.clear();
			m_pendingForTransfer = false;
		}

		public boolean isEmpty()
		{
			return m_elements.isEmpty();
		}

		public Set<IUIDObject> getElements()
		{
			return Collections.unmodifiableSet(m_elements);
		}

		public boolean remove(IAbstractPin pin)
		{
			return m_elements.remove(pin);
		}

		public void collectPreemies()
		{
			// To avoid ControllerActionRT Warning: Found edited objects in CreationDeletionHelper
			// for ObjectsToProcess BEFORE onTerminate(). Specially on cancel (ESC Key) activity
			CreationDeletionHelper cdh = CreationDeletionHelper.getTheCreationHelper();
			m_elements.addAll(CollectionUtils.createList(cdh.getNewObjectsToProcess()));
			cdh.clearNewObjects();
			m_pendingForTransfer = true;
		}

		public void transferPreemiesToCDH()
		{
			// To avoid ControllerActionRT Warning: Found edited objects in CreationDeletionHelper
			if (!m_elements.isEmpty() && m_pendingForTransfer) {
				CreationDeletionHelper.getTheCreationHelper().addCreationObjects(m_elements);
			}
			m_pendingForTransfer = false;
		}
	}

	public static IExtent determinePinListExtent(@NotNull IPinList pinList,
			@NotNull Collection<IAbstractSchemPin> allPins)
	{
		IExtent pinlistExtent = PinPlacementHelper.getPinlistExtent(pinList);
		Pair<Boolean, Boolean> borderInfo = Generator.determineBorderConsideration(allPins, pinlistExtent);
		boolean topBorder = borderInfo.getKey();
		boolean bottomBorder = borderInfo.getValue();
		int x = pinlistExtent.getX();
		int y = pinlistExtent.getY();
		int w = pinlistExtent.getWidth();
		int h = pinlistExtent.getHeight();
		if (topBorder) {
			h += CHSConstants.PIN_SPACING;
		}
		if (bottomBorder) {
			h += CHSConstants.PIN_SPACING;
			y -= CHSConstants.PIN_SPACING;
		}
		return FactoryMgr.getCommonFactory().constructExtent(x, y, w, h);
	}

	private String getObjectName(@Nullable IPinProxy first)
	{
		String dummyPinName = "";
		if (first != null) {
			if (first instanceof IBlockPinProxy) {
				dummyPinName = ((IBlockPinProxy) first).getNameOfActualBlockPin();
			}
			else {
				dummyPinName = first.getDisplayName();
			}
		}
		return dummyPinName;
	}

	/**
	 * Displays "Pin Place" dialog for shared pinlist and populates selected pins
	 *
	 * @param parentFrame Parent frame/dialog
	 *
	 * @return true if pins are selected for placing
	 */
	@Nullable private Collection<IPinProxy> setupSharedPins(Frame parentFrame)
	{
		IPinList anchor = m_pinPlacementController.getAnchor();
		chs.cof.logical.cable.IPinList cablePinlist = anchor.getConnectivity();
		ISharedPinList spl = cablePinlist.getSharedPinList();
		assert spl != null;
		if (!SharedPinListHelper.lockForExclusiveRead(spl)) {
			LogicActionMessageHelper.warnLocked(spl);
			return null;
		}
		RefreshStatusEnum rs = spl.refresh();
		if (RefreshStatusEnum.eObjectDoesNotExist.equals(rs)) {
			LogicActionMessageHelper.warnDeleted(spl);
			return null;
		}
		try (LogicalDesignCacheGuard ignored = new LogicalDesignCacheGuard(Set.of(spl))) {
			if (!loadRequiredSharedUsages(parentFrame, cablePinlist, spl)) {
				return null;
			}

			AddSharedPinDialog pinSelectDialog = getSharedPinSelectionDialog(parentFrame, spl);

			PinPlaceOptionsParams params = new PinPlaceOptionsParams(spl);
			params.enableWithConductorOption(isPinListMappedWithICD(cablePinlist), cablePinlist.getProject());
			ISharedPinReservationView pinview = FactoryMgr.getCommonFactory().constructSharedPinReservationView(spl);
			Collection<IPinProxy> selectedSharedPins = new ArrayList<>();
			if (pinSelectDialog.selectPins(spl, getModel().getDesign(), pinview, params) &&
					!pinSelectDialog.getUsedPins().isEmpty()) {
				setIsReference(pinSelectDialog.isReference());
				setPlaceAsStack(pinSelectDialog.isPlaceAsStack());
				setPlaceAsGroup(pinSelectDialog.isPlaceAsGroup());
				setWithConductor(pinSelectDialog.isWithConductor());

				selectedSharedPins.addAll(pinSelectDialog.getUsedPins());
				CAFUtils.getInstance().setTempUndoableContainer(UndoableContainerIdler.instance());
				pinSelectDialog.cleanup();
				CAFUtils.getInstance().clearTempUndoableContainer();
			}
			else {
				// dts0100445789 - VALIDATION FAILURE: UIDMgr map contains an orphaned objec
				// This path gets exercised when you cancel or Ok the dialog with no pins selected
				// Orphaned objects still need clearing up. Would put in pinSelectDialog.showDialog - but got cold feet
				CAFUtils.getInstance().setTempUndoableContainer(UndoableContainerIdler.instance());
				pinSelectDialog.cleanup();
				CAFUtils.getInstance().clearTempUndoableContainer();
				return null;
			}
			return selectedSharedPins;
		}
	}

	protected boolean isPinListMappedWithICD(@Nullable chs.cof.logical.cable.IPinList cablePinlist)
	{
		IDevice device = CommonUtils.cast(cablePinlist, IDevice.class);
		return device != null && ICDUtils.getMappedICD(device) != null;
	}

	private boolean loadRequiredSharedUsages(Frame parentFrame, chs.cof.logical.cable.IPinList cablePinlist,
			ISharedPinList spl)
	{
		String title = getDialogTitle();
		String description = ResourceMgr.getString(AbstractPinActionHelper.class, "AddPinActionHelper.ProgressBar.dec",
				getObjectType(cablePinlist), spl.getName());
		String longDes = ResourceMgr
				.getString(AbstractPinActionHelper.class, "AddPinActionHelper.LoadUsage.progress.longDescription");
		return DesignSharedUsageHelper.loadeUsagesWithProgressBar(cablePinlist.getProject(), parentFrame,
				SharedPinHelper.getLogicDesignsUsingSharedPinList(cablePinlist.getLogicDesign(), spl),
				title, description, longDes);
	}

	public String getObjectType(chs.cof.logical.cable.IPinList pinlist)
	{
		if (pinlist instanceof IGenericInlineConnector) {
			return COFTypeEnum.Inline.toString();
		}
		return COFTypeEnum.from_object(pinlist).toString();
	}

	protected AddSharedPinDialog getSharedPinSelectionDialog(Frame parentFrame, ISharedPinList sharedPinList)
	{
		return new AddSharedPinDialog(parentFrame, getDialogTitle(), sharedPinList);
	}

	protected String getDialogTitle()
	{
		return ResourceMgr.getString(AbstractPinActionHelper.class, "AddPinActionHelper.PinSelectDialogTitle.text");
	}

	/**
	 * Directly specify a pin to be added and it's location
	 *
	 * @param args Defines the pin and the location at which to create a schem/conn pin
	 */
	public void setupPin(AddPinArgs args)
	{
		m_pinsToAdd.add(args);
	}

	public boolean isActive()
	{
		return m_pinPlacementController.isActive();
	}

	/**
	 * Clean up the data structures and remove the loc. if modified, will also flush the shared pin list
	 *
	 * @param modificationMade flag to indicate if modification made & hence trigger SPL flush
	 */
	public void cleanUp(boolean modificationMade)
	{
		//check for connectivity also. dts0100783412
		chs.cof.logical.cable.IPinList cablePL =
				isActive() ? m_pinPlacementController.getAnchor().getConnectivity() : null;
		ISharedPinList sharedPL = cablePL != null ? cablePL.getSharedPinList() : null;
		if (sharedPL != null) {
			//
			// ONLY flush the shared pinlist if there was a change made.
			//
			if (modificationMade) {
				SharedPinListHelper.flush(sharedPL);
			}
			SharedPinListHelper.unlock(sharedPL);
		}
		cleanUp();
	}

	protected void cleanUp()
	{
		cleanUpToolTip();
		placementMode = PlacementMode.INDIVIDUAL;
		m_existingConnectivity.clear();
		m_pinPlacementController.cleanup();
		m_pinsDistributed = null;
		m_genParams = null;
		m_pinsToAdd.clear();
		m_preemies.clear();
		placedPoints.clear();
		// Cleanup the transient graphics
		m_dynamics.removeAllDynamicGfx();
		m_dynamics.removeAllTransientGfx();
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eFull);
		}
		getModel().getDiagram().refreshRepresentations();
	}

	public void clearTransientGraphics()
	{
		placedPoints.clear();
		m_dynamics.removeAllDynamicGfx();
		m_dynamics.removeAllTransientGfx();
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eFull);
		}
	}

	public void setIsReference(boolean value)
	{
		m_pinPlacementController.setIsReference(value);
	}

	public void setWithConductor(boolean value)
	{
		m_pinPlacementController.setWithConductor(value);
	}

	public void setPlaceAsStack(boolean value)
	{
		if (value) {
			placementMode = PlacementMode.STACK;
		}
		else if (placementMode == PlacementMode.STACK) {
			placementMode = PlacementMode.INDIVIDUAL;
		}
	}

	public void setPlaceAsGroup(boolean value)
	{
		if (value) {
			placementMode = PlacementMode.GROUP;
		}
		else if (placementMode == PlacementMode.GROUP) {
			placementMode = PlacementMode.INDIVIDUAL;
		}
	}

	public boolean isReference()
	{
		return m_pinPlacementController.isReference();
	}

	public boolean isWithConductor()
	{
		return m_pinPlacementController.isWithConductor();
	}

	public boolean isPlaceAsStack()
	{
		return placementMode == PlacementMode.STACK;
	}

	public boolean isGroupPlacement()
	{
		return placementMode == PlacementMode.GROUP;
	}

	public boolean isPlaceAsIndividual()
	{
		return placementMode == PlacementMode.INDIVIDUAL;
	}

	public void regenerateGraphics(@Nullable IPinList pl)
	{
		if (pl == null) {
			return;
		}
		ISchemDiagram diagram = getModel().getDiagram();
		PinListAddPinHelper.regeneratePinListGraphics(pl, diagram, m_generator, false);
	}

	protected chs.cof.logical.cable.IPinList getConnectivityPinOwner(chs.cof.logical.cable.IPinList cablePL)
	{
		return cablePL;
	}

	public Set<IUIDObject> getCreatedPinObjects()
	{
		return m_preemies.getElements();
	}

	private void setupNextPin()
	{
		if (!m_existingConnectivity.isEmpty()) {
			GfxView view = (GfxView) CAFUtils.getInstance().getActiveCapletView();
			resetTransientGraphicsAndrecalConstrainsts(view, m_currValidPoint);
		}
	}

	private boolean isPlacementNotAllowed(Point pt, @Nullable IAbstractPin placementPin,
			boolean assumeInfiniteExtBoundary)
	{
		return !isActionAllowed(placementPin, pt, assumeInfiniteExtBoundary);
	}

	private int computeShift()
	{
		int shift = group_placement_pin_sep * CHSConstants.PIN_SPACING;
		return isUnderReversePlacement() ? -shift : shift;
	}

	private Point getNextHorizontalPoint(Point point)
	{
		return new Point(point.x + computeShift(), point.y);
	}

	private Point getNextVerticalPoint(Point point)
	{
		return new Point(point.x, point.y - computeShift());
	}

	@Nullable IAbstractPin getPlacementPin(int idx)
	{
		if (idx < 0) {
			return null;
		}
		if (idx < m_existingConnectivity.size()) {
			IPinProxy first = m_existingConnectivity.get(idx);
			return (first != null) ? first.getCablePin() : null;
		}

		return null;
	}

	private boolean checkOverlappingOnCommitedPoints(List<Point> candiatepoints)
	{
		List<Point> pointsToCheck = candiatepoints;
		if (isUnderStrictOverlapCheck() && candiatepoints.size() > 1) {
			//assuming that the points would be either horizontal or vertical.
			Point first = candiatepoints.get(0);
			Point last = candiatepoints.get(candiatepoints.size() - 1);
			if (first.x == last.x) {
				int startY = first.y > last.y ? last.y : first.y;
				int endY = first.y < last.y ? last.y : first.y;
				while (startY <= endY) {
					if (isAlreadyCommited(new Point(first.x, startY))) {
						return false;
					}
					startY += CHSConstants.PIN_SPACING;
				}
			}
			else if (first.y == last.y) {
				int startX = first.x > last.x ? last.x : first.x;
				int endX = first.x < last.x ? last.x : first.x;
				while (startX <= endX) {
					if (isAlreadyCommited(new Point(startX, first.y))) {
						return false;
					}
					startX += CHSConstants.PIN_SPACING;
				}
			}
		}
		else {
			for (Point candiatepoint : pointsToCheck) {
				if (isAlreadyCommited(candiatepoint)) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean isAlreadyCommited(Point candiatepoint)
	{
		//already a pin is placed.
		return placedPoints.contains(candiatepoint);
	}

	protected void generateConstraintDynamicGraphics()
	{
		if (!m_pinPlacementController.hasConstraints()) {
			//if we somehow come here withput setup. we will ignore.
			return;
		}
		//
		// Right, at this point, we know where the boundaries/constraints are.
		// - the edges are fair game for pin addition (as long as the object type
		// allows for it - e.g. connectors).
		//
		// The areas above/below/side of the object are fair game IF there are no pins
		// on that side.
		//
		int consecutiveLimit = isGroupPlacement() ? m_existingConnectivity.size() : 1;
		Collection<IGfxObject> boundaryExtensions = m_pinPlacementController.getBoundaryExtensions();
		Collection<IGfxObject> validMovePositions = m_pinPlacementController.getValidMovePositions();
		boundaryExtensions.forEach(rectangle -> m_dynamics.addTransientGfx(rectangle));
		prevTransientGraphics.clear();
		//enforce consecutive placement constraints.
		Set<Point> alreadyComputed = new HashSet<>(validMovePositions.size());
		validMovePositions.forEach(circle -> {
			ILocation location = circle.getLocation();
			alreadyComputed.add(new Point(location.getX(), location.getY()));
		});
		Map<Point, Side> placementPositionSides = new HashMap<>();
		PinPlacementValidityEvaluator placementValidityEvaluator = m_placementValidityEvaluator.getClone();
		validMovePositions.forEach(circle -> {
			ILocation location = circle.getLocation();
			//no need to re-validate the current position
			Point pt = new Point(location.getX(), location.getY());

			Side relSide = placementPositionSides.get(pt);
			if (relSide == null) {
				relSide = determinePlacementSideForPinListInRelative(pt);
				placementPositionSides.put(pt, relSide);
			}

			List<Point> candiatepoints = new GroupPlacementNextPointCalculator(relSide, pt).getPoints(consecutiveLimit);

			//first criteria: no overlap on already placed pins.
			if (!checkOverlappingOnCommitedPoints(candiatepoints)) {
				return;
			}

			//check the boundary conditions.
			if (!placementValidityEvaluator.tryPlace(candiatepoints)) {
				return;
			}

			int matchCount = 1;
			for (int i = 1; i < consecutiveLimit; ++i) {
				Point candiatepoint = candiatepoints.get(i);
				if (!alreadyComputed.contains(candiatepoint)) {
					if (isPlacementNotAllowed(candiatepoint, getPlacementPin(i),
							shouldAssumeInfiniteExtBoundary(candiatepoint, m_currValidPoint))) {
						break;
					}
				}
				++matchCount;
			}
			if (matchCount == consecutiveLimit) {
				m_dynamics.addTransientGfx(circle);
				prevTransientGraphics.add(circle);
			}
		});
		alreadyComputed.clear();
	}

	protected boolean isPlacingBackshellTerminations()
	{
		return false;
	}

	@NotNull protected PinPlacementConstraintsHolder getPinPlacementConstraintsHolder(
			@NotNull IPinList candidate, boolean includeBoundaryExtensions, ISheet sheet, IGfxContext context)
	{
		return getPinPlacementConstraints(candidate, includeBoundaryExtensions, sheet, context)
				.getHolder();
	}

	@NotNull protected PinPlacementConstraints getPinPlacementConstraints(@NotNull IPinList candidate,
			boolean includeBoundaryExtensions, ISheet sheet, IGfxContext context)
	{
		return new AddPinPlacementConstraints(m_currentPin, candidate, m_genParams.getSpacing(),
				includeBoundaryExtensions, sheet, context, isPlacingBackshellTerminations());
	}

	public boolean hasTempPlaceHolderForDevicesWithSymbols()
	{
		return false;
	}

	void cleanupConstraintGraphics()
	{
		for (IGfxObject object : m_pinPlacementController.getBoundaryExtensions()) {
			m_dynamics.removeTransientGfx(object);
		}
	}

	public void addPinAtPosition(Point position)
	{
		m_currValidPoint = position;
		addPinIfValidPoint();
	}

	/**
	 * addPinIfValidPoint Add a pin to the pinlist at m_currValidPoint.
	 *
	 * @return True iff there are more pins to be added. For shared pinlists, returns true iff there are still selected
	 * pins remaining to be placed. For non-shared pinlists, always returns true.
	 */
	protected boolean addPinIfValidPoint()
	{
		// Cleanup the transient graphics

		if (m_currValidPoint != null) {
			//freeze the current connection graphics.
			m_transientGraphics.clear();
			IPinList anchor = m_pinPlacementController.getAnchor();
			ILocation objLoc = anchor.getLocation();
			ITransform tform = anchor.getTransform();
			Side relSide = determinePlacementSideForPinListInRelative(m_currValidPoint);
			Side absSide = determinePlacementSideForPinListInAbsolute(relSide);
			int placeLimit = isPlaceAsIndividual() ? 1 : m_existingConnectivity.size();
			List<Point> absPlacePoints =
					new GroupPlacementNextPointCalculator(relSide, m_currValidPoint).getPoints(placeLimit);
			Set<Point> pointsCommited = new LinkedHashSet<>(placeLimit);
			for (int i = 0; i < placeLimit; i++) {
				Point absPlacePoint = absPlacePoints.get(isGroupPlacement() ? i : 0);
				//
				// We now have the pin location, relative to the objects location.
				//
				int addx = absPlacePoint.x - objLoc.getX();
				int addy = absPlacePoint.y - objLoc.getY();

				//
				// Un-Transform the location - if the object is transformed, we need to take that into account
				// as we are working with the base (untransformed) object.
				//
				Point relPlacePoint = new Point(addx, addy);
				IPinProxy obj = m_existingConnectivity.get(0);
				try {
					tform.getAffineTransform().inverseTransform(relPlacePoint, relPlacePoint);
				}
				catch (NoninvertibleTransformException nite) { /* Ignore */ }
				addToAddedtoPinlist(relPlacePoint, obj, isPlaceAsStack());
				m_existingConnectivity.remove(0);
				placedPoints.add(absPlacePoint);
				pointsCommited.add(absPlacePoint);
				addTransientPlacedPinGraphics(absSide, absPlacePoint, obj);
				//Reason for providing different location for different proxies - In case of mated devices,
				//while placing pins in bulk, only the first pin is getting placed on the mate.
			}
			m_placementValidityEvaluator.placed(pointsCommited);
			transientPlacedPinGraphicsAdded();
			setupNextPin();
			m_currValidPoint = null;
		}

		//
		// Redraw view...
		//
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eFull);
		}

		return !m_existingConnectivity.isEmpty();
	}

	public void mousePressed(MouseEvent e)
	{
		if (e.getButton() == MouseEvent.BUTTON3) {
			cleanUpToolTip();
		}
	}

	protected void addToAddedtoPinlist(Point2D placePoint1, @Nullable IPinProxy obj, boolean stackpin)
	{
		if (obj != null) {
			setupPin(new AddPinArgs(obj, placePoint1, stackpin));
		}
		else {
			setupPin(new AddPinArgs(null, placePoint1, null, stackpin));
		}
	}

	public void mouseReleased(MouseEvent e)
	{
		boolean morePins = addPinIfValidPoint();
		// Tell the action manager to terminate us so we can do the edit.
		if (!morePins || e.getClickCount() == 2) {
			clearTransientGraphics();
			cleanUpToolTip();
			if (!m_requirePlacement && morePins) {
				// If this is adding a shared pins then the connectivity objects have already been made and added
				// to the creation helper and UIDMgr.  If we cancelled the adding then we need to remove them again.
				cleanupUnplacedObjects();
			}

			terminateActiveAction();
		}
	}

	public void terminateActiveAction()
	{
		m_controller.getActionMgr().terminateActiveAction(isSuccessfulTermination());
	}

	public boolean isSuccessfulTermination()
	{
		return !m_requirePlacement || !m_pinsToAdd.isEmpty();
	}

	private void cleanUpToolTip()
	{
		GfxView view = CommonUtils.cast(CAFUtils.getInstance().getActiveCapletView(), GfxView.class);
		if (view != null) {
			view.clearPopupTooltip();
			view.setToolTipText(null);
			ToolTipManager.sharedInstance().setInitialDelay(0);
		}
	}

	protected void cleanupUnplacedObjects()
	{
		for (IPinProxy obj : m_existingConnectivity) {
			//SP1310-dts0100993066
			IAbstractPin pin = (obj != null) ? obj.getCablePin() : null;
			if (pin != null) {
				if (CreationDeletionHelper.getTheCreationHelper().removeCreationObject(pin)) {
					UIDMgr.removeObject(pin.getUID());

					for (IAbstractPin connectedPin : pin.getConnectedPins()) {
						// Mated pins aren't added to the exsiting connectivty collection, but still need to be
						// removed.
						if (CreationDeletionHelper.getTheCreationHelper().removeCreationObject(connectedPin)) {
							UIDMgr.removeObject(connectedPin.getUID());
						}
					}
				}
				//Pins which are not added into pin placeholders should be removed from m_preemies
				if (m_preemies.remove(pin)) {
					UIDMgr.removeObject(pin.getUID());
					for (IAbstractPin connectedPin : pin.getConnectedPins()) {
						if (m_preemies.remove(connectedPin)) {
							UIDMgr.removeObject(connectedPin.getUID());
						}
					}
				}
			}
		}
	}

	public void keyPressed(KeyEvent e)
	{
		Point currValidPoint = m_currMousePoint;
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		GfxView gview = (GfxView) view;
		boolean clearToolTip = true;
		if (placementMode.canChange()) {
			PlacementMode oldPlacementMode = placementMode;
			if (e.getModifiers() == InputEvent.SHIFT_MASK) {
				if (isPlaceAsIndividual()) {
					placementMode = PlacementMode.GROUP;
				}
				else if (isGroupPlacement()) {
					placementMode = PlacementMode.INDIVIDUAL;
				}
			}
			else if (e.getModifiers() == InputEvent.CTRL_MASK) {
				if (isPlaceAsIndividual() && isStackPinAllowed()) {
					placementMode = PlacementMode.STACK;
				}
				else if (isPlaceAsStack()) {
					placementMode = PlacementMode.INDIVIDUAL;
					//change the name of pins from 'Stack' to original
					for (Map.Entry<String, IText> entry : m_dummyPinNameMap.entrySet()) {
						entry.getValue().setString(entry.getKey());
					}
				}
			}
			if (oldPlacementMode != placementMode) {
				resetTransientGraphicsAndrecalConstrainsts(gview, currValidPoint);
				e.consume();
				clearToolTip = false;
			}
		}
		if (!e.isShiftDown() && !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() && !e.isAltGraphDown()) {
			int keyCode = e.getKeyCode();
			Consumer<Integer> keyConsumer = m_keyHandlers.get(keyCode);
			if (keyConsumer != null) {
				keyConsumer.accept(keyCode);
				resetTransientGraphicsAndrecalConstrainsts(gview, currValidPoint);
				e.consume();
				clearToolTip = false;
			}
		}
		if (clearToolTip) {
			cleanUpToolTip();
		}
	}

	private void resetTransientGraphicsAndrecalConstrainsts(GfxView gview, Point currValidPoint)
	{
		for (IGfxObject gfxObject : prevTransientGraphics) {
			m_dynamics.removeTransientGfx(gfxObject);
		}
		removeDynamicPinGraphics();
		gview.invalidate(IViewInvalidationEnum.eTransient);
		generateConstraintDynamicGraphics();
		mouseMoved(currValidPoint, true);
	}

	protected void updateToolTipText(GfxView gview, Point worldPoint)
	{
		String toolTipText;
		if (isReference()) {
			toolTipText = isPlaceAsIndividual() ?
					ResourceMgr.getString(AbstractPinActionHelper.class, "AddPinActionHelper.Reference.tooltip") : "";
		}
		else if (isPlaceAsIndividual()) {
			String toolTipRes =
					isStackPinAllowed() ? "AddPinActionHelper.Individual.tooltip" :
							"AddPinActionHelper.IndividualNoStack.tooltip";
			toolTipText = ResourceMgr.getString(AbstractPinActionHelper.class, toolTipRes);
		}
		else if (isGroupPlacement()) {
			toolTipText = ResourceMgr.getString(AbstractPinActionHelper.class, "AddPinActionHelper.Group.tooltip");
		}
		else if (isPlaceAsStack()) {
			toolTipText = ResourceMgr.getString(AbstractPinActionHelper.class, "AddPinActionHelper.Stack.tooltip");
		}
		else {
			toolTipText = "";
		}
		if (gview != null) {
			Point devicePoint = gview.worldToDevice(worldPoint);
			int tooltipShift = GfxUtils.TOOLTIP_SHIFT;
			Point toolTipPoint = new Point(devicePoint.x + tooltipShift, devicePoint.y + tooltipShift);
			gview.showTooltipAtLocation(toolTipText, toolTipPoint);
		}
	}

	private void ensurePlacementModeValidity()
	{
		if (placementMode.isStack() && !isStackPinAllowed()) {
			placementMode = PlacementMode.INDIVIDUAL;
		}
	}

	public void mouseMoved(MouseEvent e)
	{
		m_currMousePoint = CAFUtils.getInstance().getWorldPoint(e.getPoint(), e.getSource());
		mouseMoved(m_currMousePoint, false);
	}

	private void mouseMoved(@Nullable Point mousePoint, boolean forcedMove)
	{
		if (mousePoint == null || !isActive() || m_existingConnectivity.isEmpty()) {
			return;
		}
		ensurePlacementModeValidity();
		Point currValidPoint = m_currValidPoint;
		m_currValidPoint = mousePoint;
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		GfxView gview = (GfxView) view;
		ISchemDiagram diagram = (ISchemDiagram) gview.getSheet();
		IGrid grid = diagram.getGrid();
		m_currValidPoint.setLocation(grid.snap(m_currValidPoint.x), grid.snap(m_currValidPoint.y));

		//try not to do redundant computation if there is no actual change in snapped location.
		//this would improve performance during icd placement etc.
		if (!forcedMove && null != currValidPoint && m_currValidPoint.equals(currValidPoint)) {
			return;
		}

		cleanUpToolTip();
		removeTransiantGfx();

		// Check to see if it hits the transient area [NOTE: do not allow any swapping!].

		// Avoid adding pins on top of each other. The constraints will typically prevent this providing the extension
		// dont overlap pinlist boundary. However we can also add pins within the boundary extensions, this is not
		// covered by the constraints hence we do it  here using the previously placed pins list to avoid adding overlaps
		// Additionally we prevent placing pins on the same side where the pins are aligned. This prevents pin overlaps
		// when the device is resized.
		// We allow pins to place only on horizontally aligned or vertically aligned depending upon the side.
		// If side is left or right, allowed pins are aligned vertically ( with same x co-ordinate)
		// if side is top or bottom, allowed pins are aligned horizontally ( with same y co-ordinate)
		// This prevents creation of pins inside the parameterized device boundary including the pins
		// which are placed during this action.
		Side relSide = determinePlacementSideForPinListInRelative(m_currValidPoint);
		int consecutiveLimit = isGroupPlacement() ? m_existingConnectivity.size() : 1;
		List<Point> candiatepoints =
				new GroupPlacementNextPointCalculator(relSide, m_currValidPoint).getPoints(consecutiveLimit);

		//first criteria: no overlap on already placed pins.
		boolean isPlacementAllowed = checkOverlappingOnCommitedPoints(candiatepoints);

		if (isPlacementAllowed) {
			PinPlacementValidityEvaluator placementValidityEvaluator = m_placementValidityEvaluator.getClone();
			//check the boundary conditions.
			if (!placementValidityEvaluator.tryPlace(candiatepoints)) {
				isPlacementAllowed = false;
			}
		}

		if (isPlacementAllowed) {
			for (int idx = 0; idx < consecutiveLimit; ++idx) {
				Point placementPoint = candiatepoints.get(idx);
				if (!isActionAllowed(getPlacementPin(idx), placementPoint,
						shouldAssumeInfiniteExtBoundary(placementPoint, m_currValidPoint))) {
					isPlacementAllowed = false;
					break;
				}
			}
		}

		if (isPlacementAllowed) {
			//we will not show tool-tip because this may hide the pin transient graphics.
			cleanUpToolTip();
			if (!m_existingConnectivity.isEmpty()) {
				Side absSide = determinePlacementSideForPinListInAbsolute(relSide);
				if (isGroupPlacement()) {
					String popupTooltip = getTooltipForGroupPlacement();
					Point tooltipDeviceLocation = determineTooltipDeviceLocation(gview, m_currValidPoint, absSide);
					gview.showTooltipAtLocation(popupTooltip, tooltipDeviceLocation);
				}
				else {
					updateToolTipText(gview, m_currValidPoint);
				}
				int idx = 0;
				for (Point dummyPoint : candiatepoints) {
					IPinProxy object = m_existingConnectivity.get(idx);
					idx++;
					String dummyPinName = getObjectName(object);
					IGfxObject dummyPin = m_dummyPinMap.get(dummyPinName);
					IText dummyPinNameText = m_dummyPinNameMap.get(dummyPinName);
					placeDummyPin(dummyPin, dummyPoint);
					m_dynamics.addTransientGfx(dummyPin);
					if (isPlaceAsStack()) {
						dummyPinNameText.setString(STACK_PIN_NAME);
					}
					if (!StringUtils.isBlank(dummyPinNameText.getString())) {
						justifyTextPoint(dummyPinNameText, absSide, dummyPoint);
						m_dynamics.addTransientGfx(dummyPinNameText);
					}
				}
				addTransientGfx(diagram);
			}
			m_action.setCursor(getAddPinValidCursor());
		}
		else {
			updateToolTipText(gview, m_currValidPoint);
			m_currValidPoint = null;
			removeDynamicPinGraphics();
			m_action.setCursor(getAddPinInvalidCursor());
		}
		gview.invalidate(IViewInvalidationEnum.eTransient);
	}

	protected String getTooltipForGroupPlacement()
	{
		return ResourceMgr.getString(AbstractPinActionHelper.class,
				"AddPinActionHelper.GroupHint.tooltip");
	}

	public static Point determineTooltipDeviceLocation(@NotNull GfxView view, @NotNull Point worldPoint, Side absSide)
	{
		Point shift = new Point();
		int tooltipShift = GfxUtils.TOOLTIP_SHIFT;
		switch (absSide) {
			case LEFT:
				shift.setLocation(-TOOLTIP_WIDTH - tooltipShift, 0);
				break;
			case RIGHT:
				shift.setLocation(tooltipShift, 0);
				break;
			case BOTTOM:
				shift.setLocation(0, -tooltipShift);
				break;
			case TOP:
				shift.setLocation(0, TOOLTIP_HEIGHT + tooltipShift);
				break;
			default:
				break;
		}
		Point devicePoint = view.worldToDevice(worldPoint);
		return new Point(devicePoint.x + shift.x, devicePoint.y + shift.y);
	}

	private boolean shouldAssumeInfiniteExtBoundary(Point placementPoint, Point currMousePt)
	{
		//the first commit should be on visible boundary.
		//and allow pins on infinitely extended boundary
		//only for vertical placement. otherwise the last
		//commit might end at corner.
		return !placementPoint.equals(currMousePt);
	}

	protected boolean isUnderStrictOverlapCheck()
	{
		return m_isUnderStrictOverlapCheck;
	}

	protected boolean isUnderReversePlacement()
	{
		return m_isUnderReversePlacement;
	}

	protected boolean isUnderReversePinOrder()
	{
		return m_isUnderReversePinOrder;
	}

	@NotNull protected Point getGroupPlacementNextPoint(@NotNull Side side, @NotNull Point placementPoint)
	{
		return side.isHorizontal() ? getNextVerticalPoint(placementPoint) : getNextHorizontalPoint(placementPoint);
	}

	private void removeDynamicPinGraphics()
	{
		for (IGfxObject gfxObject : m_dummyPinMap.values()) {
			m_dynamics.removeTransientGfx(gfxObject);
		}
		for (IText gfxObject : m_dummyPinNameMap.values()) {
			m_dynamics.removeTransientGfx(gfxObject);
		}
	}

	private Side determinePlacementSideForPinListInAbsolute(@NotNull Side relSide)
	{
		IPinList anchor = m_pinPlacementController.getAnchor();
		int numRotation = (TransformHelper.getTransformInfo(anchor.getTransform()).getRotation() /
				GeometryUtils.NINETY_DEGREES) % 2;
		Side absSide = relSide;
		for (int i = 0; i < numRotation; ++i) {
			switch (absSide) {
				case LEFT:
					absSide = Side.TOP;
					break;
				case RIGHT:
					absSide = Side.BOTTOM;
					break;
				case BOTTOM:
					absSide = Side.LEFT;
					break;
				case TOP:
					absSide = Side.RIGHT;
					break;
			}
		}
		return absSide;
	}

	private Side determinePlacementSideForPinListInRelative(@NotNull Point absPoint)
	{
		IPinList anchor = m_pinPlacementController.getAnchor();
		IExtent anchorExtent = m_pinPlacementController.getAnchorExtent();
		return determinePlacementSideForPinList(anchorExtent,
				PinListHelper.getRelativeToPinList(anchor, absPoint));
	}

	private static Side determinePlacementSideForPinList(@NotNull IExtent extent, @NotNull Point point)
	{
		int left = extent.getLeft();
		int right = extent.getRight();
		int top = extent.getTop();
		int bottom = extent.getBottom();

		int x = point.x;
		int y = point.y;
		//the corners are treated to be only on left-right side only.
		//check the left side fidelity
		if (x == left) {
			return Side.LEFT;
		}

		//check the right side fidelity
		if (x == right) {
			return Side.RIGHT;
		}

		//check the top side fidelity
		if (y == top) {
			return Side.TOP;
		}

		//check the bottom side fidelity
		if (y == bottom) {
			return Side.BOTTOM;
		}

		return Side.getSide(extent, FactoryMgr.getCommonFactory().constructLocation(x, y));
	}

	protected Cursor getAddPinInvalidCursor()
	{
		return m_addPinInvalidCursor;
	}

	protected Cursor getAddPinValidCursor()
	{
		return m_addPinValidCursor;
	}

	protected boolean isStackPinAllowed()
	{
		chs.cof.logical.cable.IPinList candidateCablePlForStackPin = determineCablePinListForStackPlacement();
		if (candidateCablePlForStackPin == null) {
			return false;
		}
		return !((candidateCablePlForStackPin instanceof IFunction || isReference() || addingBackshell()));
	}

	private void addTransientGfx(ISchemDiagram diagram)
	{
		if (!m_existingConnectivity.isEmpty()) {
			//Do not show connection transient graphics when adding the placed pin graphics
			if (!isReference()) {
				TransientPinInfo pinInfo = new TransientPinInfo(m_existingConnectivity);
				List<String> pinsToTransientGfx = pinInfo.getPinNames();
				ObjectConnectionsGetter.createTransientGraphics(pinsToTransientGfx, diagram, pinInfo, m_dynamics,
						m_transientGraphics);
				ListMap<IHarnessPlugConnector, IAbstractPin> matedPins = new ListMap<>();
				for (IPinProxy obj : m_existingConnectivity) {
					if (obj != null) {
						IPinProxy proxy = obj;
						IAbstractPin cablePin = proxy.getCablePin();
						if (cablePin instanceof IDevicePin) {
							for (IAbstractPin connectedPin : cablePin.getConnectedPins()) {
								chs.cof.logical.cable.IPinList connectedPinOwner = connectedPin.getOwner();
								if (connectedPinOwner instanceof IHarnessPlugConnector) {
									matedPins.add((IHarnessPlugConnector) connectedPinOwner, connectedPin);
								}
							}
						}
					}
				}

				for (List<IAbstractPin> pins : matedPins.values()) {
					TransientMatePinInfo matedPinInfo = new TransientMatePinInfo(pins);
					List<String> matedPinsToTransientGfx = matedPinInfo.getPinNames();
					ObjectConnectionsGetter
							.createTransientGraphics(matedPinsToTransientGfx, diagram, matedPinInfo, m_dynamics,
									m_transientGraphics);
				}
			}
		}

		IDeviceICD icd = null;
		if (m_action instanceof AddParameterizedDeviceFromLibraryPartAction) {
			ILibraryPartSelection librarySelection =
					((AddParameterizedDeviceFromLibraryPartAction) m_action).getLibrarySelection();
			if (librarySelection instanceof IICDSelection) {
				icd = ((IICDSelection) librarySelection).getICD();
			}
		}
		if (m_action instanceof IICDProviderAction) {
			icd = ((IICDProviderAction) m_action).getICD();
		}
		if (icd != null) {
			updateICDTransientGfx(diagram, icd);
		}
	}

	private void updateICDTransientGfx(ISchemDiagram diagram, @Nullable IDeviceICD icd)
	{
		if (icd != null && m_currValidPoint != null) {
			Side relSide = determinePlacementSideForPinListInRelative(m_currValidPoint);
			GroupPlacementNextPointCalculator nextPointCalculator =
					new GroupPlacementNextPointCalculator(relSide, m_currValidPoint);
			List<Pair<ILocation, String>> pinAbsLocationInfo = new ArrayList<>(m_existingConnectivity.size());
			if (isPlaceAsStack()) {
				List<Point> placementPoints = nextPointCalculator.getPoints(1);
				if (!placementPoints.isEmpty()) {
					ILocation location = GfxUtils.getLocation(placementPoints.get(0));
					for (IPinProxy pinObj : m_existingConnectivity) {
						String objectName = StringUtils.nonNull(getObjectName(pinObj));
						pinAbsLocationInfo.add(new Pair<>(location, objectName));
					}
				}
				assert placementPoints.size() == 1 : "Some issue in next points calculation!!!";

				Iterator<IPinProxy> iterator = m_existingConnectivity.iterator();
				for (Point placementPoint : placementPoints) {
					String objectName = m_existingConnectivity.isEmpty() ? "" : getObjectName(iterator.next());
					ILocation location = GfxUtils.getLocation(placementPoint);
					pinAbsLocationInfo.add(new Pair<>(location, StringUtils.nonNull(objectName)));
				}
			}
			else {
				int limit = isGroupPlacement() ? m_existingConnectivity.size() : 1;
				List<Point> placementPoints = nextPointCalculator.getPoints(limit);
				assert placementPoints.size() == limit : "Some issue in next points calculation!!!";
				Iterator<IPinProxy> iterator = m_existingConnectivity.iterator();
				for (Point placementPoint : placementPoints) {
					if (iterator.hasNext()) {
						String objectName = StringUtils.nonNull(getObjectName(iterator.next()));
						ILocation location = GfxUtils.getLocation(placementPoint);
						pinAbsLocationInfo.add(new Pair<>(location, objectName));
					}
					else {
						break;
					}
				}
			}
			List<IDynamicGfx> icdTransientGraphics = new ArrayList<IDynamicGfx>();
			if (!pinAbsLocationInfo.isEmpty()) {
				for (IPinList candidate : m_pinPlacementController.getCandidates()) {
					icdTransientGraphics.addAll(ICDPlacementHelper
							.updateNetTraces(candidate, icd, diagram, pinAbsLocationInfo, addingBackshell()));
				}
			}
			for (IGfxObject gfxObj : icdTransientGraphics) {
				m_dynamics.addTransientGfx(gfxObj);
			}
			m_transientGraphics.addAll(icdTransientGraphics);
		}
	}

	private class GroupPlacementNextPointCalculator
	{

		private Point m_currRelPoint;
		private final Side m_relSide;

		private GroupPlacementNextPointCalculator(@NotNull Side relSide, @NotNull Point startAbsPoint)
		{
			m_relSide = relSide;
			IPinList anchor = m_pinPlacementController.getAnchor();
			m_currRelPoint = PinListHelper.getRelativeToPinList(anchor, startAbsPoint);
		}

		private List<Point> getPoints(int count)
		{
			List<Point> points = new ArrayList<>(count);
			IPinList anchor = m_pinPlacementController.getAnchor();
			for (int i = 0; i < count; ++i) {
				ILocation absLoc = CoordinateHelper.getAbsGfxLocation(anchor, m_currRelPoint.x, m_currRelPoint.y);
				points.add(new Point(absLoc.getX(), absLoc.getY()));
				m_currRelPoint = getGroupPlacementNextPoint(m_relSide, m_currRelPoint);
			}
			if (isUnderReversePinOrder()) {
				//reverse the pin locations.
				Collections.reverse(points);
			}
			return points;
		}
	}

	public void updateICDRouting(@NotNull IPinList pinList, @Nullable IDeviceICD icd,
			@NotNull IObjectFilter<IPin> pinFilter, boolean generateSingleEnded)
	{
		if (icd != null) {
			ISchemDiagram sheet =
					(ISchemDiagram) ((IDrawingComponentOwner) CAFUtils.getInstance().getActiveCapletView()).getSheet();
			chs.cof.logical.cable.IPinList iPinList = pinList.getConnectivity();
			ISharedObject sharedDevice = iPinList.getSharedObject();
			if (sharedDevice != null) {
				boolean lockSuccess = false;
				try {
					lockSuccess = LockUpdateHelper.obtainLockOnSharedObject(sharedDevice);
					if (lockSuccess) {
						ICDPlacementHelper.updateICDNameAndRouting(pinList, icd, icd.getRole(), sheet, pinFilter, generateSingleEnded);
					}
				}
				finally {
					if (lockSuccess) {
						LockUpdateHelper.flushAndUnlockSharedObject(sharedDevice);
					}
				}
			}
			else {
				ICDPlacementHelper.updateICDNameAndRouting(pinList, icd, icd.getRole(), sheet, pinFilter, generateSingleEnded);
			}
		}
	}

	private void removeTransiantGfx()
	{
		for (IGfxObject gfxObj : m_transientGraphics) {
			m_dynamics.removeTransientGfx(gfxObj);
		}
		m_transientGraphics.clear();
	}

	protected boolean isActionAllowed(@Nullable IAbstractPin placementPin, @NotNull Point currPt,
			boolean assumeInfiniteExtBoundary)
	{
		for (Integer validState : m_pinPlacementController.allow(currPt, placementPin, addingBackshell(),
				editingStack(), assumeInfiniteExtBoundary)) {
			if ((validState & PinPlacementConstraintsHolder.PLACEMENT_ON_EXT) ==
					PinPlacementConstraintsHolder.PLACEMENT_ON_EXT) {
				validState = isValidBoundaryExtent(placementPin, currPt) ? PinPlacementConstraintsHolder.PLACEMENT_YES :
						PinPlacementConstraintsHolder.PLACEMENT_NO;
			}
			if (!(validState == PinPlacementConstraintsHolder.PLACEMENT_YES)) {
				return false;
			}
		}
		if (isPlaceAsStack()) {
			return isValidToStackAtCurrentValidPoint(currPt);
		}
		return true;
	}

	protected boolean isValidBoundaryExtent(@Nullable IAbstractPin placementPin, @NotNull Point currPt)
	{
		return true;
	}

	protected int getNumberOfPinsToPlace()
	{
		return m_existingConnectivity.size();
	}

	protected int getNumberOfPinsUnderPlacement()
	{
		return isPlaceAsIndividual() ? 1 : m_existingConnectivity.size();
	}

	protected int countOfPinsPendingToAdd()
	{
		return m_pinsToAdd.size();
	}

	@Nullable protected chs.cof.logical.cable.IPinList determineCablePinListForStackPlacement()
	{
		chs.cof.logical.cable.IPinList anchorPinList = m_pinPlacementController.getAnchor().getConnectivity();
		if (anchorPinList instanceof IConnector) {
			Set<chs.cof.logical.cable.IPinList> cablePLSet = new HashSet<>();
			for (IPinProxy existingObject : m_existingConnectivity) {
				chs.cof.logical.cable.IPinList currCablePL =
						existingObject != null ? existingObject.getCablePinList() : null;
				if (currCablePL != null) {
					cablePLSet.add(currCablePL);
				}
			}
			if (cablePLSet.size() == 1) {
				return cablePLSet.iterator().next();
			}
			if (cablePLSet.size() > 1) {
				//cable pins of different pinlists.
				return null;
			}
		}

		return anchorPinList;
	}

	// Check if the current point is valid to create add stack. These are extra checks other than we did in method PinPlacementContraints.allow()
	protected boolean isValidToStackAtCurrentValidPoint(Point currPt)
	{
		chs.cof.logical.cable.IPinList candidateCablePlForStackPin = determineCablePinListForStackPlacement();
		if (candidateCablePlForStackPin == null) {
			return false;
		}
		IGfxObject obj = m_pinPlacementController.getObjectAtCurrentValidPoint(currPt, candidateCablePlForStackPin);
		if (obj instanceof IPinPlaceholder) {
			IPinPlaceholder placeholder = (IPinPlaceholder) obj;
			IPinList parent = (IPinList) placeholder.getOwner();
			if (parent != null) {
				for (IPinProxy existingObject : m_existingConnectivity) {
					IAbstractPin cablePin = (existingObject != null) ? existingObject.getCablePin() : null;
					if (!isValidForStackPin(parent, cablePin)) {
						return false;
					}
				}

				IGfxObject match = ConnectionHelper.getMatchingPinOrPlaceholderForPlaceHolder(placeholder, parent,
						chs.cof.logical.cable.IPinList.class);
				if (match instanceof IPinPlaceholder) {
					IPinList matedParent = (IPinList) ((IPinPlaceholder) match).getOwner();
					return matedParent != null && isValidForStackPin(matedParent, null);
				}
				else {
					for (IPinList attachedPinList : parent.getAttachedPinListObjects()) {
						if (!isValidForStackPin(attachedPinList, null)) {
							return false;
						}
					}
				}
			}
		}
		else {
			return isValidForStackPin(m_pinPlacementController.getAnchor(), null);
		}
		return true;
	}

	private boolean isValidForStackPin(IPinList parent, @Nullable IAbstractPin existingObject)
	{
		return StackPinActionHelper.isValidToCreateStackOnPinList(parent, existingObject);
	}

	private void placeDummyPin(IGfxObject dp, Point pt)
	{
		IExtent ext = dp.getExtent();
		ILocation loc = dp.getLocation();
		int dim = getModel().getDiagram().getGrid().getGridSpacing() / 2;
		ext.setBounds(0, 0, dim, dim);
		loc.setLocation(pt.x - (dim / 2), pt.y - (dim / 2));
	}

	private void justifyTextPoint(IText nameText, Side side, Point pinPoint)
	{
		int dim = getModel().getDiagram().getGrid().getGridSpacing() / 2;
		IExtent ext = nameText.getExtent();
		ILocation loc = nameText.getLocation();
		ext.setBounds(0, 0, ext.getWidth(), ext.getHeight());
		loc.setLocation(pinPoint.x, pinPoint.y);

		if (side.isLeft()) {
			loc.setLocation(pinPoint.x + dim, pinPoint.y);
			nameText.setHorizontalJustification(HorizJustificationEnum.JustLeft);
			nameText.setVerticalJustification(VertJustificationEnum.JustCenter);
			nameText.setRotation(GeometryUtils.ZERO_DEGREES);
		}
		else if (side.isRight()) {
			loc.setLocation(pinPoint.x - dim, pinPoint.y);
			nameText.setHorizontalJustification(HorizJustificationEnum.JustRight);
			nameText.setVerticalJustification(VertJustificationEnum.JustCenter);
			nameText.setRotation(GeometryUtils.ZERO_DEGREES);
		}
		else if (side.isBottom()) {
			loc.setLocation(pinPoint.x, pinPoint.y + dim);
			nameText.setHorizontalJustification(HorizJustificationEnum.JustRight);
			nameText.setVerticalJustification(VertJustificationEnum.JustCenter);
			nameText.setRotation(GeometryUtils.NINETY_DEGREES);
		}
		else if (side.isTop()) {
			loc.setLocation(pinPoint.x, pinPoint.y - dim);
			nameText.setHorizontalJustification(HorizJustificationEnum.JustLeft);
			nameText.setVerticalJustification(VertJustificationEnum.JustCenter);
			nameText.setRotation(GeometryUtils.NINETY_DEGREES);
		}
	}

	private void addTransientPlacedPinGraphics(Side side, Point pt, @Nullable IPinProxy obj)
	{
		String name = getObjectName(obj);
		IText text = m_dummyPinNameMap.get(name);
		IGfxObject iGfxObject = m_dummyPinMap.get(name);
		IDrawFactory drawFact = FactoryMgr.getDrawFactory();
		IGfxObject dummyPlacedPin = drawFact.constructRectangle(0, 0, 0, 0);
		placeDummyPin(dummyPlacedPin, pt);

		IText dummyPlacedPinName = drawFact.constructText(0, 0,
				(3 * getModel().getDiagram().getGrid().getGridSpacing()) / 4, 0, text.getString());
		justifyTextPoint(dummyPlacedPinName, side, pt);

		IWritableGfxAttribute gfxAttr =
				FactoryMgr.getDrawFactory().constructGfxAttribute(dummyPlacedPin.getAttribute());
		gfxAttr.setFillPattern(IFillPattern.PATTERN_SOLID);
		dummyPlacedPin.setAttribute(gfxAttr);
		m_dynamics.addTransientGfx(dummyPlacedPin);
		m_dynamics.addTransientGfx(dummyPlacedPinName);

		m_dynamics.removeTransientGfx(iGfxObject);
		m_dynamics.removeTransientGfx(text);
	}

	private void transientPlacedPinGraphicsAdded()
	{
		for (IGfxObject gfxObject : m_dummyPinMap.values()) {
			m_dynamics.removeTransientGfx(gfxObject);
		}
		for (IText gfxObject : m_dummyPinNameMap.values()) {
			m_dynamics.removeTransientGfx(gfxObject);
		}

		m_action.setCursor(getAddPinInvalidCursor());
		CAFUtils.getInstance().getActiveCapletView().invalidate(IViewInvalidationEnum.eTransient);
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(AbstractPinActionHelper.class, "AddPinActionHelper.StatusBarMessage");
	}

	public static class AddPinArgs extends AbstractAddPinArgs
	{

		private IAbstractPin m_pin;
		private String m_name;
		//		private ISharedPin m_sharedPin;
		private boolean m_bStackPin;

		private IPinProxy pinProxy;

//		private boolean m_isBlockPin = false;

		public AddPinArgs(@Nullable IAbstractPin apin, Point2D point, @Nullable String name, boolean stackpin)
		{
			super(point);
			m_pin = apin;
			m_name = name;
			m_bStackPin = stackpin;
		}

		public AddPinArgs(IPinProxy pinProxy, Point2D point, boolean stackpin)
		{
			super(point);
			this.pinProxy = pinProxy;
			m_bStackPin = stackpin;
		}

		@Nullable public IAbstractPin getPin()
		{
			return m_pin != null ? m_pin : pinProxy != null ? pinProxy.getCablePin() : null;
		}

		@Nullable public String getName()
		{
			if (m_name != null) {
				return m_name;
			}
			if (pinProxy instanceof IBlockPinProxy) {
				return ((IBlockPinProxy) pinProxy).getNameOfActualBlockPin();
			}
			return pinProxy != null ? pinProxy.getName() : null;
		}

		@Nullable public IInternalPin getInternalPin()
		{
			return pinProxy != null ? pinProxy.getInternalPin() : null;
		}

		public boolean isStackPin()
		{
			return m_bStackPin;
		}

		@Nullable public String getAssociatedObject()
		{
			return pinProxy instanceof IBlockPinProxy ? ((IBlockPinProxy) pinProxy).getAssociatedObject() : null;
		}

		@Nullable public String getAssociatedPinName()
		{
			return pinProxy instanceof IBlockPinProxy ? ((IBlockPinProxy) pinProxy).getAssociatedPinName() : null;
		}

		@Nullable public String getAssociatedObjectType()
		{
			return pinProxy instanceof IBlockPinProxy ? ((IBlockPinProxy) pinProxy).getAssociatedObjectType() : null;
		}

		@Nullable public chs.cof.logical.cable.IPinList getCablePinlist()
		{
			if (m_pin != null) {
				return m_pin.getOwner();
			}
			if (pinProxy != null) {
				return pinProxy.getCablePinList();
			}
			return null;
		}

		public boolean assignLibraryCavity(IAbstractPin cablePin)
		{
			if (pinProxy != null) {
				if (cablePin.getOwner() == getCablePinlist()) {
					if (pinProxy.getLibraryCavity() != null) {
						PropertyCopier.copyCavityAttributesAndPropertiesOntoPin(cablePin, pinProxy.getLibraryCavity());
					}
				}
				return true;
			}
			return false;
		}

		public IPinProxy getPinProxy()
		{
			return pinProxy;
		}

		public boolean isForNewCablePinCreation()
		{
			return pinProxy == null && m_pin == null;
		}
	}

	protected boolean addingBackshell()
	{
		return false;
	}

	protected boolean editingStack()
	{
		return false;
	}

	protected List<AddPinArgs> getPinsToAdd()
	{
		return m_pinsToAdd;
	}

	private class TransientPinInfo implements IPinInfo
	{

		private Map<String, IPinProxy> m_pinNameToProxy = new LinkedHashMap<>();

		TransientPinInfo(List<? extends IPinProxy> objects)
		{
			for (IPinProxy obj : objects) {
				if (obj instanceof IBlockPinProxy) {
					IBlockPinProxy blockPinProxy = (IBlockPinProxy) obj;
					m_pinNameToProxy.put(blockPinProxy.getNameOfActualBlockPin(), blockPinProxy);
				}
				else if (obj != null) {
					IPinProxy proxy = obj;
					m_pinNameToProxy.put(proxy.getDisplayName(), proxy);
				}
			}
		}

		@Override
		public ILocation getAbsLocation(String pinName)
		{
			return placementMode.isStack() ? m_dummyPinMap.values().iterator().next().getLocation() :
					m_dummyPinMap.get(pinName).getLocation();
		}

		@Nullable
		@Override
		public IAbstractSchemPin getOriginatingSchemPin(String pinName)
		{
			return null;
		}

		@Nullable
		@Override
		public IAbstractPin getCablePin(String pinName)
		{
			IPinProxy proxy = m_pinNameToProxy.get(pinName);
			if (proxy != null) {
				return proxy.getCablePin();
			}
			return null;
		}

		public List<String> getPinNames()
		{
			if (m_pinNameToProxy.isEmpty()) {
				return Collections.emptyList();
			}
			return placementMode.isIndividual() ?
					Collections.singletonList(m_pinNameToProxy.keySet().iterator().next()) :
					new ArrayList<>(m_pinNameToProxy.keySet());
		}
	}

	private class TransientMatePinInfo implements IPinInfo
	{

		private Map<String, IAbstractPin> m_pinNameToPin = new LinkedHashMap<>();

		TransientMatePinInfo(List<IAbstractPin> pins)
		{
			for (IAbstractPin pin : pins) {
				m_pinNameToPin.put(pin.getConnectedPin(), pin);
			}
		}

		@Override
		public ILocation getAbsLocation(String pinName)
		{
			return m_dummyPinMap.get(pinName).getLocation();
		}

		@Nullable
		@Override
		public IAbstractSchemPin getOriginatingSchemPin(String pinName)
		{
			return null;
		}

		@Nullable
		@Override
		public IAbstractPin getCablePin(String pinName)
		{
			return m_pinNameToPin.get(pinName);
		}

		public List<String> getPinNames()
		{
			if (isPlaceAsStack()) {
				return new ArrayList<>(m_pinNameToPin.keySet());
			}
			else {
				return new ArrayList<>(m_dummyPinMap.keySet());
			}
		}
	}

	protected interface IPinPlacementController
	{

		@NotNull PinListAddPinHelper getPinlistAddPinHelper(@NotNull IPinList pinList);

		@NotNull IPinList getAnchor();

		boolean containsMatchingPinOnMatedPinLists(Point currPt, @Nullable IAbstractPin placementPin);

		@Nullable IGfxObject getObjectAtCurrentValidPoint(@Nullable Point currPt,
				@NotNull chs.cof.logical.cable.IPinList cablePlForStackPin);
	}

	protected static class PinPlacementController implements IPinPlacementController
	{

		@Nullable private IExtent m_pinListExtent;
		@Nullable private ModularSchemPinListInfo m_pinListInfo;
		private boolean m_isReference;
		private boolean m_withConductor;
		private Map<IPinList, PinListAddPinHelper> m_pinlistAddPinHelpers = new LinkedHashMap<>();
		@Nullable protected PinPlacementConstraintsHolder m_constraints;

		protected PinPlacementController()
		{
		}

		private Set<IPinList> getCandidates()
		{
			return m_pinListInfo != null ? m_pinListInfo.getCandidates() : Collections.emptySet();
		}

		private void setup(IPinList pinList, boolean ignoreModularity)
		{
			m_pinListInfo = new ModularSchemPinListInfo(pinList, ignoreModularity);
			IPinList anchor = m_pinListInfo.getAnchor();
			m_pinListExtent = determinePinListExtent(anchor, anchor.getAllPins());
		}

		@NotNull public PinListAddPinHelper getPinlistAddPinHelper(@NotNull IPinList pinList)
		{
			return m_pinlistAddPinHelpers.computeIfAbsent(pinList, p -> getPinListAddPinHelper(p, isReference()));
		}

		@NotNull protected PinListAddPinHelper getPinListAddPinHelper(IPinList pinlist, boolean isReference)
		{
			return new PinListAddPinHelper(pinlist, isReference);
		}

		@NotNull public IPinList getAnchor()
		{
			assert m_pinListInfo != null;
			return m_pinListInfo.getAnchor();
		}

		public boolean containsMatchingPinOnMatedPinLists(Point currPt, @Nullable IAbstractPin placementPin)
		{
			for (PinPlacementConstraintsHolder value : determinePinPlacementConstraintsToCheck().values()) {
				if (value.containsMatchingPinOnMatedPinLists(currPt)) {
					return true;
				}
			}
			return false;
		}

		@NotNull private Set<IAbstractSchemPin> getAllPins()
		{
			return m_pinListInfo != null ? m_pinListInfo.getAllPins() : Collections.emptySet();
		}

		@NotNull private IExtent getAnchorExtent()
		{
			assert m_pinListExtent != null;
			return m_pinListExtent;
		}

		private boolean isActive()
		{
			return m_pinListInfo != null;
		}

		private void cleanup()
		{
			m_pinListInfo = null;
			m_pinListExtent = null;
			m_isReference = false;
			m_withConductor = false;
			m_constraints = null;
			m_pinlistAddPinHelpers.clear();
		}

		private void setIsReference(boolean value)
		{
			m_isReference = value;
			for (PinListAddPinHelper helper : m_pinlistAddPinHelpers.values()) {
				helper.setIsReference(value);
			}
		}

		public void setWithConductor(boolean m_withConductor)
		{
			this.m_withConductor = m_withConductor;
		}

		private Collection<IGfxObject> getBoundaryExtensions()
		{
			PinPlacementConstraintsHolder constraints = getAnchorPinPlacementConstraints();
			return constraints != null ? Collections.unmodifiableCollection(constraints.getBoundaryExtensions()) :
					Collections.emptyList();
		}

		private void registerPinPlacementConstraints(@NotNull PinPlacementConstraintsHolder pinPlacementConstraints)
		{
			m_constraints = pinPlacementConstraints;
		}

		@NotNull private List<Integer> allow(Point currPt, @Nullable IAbstractPin placementPin,
				boolean addingBackshell, boolean editingStack, boolean assumeInfiniteExtBoundary)
		{
			List<Integer> result = new ArrayList<>();
			IPinList anchor = m_pinListInfo != null ? m_pinListInfo.getAnchor() : null;
			Map<IPinList, PinPlacementConstraintsHolder> constraints =
					determinePinPlacementConstraintsToCheck();
			for (Map.Entry<IPinList, PinPlacementConstraintsHolder> entry : constraints.entrySet()) {
				//always assume infinite boundary for children. otherwise it will fail to place at far away location.
				PinPlacementConstraintsHolder constraintsHolder = entry.getValue();
				boolean assumeInfiniteExtBoundary1 = (anchor != entry.getKey()) || assumeInfiniteExtBoundary;
				int allow = allow(currPt, placementPin, addingBackshell, editingStack, assumeInfiniteExtBoundary1,
						constraintsHolder);
				result.add(allow);
			}
			return Collections.unmodifiableList(result);
		}

		protected int allow(Point currPt, @Nullable IAbstractPin placementPin,
				boolean addingBackshell, boolean editingStack, boolean assumeInfiniteExtBoundary,
				PinPlacementConstraintsHolder constraintsHolder)
		{
			return constraintsHolder.allow(currPt, placementPin, addingBackshell, editingStack,
					assumeInfiniteExtBoundary);
		}

		@NotNull private Map<IPinList, PinPlacementConstraintsHolder> determinePinPlacementConstraintsToCheck()
		{
			Map<IPinList, PinPlacementConstraintsHolder> constraintsToCheck = new LinkedHashMap<>();
			IPinList anchor = m_pinListInfo != null ? m_pinListInfo.getAnchor() : null;
			PinPlacementConstraintsHolder constraints = anchor != null ? m_constraints : null;
			if (constraints != null) {
				constraintsToCheck.put(anchor, constraints);
			}
			return constraintsToCheck;
		}

		private boolean hasConstraints()
		{
			return m_constraints != null;
		}

		private Collection<IGfxObject> getValidMovePositions()
		{
			PinPlacementConstraintsHolder constraints = getAnchorPinPlacementConstraints();
			return constraints != null ? Collections.unmodifiableCollection(constraints.getValidMovePositions())
					: Collections.emptyList();
		}

		@Nullable private PinPlacementConstraintsHolder getAnchorPinPlacementConstraints()
		{
			return m_constraints;
		}

		@Nullable @Override public IGfxObject getObjectAtCurrentValidPoint(@Nullable Point currPt,
				@NotNull chs.cof.logical.cable.IPinList cablePlForStackPin)
		{
			PinPlacementConstraintsHolder constraints = m_constraints;
			return constraints != null && currPt != null ? constraints.getObjectAt(currPt) : null;
		}

		private boolean isReference()
		{
			return m_isReference;
		}

		public boolean isWithConductor()
		{
			return m_withConductor;
		}
	}
}