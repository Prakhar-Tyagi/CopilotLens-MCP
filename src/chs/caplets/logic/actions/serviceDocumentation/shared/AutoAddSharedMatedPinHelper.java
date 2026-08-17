/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.serviceDocumentation.shared;

import chs.caplets.logic.actions.AddPinActionModel;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGrid;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinPlaceholder;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cofUtils.parameterized.AddPinHelper;
import chs.cofUtils.parameterized.PinPlacementConstraintsHolder;
import chs.cofUtils.parameterized.PinPlacementHelper;
import chs.common.IUID;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.caf.utils.PinProxy;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utility.helpers.LibraryObjectInfoCache;
import chs.utility.ui.SharedPinListEditUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

class AutoAddSharedMatedPinHelper
{

	private final PublisherAddPinActionHelper.PublisherPinPlacementController m_controller;
	private AddPinActionModel m_pinActionModel;
	private SharedPinConnectivityHelper m_sharedPinConnectivityHelper;
	private boolean m_isReference;

	AutoAddSharedMatedPinHelper(AddPinActionModel pinActionModel,
			SharedPinConnectivityHelper sharedPinConnectivityHelper,
			PublisherAddPinActionHelper.PublisherPinPlacementController controller,
			boolean isReference)
	{
		m_pinActionModel = pinActionModel;
		m_sharedPinConnectivityHelper = sharedPinConnectivityHelper;
		m_controller = controller;
		m_isReference = isReference;
	}

	void addSharedConnectivityPinsToMates(chs.cof.logical.schem.IPinList pinList)
	{
		PinPlacementConstraintsHolder constraints = m_controller.getAnchorPinPlacementConstraints();
		addSharedConnectivityPinsToMates(pinList, constraints);
	}

	void addSharedConnectivityPinsToMates(chs.cof.logical.schem.IPinList pinList,
			@Nullable PinPlacementConstraintsHolder constraints)
	{
		Set<IAbstractSchemPin> pins = m_pinActionModel.getPins(pinList);
		IPinList connPinList = pinList.getConnectivity();
		if (constraints != null) {
			for (IAbstractSchemPin schematicPin : pins) {
				if (schematicPin instanceof IPin) {
					IPin schemPin = (IPin) schematicPin;
					IAbstractPin connPin = schemPin.getConnectivity();
					Point point = PinPlacementHelper.getTransformedCoord(schemPin);
					IGfxObject matchingObject = constraints.getMatchingObjectAt(point);
					if (matchingObject == null) {
						addMatedPinOnAttachedPinList(pinList, connPinList, connPin);
					}
					else if (matchingObject instanceof IPinPlaceholder) {
						addMatedPinOnMatchingMate(pinList, connPin, (IPinPlaceholder) matchingObject);
					}
				}
			}
		}
	}

	private void addMatedPinOnAttachedPinList(chs.cof.logical.schem.IPinList pinList, IPinList connPinList,
			IAbstractPin connPin)
	{
		Collection<chs.cof.logical.schem.IPinList> attachedPinListObjects =
				pinList.getAttachedPinListObjects(chs.cof.logical.schem.IPinList.INCLUDE_MODULAR);
		Set<String> sharedPinMatingDetails =
				m_sharedPinConnectivityHelper.getSharedPinMatingDetails(connPin, connPinList);
		for (chs.cof.logical.schem.IPinList attachedPinList : attachedPinListObjects) {
			IPinList connAttachedPinList = attachedPinList.getConnectivity();
			if (connAttachedPinList.isShared()) {
				ISharedPinList sharedPinList = connAttachedPinList.getSharedPinList();
				assert sharedPinList != null;
				boolean containsSharedPin = sharedPinList
						.getPins()
						.stream()
						.map(ISharedPin::getUID)
						.map(IUID::getString)
						.filter(sharedPinMatingDetails::contains)
						.findFirst()
						.isPresent();
				if (containsSharedPin) {
					addMatedPinOnMate(connPin, attachedPinList);
					return;
				}
			}
		}
	}

	private void addMatedPinOnMatchingMate(chs.cof.logical.schem.IPinList pinList, IAbstractPin connPin,
			IPinPlaceholder toMatePinPlaceHolder)
	{
		if (toMatePinPlaceHolder.getOwner() instanceof chs.cof.logical.schem.IPinList) {
			IAbstractPin matedPin =
					addMatedPinOnMate(connPin, (chs.cof.logical.schem.IPinList) toMatePinPlaceHolder.getOwner());
			//for pin on connector, we add the mated device pin schematics also.
			//for pins on device, inlines, the mated pin schematics are added by the PublisherAddPinActionHelper framework, we need not add the mated pin schematics.
			if (matedPin != null && connPin.getOwner() instanceof IConnector &&
					matedPin.getOwner() instanceof IDevice) {
				addMatedPinSchematicsOnMatedDevice(pinList, connPin, matedPin, toMatePinPlaceHolder);
			}
		}
	}

	private void addMatedPinSchematicsOnMatedDevice(chs.cof.logical.schem.IPinList pinList, IAbstractPin connectorPin,
			IAbstractPin matedDevicePin,
			IPinPlaceholder toMatePinPlaceHolder)
	{
		IPin newSchematicPin = toMatePinPlaceHolder.transmogrify(matedDevicePin, pinList.getDiagram().getGrid());
		if (newSchematicPin != null) {
			AddPinHelper.assignLibraryPartOnPin(newSchematicPin, connectorPin, new LibraryObjectInfoCache());
		}
	}

	@Nullable private IAbstractPin addMatedPinOnMate(IAbstractPin connPin, chs.cof.logical.schem.IPinList mateOwner)
	{
		chs.cof.logical.schem.IPinList pinListMate = mateOwner;
		IPinList connPinList = connPin.getOwner();
		assert connPinList != null;
		String sharedMateId = m_sharedPinConnectivityHelper.getSharedMateId(connPin, connPinList);
		if (sharedMateId != null) {
			ISharedPin sharedMatePin =
					UIDMgr.getObjectOfType(FactoryMgr.getCommonFactory().constructUID(sharedMateId),
							ISharedPin.class);
			if (sharedMatePin != null) {
				IPinProxy pinProxy = new PinProxy(sharedMatePin);
				List<IPinProxy> pinProxiesSelected = new ArrayList<>();
				pinProxiesSelected.add(pinProxy);
				IPinList connPinListMate = pinListMate.getConnectivity();
				SharedPinListEditUtils
						.createAndAddCablePins(connPinListMate, pinProxiesSelected, m_isReference,
								true);
				IAbstractPin connMatePin = pinProxy.getCablePin();
				boolean connected = connPin.connectIfPossible(connMatePin);
				if (!connected) {
					System.out.println("Not able to add shared mated pin to mate");
				}
				return connMatePin;
			}
		}
		return null;
	}
}
