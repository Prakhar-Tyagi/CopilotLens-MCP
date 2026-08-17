package chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.connect;

import chs.caplets.logic.actions.serviceDocumentation.offPage.FetchOffPageContentHelper;
import chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.RouteAndConnectFetchedObjectsCmd;
import chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.edge.SchemConductorEdges;
import chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.edge.SchemConductorEdgesHelper;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IHighwayConductor;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.ILogicSegment;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.common.IUIDObjectCollection;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This provides the objects to connect a fetched schem conductor
 * <p>
 * The following algorithm is used, before each step we check if the objects to connect are found, if not only then we
 * proceed
 * <p>
 * 1. For the schematic representation of connectivity pins of the conductor in the diagram, in fetched pins, in the
 * pins from other designs (in that order), the following 2 steps are done {@link ObjectsToConnectWithConductorProvider#addSchemPins(chs.cof.logical.schem.ILogicSegmentContainer,
 * ILogicObject, Set, SchemObjectsToConnect)}
 * <p>
 * 1.1 If the probable pin is not already connected to the conductor, then conductors connected to the probable pin are
 * checked. If one of those conductors is single ended and it is of same connectivity as our original schem conductor,
 * then that conductor is added to the objects to connect
 * <p>
 * 1.2 If not such conductor is found, then the probable pin is added to the objects to connect
 * <p>
 * 2. For the other schem instances of the conductor in the diagram, if the number of pins connected of that instance
 * are 0 or 1, then it is added to the objects to connect. This case arises when the instance is associated with a
 * highway {@link ObjectsToConnectWithConductorProvider#addSchemConductorsToConnect(IConductor,
 * ObjectsToConnectForConductor)}
 * <p>
 * 3. Finally if the conductor is a highway conductor, then we look for highway schems in the diagram whose connectivity
 * is same as the conductors and add it to the objects to connect {@link ObjectsToConnectWithConductorProvider#addOtherSCsToConnect(IConductor,
 * ObjectsToConnectForConductor)}
 */
class ObjectsToConnectWithConductorProvider
		extends ObjectsToConnectProvider<IConductor, IPin, IHighwaySchematic, ObjectsToConnectForConductor>
{

	ObjectsToConnectWithConductorProvider(IProject project,
			List<ILogicDesign> designs,
			ISchemDiagram diagram, Set<IAbstractSchemPin> pinsInSelection, Set<IAbstractSchemPin> fetchedPins,
			List<IConductor> conductorsInSelection)
	{
		super(project, designs, diagram, pinsInSelection, fetchedPins, conductorsInSelection);
	}

	void addObjectsToConnect(IConductor segmentsContainer, ObjectsToConnectForConductor objectsToConnect)
	{
		chs.cof.logical.cable.IConductor connectivity = segmentsContainer.getConnectivity();
		//if the conductor has center stripped, then look for conductors to connect first.
		if (connectivity instanceof IWireConductor &&
				!((IWireConductor) connectivity).getCenterStripSplicesAsSet().isEmpty()) {
			addSchemConductorsToConnect(segmentsContainer, objectsToConnect);
			addSchemPins(segmentsContainer, objectsToConnect, () -> getConductorEndPinsInDiagram(connectivity),
					() -> getConductorEndPinsInFetchedPins(connectivity),
					() -> getConductorEndPinsFromAcrossDesignsInDiagram(connectivity));
		}
		else {
			addSchemPins(segmentsContainer, objectsToConnect, () -> getConductorEndPinsInDiagram(connectivity),
					() -> getConductorEndPinsInFetchedPins(connectivity),
					() -> getConductorEndPinsFromAcrossDesignsInDiagram(connectivity));
			addSchemConductorsToConnect(segmentsContainer, objectsToConnect);
		}
		addOtherSCsToConnect(segmentsContainer, objectsToConnect);
	}

//	boolean shouldBeDeleted(IConductor floatingConductor)
//	{
//		return floatingConductor.getConnectivity().getMulticore() == null;
//	}

	@Override boolean shouldSingleEndedConductorBeDeleted(IConductor singleEndedConductor)
	{
		ILogicObject connectivity = getConnectivity(singleEndedConductor);
		return connectivity instanceof IShieldConductor && singleEndedConductor.getHookup() != null;
	}

	@Override protected ILogicObject getConnectivity(IConductor segmentContainer)
	{
		return segmentContainer.getConnectivity();
	}

	@Override protected Set<? extends IAbstractPin> getConnectivity(IPin pin)
	{
		return Collections.singleton(pin.getConnectivity());
	}

	@Nullable @Override protected IPin getSchemPin(IDiagramObject object)
	{
		return IPin.class.isInstance(object) ? IPin.class.cast(object) : null;
	}

	@Override protected Collection<IPin> getPins(IConductor segmentContainer)
	{
		return segmentContainer.getPins();
	}

	@Override protected Collection<IConductor> getConnectedSegmentContainers(IPin pin)
	{
		return pin.getConductors();
	}

	@Override protected int getNumberPins(IConductor segmentContainer)
	{
		return segmentContainer.getNumPins();
	}

	@Override protected int getNumberPinsOfOtherSC(IHighwaySchematic segmentContainer)
	{
		return segmentContainer.getConnectedStackPins().size();
	}

	@Override protected Collection<IHighwaySchematic> getOtherSegmentContainersToCheck()
	{
		return m_diagram.getHighways();
	}

	private void addSchemConductorsToConnect(IConductor conductor, ObjectsToConnectForConductor objectsToConnect)
	{
		if (objectsToConnect.foundObjectsToConnect()) {
			return;
		}
		RouteAndConnectFetchedObjectsCmd.logMsg("Checking in other schem conductors");
		chs.cof.logical.cable.IConductor connectivity = conductor.getConnectivity();
		IUIDObjectCollection<IConductor> conductors = m_diagram.getConductors();
		conductors.forEach(schem -> {
			addSchemConductorsToConnect(conductor, connectivity, schem, objectsToConnect);
		});
	}

	private void addSchemConductorsToConnect(IConductor original, chs.cof.logical.cable.IConductor connectivity,
			IConductor probable, ObjectsToConnectForConductor objectsToConnect)
	{
		if (objectsToConnect.foundObjectsToConnect()) {
			return;
		}
		if (probable.getConnectivity().equals(connectivity) && !probable.equals(original)) {
			//if the existing schem conductor is attached to a highway then this condition arises
			if ((probable.getNumPins() == 0) && original.getNumPins() == 1) {
				RouteAndConnectFetchedObjectsCmd.logMsg("Found conductor attached to a highway to connect");
				objectsToConnect.addSegmentContainerToConnect(probable);
			}
			else if (singleEndedCheck(probable) && singleEndedCheck(original) && isNotALoop(probable, original)) {
				RouteAndConnectFetchedObjectsCmd.logMsg("Found conductor attached to a highway to connect");
				objectsToConnect.addSegmentContainerToConnect(probable);
			}
		}
	}

	protected void addOtherSCsToConnect(IConductor conductor, ObjectsToConnectForConductor objectsToConnect)
	{
		super.addOtherSCsToConnect(conductor, objectsToConnect);
		if (objectsToConnect.foundObjectsToConnect()) {
			return;
		}
		RouteAndConnectFetchedObjectsCmd.logMsg("Checking in other kind of schem conductors for conductor");
		chs.cof.logical.cable.IConductor connectivity = conductor.getConnectivity();
		IUIDObjectCollection<IHighwaySchematic> highwaySchems = m_diagram.getHighways();
		if (IHighwayConductor.class.isInstance(connectivity)) {
			IHighwayConductor highwayConductor = IHighwayConductor.class.cast(connectivity);
			highwaySchems
					.stream()
					.forEach(highwaySchematic -> {
						getHighwaySchematicToConnectByMatchingConductorConn(highwayConductor, objectsToConnect,
								highwaySchematic);
					});
		}
	}

	@Override protected boolean shouldConnectBasedOnConnectivity(IConductor sc, IHighwaySchematic otherProbableSc)
	{
		return areConductorHighwaySchemRelated(sc, otherProbableSc);
	}

	private void getHighwaySchematicToConnectByMatchingConductorConn(IHighwayConductor connectivity,
			ObjectsToConnectForConductor objectsToConnect, IHighwaySchematic highwaySchematic)
	{
		if (objectsToConnect.foundObjectsToConnect()) {
			return;
		}
		highwaySchematic.getStackedPinConductors()
				.forEach(spCond -> {
					getHighwaySchematicToConnectByMatchingConductorConn(connectivity, objectsToConnect,
							highwaySchematic, spCond);
				});
	}

	private void getHighwaySchematicToConnectByMatchingConductorConn(IHighwayConductor connectivity,
			ObjectsToConnectForConductor objectsToConnect, IHighwaySchematic highwaySchematic,
			chs.cof.logical.cable.IConductor spCond)
	{
		if (objectsToConnect.foundObjectsToConnect()) {
			return;
		}
		if (spCond.equals(connectivity)) {
			RouteAndConnectFetchedObjectsCmd
					.logMsg("Found highway schem to connect based on matching conductor connectivity");
			objectsToConnect.addSegmentContainerOfOtherType(highwaySchematic);
		}
	}

	boolean floatingCheck(IConductor c)
	{
		int numPins = c.getPins().size();
		boolean isFloating = numPins == 0 && c.getNumberOfInterfacedHighways() == 0;
		chs.cof.logical.cable.IConductor connectivity = c.getConnectivity();
		if (connectivity instanceof IShieldConductor) {
			boolean forShield = c.getHookup() == null;
			return isFloating && forShield;
		}
		//----------0----------
//		if (numPins == 1 && FetchOffPageContentHelper.isCenterStrippedConductor(c)) {
//			return true;
//		}
		return isFloating;
	}

	boolean doubleEndedCheck(IConductor c)
	{
		int numPins = c.getPins().size();
		boolean isDoubleEnded = numPins >= 2 || (numPins >= 1 && c.getNumberOfInterfacedHighways() != 0);
		chs.cof.logical.cable.IConductor connectivity = c.getConnectivity();
		if (connectivity instanceof IShieldConductor) {
			boolean forShield = numPins >= 1 && c.getHookup() != null;
			return isDoubleEnded || forShield;
		}
		//0----------0----------
		if (numPins == 2 && FetchOffPageContentHelper.isCenterStrippedConductor(c)) {
			isDoubleEnded = false;
		}
		if (connectivity instanceof INetConductor) {
			Set<ILogicSegment> segments = c
					.getSegments()
					.stream()
					.filter(seg -> seg instanceof ILogicSegment)
					.map(seg -> (ILogicSegment) seg)
					.collect(Collectors.toSet());
			SchemConductorEdges schemConductorEdges = new SchemConductorEdgesHelper().getEdgesOfSameConductor(segments);
			return schemConductorEdges != null && schemConductorEdges.getStart() == null &&
					schemConductorEdges.getEnd() == null;
		}
		return isDoubleEnded;
	}

	protected boolean isConductorConnectible(IConductor schemConductorToConnect)
	{
		ILogicObject connectivity = getConnectivity(schemConductorToConnect);
		if (connectivity instanceof IShieldConductor) {
			return getNumberPins(schemConductorToConnect) == 0;
		}
		return super.isConductorConnectible(schemConductorToConnect);
	}
}
