package chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.connect;

import chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.RouteAndConnectFetchedObjectsCmd;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IGenericSplice;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.cable.IHighwayConductor;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.ILogicSegmentContainer;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.IProjectSharedUsageView;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedUsage;
import chs.cof.project.IProject;
import chs.common.IDesignContainer;
import chs.common.IUID;
import chs.common.IUIDObjectCollection;
import chs.utility.helpers.HighwayHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

abstract class ObjectsToConnectProvider<C extends ILogicSegmentContainer, P extends IAbstractSchemPin, V extends ILogicSegmentContainer, O extends SchemObjectsToConnect<C, P, V>>
{

	protected final IProject m_project;
	protected final List<ILogicDesign> m_designs;
	protected final ISchemDiagram m_diagram;
	private final Set<IAbstractSchemPin> m_fetchedPins;
	protected final Set<IAbstractSchemPin> m_pinsInSelection;
	protected List<chs.cof.logical.schem.IConductor> m_conductorsInSelection;

	ObjectsToConnectProvider(IProject project, List<ILogicDesign> designs, ISchemDiagram diagram,
			Set<IAbstractSchemPin> pinsInSelection,
			Set<IAbstractSchemPin> fetchedPins, List<chs.cof.logical.schem.IConductor> conductorsInSelection)
	{
		m_project = project;
		m_designs = designs;
		m_diagram = diagram;
		m_pinsInSelection = pinsInSelection;
		m_fetchedPins = fetchedPins;
		m_conductorsInSelection = conductorsInSelection;
	}

	abstract void addObjectsToConnect(C segmentsContainer, O objectsToConnect);

	protected void addSchemPins(C segmentsContainer, O objectsToConnect,
			Supplier<Set<IDiagramObject>> probablePinsInDiagramSupplier,
			Supplier<Set<IDiagramObject>> probablePinsInFetchedPinsSupplier,
			Supplier<Set<IDiagramObject>> probablePinsAcrossDesignsSupplier)
	{
		Set<IDiagramObject> probablePinsInDiagram = probablePinsInDiagramSupplier.get();
		Set<IDiagramObject> probablePinsInFetchedPins = probablePinsInFetchedPinsSupplier.get();
		probablePinsInDiagram.removeAll(probablePinsInFetchedPins);
		ILogicObject connectivity = getConnectivity(segmentsContainer);
		RouteAndConnectFetchedObjectsCmd.logMsg("Checking in pins in diagram");
		addSchemPins(segmentsContainer, connectivity, objectsToConnect, probablePinsInDiagram);
		if (objectsToConnect.foundObjectsToConnect()) {
			return;
		}
		RouteAndConnectFetchedObjectsCmd.logMsg("Checking in fetched pins");
		addSchemPins(segmentsContainer, connectivity, objectsToConnect, probablePinsInFetchedPins);
		if (objectsToConnect.foundObjectsToConnect()) {
			return;
		}
		RouteAndConnectFetchedObjectsCmd.logMsg("Checking in pins from other diagrams");
		Set<IDiagramObject> probablePinsAcrossDesigns = probablePinsAcrossDesignsSupplier.get();
		addSchemPins(segmentsContainer, connectivity, objectsToConnect, probablePinsAcrossDesigns);
	}

	private void addSchemPins(C segmentsContainer, ILogicObject connectivity, O objectsToConnect,
			Set<IDiagramObject> probablePins)
	{
		Set<IDiagramObject> probablePinsInSelection = new HashSet<IDiagramObject>(m_pinsInSelection);
		probablePinsInSelection.retainAll(probablePins);
		probablePins.removeAll(probablePinsInSelection);
		if (objectsToConnect.foundObjectsToConnect()) {
			return;
		}
		RouteAndConnectFetchedObjectsCmd.logMsg("Checking in selections");
		addSchemPins(segmentsContainer, connectivity, probablePinsInSelection, objectsToConnect);
		if (objectsToConnect.foundObjectsToConnect()) {
			return;
		}
		RouteAndConnectFetchedObjectsCmd.logMsg("Checking in pins other than selections");
		addSchemPins(segmentsContainer, connectivity, probablePins, objectsToConnect);
	}

	protected void addSchemPins(C segmentContainer, ILogicObject connectivity,
			Set<IDiagramObject> pinsToConnectProbables, O objectsToConnect)
	{
		Collection<P> schemPinsOnConductor = getPins(segmentContainer);
		Set<IAbstractPin> connPinsOnConductor = getConnectivityPins(schemPinsOnConductor);
		for (IDiagramObject object : pinsToConnectProbables) {
			if (objectsToConnect.foundObjectsToConnect()) {
				return;
			}
			P pinToConnectProbable = getSchemPin(object);
			if (pinToConnectProbable != null) {
				Set<? extends IAbstractPin> probablePinConnectivity = getConnectivity(pinToConnectProbable);
				if (schemPinsOnConductor.contains(pinToConnectProbable)) {
					continue;
				}
				if (connPinsOnConductor.containsAll(probablePinConnectivity)) {
					continue;
				}
				addPinOrConductor(segmentContainer, pinToConnectProbable, connectivity, objectsToConnect);
			}
		}
	}

	protected void addPinOrConductor(C conductor, P pinToConnectProbable, ILogicObject connectivity, O objectsToConnect)
	{
		Collection<C> conductors = getConnectedSegmentContainers(pinToConnectProbable);
		List<C> schemConductorsToConnect = conductors
				.stream()
				.filter(sc -> getConnectivity(sc).equals(connectivity))
				.filter(sc -> !sc.equals(conductor))
				.collect(Collectors.toList());
		List<C> sameConductors = conductors
				.stream()
				.filter(sc -> getConnectivity(sc).equals(connectivity))
				.filter(sc -> sc.equals(conductor))
				.collect(Collectors.toList());
		int size = schemConductorsToConnect.size();
		if (size > 0) {
			for (C schemConductorToConnect : schemConductorsToConnect) {
				if (isConductorConnectible(schemConductorToConnect)) {
					RouteAndConnectFetchedObjectsCmd.logMsg("Found seg container to connect");
					objectsToConnect.addSegmentContainerToConnect(schemConductorToConnect);
					break;
				}
			}
			if (!objectsToConnect.foundObjectsToConnect()) {
				//if the conductor is found and both ends are connected and the pin is a splice pin means this is center stripepd
				Set<IAbstractPin> connectivityPins = getConnectivityPins(Collections.singleton(pinToConnectProbable));
				for (IAbstractPin abstractPin : connectivityPins) {
					if (abstractPin.getOwner() instanceof IGenericSplice) {
						addSplicePin(pinToConnectProbable, conductor, objectsToConnect);
						break;
					}
				}
			}
		}
		else if(sameConductors.isEmpty()){
			RouteAndConnectFetchedObjectsCmd.logMsg("Found pin to connect");
			objectsToConnect.addPinToConnect(pinToConnectProbable);
		}
	}

	protected boolean isConductorConnectible(C schemConductorToConnect)
	{
		return singleEndedCheck(schemConductorToConnect);
	}

	private void addSplicePin(P pinToConnectProbable, C conductor, O objectsToConnect)
	{
		Collection<C> connectedSegmentContainers = getConnectedSegmentContainers(pinToConnectProbable);
		boolean foundMatch = false;
		//if there is a single ended conductor already connected to this splice pin, then do not add this conductor again
		for (C otherSegmentContainer : connectedSegmentContainers) {
			if (getConnectivity(otherSegmentContainer).equals(getConnectivity(conductor))) {
				foundMatch = true;
				if (singleEndedCheck(otherSegmentContainer)) {
					return;
				}
			}
		}
		//this is for the center spliced conductor
		if (!foundMatch) {
			objectsToConnect.addPinToConnect(pinToConnectProbable);
		}
	}

	@NotNull private Set<IAbstractPin> getConnectivityPins(Collection<P> schemPinsOnConductor)
	{
		return schemPinsOnConductor
				.stream()
				.map(this::getConnectivity)
				.flatMap(Set::stream)
				.collect(Collectors.toSet());
	}

	protected abstract ILogicObject getConnectivity(C segmentContainer);

	protected abstract Set<? extends IAbstractPin> getConnectivity(P pin);

	@Nullable protected abstract P getSchemPin(IDiagramObject object);

	protected abstract Collection<P> getPins(C segmentContainer);

	protected abstract Collection<C> getConnectedSegmentContainers(P pin);

	protected abstract int getNumberPins(C segmentContainer);

	protected abstract int getNumberPinsOfOtherSC(V segmentContainer);

	protected Set<IDiagramObject> getConductorEndPinsInDiagram(IConductor connectivity)
	{
		Set<IAbstractPin> allPins = connectivity.getAllPins();
		ILogicDesign design = connectivity.getLogicDesign();
		if (design != null) {
			IDesignWideUsageMgr designWideUsageMgr = design.getDesignWideUsageMgr();
			Set<IDiagramObject> diagramPins = allPins
					.stream()
					.flatMap(p -> designWideUsageMgr.getUsages(p).stream())
					.filter(usage -> usage.getDiagram().equals(m_diagram))
					.map(usage -> usage.getDiagramObject())
					.collect(Collectors.toSet());
			return diagramPins;
		}
		return Collections.emptySet();
	}

	protected Set<IDiagramObject> getConductorEndPinsInFetchedPins(IConductor connectivity)
	{
		Set<IAbstractPin> allPins = connectivity.getAllPins();
		return m_fetchedPins
				.stream()
				.filter(pin -> {
					return pin instanceof IConnectivityRef &&
							allPins.contains(((IConnectivityRef) pin).getConnectivity());
				})
				.collect(Collectors.toSet());
	}

	protected Set<IDiagramObject> getConductorEndPinsFromAcrossDesignsInDiagram(
			IConductor connectivity)
	{
		Set<IAbstractPin> allPins = new HashSet<>(connectivity.getAllPins());
		Set<IAbstractPin> pinsAcrossDesigns = getUsages(connectivity)
				.stream()
				.map(ISharedUsage::getLogicObject)
				.filter(Objects::nonNull)
				.map(IConductor.class::cast)
				.map(IConductor::getAllPins)
				.flatMap(Set::stream)
				.collect(Collectors.toSet());
		allPins.addAll(pinsAcrossDesigns);
		Set<IDiagramObject> diagramPins = allPins
				.stream()
				.map(this::getUsages)
				.flatMap(Collection::stream)
				.filter(usage -> usage.getDiagramUID().equals(m_diagram.getUID()))
				.map(ISharedUsage::getDiagramObjectUID)
				.filter(Objects::nonNull)
				.map(IUID::getObject)
				.filter(Objects::nonNull)
				.map(IDiagramObject.class::cast)
				.collect(Collectors.toSet());
		return diagramPins;
	}

	protected abstract Collection<V> getOtherSegmentContainersToCheck();

	protected boolean areConductorHighwaySchemRelated(
			chs.cof.logical.schem.IConductor sc, IHighwaySchematic otherProbableSc)
	{
		IConductor connectivity = sc.getConnectivity();
		if (IHighwayConductor.class.isInstance(connectivity)) {
			IHighwayConductor highwayConductor = IHighwayConductor.class.cast(connectivity);
			IHighway otherConnectivity = otherProbableSc.getConnectivity();
			return HighwayHelper.isSameSharedHighwayInterface(highwayConductor, otherConnectivity);
		}
		return false;
	}

	protected void addOtherSCsToConnect(C conductor, O objectsToConnect)
	{
		if (objectsToConnect.foundObjectsToConnect()) {
			return;
		}
		RouteAndConnectFetchedObjectsCmd.logMsg("Checking in other kind of schem conductors");
		Collection<V> otherProbableSCs = getOtherSegmentContainersToCheck();
		addOtherSCToConnectBasedOnConnectivity(conductor, otherProbableSCs, objectsToConnect);
	}

	private void addOtherSCToConnectBasedOnConnectivity(C sc, Collection<V> otherProbableSCs,
			O objectsToConnect)
	{
		if (objectsToConnect.foundObjectsToConnect()) {
			return;
		}
		otherProbableSCs.forEach(schem -> {
			addOtherSCToConnectBasedOnConnectivity(sc, schem, objectsToConnect);
		});
	}

	private void addOtherSCToConnectBasedOnConnectivity(C sc, V otherProbableSc, O objectsToConnect)
	{
		if (objectsToConnect.foundObjectsToConnect()) {
			return;
		}
		if (shouldConnectBasedOnConnectivity(sc, otherProbableSc)) {
			int numberPins = getNumberPins(sc);
			if (isOtherSCConnectible(otherProbableSc) && (numberPins == 1 || numberPins == 0)) {
				RouteAndConnectFetchedObjectsCmd.logMsg("Found other sc to connect based on connectivity");
				objectsToConnect.addSegmentContainerOfOtherType(otherProbableSc);
			}
		}
	}

	protected boolean isOtherSCConnectible(V otherProbableSc)
	{
		return getNumberPinsOfOtherSC(otherProbableSc) == 1;
	}

	protected abstract boolean shouldConnectBasedOnConnectivity(C sc, V otherProbableSc);

	boolean shouldFloatingConductorBeDeleted(C floatingConductor)
	{
		return floatingConductor != null;
	}

	boolean shouldSingleEndedConductorBeDeleted(C singleEndedConductor)
	{
		return false;
	}

	/**
	 * checks if the end pins of the single ended segment containers are same, connecvity wise also
	 *
	 * @param c1 single ended segment container 1
	 * @param c2 single ended segment container 2
	 *
	 * @return false if connecting these two will result in a loop, true otherwise
	 */
	boolean isNotALoop(C c1, C c2)
	{
		Collection<P> pins1 = getPins(c1);
		Iterator<P> pins1Iter = pins1.iterator();
		if (!singleEndedCheck(c1) || !pins1Iter.hasNext()) {
			return false;
		}
		P connectible1 = pins1Iter.next();

		Collection<P> pins2 = getPins(c2);
		Iterator<P> pins2Iter = pins2.iterator();
		//if the conductor is single ended and it does not have any pins means it is a highway through a wire, which will not create a loop
		if (singleEndedCheck(c2) && !pins2Iter.hasNext()) {
			return true;
		}
		if (!singleEndedCheck(c2) || !pins2Iter.hasNext()) {
			return false;
		}
		P connectible2 = pins2Iter.next();
		if (connectible1.equals(connectible2)) {
			return false;
		}
		Set<? extends IAbstractPin> connectivity1 = getConnectivity(connectible1);
		Set<? extends IAbstractPin> connectivity2 = getConnectivity(connectible2);
		return !connectivity1.containsAll(connectivity2);
	}

	protected Collection<ISharedUsage> getUsages(ILogicObject connectivity)
	{
		return getSharedUsages(connectivity, m_project, m_designs);
	}

	private static Collection<ISharedUsage> getSharedUsages(ILogicObject connectivity, IProject project,
			List<ILogicDesign> designs)
	{
		IProjectSharedUsageView suView = project.getSharedUsageView();
		Set<IUID> designUIDs = designs
				.stream()
				.map(IDesignContainer::getUID)
				.collect(Collectors.toSet());
		ISharedObject sharedObject = connectivity.getSharedObject();
		if (sharedObject != null) {
			return suView
					.getSharedUsageInfo(sharedObject, designUIDs)
					.getUsages();
		}
		return Collections.emptyList();
	}

	abstract boolean floatingCheck(C c);

	abstract boolean doubleEndedCheck(C c);

	boolean singleEndedCheck(C c)
	{
		return !floatingCheck(c) && !doubleEndedCheck(c);
	}
}
