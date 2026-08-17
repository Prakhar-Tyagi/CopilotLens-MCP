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
import chs.cof.icd.IICDAssociatedSignal;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.common.ILocation;
import chs.utilities.CommonUtils;
import chs.utility.ICDUtils;
import chs.utility.helpers.SharedConductorHelper;
import chs.view.utils.ConductorRouteActionHelper;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Generate single ended conductors to represent signals defined on ICD pins
 */

public abstract class ICDSingleEndedConnectStrategy extends ICDInterconnectStrategy
{
	private ICDSingleEndedConductorData singleEndedData;

	ICDSingleEndedConnectStrategy(@NotNull PersistenceHandler persistenceHandler)
	{
		super(persistenceHandler);
		singleEndedData = new ICDSingleEndedConductorData();
	}

	public void generateSingleEndedConductors(@NotNull ISchemDiagram diagram,
			@NotNull List<PlacingPinRouteInfo> placingPinInfo)
	{
		for (PlacingPinRouteInfo pinRouteInfo : placingPinInfo) {
			for (IICDAssociatedSignal associatedSignal : pinRouteInfo.getAssociatedSignals()) {
				createSingleEndedConductors(diagram, pinRouteInfo.getPlacingPin(), associatedSignal);
			}
		}
		if (!singleEndedData.getData().isEmpty()) {
			IPinList pinList = placingPinInfo.get(0).getPlacingDevPin().getConnectivity().getOwner();
			if (pinList != null) {
				mPersistenceHandler.getReporter().reportSingleEndedMessage(pinList);
			}
		}
	}

	protected void createSingleEndedConductors(@NotNull ISchemDiagram diagram, @NotNull IPin placingPin, @NotNull IICDAssociatedSignal signal)
	{
		if (signal.isShieldWire()) {
			return;
		}

		if (isSignalAlreadyPresentOnTheDiagram(diagram, placingPin, signal, isWiringAbstraction(),
				getCableConductorType())) {
			return;
		}

		// TODO Use the right value for length
		ILocation danglingLocation = calculateDanglingLocation(placingPin, ConductorRouteActionHelper.SINGLE_ENDED_CONDUCTOR_LENGTH);
		if (danglingLocation == null) {
			return;
		}

		chs.cof.logical.cable.IConductor cableConductor = getCableConductorToJoinWithPin(diagram, signal);
		if (cableConductor == null) {
			return;
		}
		IConductor conductor =
				getNewSchemConductor(diagram, danglingLocation, placingPin.getAbsLocation(), cableConductor);
		addNewConductorToContext(signal, conductor);
		ISegment newSegment = (ISegment) conductor.getSegments().iterator().next();
		newSegment.connectPin(placingPin);
		addForRouting(placingPin, newSegment);
		SharedConductorHelper.updateUsages(cableConductor, Objects.requireNonNull(diagram.getDesign()));
		singleEndedData.addData(placingPin, conductor);
	}

	public static boolean isSignalAlreadyPresentOnTheDiagram(@NotNull ISchemDiagram diagram, @NotNull IPin placingPin,
			@NotNull IICDAssociatedSignal signal, boolean wiringAbstraction,
			Class<? extends chs.cof.logical.cable.IConductor> cableConductorType)
	{
		for (IDiagramObject object : diagram.getRepresentations(placingPin.getConnectivity().getUID())) {
			IPin pin = CommonUtils.cast(object, IPin.class);
			if (pin == null) {
				continue;
			}
			for (IConductor conductor : pin.getConductors()) {
				chs.cof.logical.cable.IConductor cableConductor = conductor.getConnectivity();
				if (!cableConductorType.isAssignableFrom(cableConductor.getClass())) {
					continue;
				}
				String associatedSignalName =
						ICDUtils.getAssociatedSignalNameForConductor(cableConductor, wiringAbstraction);
				if (signal.getNetName().equalsIgnoreCase(associatedSignalName)) {
					return true;
				}
			}
		}
		return false;
	}
}
