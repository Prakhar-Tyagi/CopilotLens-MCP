/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration;

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * provides the operands for the mated connector generation
 */
class MatePinListGenerationOperands
{

	private ILogicDesign m_currentDesign;
	private Map<IPin, IPin> m_fetchedPinToDevicePinMap;
	private Set<IPin> m_pinsToReplicate;
	private Map<IPin, IAbstractPin> m_existingPins;
	private IPinList m_matedPinList = null;

	MatePinListGenerationOperands(ILogicDesign design, Map<IPin, IPin> fetchedPinToDevicePinMap)
	{
		m_currentDesign = design;
		m_fetchedPinToDevicePinMap = fetchedPinToDevicePinMap;
		m_pinsToReplicate = new LinkedHashSet<>();
		m_existingPins = new HashMap<>();
		updatePinsToReplicate();
	}

	/**
	 * @return the pins that are needed to be replicated
	 */
	@NotNull Set<IPin> getPinsToReplicate()
	{
		return m_pinsToReplicate;
	}

	/**
	 * @return instance of the connector available in the current design
	 */
	@Nullable IPinList getInstanceInDesign()
	{
		return m_matedPinList;
	}

	/**
	 * @return pins available on the existing connector instance, which need not be replicated
	 */
	@NotNull Map<IPin, IAbstractPin> getExistingPins()
	{
		return m_existingPins;
	}

	private void updatePinsToReplicate()
	{
		Set<IPin> fetchedPins = m_fetchedPinToDevicePinMap.keySet();
		IAbstractPin firstFetchedMatePin = fetchedPins.iterator().next().getConnectivity();
		assert firstFetchedMatePin.getOwner() != null;
		IPinList matedPinList = getInstanceInDesign(firstFetchedMatePin.getOwner());
		if (matedPinList == null) {
			m_pinsToReplicate = m_fetchedPinToDevicePinMap.keySet();
			return;
		}
		m_matedPinList = matedPinList;
		for (IPin fetchedSchematicPin : m_fetchedPinToDevicePinMap.keySet()) {
			IAbstractPin fetchedConnectorPin = fetchedSchematicPin.getConnectivity();
			IAbstractPin connectorPin = getMatchingPin(fetchedConnectorPin, matedPinList);
			if (connectorPin == null) {
				m_pinsToReplicate.add(fetchedSchematicPin);
			}
			else {
				m_existingPins.put(fetchedSchematicPin, connectorPin);
			}
		}
	}

	@Nullable private IAbstractPin getMatchingPin(IAbstractPin fetchedMatePin, @NotNull IPinList matedPinList)
	{
		IAbstractPin connectorPin = null;
		Collection<IAbstractPin> pinCollection = matedPinList.getPinCollection();
		//if the fetched pin connectivity is already present in the current design.
		//then we need not replicate the fetched pin
		for (IAbstractPin pin : pinCollection) {
			if (isMatchingPin(pin, fetchedMatePin)) {
				connectorPin = pin;
				break;
			}
		}
		return connectorPin;
	}

	private boolean isMatchingPin(IAbstractPin pin, IAbstractPin fetchedMatePin)
	{
		ISharedPin sharedPin = fetchedMatePin.getSharedPin();
		if (sharedPin == null) {
			return Objects.equals(pin, fetchedMatePin);
		}
		else {
			return Objects.equals(pin.getSharedPin(), sharedPin);
		}
	}

	@Nullable private IPinList getInstanceInDesign(IPinList fetchedMate)
	{
		IPinList connector = null;
		//if the fetched objects are from the same design, there is no need to replicate connectivity
		//we can use the same objects as connectivity for generating schem.
		if (Objects.equals(fetchedMate.getDesign(), m_currentDesign)) {
			connector = fetchedMate;
		}
		else {
			//check in the current design connectivity pin lists
			ISharedPinList sharedPinList = fetchedMate.getSharedPinList();
			assert sharedPinList != null;
			List<IDesignSharedUsage> usages = m_currentDesign.getSharedUsageMgr().getUsages(sharedPinList);
			if (!usages.isEmpty()) {
				IDesignSharedUsage designSharedUsage = usages.iterator().next();
				ILogicObject logicObject = designSharedUsage.getLogicObject();
				assert logicObject instanceof IPinList;
				//if the fetched pin list connectivity is already present in the current design.
				//then we need not replicate the fetched pin list
				connector = (IPinList) logicObject;
				//check the pins of the connectivity pin list
			}
		}
		return connector;
	}
}
