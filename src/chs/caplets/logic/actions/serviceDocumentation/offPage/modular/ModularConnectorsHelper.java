/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage.modular;

import chs.cof.logical.cable.IBaseDevice;
import chs.cof.logical.cable.IDeviceOwned;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedObject;
import chs.common.IUIDObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper class for modular connectors which has methods to sort the hierarchy from parent to child.
 */
class ModularConnectorsHelper
{

	ModularConnectorsHelper()
	{
	}

	/**
	 * @param objectsToBeSharedInto the map of object and shared object into which it will be shared into
	 * @return list of modular shared connectors sorted from parent
	 */
	@NotNull List<ISharedConnector> getHierarchyConnectors(Map<IUIDObject, ISharedObject> objectsToBeSharedInto)
	{
		Map<ISharedConnector, List<ConnectorInfo>> modularConnectorsInfo =
				getModularConnectorsInfo(objectsToBeSharedInto);
		Set<ISharedConnector> modularConnectors = modularConnectorsInfo.keySet();
		Set<ISharedConnector> sortedAll = getHierarchyConnectors(modularConnectors);
		return new ArrayList<>(sortedAll);
	}

	@NotNull protected Map<ISharedConnector, List<ConnectorInfo>> getModularConnectorsInfo(
			Map<IUIDObject, ISharedObject> objectsToBeSharedInto)
	{
		Map<ISharedConnector, List<ConnectorInfo>> modularConnectorsInfo = objectsToBeSharedInto
				.entrySet()
				.stream()
				.map(this::getConnectorInfo)
				.filter(Objects::nonNull)
				.collect(Collectors.groupingBy(ConnectorInfo::getSharedConnector));
		return modularConnectorsInfo;
	}

	@NotNull protected static Set<ISharedConnector> getHierarchyConnectors(Set<ISharedConnector> modularConnectors)
	{
		Set<Set<ISharedConnector>> hierarchies = getSortedHierarchies(modularConnectors);
		Set<ISharedConnector> sortedAll = new LinkedHashSet<>();
		for (Set<ISharedConnector> hierarchy : hierarchies) {
			sortedAll.addAll(hierarchy);
		}
//		Set<ISharedConnector> includingParents = getParentsInHierarchy(modularConnectors);
		sortedAll.retainAll(modularConnectors);
		return sortedAll;
	}

	private static boolean hasParentOrChildren(ISharedConnector c)
	{
		return c.getParentConnector() != null || !c.getChildConnectors().isEmpty();
	}

	@NotNull private static Set<Set<ISharedConnector>> getSortedHierarchies(Set<ISharedConnector> modularConnectors)
	{
		Set<ISharedConnector> traced = new HashSet<>();
		Set<Set<ISharedConnector>> hierarchies = new LinkedHashSet<>();
		for (ISharedConnector c : modularConnectors) {
			if (traced.contains(c)) {
				continue;
			}
			ISharedConnector root = c.getTopLevelConnector();
			Set<ISharedConnector> sorted = new LinkedHashSet<>();
			if (root != null) {
				preOrderTraversal(root, sorted);
				traced.addAll(sorted);
			}
			hierarchies.add(sorted);
		}
		return hierarchies;
	}

	private static void preOrderTraversal(ISharedConnector root, Set<ISharedConnector> visited)
	{
		visited.add(root);
		for (ISharedConnector child : root.getChildConnectors()) {
			preOrderTraversal(child, visited);
		}
	}

	@Nullable private ConnectorInfo getConnectorInfo(Map.Entry<IUIDObject, ISharedObject> entry)
	{
		IUIDObject iuidObject = entry.getKey();
		if (iuidObject instanceof IPinList) {
			IPinList connectivity = (IPinList) iuidObject;
			ISharedObject sharedObject = entry.getValue();
			if (connectivity instanceof IDeviceOwned && sharedObject instanceof ISharedConnector) {
				ISharedConnector sharedConnector = (ISharedConnector) sharedObject;
				if (hasParentOrChildren(sharedConnector)) {
					IDeviceOwned deviceOwned = (IDeviceOwned) connectivity;
					return new ConnectorInfo(deviceOwned, sharedConnector);
				}
			}
		}
		return null;
	}

	protected static class ConnectorInfo
	{

		@NotNull private IDeviceOwned deviceOwned;
		@NotNull private ISharedConnector sharedConnector;

		private ConnectorInfo(@NotNull IDeviceOwned deviceOwned, @NotNull ISharedConnector sharedConnector)
		{
			this.deviceOwned = deviceOwned;
			this.sharedConnector = sharedConnector;
		}

		@NotNull protected IDeviceOwned getDeviceOwned()
		{
			return deviceOwned;
		}

		@NotNull protected ISharedConnector getSharedConnector()
		{
			return sharedConnector;
		}
	}
}
