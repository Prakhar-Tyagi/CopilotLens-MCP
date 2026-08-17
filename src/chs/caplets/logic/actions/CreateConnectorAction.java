/*
 * Copyright 2002-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.helpers.browser.IBrowserTreeContainer;
import chs.caplets.logic.ApplyLibraryPartOnPinlist;
import chs.caplets.logic.Model;
import chs.caplets.logic.shared.ISharedPinListInfoProvider;
import chs.cof.draw.FlipAxisEnum;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGrid;
import chs.cof.drawplus.ISchemCrossReferenceable;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.ICableFactory;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.IInlineJackConnector;
import chs.cof.logical.cable.IInlinePlugConnector;
import chs.cof.logical.cable.IInternalPosition;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IRingTerminal;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IPinListShapeDescriptor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.parts.ILibraryBaseConnector;
import chs.cof.parts.ILibraryCavity;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cofUtils.parameterized.AddPinHelper;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.GeneratorStyle;
import chs.common.ICommonFactory;
import chs.common.IExtent;
import chs.common.IParameterized;
import chs.common.IReadOnlyNamedObject;
import chs.common.IUID;
import chs.common.preferencesets.IPreferenceSet;
import chs.ctf.caf.utils.IGenericPinProxy;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.caf.utils.LibraryPinMapProvider;
import chs.ctf.caf.utils.PinMapProviderFactory;
import chs.ctf.caf.utils.PinMappings;
import chs.services.dynamicgfx.DynamicRotationIndicator;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.utilities.CHSConstants;
import chs.utilities.CommonUtils;
import chs.utilities.ListMap;
import chs.utilities.ReverseMap;
import chs.utilities.SortedList;
import chs.utility.DiagramHelper;
import chs.utility.helpers.CompositeConnectivityFinder;
import chs.utility.helpers.CompositePinConnectivityFinder;
import chs.utility.helpers.ConnectorHelper;
import chs.utility.helpers.ExtentHelper;
import chs.utility.helpers.LibraryHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.NamedObjectComparator;
import chs.utility.helpers.PropertyCopier;
import chs.utility.logic.ILogicModel;
import chs.utility.logic.LogicUtils;
import chs.utility.logic.ModularConnectorHelper;
import chs.utility.logic.SizeHelper;
import chs.utility.preferences.PreferenceSetHelper;
import chs.utility.ui.SharedPinListEditUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class exists for typing only.
 */
public abstract class CreateConnectorAction extends CreateParameterizedObjectAction
{

	public static final PinListTypeEnum PLUG_CONNECTOR = PinListTypeEnum.TypePlug;
	public static final PinListTypeEnum JACK_CONNECTOR = PinListTypeEnum.TypeJack;
	public static final PinListTypeEnum INLINE_JACK_CONNECTOR = PinListTypeEnum.TypeInlineJack;
	public static final PinListTypeEnum INLINE_PLUG_CONNECTOR = PinListTypeEnum.TypeInlinePlug;
	public static final PinListTypeEnum INLINE_INTERCONNECT_JACK_CONNECTOR = PinListTypeEnum.TypeInlineInterconnectJack;
	public static final PinListTypeEnum INLINE_INTERCONNECT_PLUG_CONNECTOR = PinListTypeEnum.TypeInlineInterconnectPlug;
	public static final PinListTypeEnum INTERCONNECT_CONNECTOR = PinListTypeEnum.TypeInterconnectConnector;
	public static final PinListTypeEnum RINGTERMINAL_CONNECTOR = PinListTypeEnum.TypeRingTerminal;

	private static Cursor m_connectorCursor = null;

	/**
	 * The connector subtype.
	 */
	protected PinListTypeEnum m_subType = PLUG_CONNECTOR;
	protected String m_subTypeName = null;

	protected boolean m_pinsOnLeft = false;

	private IParameterized params;
	private Generator generator;
	private GeneratorParameters genParams;

	protected ISharedPinListInfoProvider m_addPinListDialog;

	protected static final int AUTO_GEN_CONN_WIDTH = 5;

	@SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod", "NonThreadSafeLazyInitialization"})
	protected CreateConnectorAction(ICapletController controller)
	{
		super(controller);
		if (m_connectorCursor == null) {
			m_connectorCursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/cur_connector.gif", new Point(7, 7));
		}
	}

	protected double calculateBorderSize()
	{
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		GfxView gview = (GfxView) view;
		ISchemDiagram diagram = (ISchemDiagram) gview.getSheet();
		GeneratorParameters gp = DiagramHelper.createGeneratorParameters(diagram);
		return gp.getSpacing();
	}

	@Override protected String getObjectType()
	{
		return "connector";
	}

	protected boolean getIndicateBothEdges()
	{
		return false;
	}

	/**
	 * Gets the ActionUIClass attribute of the CreateCircleAction object
	 *
	 * @return The ActionUIClass value
	 */
	public String getActionUIClass()
	{
		return CreateConnectorActionUI.class.getName();
	}

	@Nullable
	protected IConnector getLogicConnector()
	{
		ICableFactory cblFactory = FactoryMgr.getCablePropertiedFactory();
		IUID uid = FactoryMgr.getCommonFactory().createUID();
		IConnector connector = null;
		if (getSubType() == PLUG_CONNECTOR) {
			connector = cblFactory.createPlugConnector(uid);
		}
		else if (getSubType() == RINGTERMINAL_CONNECTOR) {
			IRingTerminal ringterminal = cblFactory.createRingTerminal("plug", uid);
			//In 2011.1, ring terminal implementation is based off on connector implementation.
			//So, ring terminal can be type casted to a connector object.
			connector = CommonUtils.cast(ringterminal, IConnector.class);
		}
		else if (getSubType() == JACK_CONNECTOR) {
			connector = cblFactory.createJackConnector(uid);
		}
		else if (getSubType() == INLINE_JACK_CONNECTOR) {
			connector = cblFactory.createInlineJackConnector(uid);
		}
		else if (getSubType() == INLINE_PLUG_CONNECTOR) {
			connector = cblFactory.createInlinePlugConnector(uid);
		}
		else if (getSubType() == INLINE_INTERCONNECT_JACK_CONNECTOR) {
			connector = cblFactory.createInlineInterconnectJackConnector(uid);
		}
		else if (getSubType() == INLINE_INTERCONNECT_PLUG_CONNECTOR) {
			connector = cblFactory.createInlineInterconnectPlugConnector(uid);
		}
		else if (getSubType() == INTERCONNECT_CONNECTOR) {
			connector = cblFactory.createInterconnectConnector(uid);
		}
		ILogicDesign logicDesign = ((ILogicModel) getModel()).getDesign();
		if (connector != null && logicDesign != null) {
			if (LogicObjectLockFinder.tryEdit(logicDesign, connector)) {
				IConnectivity connectivity = logicDesign.getConnectivity();
				assert connectivity != null;
				connectivity.addConnector(connector);
			}
			else {
				connector = null;
			}
		}
		return connector;
	}

	protected IGfxObject createParamObject(Point p1, Point p2)
	{
		// Create our connectivity device
		IConnector connector = getLogicConnector();
		assert connector != null;

		ICapletView view = CAFUtils.getInstance().getViewForDiagram(getDiagram());
		GfxView gview = (GfxView) view;
		ISchemDiagram diagram = (ISchemDiagram) gview.getSheet();
		IGrid grid = diagram.getGrid();
		int pinspacing = grid.getGridSpacing();
		DynamicRotationIndicator indicator = getRotationIndicator();
		IUID uid = FactoryMgr.getCommonFactory().createUID();

		generateParameters();
		SizeHelper sizeH = new SizeHelper(p1, p2, indicator.getVertical(), genParams, 1.0);

		// If the user specifies a 0 width connector we expand it to one grid space, else the pins of a plug
		// connector would be placed on the left.
		sizeH.setMinModelWidth(pinspacing);
		int height = sizeH.getModelHeight();
		int width = sizeH.getModelWidth();
		Point lowerLeft = sizeH.getModelLocation();

		// Create visible schem representation & adds pins to it as well as the connectivity.

		IPinList schem_conn = FactoryMgr.getSchemFactory().constructPinList(uid, connector, lowerLeft.x, lowerLeft.y);
		diagram.addObject(schem_conn);
		schem_conn.setParameterized(params);

		if (schem_conn.supportsFill()) {
			schem_conn.setOutlineShapeVersion(IPinListShapeDescriptor.OutlineShapeVersion.getLatestVersion());
		}

		// Generate for the first time to create the parameterized object, needed before we add the pins.
		generateConnGraphics(schem_conn, pinspacing, Generator.REGENERATE_PROPERTIES, sizeH);

		boolean regenerateConnGraphics = assignLibraryPart(connector, getLibrarySelectedObject());

		//we must do all the transformation on anchor pinlist before creating pins. because the transformation
		//would be used while creating child modular connector schematics.

		// Shift the pins from right to left. If necessary, a subsequent rotation will take them to the top or
		// (for flipped connectors) bottom edge. We flip on the mid point to avoid changing the connectors position.
		if (indicator.getReversePinSide()) {
			schem_conn.flip(FlipAxisEnum.YAxis, lowerLeft.x + (width / 2), 0, 0, 0);
		}

		// DR 397170: We no longer apply property styles for pins here, it is done in GenerateHelper.generatePin()
		// and we do not want two copies of the attributes.

		sizeH.rotateModel(schem_conn);

		//add pins only after modular connector is properly created so that hierarchy of connector with pins is created.
		addPins(width, height, grid, schem_conn, connector, indicator.getReversePinOrder());

		// Must generate for the second time after assigning library part so that the pin graphics will be drawn.
		// DR 348343 - exclude properties this time round, those copied from the library part should not be visible
		// by default.
		if (regenerateConnGraphics) {
			generateConnGraphics(schem_conn, pinspacing, Generator.NOREGENERATE_PROPERTIES, sizeH);
		}

		// Restyle the generated connector
		PreferenceSetHelper
				.applyStyleSet(schem_conn.getObjectsForStyling(), genParams.getStyleSet(), true);

		//dts0100830418:SR2446865780, we can have styling such that, there are no pinlist parameters. It may
		//have only pin parameters and we are constructing this param connector without pins. in this case
		//we will having zero reference width and inline will not be connected by connection helper and will
		//endup in validation reported by the referred sr in above numbered defect.
		//dts0100878254:need to set is directly because due to shape style we may get overlaping reference width
		//in jack/plug of inlines. what actually a reference width should be??
		schem_conn.setReferenceWidth(width);
		return schem_conn;
	}

	protected boolean assignLibraryPart(@NotNull IConnector connector, @Nullable ILibraryPartSelection libSelObj)
	{
		if (libSelObj != null) {
			// Tie to library object...
			assert libSelObj.getSelectedObject() != null : "Libary part must be there in library selection object ";
			connector.assignLibraryDetails(libSelObj);
			connector.updatePositionsFromLibrary(libSelObj.getSelectedObject());
			connector.removeAllSymbolRef();
			ILibraryBaseConnector libraryConnector =
					CommonUtils.cast(libSelObj.getSelectedObject(), ILibraryBaseConnector.class);
			if (libraryConnector != null) {
				ILibraryObject refConnector = libraryConnector.getReferencedConnector();
				if (refConnector != null) {
					boolean isPuchasedPart = libraryConnector.getPurchasedPart().isTrue();
					ModularConnectorHelper.updateIncludeOnBOMOnAeroModularConnector(connector, libraryConnector,
							isPuchasedPart, true);
					ModularConnectorHelper.createAeroModularHierarchy(connector, libraryConnector,
							() -> getLogicConnector(), isPuchasedPart);
				}
			}
			return true;
		}
		return false;
	}

	@SuppressWarnings({"NumericCastThatLosesPrecision"})
	protected String getPinCountStr(IExtent ext, int spacing)
	{
		int length = ext.getHeight();
		if (!getRotationIndicator().getVertical()) {
			length = ext.getWidth();
		}
		double borderSize = getBorderSize();
		if (length >= (int) (borderSize * 2)) {
			length -= (int) (borderSize * 2);
		}
		else if (length >= (int) borderSize) {
			length -= (int) borderSize;
		}
		return String.valueOf(((length / spacing) + 1));
	}

	private void generateParameters()
	{
		ICommonFactory commonFactory = FactoryMgr.getCommonFactory();
		//
		// Get the generator, add the defaults, and go!
		//
		params = commonFactory.createParameterized();

		ISchemDiagram diagram = (ISchemDiagram) getLocalModel().getSheet();
		genParams = DiagramHelper.createGeneratorParameters(diagram);
		genParams.setNewObject(true);

		GeneratorStyle gs = getGenerator().getStyle();
		if (m_subTypeName != null) {
			gs.addDefaults(params, "connector", m_subTypeName);
		}
		else {
			PinListTypeEnum subType = getSubType();

			ConnectorHelper.addDefaults(params, gs, subType);
		}
	}

	private Generator getGenerator()
	{
		if (generator == null) {
			generator = Generator.getGenerator();
		}
		return generator;
	}

	protected void generateConnGraphics(IPinList schem_conn, int pinspacing, int generationMode, SizeHelper sizeH)
	{
		ICommonFactory commonFactory = FactoryMgr.getCommonFactory();
		params.setExtent(commonFactory.constructExtent(0, 0, sizeH.getModelWidth(), sizeH.getModelHeight()));

		getGenerator().generateConnector(schem_conn, genParams, false, generationMode);
	}

	private static class AutoGenPinArgs extends AbstractAddPinArgs
	{

		@NotNull private IConnector m_conn;
		@Nullable private ILibraryCavity m_cavity;

		private AutoGenPinArgs(@NotNull IConnector conn, @NotNull Point loc, @Nullable ILibraryCavity cavity)
		{
			super(loc);
			m_conn = conn;
			m_cavity = cavity;
		}

		@NotNull @Override public IConnector getCablePinlist()
		{
			return m_conn;
		}

		@Nullable public ILibraryCavity getCavity()
		{
			return m_cavity;
		}
	}

	protected List<?> addPins(int width, int height, IGrid grid, IPinList schem_conn,
			IConnector connector, boolean topdown)
	{
		return addPins(width, height, 0, grid, schem_conn, connector, topdown);
	}

	protected List<IPin> addPins(int width, int height, int verticalOffset, IGrid grid, IPinList schem_conn,
			IConnector connector, boolean topdown)
	{

		int pinspacing = grid.getGridSpacing();
		List<IPin> addedPins = new ArrayList<IPin>();

		boolean ADD_PINS = shouldAddPins();
		if (ADD_PINS) {
			ISchemDiagram diagram = getDiagram();
			IPreferenceSet styleSet = PreferenceSetHelper.getStyleSet(diagram);
			List<AutoGenPinArgs> pinArgs = new ArrayList<>();
			collectAddPinArgs(connector, width, height, verticalOffset, topdown, pinspacing, pinArgs);

			ListMap<IPinList, AutoGenPinArgs> distribution = new ListMap<>();
			ConnectorHelper.distributeAddPinArgsToPinLists(schem_conn, diagram, pinArgs,
					(pl, a) -> distribution.add(pl, (AutoGenPinArgs) a));

			for (Map.Entry<IPinList, List<AutoGenPinArgs>> entry : distribution.entrySet()) {
				IPinList pinList = entry.getKey();
				for (AutoGenPinArgs pinArg : entry.getValue()) {
					ILibraryCavity cavity = pinArg.getCavity();
					Point2D loc = pinArg.getPoint();
					int x = CommonUtils.toInteger(loc.getX());
					int y = CommonUtils.toInteger(loc.getY());
					IPin pin = AddPinHelper.createPin(pinList, pinArg.getCablePinlist(), x, y
							, null, null, null, null, null);
					IAbstractPin cableConnPin = pin.getConnectivity();
					if (cavity != null) {
						cableConnPin.setName(cavity.getName());
						cableConnPin.assignLibraryCavity(cavity);
					}
					LogicUtils.setMatchingShortDescriptionFromOTI(cableConnPin, cableConnPin.getProject());
					addedPins.add(pin);
				}

				Collection<IPinList> oldePinLists = pinList.getAttachedPinListObjects();
				GeneratorParameters gp = new GeneratorParameters(grid, styleSet);
				getGenerator().generate(pinList, gp, Generator.NOREGENERATE_PROPERTIES, false);

				for (IPin addedPin : addedPins) {
					AddPinHelper.regeneratePin(oldePinLists, gp, getGenerator(), pinList, styleSet, addedPin);
				}
			}
		}
		return addedPins;
	}

	private void collectConnectorPinsToProcess(@NotNull IConnector topConnector,
			@NotNull List<AutoGenPinArgs> connectorPinsToProcess)
	{
		ILibraryObject libraryObject = CommonUtils.cast(topConnector.getLibraryObject(), ILibraryObject.class);
		if (libraryObject != null) {
			Set<ILibraryCavity> cavities = LibraryHelper.getCavities(libraryObject);
			Collection<String> blockedCavities = topConnector.getBlockedCavities();
			for (ILibraryCavity cavity : cavities) {
				if (!blockedCavities.contains(cavity.getName())) {
					connectorPinsToProcess.add(new AutoGenPinArgs(topConnector, new Point(), cavity));
				}
			}
		}
		Map<IInternalPosition, IConnector> children = new HashMap<>();
		for (IConnector childConnector : topConnector.getChildConnectorsBlockingParentCavities()) {
			IInternalPosition occupiedPosition = childConnector.getOccupiedPosition();
			if (occupiedPosition != null) {
				children.put(occupiedPosition, childConnector);
			}
		}
		SortedList<IInternalPosition> positionSortedList =
				new SortedList<>(new NamedObjectComparator<IInternalPosition>()
				{
					@Override protected String getString(IInternalPosition object)
					{
						//internal position is not IReadOnlyNamedObject.
						//so default implementation doesn't work for it.
						//hence causing intermittency for modular connector creation.
						return object.getName();
					}
				});
		positionSortedList.addAll(topConnector.getPositions());
		for (IInternalPosition position : positionSortedList) {
			IConnector child = children.get(position);
			if (child != null) {
				collectConnectorPinsToProcess(child, connectorPinsToProcess);
			}
		}
	}

	private void collectAddPinArgs(@NotNull IConnector connector, int width, int height, int verticalOffset,
			boolean topdown, int pinspacing, @NotNull List<AutoGenPinArgs> pinArgs)
	{
		ILibraryPartSelection libSelObj = getLibrarySelectedObject();
		Iterator<AutoGenPinArgs> pinArgsIterator = null;
		if (libSelObj != null) {
			List<AutoGenPinArgs> connectorPinsToProcess = new ArrayList<>();
			collectConnectorPinsToProcess(connector, connectorPinsToProcess);
			pinArgsIterator = connectorPinsToProcess.iterator();
		}
		for (int idx = 0; (idx * pinspacing) <= height; idx++) {
			AutoGenPinArgs element;
			if (pinArgsIterator != null) {
				if (!pinArgsIterator.hasNext()) {
					break;
				}
				element = pinArgsIterator.next();
			}
			else {
				element = new AutoGenPinArgs(connector, new Point(), null);
			}
			int ypos;
			if (topdown) {
				ypos = height - (idx * pinspacing);
			}
			else {
				ypos = (idx * pinspacing);
			}
			element.setPoint(new Point((m_pinsOnLeft) ? 0 : width, ypos + verticalOffset));
			pinArgs.add(element);
		}
	}

	protected boolean shouldAddPins()
	{
		return true;
	}

	protected Model getLocalModel()
	{
		return (Model) getModel();
	}

	protected Class<?> snappingSource()
	{
		return IPinList.class;
	}

	public Cursor getCursor()
	{
		return m_connectorCursor;
	}

	/**
	 * Returns the subType.
	 *
	 * @return int
	 */
	public PinListTypeEnum getSubType()
	{
		return m_subType;
	}

	/**
	 * Sets the subType.
	 *
	 * @param subType The subType to set
	 */
	public void setSubType(PinListTypeEnum subType)
	{
		m_pinsOnLeft = subType.isJack();
		m_subType = subType;
	}

	@Nullable
	public ILibraryPartSelection getLibrarySelectedObject()
	{
		return null;
	}

	protected final void addAutoGenPins(IPinList schem_conn, @Nullable IPinList schem_conn_mate,
			CompositePinConnectivityFinder connectivityFinder)
	{
		if (getState() != STATE_GENERATE) {
			return;
		}

		//why should take tables etc into account here. do we really need to play with width here?
		IExtent ext = ExtentHelper.getConnSelectiveExtent(schem_conn, null);
		int width = ext.getWidth();
		IParameterized param = schem_conn.getParameterized();
		if (param != null) {
			IExtent pExt = param.getExtent();
			pExt.setWidth(width);
		}

		//need to add the created cable pins to its owner also otherwise below
		//partitioning for inlines will not work.
		IConnector connector = getLogicConnector();
		assert connector != null;
		Collection<?> pins = SharedPinListEditUtils.createAndAddCablePins(connector,
				m_addPinListDialog.getUsedPins(), m_addPinListDialog.getReference(), true);

		ISchemDiagram diagram = (ISchemDiagram) getLocalModel().getSheet();
		List<IAbstractPin> abstractPins = new ArrayList<IAbstractPin>(pins.size());
		for (Object obj : pins) {
			IAbstractPin pin = null;
			if (obj instanceof IAbstractPin) {
				pin = (IAbstractPin) obj;
			}
			else if (obj instanceof IGenericPinProxy) {
				IGenericPin gpin = ((IGenericPinProxy) obj).getCablePin();
				if (gpin instanceof IAbstractPin) {
					pin = (IAbstractPin) gpin;
				}
			}
			if (pin != null) {
				abstractPins.add(pin);
			}
		}
		// clients must pass schem mates for inlines
		if (schem_conn.getConnectivity() instanceof IGenericInlineConnector) {
			//TODO:warning autogenerateSchematicPins is not working for shared objects because
			//the mate pin creation is dependent upon connetion helper and that doesn't allow
			//the mate pin creation if pin/owner are shared objects. so this is a hack to
			//call them twice to generate the schem pins on both sides of inlines.
			//need to work and find out better way to handle this case. chandras
			List<IAbstractPin> plugPins = new ArrayList<IAbstractPin>(pins.size());
			List<IAbstractPin> jackPins = new ArrayList<IAbstractPin>(pins.size());
			if (schem_conn.getConnectivity() instanceof IInlineJackConnector) {
				assert schem_conn_mate != null && schem_conn_mate.getConnectivity() instanceof IInlinePlugConnector;
				AddPinListActionHelper.partitionInlinePins(plugPins, jackPins, abstractPins);
				assert plugPins.size() == jackPins.size();
				assert plugPins.size() == abstractPins.size();
				abstractPins.clear();
				PinListAddPinHelper.autogenerateSchematicPins(diagram, schem_conn, schem_conn_mate, jackPins,
						m_addPinListDialog.getReference());
				PinListAddPinHelper.autogenerateSchematicPins(diagram, schem_conn_mate, schem_conn, plugPins,
						m_addPinListDialog.getReference());
			}
			else {
				assert schem_conn_mate != null && schem_conn_mate.getConnectivity() instanceof IInlineJackConnector;
				AddPinListActionHelper.partitionInlinePins(plugPins, jackPins, abstractPins);
				assert plugPins.size() == jackPins.size();
				assert plugPins.size() == abstractPins.size();
				abstractPins.clear();
				PinListAddPinHelper.autogenerateSchematicPins(diagram, schem_conn, schem_conn_mate, plugPins,
						m_addPinListDialog.getReference());
				PinListAddPinHelper.autogenerateSchematicPins(diagram, schem_conn_mate, schem_conn, jackPins,
						m_addPinListDialog.getReference());
			}
		}
		else {
			PinListAddPinHelper.autogenerateSchematicPins(diagram, schem_conn, schem_conn_mate, abstractPins,
					m_addPinListDialog.getReference(), connectivityFinder);
		}
	}

	protected void updatePinMappingAfterConnection(Collection<? extends IGfxObject> displayObjects)
	{
		for (IGfxObject displayObject : displayObjects) {
			if (displayObject instanceof IPinList) {
				ISchemDiagram diagram = ((ISchemCrossReferenceable) displayObject).getDiagram();
				IPinList schemPinList = (IPinList) displayObject;
				chs.cof.logical.cable.IPinList cablePinList = schemPinList.getConnectivity();
				ILibraryObject libraryObject =
						CommonUtils.cast(cablePinList.getLibraryObject(), ILibraryObject.class);
				if (libraryObject != null && cablePinList instanceof IConnector) {

					LibraryPinMapProvider pinMapProvider = PinMapProviderFactory.instance()
							.createLibraryPinMapperProvider(libraryObject, cablePinList);
					//Do not use the default mapping. Even after ignoring default mapping, if there is a mapping between the mates
					//then there is a need to reconfigure.
					ReverseMap<IReadOnlyNamedObject, IPinProxy> mapping =
							pinMapProvider.generateMappingWithoutDefault();
					if (!mapping.isEmpty()) {
						ApplyLibraryPartOnPinlist applyLibraryPart = new ApplyLibraryPartOnPinlist();
						final List<IAbstractPin> unmappedPins = new ArrayList<IAbstractPin>();
						applyLibraryPart.applyPinMapping(schemPinList.getConnectivity(), new PinMappings(mapping),
								unmappedPins);
						//We should not delte the unmapped pins and leave it to the user to decide on deletion.
						applyLibraryPart.deletePins(unmappedPins, diagram);
						PropertyCopier.copyCavityAttributesAndProperties(cablePinList, libraryObject);

						regenerate(cablePinList, diagram);
					}
				}
			}
		}
	}

	private void regenerate(ILogicObject logObj, ISchemDiagram diagram)
	{
		GeneratorParameters gp = setupGeneratorParameters(diagram);

		if (logObj instanceof IConnector) {
			if (logObj instanceof IPinList) {
				getGenerator().generateConnector((IPinList) logObj, gp);
			}
		}
	}

	private GeneratorParameters setupGeneratorParameters(ISchemDiagram diagram)
	{
		//
		// Try to get the preferences and use those for initialization...
		//
		GeneratorParameters gp;
		if (diagram == null) {
			gp = new GeneratorParameters(CHSConstants.PIN_SPACING);
		}
		else {
			gp = DiagramHelper.createGeneratorParameters(diagram);
		}
		return gp;
	}

	protected boolean hasCavities(ILibraryObject libObj)
	{
		return !LibraryHelper.getCavities(libObj).isEmpty();
	}

	protected void enableDesignTreeExpansion(boolean enabled)
	{
		JComponent browser = getController().getBrowser();
		if (browser instanceof IBrowserTreeContainer) {
			((IBrowserTreeContainer) browser).setHomeTreeExpansionEnabled(enabled);
		}
	}

	// If libary object is a modular and has no pins, connector could be created with out instance of the connector on the diagram
	protected boolean shouldCreateModularConnectorWithOutInstance(ILibraryObject libraryObject)
	{
		return LibraryHelper.isModularConnector(libraryObject) && !hasCavities(libraryObject);
	}

	@Override protected void connectGfxObjectToModel(IGfxObject newObject)
	{
		if (newObject instanceof IPinList) {
			GfxView gview = (GfxView) CAFUtils.getInstance().getActiveCapletView();
			final ILogicDesign design = getDiagram().getDesign();
			assert design != null;
			CompositeConnectivityFinder finder = new CompositeConnectivityFinder(design);
			finder.connect((IPinList) newObject, gview, allowPinCreationAtPlaceholders(), true, isCtrlDown());
		}
	}
}
