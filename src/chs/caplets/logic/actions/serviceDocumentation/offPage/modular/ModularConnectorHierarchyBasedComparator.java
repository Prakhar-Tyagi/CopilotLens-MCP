/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage.modular;

import chs.caplets.logic.commands.BulkAutoShareIntoCmd;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedObject;
import chs.common.IUIDObject;

import java.util.List;
import java.util.Map;

/**
 * orders the modular connectors from parent to child in the hierarchy
 */
public class ModularConnectorHierarchyBasedComparator extends BulkAutoShareIntoCmd.UIDObjectTypeComparator
{

	private Map<IUIDObject, ISharedObject> objectsToBeSharedInto;
	private List<ISharedConnector> sortedList;

	public ModularConnectorHierarchyBasedComparator(Map<IUIDObject, ISharedObject> objectsToBeSharedInto)
	{
		this.objectsToBeSharedInto = objectsToBeSharedInto;
		sortedList = new ModularConnectorsHelper().getHierarchyConnectors(objectsToBeSharedInto);
	}

	@Override public int compare(IUIDObject o1, IUIDObject o2)
	{
		ISharedObject s1 = objectsToBeSharedInto.get(o1);
		ISharedObject s2 = objectsToBeSharedInto.get(o2);
		if (sortedList.contains(s1) && sortedList.contains(s2)) {
			return Integer.compare(sortedList.indexOf(s1), sortedList.indexOf(s2));
		}
		return super.compare(o1, o2);
	}

	@Override protected int getTypeOrder(IUIDObject uidObject)
	{
		ISharedObject sharedObject = objectsToBeSharedInto.get(uidObject);
		if (sortedList.contains(sharedObject)) {
			return 10;
		}
		return super.getTypeOrder(uidObject);
	}
}
