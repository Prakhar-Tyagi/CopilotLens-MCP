package chs.caplets.logic.actions.serviceDocumentation.delete;

import chs.cof.drawplus.IDiagramObject;
import chs.common.IUIDObject;

import java.util.HashSet;
import java.util.Set;

class SelectionObjects
{

	private Set<IUIDObject> m_deletables;
	private Set<IDiagramObject> m_notFetched;
	private Set<IDiagramObject> m_lastInstances;
	private Set<IDiagramObject> m_deleteLeadsToConnChange;
	private Set<IDiagramObject> m_notDeletableDueToOtherReasons;

	SelectionObjects()
	{
		m_deletables = new HashSet<>();
		m_notFetched = new HashSet<>();
		m_lastInstances = new HashSet<>();
		m_deleteLeadsToConnChange = new HashSet<>();
		m_notDeletableDueToOtherReasons = new HashSet<>();
	}

	Set<IUIDObject> getDeletables()
	{
		return m_deletables;
	}

	Set<IDiagramObject> getNotFetchedObjects()
	{
		return m_notFetched;
	}

	Set<IDiagramObject> getLastInstances()
	{
		return m_lastInstances;
	}

	Set<IDiagramObject> getObjectsWhoseDeleteLeadsToConnectivityChange()
	{
		return m_deleteLeadsToConnChange;
	}

	Set<IDiagramObject> getNotDeletablesDueToOtherReasons()
	{
		return m_notDeletableDueToOtherReasons;
	}

	void addToNotDeletablesDueToOtherReasons(IDiagramObject object)
	{
		m_notDeletableDueToOtherReasons.add(object);
	}

	void addToDeletables(Set<IUIDObject> objects)
	{
		m_deletables.addAll(objects);
	}

	void addToNotFetched(IDiagramObject object)
	{
		m_notFetched.add(object);
	}

	void addToLastInstances(IDiagramObject object)
	{
		m_lastInstances.add(object);
	}

	void addToDeleteLeadsToConnectivityChange(IDiagramObject object)
	{
		m_deleteLeadsToConnChange.add(object);
	}
}
