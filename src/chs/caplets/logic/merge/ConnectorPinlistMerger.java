package chs.caplets.logic.merge;

import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBaseDevice;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IConnectorPin;
import chs.cof.logical.cable.IDeviceOwned;
import chs.cof.logical.cable.IDeviceOwnedConnector;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IHarnessPlugConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IPinList;
import chs.common.INamedPropertiedObject;
import chs.common.IUID;
import chs.utilities.CollectionUtils;
import chs.utilities.Pair;
import chs.utility.helpers.BlockDeviceConnectionHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by IntelliJ IDEA. User: melmorsy Date: 18-Mar-2010 Time: 16:32:29
 */
public class ConnectorPinlistMerger extends PinlistMerger
{

	public ConnectorPinlistMerger(ILogicObject sourceLogicObject, ILogicObject targetLogicObject,
			@NotNull IMergeActionChangeReporter reporter)
	{
		super(sourceLogicObject, targetLogicObject, reporter);
	}

	@Override void mergeChildrenConnectivity(ILogicObject sourceLogicObject, ILogicObject targetLogicObject)
	{
		super.mergeChildrenConnectivity(sourceLogicObject, targetLogicObject);

		IConnector sourceConnector = (IConnector) sourceLogicObject;
		IConnector targetConnector = (IConnector) targetLogicObject;
		mergeBackshell(sourceConnector, targetConnector);

		if (sourceConnector instanceof IDeviceOwnedConnector && targetConnector instanceof IDeviceOwnedConnector) {
			IBaseDevice sourceOwner = ((IDeviceOwned) sourceConnector).getOwner();
			IBaseDevice targetOwner = ((IDeviceOwned) targetConnector).getOwner();
			if (sourceOwner != null && targetOwner == null && targetConnector.getNumMates() == 0) {
				((IDeviceOwned) targetConnector).setOwner(((IDeviceOwned) sourceConnector).getOwner());
			}
			else if (sourceOwner != null && targetOwner != null) {
				//Disconnect connector pins that are still connected to the old owner device - if any
				fixupConnectorDeviceCOnnections(targetOwner, (IDeviceOwnedConnector) targetConnector);
			}
		}
	}

	private void mergeBackshell(@NotNull IConnector sourceConnector, @NotNull IConnector targetConnector)
	{
		new BackshellMerger()
				.mergeBackshell(sourceConnector, targetConnector, this);
	}

	private void fixupConnectorDeviceCOnnections(IBaseDevice targetOwner,
			IDeviceOwnedConnector targetConnector)
	{
		for (IAbstractPin connectorPin : targetConnector.getPins()) {
			if (!connectorPin.isConnected(targetOwner)) {
				connectorPin.clearConnectedPin();
			}
		}
	}

	@Override protected void mergeSchematic(IConnectivityRef sourceSchemObject, ILogicObject targetlogicObject)
	{
		super.mergeSchematic(sourceSchemObject, targetlogicObject);

		fixupConnectorMatings(sourceSchemObject);

		resetSchematicsConnectivity(sourceSchemObject, targetlogicObject);
	}

	protected void fixupConnectorMatings(IConnectivityRef sourceSchemObject)
	{
		IPinList schemConnector = (IPinList) sourceSchemObject;
		for (IPinList attachedPinlist : schemConnector.getAttachedPinListObjects()) {
			detachUnconnectedConnector(schemConnector, attachedPinlist);
		}
	}

	public static Mergeable areMergeable(IConnector sourceObject, IConnector targetObject)
	{
		boolean isSourceObjectARingTerminal = sourceObject.isRingTerminal();
		boolean isTargetObjectARingTerminal = targetObject.isRingTerminal();
		if (isSourceObjectARingTerminal && isTargetObjectARingTerminal) {
			//Return as not mergeable if the cavity names doesnt match
			chs.cof.logical.cable.IPinList sourcePinList = sourceObject;
			chs.cof.logical.cable.IPinList targetPinList = targetObject;

			if ((sourcePinList.getNumPins() == 1 && targetPinList.getNumPins() == 1)) {
				String sourcePinName = sourcePinList.getPins().iterator().next().getName();
				String targetPinName = targetPinList.getPins().iterator().next().getName();
				if (!sourcePinName.equalsIgnoreCase(targetPinName)) {
					return Mergeable.RingTerminalCavityNamesMismatch;
				}
			}
		}
		Mergeable modularConnectorMergeability = modularConnectorMergeability(sourceObject, targetObject);
		if (modularConnectorMergeability != Mergeable.Possible) {
			return modularConnectorMergeability;
		}

		return Mergeable.Possible;
	}

	public static Mergeable modularConnectorMergeability(IConnector source, IConnector target)
	{
		Set<String> sourcePins = source.getPins().stream()
				.map(INamedPropertiedObject::getName)
				.collect(Collectors.toSet());
		if(CollectionUtils.containsAtLeastOne(sourcePins, target.getBlockedCavities()))
		{
			return Mergeable.SourceCavitiesBlockedInTarget;
		}

		if (source.getLibraryRef() == null)  //a connector with no libpart can be merged into any other connector
		{
			return Mergeable.Possible;
		}
		IUID sourceLibRef = source.getLibraryRef();
		IUID targetLibRef = target.getLibraryRef();
		if (sourceLibRef != null && targetLibRef != null && sourceLibRef != targetLibRef) {  //lib-part compatibility
			return Mergeable.LibraryDifferentLibraries;
		}
		if (source.getOccupiedPosition() != null) {
			return Mergeable.CannotMergeAChildModularConnector;
		}
		if (source.getPositions().isEmpty()) { //non-modular connector
			if (!target.getPositions().isEmpty()) { //cannot be merged into modular connector
				return Mergeable.NonModularConnectorCannotBeMergedIntoModularConnector;
			}
		}
		else {  //modular connector
			if (targetLibRef == null) { //cannot be merged into connector with no library part
				return Mergeable.ModularConnectorCannotBeMergedIntoNonLibrariedConnector;
			}
			if (!source.getPositionedObjects().isEmpty()) {
				//modular connector with atleast one filled position - can't be merged
				return Mergeable.SelectedModularConnectorHasAssociatedConnectors;
			}
			else if (target.getOccupiedPosition() != null) {
				//modular connector with all empty positions cannot be merged into a child connector
				return Mergeable.CannotMergeIntoAChildModularConnector;
			}
		}
		return Mergeable.Possible;
	}

	public static Mergeable inlinesMergeability(IGenericInlineConnector source, IGenericInlineConnector target)
	{
		if (source.isInline() ^ target.isInline()) {
			return Mergeable.InlinesNotBothAreInlines;
		}

		boolean pinPairsCheck =
				(checkPinPairs(source) && checkPinPairs(target)) &&
						checkPinPairs(source.getMatedInlines().iterator().next()) &&
						checkPinPairs(target.getMatedInlines().iterator().next());
		if (!pinPairsCheck) {
			return Mergeable.InlinesInvalidPinPairs;
		}

		boolean libraryPartsCheck = checkLibraryParts(source, target) &&
				checkLibraryParts(source.getMatedInlines().iterator().next(),
						target.getMatedInlines().iterator().next());
		if (!libraryPartsCheck) {
			return Mergeable.InlinesLibraryPartsMismatch;
		}

		boolean pinpairsMatch = checkMergeablePinPairs(source, target) &&
				checkMergeablePinPairs(source.getMatedInlines().iterator().next(),
						target.getMatedInlines().iterator().next());
		if (!pinpairsMatch) {
			return Mergeable.InlinesPinPairsMismatch;
		}

		return Mergeable.Possible;
	}

	private static boolean checkPinPairs(IGenericInlineConnector connector)
	{
		for (IAbstractPin pin : connector.getPins()) {
			IConnectorPin connectorPin = (IConnectorPin) pin;
			if (connectorPin.getMatedPin() == null) {
				return false;
			}
		}
		return true;
	}

	private static boolean checkLibraryParts(IGenericInlineConnector source, IGenericInlineConnector target)
	{
		IUID sourceLibRef = source.getLibraryRef();
		IUID targetLibRef = target.getLibraryRef();
		return sourceLibRef == targetLibRef;
	}

	private static boolean checkMergeablePinPairs(IGenericInlineConnector source, IGenericInlineConnector target)
	{
		for (IAbstractPin sourcePin : source.getPins()) {
			IConnectorPin matedPin = ((IConnectorPin) sourcePin).getMatedPin();

			IAbstractPin targetPin =
					chs.cof.logical.cable.IPinList.Statics.findPinByName(target, sourcePin.getName(), false);
			if (targetPin instanceof IConnectorPin) {
				IConnectorPin targetMatedPin = ((IConnectorPin) targetPin).getMatedPin();
				if (!targetMatedPin.getName().equals(matedPin.getName())) {
					return false;
				}
			}
		}
		return true;
	}

	@Override protected void fixupIncompatiblePins(chs.cof.logical.cable.IPinList sourcePinList,
			chs.cof.logical.cable.IPinList targetPinList, IAbstractPin sourcePin)
	{
		if (sourcePinList instanceof IHarnessPlugConnector && targetPinList instanceof IHarnessPlugConnector) {
			IBaseDevice sourceOwner = ((IDeviceOwned) sourcePinList).getOwner();
			IBaseDevice targetOwner = ((IDeviceOwned) targetPinList).getOwner();

			if (sourceOwner != null && sourceOwner == targetOwner) {
				Pair<String, String> targetConnectionType =
						BlockDeviceConnectionHelper.getPinConnectionType(sourceOwner, targetPinList);

				Pair<String, String> sourceConnectionType =
						BlockDeviceConnectionHelper.getPinConnectionType(sourceOwner, sourcePinList);

				if(!BlockDeviceConnectionHelper.areBlockPinOfSameType(sourceConnectionType, targetConnectionType)){
					sourcePin.clearConnectedPin();
				}
			}
		}
	}
}
