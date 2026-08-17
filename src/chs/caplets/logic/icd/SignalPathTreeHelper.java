package chs.caplets.logic.icd;

import chs.cof.icd.IICDAssociatedSignal;
import chs.cof.logical.cable.IBaseDevice;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceOwned;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.IHarnessPlugConnector;
import chs.cof.logical.cable.Splice;
import chs.cof.logical.cable.wdg.IGeneratedConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cofUtils.parameterized.PinPlacementHelper;
import chs.utility.ICDUtils;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.InternalLinkHelper;
import chs.view.utils.GeneratorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SignalPathTreeHelper
{

	@Nullable private IPin originPin;
	@Nullable private IPin targetPin;
	private Set<IPinList> pinlistsToSkip;
	private Class<? extends chs.cof.logical.cable.IConductor> classType;

	public SignalPathTreeHelper(@Nullable IPin pin, Set<IPinList> pinlistsToBeSkipped,
			Class<? extends chs.cof.logical.cable.IConductor> abstractionType)
	{
		this (null, pin, pinlistsToBeSkipped, abstractionType);
	}

	public SignalPathTreeHelper(@Nullable IPin startPin, @Nullable IPin pin, Set<IPinList> pinlistsToBeSkipped,
			Class<? extends chs.cof.logical.cable.IConductor> abstractionType)
	{
		originPin = startPin;
		targetPin = pin;
		pinlistsToSkip = pinlistsToBeSkipped;
		classType = abstractionType;
	}

	@NotNull public SignalPathTree buildSignalPathTree(IPin startPin, Set<IPin> pinsTraversed,
			@Nullable List<IICDAssociatedSignal> otherValidSignalsToBeSkipped)
	{
		SignalPathTree rootNode = new SignalPathTree(startPin);
		pinsTraversed.add(startPin);
		if (startPin == targetPin) {
			return rootNode;
		}

		// we haven't reached our target, so keep traversing
		SignalPathTree childNode;
		// a child could be a pin reached through schem conductor
		for (IConductor conductor : startPin.getConductors()) {
			chs.cof.logical.cable.IConductor cableConductor = conductor.getConnectivity();
			boolean typeMatch = classType.isAssignableFrom(cableConductor.getClass());
			if (!typeMatch) {
				continue;
			}
			if (isConductorReferringToOtherValidSignal(conductor, otherValidSignalsToBeSkipped)) {
				continue;
			}
			List<IPin> childPins = conductor.getPins().stream()
					.filter(pin -> pin != startPin)
					.collect(Collectors.toList());
			if (!childPins.isEmpty()) {
				for (IPin childPin : childPins) {
					if (pinsTraversed.contains(childPin) ||
							pinlistsToSkip.contains(ICDInterconnectStrategy.getPinList(childPin))) {
						continue;
					}

					// or should it be checked that the child pin is only on a splitting object provided its not the target pin
					if (childPin != targetPin && !isPinOnASplittingObject(childPin)) {
						// we are not interested in traversing from devices
						continue;
					}

					childNode = buildSignalPathTree(childPin, pinsTraversed, otherValidSignalsToBeSkipped);
					rootNode.addChild(childNode);
				}
			}
			else {
				// represents a dangling conductor
				childNode = new SignalPathTree(null);
				rootNode.addChild(childNode);
			}
		}

		// a child could be a pin reached as a mated Pin or through internal link
		IPinList schemPinList = (IPinList) startPin.getParent();
		if (schemPinList != null) {
			chs.cof.logical.cable.IPinList cablePinlist = schemPinList.getConnectivity();
			// if inline or plug/jack pair or harness connector attached to a device with internal connectivity
			if (cablePinlist instanceof IConnector) {
				IPin matchingPin;
				if (isHarnessConnectorOfADeviceWithInternalConnectivity(cablePinlist)) {
					matchingPin = ConnectionHelper.getMatchingPinForConnectorPin(startPin, schemPinList, IDevice.class);
				}
				else {
					matchingPin =
							ConnectionHelper.getMatchingPinForConnectorPin(startPin, schemPinList, IConnector.class);
				}

				if (matchingPin != null && !pinsTraversed.contains(matchingPin)) {
					childNode = buildSignalPathTree(matchingPin, pinsTraversed, otherValidSignalsToBeSkipped);
					rootNode.addChild(childNode);
				}
			}

			// if device with internal connectivity
			if (cablePinlist instanceof IDevice && ((IDevice) cablePinlist).hasInternalConnectivity()) {
				// get all those generic pins, and if current schem pins point to any such pins
				// use them to construct tree
				Set<IGenericPin> internallyConnectedPins = InternalLinkHelper
						.getAllPinsConnectedByInternalLinks(startPin.getConnectivity(), new ArrayList<>());
				for (IPin schempin : schemPinList.getPins()) {
					if (internallyConnectedPins.contains(schempin.getConnectivity()) &&
							!pinsTraversed.contains(schempin) && !doesPinBelongToOriginPinlist(schempin)) {
						childNode = buildSignalPathTree(schempin, pinsTraversed, otherValidSignalsToBeSkipped);
						rootNode.addChild(childNode);
					}
				}
				// traverse to any pins on the attached harness connector
				for (IPin schemConnectedPin : PinPlacementHelper.getConnectedSchemPins(startPin)) {
					if (schemConnectedPin != null && !pinsTraversed.contains(schemConnectedPin)) {
						childNode = buildSignalPathTree(schemConnectedPin, pinsTraversed, otherValidSignalsToBeSkipped);
						rootNode.addChild(childNode);
					}
				}
			}

			// if splice symbol
			if (ICDInterconnectStrategy.isSpliceSymbol(cablePinlist)) {
				for (IPin splicePin : schemPinList.getPins()) {
					if (splicePin != startPin && !pinsTraversed.contains(splicePin)) {
						childNode = buildSignalPathTree(splicePin, pinsTraversed, otherValidSignalsToBeSkipped);
						rootNode.addChild(childNode);
					}
				}
			}
		}
		return rootNode;
	}

	private boolean doesPinBelongToOriginPinlist(@NotNull IPin schemPin)
	{
		return originPin != null && originPin.getParent() == schemPin.getParent();
	}

	@NotNull public static List<IPin> getPathToRoot(SignalPathTree node)
	{
		List<IPin> pinsOnPath = new ArrayList<>();
		if (node.getPin() != null) {
			pinsOnPath.add(node.getPin());
		}
		if (node.getParent() != null) {
			pinsOnPath.addAll(getPathToRoot(node.getParent()));
		}
		return pinsOnPath;
	}

	public static boolean isPinOnASplittingObject(@NotNull IPin pin)
	{
		IPinList schemPinlist = ICDInterconnectStrategy.getPinList(pin);
		if (schemPinlist == null) {
			return false;
		}
		chs.cof.logical.cable.IPinList cablePinlist = schemPinlist.getConnectivity();
		if (cablePinlist instanceof IDevice && ICDUtils.getMappedICD((IDevice) cablePinlist) != null) {
			// ICDs are NOT splitting objects : this check is needed as a device with internal connectivity
			// cannot be considered as a splitting object if it is an ICD - LOGIC2017-223
			return false;
		}

		return (cablePinlist instanceof IGenericInlineConnector ||
				GeneratorUtils.isPlugNJackPin(pin.getConnectivity()) ||
				cablePinlist instanceof Splice ||
				(cablePinlist instanceof IDevice && ((IDevice) cablePinlist).hasInternalConnectivity()) ||
				ICDInterconnectStrategy.isSpliceSymbol(cablePinlist) ||
				isHarnessConnectorOfADeviceWithInternalConnectivity(cablePinlist));
	}

	public static boolean isHarnessConnectorOfADeviceWithInternalConnectivity(
			chs.cof.logical.cable.IPinList cablePinlist)
	{
		if (!(cablePinlist instanceof IHarnessPlugConnector)) {
			return false;
		}
		IBaseDevice owningPinlist = ((IDeviceOwned) cablePinlist).getOwner();
		return (owningPinlist instanceof IDevice && ((IDevice) owningPinlist).hasInternalConnectivity());
	}

	public static boolean isConductorReferringToOtherValidSignal(IConductor conductor, @Nullable List<IICDAssociatedSignal> otherValidSignalsToBeSkipped)
	{
		if (conductor.getConnectivity() instanceof IGeneratedConductor && otherValidSignalsToBeSkipped != null) {
			String signalName = ICDUtils.getSourceICDSignal(conductor.getConnectivity());
			return otherValidSignalsToBeSkipped.stream()
					.map(signal -> signal.getNetName())
					.anyMatch(name -> name.equals(signalName));
		}
		return false;
	}
}
