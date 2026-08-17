/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.helpers.RegenerateGraphicsAction;
import chs.cof.draw.IGfxContext;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGfxObjectIterator;
import chs.cof.draw.IGrid;
import chs.cof.drawplus.IGfxView;
import chs.cof.drawplus.ISecondaryRepresentation;
import chs.cof.icd.IDeviceICD;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IPinPlaceholder;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedDevice;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.parts.ILibraryCavity;
import chs.cof.parts.ILibraryDevice;
import chs.cof.parts.ILibraryObject;
import chs.cofUtils.parameterized.AddPinHelper;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.common.ILocation;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.preferencesets.IPreferenceSet;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.ctf.caf.ui.PinPlaceOptionsParams;
import chs.ctf.caf.utils.IPinProxy;
import chs.services.gfx.GfxView;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import chs.utilities.Pair;
import chs.utilities.StringUtils;
import chs.utility.ConductorSplitter;
import chs.utility.DiagramHelper;
import chs.utility.ICDUtils;
import chs.utility.helpers.CompositePinConnectivityFinder;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.DSCWithBackshellPlaceholderHelper;
import chs.utility.helpers.ILibraryObjectInfoCache;
import chs.utility.helpers.LibraryHelper;
import chs.utility.helpers.LibraryObjectInfoCache;
import chs.utility.helpers.PinListHelper;
import chs.utility.helpers.SchemPinListHelper;
import chs.utility.logic.DeferPinListRegenerationAutoClosable;
import chs.utility.logic.DeferrablePinListRegenerator;
import chs.utility.logic.LogicUtils;
import chs.utility.logic.ModularConnectorHelper;
import chs.utility.preferences.PreferenceSetHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Dec 7, 2004 Time: 11:28:31 AM
 */
public class AddPinActionHelper extends AbstractPinActionHelper
{

	protected AddPinActionModel m_pinActionModel;
	private Collection<ILibraryCavity> m_libraryCavities;

	public AddPinActionHelper(ControllerActionRT action, boolean requirePlacement, boolean useBoundaryExtensions)
	{
		super(action, requirePlacement, useBoundaryExtensions);
		m_libraryCavities = Collections.emptySet();
	}

	public boolean setUp(IPinList pinList, @Nullable Collection<? extends IPinProxy> existingConnectivity)
	{
		return super.setUp(pinList, existingConnectivity);
	}

	public boolean setUp(IPinList pinList)
	{
		return setUp(pinList, null);
	}

	public boolean setUp(IPinList pinList, boolean bOverrideBoundaryExtensions)
	{
		return setUp(pinList, null, bOverrideBoundaryExtensions);
	}

	protected void addPinArgsDistributed()
	{
		//some new child connector schems could have been created.
		if (m_pinActionModel != null) {
			m_pinActionModel.reBuild();
		}
	}

	public void addPins(IPinList pinList, ISchemDiagram diagram, CompositePinConnectivityFinder connectivityFinder)
	{
		//must call this before processing add pins.
		ensurePlacingPinArgsDistributed();
		transferPreemiesToCDH();
		if (m_pinActionModel != null) {
			for (IPinList candidate : m_pinActionModel.getPinLists()) {
				Set<IPinList> matePinLists = m_pinActionModel.getMatePinLists(candidate);
				if (matePinLists.isEmpty()) {
					addPins(candidate, null, diagram, connectivityFinder);
				}
				else {
					for (IPinList matedSchemPinList : matePinLists) {
						addPins(candidate, matedSchemPinList, diagram, connectivityFinder);
					}
				}
			}
		}
		else {
			for (IPinList candidate : getEditedPinLists(pinList)) {
				addPins(candidate, null, diagram, connectivityFinder);
			}
		}
	}

	protected void addPins(IPinList pinList, @Nullable IPinList matePinList, ISchemDiagram diagram,
			CompositePinConnectivityFinder connectivityFinder)
	{
		List<AddPinArgs> candidateStackPins = new ArrayList<AddPinArgs>();
		List<IAbstractSchemPin> newSchemPins = new ArrayList<IAbstractSchemPin>();
		List<IPin> newSchemConnectorPins = new ArrayList<IPin>();
		DSCWithBackshellPlaceholderHelper helper = new DSCWithBackshellPlaceholderHelper(pinList);
		IPinList plForPlaceholderCreation = helper.getPinListForPlaceholderCreation();
		chs.cof.logical.cable.IPinList plc = pinList.getConnectivity();
		ILibraryObject libObj = (ILibraryObject) plc.getLibraryObject();
		m_libraryCavities = Collections.emptySet();
		if (libObj != null) {
			m_libraryCavities = LibraryHelper.getCavities(libObj);
		}

		boolean bConnector = false;
		if (plc instanceof IConnector) {
			bConnector = true;
		}
		IGrid grid = diagram.getGrid();
		IPreferenceSet styleSet = PreferenceSetHelper.getStyleSet(diagram);
		GeneratorParameters gp = new GeneratorParameters(grid, styleSet);
		Generator generator = Generator.getGenerator();
		generator.generate(plForPlaceholderCreation, gp, Generator.NOREGENERATE_PROPERTIES, false);
		if (matePinList != null) {
			generator.generate(matePinList, gp, Generator.NOREGENERATE_PROPERTIES, false);
		}
		List<Pair<AddPinArgs, IPin>> deferedPinArgs = new ArrayList<>();
		try(DeferPinListRegenerationAutoClosable ignored = new DeferPinListRegenerationAutoClosable(pinList)){
			DeferrablePinListRegenerator.getInstance().suspendPinListRegeneration();
			for (AddPinArgs args : getPinsCommitedToPlace(pinList)) {
				if (args.isStackPin()) {
					candidateStackPins.add(args);
					continue;
				}
				IPin newSchemPin = createPinOnly(pinList, diagram, args);
				if (newSchemPin != null) {
					newSchemPins.add(newSchemPin);
					if (bConnector) {
						newSchemConnectorPins.add(newSchemPin);
					}
					deferedPinArgs.add(new Pair<AddPinArgs, IPin>(args, newSchemPin));
				}
			}
		}

		ISchemStackPin stackPin = createStackPin(pinList, matePinList, diagram, candidateStackPins);
		if (stackPin != null) {
			newSchemPins.add(stackPin);
		}
		addConnectivity(pinList);

		GfxView gview = (GfxView) CAFUtils.getInstance().getActiveCapletView();
		IGfxContext context = gview.getGfxContext();
		generator.generate(plForPlaceholderCreation, gp, Generator.NOREGENERATE_PROPERTIES, false);
		if (matePinList != null) {
			generator.generate(matePinList, gp, Generator.NOREGENERATE_PROPERTIES, false);
		}
		List<IPin> tobeStyledObjects = new ArrayList<>();
		List<IAbstractSchemPin> newSchemPinsToBeConnected = new ArrayList<>();

		for (IAbstractSchemPin newSchPin : newSchemPins) {
			if (newSchPin instanceof IPin) {
				if (matePinList != null) {
					newSchemPinsToBeConnected.add(newSchPin);
				}
				tobeStyledObjects.add((IPin) newSchPin);
			}
		}
		if (matePinList != null && !newSchemPinsToBeConnected.isEmpty()) {
			ConnectionHelper connectionHelper = new ConnectionHelper(pinList);
			connectionHelper.resetPinList(matePinList);
			connectionHelper.connectPins(newSchemPinsToBeConnected, diagram.getGrid(), true, false);
		}

		if (matePinList != null) {
			for (Pair<AddPinArgs, IPin> pinArgsPair : deferedPinArgs) {
				IPin mateSchemPin = createMatedInlinePin(pinList, matePinList, diagram, pinArgsPair.getFirst(),
						pinArgsPair.getSecond());
				if (mateSchemPin != null) {
					tobeStyledObjects.add(mateSchemPin);
				}
				if (m_pinActionModel != null) {
					m_pinActionModel.registerNewPin(pinArgsPair.getSecond(), mateSchemPin);
				}
			}
		}
		if (matePinList == null) {
			connectivityFinder.collectConnectionMakers(context, newSchemPins);
		}
		// dts010089}                                                               2406 Pin names of a connector are not automatically styled if a library part is assigned with pin graphics
		// Incase if the pin contains the pin graphics, apply style the pins after graphics are generated.
		// So the decorations will be created w.r.t. new pin graphic extent.

		tobeStyledObjects.stream().forEach((styleObject)
				-> PreferenceSetHelper.applyStyleSet(styleObject, diagram, true));

		m_libraryCavities.clear();
		m_libraryCavities = Collections.emptySet();
	}

	protected void addConnectivity(IPinList pinList)
	{

	}

	@Nullable private IPin createMatedInlinePin(IPinList pinList, @Nullable IPinList matePinList, ISchemDiagram
			diagram,
			AddPinArgs args, IPin newSchemPin)
	{
		// not sure when it would be null ?
		if (args.getPin() == null) {
			//only when new connectivity pin is created, it makes sense to copy library details
			PinListAddPinHelper pinlistAddPinHelper = getPinPlacementController().getPinlistAddPinHelper(pinList);
			pinlistAddPinHelper.assignLibraryCavity(newSchemPin, m_libraryCavities);
		}
		// TODO jacobt FEAT13040 : this caused a duplicate schem pin to get added to an inline half - is it safe to remove it?
		// ConnectionHelper should take care of adding pins to the other half, but it is not working for shared (and libraried?) inlines
		ISharedObject sharedPL = pinList.getSharedObject();
		if (sharedPL != null && sharedPL instanceof ISharedDevice &&
				PinListHelper.isHarnessFootprinted(pinList.getConnectivity())) {
			ISharedDevice shDev = (ISharedDevice) sharedPL;
			shDev.markDiagramForHCGeneration(diagram.getUID(), true);
		}
		return creatMatedSchemPin(newSchemPin, args, pinList, matePinList, diagram);
	}

	@Nullable private IPin creatMatedSchemPin(IPin newSchemPin, AddPinArgs args, IPinList pinList,
			@Nullable IPinList matePinList, ISchemDiagram diagram)
	{
		if (matePinList != null) {
			IAbstractPin cablePin = newSchemPin.getConnectivity();
			IAbstractPin connectedPin = cablePin.getConnectedPin(matePinList.getConnectivity());
			// If the pin has a connected pin, this must be either be a pin belonging to
			// an inline, or a reusable pin connected to a reusable pin on an attached pinlist.

			// so in other words we might add a schematic pin opposite the new pin
			ILocation mateLocation = null;
			for (IGfxObjectIterator gobjit = matePinList.getObjects(); gobjit.hasNext(); ) {
				IGfxObject gobj = gobjit.getNext();
				if ((gobj instanceof IPinPlaceholder && ((IPinPlaceholder) gobj).getAllowAddPin()) ||
						gobj instanceof IPin) {
					mateLocation = gobj.getLocation();
					break;
				}
			}

			Point2D matePinPoint =
					mateLocation != null ? new Point2D.Double(mateLocation.getX(), args.getPoint().getY()) : null;
			return createMatedSchemPinAt(newSchemPin, pinList, matePinList, diagram, connectedPin,
					matePinPoint);
		}
		return null;
	}

	@Nullable private IPin createMatedSchemPinAt(IPin newSchemPin, IPinList pinList,
			@NotNull IPinList matePinList, ISchemDiagram diagram, @Nullable IAbstractPin connectedPin,
			@Nullable Point2D matePinPoint)
	{
		IPin mateSchemPin = null;
		if (matePinPoint != null) {
			final IPinList schemPinList = (IPinList) newSchemPin.getParent();
			assert schemPinList != null;
			ConnectionHelper chelper = new ConnectionHelper(schemPinList);
			chelper.resetPinList(matePinList);
			// examine is used to find the mate pin list and then create an inner class
			// PinListConnectionHelper. If we already have the mate pin list, then we don't need to call examine
			if (chelper.getMate(schemPinList) == null) {
				chelper.examine(newSchemPin, diagram);
			}

			IGfxObject match =
					chelper.getMatchingPinPosition(newSchemPin, schemPinList);
			if (!(match instanceof IAbstractSchemPin)) {

				String pinName = newSchemPin.getConnectivity().getName();
				if (matePinList.getConnectivity().isPartAssigned()) {
					IPinList parent = schemPinList;
					IUID orgPart = parent.getConnectivity().getLibraryRef();
					IUID matedPart = matePinList.getConnectivity().getLibraryRef();
					String libPinName = LibraryHelper.getMatedPinName(pinName, orgPart, matedPart);
					if (libPinName != null) {
						pinName = libPinName;
					}
					else {
						ILibraryCavity libCaivity = AddPinHelper
								.getMatchingLibraryPartOnPin(matePinList.getConnectivity(),
										pinList.getConnectivity(), newSchemPin.getConnectivity());
						if (libCaivity != null) {
							pinName = libCaivity.getName();
						}
						else {
							pinName = null;
						}
					}
				}

				PinListAddPinHelper pinlistAddPinHelper = getPinPlacementController().getPinlistAddPinHelper(pinList);
				mateSchemPin = pinlistAddPinHelper.addPin(diagram, matePinPoint, matePinList,
						getConnectivityPinOwner(matePinList.getConnectivity()), connectedPin, pinName);
				ISharedObject matedSharedPL = matePinList.getSharedObject();
				if (matedSharedPL != null &&
						matedSharedPL instanceof ISharedDevice &&
						PinListHelper.isHarnessFootprinted(matePinList.getConnectivity())) {
					ISharedDevice shDev = (ISharedDevice) matedSharedPL;
					shDev.markDiagramForHCGeneration(diagram.getUID(), true);
				}
			}
			else if (match instanceof IPin) {
				mateSchemPin = (IPin) match;
			}
		}
		return mateSchemPin;
	}

	public void addPins(IPinList pinList, @Nullable IPinList matePinList, ISchemDiagram diagram)
	{
		//must call this before processing add pins.
		ensurePlacingPinArgsDistributed();
		transferPreemiesToCDH();

		List<AddPinArgs> candidateStackPins = new ArrayList<AddPinArgs>();
		List<IPin> newSchemPins = new ArrayList<IPin>();
		chs.cof.logical.cable.IPinList plc = pinList.getConnectivity();
		if (plc == null) {
			System.out.println("Connectivity of schem pinlist with UID : " + pinList.getUID() + " is missing");
		}
		assert plc != null;
		ILibraryObject libObj = (ILibraryObject) plc.getLibraryObject();
		m_libraryCavities = Collections.emptySet();
		if (libObj != null) {
			m_libraryCavities = LibraryHelper.getCavities(libObj);
		}
		IGrid grid = diagram.getGrid();
		IPreferenceSet styleSet = PreferenceSetHelper.getStyleSet(diagram);
		GeneratorParameters gp = new GeneratorParameters(grid, styleSet);
		Generator generator = Generator.getGenerator();
		generator.generate(pinList, gp, Generator.NOREGENERATE_PROPERTIES, false);
		if (matePinList != null) {
			generator.generate(matePinList, gp, Generator.NOREGENERATE_PROPERTIES, false);
		}
		for (AddPinArgs args : getPinsCommitedToPlace(pinList)) {
			if (args.isStackPin()) {
				candidateStackPins.add(args);
				continue;
			}
			IPin newSchemPin = createPin(pinList, matePinList, diagram, args);
			if (newSchemPin != null) {
				newSchemPins.add(newSchemPin);
			}
		}

		createStackPin(pinList, matePinList, diagram, candidateStackPins);
		generator.generate(pinList, gp, Generator.NOREGENERATE_PROPERTIES, false);
		regenerateGraphics(pinList);
		if (matePinList != null) {
			generator.generate(matePinList, gp, Generator.NOREGENERATE_PROPERTIES, false);
			regenerateGraphics(matePinList);
		}

		if (matePinList != null) {
			ConnectionHelper connectionHelper = new ConnectionHelper(pinList);
			connectionHelper.resetPinList(matePinList);
			for (IAbstractSchemPin newSchPin : newSchemPins) {
				if (newSchPin instanceof IPin) {
					connectionHelper.connectPin(newSchPin, diagram.getGrid(), true, false, new LibraryObjectInfoCache());
				}
			}
		}
		// dts0100892406 Pin names of a connector are not automatically styled if a library part is assigned with pin graphics
		// Incase if the pin contains the pin graphics, apply style the pins after graphics are generated.
		// So the decorations will be created w.r.t. new pin graphic extent.
		for (IPin newSchemPin : newSchemPins) {
			PreferenceSetHelper.applyStyleSet(newSchemPin, diagram, true);
		}

		m_libraryCavities.clear();
		m_libraryCavities = Collections.emptySet();
	}

	@Nullable
	private IPin createPinOnly(IPinList pinList, ISchemDiagram diagram, AddPinArgs args)
	{
		PinListAddPinHelper pinlistAddPinHelper = getPinPlacementController().getPinlistAddPinHelper(pinList);
		chs.cof.logical.cable.IPinList cableOwner = getConnectivityPinOwner(pinList.getConnectivity());
		ISharedObject sharedPL = pinList.getSharedObject();
		IPin newlyCreated = pinlistAddPinHelper
				.addPinOnly(diagram, args.getPoint(), pinList, cableOwner, args.getPin(),
						args.getInternalPin(), args.getName(), args.getAssociatedObjectType(),
						args.getAssociatedObject(), args.getAssociatedPinName());
		if (newlyCreated != null) { // not sure when it would be null ?
			if (args.getPin() == null) {
				boolean libraryCavityAssignationComplete = args.assignLibraryCavity(newlyCreated.getConnectivity());

				if (!libraryCavityAssignationComplete &&
						newlyCreated.getConnectivity() == getConnectivityPinOwner(pinList.getConnectivity())) {
					//only when new connectivity pin is created, it makes sense to copy library details
					pinlistAddPinHelper.assignLibraryCavity(newlyCreated, m_libraryCavities);
				}
			}
			if (sharedPL instanceof ISharedDevice && PinListHelper.isHarnessFootprinted(pinList.getConnectivity())) {
				ISharedDevice shDev = (ISharedDevice) sharedPL;
				shDev.markDiagramForHCGeneration(diagram.getUID(), true);
			}
			if (m_pinActionModel != null) {
				m_pinActionModel.registerNewPin(newlyCreated, null);
			}
		}
		return newlyCreated;
	}

	@Nullable
	private IPin createPin(IPinList pinList, @Nullable IPinList matePinList, ISchemDiagram diagram, AddPinArgs args)
	{
		chs.cof.logical.cable.IPinList cablePinOwner = getConnectivityPinOwner(pinList.getConnectivity());
		PinListAddPinHelper pinlistAddPinHelper = getPinPlacementController().getPinlistAddPinHelper(pinList);
		IPin newSchemPin = pinlistAddPinHelper.addPin(diagram, args.getPoint(), pinList,
				cablePinOwner, args.getPin(), args.getInternalPin(), args.getName(),
				args.getAssociatedObjectType(), args.getAssociatedObject(), args.getAssociatedPinName());

		if (newSchemPin != null) { // not sure when it would be null ?
			if (args.getPin() ==
					null) {    //only when new connectivity pin is created, it makes sense to copy library details
				boolean libraryCavityAssignationComplete = args.assignLibraryCavity(newSchemPin.getConnectivity());

				if (!libraryCavityAssignationComplete &&
						newSchemPin.getConnectivity().getOwner() == pinList.getConnectivity()) {
					pinlistAddPinHelper.assignLibraryCavity(newSchemPin, m_libraryCavities);
				}
			}
			// TODO jacobt FEAT13040 : this caused a duplicate schem pin to get added to an inline half - is it safe to remove it?
			// ConnectionHelper should take care of adding pins to the other half, but it is not working for shared (and libraried?) inlines
			ISharedObject sharedPL = pinList.getSharedObject();
			if (sharedPL != null && sharedPL instanceof ISharedDevice &&
					PinListHelper.isHarnessFootprinted(pinList.getConnectivity())) {
				ISharedDevice shDev = (ISharedDevice) sharedPL;
				shDev.markDiagramForHCGeneration(diagram.getUID(), true);
			}
			IPin mateSchemPin = creatMatedSchemPin(newSchemPin, args, pinList, matePinList, diagram);
			if (m_pinActionModel != null) {
				m_pinActionModel.registerNewPin(newSchemPin, mateSchemPin);
			}
		}
		return newSchemPin;
	}

	@Nullable
	private ISchemStackPin createStackPin(IPinList pinList, @Nullable IPinList matePinList, ISchemDiagram diagram,
			List<AddPinArgs> candidateStackPins)
	{
		ISchemStackPin pinStack = null;
		if (isPlaceAsStack() && !candidateStackPins.isEmpty()) {
			Point2D location = null;
			for (AddPinArgs pinArgs : candidateStackPins) {
				if (pinArgs.getPoint() != null) {
					location = pinArgs.getPoint();
					break;
				}
			}

			if (location == null) {
				return null;
			}

			PinListAddPinHelper pinlistAddPinHelper = getPinPlacementController().getPinlistAddPinHelper(pinList);
			// Creates stack pin with the given pins if stack is not already not there at given location else adds the given pins to the existing stack
			pinStack = pinlistAddPinHelper.addStackPin(getModel().getDiagram(), location, pinList,
					getConnectivityPinOwner(pinList.getConnectivity()), candidateStackPins);
			RegenerateGraphicsAction.getInstance().addObjectForRegenrate(pinStack);

			ISchemStackPin matedStackPin = null;
			if (pinStack != null && matePinList != null) {
				// If the pin has a connected pin, this must be either be a pin belonging to
				// an inline, or a reusable pin connected to a reusable pin on an attached pinlist.

				// so in other words we might add a schematic pin opposite the new pin
				ILocation mateLocation = null;
				for (IGfxObjectIterator gobjit = matePinList.getObjects(); gobjit.hasNext(); ) {
					IGfxObject gobj = gobjit.getNext();
					if ((gobj instanceof IPinPlaceholder && ((IPinPlaceholder) gobj).getAllowAddPin()) ||
							gobj instanceof IPin) {
						mateLocation = gobj.getLocation();
						break;
					}
				}
				if (mateLocation != null) {
					ConnectionHelper chelper = new ConnectionHelper(pinList);
					chelper.resetPinList(matePinList);
					Point2D matePinPoint = new Point2D.Double(mateLocation.getX(), location.getY());
					chelper.examine(pinStack, diagram);
					IGfxObject match = chelper.getMatchingPinPosition(pinStack, pinList);
					if (!(match instanceof IAbstractSchemPin)) {
						List<IAbstractPin> connectedPins = getConnectedPinsOfStack(pinList, matePinList, pinStack);
						matedStackPin = pinlistAddPinHelper
								.addStackPin(getModel().getDiagram(), matePinPoint, matePinList, null,
										getConnectivityPinOwner(matePinList.getConnectivity()),
										getConnectivityPinOwner(matePinList.getConnectivity()),
										connectedPins);
						RegenerateGraphicsAction.getInstance().addObjectForRegenrate(matedStackPin);
					}
					else if (match instanceof ISchemStackPin) {
						matedStackPin = (ISchemStackPin) match;
					}
				}
			}
			if (m_pinActionModel != null && pinStack != null) {
				m_pinActionModel.registerNewPin(pinStack, matedStackPin);
			}
		}
		return pinStack;
	}

	private List<IAbstractPin> getConnectedPinsOfStack(IPinList pinList, IPinList matePinList, ISchemStackPin pinStack)
	{
		List<IAbstractPin> connectedPins = new ArrayList<IAbstractPin>();
		chs.cof.logical.cable.IPinList matedCablePinList = matePinList.getConnectivity();
		ILibraryObjectInfoCache libraryObjectInfoCache = new LibraryObjectInfoCache();
		for (IAbstractPin pin : pinStack.getAllConnectivity()) {
			IAbstractPin connectedPin = pin.getConnectedPin(matedCablePinList);
			if (connectedPin == null) {
				AddPinHelper.CablePinGenerator cablePinGenerator =
						new AddPinHelper.CablePinGenerator(matedCablePinList, null, null);
				cablePinGenerator.generate();
				connectedPin = cablePinGenerator.getCpin();
				connectedPin.connectIfPossible(pin);
				chs.cof.logical.cable.IPinList connectedPinOwner = connectedPin.getOwner();
				if (connectedPinOwner != null && connectedPinOwner.isPartAssigned()) {
					AddPinHelper.assignLibraryPartOnPin(connectedPinOwner, connectedPin, pin, libraryObjectInfoCache);
				}
				connectedPins.add(connectedPin);
			}
			else {
				connectedPins.add(connectedPin);
			}

			if (matedCablePinList.isPartAssigned()) {
				String pinName = pin.getName();
				IUID orgPart = pinList.getConnectivity().getLibraryRef();
				IUID matedPart = matedCablePinList.getLibraryRef();
				String libPinName = LibraryHelper.getMatedPinName(pinName, orgPart, matedPart);
				if (libPinName != null) {
					pinName = libPinName;
				}
				else if (orgPart != matedPart) {
					pinName = null;
				}
				if (!StringUtils.isBlank(pinName)) {
					connectedPin.setName(pinName);
				}
			}
		}
		return connectedPins;
	}

	public boolean initialize(AddPinActionModel pinActionModel, boolean altPressed,
			boolean shiftNotPressed)
	{
		m_pinActionModel = pinActionModel;

		// show a dialog to select the pins
		IPinList pinList = m_pinActionModel.getReference();
		boolean setup = false;
		if (pinList != null) {
			chs.cof.logical.cable.IPinList cpl = pinList.getConnectivity();
			if (cpl.isShared()) {
				// shared - use a different dialog for adding shared pins, AddPinActionHelper currently pops up
				// helper should probably not popup the dialog & perhaps we should use a common dialog here?
				// We need to note the matePinList here as this normally only gets initialized when we show the dialog
				// (which we don't do for shared as this is handled by the helper)
				m_pinActionModel.addMatePinList(pinList, (IPinList) SchemPinListHelper.getInlineMateObject(pinList));
				boolean bUseBoundaryExtensions = !IConnector.Statics.isRingTerminalTypeConnector(pinList);
				setup = setUp(pinList, bUseBoundaryExtensions);
			}
			else {
				IPlacementOptionParams params = createPlacementOptionParams(cpl);
				IDeviceICD selectICD = ICDUtils.getMappedICD(CommonUtils.cast(cpl, IDevice.class));
				if (selectICD != null) {
					ILibraryDevice transientLibraryDevice = null;
					try {
						transientLibraryDevice = PinListAddPinHelper.assignTransiantLibraryPart(selectICD, cpl);
						setup = showDialog(cpl, params);
					}
					finally {
						PinListAddPinHelper.removeTransiantLibraryPart(transientLibraryDevice, cpl);
					}
				}
				else {
					// unshared - use the place pins dialog, sometimes allowing creation of a new connectivity pin
					if (cpl.getLibraryRef() != null || altPressed || shiftNotPressed) {
						setup = showDialog(cpl, params);
					}
					else {
						m_pinActionModel
								.addMatePinList(pinList, (IPinList) SchemPinListHelper.getInlineMateObject(pinList));
						boolean bUseBoundaryExtensions = !IConnector.Statics.isRingTerminalTypeConnector(pinList);
						setup = setUp(pinList, bUseBoundaryExtensions);
					}
				}
			}
		}

		if (!setup) {
			// dialog cancelled
			cleanUp(false);
			return false;
		}
		LogicUtils.deferRegenerationOfSchemDeviceConnectors();
		return true;
	}

	@NotNull protected IPlacementOptionParams createPlacementOptionParams(@NotNull chs.cof.logical.cable.IPinList cpl)
	{
		IPlacementOptionParams params = new PinPlaceOptionsParams(cpl);
		enableWithConductorOptionIfValid(params, cpl);
		return params;
	}

	private void enableWithConductorOptionIfValid(@NotNull IPlacementOptionParams params,
			@NotNull chs.cof.logical.cable.IPinList cpl)
	{
		IDeviceICD selectICD = ICDUtils.getMappedICD(CommonUtils.cast(cpl, IDevice.class));
		if (selectICD != null) {
			params.enableWithConductorOption(true, cpl.getProject());
		}
	}

	public boolean initializePresenterForWNAccel(AddPinActionModel pinActionModel, @NotNull IPlacementOptionParams params)
	{
		m_pinActionModel = pinActionModel;

		// show a dialog to select the pins
		IPinList pinList = m_pinActionModel.getReference();
		boolean setup = false;
		if (pinList != null) {
			chs.cof.logical.cable.IPinList cpl = pinList.getConnectivity();
			if (cpl.isShared()) {
				// shared - use a different dialog for adding shared pins, AddPinActionHelper currently pops up
				// helper should probably not popup the dialog & perhaps we should use a common dialog here?
				// We need to note the matePinList here as this normally only gets initialized when we show the dialog
				// (which we don't do for shared as this is handled by the helper)
				m_pinActionModel.addMatePinList(pinList, (IPinList) SchemPinListHelper.getInlineMateObject(pinList));
				setup = setUp(pinList);
			}
			else {
				// unshared - use the place pins dialog, sometimes allowing creation of a new connectivity pin
				setup = showDialog(cpl, params);
			}
		}

		if (!setup) {
			// dialog cancelled
			cleanUp(false);
			return false;
		}
		return true;
	}

	public boolean execute(boolean successful)
	{
		if (successful) {
			commit();
		}
		else {
			undoModelChanges();
		}
		cleanUp(successful);
		boolean itWorked = doPostCommitChanges(successful);

		return itWorked;
	}

	private IAddPinView m_view;

	public void setView(IAddPinView view)
	{
		m_view = view;
	}

	protected boolean showDialog(chs.cof.logical.cable.IPinList cpl, @NotNull IPlacementOptionParams params)
	{
		if (m_view == null) {
			m_view = getPlacePinDialog(cpl, params);
		}

		PlacePinsDialog.Result result = m_view.showDialog();
		boolean setup = false;
		if (result != PlacePinsDialog.Result.CANCEL) {
			setIsReference(m_view.isReference());
			setPlaceAsStack(m_view.isPlaceAsStack());
			setPlaceAsGroup(m_view.isPlaceAsGroup());
			setWithConductor(m_view.isWithConductor());
			IPinList anchor = m_pinActionModel.getReference();
			assert anchor != null;
			m_pinActionModel.addMatePinList(anchor, (IPinList) SchemPinListHelper.getInlineMateObject(anchor));
			if (result == PlacePinsDialog.Result.PLACE) {
				// place the pins selected from the dialog
				setup = setUp(anchor, m_view.getPins());
			}
			else {
				// place a new connectivity + schematic pin
				setPlaceAsGroup(false);
				setup = setUp(anchor);
			}
		}
		return setup;
	}

	@NotNull
	protected IAddPinView getPlacePinDialog(chs.cof.logical.cable.IPinList cpl, @NotNull IPlacementOptionParams params)
	{
		if (cpl instanceof IBlockDevice) {
			return new PlaceBlockPinDialog(CAFUtils.getInstance().getDialogFrame(),
					(IAddBlockPinActionModel) m_pinActionModel, false, params);
		}
		return new PlacePinsDialog(CAFUtils.getInstance().getDialogFrame(), cpl, isPinCreationAllowed(cpl), params);
	}

	protected boolean isPinCreationAllowed(chs.cof.logical.cable.IPinList cablePinList)
	{
		// simplistic for now...
		return !cablePinList.isShared() && cablePinList.getLibraryRef() == null;
	}

	public void commit()
	{
		IPinList referencePinList = m_pinActionModel.getReference();
		if (referencePinList != null) {
			chs.cof.logical.cable.IPinList cablePinList = referencePinList.getConnectivity();
			if (cablePinList instanceof IConnector) {
				createRequiredModularChildren(((IConnector) cablePinList).getTopLevelConnector());
			}
		}
		//must call this before processing add pins.
		ensurePlacingPinArgsDistributed();
		transferPreemiesToCDH();
		for (IPinList pinList : m_pinActionModel.getPinLists()) {
			doCommit(pinList);
		}
	}

	private void createRequiredModularChildren(IConnector connector)
	{
		if (connector.getSharedPinList() != null) {
			Set<IPinProxy> pinProxies = getPinsToAdd().stream().filter(pinArg -> pinArg.getPinProxy() != null)
					.map(pinArg -> pinArg.getPinProxy()).collect(Collectors.toSet());
			ModularConnectorHelper.createRequiredModularChildConnectors(connector,
					(ISharedConnector) connector.getSharedPinList(), pinProxies);
		}
	}

	private void doCommit(@NotNull IPinList pinList)
	{
		ISchemDiagram diagram = DiagramHelper.getDiagram(pinList);
		assert (diagram != null);

		CompositePinConnectivityFinder connectivityFinder = new CompositePinConnectivityFinder(diagram);

		Set<IPinList> matePinLists = m_pinActionModel.getMatePinLists(pinList);
		List<IPinList> pinLists = new ArrayList<IPinList>();
		pinLists.add(pinList);
		if (matePinLists.isEmpty()) {
			addPins(pinList, null, diagram, connectivityFinder);
		}
		else {
			for (IPinList matedSchemPinList : matePinLists) {
				addPins(pinList, matedSchemPinList, diagram, connectivityFinder);
				pinLists.add(matedSchemPinList);
			}
		}

		Runnable regenerateGrphics = () -> {
			pinLists.forEach(this::regenerateGraphics);
		};

		ConductorSplitter splitter = ConductorSplitter.createConductorSplitter(pinList);
		IGfxView activeCapletView = (IGfxView) CAFUtils.getInstance().getActiveCapletView();
		splitter.splitConductors(pinLists, activeCapletView, connectivityFinder, regenerateGrphics);

		createConnectionSchematics(pinList);
	}

	public void createConnectionSchematics(@NotNull IPinList pinList)
	{
		ISchemDiagram diagram = DiagramHelper.getDiagram(pinList);
		if (diagram != null) {
			ObjectConnectionsGetter.createConnectionSchematics(m_pinActionModel, diagram);
		}
	}

	public boolean doPostCommitChanges(boolean actionSuccessful)
	{
		// refresh attached representations
		// dts0100633838 [CH] java.lang.NullPointerException  at chs.caplets.logic.actions.AddPinAction.onTerminate(AddPinAction.java:152)
		// somehow this step was triggering NPE
		// we don't know which item or how it could happen - add some additional diagnostics and checks for now
		// allow the action to complete in the customer environment
		boolean itWorked = true;
		Set<IPinList> pinLists = m_pinActionModel.getPinLists();
		if (!pinLists.isEmpty()) {
			for (IPinList pinList : pinLists) {
				chs.cof.logical.cable.IPinList cpl = pinList.getConnectivity();
				if (cpl != null) {
					Collection<ISecondaryRepresentation> reps = cpl.getAssociateRepresentations();
					if (reps != null) {
						for (ISecondaryRepresentation secRep : reps) {
							secRep.regenerateDiagramObject();
						}
					}
					else {
						assert false : diagnosticMessage("Secondary representations are null", actionSuccessful);
						itWorked = false;
					}
				}
				else {
					assert false : diagnosticMessage("Connectivity pinlist is null", actionSuccessful);
					itWorked = false;
				}
			}
		}
		else {
			assert false : diagnosticMessage("Schematic pinlist is null", actionSuccessful);
			itWorked = false;
		}

		//do not cleanup the model here. because this model is not created by this class.
		//this is helping out this class to do the work. the clean-up should be done by
		//addpin action which creates this model. after this execution also the model could
		//be used by action for example during icd routing activity.
		m_pinActionModel = null;

		return itWorked;
	}

	private String diagnosticMessage(String msg, boolean successful)
	{
		return msg + " on AddPinAction.onTerminate(" + successful + ") : ";
	}

	public void undoModelChanges()
	{
		// if we cancelled the action part way through we might have this stuff hanging around in the UIDMgr
		Iterator<IUIDObject> newObjects = CreationDeletionHelper.getTheCreationHelper().getNewObjectsToProcess();
		while (newObjects.hasNext()) {
			IUIDObject obj = newObjects.next();
			if (obj instanceof IDevice) {
				// this temp device must be removed from the connectivity!
				IDevice tempDevice = (IDevice) obj;
				tempDevice.getConnectivity().removeDevice(tempDevice);
			}
			UIDMgr.removeObject(obj.getUID());
		}
	}

	public void cleanUp(boolean modificationMade)
	{
		super.cleanUp(modificationMade);
		m_view = null;
		m_libraryCavities.clear();
		m_libraryCavities = Collections.emptySet();
	}
}