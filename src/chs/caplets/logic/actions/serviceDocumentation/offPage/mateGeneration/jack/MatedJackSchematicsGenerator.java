/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration.jack;

import chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration.SchematicsGenerator;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.ICavitiesOwner;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cofUtils.CreationUtils;
import chs.cofUtils.parameterized.BackshellGraphicsRebuilder;
import chs.common.ILocation;
import chs.common.Side;
import chs.common.preferencesets.IPreferenceSet;
import chs.utilities.ListMap;
import chs.view.schem.IPinProxy;
import chs.view.utils.DiagramGenerationUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Creates mated jack schematics for a MxN plug connector
 */
public class MatedJackSchematicsGenerator extends SchematicsGenerator
{

	@NotNull protected IPinList createConnectorSchematic(chs.cof.logical.cable.IPinList replicatedPinList, Side side,
			IAbstractPin replicatedPin, IPinProxy pinProxy, ILocation absLocation,
			@Nullable IPreferenceSet preferenceSet, IPinList pinList)
	{
		final Side opposite = side.getOpposite();
		absLocation.setX(absLocation.getX() + pinList.getReferenceWidth());
		ListMap<IAbstractPin, IPinProxy> connectorPinProxyMap = new ListMap<IAbstractPin, IPinProxy>();
		connectorPinProxyMap.add(replicatedPin, pinProxy);
		ISchemDiagram diagram = pinList.getDiagram();
		final int referenceWidth = pinList.getReferenceWidth();
		@SuppressWarnings("DataFlowIssue") IPinList schemConnector =
				CreationUtils.createSchemPinList(replicatedPinList, referenceWidth, 0, connectorPinProxyMap,
						diagram, null, preferenceSet);

		schemConnector.setLocation(absLocation);
		schemConnector.addAttachedObject(pinList);
		pinList.addAttachedObject(schemConnector);
		DiagramGenerationUtilities
				.orientDeviceConnector(schemConnector, opposite, absLocation, false, false);
		if (replicatedPinList instanceof ICavitiesOwner) {
			IBackshell backshell = ((ICavitiesOwner) replicatedPinList).getBackshell();
			if (backshell != null) {
				new BackshellGraphicsRebuilder().rebuildAllBackshellGraphics(schemConnector, backshell.getSymbolRef());
			}
		}
		return schemConnector;
	}
}
