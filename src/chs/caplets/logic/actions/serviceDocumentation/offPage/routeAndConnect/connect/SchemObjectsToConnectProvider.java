package chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.connect;

import chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.ISchemObjectsToConnect;
import chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.RouteAndConnectFetchedObjectsCmd;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.ILogicSegmentContainer;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class provides the objects which are to be connected to the fetched conductor or highway schematic
 * <p>
 * The following algorithm is used
 * <p>
 * <p>
 * 1.If a segment container is connected on both ends, then add it to the auto route list
 * <p>
 * 2. Get single ended, double ended segment containers
 * <p>
 * 3.If there are more than 1 single ended segment container, find objects to connect for that using {@link
 * ObjectsToConnectProvider}. This is to find if any object in the current diagram is connectible with this object
 * <p>
 * 4.If all objects are not found then add other single ended segment container to the objects to be connected
 * <p>
 * 5.If no single ended found and there are some double ended segment containers found, then find objects to connect for
 * that using {@link ObjectsToConnectProvider} and add all double ended to the list of objects to be deleted
 */
public class SchemObjectsToConnectProvider
{

	private ObjectsToConnectWithConductorProvider forConductor;
	private ObjectsToConnectWithHighwayProvider forHighway;

	public SchemObjectsToConnectProvider(IProject project,
			List<ILogicDesign> designs, @NotNull ISchemDiagram diagram,
			Set<IAbstractSchemPin> pinsInSelection,
			Set<IAbstractSchemPin> fetchedPins, List<IConductor> conductorsInSelection)
	{
		forConductor =
				new ObjectsToConnectWithConductorProvider(project, designs, diagram, pinsInSelection, fetchedPins,
						conductorsInSelection);
		forHighway = new ObjectsToConnectWithHighwayProvider(project, designs, diagram, pinsInSelection, fetchedPins,
				conductorsInSelection);
	}

	public ISchemObjectsToConnect getSchemObjectsToConnectForConductor(Set<IConductor> fetchedConductors,
			chs.cof.logical.cable.IConductor conductorConn)
	{
		ObjectsToConnectForConductor objectsToConnect = new ObjectsToConnectForConductor();
		if (conductorConn instanceof INetConductor) {
			objectsToConnect = new ObjectsToConnectForNet();
		}
		forConductor(fetchedConductors, objectsToConnect);
		return objectsToConnect;
	}

	public ISchemObjectsToConnect getSchemObjectsToConnectForHighway(Set<IHighwaySchematic> fetchedHighways)
	{
		ObjectsToConnectForHighway objectsToConnect = new ObjectsToConnectForHighway();
		forHighways(fetchedHighways, objectsToConnect);
		return objectsToConnect;
	}

	private void forConductor(Set<IConductor> fetchedConductors, ObjectsToConnectForConductor objectsToConnect)
	{
		forSegmentContainer(fetchedConductors, forConductor, objectsToConnect);
	}

	private void forHighways(Set<IHighwaySchematic> fetchedHighways, ObjectsToConnectForHighway objectsToConnect)
	{
		forSegmentContainer(fetchedHighways, forHighway, objectsToConnect);
	}

	private <C extends ILogicSegmentContainer
			, P extends IAbstractSchemPin
			, V extends ILogicSegmentContainer
			, O extends SchemObjectsToConnect<C, P, V>> void forSegmentContainer(
			Set<C> fetchedSegmentContainers, ObjectsToConnectProvider<C, P, V, O> provider, O objectsToConnect)
	{
		Set<C> singleEndedSegmentContainers = new HashSet<>(fetchedSegmentContainers);
		Set<C> floatingSegmentContainers =
				fetchedSegmentContainers
						.stream()
						.filter(provider::floatingCheck)
						.collect(Collectors.toSet());
		Set<C> doubleEndedSegmentContainers =
				fetchedSegmentContainers
						.stream()
						.filter(provider::doubleEndedCheck)
						.collect(Collectors.toSet());
		findObjectsToConnectForSegmentContainer(singleEndedSegmentContainers, floatingSegmentContainers,
				doubleEndedSegmentContainers,
				provider, objectsToConnect);
	}

	private static <C extends ILogicSegmentContainer, P extends IAbstractSchemPin, V extends ILogicSegmentContainer, O extends SchemObjectsToConnect<C, P, V>>
	void findObjectsToConnectForSegmentContainer(Set<C> singleEnded, Set<C> floating, Set<C> doubleEnded,
			ObjectsToConnectProvider<C, P, V, O> provider, O objectsToConnect)
	{
//		objectsToConnect.addConductorsForAutoRoute(doubleEnded);
		singleEnded.removeAll(floating);
		singleEnded.removeAll(doubleEnded);
		long floatingSize = floating.size();
		int singleEndedSize = singleEnded.size();
		if (singleEndedSize > 0) {
			RouteAndConnectFetchedObjectsCmd.logMsg("Found single ended seg container to connect");
			objectsToConnect.addSegmentContainersToDelete(floating);
			Iterator<C> iterator = singleEnded.iterator();
			C segmentContainer = iterator.next();
			objectsToConnect.addSegmentContainerToConnect(segmentContainer);
			provider.addObjectsToConnect(segmentContainer, objectsToConnect);
			if (singleEndedSize > 1) {
				if (!objectsToConnect.foundObjectsToConnect()) {
					C otherFetchedSegmentContainer = iterator.next();
					if (provider.isNotALoop(segmentContainer, otherFetchedSegmentContainer)) {
						RouteAndConnectFetchedObjectsCmd.logMsg("Found another single ended seg container to connect");
						objectsToConnect.addSegmentContainerToConnect(otherFetchedSegmentContainer);
					}
					else {
						singleEnded
								.stream()
								.filter(provider::shouldSingleEndedConductorBeDeleted)
								.forEach(objectsToConnect::addSegmentContainerToDelete);
					}
				}
				else {
					final C next = iterator.next();
					objectsToConnect.addLeftOverSegmentContainer(next);
				}
			}
			else {
				if (!objectsToConnect.foundObjectsToConnect()) {
					if (provider.shouldSingleEndedConductorBeDeleted(segmentContainer)) {
						RouteAndConnectFetchedObjectsCmd.logMsg("Deleting the single ended seg container");
						objectsToConnect.addSegmentContainerToDelete(segmentContainer);
					}
				}
			}
		}
		else if (floatingSize >= 1) {
			Iterator<C> iterator = floating.iterator();
			C segmentContainer = iterator.next();
			provider.addObjectsToConnect(segmentContainer, objectsToConnect);
			if (provider.shouldFloatingConductorBeDeleted(segmentContainer)) {
				objectsToConnect.addSegmentContainerToDelete(segmentContainer);
			}
			while (iterator.hasNext()) {
				C next = iterator.next();
				if (provider.shouldFloatingConductorBeDeleted(next)) {
					RouteAndConnectFetchedObjectsCmd.logMsg("Found dangling seg container to connect");
					objectsToConnect.addSegmentContainerToDelete(next);
				}
			}
		}
	}
}
