/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2005-2026 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caplets.logic.actions.ghc.ConnectivityGHCHelper;
import chs.caplets.logic.actions.ui.ManageConnectorsDialog;
import chs.cof.draw.IGfxContext;
import chs.cof.draw.IGrid;
import chs.cof.draw.ISheet;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.icd.IDeviceICD;
import chs.cof.logical.FootprintUtils;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnPin;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.IFunction;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IInternalPin;
import chs.cof.logical.cable.IPlugConnector;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.shared.ISharedDevice;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.parts.ILibraryBaseObject;
import chs.cof.parts.ILibraryBatchLoader;
import chs.cof.parts.ILibraryCavity;
import chs.cof.parts.ILibraryDevice;
import chs.cof.parts.ILibraryDeviceFootprint;
import chs.cof.parts.ILibraryHousingDefinition;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.ILibraryPartMgr;
import chs.cofUtils.CreationUtils;
import chs.cofUtils.parameterized.AddPinHelper;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parts.LibraryBatchLoader;
import chs.cofUtils.parts.LibraryLazyLoadRequestGenerator;
import chs.common.IDesignDescriptor;
import chs.common.IExtent;
import chs.common.IParameterized;
import chs.common.attr.IAttributeTypes;
import chs.common.autoLoad.ILibraryLazyLoadRequestGenerator;
import chs.common.preferencesets.IPreferenceSet;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.ListMap;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.DiagramHelper;
import chs.utility.ICDUtils;
import chs.utility.helpers.CompositePinConnectivityFinder;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.ConnectionHelperCreationParams;
import chs.utility.helpers.ConnectorHelper;
import chs.utility.helpers.ILibraryObjectInfoCache;
import chs.utility.helpers.LibraryHelper;
import chs.utility.helpers.LibraryObjectInfoCache;
import chs.utility.helpers.PinHomeConditionControl;
import chs.utility.helpers.PinListHelper;
import chs.utility.logic.DeferPinListRegenerationAutoClosable;
import chs.utility.logic.LogicUtils;
import chs.utility.preferences.PreferenceSetHelper;
import chs.utility.ui.PinConductorConnectionSortHelper;
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.Table;
import com.mentor.capital.javafx.table.cell.CapitalTableCell;
import com.mentor.capital.javafx.table.cell.IGenericTableCell;
import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.Popup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PinListAddPinHelper
{

	protected boolean m_isReference;
	protected IPinList m_pinList;

	public PinListAddPinHelper(IPinList pinlist, boolean isReference)
	{
		m_pinList = pinlist;
		m_isReference = isReference;
	}

	public void setIsReference(boolean isReference)
	{
		m_isReference = isReference;
	}

	public IPin addPin(ISchemDiagram diagram, Point2D p2, IPinList paramObj, chs.cof.logical.cable.IPinList device,
			@Nullable IAbstractPin existingConnectivity, @Nullable String pinName)
	{
		return addPin(diagram, p2, paramObj, device, existingConnectivity, null, pinName, null, null, null);
	}

	public IPin addPin(ISchemDiagram diagram, Point2D p2, IPinList paramObj, chs.cof.logical.cable.IPinList device,
			@Nullable IAbstractPin existingConnectivity, @Nullable IInternalPin internalPin, @Nullable String pinName,
			@Nullable String associatedObjectType, @Nullable String associatedObject,
			@Nullable String associatedPinName)
	{
		// Add the pin (it Generates the gfx for us)
		IPin newpin = createPin(paramObj, device, p2, existingConnectivity, internalPin, pinName, associatedObjectType,
				associatedObject, associatedPinName);
		generatePin(diagram, paramObj, device, existingConnectivity, m_pinList.getConnectivity(), newpin);
		return newpin;
	}

	public IPin addPinOnly(ISchemDiagram diagram, Point2D p2, IPinList paramObj, chs.cof.logical.cable.IPinList device,
			IAbstractPin existingConnectivity, IInternalPin internalPin, String pinName,
			String associatedObjectType, String associatedObject, String associatedPinName)
	{
		// Add the pin (it Generates the gfx for us)
		IPin newpin = createPin(paramObj, device, p2, existingConnectivity, internalPin, pinName, associatedObjectType,
				associatedObject, associatedPinName);
		generatePinOnly(diagram, paramObj, device, existingConnectivity, m_pinList.getConnectivity(), newpin);
		return newpin;
	}

	protected IPin createPin(IAbstractPin existingConnectivity, Point2D p2)
	{
		return createPin(m_pinList, m_pinList.getConnectivity(), p2, existingConnectivity, null, null, null, null,
				null);
	}

	protected IPin createPin(Pair<IAbstractPin, Boolean> existingConnectivity, Point2D p2)
	{
		IPin newpin = AddPinHelper.createPin(m_pinList, m_pinList.getConnectivity(), (int) p2.getX(), (int) p2.getY(),
				existingConnectivity);
		return newpin;
	}

	private IPin createPin(IPinList paramObj, chs.cof.logical.cable.IPinList device, Point2D p2,
			@Nullable IAbstractPin existingConnectivity, @Nullable IInternalPin internalPin, @Nullable String pinName,
			@Nullable String associatedObjectType,
			@Nullable String associatedObject, @Nullable String associatedPinName)
	{
		//noinspection NumericCastThatLosesPrecision
		IPin newpin = AddPinHelper.createPin(paramObj, device, (int) p2.getX(), (int) p2.getY(),
				existingConnectivity, internalPin, associatedObjectType, associatedObject, associatedPinName);
		IAbstractPin connectivityPin = newpin.getConnectivity();
		createPinName(connectivityPin, pinName);
		LogicUtils.setMatchingShortDescriptionFromOTI(connectivityPin, connectivityPin.getProject());
		return newpin;
	}

	protected void createPinName(IAbstractPin newpin, @Nullable String pinName)
	{
		if (pinName != null) {
			newpin.setName(pinName);
		}
	}

	private void generatePin(ISchemDiagram diagram, IPinList paramObj, chs.cof.logical.cable.IPinList device,
			@Nullable IAbstractPin existingConnectivity, chs.cof.logical.cable.IPinList plc, IPin newpin)
	{
		// if the connectivity exists, assign the libray info
		if (existingConnectivity == null) {
			assignLibraryCavity(newpin);
		}
		IGrid grid = diagram.getGrid();
		IPreferenceSet styleSet = PreferenceSetHelper.getStyleSet(diagram);
		Collection<IPinList> oldePinLists = paramObj.getAttachedPinListObjects();
		// Not sure why styleset is required to construct parameters. Generator seems to be ineterested only in grid spacing and width
		// Check and remove if not required
		GeneratorParameters gp = new GeneratorParameters(grid, styleSet);
		Generator generator = Generator.getGenerator();
		AddPinHelper.regeneratePin(oldePinLists, gp, generator, paramObj, styleSet, newpin, false);

		setReferenceState(m_isReference, newpin);

		// Connect added pin to a matching pin on mated device or connector, if appropriate.
		ConnectionHelper chelper = new ConnectionHelper();
		if (!chelper.examine(newpin, diagram)) {
			// gdh 11/19/03 5834 add single pin, check for connectivity
			GfxView gview = (GfxView) CAFUtils.getInstance().getActiveCapletView();
			IGfxContext context = gview.getGfxContext();
			ISheet sheet = gview.getSheet();
			chelper.examine(context, sheet, newpin);
		}
		chelper.connectPin(newpin, grid, true, new LibraryObjectInfoCache());
		if (newpin.getConnectivity().getOwner() instanceof IGenericInlineConnector) {
			setReferenceState(m_isReference, chelper.getConnectedPin(newpin));
		}
		//
		// We are adding a new pin, if ther eis sone shared-ness, tie up the device connectors.
		//
		if (plc instanceof IDevice && plc.getSharedPinList() != null && ((IDevice) plc).getNumDeviceConnectors() > 0) {
			syncDeviceConnectorMatingFromShared((IDevice) plc, (IDevicePin) newpin.getConnectivity());
		}

		markDiagramForGHCGeneration(diagram, device);
	}

	private void generatePinOnly(ISchemDiagram diagram, IPinList paramObj, chs.cof.logical.cable.IPinList device,
			IAbstractPin existingConnectivity, chs.cof.logical.cable.IPinList plc, IPin newpin)
	{
		// if the connectivity exists, assign the libray info
		if (existingConnectivity == null) {
			assignLibraryCavity(newpin);
		}
		IGrid grid = diagram.getGrid();
		IPreferenceSet styleSet = PreferenceSetHelper.getStyleSet(diagram);
		Collection<IPinList> oldePinLists = paramObj.getAttachedPinListObjects();
		// Not sure why styleset is required to construct parameters. Generator seems to be ineterested only in grid spacing and width
		// Check and remove if not required
		GeneratorParameters gp = new GeneratorParameters(grid, styleSet);
		Generator generator = Generator.getGenerator();
		AddPinHelper.regeneratePin(oldePinLists, gp, generator, paramObj, styleSet, newpin, false);

		setReferenceState(m_isReference, newpin);

		//
		// We are adding a new pin, if ther eis sone shared-ness, tie up the device connectors.
		//
		if (plc instanceof IDevice && plc.getSharedPinList() != null && ((IDevice) plc).getNumDeviceConnectors() > 0) {
			syncDeviceConnectorMatingFromShared((IDevice) plc, (IDevicePin) newpin.getConnectivity());
		}

		markDiagramForGHCGeneration(diagram, device);
	}

	protected static void syncDeviceConnectorMatingFromShared(IDevice device, IDevicePin devicePin)
	{
		if (devicePin.getDeviceConnectorPin() == null) {
			ISharedPin spin = devicePin.getSharedPin();
			if (spin != null) {
				ISharedPin mate = spin.getMatePin();
				for (IDeviceConnector dc : device.getDeviceConnectors()) {
					for (IAbstractPin ap : dc.getPins()) {
						if (ap.getSharedPin() == mate && ap instanceof IDeviceConnPin) {
							devicePin.setDeviceConnectorPin((IDeviceConnPin) ap);
							((IDeviceConnPin) ap).setDevicePin(devicePin);
							return;
						}
					}
				}
			}
		}
	}

	public ISchemStackPin addStackPin(ISchemDiagram diagram, Point2D p2, IPinList paramObj,
			chs.cof.logical.cable.IPinList device, List<AbstractPinActionHelper.AddPinArgs> pinArgsSet)
	{
		return addStackPin(diagram, p2, paramObj, device, null, pinArgsSet);
	}

	private ISchemStackPin addStackPin(ISchemDiagram diagram, Point2D p2, IPinList paramObj,
			chs.cof.logical.cable.IPinList device, @Nullable ISchemStackPin stack,
			List<AbstractPinActionHelper.AddPinArgs> pinArgsSet)
	{
		chs.cof.logical.cable.IPinList plc = m_pinList.getConnectivity();
		// Add the pin (it Generates the gfx for us)
		//noinspection NumericCastThatLosesPrecision
		List<IAbstractPin> pinSet = new ArrayList<IAbstractPin>();
		for (AbstractPinActionHelper.AddPinArgs pinArgs : pinArgsSet) {
			AddPinHelper.CablePinGenerator cablePinGenerator = new AddPinHelper.CablePinGenerator(plc,
					pinArgs.getPin(), null, pinArgs.getAssociatedObjectType(),
					pinArgs.getAssociatedObject(), pinArgs.getAssociatedPinName(), null);
			cablePinGenerator.generate();
			IAbstractPin cpin = cablePinGenerator.getCpin();
			pinSet.add(cpin);
			String pinName = pinArgs.getName();
			setPinName(cpin, pinName);
			LogicUtils.setMatchingShortDescriptionFromOTI(cpin, cpin.getProject());
			if (pinArgs.getPin() != cpin && plc.getSharedPinList() == null) {
				AddPinHelper.assignLibraryCavity(plc, cpin);
			}
		}

		return addStackPin(diagram, p2, paramObj, stack, device, plc, pinSet);
	}

	protected void setPinName(IAbstractPin pin, @Nullable String pinName)
	{
		if (!StringUtils.isBlank(pinName)) {
			pin.setName(pinName);
		}
	}

	public ISchemStackPin addStackPin(ISchemDiagram diagram, Point2D p2, IPinList paramObj,
			@Nullable ISchemStackPin stack, chs.cof.logical.cable.IPinList pinlist, chs.cof.logical.cable.IPinList plc,
			List<IAbstractPin> pinSet)
	{
		IGrid grid = diagram.getGrid();

		ISchemStackPin pinStack;
		if (stack != null) {
			pinStack = stack;
			for (IAbstractPin cablePin : pinSet) {
				pinStack.addPinToStack(cablePin);
			}
		}
		else {
			pinStack = AddPinHelper.generateStackPin(paramObj, (int) p2.getX(), (int) p2.getY(), diagram, pinSet);
			// Connect added pin to a matching pin on mated device or connector, if appropriate.
			ConnectionHelper chelper = new ConnectionHelper();
			chelper.examine(pinStack, diagram);
			chelper.connectPin(pinStack, grid, true, false, new LibraryObjectInfoCache());
		}

		// If there is any shared-ness, tie up the device connectors.
		if (plc instanceof IDevice && plc.getSharedPinList() != null && ((IDevice) plc).getNumDeviceConnectors() > 0) {
			for (IAbstractPin newpin : pinSet) {
				syncDeviceConnectorMatingFromShared((IDevice) plc, (IDevicePin) newpin);
			}
		}
		markDiagramForGHCGeneration(diagram, pinlist);
		return pinStack;
	}

	protected static void markDiagramForGHCGeneration(ISchemDiagram diagram, chs.cof.logical.cable.IPinList device)
	{
		if (device != null && device instanceof IDevice && PinListHelper.isHarnessFootprinted(device)) {
			if (device.getSharedObject() != null) {
				// dts0100600448: We might have to generate shared connectors
				// So call for generation on shared device
				ISharedObject sharedObject = device.getSharedObject();
				if (sharedObject instanceof ISharedDevice) {
					((ISharedDevice) sharedObject).markDiagramForHCGeneration(diagram.getUID(), true);
				}
			}
		}
	}

	protected static void setReferenceState(boolean isReference, @Nullable IPin pin)
	{
		if (isReference && pin != null) {
			chs.cof.logical.cable.IPinList plc = pin.getConnectivity().getOwner();
			if (plc != null && plc.canHaveReferencePin()) {
				pin.setReference(true);
			}
		}
	}

	public void assignLibraryCavity(IPin newpin)
	{
		chs.cof.logical.cable.IPinList plc = m_pinList.getConnectivity();
		// add library info if there is a library assingment on the pinlist
		IAbstractPin cablePin = newpin.getConnectivity();
		AddPinHelper.assignLibraryCavity(plc, cablePin);
	}

	public static void assignLibraryCavity(IPin newpin, Collection<ILibraryCavity> libraryCavities)
	{
		// add library info if there is a library assingment on the pinlist
		IAbstractPin cablePin = newpin.getConnectivity();
		AddPinHelper.assignLibraryCavity(cablePin, libraryCavities);
	}

	/**
	 * Autogenerate pins of the specified names, evenly spaced on the pinlist.
	 * <p>
	 * A new connectivity & schematic pin is created for each name.
	 * <p>
	 * The pinlist may be resized to fit the specified number of pins if required.
	 * <p>
	 * Currently assumes that pin generation is on the left/right of the pinlist
	 *
	 * @param diagram Schematic diagram on which the pins will be created.  The pinlist may not yet be on a diagram.
	 * @param pinlist Schematic pinlist on which to create pins.  Connectivity pins are also created.
	 * @param pinNames The names of the pins to create
	 * @param reference Add pins as reference
	 * @param connectivityFinder connectivity finders for lock and edit
	 */
	public static void autogeneratePins(ISchemDiagram diagram, IPinList pinlist, List<String> pinNames,
			boolean reference, CompositePinConnectivityFinder connectivityFinder)
	{
		// create the connectivity pins
		List<IAbstractPin> pins = createConnectivityPins(pinNames, pinlist.getConnectivity());
		assert !(pinlist.getConnectivity() instanceof IGenericInlineConnector); // not yet implemented for inlines
		autogenerateSchematicPins(diagram, pinlist, null, pins, reference, connectivityFinder);
	}

	/**
	 * Create connectivity pins of the specified names and add them to a connectivity pinlist.
	 *
	 * @param pinNames The names of all pins that must be added
	 * @param pinlist The connectivity pinlist to which new pins are added
	 *
	 * @return The list of pins that were added
	 */
	public static List<IAbstractPin> createConnectivityPins(List<String> pinNames,
			chs.cof.logical.cable.IPinList pinlist)
	{
		List<IAbstractPin> pins = new ArrayList<IAbstractPin>();
		for (String pinName : pinNames) {
			IAbstractPin pin = CreationUtils.createPin(pinlist);
			pinlist.addPin(pin);
			pin.setName(pinName);
			LogicUtils.setMatchingShortDescriptionFromOTI(pin, pin.getProject());
			pins.add(pin);
		}
		return pins;
	}

	public static void createSchematicPins(@NotNull ISchemDiagram diagram, @NotNull IPinList pinlist,
			@NotNull List<IAbstractPin> pins)
	{
		PinListAddPinHelper addPinHelper = new PinListAddPinHelper(pinlist, false);
		List<Pair<IAbstractPin, Boolean>> newPinMap = buildNewPinStatus(pins);
		addPinHelper.createSchematicPins(diagram, newPinMap);
	}

	/**
	 * Autogenerate schematic pins for the specified connectivity pins, evenly spaced on the pinlist.
	 * <p>
	 * The pinlist(s) may be resized to fit the specified number of pins if required.
	 *
	 * @param diagram Schematic diagram on which the pins will be created.  The pinlist may not yet be on a diagram.
	 * @param pinlist Schematic pinlist on which to create pins based on the connectivity pins.
	 * @param pinlistMate Schematic pinlist on which to create pins mate pins based on the connectivity pins.  Only used
	 * for inlines.
	 * @param pins The connectivity pins for the pins to create
	 * @param referencePins generate the pins as reference pins.
	 */
	public static void autogenerateSchematicPins(ISchemDiagram diagram, IPinList pinlist,
			@Nullable IPinList pinlistMate, List<IAbstractPin> pins, boolean referencePins)
	{
		PinListAddPinHelper addPinHelper = new PinListAddPinHelper(pinlist, referencePins);
		List<Pair<IAbstractPin, Boolean>> newPinMap = buildNewPinStatus(pins);
		addPinHelper.autogenerateSchematicPins(diagram, pinlistMate, newPinMap);
	}

	@NotNull private static List<Pair<IAbstractPin, Boolean>> buildNewPinStatus(@NotNull List<IAbstractPin> pins)
	{
		List<Pair<IAbstractPin, Boolean>> newPinMap = new ArrayList<>(pins.size());
		for (IAbstractPin pin : pins) {
			ILogicDesign logicDesign = pin.getLogicDesign();
			Pair<IAbstractPin, Boolean> pinPair;
			if (logicDesign != null) {
				pinPair = new Pair<IAbstractPin, Boolean>(pin, logicDesign.getDesignWideUsageMgr().hasUsage(pin, true));
			}
			else {
				pinPair = new Pair<IAbstractPin, Boolean>(pin, Boolean.FALSE);
			}
			newPinMap.add(pinPair);
		}
		return newPinMap;
	}

	/**
	 * Autogenerate schematic pins for the specified connectivity pins, evenly spaced on the pinlist.
	 * <p>
	 * The pinlist(s) may be resized to fit the specified number of pins if required.
	 *
	 * @param diagram Schematic diagram on which the pins will be created.  The pinlist may not yet be on a diagram.
	 * @param pinlist Schematic pinlist on which to create pins based on the connectivity pins.
	 * @param pinlistMate Schematic pinlist on which to create pins mate pins based on the connectivity pins.  Only used
	 * for inlines.
	 * @param pins The connectivity pins for the pins to create
	 * @param referencePins generate the pins as reference pins.
	 */
	public static void autogenerateSchematicPins(ISchemDiagram diagram, IPinList pinlist,
			@Nullable IPinList pinlistMate, List<IAbstractPin> pins, boolean referencePins,
			CompositePinConnectivityFinder connectivityFinder)
	{
		PinListAddPinHelper addPinHelper = new PinListAddPinHelper(pinlist, referencePins);
		List<Pair<IAbstractPin, Boolean>> newPinMap = buildNewPinStatus(pins);
		addPinHelper.autogenerateSchematicPins(diagram, pinlistMate, newPinMap, connectivityFinder);
	}

	/**
	 * This will only ensure schematic pins. Will not do device-conn/harness-conn generation etc.
	 *
	 * @param diagram -
	 * @param newPinMap -
	 */
	public void createSchematicPins(@NotNull ISchemDiagram diagram,
			@NotNull List<Pair<IAbstractPin, Boolean>> newPinMap)
	{
		doCreateSchematicPins(diagram, null, newPinMap);
	}

	/**
	 * Autogenerate schematic pins for the specified connectivity pins, evenly spaced on the pinlist.
	 * <p>
	 * The pinlist(s) may be resized to fit the specified number of pins if required.
	 *
	 * @param diagram Schematic diagram on which the pins will be created.  The pinlist may not yet be on a diagram.
	 * @param pinlistMate Schematic pinlist on which to create pins mate pins based on the connectivity pins.  Only used
	 * for inlines.
	 * @param newPinMap new pins and if usage already present.
	 */
	public void autogenerateSchematicPins(ISchemDiagram diagram, @Nullable IPinList pinlistMate,
			List<Pair<IAbstractPin, Boolean>> newPinMap)
	{
		// nothing to do if no pins
		ListMap<IPinList, IPin> newSchemPins = doCreateSchematicPins(diagram, pinlistMate, newPinMap);
		if (newSchemPins == null) {
			return;
		}
		for (Map.Entry<IPinList, List<IPin>> entry : newSchemPins.entrySet()) {
			regeneratePins(entry.getKey(), diagram, m_isReference, entry.getValue());
			regenerateGraphics(entry.getKey());
		}
		if (pinlistMate != null) {
			regenerateGraphics(pinlistMate);
		}
	}

	/**
	 * Autogenerate schematic pins for the specified connectivity pins, evenly spaced on the pinlist.
	 * <p>
	 * The pinlist(s) may be resized to fit the specified number of pins if required.
	 *
	 * @param diagram Schematic diagram on which the pins will be created.  The pinlist may not yet be on a diagram.
	 * @param pinlistMate Schematic pinlist on which to create pins mate pins based on the connectivity pins.  Only used
	 * for inlines.
	 * @param newPinMap new pins and if usage already present.
	 */
	public void autogenerateSchematicPins(ISchemDiagram diagram, @Nullable IPinList pinlistMate,
			List<Pair<IAbstractPin, Boolean>> newPinMap, CompositePinConnectivityFinder connectivityFinder)
	{
		ListMap<IPinList, IPin> newSchemPins = doCreateSchematicPins(diagram, pinlistMate, newPinMap);
		if (newSchemPins == null) {
			return;
		}
		for (Map.Entry<IPinList, List<IPin>> entry : newSchemPins.entrySet()) {
			regeneratePinsOnly(entry.getKey(), diagram, m_isReference, entry.getValue());
			collectConnectionMakers(entry.getValue(), connectivityFinder);
			regenerateGraphics(entry.getKey());
		}
		if (pinlistMate != null) {
			regenerateGraphics(pinlistMate);
		}
	}

	@Nullable private ListMap<IPinList, IPin> doCreateSchematicPins(ISchemDiagram diagram,
			@Nullable IPinList pinlistMate, List<Pair<IAbstractPin, Boolean>> newPinMap)
	{
		// nothing to do if no pins
		int numPins = newPinMap.size();
		if (numPins == 0) {
			return null;
		}

		// clients must pass schem mates for inlines
		if (m_pinList.getConnectivity() instanceof IGenericInlineConnector) {
			assert pinlistMate != null && pinlistMate.getConnectivity() instanceof IGenericInlineConnector;
		}

		// don't do anything if the schem already has some pins - clients should make sure this doesn't happen
		if (!m_pinList.getAllPins().isEmpty()) {
			assert false : "pinlist should not have pins before autogenerate";
			return null;
		}

		// make sure that the pinlist (and it's mate) is big enough
		int gridSpacing = diagram.getGrid().getGridSpacing();
		boolean singleSide = m_pinList.getConnectivity() instanceof IConnector;
		int calcHeight = adjustHeight(numPins, gridSpacing, singleSide, m_pinList);
		if (pinlistMate != null) {
			int mateHeight = adjustHeight(numPins, gridSpacing, singleSide, pinlistMate);
			assert mateHeight == calcHeight;
		}

		// add the pins
		IParameterized params = m_pinList.getParameterized();
		if (params != null) {
			int side = m_pinList.getConnectivity() instanceof IPlugConnector ? 1 : 0;
			int width = params.getExtent().getWidth();
			width = diagram.getGrid().snap(width);
			params.getExtent().setWidth(width);

			int pinIndex = 1;
			List<AutoGenPinArgs> newPinArgs = new ArrayList<>(newPinMap.size());
			int y = calcHeight;
			for (Pair<IAbstractPin, Boolean> pinPair : newPinMap) {
				newPinArgs.add(new AutoGenPinArgs(pinPair, new Point(side * width, y)));
				if ((side == 0) && (pinIndex >= numPins / 2) && !singleSide) {
					// second half of the pins on the opposite side
					side = 1;
					y = calcHeight;
				}
				else {
					y -= gridSpacing;
				}
				pinIndex++;
			}

			ListMap<IPinList, AutoGenPinArgs> distributedPinArgs = new ListMap<>(newPinMap.size());
			if (m_pinList.getConnectivity() instanceof IConnector) {
				ConnectorHelper.distributeAddPinArgsToPinLists(m_pinList, diagram,
						newPinArgs, (pl, a) -> distributedPinArgs.add(pl, (AutoGenPinArgs) a));
			}
			else {
				distributedPinArgs.addAll(m_pinList, newPinArgs);
			}

			ListMap<IPinList, IPin> newSchemPins = new ListMap<>(newPinMap.size());
			for (Map.Entry<IPinList, List<AutoGenPinArgs>> entry : distributedPinArgs.entrySet()) {
				IPinList pinList = entry.getKey();
				try(DeferPinListRegenerationAutoClosable ignored = new DeferPinListRegenerationAutoClosable(pinList)){
					for (AutoGenPinArgs pinArgs : entry.getValue()) {
						Point2D loc = pinArgs.getPoint();
						int loc_x = CommonUtils.toInteger(loc.getX());
						int loc_y = CommonUtils.toInteger(loc.getY());
						IPin newpin = AddPinHelper.createPin(pinList, pinList.getConnectivity(), loc_x, loc_y,
								pinArgs.getPinPair());
						newSchemPins.add(pinList, newpin);
					}
				}
			}
			return newSchemPins;
		}
		return null;
	}

	private static class AutoGenPinArgs extends AbstractAddPinArgs
	{

		@NotNull private Pair<IAbstractPin, Boolean> m_pin;

		private AutoGenPinArgs(@NotNull Pair<IAbstractPin, Boolean> pin, @NotNull Point loc)
		{
			super(loc);
			m_pin = pin;
		}

		@Nullable @Override public chs.cof.logical.cable.IPinList getCablePinlist()
		{
			return m_pin.getFirst().getOwner();
		}

		@NotNull Pair<IAbstractPin, Boolean> getPinPair()
		{
			return m_pin;
		}
	}

	protected static void regeneratePinsOnly(IPinList pinlist, ISchemDiagram diagram,
			boolean isReference, Collection<IPin> newSchemPins)
	{
		IPreferenceSet styleSet = PreferenceSetHelper.getStyleSet(diagram);
		IGrid grid = diagram.getGrid();
		GeneratorParameters gp = new GeneratorParameters(grid, styleSet);
		Generator generator = Generator.getGenerator();
		generator.generate(pinlist, gp, Generator.NOREGENERATE_PROPERTIES, false);

		chs.cof.logical.cable.IPinList plc = pinlist.getConnectivity();

		ILibraryObject libObj = (ILibraryObject) plc.getLibraryObject();
		Collection<ILibraryCavity> libraryCavities = Collections.emptySet();
		if (libObj != null) {
			libraryCavities = LibraryHelper.getCavities(libObj);
			loadLibraryDataWithChildInfo(libObj);
		}

		for (IPin newSchemPin : newSchemPins) {
			// if the connectivity exists, assign the libray info
			IAbstractPin abstractPin = newSchemPin.getConnectivity();
			if (abstractPin != null) {
				assignLibraryCavity(newSchemPin, libraryCavities);

				// Set up the generator & regenerate the graphics
				//
				Collection<IPinList> oldAttachedPinLists = pinlist.getAttachedPinListObjects();

				AddPinHelper.regeneratePin(oldAttachedPinLists, gp, generator, pinlist, null, newSchemPin);

				setReferenceState(isReference, newSchemPin);

				//
				// We are adding a new pin, if ther eis sone shared-ness, tie up the device connectors.
				//
				if (plc instanceof IDevice && plc.getSharedPinList() != null &&
						((IDevice) plc).getNumDeviceConnectors() > 0) {
					syncDeviceConnectorMatingFromShared((IDevice) plc, (IDevicePin) abstractPin);
				}
			}
		}
		markDiagramForGHCGeneration(diagram, plc);
	}

	private static void loadLibraryDataWithChildInfo(@NotNull ILibraryObject libObj)
	{
		ILibraryBatchLoader libraryBatchLoader = LibraryBatchLoader.createInstance();
		ILibraryLazyLoadRequestGenerator reqGenerator = LibraryLazyLoadRequestGenerator.getInstance();
		boolean batchMode = reqGenerator.isBatchMode();
		try {
			reqGenerator.setBatchMode(false);
			List<Class<? extends ILibraryBaseObject>> childrenToBeLoaded =
					Arrays.asList(ILibraryCavity.class,
							ILibraryDeviceFootprint.class,
							ILibraryHousingDefinition.class);
			doLoadOfLibraryObjectChildren(libraryBatchLoader, libObj, childrenToBeLoaded);
		}
		finally {
			reqGenerator.setBatchMode(batchMode);
		}
	}

	private static <T extends ILibraryBaseObject> void doLoadOfLibraryObjectChildren(
			@NotNull ILibraryBatchLoader libraryBatchLoader,
			@NotNull ILibraryObject libraryObject,
			@NotNull List<Class<? extends ILibraryBaseObject>> childrenToBeLoaded)
	{
		libraryBatchLoader.
				loadLibrarySelectiveChildren(Collections.singletonList(libraryObject.getUID()), IAttributeTypes.OWNER, childrenToBeLoaded);
	}

	protected static void collectConnectionMakers(Collection<IPin> newSchemPins,
			CompositePinConnectivityFinder connectivityFinder)
	{
		GfxView gview = (GfxView) CAFUtils.getInstance().getActiveCapletView();
		IGfxContext context = gview.getGfxContext();
		connectivityFinder.collectConnectionMakers(context, newSchemPins);
	}

	protected static void regeneratePins(IPinList pinList, ISchemDiagram diagram,
			boolean isReference, Collection<IPin> newSchemPins)
	{
		IPreferenceSet styleSet = PreferenceSetHelper.getStyleSet(diagram);
		IGrid grid = diagram.getGrid();
		GeneratorParameters gp = new GeneratorParameters(grid, styleSet);
		Generator generator = Generator.getGenerator();
		generator.generate(pinList, gp, Generator.NOREGENERATE_PROPERTIES, false);

		chs.cof.logical.cable.IPinList plc = pinList.getConnectivity();

		GfxView gview = (GfxView) CAFUtils.getInstance().getActiveCapletView();
		IGfxContext context = gview.getGfxContext();
		ISheet sheet = gview.getSheet();

		ILibraryObject libObj = (ILibraryObject) plc.getLibraryObject();
		Collection<ILibraryCavity> libraryCavities = Collections.emptySet();
		if (libObj != null) {
			libraryCavities = LibraryHelper.getCavities(libObj);
			ILibraryBatchLoader libraryBatchLoader = LibraryBatchLoader.createInstance();
			libraryBatchLoader.loadFully(Collections.singletonList(libObj), true);
		}

		ConnectionHelperCreationParams creationParams = new ConnectionHelperCreationParams();
		ILogicDesign logicDesign = pinList.getConnectivity().getLogicDesign();
		assert logicDesign != null : "Null design!!!";
		creationParams.setIsHomeProvider(PinHomeConditionControl.getDefaultHomePredicate(logicDesign));
		creationParams.deferRefreshRepresentations();
		creationParams.deferApplyStyle();
		Collection<IAbstractSchemPin> newlyCreatedPins = new LinkedHashSet<>();
		Collection<IPinList> newlyCreatedPinOwners = new LinkedHashSet<>();
		ILibraryObjectInfoCache libraryObjectInfoCache = new LibraryObjectInfoCache();
		for (IPin newSchemPin : newSchemPins) {
			// if the connectivity exists, assign the libray info
			IAbstractPin abstractPin = newSchemPin.getConnectivity();
			if (abstractPin != null) {
				assignLibraryCavity(newSchemPin, libraryCavities);

				// Set up the generator & regenerate the graphics
				//
				Collection<IPinList> oldAttachedPinLists = pinList.getAttachedPinListObjects();

				AddPinHelper.regeneratePin(oldAttachedPinLists, gp, generator, pinList, null, newSchemPin);

				setReferenceState(isReference, newSchemPin);

				// Connect added pin to a matching pin on mated device or connector, if appropriate.
				ConnectionHelper chelper = new ConnectionHelper(creationParams);
				if (!chelper.examine(newSchemPin, diagram)) {
					// gdh 11/19/03 5834 add single pin, check for connectivity
					chelper.examine(context, sheet, newSchemPin);
				}
				ConnectionHelper.ConnectPinResult connectPinResult = chelper.connectPin(newSchemPin, grid, true,
						libraryObjectInfoCache);
				if (connectPinResult != null && connectPinResult.isNewlyCreatedPin()) {
					newlyCreatedPins.add(connectPinResult.getMatchingPin());
					newlyCreatedPinOwners.add((IPinList) connectPinResult.getMatchingPin().getParent());
				}
				if (abstractPin.getOwner() instanceof IGenericInlineConnector) {
					setReferenceState(isReference, chelper.getConnectedPin(newSchemPin));
				}
				//
				// We are adding a new pin, if ther eis sone shared-ness, tie up the device connectors.
				//
				if (plc instanceof IDevice && plc.getSharedPinList() != null &&
						((IDevice) plc).getNumDeviceConnectors() > 0) {
					syncDeviceConnectorMatingFromShared((IDevice) plc, (IDevicePin) abstractPin);
				}
			}
		}
		pinList.getDiagram().refreshRepresentations();

		newlyCreatedPinOwners.forEach(aPinlist -> generator.generate(aPinlist, gp,
				Generator.NOREGENERATE_PROPERTIES, false));
		Collection<IPin> newlyCreatedSchemPins = CollectionUtils.filterByClass(newlyCreatedPins, IPin.class);
		newlyCreatedSchemPins.forEach(aPin -> {
			AddPinHelper.regeneratePin(gp, generator, (IPinList) aPin.getParent(), styleSet, aPin);
		});

		markDiagramForGHCGeneration(diagram, plc);
		ISharedPinList sharedPinList = plc.getSharedPinList();
		if (sharedPinList != null)
		{
			sharedPinList.flush();
		}
	}

	/**
	 * Adjust the height of the pinlist before adding pins (if required)
	 *
	 * @param numPins The number of pins to be added
	 * @param gridSpacing The diagram grid spacing (1 pin per grid)
	 * @param singleSide Should the pins be added to only one side?
	 * @param pinlist The pinlist
	 *
	 * @return The new height of the pinlist, in diagram coords, minus the extra grids above/below the pins that
	 * something else somehow puts there!
	 */
	public static int adjustHeight(int numPins, int gridSpacing, boolean singleSide, IPinList pinlist)
	{
		// ASSUME : current assumption is that autogen of pins only happens on the left/right of a pinlist
		int pinsPerSide = numPins;
		if (!singleSide) {
			// this pinlist will have pins on opposite sides
			// NOTE : deliberate integer truncation here we want the same result for 0/1 pins on a device
			pinsPerSide /= 2;
		}
		if (pinsPerSide > 0) {
			// we want the height to omit the top/bottom grid space (so this is 0 for 1 pin)
			--pinsPerSide;
		}

		int calcHeight = pinsPerSide * gridSpacing;
		IExtent pExt = pinlist.getParameterized().getExtent();
		if (pExt.getHeight() < calcHeight) {
			pExt.setBounds(0, 0, pExt.getWidth(), calcHeight);
			pinlist.setReferenceHeight(calcHeight); // D'OH!
		}
		return calcHeight;
	}

	public static void regenerateGraphics(@Nullable IDevice device)
	{
		if (device == null) {
			return;
		}
		ILogicDesign logicDesign = device.getLogicDesign();
		if (logicDesign == null) {
			return;
		}
		Collection<IDiagramObject> representations = logicDesign.getDesignWideUsageMgr().getRepresentations(device);
		Collection<IPinList> schemDevices = CollectionUtils.filterByClass(representations, IPinList.class);
		schemDevices.forEach(PinListAddPinHelper::regenerateGraphics);
	}

	public static void regenerateGraphics(IPinList pl)
	{
		ISchemDiagram diagram = DiagramHelper.getDiagram(pl);
		Generator generator = Generator.getGenerator();
		regeneratePinListGraphics(pl, diagram, generator, false);
	}

	public static void regeneratePinListGraphics(IPinList pl, ISchemDiagram diagram, Generator generator,
			boolean isGenerationForMovePin)
	{
		chs.cof.logical.cable.IPinList plc = pl.getConnectivity();
		//
		// Only do the regeneration here if it has been placed (has a parent).
		//
		if (pl.getParent() != null) {
			// dts0100497641 - The DeviceConnector text appeared when you constructed a device from a library part as that was
			// the behaviour specified in the default style. However, as this method was not using the default style, the
			// recreation of the device after adding a pin resulted in the DeviceConnector text disappearing. So to fix this
			// defect, we now ensure that we are using the default style by constructing 'GeneratorParameters' from the factory.
			GeneratorParameters gp = DiagramHelper.createGeneratorParameters(diagram);
			if (plc instanceof IDevice) {
				IDevice pinlistConnectivity = (IDevice) plc;
				if (pinlistConnectivity.getNumDeviceConnectors() > 0 ||
						PinListHelper.isDeviceConnectorFootprinted(pinlistConnectivity)) {
					// draw device connectors
					// todo - this is in lieu of the generateDevice call below - which we may want to do in any case

					// if ther is a library part association call the utility
					// that rebuilds the graphic completely (except for shared pins - because they do this
					// elsewhere)
					IDevice dev = (IDevice) plc;
					//check shared state first to make it faster condition check.
					if (ConnectionHelper.isConnectivityEditable() && (dev.getSharedPinList() == null) &&
							FootprintUtils.hasFootprintContext(pinlistConnectivity)) {
						generator.rebuildDeviceConnectors(pl, gp, null, true);
					}
					else {
						generator.regenerateSchemDeviceConnectors(pl, gp);
					}
				}
				//Process GHS for device
				ConnectivityGHCHelper.generateHarnessConnectors(diagram, true, isGenerationForMovePin, pl);
				// regenerate device to pick up pin graphics
				generator.generateDevice(pl, gp);
			}
			else if (plc instanceof IConnector) {
				// seems odd to have an instanceof for each pinlist type
				// but we needed this one here to fix 457487
				generator.generateConnector(pl, gp);
			}
			else if (plc instanceof IBlockDevice) {
				// for block device
				generator.generateDevice(pl, gp);
			}
			else if (plc instanceof IFunction) {
				generator.generate(pl, gp, Generator.NOREGENERATE_PROPERTIES, false);
			}
		}
	}

	public static void removeTransiantLibraryPart(@Nullable ILibraryDevice transientLibraryDevice, @Nullable
			chs.cof.logical.cable.IPinList pinList)
	{
		if (transientLibraryDevice != null && pinList != null) {
			ILibraryPartMgr partMgr = FactoryMgr.getSystemFactory().getCHSSystem().getPartsLibrary().getPartMgr();
			partMgr.remove(transientLibraryDevice);
			pinList.assignLibraryPart(null);
		}
	}

	@Nullable public static ILibraryDevice assignTransiantLibraryPart(@Nullable IDeviceICD selectICD, @Nullable
			chs.cof.logical.cable.IPinList pinList)
	{
		//if the device is already having a part. we can't assign an transient library
		//part. this would cause removal of part from the device at the end of action.
		//and also there are NPE etc problems due to this assignment.
		Predicate<chs.cof.logical.cable.IPinList> existingLibRefCheck = (p) -> p.getLibraryRef() == null;
		return assignTransiantLibraryPart(selectICD, pinList, existingLibRefCheck);
	}

	@Nullable public static ILibraryDevice assignTransiantLibraryPart(@Nullable IDeviceICD selectICD,
			@Nullable chs.cof.logical.cable.IPinList pinList,
			@NotNull Predicate<chs.cof.logical.cable.IPinList> existingLibRefCheck)
	{
		ILibraryDevice transientLibraryDevice = null;
		if (selectICD != null && pinList != null && existingLibRefCheck.test(pinList) &&
				selectICD.getLibraryDevice() == null) {
			transientLibraryDevice = ICDUtils.createTransientLibraryDevice(selectICD);
			pinList.assignLibraryPart(transientLibraryDevice);
			ILibraryPartMgr partMgr = FactoryMgr.getSystemFactory().getCHSSystem().getPartsLibrary().getPartMgr();
			partMgr.add(transientLibraryDevice);
		}
		return transientLibraryDevice;
	}

	public static class ManageConnectorDnDValidateDnDBetweenCells
			extends AbstractDnDHelper.ValidateDnDBetweenCells<ManageConnectorConnectionsInfo>
	{

		@Nullable @Override protected String findIfCellIsAcceptable(
				@Nullable ColumnInformation<ManageConnectorConnectionsInfo> columnInformation,
				@Nullable ManageConnectorConnectionsInfo item)
		{
			String disableReason = null;
			if (item == null) {
				return ManageConnectorConnectionsInfo.cellOutOfRange();
			}
			if (ManageConnectorsDialog.WIRE_NAME.equals(columnInformation.getName())) {
				if (!item.isEditable()) {
					disableReason = item.getDisableConductorDnDReason();
				}
			}
			else if (ManageConnectorsDialog.PIN_NAME.equals(columnInformation.getName())) {
				if (!item.isUsableInCurrentDesign() && !item.isEditable()) {

					disableReason = item.getDisablePinDnDReason();
				}
			}
			return disableReason;
		}

		@Nullable @Override protected String areCompatibleForDragAndDrop(
				ManageConnectorConnectionsInfo givensourceCellItem,
				ManageConnectorConnectionsInfo givenhoverCellItem,
				CapitalTableCell<ManageConnectorConnectionsInfo> givenHoverCell)
		{

			if (givenhoverCellItem == null || givensourceCellItem == null) {
				return "Unknown cell type";
			}

			String cellHoverDisability = null;
			// allow if both are backshell terminations, or if neither are backshell terminations
			if (areBothBackshellTerminations(givensourceCellItem, givenhoverCellItem) ||
					!srcOrTargetHasBackshellTermination(givensourceCellItem, givenhoverCellItem)) {
				cellHoverDisability =
						findIfCellIsAcceptable(givenHoverCell.getColumn(), givenhoverCellItem);
			}
			if (cellHoverDisability == null) {

				if (givenHoverCell.getColumn() != null) {

					if (ManageConnectorsDialog.WIRE_NAME.equals(givenHoverCell.getColumn().getName())) {

						cellHoverDisability =
								givensourceCellItem.isSwapAcceptableWith(givenhoverCellItem);
					}
					else if (ManageConnectorsDialog.PIN_NAME.equals(givenHoverCell.getColumn().getName())) {
						cellHoverDisability =
								givensourceCellItem.isMoveAcceptableWith(givenhoverCellItem);
					}
					else {
						return "Use Pin column for move and wire columns for swap";
					}
				}
			}
			return cellHoverDisability;
		}

		private boolean areBothBackshellTerminations(ManageConnectorConnectionsInfo givenSourceCellItem,
				ManageConnectorConnectionsInfo givenHoverCellItem)
		{
			return givenSourceCellItem.isBackshellTermination() &&
					givenHoverCellItem.isBackshellTermination();
		}

		private boolean srcOrTargetHasBackshellTermination(ManageConnectorConnectionsInfo givenSourceCellItem,
				ManageConnectorConnectionsInfo givenHoverCellItem)
		{
			return givenSourceCellItem.isBackshellTermination() ||
					givenHoverCellItem.isBackshellTermination();
		}
	}

	public static class ManageConnectorDnDHelper extends AbstractDnDHelper<ManageConnectorConnectionsInfo>

	{

		private final Supplier<PinConductorConnectionSortHelper> sortHelperSupplier;

		@Override
		protected void handleItemsSelectedForDrag(Collection<ManageConnectorConnectionsInfo> itemsSelectedForDrag)
		{
			((ManageConnectorPopupHandler) popup).setDragConductors(itemsSelectedForDrag);
		}

		public ManageConnectorDnDHelper(Supplier<PinConductorConnectionSortHelper> sortHelperSupplier)
		{
			this.sortHelperSupplier = sortHelperSupplier;
		}

		public Consumer<IGenericTableCell<ManageConnectorConnectionsInfo>> addDragAndDropFunctionalityOnCell(
				Table<ManageConnectorConnectionsInfo> table, BiConsumer<Popup, Boolean> popupDisplay,
				ColumnInformation<ManageConnectorConnectionsInfo> columnInformation)
		{

			ManageConnectorDnDHelper dnDHelper = this;
			return new Consumer<IGenericTableCell<ManageConnectorConnectionsInfo>>()
			{

				@Override
				public void accept(IGenericTableCell<ManageConnectorConnectionsInfo> givenCell)
				{

					if (givenCell instanceof CapitalTableCell) {
						CapitalTableCell<ManageConnectorConnectionsInfo> cell =
								((CapitalTableCell<ManageConnectorConnectionsInfo>) givenCell);

						if (ManageConnectorsDialog.WIRE_NAME.equals(columnInformation.getName())) {

							cell.setOnDragDetected(event -> {
								dnDHelper.reset(cell, table, new ManageConnectorDnDValidateDnDBetweenCells(),
										new ManageConnectorBlockFinder(table),
										new ManageConnectorPopupHandler(popupDisplay));
								dnDHelper.setOnDragDetected(cell, event);
							});
						}
						cell.setOnDragOver(event -> {

							dnDHelper.setOnDragOver(cell, event);
						});

						// highlight the current cell
						cell.setOnDragEntered(event -> {
							dnDHelper.setOnDragEntered(cell, event);
						});
						cell.setOnDragDone(event -> {

							dnDHelper.setOnDragDone(cell, event);
						});

						//remove highlight
						cell.setOnDragExited(event -> {

							dnDHelper.setOnDragExited(cell, event);
						});
						cell.setOnDragDropped(event -> {
							dnDHelper.setOnDragDropped(cell, event);
						});
					}
				}
			};
		}

		@Override protected void doWhatEverIsRequired(
				CapitalTableCell<ManageConnectorConnectionsInfo> targetCell,
				int sourceRowIndex, TableColumn<?, ?> sourceTableColumn,
				Object source)
		{
			Map<ManageConnectorConnectionsInfo, Integer> swap =
					doWhatEverIsRequired(table, targetCell.getTableView(), sourceTableColumn,
							targetCell.getTableColumn(), sourceRowIndex, targetCell.getIndex(),
							source);
			if (swap != null) {
				Comparator<ManageConnectorConnectionsInfo> comparator =
						getComparatorToUseAfterSwap(swap);

				table.getData().sort(comparator);
			}
		}

		@Override protected boolean canExtendSelections()
		{
			return true;
		}

		protected Comparator<ManageConnectorConnectionsInfo> getComparatorToUseAfterSwap(
				Map<ManageConnectorConnectionsInfo, Integer> swapIndicies)
		{
			return
					new Comparator<ManageConnectorConnectionsInfo>()
					{

						@Override public int compare(ManageConnectorConnectionsInfo o1,
								ManageConnectorConnectionsInfo o2)
						{
							return swapIndicies.get(o1).compareTo(swapIndicies.get(o2));
						}
					};
		}

		@Nullable private Map<ManageConnectorConnectionsInfo, Integer> doWhatEverIsRequired(
				Table<ManageConnectorConnectionsInfo> table,
				TableView<ManageConnectorConnectionsInfo> tableView,
				TableColumn sourceTableColumn, TableColumn targetTableColumn, int draggedRowIndex,
				int droppedRowIndex,
				Object source)
		{
			ManageConnectorConnectionsInfo droppedRow = tableView.getItems().get(droppedRowIndex);

			ManageConnectorConnectionsInfo draggedRow = tableView.getItems().get(draggedRowIndex);
			if (!ManageConnectorsDialog.WIRE_NAME
					.equals(((ColumnInformation<?>) sourceTableColumn.getUserData()).getName())) {
				return null;
			}
			int droppedColumnIndex = tableView.getColumns().indexOf(targetTableColumn);

			if (ManageConnectorsDialog.PIN_NAME
					.equals(((ColumnInformation<?>) targetTableColumn.getUserData()).getName())) {
			/*
			DROPPED ON PIN COLUMN --> MOVE
			Proposal - 1
			Replace target row's wire & related data with source info..
			shift everything else down..
			update dropindex row
			Remove wire & related info from existing source row;

			Proposal-2
			Replace the source row's pin with target selected pin
			No additional rows added to the table
			Will behave exactly as changing value in column
			*/

				//table updates

				try (ManageConnectorReplaceAllCollector replaceAll = new ManageConnectorReplaceAllCollector()) {
					Comparable<?> droppedRowValue =
							sortHelperSupplier.get().createComparableForPin(droppedRow.getFirst());
					table.setValue(draggedRowIndex, droppedColumnIndex, droppedRowValue, true);
				}

				//selections
				tableView.getSelectionModel()
						.select(draggedRowIndex, tableView.getColumns().get(droppedColumnIndex));
			}
			if (ManageConnectorsDialog.WIRE_NAME
					.equals(((ColumnInformation<?>) targetTableColumn.getUserData()).getName())) {

				int pinNameColumnIndex = -1;
				int columnIteratorIndex = 0;
				for (TableColumn aCol : tableView.getColumns()) {
					if (ManageConnectorsDialog.PIN_NAME
							.equals(((ColumnInformation<?>) aCol.getUserData()).getName())) {
						pinNameColumnIndex = columnIteratorIndex;
						break;
					}
					columnIteratorIndex++;
				}
				if (pinNameColumnIndex == -1) {
					return null;
				}
				//DROPPED ON WIRE COLUMN --> SWAP"
				//table updates
//			table.setValue(dropIndex, 1, draggedRow.getSecond().getValueOfAttribute("NAME"));
//			table.setValue(draggedIndex, 1, targetValue.getValueOfAttribute("NAME"));
				Comparable<?> droppedPinName =
						sortHelperSupplier.get().createComparableForPin(droppedRow.getFirst());
				Comparable<?> draggedPinName =
						sortHelperSupplier.get().createComparableForPin(draggedRow.getFirst());

				Map<ManageConnectorConnectionsInfo, Integer> swap =
						createIndicesForUseinSort(tableView.getItems(), draggedRowIndex, droppedRowIndex);

				tableView.getSortOrder().clear();

				int droppedViewIndex = tableView.getItems().indexOf(droppedRow);
				try (ManageConnectorReplaceAllCollector temp = new ManageConnectorReplaceAllCollector()) {

					table.setValue(droppedViewIndex, pinNameColumnIndex, draggedPinName, true);
				}

				int draggedViewIndex = tableView.getItems().indexOf(draggedRow);
				try (ManageConnectorReplaceAllCollector temp = new ManageConnectorReplaceAllCollector()) {

					table.setValue(draggedViewIndex, pinNameColumnIndex, droppedPinName, true);
				}

				//selections
//			if (droppedRowIndex > draggedRowIndex) {
//				tableView.getSelectionModel()
//						.selectRange(draggedRowIndex, tableView.getColumns().get(draggedColumnIndex), droppedRowIndex,
//								tableView.getColumns().get(draggedColumnIndex));
//				for (int i = draggedRowIndex + 1; i < droppedRowIndex; i++) {
//					tableView.getSelectionModel().clearSelection(i, tableView.getColumns().get(draggedColumnIndex));
//				}
//			}
//			else {
//				tableView.getSelectionModel()
//						.selectRange(droppedRowIndex, tableView.getColumns().get(draggedColumnIndex), draggedRowIndex,
//								tableView.getColumns().get(1));
//				for (int i = droppedRowIndex + 1; i < draggedRowIndex; i++) {
//					tableView.getSelectionModel().clearSelection(i, tableView.getColumns().get(draggedColumnIndex));
//				}
//			}
				return swap;

//
			}
			return null;
		}

		private Map<ManageConnectorConnectionsInfo, Integer> createIndicesForUseinSort(
				ObservableList<ManageConnectorConnectionsInfo> dataItems, int draggedIndex, int droppedIndex)
		{
			Map<ManageConnectorConnectionsInfo, Integer> swap = new HashMap<>();

			String originalDroppedPinName = dataItems.get(droppedIndex).getOriginalValue();
			String originalDraggedPinName = dataItems.get(draggedIndex).getOriginalValue();
			int dropBlockStart = ((ManageConnectorBlockFinder) blockFinder)
					.getStartBlockIndex(dataItems, droppedIndex, originalDroppedPinName,
							DragBlockFinder.DragOrDrop.DROP);

			int dropBlockEnd = ((ManageConnectorBlockFinder) blockFinder)
					.getEndBlockIndex(dataItems, droppedIndex, originalDroppedPinName, DragBlockFinder.DragOrDrop.DROP);

			int dragBlockStart = ((ManageConnectorBlockFinder) blockFinder)
					.getStartBlockIndex(dataItems, draggedIndex, originalDraggedPinName,
							DragBlockFinder.DragOrDrop.DRAG);

			int dragBlockEnd = ((ManageConnectorBlockFinder) blockFinder)
					.getEndBlockIndex(dataItems, draggedIndex, originalDraggedPinName, DragBlockFinder.DragOrDrop.DRAG);

			int dropBlockSize = dropBlockEnd - dropBlockStart;
			int dragBlockSize = dragBlockEnd - dragBlockStart;

			int gap = Math.abs(dragBlockSize - dropBlockSize) + 1;

			int firstJumpIndex = dragBlockEnd < dropBlockEnd ? dragBlockEnd : dropBlockEnd;
			int secondJumpIndex = dragBlockEnd > dropBlockEnd ? dragBlockEnd : dropBlockEnd;

			int compareIndex = 0;
			int seqIndex = 0;
			for (ManageConnectorConnectionsInfo anItem : dataItems) {
				swap.put(anItem, compareIndex);

				if (seqIndex == firstJumpIndex || seqIndex == secondJumpIndex) {
					compareIndex += gap;
				}
				else {
					compareIndex++;
				}
				seqIndex++;
			}

			Integer dropCompareIndex = swap.get(dataItems.get(dragBlockStart));
			Integer dragCompareIndex = swap.get(dataItems.get(dropBlockStart));
			for (int dropModifyIndex = dropBlockStart; dropModifyIndex < dropBlockEnd + 1; dropModifyIndex++) {
				swap.put(dataItems.get(dropModifyIndex), dropCompareIndex);
				dropCompareIndex++;
			}

			for (int dragModifyIndex = dragBlockStart; dragModifyIndex < dragBlockEnd + 1; dragModifyIndex++) {
				swap.put(dataItems.get(dragModifyIndex), dragCompareIndex);
				dragCompareIndex++;
			}

			return swap;
		}
	}

	private static class ManageConnectorBlockFinder
			extends AbstractDnDHelper.DragBlockFinder<ManageConnectorConnectionsInfo>
	{

		private Table<ManageConnectorConnectionsInfo> table;

		ManageConnectorBlockFinder(Table<ManageConnectorConnectionsInfo> table)
		{
			this.table = table;
		}

		@Override protected int getEndBlockIndex(CapitalTableCell<ManageConnectorConnectionsInfo> cell, DragOrDrop val)
		{
			int blockEndIndex = cell.getIndex();
			ObservableList<ManageConnectorConnectionsInfo> dataItems = table.getData();
			String originalPinName = cell.getRowItem().getOriginalValue();
			return getEndBlockIndex(dataItems, blockEndIndex, originalPinName, val);
		}

		@Override
		protected int getStartBlockIndex(CapitalTableCell<ManageConnectorConnectionsInfo> cell, DragOrDrop val)
		{
			int blockStartIndex = cell.getIndex();
			ObservableList<ManageConnectorConnectionsInfo> dataItems = table.getData();
			String orginalPinName = cell.getRowItem().getOriginalValue();
			return getStartBlockIndex(dataItems, blockStartIndex, orginalPinName, val);
		}

		protected int getStartBlockIndex(ObservableList<ManageConnectorConnectionsInfo> dataItems, int blockStart,
				String orginalPinName, DragOrDrop val)
		{
			int blockStartIndex = blockStart;
			boolean isStartBackshell = dataItems.get(blockStart).isBackshellTermination();
			while (blockStartIndex > 0) {

				ManageConnectorConnectionsInfo currentRowItem = dataItems.get(blockStartIndex);
				ManageConnectorConnectionsInfo previousRowItem = dataItems.get(blockStartIndex - 1);
				IDesignDescriptor currentRowDesign = currentRowItem.getDesign();
				IDesignDescriptor previousRowDesign = previousRowItem.getDesign();
				if (currentRowDesign == null || !currentRowDesign.equals(previousRowDesign)) {
					break;
				}
				if (currentRowItem.isEditable() && previousRowItem.isEditable() &&
						previousRowItem.getOriginalValue().equals(orginalPinName)
						&& previousRowItem.isBackshellTermination() == isStartBackshell) {
					blockStartIndex--;
				}
				else {
					break;
				}
			}
			return blockStartIndex;
		}

		protected int getEndBlockIndex(ObservableList<ManageConnectorConnectionsInfo> dataItems, int blockEnd,
				String orginalPinName, DragOrDrop val)
		{
			int blockEndIndex = blockEnd;
			boolean isStartBackshell = dataItems.get(blockEnd).isBackshellTermination();
			while (blockEndIndex + 1 < dataItems.size()) {
				ManageConnectorConnectionsInfo currentrowItem = dataItems.get(blockEndIndex);

				ManageConnectorConnectionsInfo nextrowItem = dataItems.get(blockEndIndex + 1);
				IDesignDescriptor currentRowDesign = currentrowItem.getDesign();
				IDesignDescriptor nextRowDesign = nextrowItem.getDesign();
				if (currentRowDesign == null || !currentRowDesign.equals(nextRowDesign)) {
					break;
				}
				if (currentrowItem.isEditable() && nextrowItem.isEditable() &&
						nextrowItem.getOriginalValue().equals(orginalPinName)
						&& nextrowItem.isBackshellTermination() == isStartBackshell) {
					blockEndIndex++;
				}
				else {
					break;
				}
			}
			return blockEndIndex;
		}
	}

	private static class ManageConnectorPopupHandler extends AbstractDnDHelper.PopupHandler
	{

		private List<String> wires;

		ManageConnectorPopupHandler(BiConsumer<Popup, Boolean> popupDisplay)
		{
			super(popupDisplay);
		}

		private String getSource()
		{
			String source = wires.stream().limit(2).collect((Collectors.joining(",")));
			if (wires.size() > 2) {
				source += "...";
			}
			return source;
		}

		protected void showSwap(double x, double y, TableCell givenCurrentCell)
		{

			show(x, y,
					ResourceMgr.getString(PinListAddPinHelper.class, "PinListAddPinHelper.tooltip.SwapConductors"),
					(CapitalTableCell<?>) givenCurrentCell);
		}

		protected void showMove(double x, double y, TableCell givenCurrentCell)
		{
			String text =
					ResourceMgr
							.getString(PinListAddPinHelper.class, "PinListAddPinHelper.tooltip.Move", getSource());

			show(x, y, text, (CapitalTableCell<?>) givenCurrentCell);
		}

		void setDragConductors(Collection<ManageConnectorConnectionsInfo> itemsSelectedForDrag)
		{
			wires = itemsSelectedForDrag.stream().filter(anItem -> anItem.getSecond() != null)
					.map(anItem -> anItem.getSecond().getValueOfAttribute(IAttributeTypes.NAME))
					.collect(Collectors.toList());
		}

		void clear()
		{
			super.clear();
			if (wires != null) {
				wires.clear();
			}
		}
	}

	public static class DragboardWrapper
	{

		private Dragboard db;

		DragboardWrapper(Dragboard db)
		{
			this.db = db;
		}

		void setClipboardContent(Map<DataFormat, Object> content)
		{
			db.setContent(content);
		}

		boolean isValidForCell(CapitalTableCell<?> cell)
		{
			if (hasContent(AbstractDnDHelper.SERIALIZED_MIME_TYPE)) {
				if (cell.getIndex() != (Integer) db.getContent(AbstractDnDHelper.SERIALIZED_MIME_TYPE)) {
					return true;
				}
			}
			return false;
		}

		boolean hasContent(DataFormat format)
		{
			return db.hasContent(format);
		}

		Object getContent(DataFormat dataFormat)
		{
			return db.getContent(dataFormat);
		}

		void acceptTransferModes(DragEvent event)
		{
			event.acceptTransferModes(TransferMode.ANY);
		}
	}
}