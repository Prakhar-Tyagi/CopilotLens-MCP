package chs.caplets.logic.merge;

/*
 * Copyright 2010 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IBaseDevice;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConductorIterator;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IDeviceOwned;
import chs.cof.logical.cable.IDeviceOwnedConnector;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.parts.ILibraryBaseObject;
import chs.cof.parts.ILibraryCavity;
import chs.cof.parts.ILibraryCavityContainer;
import chs.cof.parts.ILibraryObject;
import chs.common.IUIDObjectCollection;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.LibraryHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: melmorsy Date: 16-Mar-2010 Time: 10:07:54
 */
public class PinlistMerger extends Merger implements IPinlistMerger
{

	public PinlistMerger(ILogicObject sourceLogicObject, ILogicObject targetLogicObject,
			@NotNull IMergeActionChangeReporter reporter)
	{
		super(sourceLogicObject, targetLogicObject, reporter);
	}

	@Override void mergeChildrenConnectivity(ILogicObject sourceLogicObject, ILogicObject targetLogicObject)
	{
		chs.cof.logical.cable.IPinList sourceParent = (chs.cof.logical.cable.IPinList) sourceLogicObject;
		chs.cof.logical.cable.IPinList targetParent = (chs.cof.logical.cable.IPinList) targetLogicObject;

		mergeAbstractPins(sourceParent, targetParent);
	}

	protected void postMergeConnectivity(ILogicObject sourceLogicObject, ILogicObject targetLogicObject)
	{
		super.postMergeConnectivity(sourceLogicObject, targetLogicObject);
		ILibraryBaseObject baseObject = targetLogicObject.getLibraryObject();
		if (targetLogicObject instanceof chs.cof.logical.cable.IPinList &&
				baseObject instanceof ILibraryCavityContainer) {
			targetLogicObject.assignLibraryPart((ILibraryObject) baseObject);
			Set<ILibraryCavity> allCavities = ((ILibraryCavityContainer) baseObject).getAllCavities();
			Map<String, ILibraryCavity> libraryCavities = new HashMap<>(allCavities.size());
			for (ILibraryCavity cavity : allCavities) {
				libraryCavities.put(cavity.getName(), cavity);
			}
			for (IAbstractPin pin : ((chs.cof.logical.cable.IPinList) targetLogicObject).getPins()) {
				pin.assignLibraryCavity(libraryCavities.get(pin.getName()));
			}
		}
	}

	@NotNull public static Map<IAbstractPin, IAbstractPin> getPinMappingForMerge(chs.cof.logical.cable.IPinList sourceParent,
			chs.cof.logical.cable.IPinList targetParent)
	{
		Map<IAbstractPin, IAbstractPin> pinMapping = new LinkedHashMap<IAbstractPin, IAbstractPin>();
		IAbstractPinIterator abstractPinIterator = sourceParent.getPins();
		while (abstractPinIterator.hasNext()) {
			IAbstractPin pin = abstractPinIterator.getNext();
			IAbstractPin targetPin =
					chs.cof.logical.cable.IPinList.Statics.findPinByName(targetParent, pin.getName(), false);
			pinMapping.put(pin, targetPin);
		}
		return pinMapping;
	}

	protected void mergeAbstractPins(chs.cof.logical.cable.IPinList sourceParent,
			chs.cof.logical.cable.IPinList targetParent)
	{
		List<IAbstractPin> pinsToBeMoved = mergeMatchingPins(sourceParent, targetParent);

		for (IAbstractPin moveablePin : pinsToBeMoved) {
			mergePin(sourceParent, targetParent, moveablePin, null);
		}
	}

	@Override
	public void mergeBackshellTerminations(@NotNull IBackshell sourceBackshell, @NotNull IBackshell targetBackshell)
	{
		mergeAbstractPins(sourceBackshell, targetBackshell);
	}

	@Override @Nullable
	public ILogicObject getMappedValue(ILogicObject key)
	{
		return super.getMappedValue(key);
	}

	@Override
	public void addProcessedSchematic(@NotNull IConnectivityRef schemSourceObject)
	{
		super.addProcessedSchematic(schemSourceObject);
	}

	@Override
	public void processSchematicsFor(@NotNull ILogicObject logicObject, @NotNull ISchematicProcessor processor)
	{
		super.processSchematicsFor(logicObject, processor);
	}

	@Override
	public void processSchematicsForDiagram(@NotNull ILogicObject logicObject, @NotNull ISchematicProcessor processor,
			@NotNull ISchemDiagram schemDiagram)
	{
		super.processSchematicsForDiagram(logicObject, processor, schemDiagram);
	}

	@NotNull protected List<IAbstractPin> mergeMatchingPins(chs.cof.logical.cable.IPinList sourceParent,
			chs.cof.logical.cable.IPinList targetParent)
	{
		List<IAbstractPin> pinsToBeMoved = new LinkedList<IAbstractPin>();
		Map<IAbstractPin, IAbstractPin> pinMapping = getPinMappingForMerge(sourceParent, targetParent);
		for (Map.Entry<IAbstractPin, IAbstractPin> entry : pinMapping.entrySet()) {
			IAbstractPin pin = entry.getKey();
			IAbstractPin targetPin = entry.getValue();
			if (targetPin != null) {
				mergePin(sourceParent, targetParent, pin, targetPin);
			}
			else {
				// Pins to be moved to the target (not merged) will be deferred to the end of this loop
				// such that in case the source has pins with the same name, it wouldn't move the first one to the target, then
				// merge the second one with the first one moved there!
				pinsToBeMoved.add(pin);
			}
		}

		return pinsToBeMoved;
	}

	protected void mergePin(chs.cof.logical.cable.IPinList sourceParent, chs.cof.logical.cable.IPinList targetParent,
			IAbstractPin sourcePin, @Nullable IAbstractPin targetPin)
	{
		mergePinAttributes(sourcePin, targetPin);
		if (targetPin != null) {
			addMapping(sourcePin, targetPin);
			fixupConnections(sourcePin, targetPin);
			mergeProperties(sourcePin, targetPin);
		}
		else {
			fixupIncompatiblePins(sourceParent, targetParent, sourcePin);
			sourceParent.removePin(sourcePin);
			addMapping(sourcePin, sourcePin);
			targetParent.addPin(sourcePin);
		}
	}

	protected void fixupIncompatiblePins(chs.cof.logical.cable.IPinList sourcePinList,
			chs.cof.logical.cable.IPinList targetPinList, IAbstractPin sourcePin)
	{
	}

	protected void fixupConnections(IAbstractPin pin, IAbstractPin targetPin)
	{
		if (pin == targetPin) {
			return;
		}
		pin.transferConnections(targetPin, true);
		pin.clearConnectedPin();
	}

	protected void fixupConductors(IAbstractPin pin, IAbstractPin targetPin)
	{
		IConductorIterator conductorsIterator = pin.getConductors();
		while (conductorsIterator.hasNext()) {
			IConductor conductor = conductorsIterator.getNext();
			conductor.removePin(pin);
			conductor.addPin(targetPin);
		}
	}

	protected void resetSchematicsConnectivity(IConnectivityRef sourceSchemObject, ILogicObject targetlogicObject)
	{
		IPinList sourceSchemPinlist = (IPinList) sourceSchemObject;
		sourceSchemPinlist.setConnectivity((chs.cof.logical.cable.IPinList) targetlogicObject);

		IUIDObjectCollection<IPin> schemPins = sourceSchemPinlist.getPins();
		for (IPin schemPin : schemPins) {
			IGenericPin mappedConnectivity = (IGenericPin) getMappedValue(schemPin.getConnectivity());
			if (mappedConnectivity != null) {
				schemPin.setConnectivity(mappedConnectivity);
			}
		}
		Set<IAbstractPin> pinsToAdd = new HashSet<IAbstractPin>();
		Set<IAbstractPin> pinsToDelete = new HashSet<IAbstractPin>();
		for (ISchemStackPin stackPin : sourceSchemPinlist.getStackPins()) {
			for (IAbstractPin pin : stackPin.getAllConnectivity()) {
				pinsToDelete.add(pin);
				IAbstractPin mappedConnectivity = (IAbstractPin) getMappedValue(pin);
				if (mappedConnectivity != null) {
					pinsToAdd.add(mappedConnectivity);
				}
			}
			for (IAbstractPin pin : pinsToDelete) {
				stackPin.removePinFromStack(pin);
			}
			for (IAbstractPin pin : pinsToAdd) {
				stackPin.addPinToStack(pin);
			}
		}
	}

	protected void detachUnconnectedConnector(IPinList pinList, IPinList attchedPL)
	{
		boolean detachConnected = !areConnectored(pinList, attchedPL);

		if (!detachConnected && isConnectorMerger() &&
				shouldDetach(pinList.getConnectivity(), attchedPL.getConnectivity())) {
			detachConnected = true;

			//Disconnect pins before detaching the attached pinlist
			for (IAbstractSchemPin pin : attchedPL.getPins()) {
				pin.disconnect();
			}
		}
		if (detachConnected) {
			pinList.disconnectFrom(attchedPL);
		}
	}

	private boolean isConnectorMerger()
	{
		return m_sourceLogicObject instanceof IConnector;
	}

	protected boolean shouldDetach(chs.cof.logical.cable.IPinList sourcePL, chs.cof.logical.cable.IPinList attachedPL)
	{
		// Detach attached pinlist from the source, if we are landing into a case where connector mated to both connetors as well as device
		if (sourcePL instanceof IConnector) {
			if (attachedPL instanceof IConnector) {
				if (getConnectedDevice((IConnector) m_targetLogicObject) != null) {
					return true;
				}
			}
			if (attachedPL instanceof IBaseDevice) {
				chs.cof.logical.cable.IPinList targetPl = (chs.cof.logical.cable.IPinList) m_targetLogicObject;
				if (isConnectedToConnector(targetPl)) {
					return true;
				}

				IBaseDevice devAttToTarget = getConnectedDevice((IConnector) m_targetLogicObject);
				return devAttToTarget != null && devAttToTarget != attachedPL;
			}
		}
		return false;
	}

	@Nullable private IBaseDevice getConnectedDevice(IConnector connector)
	{
		return connector instanceof IDeviceOwnedConnector ? ((IDeviceOwned) connector).getOwner() : null;
	}

	private boolean isConnectedToConnector(chs.cof.logical.cable.IPinList pinlist)
	{
		for (IAbstractPin pin : pinlist.getPins()) {
			Collection<chs.cof.logical.cable.IPinList> connectedPinLists = pin.getConnectedPinLists();

			for (chs.cof.logical.cable.IPinList matedPL : connectedPinLists) {
				if (matedPL instanceof IConnector) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean areConnectored(IPinList schemAttachment, IPinList schemOwnerDevice)
	{
		ConnectionHelper chelper = new ConnectionHelper(schemOwnerDevice);
		chelper.resetPinList(schemAttachment);
		for (IAbstractSchemPin attachmentPin : schemAttachment.getAllPins()) {
			IAbstractSchemPin pin = chelper.getConnectedPin(attachmentPin, (matchingPin) -> {
				if (attachmentPin instanceof IPin && matchingPin instanceof IPin) {
					return true;
				}
				if (attachmentPin instanceof ISchemStackPin && matchingPin instanceof ISchemStackPin) {
					return attachmentPin.isConnected(matchingPin);
				}
				return false;
			});

			if (pin instanceof IPin && attachmentPin instanceof IPin) {
				IPin schemPin = (IPin) pin;
				IPin schemAttachmentPin = (IPin) attachmentPin;
				IAbstractPin attachmentCablePin = schemAttachmentPin.getConnectivity();
				IAbstractPin cablePin = schemPin.getConnectivity();
				if (areConnectedPins(attachmentCablePin, cablePin)) {
					return true;
				}
			}
			else if (pin instanceof ISchemStackPin && attachmentPin instanceof ISchemStackPin) {
				ISchemStackPin schemPin = (ISchemStackPin) pin;
				ISchemStackPin schemAttachmentPin = (ISchemStackPin) attachmentPin;
				for (IAbstractPin attachmentCablePin : schemAttachmentPin.getAllConnectivity()) {
					for (IAbstractPin connectedPin : attachmentCablePin.getConnectedPins()) {
						if (schemPin.hasPin(connectedPin)) {
							if (areConnectedPins(attachmentCablePin, connectedPin)) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	private boolean areConnectedPins(IAbstractPin attachmentCablePin, IAbstractPin cablePin)
	{
		IAbstractPin mappedAttachmentCablePin = attachmentCablePin;
		// If this pin has been merged into antoerh pin, then use the target pin for checking
		if (attachmentCablePin != null && getMappedValue(attachmentCablePin) instanceof IAbstractPin) {
			mappedAttachmentCablePin = (IAbstractPin) getMappedValue(attachmentCablePin);
		}
		// The same check here
		// Probably, at that instance we can't merge into multiple logic objects,
		// so either the "attachmentCablePin" will have mapped value (in case of merging connectors)
		// or the "cablePin" will have mapped value (in case of merging devices)
		IAbstractPin mappedCablePin = cablePin;
		if (cablePin != null && getMappedValue(cablePin) instanceof IAbstractPin) {
			mappedCablePin = (IAbstractPin) getMappedValue(cablePin);
		}

		boolean arePinsConnected = false;
		if (mappedAttachmentCablePin instanceof IDevicePin && mappedCablePin instanceof IDevicePin) {
			arePinsConnected = ((IDevicePin) mappedCablePin).getConnectedDevicePin() == mappedAttachmentCablePin;
		}
		else if (mappedAttachmentCablePin != null && mappedCablePin != null) {
			arePinsConnected = mappedCablePin.isConnected(mappedAttachmentCablePin);
		}
		return arePinsConnected;
	}

	@Override protected void postSchematicMerge(IConnectivityRef schemSourceObject)
	{
		super.postSchematicMerge(schemSourceObject);
	}

	@NotNull public static Mergeable areMergeable(
			chs.cof.logical.cable.IPinList sourceObject, chs.cof.logical.cable.IPinList targetObject)
	{
		ILibraryBaseObject libraryObject = sourceObject.getLibraryObject();
		chs.cof.logical.cable.IPinList pinlist = targetObject;
		if (libraryObject == null) {
			libraryObject = targetObject.getLibraryObject();
			pinlist = sourceObject;
		}
		if (libraryObject != null && libraryObject instanceof ILibraryCavityContainer) {
			int numOfcavities = ((ILibraryObject) libraryObject).getNumCavities();
			Set<String> mergedPinsSet = new HashSet<String>();
			for (IAbstractPin abstractPin : sourceObject.getPins()) {
				mergedPinsSet.add(abstractPin.getName());
			}
			for (IAbstractPin abstractPin : targetObject.getPins()) {
				mergedPinsSet.add(abstractPin.getName());
			}
			if (numOfcavities < mergedPinsSet.size()) {
				return Mergeable.NumberOfCavitiesMismatch;
			}
			Set<String> cavityNames = new HashSet<String>(numOfcavities);
			LibraryHelper.collectCavityNames((ILibraryObject) libraryObject, cavityNames);
			for (IAbstractPin pin : pinlist.getPins()) {
				if (!cavityNames.contains(pin.getName())) {
					return Mergeable.LibraryCavityNamesMismatch;
				}
			}
		}
		return Mergeable.Possible;
	}

	protected void mergePinAttributes(IAbstractPin sourcePin, @Nullable IAbstractPin targetPin)
	{
	}

	@Override
	public void mergeWithSchematics(chs.cof.logical.cable.IPinList sourceParent, chs.cof.logical.cable.IPinList targetParent) {

		// reuse existing mapping + connectivity logic
		mergeAbstractPins(sourceParent, targetParent);
		// trigger schematic merge (this is what you were missing)
		if(!(targetParent instanceof IBackshell bs)){
			return;
		}
		if(!(bs.getOwner() instanceof IDeviceConnector dc)){
			return;
		}
		if(!(dc.getOwner() instanceof IDevice dev)) {
			return;
		}
		processSchematicsFor(dev, schemObject -> {
			resetDSCBackShellTermSchematicsConnectivity(schemObject);
		});

	}

	protected void resetDSCBackShellTermSchematicsConnectivity(IConnectivityRef targetSchemObject)
	{
		if (!(targetSchemObject instanceof IPinList targetSchemObject1)) {
			return;
		}
		IUIDObjectCollection<IPin> schemPins = targetSchemObject1.getPins();
		for (IPin schemPin : schemPins) {
			if (schemPin.getConnectivity() instanceof IBackshellTermination bst) {
				IGenericPin mappedConnectivity = (IGenericPin) getMappedValue(bst);
				if (mappedConnectivity != null) {
					schemPin.setConnectivity(mappedConnectivity);
				}
			}
		}
	}
}
