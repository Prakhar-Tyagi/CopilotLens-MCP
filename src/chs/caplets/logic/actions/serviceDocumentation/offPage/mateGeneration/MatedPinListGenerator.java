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
import chs.cof.logical.cable.IBaseDevice;
import chs.cof.logical.cable.IDeviceOwned;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.IPin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Mated plug generator for plug and modular connectors
 * Algorithm is as follows.
 * <p>
 * We get the instance of the connector in the current design, pins to be replicated and pins already available.
 * If there are pins to be replicated, then we replicate connector i.e., create a new instance of the connector
 * add the pins to be replicated
 * <p>
 * We then generate the schematics, this is needed because design wide usage manager needs to be updated with usages before we do share into
 * <p>
 * Then we do share into for the newly replicated connector.
 * <p>
 * Then we {@link IMatedPinListSchematicsGenerator#regenerateSchematics(chs.cof.logical.schem.IPinList)} This is needed esp for modular connectors
 */
public class MatedPinListGenerator implements IMatedPinListGenerator
{

	private ILogicDesign m_currentDesign;
	private IMatedPinListReplicator m_matedConnectorReplicator;
	private IShareIntoExecutor m_shareIntoExecutor;
	private IMatedPinListSchematicsGenerator m_schematicsGenerator;
	private Map<IPin, IPin> m_fetchedPinToDevicePinMap;

	public MatedPinListGenerator(ILogicDesign design, @NotNull IMatedPinListReplicator replicator,
			@NotNull IMatedPinListSchematicsGenerator schematicsGenerator,
			IShareIntoExecutor shareIntoExecutor, Map<IPin, IPin> fetchedPinToDevicePinMap)
	{
		m_currentDesign = design;
		m_matedConnectorReplicator = replicator;
		m_schematicsGenerator = schematicsGenerator;
		m_shareIntoExecutor = shareIntoExecutor;
		m_fetchedPinToDevicePinMap = fetchedPinToDevicePinMap;
	}

	public boolean generateMatedPinLists()
	{
		MatePinListGenerationOperands matePinListGenerationOperands =
				new MatePinListGenerationOperands(m_currentDesign, m_fetchedPinToDevicePinMap);
		IPinList connector = matePinListGenerationOperands.getInstanceInDesign();
		Set<IPin> pinsToReplicate = matePinListGenerationOperands.getPinsToReplicate();
		Map<IPin, IAbstractPin> existingPins = matePinListGenerationOperands.getExistingPins();
		IPinList replicatedConnector = null;
		//if there are pins to replicate, then create a new replicated connector
		if (!pinsToReplicate.isEmpty()) {
			Set<IPin> fetchedPins = m_fetchedPinToDevicePinMap.keySet();
			IAbstractPin fetchedConnectorPin = fetchedPins.iterator().next().getConnectivity();
			assert fetchedConnectorPin.getOwner() != null;
			replicatedConnector = m_matedConnectorReplicator.replicatePinList(fetchedConnectorPin.getOwner());
		}
		IPinList fetchedConnector = null;
		Set<chs.cof.logical.schem.IPinList> schematicConnectors = new LinkedHashSet<>();
		for (IPin fetchedSchematicPin : m_fetchedPinToDevicePinMap.keySet()) {
			IAbstractPin fetchedConnectorPin = fetchedSchematicPin.getConnectivity();
			IPin schematicDevicePin = m_fetchedPinToDevicePinMap.get(fetchedSchematicPin);
			assert fetchedConnectorPin.getOwner() != null;
			fetchedConnector = fetchedConnectorPin.getOwner();
			//for the pins to replicate, use the replicated connector
			//replicate pin and then create schematics
			if (pinsToReplicate.contains(fetchedSchematicPin)) {
				chs.cof.logical.schem.IPinList schematicConnector =
						replicateAndCreateSchematics(replicatedConnector, fetchedConnector, fetchedConnectorPin,
								schematicDevicePin);
				schematicConnectors.add(schematicConnector);
			}
			else {
				//for the pins already available in the current design, create schematics using existing connector instance and pins
				assert connector != null;
				chs.cof.logical.schem.IPinList schematicConnector =
						createSchematics(connector, existingPins, fetchedSchematicPin, schematicDevicePin);
				schematicConnectors.add(schematicConnector);
			}
		}

		//share into the newly replicated connector
		if (replicatedConnector != null) {
			m_shareIntoExecutor.doShareInto(replicatedConnector, fetchedConnector);
		}
		//regenerate schematics
		for (chs.cof.logical.schem.IPinList schematicConnector : schematicConnectors) {
			m_schematicsGenerator.regenerateSchematics(schematicConnector);
		}
		return true;
	}

	@NotNull
	private chs.cof.logical.schem.IPinList createSchematics(IPinList connector, Map<IPin, IAbstractPin> existingPins,
			IPin fetchedSchematicPin, IPin schematicDevicePin)
	{
		IAbstractPin connectorPin = existingPins.get(fetchedSchematicPin);
		chs.cof.logical.schem.IPinList schematicConnector =
				m_schematicsGenerator.createSchematics(connector, connectorPin, schematicDevicePin);
		return schematicConnector;
	}

	@NotNull private chs.cof.logical.schem.IPinList replicateAndCreateSchematics(@Nullable IPinList replicatedPinList,
			IPinList fetchedPinList, IAbstractPin fetchedPinListPin, IPin schematicDevicePin)
	{
		assert replicatedPinList != null;
		IAbstractPin connectorPin =
				m_matedConnectorReplicator.replicatePin(fetchedPinListPin, fetchedPinList, replicatedPinList);
		assert connectorPin != null;
		connectorPin.setBlockRef(null);
		chs.cof.logical.schem.IPinList schematicConnector =
				m_schematicsGenerator.createSchematics(replicatedPinList, connectorPin, schematicDevicePin);
		//we need to set the parent correctly, otherwise the share into will fail
		setParent(replicatedPinList, schematicDevicePin);
		return schematicConnector;
	}

	private void setParent(@NotNull IPinList replicatedConnector, IPin schematicDevicePin)
	{
//		assert replicatedConnector instanceof IDeviceOwned;
		if (!(replicatedConnector instanceof IDeviceOwned)) {
			return;
		}
		IPinList owner = schematicDevicePin.getConnectivity().getOwner();
		assert owner instanceof IBaseDevice;
		((IDeviceOwned) replicatedConnector).setOwner((IBaseDevice) owner);
	}
}
