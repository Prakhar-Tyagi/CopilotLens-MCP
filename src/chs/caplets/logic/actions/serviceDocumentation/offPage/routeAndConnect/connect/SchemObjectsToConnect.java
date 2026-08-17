package chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.connect;

import chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.ISchemObjectsConnector;
import chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.ISchemObjectsToConnect;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISchemStackPin;
import chs.common.IUIDObject;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

abstract class SchemObjectsToConnect<C extends IUIDObject, P extends IAbstractSchemPin, V extends IUIDObject> implements
		ISchemObjectsToConnect
{

	protected Set<IAbstractPin> m_connectivityPins;
	//	private Set<ILogicSegmentContainer> m_conductorsForAutoRoute;
	protected Set<C> m_segmentContainers;
	protected Set<V> m_otherSCs;
	private Set<C> m_segmentContainerToDelete;
	protected Set<P> m_pins;
	protected Set<C> m_leftOverSegmentContainers;

	SchemObjectsToConnect()
	{
		m_connectivityPins = new HashSet<>();
//		m_conductorsForAutoRoute = new HashSet<>();
		m_segmentContainers = new HashSet<>();
		m_otherSCs = new LinkedHashSet<>();
		m_segmentContainerToDelete = new HashSet<>();
		m_pins = new HashSet<>();
		m_leftOverSegmentContainers = new LinkedHashSet<>();
	}

	void addSegmentContainerOfOtherType(V scOfOtherType)
	{
		m_otherSCs.add(scOfOtherType);
	}

	public Set<? extends IUIDObject> getObjectsToDelete()
	{
		return m_segmentContainerToDelete;
	}

	@Override public Set<? extends IUIDObject> getObjectsToIgnore()
	{
		return m_otherSCs;
	}

	public void addForAutoRoute()
	{
//		ConductorRouteAction.getInstance().addConductorsForRoute(m_conductorsForAutoRoute, true);
	}

	void addSegmentContainerToConnect(C conductor)
	{
		m_segmentContainers.add(conductor);
		m_segmentContainerToDelete.remove(conductor);
	}

	void addPinToConnect(P pin)
	{
		if (isPinAlreadyFound(pin)) {
			return;
		}
		m_pins.add(pin);
		addConnectivityPin(pin);
	}

	protected abstract void addConnectivityPin(P pin);

	void addSegmentContainersToDelete(Set<C> floatingConductors)
	{
		m_segmentContainerToDelete.addAll(floatingConductors);
		m_segmentContainers.removeAll(floatingConductors);
	}

	void addSegmentContainerToDelete(C floatingConductor)
	{
		m_segmentContainerToDelete.add(floatingConductor);
		m_segmentContainers.remove(floatingConductor);
	}

	boolean isEmpty()
	{
		return m_segmentContainers.isEmpty() && m_pins.isEmpty();
	}

	boolean foundObjectsToConnect()
	{
		int segmentContainersSize = m_segmentContainers.size();
		int otherSCs = m_otherSCs.size();
		int pinsCount = m_pins.size();
		if (segmentContainersSize == 1 && otherSCs >= 1) {
			return true;
		}
		if (segmentContainersSize == 2 && pinsCount == 0) {
			return true;
		}
		if (segmentContainersSize == 1 && pinsCount == 1) {
			return true;
		}
//		return segmentContainersSize == 0 && (pinsCount == 2 || pinsCount == 1);
		return segmentContainersSize == 0 && (pinsCount == 2);
	}

//	void addConductorsForAutoRoute(Set<? extends ILogicSegmentContainer> doubleEndedConductors)
//	{
//		m_conductorsForAutoRoute.addAll(doubleEndedConductors);
//	}

	protected boolean isPinAlreadyFound(IAbstractSchemPin schemPin)
	{
		if (IPin.class.isInstance(schemPin)) {
			IPin pin = IPin.class.cast(schemPin);
			return m_connectivityPins.contains(pin.getConnectivity());
		}
		if (ISchemStackPin.class.isInstance(schemPin)) {
			ISchemStackPin pin = ISchemStackPin.class.cast(schemPin);
			return m_connectivityPins.containsAll(pin.getAllConnectivity());
		}
		return false;
	}

	public boolean connect(ISchemObjectsConnector schemObjectsConnector)
	{
		int scSize = m_segmentContainers.size();
		int pinSize = m_pins.size();
		int otherSCSize = m_otherSCs.size();
		if (scSize == 0 && pinSize == 0 && otherSCSize == 0) {
			return true;
		}
		Iterator<C> scIterator = m_segmentContainers.iterator();
		Iterator<P> pinIterator = m_pins.iterator();
		Iterator<V> otherScIterator = m_otherSCs.iterator();
		if (scSize == 2 && pinSize == 0) {
			C schem1 = scIterator.next();
			C schem2 = scIterator.next();
			return connectSegmentContainers(schem1, schem2, schemObjectsConnector);
		}
		if (scSize == 1 && pinSize == 1) {
			C schem1 = scIterator.next();
			P pin = pinIterator.next();
			return connectSegmentContainerAndPin(schem1, pin, schemObjectsConnector);
		}
		if (scSize == 0 && pinSize == 2) {
			P pin1 = pinIterator.next();
			P pin2 = pinIterator.next();
			return schemObjectsConnector.connectSchemPins(pin1, pin2);
		}
		if (otherSCSize >= 1 && scSize == 1) {
			C schem1 = scIterator.next();
			V schem2 = otherScIterator.next();
			return connectSegmentContainersOfDifferentTypes(schem1, schem2, schemObjectsConnector);
		}
		if (pinSize == 1 && otherSCSize >= 1) {
			P pin1 = pinIterator.next();
			V schem2 = otherScIterator.next();
			return connectSegmentContainerBetweenPinAndOtherSCType(pin1, schem2, schemObjectsConnector);
		}
		if (scSize == 0 && pinSize == 1) {
			P pin1 = pinIterator.next();
			return schemObjectsConnector.connectSchemPins(pin1, null);
		}
		return false;
	}

	protected abstract boolean connectSegmentContainerBetweenPinAndOtherSCType(P pin1, V schem2,
			ISchemObjectsConnector schemObjectsConnector);

	protected abstract boolean connectSegmentContainers(C schem1, C schem2,
			ISchemObjectsConnector schemObjectsConnector);

	protected abstract boolean connectSegmentContainerAndPin(C schem1, P schem2,
			ISchemObjectsConnector schemObjectsConnector);

	protected abstract boolean connectSegmentContainersOfDifferentTypes(C schem1, V schem2,
			ISchemObjectsConnector schemObjectsConnector);

	public void addLeftOverSegmentContainer(C next)
	{
		m_leftOverSegmentContainers.add(next);
	}

	@NotNull
	@Override public Set<? extends IUIDObject> getLeftOverObjects()
	{
		return m_leftOverSegmentContainers;
	}
}
