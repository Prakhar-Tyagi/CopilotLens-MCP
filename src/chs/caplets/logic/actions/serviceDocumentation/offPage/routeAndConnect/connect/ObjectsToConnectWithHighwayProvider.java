package chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.connect;

import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConductorIterator;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.shared.ISharedUsage;
import chs.cof.project.IProject;
import chs.common.IUID;
import chs.common.IUIDObjectCollection;
import chs.common.UIDUtils;
import chs.utility.helpers.HighwayHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This provides the objects to connect a fetched schem conductor
 * <p>
 * The following algorithm is used, before each step we check if the objects to connect are found, if not only then we
 * proceed
 * <p>
 * 1. For the schematic stack pin representation of connectivity pins of the highway schem in the diagram, in fetched
 * stack pins , in the stack pins from other designs (in that order), the following 2 steps are done {@link
 * ObjectsToConnectWithHighwayProvider#addSchemPins(chs.cof.logical.schem.ILogicSegmentContainer, ILogicObject, Set,
 * SchemObjectsToConnect)}}
 * <p>
 * 1.1 If the probable stacked pin is not already connected to the highway schematic, then highway schematics connected
 * to the probable stack pin are checked. If one of those highway schematics is single ended and it is of same
 * connectivity as our original highway schematic, then that highway schematic is added to the objects to connect
 * <p>
 * 1.2 If not such highway schematic is found, then the probable stack pin is added to the objects to connect
 */
class ObjectsToConnectWithHighwayProvider
		extends ObjectsToConnectProvider<IHighwaySchematic, ISchemStackPin, IConductor, ObjectsToConnectForHighway>
{

	ObjectsToConnectWithHighwayProvider(IProject project,
			List<ILogicDesign> designs,
			ISchemDiagram diagram, Set<IAbstractSchemPin> pinsInSelection, Set<IAbstractSchemPin> fetchedPins,
			List<IConductor> conductorsInSelection)
	{
		super(project, designs, diagram, pinsInSelection, fetchedPins, conductorsInSelection);
	}

	void addObjectsToConnect(IHighwaySchematic segmentsContainer, ObjectsToConnectForHighway objectsToConnect)
	{
		IHighway connectivity = segmentsContainer.getConnectivity();
		addSchemPins(segmentsContainer, objectsToConnect,
				() -> getEndPinsInDiagram(segmentsContainer, connectivity),
				() -> getEndPinsInFetchedPins(connectivity),
				() -> getEndPinsFromAcrossDesignsInDiagram(connectivity));
		addOtherSCsToConnect(segmentsContainer, objectsToConnect);
	}

	private Set<IDiagramObject> getEndPinsInDiagram(IHighwaySchematic segmentsContainer, IHighway connectivity)
	{
		Set<IDiagramObject> endPinsFor = getEndPinsFor(connectivity, this::getConductorEndPinsInDiagram);
		endPinsFor.addAll(segmentsContainer.getStackPinsFromNodes());
		return endPinsFor;
	}

	private Set<IDiagramObject> getEndPinsInFetchedPins(IHighway connectivity)
	{
		return getEndPinsFor(connectivity, this::getConductorEndPinsInFetchedPins);
	}

	private Set<IDiagramObject> getEndPinsFromAcrossDesignsInDiagram(IHighway connectivity)
	{
		Set<IDiagramObject> endPinsFor =
				getEndPinsFor(connectivity, this::getConductorEndPinsFromAcrossDesignsInDiagram);
		endPinsFor.addAll(getHighwayEndPinsFromAcrossDesigns(connectivity));
		return endPinsFor;
	}

	private Set<IDiagramObject> getEndPinsFor(IHighway connectivity,
			Function<chs.cof.logical.cable.IConductor, Set<IDiagramObject>> provider)
	{
		IConductorIterator stackPinConductors = HighwayHelper.toStackPinConductors(connectivity);
		Set<IDiagramObject> endPins = new HashSet<>();
		while (stackPinConductors.hasNext()) {
			chs.cof.logical.cable.IConductor next = stackPinConductors.getNext();
			endPins.addAll(provider.apply(next));
		}
		return endPins;
	}

	@Override protected boolean shouldConnectBasedOnConnectivity(IHighwaySchematic sc, IConductor otherProbableSc)
	{
		return areConductorHighwaySchemRelated(otherProbableSc, sc);
	}

	@Override protected boolean isOtherSCConnectible(IConductor otherProbableSc)
	{
		return super.isOtherSCConnectible(otherProbableSc) && otherProbableSc.getNumberOfInterfacedHighways() == 0;
	}

	@Override protected ILogicObject getConnectivity(IHighwaySchematic segmentContainer)
	{
		return segmentContainer.getConnectivity();
	}

	@Override protected Set<? extends IAbstractPin> getConnectivity(ISchemStackPin pin)
	{
		return pin.getAllConnectivity();
	}

	@Nullable @Override protected ISchemStackPin getSchemPin(IDiagramObject object)
	{
		return ISchemStackPin.class.isInstance(object) ? ISchemStackPin.class.cast(object) : null;
	}

	@Override protected Collection<ISchemStackPin> getPins(IHighwaySchematic segmentContainer)
	{
		return UIDUtils.convertToUIDObjectCollection(
				segmentContainer.getConnectedStackPins(), ISchemStackPin.class);
	}

	@Override protected Collection<IHighwaySchematic> getConnectedSegmentContainers(ISchemStackPin pin)
	{
		return pin.getConnectedHighways();
	}

	@Override protected int getNumberPins(IHighwaySchematic segmentContainer)
	{
		return segmentContainer.getConnectedStackPins().size();
	}

	@Override protected int getNumberPinsOfOtherSC(IConductor segmentContainer)
	{
		return segmentContainer.getNumPins();
	}

	@Override protected Collection<IConductor> getOtherSegmentContainersToCheck()
	{
		Collection<IConductor> conds = new LinkedHashSet<>();
		conds.addAll(m_conductorsInSelection);
		conds.addAll(m_pinsInSelection
				.stream()
				.filter(IPin.class::isInstance)
				.map(IPin.class::cast)
				.map(IPin::getConductors)
				.flatMap(IUIDObjectCollection::stream)
				.collect(Collectors.toSet()));
		conds.addAll(m_diagram.getConductors());
		return conds;
	}

	protected Set<IDiagramObject> getHighwayEndPinsFromAcrossDesigns(
			IHighway connectivity)
	{
		Set<ISchemStackPin> allPins = new HashSet<>();
		Collection<ISharedUsage> usages = getUsages(connectivity);
		Set<ISchemStackPin> pinsAcrossDesigns = usages
				.stream()
				.map(ISharedUsage::getDiagramObjectUID)
				.map(IUID::getObject)
				.filter(Objects::nonNull)
				.filter(IHighwaySchematic.class::isInstance)
				.map(IHighwaySchematic.class::cast)
				.map(IHighwaySchematic::getStackPinsFromNodes)
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());
		allPins.addAll(pinsAcrossDesigns);
		Set<IDiagramObject> diagramPins = allPins
				.stream()
				.map(ISchemStackPin::getAllConnectivity)
				.flatMap(Set::stream)
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

	boolean floatingCheck(IHighwaySchematic c)
	{
		return c.getConnectedStackPins().isEmpty() && c.getConductors().isEmpty();
	}

	boolean doubleEndedCheck(IHighwaySchematic c)
	{
		return c.getConnectedStackPins().size() >= 2 ||
				(c.getConnectedStackPins().size() >= 1 && !c.getConductors().isEmpty());
	}
}
