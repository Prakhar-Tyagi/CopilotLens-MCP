/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2006-2025 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.cof.draw.FlipAxisEnum;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGrid;
import chs.cof.drawplus.IAnchor;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.table.ITableData;
import chs.cof.logical.FootprintUtils;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IFunction;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IHarnessPlugConnector;
import chs.cof.logical.cable.IJackConnector;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.IPlugConnector;
import chs.cof.logical.footprint.IDeviceFootprintContext;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemFactory;
import chs.cof.parts.ILibraryDeviceFootprint;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.common.ICommonFactory;
import chs.common.IDesignContainer;
import chs.common.IParameterized;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.ctf.caf.utils.DeviceConnectorPinProxy;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.caf.utils.PinProxy;
import chs.ctf.caf.utils.PortProxy;
import chs.images.CHSImages;
import chs.services.dynamicgfx.DynamicRotationIndicator;
import chs.services.dynamicgfx.ISmartPoint;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utility.ConductorSplitter;
import chs.utility.DiagramHelper;
import chs.utility.helpers.CompositeConnectivityFinder;
import chs.utility.helpers.CompositePinConnectivityFinder;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.ModularSchemPinListInfo;
import chs.utility.logic.LogicUtils;
import chs.utility.logic.SizeHelper;
import chs.utility.preferences.PreferenceSetHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adds an instance of an existing pinlist (device, connector, etc) to a diagram as a parameterized object.
 * <p>
 * This is a "sub action" designed to be constructed and used by another action, rather than called via CAF
 */
public class AddParameterizedPinListAction extends CreateParameterizedObjectAction
{

	private IPinList pinlist = null;
	private AddPinListActionHelper addPinListActionHelper = null;
	protected chs.cof.logical.schem.IPinList createdSchematic;
	private List<IAbstractPin> pins;
	private List<IPinProxy> m_pinProxies;
	private boolean autogenerate;
	private Cursor cursor = null;
	private List/*Set*/<IUIDObject> m_preemies;
	private boolean reference;
	private boolean placeAsStack;
	private boolean placeAsGroup = false;

	public AddParameterizedPinListAction(ICapletController controller, IPinList pinlist, List<IAbstractPin> pins,
			boolean autogenerate, boolean reference, boolean placeAsStack, boolean placeAsGroup,
			List<IPinProxy> pinProxies)
	{
		super(controller);
		this.pins = pins;
		this.autogenerate = autogenerate;
		this.pinlist = pinlist;
		this.reference = reference;
		this.placeAsStack = placeAsStack;
		this.placeAsGroup = placeAsGroup;
		m_pinProxies = pinProxies;
	}

	protected boolean shouldShowFeedback()
	{
		return !(pinlist instanceof IBlockDevice);
	}

	public String getActionUIClass()
	{
		return AddPinListActionUI.class.getName();
	}

	@Override public IActionEnum onActivate(ActionEvent e)
	{
		// null out fields just in case, caller should have specified the pinlist
		addPinListActionHelper = null;
		createdSchematic = null;
		if (pinlist == null) {
			return IActionEnum.eCanceled;
		}

		// use the general purpose pinlist/pin placement helper while the action is activated
		// anything created here gets properly added to the diagram in the onTerminate
		addPinListActionHelper = new AddPinListActionHelper(this);

		autogenerate = shouldAutoGeneratePins();

		//addPinListActionHelper.setup(PinProxy.create(pins), autogenerate);
		IDeviceFootprintContext footPrint = null;
		// dts0101237638 [CH] java.lang.NullPointerException  at chs.utility.helpers.LibraryHelper.getFootPrintCavityMap(LibraryHelper.java:1488)
		if (pinlist instanceof IDevice) {
			footPrint = FootprintUtils.getDeviceFootprintContext((IDevice) pinlist);
		}
		List<IPinProxy> proxies = new ArrayList<IPinProxy>();
		proxies.addAll(m_pinProxies);
		if (footPrint != null &&
				footPrint.getFootprintType() == ILibraryDeviceFootprint.FootprintType.DEVICE_CONNECTOR) {
			IDesignContainer designContainer = CAFUtils.getInstance().getActiveDesignContainer();
			Map<String, String> fpCavityMap = FootprintUtils.getFootprintPinCavityMap(footPrint);
			proxies.addAll(DeviceConnectorPinProxy.create(pins, fpCavityMap, designContainer));
			addPinListActionHelper.setup(proxies, autogenerate, reference, placeAsStack, placeAsGroup);
		}
		else {
			if (pinlist != null && pinlist instanceof IFunction) {
				proxies.addAll(PortProxy.create(pins));
			}
			proxies.addAll(PinProxy.create(pins));
			addPinListActionHelper
					.setup(proxies/*PinProxy.create(pins)*/, autogenerate, reference, placeAsStack, placeAsGroup);
		}

		//m_preemies = new HashSet<IUIDObject>();
		m_preemies = new ArrayList<IUIDObject>();
		// we still need the base class to kick off the activation
		return super.onActivate(e);
	}

	@Override protected void preActionChanges()
	{
		super.preActionChanges();
		if (autogenerate && addPinListActionHelper != null && createdSchematic != null) {
			addPinListActionHelper.createMissingCablePinsFromProxies(createdSchematic.getConnectivity());
		}
	}

	public void keyPressed(KeyEvent event)
	{
		if (addPinListActionHelper != null &&
				addPinListActionHelper.getState() == AddPinListActionHelper.State.PLACING_PINS) {
			addPinListActionHelper.keyPressed(event);
		}
		else {
			super.keyPressed(event);
		}
	}

	@Override protected boolean postActionChanges()
	{
		LogicUtils.deferRegenerationOfSchemDeviceConnectors();
		ISchemDiagram diagram = (ISchemDiagram) getController().getCapletModel().getModelRoot();

		if (diagram == null || addPinListActionHelper == null || createdSchematic == null) {
			assert false;
			return false;
		}

		boolean actionSuccess = true;
		ModularSchemPinListInfo modularSchemPinListInfo = new ModularSchemPinListInfo(createdSchematic);
		if (modularSchemPinListInfo.getAllPins().isEmpty()) {
			// actually this just adds the pins!
			CompositePinConnectivityFinder pinConnectivityFinder = new CompositePinConnectivityFinder(diagram);
			actionSuccess = addPinListActionHelper.addPins(diagram, createdSchematic, pinConnectivityFinder);
			if (actionSuccess) {
				pinConnectivityFinder.connect();
				for (chs.cof.logical.schem.IPinList candidate : modularSchemPinListInfo.getCandidates()) {
					addPinListActionHelper.regenerateGraphics(candidate);
				}
			}
		}

		Set<chs.cof.logical.schem.IPinList> candidatesForPostAction = new LinkedHashSet<>();
		for (chs.cof.logical.schem.IPinList candidate : modularSchemPinListInfo.getCandidates()) {
			candidatesForPostAction.add(candidate);
			for (chs.cof.logical.schem.IPinList attachedPinList : candidate.getAttachedPinListObjects()) {
				IPinList connectivity = attachedPinList.getConnectivity();
				if (connectivity instanceof IHarnessPlugConnector) {
					candidatesForPostAction.add(attachedPinList);
				}
			}
		}
		for (chs.cof.logical.schem.IPinList candidate : candidatesForPostAction) {
			completePostAction(candidate, diagram);
		}
		return actionSuccess;
	}

	private static void completePostAction(@NotNull chs.cof.logical.schem.IPinList schematic,
			@NotNull ISchemDiagram diagram)
	{
		ObjectConnectionsGetter.createConnectionSchematics(schematic, diagram);
		PreferenceSetHelper.applyStyleSet(schematic.getObjectsForStyling(), diagram, true);
	}

	@Override protected void clearAction(boolean actionSuccess)
	{
		super.clearAction(actionSuccess);
		// If add pin action is cancelled after mouseReleased(), is called m_preemies will not be empty
		boolean pinCreationCancelled = false;
		if (!actionSuccess && !m_preemies.isEmpty()) {
			List<IUIDObject> toDelObjList = deleteChildObjects();
			for (IUIDObject object : toDelObjList) {
				CreationDeletionHelper.getTheCreationHelper().addDeletionObject(object);
			}
			pinCreationCancelled = true;
		}
		if (pinCreationCancelled) {
			// This is needed to update the usages and process edit in the case of cencel action (ESC button) while adding pins.
			getController().getUndoableContainer().startEdit("Add PinList");
			CreationDeletionHelper.getTheCreationHelper().processObjects();
			getController().getUndoableContainer().endEdit();
		}
		cleanup();
	}

	protected void cleanup()
	{
		createdSchematic = null;
		pinlist = null;
		addPinListActionHelper = null;
	}

	private List<IUIDObject> deleteChildObjects()
	{
		List<IUIDObject> topLevelObjList = new ArrayList<IUIDObject>();
		//Check if the parent object is also part of the current list
		for (IUIDObject obj : m_preemies) {
			IUIDObject parent = getParent(obj);
			if (parent == null || !m_preemies.contains(parent)) {
				topLevelObjList.add(obj);
			}
		}
		return topLevelObjList;
	}

	@Nullable private IUIDObject getParent(IUIDObject obj)
	{
		if (obj instanceof IDiagramObject) {
			IDiagramObject diagramObj = CommonUtils.cast(obj, IDiagramObject.class);
			return diagramObj != null && diagramObj.getContainer() instanceof IUIDObject ?
					(IUIDObject) diagramObj.getContainer() : null;
		}
		if (obj instanceof IAnchor) {
			IAnchor anchorObj = CommonUtils.cast(obj, IAnchor.class);
			return anchorObj != null ? (IUIDObject) anchorObj.getSourceObject() : null;
		}
		if (obj instanceof ITableData) {
			ITableData tableObj = CommonUtils.cast(obj, ITableData.class);
			return tableObj != null ? tableObj.getOwner() : null;
		}
		return null;
	}

	protected String getObjectType()
	{
		return "device";
	}

	@Override protected double calculateBorderSize()
	{
		ISchemDiagram diagram = (ISchemDiagram) getModel().getSheet();
		return calculateBorderSize(diagram);
	}

	public void connectGfxObjectToModel(IGfxObject newObject)
	{
		//dts0100646110 - Attaching instance of a connector to device always create new pins on connector even though when we select a non-connected pin from Place Pin dialog
		//Similar to AddSharedJackConnectorAction::OnTerminate
		ISchemDiagram diagram = (ISchemDiagram) getModel().getSheet();
		CompositePinConnectivityFinder pinConnectivityFinder = new CompositePinConnectivityFinder(diagram);
		addPinListActionHelper.addPins(getDiagram(), createdSchematic, pinConnectivityFinder);

		final ILogicDesign design = getDiagram().getDesign();
		assert design != null;
		CompositeConnectivityFinder finder = new CompositeConnectivityFinder(design);
		finder.addLockableObjects(pinConnectivityFinder.getLockables());
		if (newObject instanceof chs.cof.logical.schem.IPinList) {
			GfxView gview = (GfxView) CAFUtils.getInstance().getActiveCapletView();
			finder.addConnectionMakers(pinConnectivityFinder.getConnectionMakers());
			chs.cof.logical.schem.IPinList pinList = (chs.cof.logical.schem.IPinList) newObject;
			finder.connect(pinList, gview, allowPinCreationAtPlaceholders(), true);

			Runnable regenerateGraphics = () -> addPinListActionHelper.regenerateGraphics(pinList);
			splitConductors(pinList, diagram, regenerateGraphics);
		}
	}

	protected void splitConductors(chs.cof.logical.schem.IPinList schemPinlist, ISchemDiagram diagram,
			Runnable ghcRunner)
	{
		ILogicDesign design = diagram.getDesign();
		assert design != null;
		GfxView gview = (GfxView) CAFUtils.getInstance().getActiveCapletView();

		ConductorSplitter spliceSplitter = ConductorSplitter.createConductorSplitter(schemPinlist);
		spliceSplitter.splitConductors(schemPinlist, gview, false, false, true, ghcRunner);
	}

	@Override protected IGfxObject createParamObject(Point p1, Point p2)
	{
		// decision on whether the instance will be home is based on usages of the Logic object before this one
		boolean home = !LogicUtils.hasUsage(pinlist);

		chs.cof.logical.schem.IPinList pl = null;
		if (pinlist instanceof IDevice) {
			// create the device for preview gfx, without pins because these are always added by this action
			pl = CreateDeviceAction.createSchemDevice((IDevice) pinlist, p1, p2, false, getRotationIndicator());
		}
		if (pinlist instanceof IFunction) {
			// create the device for preview gfx, without pins because these are always added by this action
			pl = CreateFunctionAction.createSchemDevice((IFunction) pinlist, p1, p2, false, getRotationIndicator());
		}
		if (pinlist instanceof IBlockDevice) {
			// create the device for preview gfx, without pins because these are always added by this action
			pl = CreateBlockDeviceAction.createSchemBlockDevice((IBlockDevice) pinlist, p1, p2, getRotationIndicator());
		}
		else if (pinlist instanceof IGenericInlineConnector) {
			// inlines must be handled elsewhere - illegal state
		}
		else if (pinlist instanceof IConnector) {
			pl = createSchematicConnector(p1, p2);
		}
		// splices are handled elsewhere

		if (pl != null) {
			// First instance of a pinlist should be home, subsequent instances non-home
			// explicitly set both cases so we don't rely on the initial value in the ctor
			pl.setHome(home); // similar behaviour to shared
			return pl;
		}
		throw new IllegalStateException("Unexpected pinlist type");
	}

	private chs.cof.logical.schem.IPinList createSchematicConnector(Point p1, Point p2)
	{
		// couldnt reuse CreateConnectorAction.createParamObject - had to hack some of it here :(
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		GfxView gview = (GfxView) view;
		ISchemDiagram diagram = (ISchemDiagram) gview.getSheet();
		IGrid grid = diagram.getGrid();
		int pinspacing = grid.getGridSpacing();
		DynamicRotationIndicator indicator = getRotationIndicator();

		ISchemFactory schemFactory =
				FactoryMgr.getSchemFactory();
		ICommonFactory commonFactory = FactoryMgr.getCommonFactory();
		IUID uid = commonFactory.createUID();

		IParameterized params = commonFactory.createParameterized();
		GeneratorParameters gp = DiagramHelper.createGeneratorParameters(diagram);
		gp.setNewObject(true);
		Generator generator = Generator.getGenerator();

		// If the user specifies a 0 width connector we expand it to one grid space, else the pins of a plug
		// connector would be placed on the left.
		SizeHelper sizeH = new SizeHelper(p1, p2, indicator.getVertical(), gp, 1.0);
		sizeH.setMinModelWidth(pinspacing);
		Point lowerLeft = sizeH.getModelLocation();

		// Create visible schem representation & adds pins to it as well as the connectivity.
		chs.cof.logical.schem.IPinList schemConnector =
				schemFactory.constructPinList(uid, pinlist, lowerLeft.x, lowerLeft.y);
		schemConnector.setParameterized(params);
		diagram.addObject(schemConnector);

		// Generate for the first time to create the parameterized object, needed before we add the pins.
		params.setExtent(commonFactory.constructExtent(0, 0, sizeH.getModelWidth(), sizeH.getModelHeight()));
		generator.generateConnector(schemConnector, gp, false, Generator.REGENERATE_PROPERTIES);

		// Shift the pins from right to left. If necessary, a subsequent rotation will take them to the top or
		// (for flipped connectors) bottom edge. We flip on the mid point to avoid changing the connectors position.
		int width = sizeH.getModelWidth();
		if (indicator.getReversePinSide()) {
			schemConnector.flip(FlipAxisEnum.YAxis, lowerLeft.x + (width / 2), 0, 0, 0);
		}
		sizeH.rotateModel(schemConnector);

		return schemConnector;
	}

	/**
	 * Overridden here to allow a single pinlist to be created/stored by this action.
	 */
	// suppressed warnings here because it would be too much work to fix the base class and all implementations
	@SuppressWarnings({"CollectionDeclaredAsConcreteClass", "RawUseOfParameterizedType"})
	protected IGfxObject createDisplayObject(List<ISmartPoint> point_list)
	{
		if (createdSchematic == null) {
			createdSchematic = (chs.cof.logical.schem.IPinList) super.createDisplayObject(point_list);
		}
		return createdSchematic;
	}

	public void mouseMoved(MouseEvent e)
	{
		if (addPinListActionHelper != null) {
			addPinListActionHelper.mouseMoved(e);
			if (addPinListActionHelper.getState() != AddPinListActionHelper.State.PLACING_PINS) {
				super.mouseMoved(e);
			}
		}
	}

	public void mousePressed(MouseEvent e)
	{
		// AddPinListHelper handles all mourse events during pin placement,
		// still needs some help from the action base code during pinlist placement - should refactor
		if (addPinListActionHelper != null) {
			if (addPinListActionHelper.getState() != AddPinListActionHelper.State.PLACING_PINS) {
				super.mousePressed(e);
			}
		}
	}

	public void mouseReleased(MouseEvent e)
	{
		if (addPinListActionHelper != null) {
			if (addPinListActionHelper.getState() == AddPinListActionHelper.State.DRAWING_PARAMETERISED) {
				// currently we still construct the display object here using all the base code in this action hierarchy
				final List<ISmartPoint> pointList = getPointList();
				//null check is part of fix for dts0100619786 
				if (pointList != null) {
					IGfxObject displayObject = createDisplayObject(pointList);
					if (displayObject instanceof chs.cof.logical.schem.IPinList) {
						addPinListActionHelper.setDisplayObject((chs.cof.logical.schem.IPinList) displayObject);
					}
				}
			}

			CreationDeletionHelper cdh = CreationDeletionHelper.getTheCreationHelper();
			m_preemies.addAll(CollectionUtils.createList(cdh.getNewObjectsToProcess()));

			// forward any other UI interaction to the helper (e.g. adding pins)
			addPinListActionHelper.mouseReleased(e);

			// yes this field can get nulled out by the previous call!
			// this is because  mouseReleased is forwarded directly to the AddPinActionHelper,
			// which terminates the active action when mouseReleased is done for the last pin,
			// this field is then correctly cleared when the action is terminated (to avoid leaks)
			if (addPinListActionHelper != null) {
				if (addPinListActionHelper.getState() == AddPinListActionHelper.State.COMPLETE) {
					super.mouseReleased(e); // base class does it's thing to complete the activation
				}
			}
		}
	}

	public Cursor getCursor()
	{
		// this action is a sub action that gets called by AddPinAction
		// cursor will be null when the action is activated
		// cursors are cached anyway so we may not even need this field
		if (cursor == null) {
			ICaplet caplet = getController().getCaplet();
			Point spot = new Point(7, 7);
			String cursorPath = null;
			if (pinlist instanceof IDevice) {
				cursorPath = "chs/images/app/cur_device.gif";
			}
			else if (pinlist instanceof IFunction) {
				cursorPath = CHSImages.FUNCTION_ADD_CURSOR;
			}
			else if (pinlist instanceof IBlockDevice) {
				cursorPath = CHSImages.BLOCKDEVICE_CURSOR;
			}
			else if (pinlist instanceof IGenericInlineConnector) {
				cursorPath = "chs/images/app/cur_inline.gif";
			}
			else if (pinlist instanceof IConnector) {
				if (IConnector.Statics.isRingTerminalTypeConnector(pinlist)) {
					cursorPath = "chs/images/app/cur_ringterminal.gif";
				}
				else if (pinlist instanceof IJackConnector) {
					cursorPath = "chs/images/app/cur_connector_jack.gif";
				}
				else {
					cursorPath = "chs/images/app/cur_connector.gif";
				}
			}
			else {
				assert false : "Cursor not set for adding pinlist of this type";
				cursor = super.getCursor();
			}
			cursor = CAFUtils.getInstance().loadCursor(caplet, cursorPath, spot);
		}
		return cursor;
	}

	protected boolean shouldAutoGeneratePins()
	{
		//dts0100646110 - For PlugConnector, allow Interactive Placement in case,
		// existing pins are selected in 'Place Plug' dialog - Similar to SharedPlugConnector
		if (pinlist instanceof IConnector) {
			//So, for plug connector, some pins are selected in 'Place Plug' dialog
			//dont auto-generate
			if ((!pins.isEmpty() || !m_pinProxies.isEmpty()) && !shouldForceAutoGeneratePins()) {
				return false;
			}
		}

		return autogenerate;
	}

	protected boolean shouldForceAutoGeneratePins()
	{
		return false;
	}

	@Override protected boolean getIndicateBothEdges()
	{
		if (pinlist instanceof IPlugConnector || pinlist instanceof IJackConnector) {
			return false;
		}
		return super.getIndicateBothEdges();
	}

	@Override protected void setRotationIndicator(DynamicRotationIndicator indicator)
	{
		super.setRotationIndicator(indicator);
		getRotationIndicator().setIsJackStyle(pinlist instanceof IJackConnector);
	}

	protected chs.cof.logical.schem.IPinList getCreatedSchematic()
	{
		return createdSchematic;
	}
}
