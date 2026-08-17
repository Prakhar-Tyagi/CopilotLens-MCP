/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage.modular;

import chs.caplets.logic.actions.shared.SelectSharedPanel;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IBaseDevice;
import chs.cof.logical.cable.IDeviceOwned;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPinList;
import chs.common.IUIDObject;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.IMessageCollectorAndReporter;
import chs.utility.IMessageContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 *
 */
public class FetchedModularConnectorsOwnerResolver extends ModularConnectorsHelper
{

	@NotNull private ILogicDesign targetDesign;
	@NotNull private IMessageCollectorAndReporter reporter;

	public FetchedModularConnectorsOwnerResolver(@NotNull ILogicDesign targetDesign,
			@NotNull IMessageCollectorAndReporter reporter)
	{
		this.targetDesign = targetDesign;
		this.reporter = reporter;
	}

	public boolean resolve(Map<IUIDObject, ISharedObject> objectsToBeSharedInto)
	{
		Map<ISharedConnector, List<ConnectorInfo>> modularConnectorsInfo =
				getModularConnectorsInfo(objectsToBeSharedInto);
		Set<ISharedConnector> sharedConnectors = modularConnectorsInfo.keySet();
		Set<ISharedConnector> sortedAll = getHierarchyConnectors(sharedConnectors);
		for (ISharedConnector sharedConnector : sortedAll) {
			List<ConnectorInfo> connectorInfos = modularConnectorsInfo.get(sharedConnector);
			//this is per design connector infos, so there must be one connectivity instance of connector
			assert connectorInfos.size() == 1;
			ConnectorInfo connectorInfo = connectorInfos.get(0);
			IDeviceOwned connector = connectorInfo.getDeviceOwned();
			boolean success = resolve(sharedConnector, connector, objectsToBeSharedInto);
			if (!success) {
				return false;
			}
		}
		return true;
	}

	private boolean resolve(ISharedConnector sharedConnector, IDeviceOwned connector,
			Map<IUIDObject, ISharedObject> objectsToBeSharedInto)
	{
		IBaseDevice owner = connector.getOwner();
		//current design instance of shared connector and its owner
		IPinList existingConnector = getSharedInstance(sharedConnector);
		IBaseDevice existingOwner = null;
		if (existingConnector instanceof IDeviceOwned) {
			existingOwner = ((IDeviceOwned) existingConnector).getOwner();
		}
//		else if (existingConnector != null) {
//			//can the connector in current design be mated with inline?can it be non IDeviceOwned?
//			return false;
//		}
		//if owner is null
		if (owner == null) {
			//if the owner in current design is not null, then set as owner
			if (existingOwner != null) {
				connector.setOwner(existingOwner);
			}
			//else the owner might be set in the future design share into
			return true;
		}
		//there must be as shared object into which the owner would be shared into
		ISharedObject so = objectsToBeSharedInto.get(owner);
		assert so instanceof ISharedPinList;
		ISharedPinList sharedObject = (ISharedPinList) so;
		ISharedPinList sharedOwner = sharedObject;
		//if the owner of connector instance in the current design and shared owner's instance in the current design must be same
		IPinList ownerInstanceInCurrentDesign = getSharedInstance(sharedOwner);
		//if there is an instance of owner, then current owner and owner should be same
		if (existingOwner != null) {
			boolean same = Objects.equals(existingOwner, ownerInstanceInCurrentDesign);
			if (!same) {
				reporter.storeMessage(PromptSeverity.ERROR,
						ResourceMgr
								.getString(SelectSharedPanel.class, "SelectSharedPanel.shareInto.ownerNotCompatible"),
						IMessageContext.createContext(connector));
			}
			return same;
		}
		//if the existingOwner is null, then existingConnector can be set with owner
		if (existingConnector != null) {
			assert existingConnector instanceof IDeviceOwned;
			((IDeviceOwned) existingConnector).setOwner(owner);
		}
		return true;
	}

	@Nullable private IPinList getSharedInstance(ISharedPinList sharedOwner)
	{
		return Objects.requireNonNull(targetDesign.getConnectivity()).findSharedPinList(sharedOwner);
	}
}
