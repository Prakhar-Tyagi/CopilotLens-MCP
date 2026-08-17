/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.helpers.backshell;

import chs.caplets.logic.actions.ConnectionFlow;
import chs.cof.logical.schem.IPin;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

/**
 * Interface for resolving pin overlaps between a backshell pin and another pin.
 * This is needed during Backshell transfer flow when a device mates a connector.
 */
public interface IBackshellPinOverlapResolver
{

	IBackshellPinOverlapResolver NEW_CONNECTOR_PIN_OVERLAP_RESOLVER = new NewConnectorPinOverlapResolver();
	IBackshellPinOverlapResolver DEFAULT_BACKSHELL_PIN_OVERLAP_RESOLVER = new DefaultBackshellPinOverlapResolver();

	Map<ConnectionFlow, IBackshellPinOverlapResolver> overlapResolvers = createResolvers();

	@NotNull static Map<ConnectionFlow, IBackshellPinOverlapResolver> createResolvers()
	{
		Map<ConnectionFlow, IBackshellPinOverlapResolver> resolvers = new EnumMap<>(ConnectionFlow.class);
		resolvers.put(ConnectionFlow.AutCreateConnectorConnection, NEW_CONNECTOR_PIN_OVERLAP_RESOLVER);
		resolvers.put(ConnectionFlow.NewPinListConnection, NEW_CONNECTOR_PIN_OVERLAP_RESOLVER);
		resolvers.put(ConnectionFlow.GHCConnectorConnection, NEW_CONNECTOR_PIN_OVERLAP_RESOLVER);
		resolvers.put(ConnectionFlow.ExistingPinListConnection, DEFAULT_BACKSHELL_PIN_OVERLAP_RESOLVER);
		resolvers.put(ConnectionFlow.UnDefinedFlow, DEFAULT_BACKSHELL_PIN_OVERLAP_RESOLVER);
		return resolvers;
	}

	void resolveOverlappedPins(@NotNull IPin backshellPin, @NotNull IPin overlappedPin);

	@NotNull
	static IBackshellPinOverlapResolver getInstance(ConnectionFlow connectionFlow)
	{
		return overlapResolvers.getOrDefault(connectionFlow, DEFAULT_BACKSHELL_PIN_OVERLAP_RESOLVER);
	}
}
