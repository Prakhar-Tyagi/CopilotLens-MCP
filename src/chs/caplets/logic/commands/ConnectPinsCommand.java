/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.commands;

import chs.caf.CAFUtils;
import chs.caf.WaitCursor;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caplets.logic.actions.ConnectAction;
import chs.caplets.logic.actions.connection.IConnectablePin;
import chs.caplets.logic.actions.connection.IConnectablePinGroup;
import chs.caplets.logic.actions.connection.IConnectablePinGroupProvider;
import chs.caplets.logic.actions.shared.CreateConductorInstanceActionHelper;
import chs.cof.draw.IGrid;
import chs.cof.drawplus.IJoint;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.cable.ISplicePin;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.schem.ISegment;
import chs.cofUtils.cmd.CHSCommand;
import chs.cofUtils.cmd.CreateSchemConductorCmd;
import chs.cofUtils.parameterized.AddSpliceHelper;
import chs.common.ILocation;
import chs.common.INamedUIDObject;
import chs.common.IUIDObject;
import chs.common.IUIDObjectCollection;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utilities.StringUtils;
import chs.utilities.UnorderedPair;
import chs.utility.DiagramHelper;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.NodeHelper;
import chs.utility.helpers.SegmentHelper;
import chs.utility.logic.LogicUtils;
import chs.utility.logic.PinUtils;
import chs.utility.preferences.PreferenceSetHelper;
import chs.utility.ui.HTMLHelper;
import chs.view.utils.DiagramGenerationUtilities;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author rmahato
 */
public class ConnectPinsCommand extends CHSCommand
{

	private IConnectablePinGroupProvider pinProvider;
	private Class<? extends IConductor> conductorClass;
	@NotNull private ILogicDesign m_design;
	@NotNull private ISchemDiagram m_diagram;
	private Action actionUI;
	private boolean conductorsCreated;
	protected Collection<String> errors;
	@NotNull private SetMap<IPinList, IPinList> connectedPinlistsCache;
	@NotNull private Set<Point> m_alreadyUsedGridPoints;
	@NotNull private Set<IPinList> m_ignoredSplices;

	public ConnectPinsCommand(@NotNull IConnectablePinGroupProvider pinProvider,
			@NotNull Class<? extends IConductor> conductorClass, @NotNull ILogicDesign design,
			@NotNull ISchemDiagram diagram, @NotNull Action actionUI)
	{
		this.pinProvider = pinProvider;
		this.conductorClass = conductorClass;
		m_design = design;
		m_diagram = diagram;
		this.actionUI = actionUI;
		conductorsCreated = false;
		errors = new ArrayList<>();
		connectedPinlistsCache = new SetMap<>();
		m_alreadyUsedGridPoints = new HashSet<>();
		m_ignoredSplices = new LinkedHashSet<>();
	}

	@Override protected boolean doExecute()
	{
		try (WaitCursor ignored = new WaitCursor()) {
			errors.clear();
			List<IConnectablePinGroup> connectionCandidates = pinProvider.getConnectionCandidates(m_design);

			if (m_design.isUnderConcurrentEdit()) {
				m_design.refresh();
			}
			if (!ConnectAction.attemptLockingRequiredObjects(getAllLockables(connectionCandidates), m_design,
					getLockFailureMessage())) {
				return true;
			}
			for (IConnectablePinGroup canditate : connectionCandidates) {
				connect(canditate.getConnectablePins());
			}
			if (!m_ignoredSplices.isEmpty()) {
				String spliceRefText = m_ignoredSplices.stream()
						.map(splice -> getHTMLLink(splice))
						.collect(Collectors.joining(StringUtils.COMMA_SPACE));
				reportMsg(ResourceMgr.getString(ConnectPinsCommand.class, "ConnectPinsCommand.ignoringSplice.message",
						spliceRefText));
			}
			if (!conductorsCreated) {
				String message = HTMLHelper.bold(actionUI.getValue(Action.SHORT_DESCRIPTION) + ": ") +
						ResourceMgr
								.getString(ConnectPinsCommand.class, "ConnectPinsCommand.noConnectionCreated.message");
				errors.add(message);
			}
			showAllErrors();
		}

		return true;
	}

	private void showAllErrors()
	{
		for (String error : errors) {
			CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(error);
		}
	}

	@NotNull private String getLockFailureMessage()
	{
		return ResourceMgr
				.getString(ConnectAction.class, "ConnectAction.error.unableToLock", actionUI.getValue(Action.NAME));
	}

	@NotNull private Set<IUIDObject> getAllLockables(@NotNull Collection<IConnectablePinGroup> candidatePinGroups)
	{
		Set<IUIDObject> lockables = new HashSet<>();
		for (IConnectablePinGroup candidatePinGroup : candidatePinGroups) {
			for (IConnectablePin connectablePin : candidatePinGroup.getConnectablePins()) {
				lockables.addAll(connectablePin.getLockable());
			}
		}
		return lockables;
	}

	private void connect(@NotNull List<IConnectablePin> candidatePins)
	{
		List<IConnectablePin> connectablePins = candidatePins.stream()
				.filter(pin -> !hasShieldConductor(pin.getPin()))
				.collect(Collectors.toList());
		boolean connectMultiplePinlist = getRepresentativePinlistsCount(connectablePins) > 2;
		if (connectMultiplePinlist) {
			List<IConnectablePin> pinsWithExistingConnections = new ArrayList<>();
			List<IConnectablePin> splicePins = new ArrayList<>();
			List<IConnectablePin> applicablePinsForConnection = new ArrayList<>();
			for (IConnectablePin connectablePin : connectablePins) {
				if (isSplicePin(connectablePin)) {
					splicePins.add(connectablePin);
				}
				else if (connectablePin.getPin().getNumConductors() != 0) {
					pinsWithExistingConnections.add(connectablePin);
				}
				else {
					applicablePinsForConnection.add(connectablePin);
				}
			}
			if (!pinsWithExistingConnections.isEmpty() && !applicablePinsForConnection.isEmpty()) {
				reportMsg(ResourceMgr.getString(ConnectPinsCommand.class,
						"ConnectPinsCommand.errorMsg.somePinsNotConnectedDueToExistingConnections",
						stringifyConnectablePins(pinsWithExistingConnections),
						stringifyConnectablePins(applicablePinsForConnection)));
			}
			if (pinsWithExistingConnections.size() > 1 && applicablePinsForConnection.isEmpty()) {
				reportMsg(ResourceMgr.getString(ConnectPinsCommand.class,
						"ConnectPinsCommand.errorMsg.allPinsNotConnectedDueToExistingConnections",
						stringifyConnectablePins(pinsWithExistingConnections)));
			}
			if (applicablePinsForConnection.size() < 2) {
				return;
			}
			connectMultiplePinlist = getRepresentativePinlistsCount(applicablePinsForConnection) > 2;
			connectablePins = new ArrayList<>();
			connectablePins.addAll(applicablePinsForConnection);
			if (connectMultiplePinlist) {
				connectablePins.addAll(splicePins);
			}
		}
		if (connectMultiplePinlist) {
			connectThroughCommonPoint(connectablePins);
		}
		else {
			List<IConnectablePin> applicablePins =
					connectablePins.stream().filter(Predicate.not(this::isSplicePin)).collect(Collectors.toList());
			createExhaustiveConnections(applicablePins);
		}
	}

	private void reportMsg(@NotNull String msg)
	{
		String outputMsg = HTMLHelper.bold(actionUI.getValue(Action.SHORT_DESCRIPTION) + ": ") + msg;
		errors.add(outputMsg);
	}

	private boolean isSplicePin(@NotNull IConnectablePin connectablePin)
	{
		return connectablePin.getPin() instanceof ISplicePin;
	}

	@NotNull private String stringifyConnectablePins(@NotNull List<IConnectablePin> connectablePins)
	{
		return connectablePins.stream()
				.map(this::getPinDisplayText)
				.collect(Collectors.joining(StringUtils.COMMA_SPACE));
	}

	@NotNull private String getPinDisplayText(@NotNull IConnectablePin connectablePin)
	{
		IAbstractPin pin = connectablePin.getPin();
		IPinList owner = pin.getOwner();
		StringBuilder sb = new StringBuilder();
		if (owner != null) {
			sb.append(getHTMLLink(owner)).append(StringUtils.COLON);
		}
		sb.append(getHTMLLink(pin));
		return sb.toString();
	}

	@NotNull private String getHTMLLink(@NotNull INamedUIDObject namedUIDObject)
	{
		return HTMLHelper.link(namedUIDObject.getUID(), namedUIDObject.getName());
	}

	private int getRepresentativePinlistsCount(@NotNull Collection<IConnectablePin> connectablePins)
	{
		Set<IPinList> candidatePinlists = new HashSet<>();
		for (IConnectablePin connectablePin : connectablePins) {
			IPinList owner = PinUtils.getRootOwnerPinList(connectablePin.getPin());
			if (owner == null || owner instanceof ISplice) {
				continue;
			}
			owner = LogicUtils.getTopLevelPinList(owner);
			if (getConnectedPinLists(owner).stream().noneMatch(candidatePinlists::contains)) {
				candidatePinlists.add(owner);
			}
		}
		return candidatePinlists.size();
	}

	@NotNull private Set<IPinList> getConnectedPinLists(@NotNull IPinList owner)
	{
		if (!connectedPinlistsCache.containsKey(owner)) {
			connectedPinlistsCache.put(owner, owner.getConnectedPinLists());
		}
		return connectedPinlistsCache.pullReadOnlySafeSet(owner);
	}

	private void createExhaustiveConnections(@NotNull List<IConnectablePin> connectablePins)
	{
		IConnectablePin[] connectablePinsArray = connectablePins.toArray(IConnectablePin[]::new);
		for (int i = 0; i < connectablePinsArray.length; i++) {
			IConnectablePin connectablePin1 = connectablePinsArray[i];
			for (int j = i + 1; j < connectablePinsArray.length; j++) {
				IConnectablePin connectablePin2 = connectablePinsArray[j];
				for (IAbstractSchemPin schem1 : connectablePin1.getSchemRepresentations()) {
					for (IAbstractSchemPin schem2 : connectablePin2.getSchemRepresentations()) {
						connectPins(schem1, schem2,
								new UnorderedPair<>(connectablePin1.getPin(), connectablePin2.getPin()));
					}
				}
			}
		}
	}

	private void connectThroughCommonPoint(@NotNull List<IConnectablePin> connectablePins)
	{
		Set<IAbstractSchemPin> candidateSchemPins = connectablePins.stream()
				.flatMap(pin -> pin.getSchemRepresentations().stream())
				.collect(Collectors.toSet());
		boolean connectUsingSplice = conductorClass.isAssignableFrom(IWireConductor.class);
		if (connectUsingSplice) {
			chs.cof.logical.schem.IPinList schemSplice = getSpliceToConnect(candidateSchemPins);
			Collection<IPin> splicePins = schemSplice.getPins().getCollection();
			if (!splicePins.isEmpty()) {
				IPin splicePin = splicePins.iterator().next();
				for (IConnectablePin candidatePin : connectablePins) {
					if (isSplicePin(candidatePin) &&
							candidatePin.getPin().getOwner() != schemSplice.getConnectivity()) {
						m_ignoredSplices.add(candidatePin.getPin().getOwner());
						continue;
					}
					for (IAbstractSchemPin schemPin : candidatePin.getSchemRepresentations()) {
						connectPins(splicePin, schemPin,
								new UnorderedPair<>(splicePin.getConnectivity(), candidatePin.getPin()));
					}
				}
			}
		}
		else {
			IConductor cableConductor = null;
			chs.cof.logical.schem.IConductor schemConductor = null;
			ILocation centerLoc = getCommonLocationByMean(candidateSchemPins);
			IJoint centerJoint = NodeHelper.createJointAtLocation(centerLoc);
			for (IConnectablePin candidatePin : connectablePins) {
				if (candidatePin.getPin() instanceof ISplicePin) {
					continue;
				}
				for (IAbstractSchemPin schemPin : candidatePin.getSchemRepresentations()) {
					List<ILocation> points = new ArrayList<>(2);
					points.add(centerJoint);
					points.add(DiagramGenerationUtilities.getNode(schemPin));
					List<ISegment> segments = new ArrayList<>();
					schemConductor = CreateSchemConductorCmd.createConductor(m_design, m_diagram, points,
							conductorClass, segments, cableConductor, schemConductor, false);
					if (schemConductor != null && !segments.isEmpty()) {
						cableConductor = schemConductor.getConnectivity();
						ConductorRouteAction.getInstance().addConductorForRoute(schemConductor);
						ConnectionHelper.connect(candidatePin.getPin(), cableConductor);
						ISegment segment = segments.iterator().next();
						if (schemPin instanceof ISchemStackPin) {
							SegmentHelper
									.connectStackedPin(segment, (ISchemStackPin) schemPin, candidatePin.getPin(), true);
						}
						conductorsCreated = true;
					}
				}
			}
		}
	}

	@NotNull private ILocation getCommonLocationByMean(@NotNull Set<IAbstractSchemPin> candidateSchemPins)
	{
		List<ILocation> pinLocations = candidateSchemPins.stream()
				.map(IAbstractSchemPin::getAbsLocation)
				.collect(Collectors.toList());
		double avgX = 0;
		double avgY = 0;
		int n = pinLocations.size();
		for (ILocation pinLocation : pinLocations) {
			avgX += (double) pinLocation.getX() / n;
			avgY += (double) pinLocation.getY() / n;
		}
		IGrid grid = m_diagram.getGrid();
		int gridLocX = grid.snap(Double.valueOf(avgX).intValue());
		int gridLocY = grid.snap(Double.valueOf(avgY).intValue());
		while (m_alreadyUsedGridPoints.contains(new Point(gridLocX, gridLocY))) {
			gridLocX += grid.getGridSpacing();
		}
		m_alreadyUsedGridPoints.add(new Point(gridLocX, gridLocY));
		return FactoryMgr.getCommonFactory().constructLocation(gridLocX, gridLocY);
	}

	@NotNull
	private chs.cof.logical.schem.IPinList getSpliceToConnect(@NotNull Set<IAbstractSchemPin> candidateSchemPins)
	{
		Set<IPin> splicePins = candidateSchemPins.stream()
				.filter(pin -> pin instanceof IPin)
				.map(pin -> CommonUtils.cast(pin, IPin.class))
				.filter(Objects::nonNull)
				.filter(pin -> pin.getConnectivity() instanceof ISplicePin)
				.collect(Collectors.toSet());

		if (splicePins.size() == 1) {
			IPin spliceSchemPin = splicePins.iterator().next();
			chs.cof.logical.schem.IPinList splice = CommonUtils.cast(spliceSchemPin.getParent(),
					chs.cof.logical.schem.IPinList.class);
			if (splice != null) {
				return (chs.cof.logical.schem.IPinList) Objects
						.requireNonNull(splicePins.iterator().next().getParent());
			}
		}
		return createSplice(candidateSchemPins);
	}

	@NotNull private chs.cof.logical.schem.IPinList createSplice(@NotNull Set<IAbstractSchemPin> candidateSchemPins)
	{
		ILocation location = getCommonLocationByMean(candidateSchemPins);
		ISplice cableSplice = AddSpliceHelper.createCableSplice();
		IConnectivity connectivity = m_design.getConnectivity();
		if (connectivity != null) {
			connectivity.addSplice(cableSplice);
		}
		chs.cof.logical.schem.IPinList schemSplice = AddSpliceHelper.generateSplice(cableSplice, m_diagram.getGrid(),
				null, location.getX(), location.getY());
		m_diagram.addObject(schemSplice);
		PreferenceSetHelper.applyStyleSet(schemSplice, m_diagram.getPreferenceSet(), true);
		return schemSplice;
	}

	private void connectPins(@NotNull IAbstractSchemPin schemPin1,
			@NotNull IAbstractSchemPin schemPin2,
			@NotNull UnorderedPair<IAbstractPin> connectivityPins)
	{
		if (!checkPinsHadDiffertentOwner(connectivityPins) || checkHasSchemConductor(schemPin1, schemPin2) ||
				pinsHaveShieldCondutor(connectivityPins)) {
			return;
		}
		CreateConductorInstanceActionHelper mCreateCondInstanceHelper = new CreateConductorInstanceActionHelper(
				CAFUtils.getInstance().getActiveCapletController());
		ISchemDiagram diagram = DiagramHelper.getDiagram(schemPin1);
		assert diagram != null;
		IAbstractPin pin1 = connectivityPins.getObject1();
		IAbstractPin pin2 = connectivityPins.getObject2();
		Set<IConductor> exsistingCondutors = getExistingConductor(pin1, pin2);
		if (!doesActionTypeMatchesExistingConductors(exsistingCondutors)) {
			return;
		}
		ILogicDesign design = diagram.getDesign();
		assert design != null;

		Supplier<chs.cof.logical.schem.IConductor> schemConductorSupplier = () -> {
			return getShemConductorSupplier(schemPin1, schemPin2, diagram, pin1, pin2, exsistingCondutors, design);
		};
		if (exsistingCondutors.isEmpty()) {
			schemConductorSupplier.get();
		}
		else {
			mCreateCondInstanceHelper
					.processInstanceConductorCreation(exsistingCondutors.iterator().next(), diagram,
							schemConductorSupplier);
		}
		conductorsCreated = true;
	}

	@NotNull private chs.cof.logical.schem.IConductor getShemConductorSupplier(@NotNull IAbstractSchemPin schemPin1,
			@NotNull IAbstractSchemPin schemPin2, @NotNull ISchemDiagram diagram, @NotNull IAbstractPin pin1,
			@NotNull IAbstractPin pin2, @NotNull Set<IConductor> exsistingCondutors, @NotNull ILogicDesign design)
	{
		IConductor existingConductor = null;
		if (!exsistingCondutors.isEmpty()) {
			existingConductor = exsistingCondutors.iterator().next();
		}
		List<?> points = getConnectionPoints(schemPin1, schemPin2);
		List<ISegment> segments = new ArrayList<>();
		chs.cof.logical.schem.IConductor conductor = CreateSchemConductorCmd
				.createConductor(design, diagram, points, conductorClass, segments, existingConductor);
		ConductorRouteAction.getInstance().addConductorForRoute(conductor);
		// connect the conductor endpoints to the pins
		ConnectionHelper.connect(pin1, conductor.getConnectivity());
		ConnectionHelper.connect(pin2, conductor.getConnectivity());
		ISegment segment = segments.iterator().next();
		if (schemPin1 instanceof ISchemStackPin) {
			SegmentHelper.connectStackedPin(segment, (ISchemStackPin) schemPin1, pin1, false);
		}
		if (schemPin2 instanceof ISchemStackPin) {
			SegmentHelper.connectStackedPin(segment, (ISchemStackPin) schemPin2, pin1, true);
		}
		return conductor;
	}

	private boolean doesActionTypeMatchesExistingConductors(@NotNull Set<IConductor> exsistingCondutor)
	{
		for (IConductor conductor : exsistingCondutor) {
			if (!conductorClass.isAssignableFrom(conductor.getClass())) {
				return false;
			}
		}
		return true;
	}

	@NotNull private Set<IConductor> getExistingConductor(@NotNull IAbstractPin pin1, @NotNull IAbstractPin pin2)
	{
		Set<IConductor> existingConductors = new TreeSet<IConductor>(new Comparator<IConductor>()
		{
			public int compare(IConductor o1, IConductor o2)
			{
				return o1.getUID().getString().compareTo(o2.getUID().getString());
			}
		});
		pin1.streamConductors().forEach(conductor1 -> {
			pin2.streamConductors().forEach(conductor2 -> {
				if (conductor2.equals(conductor1)) {
					existingConductors.add(conductor1);
				}
			});
		});
		return existingConductors;
	}

	@NotNull private static List<?> getConnectionPoints(@NotNull IAbstractSchemPin schemPin1,
			@NotNull IAbstractSchemPin schemPin2)
	{
		List<Object> points = new ArrayList<>(2);

		addPoints(schemPin1, points);
		addPoints(schemPin2, points);
		return points;
	}

	private static void addPoints(@NotNull IAbstractSchemPin schemPin1, @NotNull List<Object> points)
	{
		if (schemPin1 instanceof ISchemStackPin) {
			points.add(new Point(DiagramGenerationUtilities.getNode(schemPin1).getX(),
					DiagramGenerationUtilities.getNode(schemPin1).getY()));
		}
		else {
			points.add(DiagramGenerationUtilities.getNode(schemPin1));
		}
	}

	private boolean pinsHaveShieldCondutor(@NotNull UnorderedPair<IAbstractPin> connectivityPins)
	{
		IAbstractPin pin1 = connectivityPins.getObject1();
		IAbstractPin pin2 = connectivityPins.getObject2();
		return hasShieldConductor(pin1) || hasShieldConductor(pin2);
	}

	private boolean hasShieldConductor(@NotNull IAbstractPin pin1)
	{
		for (IConductor conductor : pin1.getConductors()) {
			if (conductor instanceof IShieldConductor) {
				errors.add(getSheildTerminationErrorMessage(pin1));
				return true;
			}
		}
		return false;
	}

	@NotNull private String getSheildTerminationErrorMessage(@NotNull IAbstractPin pin1)
	{
		IPinList pinOwner = pin1.getOwner();
		String pinOwnerLink = pinOwner == null ? "" : getHTMLLink(pinOwner);
		String pinLink = getHTMLLink(pin1);
		String errorMsg;
		if (conductorClass.isAssignableFrom(IWireConductor.class)) {
			errorMsg = ResourceMgr
					.getString(ConnectPinsCommand.class, "ConnectPinsCommand.hasShieldTermination.wire", pinOwnerLink,
							pinLink);
		}
		else {
			errorMsg = ResourceMgr
					.getString(ConnectPinsCommand.class, "ConnectPinsCommand.hasShieldTermination.net", pinOwnerLink,
							pinLink);
		}
		return HTMLHelper.bold(actionUI.getValue(Action.SHORT_DESCRIPTION) + ": ") + errorMsg;
	}

	private boolean checkHasSchemConductor(@NotNull IAbstractSchemPin pin1, @NotNull IAbstractSchemPin pin2)
	{
		if (IPin.class.isAssignableFrom(pin1.getClass()) && IPin.class.isAssignableFrom(pin2.getClass())) {

			return checkAlreadyConnected((IPin) pin1, (IPin) pin2);
		}
		if (IPin.class.isAssignableFrom(pin1.getClass()) &&
				ISchemStackPin.class.isAssignableFrom(pin2.getClass())) {
			IPin pin = (IPin) pin1;
			ISchemStackPin stackPin = (ISchemStackPin) pin2;

			return checkAlreadyConnected(pin, stackPin);
		}
		if (ISchemStackPin.class.isAssignableFrom(pin1.getClass()) &&
				IPin.class.isAssignableFrom(pin2.getClass())) {
			ISchemStackPin stackPin = (ISchemStackPin) pin1;
			IPin pin = (IPin) pin2;

			return checkAlreadyConnected(pin, stackPin);
		}
		if (ISchemStackPin.class.isAssignableFrom(pin1.getClass()) &&
				ISchemStackPin.class.isAssignableFrom(pin2.getClass())) {
			ISchemStackPin stackPin1 = (ISchemStackPin) pin1;
			ISchemStackPin stackPin2 = (ISchemStackPin) pin2;

			return checkAlreadyConnected(stackPin1, stackPin2);
		}
		return false;
	}

	private boolean checkAlreadyConnected(@NotNull IPin pin1, @NotNull IPin pin2)
	{
		IUIDObjectCollection<chs.cof.logical.schem.IConductor> conductors = pin2.getConductors();
		for (chs.cof.logical.schem.IConductor conductor : pin1.getConductors()) {
			if (conductors.contains(conductor)) {
				return true;
			}
		}
		return false;
	}

	private boolean checkAlreadyConnected(@NotNull ISchemStackPin stackPin1, @NotNull ISchemStackPin stackPin2)
	{
		Set<IHighwaySchematic> highwaySchematicSet1 = stackPin1.getConnectedHighways();
		Set<IHighwaySchematic> highwaySchematicSet2 = stackPin2.getConnectedHighways();
		if (highwaySchematicSet1.isEmpty() || highwaySchematicSet2.isEmpty()) {
			return false;
		}

		for (IHighwaySchematic highwaySchematic : highwaySchematicSet1) {
			if (highwaySchematicSet2.contains(highwaySchematic)) {
				return true;
			}
		}
		return false;
	}

	private boolean checkAlreadyConnected(@NotNull IPin pin, @NotNull ISchemStackPin stackPin)
	{
		Set<IHighwaySchematic> highwaySchematicSet = stackPin.getConnectedHighways();
		if (highwaySchematicSet.isEmpty()) {
			return false;
		}
		for (IHighwaySchematic highwaySchematic : highwaySchematicSet) {
			for (chs.cof.logical.schem.IConductor conductor : pin.getConductors()) {
				if (highwaySchematic.getConductors().contains(conductor)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean checkPinsHadDiffertentOwner(@NotNull UnorderedPair<IAbstractPin> connectivityPinsToConnect)
	{
		return pinsHaveDifferentOwner(connectivityPinsToConnect.getObject1(), connectivityPinsToConnect.getObject2());
	}

	private boolean pinsHaveDifferentOwner(@NotNull IAbstractPin pin1, @NotNull IAbstractPin pin2)
	{
		IPinList pin1Owner = PinUtils.getRootOwnerPinList(pin1);
		IPinList pin2Owner = PinUtils.getRootOwnerPinList(pin2);
		if (pin1Owner == null || pin2Owner == null) {
			return false;
		}
		pin1Owner = LogicUtils.getTopLevelPinList(pin1Owner);
		pin2Owner = LogicUtils.getTopLevelPinList(pin2Owner);
		return !pin1Owner.equals(pin2Owner) && !getConnectedPinLists(pin1Owner).contains(pin2Owner);
	}
}