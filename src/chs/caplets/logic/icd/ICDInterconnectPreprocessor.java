/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.icd;

import chs.cof.drawplus.IDiagramObject;
import chs.cof.icd.IDeviceICD;
import chs.cof.icd.IICDAssociatedSignal;
import chs.cof.icd.IICDBackshell;
import chs.cof.icd.IICDBackshellTermination;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IHarnessPlugConnector;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IObjectFilter;
import chs.utilities.CommonUtils;
import chs.utilities.SetMap;
import chs.utility.ICDSignalDetailsFinder;
import chs.utility.ICDUtils;
import chs.utility.IDeviceICDPinSignalAssociation;
import chs.utility.helpers.SchemPinListHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Preprocess to calculate the information needed to generate connectivity for ICD signals
 */

public class ICDInterconnectPreprocessor
{

	private ICDInterconnectPreprocessor()
	{
	}

	@NotNull public static List<PlacingPinRouteInfo> constructPlacingPinRouteInfo(
			@NotNull IICDSignalSourceSchemPinlist currentSchemDevice,
			@NotNull IDeviceICD currentICD, @NotNull IObjectFilter<IPin> pinFilter)
	{
		List<PlacingPinRouteInfo> placingPinRouteInfo = new ArrayList<>();
		for (IDeviceICDPinSignalAssociation pinSignalAsso : currentICD.getICDUsageDefinition()
				.getPinSignalAssociations()) {
			final IPin placingDevPin = currentSchemDevice.getSignalMatchingDevicePin(pinSignalAsso.getPinName());
			final IPin placingPin = currentSchemDevice.getEquivalentICDMatchingSignalPin(placingDevPin);
			if (placingDevPin != null && !placingDevPin.isReference() && placingPin != null &&
					pinFilter.accept(placingDevPin)) {
				Collection<IICDAssociatedSignal> icdAssociatedSignals = pinSignalAsso.getICDAssociatedSignals();
				placingPinRouteInfo.add(new PlacingPinRouteInfo(placingDevPin, placingPin, icdAssociatedSignals));
			}
		}

		IDevice cableDevice = currentSchemDevice.getCableDevice();
		List<IDeviceICD> placingICDs = Collections.singletonList(currentICD);
		final Map<IICDBackshell, IBackshell> icdToLogicBackshellMapForPlacingDev =
				ICDUtils.determineICDBackshellToLogicBackshellMapping(cableDevice, placingICDs);
		final SetMap<IICDBackshellTermination, IICDAssociatedSignal> icdTerminationInfoForPlacingDev =
				ICDSignalDetailsFinder.getICDSignalsAssociatedWithTerm(placingICDs);
		final SetMap<IBackshellTermination, IPin> terminationsToProcess =
				getTerminationsToProcess(currentSchemDevice.getSchemDevice());
		for (Map.Entry<IICDBackshellTermination, Set<IICDAssociatedSignal>> terminationSetEntry :
				icdTerminationInfoForPlacingDev.entrySet()) {
			final IBackshellTermination placingTerm =
					ICDUtils.determineMatchingBSTerm(terminationSetEntry.getKey(), icdToLogicBackshellMapForPlacingDev);
			if (placingTerm == null) {
				continue;
			}
			Set<IPin> schemTerms = terminationsToProcess.pullReadOnlySafeSet(placingTerm);
			for (IPin schemTerm : schemTerms) {
				if (schemTerm.isReference()) {
					continue;
				}
				if (pinFilter.accept(schemTerm) ||
						doesBTBelongToConnectorThatIsBeingProcessed(placingTerm, placingPinRouteInfo)) {
					placingPinRouteInfo
							.add(new PlacingPinRouteInfo(schemTerm, schemTerm, terminationSetEntry.getValue()));
				}
			}
		}
		return placingPinRouteInfo;
	}

	private static boolean doesBTBelongToConnectorThatIsBeingProcessed(
			@NotNull IBackshellTermination backshellTermination,
			@NotNull List<PlacingPinRouteInfo> placingPinRouteInfo)
	{
		chs.cof.logical.cable.IPinList owner = backshellTermination.getOwner();
		IBackshell backshell = CommonUtils.cast(owner, IBackshell.class);
		if (backshell == null) {
			return false;
		}
		IConnector backshellConnector = backshell.getOwner();
		if (backshellConnector == null) {
			return false;
		}
		for (PlacingPinRouteInfo pinRouteInfo : placingPinRouteInfo) {
			IPin schemPin = pinRouteInfo.getPlacingPin();
			if (backshellConnector == schemPin.getConnectivity().getOwner()) {
				return true;
			}
		}

		return false;
	}

	@NotNull public static SetMap<IBackshellTermination, IPin> getTerminationsToProcess(@Nullable IPinList schemDevice)
	{
		SetMap<IBackshellTermination, IPin> termSchemInfo = new SetMap<>();
		getTerminationsToProcess(schemDevice, termSchemInfo);
		return termSchemInfo;
	}

	private static void getTerminationsToProcess(@Nullable IPinList schemDevice,
			@NotNull SetMap<IBackshellTermination, IPin> collector)
	{
		if (schemDevice == null) {
			return;
		}
		SchemPinListHelper.getAttachedSchemPinLists(schemDevice, IHarnessPlugConnector.class).forEach((schemConn) -> {
			Collection<IPin> pins = schemConn.getObjects(IPin.class);
			for (IPin pin : pins) {
				IBackshellTermination term = CommonUtils.cast(pin.getConnectivity(), IBackshellTermination.class);
				if (term != null) {
					collector.add(term, pin);
				}
			}
		});
	}

	@NotNull public static SetMap<IBackshellTermination, IPin> getTerminationsToProcess(@NotNull ISchemDiagram diagram,
			@NotNull IDevice device)
	{
		SetMap<IBackshellTermination, IPin> termSchemInfo = new SetMap<>();
		for (IDiagramObject diagramObject : diagram.getRepresentations(device.getUID())) {
			IPinList schemDevice = CommonUtils.cast(diagramObject, IPinList.class);
			if (schemDevice != null) {
				getTerminationsToProcess(schemDevice, termSchemInfo);
			}
		}
		return termSchemInfo;
	}
}
