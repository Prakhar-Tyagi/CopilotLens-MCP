/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2016-2026 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.IOutputWindow;
import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.helpers.CHSUndoableEdit;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.UndoableContainerIdler;
import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.logic.DeleteHelper;
import chs.caplets.shared.ForeignDesignChangesUndoableContainer;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IJoint;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IConnectorPin;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IHighwayConductor;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IGenericSchemPin;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.ILogicSegment;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.IDesignSharedUsageMgr;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.ISharedBackshellTermination;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.parts.ILibraryBackshell;
import chs.cof.parts.ILibraryBaseObject;
import chs.cof.parts.ILibraryCavity;
import chs.cof.parts.ILibraryCavityContainer;
import chs.cofUtils.parameterized.AddPinHelper;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.common.IUID;
import chs.ctf.caf.utils.IPinProxy;
import chs.subsystem.logic.manageconnections.IPinProvider;
import chs.subsystem.logic.manageconnections.ManageConnectionsServices;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utility.DiagramHelper;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.HighwayHelper;
import chs.utility.helpers.LibraryHelper;
import chs.utility.helpers.NodeHelper;
import chs.utility.properties.PropTextScrubber;
import chs.utility.ui.HTMLHelper;
import chs.utility.ui.SharedPinListEditUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ConductorConnectionChangerForDesign
{

	@Nullable private ISharedPinList sharedPinList;

	private chs.cof.logical.cable.IPinList pinlist = null;
	private Map<IPinProxy, IPinProxy> pinsToSwap;
	private Set<IUID> pinsAlreayProcessed = new HashSet<>();
	private Set<IUID> segmentsAlreadyProcessed = new HashSet<>();
	private SetMap<IUID, IUID> pinToProcessedTargetWires = new SetMap<>();
	private Map<IUID, StackWrapper> stackToProcess = new HashMap<>();
	private Set<ISchemDiagram> affectedDiagrams = new HashSet<>();
	private Set<IPinList> affectedSchemDevices = new HashSet<>();
	private Set<chs.cof.logical.schem.IConductor> disconnectedConductors = new HashSet<>();
	private Set<IJoint> affectedJoints = new HashSet<>();
	private ILogicDesign design;

	public ConductorConnectionChangerForDesign(ILogicDesign logicDesign, chs.cof.logical.cable.IPinList pinOwner,
			Map<IPinProxy, IPinProxy> pinsToSwap)
	{
		design = logicDesign;
		pinlist = pinOwner;
		sharedPinList = pinlist.getSharedPinList();
		this.pinsToSwap = pinsToSwap;
	}

	public ConductorConnectionChangerForDesign(ILogicDesign design, @Nullable ISharedPinList sharedPinList,
			Map<IPinProxy, IPinProxy> pinsToSwap)
	{
		this.design = design;
		this.sharedPinList = sharedPinList;
		this.pinsToSwap = pinsToSwap;
	}

	@Nullable private IAbstractPin getCablePin(IPinProxy pinProxy, boolean createIfNotAvailable)
	{
		IAbstractPin cablePin = pinProxy.getCablePin();
		cablePin = (cablePin != null && (UIDMgr.getNonDeletedObject(cablePin.getUID()) != null)) ?
				UIDMgr.getObjectOfType(cablePin.getUID(), IAbstractPin.class) : null;
		if (cablePin == null) {
			ISharedPin sharedPin = pinProxy.getSharedPin();
			if (sharedPin != null) {
				chs.cof.logical.cable.IPinList currentPinList = getCablePinListForSharedPinList(sharedPin);
				cablePin = currentPinList != null ? getCablePinFor(sharedPin, currentPinList) : null;
				if (currentPinList != null && cablePin == null && createIfNotAvailable) {
					cablePin = createCablePin(pinProxy, currentPinList);
				}
			}
			else {
				cablePin = createCablePin(pinProxy, getPinList(pinProxy));
			}
		}
		return cablePin;
	}

	@Nullable
	private chs.cof.logical.cable.IPinList getCablePinListForSharedPinList(ISharedPin sharedPin)
	{
		if(sharedPin instanceof ISharedBackshellTermination) {
			return getCablePinList(sharedPin.getOwner());
		}
		return getCablePinList(sharedPinList);
	}

	private chs.cof.logical.cable.IPinList getPinList(IPinProxy pinProxy)
	{
		if (pinProxy.getLibraryCavity() != null &&
				pinProxy.getLibraryCavity().getOwner() instanceof ILibraryBackshell) {
			if (pinlist instanceof IConnector connector && connector.getBackshell() != null) {
				pinProxy.setName(getOriginalPinName(pinProxy, connector));
				return connector.getBackshell();
			}
		}
		return pinlist;
	}

	@NotNull
	private String getOriginalPinName(IPinProxy pinProxy, IConnector connector)
	{
		String ownerName = connector.getBackshellName();
		String pinName = pinProxy.getName();
		if (pinName.startsWith(ownerName + ":")) {
			return pinName.substring(ownerName.length() + 1);
		}
		return pinName;
	}

	@Nullable private IAbstractPin createCablePin(IPinProxy pinProxy,
			chs.cof.logical.cable.IPinList thisPinList)
	{

		IAbstractPin cablePin = createPin(pinProxy, thisPinList);
		if (cablePin != null) {
			pinProxy.setCablePin(cablePin);
		}
		return cablePin;
	}

	@Nullable private chs.cof.logical.cable.IPinList getCablePinList(@Nullable ISharedPinList thisSharedPinList)
	{
		if (pinlist != null) {
			return pinlist;
		}
		IConnectivity connectivity = design.getConnectivity();
		if (connectivity != null) {
			return connectivity.findSharedPinList(thisSharedPinList);
		}
		return null;
	}

	@Nullable private IAbstractPin getCablePinFor(ISharedPin sharedPin, chs.cof.logical.cable.IPinList currentPinList)
	{
		IAbstractPin cablePin = null;
		Optional<IAbstractPin> pinProvider =
				ManageConnectionsServices.requireExtension(currentPinList, IPinProvider.class).getAllPins().stream()
						.filter(pin -> pin.getSharedPin() == sharedPin).findFirst();
		if (pinProvider.isPresent()) {
			cablePin = pinProvider.get();
		}
		return cablePin;
	}

	@Nullable private IAbstractPin createPin(IPinProxy pinProxy, chs.cof.logical.cable.IPinList pinOwner)
	{
		IAbstractPin cablePin = null;
		ISharedPin sharedPin = pinProxy.getSharedPin();
		if (sharedPin != null) {
			cablePin = SharedPinListEditUtils.createCablePin(sharedPin, pinOwner, false, true);
		}
		if (cablePin == null) {
			ILibraryBaseObject libraryObject = pinOwner.getLibraryObject();
			if (libraryObject instanceof ILibraryCavityContainer) {
				cablePin = createPinFromLibraryCavity(pinProxy, (ILibraryCavityContainer) libraryObject, pinOwner);
				if (pinOwner instanceof IGenericInlineConnector && cablePin != null) {
					IGenericInlineConnector matedConnector =
							((IGenericInlineConnector) pinOwner).getMatedInlines().iterator().next();
					IConnectorPin cableMatePin = (IConnectorPin) createCablePin(matedConnector);
					cableMatePin.connectIfPossibleIgnoringOwners(cablePin);
					assignLibraryCavity(cablePin, matedConnector, cableMatePin);
				}
			}
		}

		return cablePin;
	}

	private void assignLibraryCavity(IAbstractPin plugPin, IGenericInlineConnector jack,
			IConnectorPin jackPin)
	{
		ILibraryCavityContainer cavityContainer = (ILibraryCavityContainer) jack.getLibraryObject();

		if (cavityContainer != null) {

			IGenericInlineConnector plug = (IGenericInlineConnector) plugPin.getOwner();
			if (jack.isPartAssigned() && plug != null) {
				String pinName;
				IUID orgPart = plug.getLibraryRef();
				IUID jackPart = jack.getLibraryRef();
				String libPinName = LibraryHelper.getMatedPinName(plugPin.getName(), orgPart, jackPart);
				if (libPinName != null) {
					pinName = libPinName;
				}
				else {
					ILibraryCavity libCaivity = AddPinHelper
							.getMatchingLibraryPartOnPin(jack,
									plug, plugPin);
					if (libCaivity != null) {
						pinName = libCaivity.getName();
					}
					else {
						pinName = null;
					}
				}
				if (pinName != null) {
					jackPin.setName(pinName);
				}
			}

			AddPinHelper.assignLibraryCavity(jackPin, cavityContainer.getCavities());
		}
	}

	@Nullable private IAbstractPin createPinFromLibraryCavity(IPinProxy pinProxy, ILibraryCavityContainer libraryObject,
			chs.cof.logical.cable.IPinList connector)
	{
		IAbstractPin cablePin = null;
		Set<ILibraryCavity> cavities = libraryObject.getCavities();
		boolean hasMatching = hasLibraryCavity(pinProxy, cavities);
		if (hasMatching) {
			cablePin = createCablePin(connector);
			cablePin.setName(pinProxy.getName());
			AddPinHelper.assignLibraryCavity(cablePin, cavities);
		}
		return cablePin;
	}

	private boolean hasLibraryCavity(IPinProxy pinProxy, Set<ILibraryCavity> cavities)
	{
		return cavities.stream().filter(t -> t.getName().equals(pinProxy.getName())).count() > 0;
	}

	public void changeConnectionsOfDesign(boolean saveDesign) throws UserSessionException
	{
		boolean designLoadedHere = false;
		try {
			if (!design.isLoadedInMemory()) {
				design.loadToMemory();
				designLoadedHere = true;
			}
			design.getDiagrams();
			if (saveDesign) {
				try (InstallUndo ignored = new InstallUndo(design)) {
					processPinConnections();
				}
				saveDesign();
			}
			else {
				processPinConnections();
			}
		}
		finally {
			if (designLoadedHere) {
				design.unloadFromMemory();
			}
		}
	}

	private class InstallUndo extends UndoBase
	{

		InstallUndo(ILogicDesign design)
		{
			//do not set the temp Creation Deletion helper.
			//call processObjects in closable to clear all the objects.
			capletController = getControllerForDesign(design);
			super.init(capletController);
		}

		@Override protected void doClose()
		{
			CHSUndoableEdit currentEdit = super.getChsUndoableEdit();
			if (currentEdit != null && capletController != null) {
				capletController.getCapletModel().notifyPreModelChange(
						new ModelChangeEvent(capletController.getCapletModel(), currentEdit.getUIDsChanged(),
								currentEdit.getNewUIDs(), currentEdit.getDeletedUIDs()));
			}

			CAFUtils.getInstance().clearTempUndoableContainer();
		}

		@Override protected void createTempContainer()
		{
			tempContainer = new ManageConnectorUndoableContainer();
		}
	}

	@Nullable protected ICapletController getControllerForDesign(ILogicDesign thisdesign)
	{
		return CAFUtils.getInstance().getControllerForDesign(thisdesign);
	}

	private static class ManageConnectorUndoableContainer extends ForeignDesignChangesUndoableContainer
	{

		@Override public boolean isTimestampCurrent(int timestamp)
		{
			if (m_currentEdit == null) {
				return true;
			}
			return timestamp == m_timestamp;
		}

		@Override public void incTimestamp()
		{
			m_timestamp++;
		}
	}

	protected void saveDesign() throws UserSessionException
	{
		CAFUtils.getInstance().setTempUndoableContainer(UndoableContainerIdler.instance());
		try {
			CAFCommandHelper cmdHelper = new CAFCommandHelper();
			cmdHelper.saveDesign(design);
			cmdHelper.setDesignModifiedFlag(design, false);
		}
		finally {
			CAFUtils.getInstance().clearTempUndoableContainer();
		}
	}

	private String getMessageForDisconnectedConductors()
	{
		return HTMLHelper.color("red", ResourceMgr.getString(ConductorConnectionChangerForDesign.class,
				"ConductorConnectionChangerForDesign.output.conductorDisconnectedMsg"
				, disconnectedConductors.stream()
						.map(conductor -> HTMLHelper.link(design, conductor, conductor.getConnectivity().getName()))
						.collect(
								Collectors.joining(", "))));
	}

	private void processPinConnections()
	{
		Map<IAbstractPin, IAbstractPin> pinsToBeSwapped = getPinsToProcess();

		Set<IAbstractPin> pinsToBeProcessed = pinsToBeSwapped.keySet();

		Map<IAbstractPin, Set<IConductor>> oldConductorMap = getOldConnections(pinsToBeSwapped);
		for (IAbstractPin sourcePin : pinsToBeProcessed) {
			swapPinConnection(design, sourcePin, pinsToBeSwapped.get(sourcePin), oldConductorMap);
		}

		for (IJoint joint : affectedJoints) {
			if (!joint.isAssociatedWith(ILogicSegment.class)) {
				joint.removeAllAssociations();
			}
		}

		if (!disconnectedConductors.isEmpty()) {
			IOutputWindow outputWindow = CAFUtils.getInstance().getOutputWindow();
			if (outputWindow != null) {
				outputWindow.sendApplicationMessage(getMessageForDisconnectedConductors());
			}
		}

		for (StackWrapper stackWrapper : stackToProcess.values()) {
			stackWrapper.process();
		}

		for (IPinList schemDevice : affectedSchemDevices) {
			GeneratorParameters generatorParameters = DiagramHelper.createGeneratorParameters(schemDevice);
			Generator generator = Generator.getGenerator();
			generator.rebuildDeviceConnectors(schemDevice, generatorParameters, null);
		}

		for (ISchemDiagram diagram : affectedDiagrams) {
			diagram.refreshRepresentations();
		}

		if (sharedPinList != null) {
			IDesignSharedUsageMgr dsum = design.getSharedUsageMgr();
			DeleteHelper.getInstance().delete(design, getUnplacedPins(pinsToBeProcessed, dsum), null);
		}
	}

	@NotNull
	private Map<IAbstractPin, Set<IConductor>> getOldConnections(Map<IAbstractPin, IAbstractPin> pinsToBeSwapped)
	{
		Map<IAbstractPin, Set<IConductor>> oldConductorMap = new HashMap<>();
		for (IAbstractPin sourcePin : pinsToBeSwapped.keySet()) {
			oldConductorMap.put(sourcePin, new HashSet<>(sourcePin.getConductorsAsSet()));
		}
		return oldConductorMap;
	}

	private Collection<IAbstractPin> getUnplacedPins(Set<IAbstractPin> pinsToBeProcessed, IDesignSharedUsageMgr dsum)
	{
		return pinsToBeProcessed.stream()
				.filter(aCablePin -> aCablePin.getSharedPin() != null &&
						dsum.getDesignSharedUsageCount(aCablePin.getSharedPin()) == 0)
				.collect(Collectors.toSet());
	}

	@NotNull private Map<IAbstractPin, IAbstractPin> getPinsToProcess()
	{
		Map<IAbstractPin, IAbstractPin> pinsToBeSwapped = getPinsToBeSwapped();
		Map<IAbstractPin, IAbstractPin> matedpinsToBeSwapped = getMatedPins(pinsToBeSwapped);

		pinsToBeSwapped.putAll(matedpinsToBeSwapped);
		return pinsToBeSwapped;
	}

	@NotNull private Map<IAbstractPin, IAbstractPin> getPinsToBeSwapped()
	{
		Map<IAbstractPin, IAbstractPin> pinsToBeSwapped = new HashMap<>();
		if (pinsToSwap != null) {
			for (Map.Entry<IPinProxy, IPinProxy> pinToSwap : pinsToSwap.entrySet()) {
				IAbstractPin sourcePin = getCablePin(pinToSwap.getKey(), false);
				//shared pinlist instance in remote session might be deleted. So there is a possibility of not finding the intended pin.
				if (sourcePin != null) {
					IAbstractPin targetPin = getCablePin(pinToSwap.getValue(), true);
					if (targetPin != null) {

						pinsToBeSwapped.put(sourcePin, targetPin);
					}
				}
			}
		}

		return pinsToBeSwapped;
	}

	@NotNull private Map<IAbstractPin, IAbstractPin> getMatedPins(Map<IAbstractPin, IAbstractPin> pinsToBeSwapped)
	{
		Map<IAbstractPin, IAbstractPin> matedpinsToBeSwapped = new HashMap<>();
		for (IAbstractPin sourcePin : pinsToBeSwapped.keySet()) {
			IAbstractPin matedPin = getMatedPin(sourcePin);
			IAbstractPin matedTargetPin = getMatedPin(pinsToBeSwapped.get(sourcePin));
			if (matedPin != null && matedTargetPin != null) {
				matedpinsToBeSwapped.put(matedPin, matedTargetPin);
			}
		}
		return matedpinsToBeSwapped;
	}

	@Nullable private IAbstractPin getMatedPin(IAbstractPin sourcePin)
	{
		Collection<IAbstractPin> connectedPins = sourcePin.getConnectedPins();
		if (!connectedPins.isEmpty()) {
			return connectedPins.iterator().next();
		}
		return null;
	}

	private boolean swapPinConnection(ILogicDesign thisDesign, IAbstractPin sourcePin, IAbstractPin targetPin,
			Map<IAbstractPin, Set<IConductor>> oldConnections)
	{
		assert thisDesign != null;
		ConductorConnectionChange connectionChange =
				new ConductorConnectionChange(thisDesign, sourcePin, targetPin, oldConnections);
		connectionChange.modifyConductorConnections();
		return true;
	}

	private class ConductorConnectionChange
	{

		private IAbstractPin sourcePin;
		private IAbstractPin targetPin;
		private ILogicDesign thisDesign;
		private Map<IAbstractPin, Set<IConductor>> oldPinConnections;

		ConductorConnectionChange(ILogicDesign design, IAbstractPin sourcePin, IAbstractPin targetPin,
				Map<IAbstractPin, Set<IConductor>> oldPinConnections)
		{
			this.sourcePin = sourcePin;
			this.targetPin = targetPin;
			thisDesign = design;
			this.oldPinConnections = oldPinConnections;
		}

		private boolean modifyConductorConnections()
		{
			SetMap<IPinList, IAbstractSchemPin> pinsToBeChanged = getPinsToBeReparented();
			if (isValidToChange(pinsToBeChanged)) {
				Set<IConductor> connectedConductors = oldPinConnections.get(sourcePin);
				Set<IConductor> conductorsToDisconnect = new HashSet<>();
				connectedConductors.forEach(targetConductor -> {
					pinToProcessedTargetWires.add(targetPin.getUID(), targetConductor.getUID());
					if (!pinToProcessedTargetWires.contains(sourcePin.getUID(), targetConductor.getUID())) {
						conductorsToDisconnect.add(targetConductor);
					}
				});
				disconnectConductorFromOldPin(sourcePin, conductorsToDisconnect);
				changeConductorConnections(targetPin, connectedConductors);
				transferStackedConductors(connectedConductors);

				reparentToTargetPin(pinsToBeChanged);
				return true;
			}
			return false;
		}

		private void transferStackedConductors(Set<IConductor> connectedConductors)
		{
			IDesignWideUsageMgr wideUsageMgr = thisDesign.getDesignWideUsageMgr();
			for (IDiagramObject diagramObject : wideUsageMgr.getRepresentations(targetPin)) {
				if (diagramObject instanceof ISchemStackPin) {
					addStackedConductors(((ISchemStackPin) diagramObject).getConnectedHighways(), connectedConductors);
				}
			}
		}

		private void reparentToTargetPin(SetMap<IPinList, IAbstractSchemPin> pinsToReparent)
		{
			for (Map.Entry<IPinList, Set<IAbstractSchemPin>> pinListSetEntry : pinsToReparent.entrySet()) {
				IPinList srcPinList = pinListSetEntry.getKey();
				assert srcPinList != null : "Found pins without pinlist";
				IAbstractSchemPin targetSchemPin = findTargetSchemPin(srcPinList);
				Set<IAbstractSchemPin> pinsToBeReparented = new HashSet<>();
				for (IAbstractSchemPin sourceSchemPin : pinListSetEntry.getValue()) {
					if (srcPinList.getSymbolDef() != null || isMateSymboled(srcPinList)) {
						changeSegmentConnections(sourceSchemPin, targetSchemPin);
					}
					else if (!pinsAlreayProcessed.contains(sourceSchemPin.getUID()) &&
							!isReferencePin(sourceSchemPin)) {
						pinsToBeReparented.add(sourceSchemPin);
					}
				}
				reparentToTargetPin(pinsToBeReparented);

				pinsAlreayProcessed.addAll(getUIDs(pinsToBeReparented));
			}
		}

		@Nullable private IAbstractSchemPin findTargetSchemPin(IPinList srcSchemPinList)
		{
			IAbstractSchemPin targetSchemPin = srcSchemPinList.findPin(targetPin);
			//try again on attached pinlist as connector might be in splits
			if (targetSchemPin == null && srcSchemPinList.getConnectivity() instanceof IConnector) {
				for (IPinList attachedDevice : srcSchemPinList.getAttachedPinListObjects(IPinList.EXCLUDE_MODULAR)) {
					for (IPinList attachedPinList : attachedDevice
							.getAttachedPinListObjects(IPinList.EXCLUDE_MODULAR)) {
						if (attachedPinList != srcSchemPinList &&
								attachedPinList.getConnectivity() == srcSchemPinList.getConnectivity()) {
							targetSchemPin = attachedPinList.findPin(targetPin);
							if (targetSchemPin != null) {
								return targetSchemPin;
							}
						}
					}
				}
			}
			return targetSchemPin;
		}

		private boolean isMateSymboled(@NotNull IPinList srcSchemPinList)
		{
			IAbstractPin srcMatePin = getMatedPin(sourcePin);
			if (srcMatePin != null) {
				for (IPinList p : srcSchemPinList.getAttachedPinListObjects(IPinList.EXCLUDE_MODULAR)) {
					if (p.getSymbolDef() != null && p.getConnectivity() == srcMatePin.getOwner()) {
						return true;
					}
				}
			}
			return false;
		}

		/*
		 *  Disconnects segments from source pin joint and if there is a target pin, connects them to target pin joint
		 * */
		private void changeSegmentConnections(@NotNull IAbstractSchemPin sourceSchemPin,
				@Nullable IAbstractSchemPin targetSchemPin)
		{
			IJoint sourceJoint = sourceSchemPin.getJoint();
			if (sourceJoint == null) {
				return;
			}
			IJoint targetJoint = null;
			if (targetSchemPin != null) {
				targetJoint = targetSchemPin.getJoint();
				if (targetJoint == null) {
					targetJoint = NodeHelper.createJointAtLocation(targetSchemPin.getAbsolutionLocation());
					targetSchemPin.setJoint(targetJoint);
					targetJoint.addAssociation(targetSchemPin);
				}
			}

			for (ISegment segment : sourceJoint.getAssociations(ISegment.class)) {
				if (!segmentsAlreadyProcessed.contains(segment.getUID())) {
					if (targetJoint != null) {
						targetJoint.addAssociation(segment);
					}
					else {
						disconnectedConductors.add(segment.getConductor());
					}
					NodeHelper.replaceEquivSegmentNode(segment, sourceJoint, targetJoint);
					sourceJoint.removeAssociation(segment);
					affectedJoints.add(sourceJoint);
					ConductorRouteAction.getInstance().addConductorForRoute(segment.getConductor());
					segmentsAlreadyProcessed.add(segment.getUID());
				}
			}
		}

		private Set<IUID> getUIDs(Set<IAbstractSchemPin> pinsToProcess)
		{
			return pinsToProcess.stream()
					.filter(t -> t instanceof IPin)
					.map(t -> t.getUID())
					.collect(Collectors.toSet());
		}

		private void changeConductorConnections(IAbstractPin connectedPin, Set<IConductor> connectedConductors)
		{
			for (IConductor connecteConductor : connectedConductors) {
				connecteConductor.addPin(connectedPin);
			}
		}

		private void disconnectConductorFromOldPin(IAbstractPin oldMatedPin, Set<IConductor> connectedConductors)
		{
			for (IConductor connecteConductor : connectedConductors) {
				connecteConductor.removePin(oldMatedPin);
			}
		}

		private boolean isReferencePin(IAbstractSchemPin pin)
		{
			if (pin instanceof IPin) {
				return ((IGenericSchemPin) pin).isReference();
			}
			return false;
		}

		private void reparentToTargetPin(Set<IAbstractSchemPin> pins)
		{
			for (IAbstractSchemPin pin : pins) {
				if (pin instanceof IPin) {
					changeConnectivity((IPin) pin, targetPin);
				}
				else if (pin instanceof ISchemStackPin) {
					StackWrapper stackWrapper = stackToProcess.get(pin.getUID());
					if (stackWrapper == null) {
						stackWrapper = new StackWrapper(pin.getUID());
						stackToProcess.put(pin.getUID(), stackWrapper);
					}
					stackWrapper.pinToRemove(sourcePin.getUID());
					stackWrapper.pinToAdd(targetPin.getUID());
				}
				(new PropTextScrubber())
						.synchronizeChangedObjects(Collections.singleton(pin), Collections.emptyList());
			}
		}

		private void changeConnectivity(IPin pin, @Nullable IAbstractPin targetCablePin)
		{
			if (targetCablePin != null) {
				pin.setConnectivity(targetCablePin);
			}
		}

		@NotNull private Map<IAbstractSchemPin, IAbstractSchemPin> getMatedPins(IPinList pinList, IPinList matedPinList,
				Set<IAbstractSchemPin> pinSet)
		{
			ConnectionHelper connectionHelper = ConnectionHelper.getConnectionHelper(pinList, matedPinList);
			Map<IAbstractSchemPin, IAbstractSchemPin> pinMatingMap = new HashMap<>();
			if (connectionHelper != null) {
				for (IAbstractSchemPin pin : pinSet) {
					IAbstractSchemPin matedSchemPin = connectionHelper.getConnectedPin(pin);
					if (matedSchemPin != null) {
						pinMatingMap.put(pin, matedSchemPin);
					}
				}
			}
			return pinMatingMap;
		}

		private boolean isConnectedDifferentPinList(chs.cof.logical.cable.IPinList matedPinList,
				IAbstractPin abstractPin)
		{
			IAbstractPin targetMatedPin = abstractPin.getConnectedPin(matedPinList);
			return (targetMatedPin == null && !abstractPin.getConnectedPins().isEmpty());
		}

		private SetMap<IPinList, IAbstractSchemPin> getPinsToBeReparented()
		{
			IDesignWideUsageMgr designWideUsageMgr = thisDesign.getDesignWideUsageMgr();
			SetMap<IPinList, IAbstractSchemPin> pinsToBeChanged = new SetMap<>();
			if (designWideUsageMgr != null) {
				pinsToBeChanged = getPinsToBeReparented(designWideUsageMgr);
			}
			return pinsToBeChanged;
		}

		@NotNull
		private SetMap<IPinList, IAbstractSchemPin> getPinsToBeReparented(IDesignWideUsageMgr designWideUsageMgr)
		{
			List<IDesignSharedUsage> usages = designWideUsageMgr.getUsages(sourcePin);
			SetMap<IPinList, IAbstractSchemPin> pinsToBeChanged = new SetMap<>();
			for (IDesignSharedUsage usage : usages) {
				ISchemDiagram diagram = usage.getDiagram();
				diagram.loadToMemory();
				affectedDiagrams.add(diagram);
				IAbstractSchemPin pin = (IAbstractSchemPin) usage.getDiagramObject();
				if (pin != null) {
					IPinList parent = (IPinList) pin.getParent();
					if (parent != null) {
						pinsToBeChanged.add(parent, pin);
						if (pin.isDevicePin()) {
							affectedSchemDevices.add(parent);
						}
					}
				}
			}
			return pinsToBeChanged;
		}

		private boolean isValidToChange(SetMap<IPinList, IAbstractSchemPin> pinsToBeChanged)
		{
			for (Map.Entry<IPinList, Set<IAbstractSchemPin>> pinListSetEntry : pinsToBeChanged.entrySet()) {
				if (!isValidToChange(pinListSetEntry.getKey(), pinListSetEntry.getValue())) {
					return false;
				}
			}
			return true;
		}

		private boolean isValidToChange(IPinList pinList, Set<IAbstractSchemPin> pinSet)
		{
			boolean validToChange = true;
			Collection<IPinList> attachedPinListObjects = pinList.getAttachedPinListObjects(IPinList.EXCLUDE_MODULAR);
			for (IPinList matedPinList : attachedPinListObjects) {
				Map<IAbstractSchemPin, IAbstractSchemPin> pinMatingMap = getMatedPins(pinList, matedPinList, pinSet);
				for (Map.Entry<IAbstractSchemPin, IAbstractSchemPin> pinEntry : pinMatingMap.entrySet()) {
					if (pinEntry.getValue() != null) {
						// If target pin is mated to some other pinlist, then do not change parent
						if (isConnectedDifferentPinList(matedPinList.getConnectivity(), targetPin)) {
							validToChange = false;
							break;
						}
					}
				}
			}
			return validToChange;
		}
	}

	private IAbstractPin createCablePin(chs.cof.logical.cable.IPinList matedPinList)
	{
		IAbstractPin pin = FactoryMgr
				.getCablePropertiedFactory().createPinForOwner(FactoryMgr.getCommonFactory().createUID(), matedPinList);
		matedPinList.addPin(pin);
		return pin;
	}

	private class StackWrapper
	{

		private Set<IUID> pinsToRemove;
		private Set<IUID> pinsToAdd;
		private IUID stack;

		private StackWrapper(IUID stackUID)
		{
			pinsToRemove = new HashSet<>();
			pinsToAdd = new HashSet<>();
			stack = stackUID;
			stackToProcess.put(stackUID, this);
		}

		private void pinToAdd(IUID pinUID)
		{
			pinsToAdd.add(pinUID);
		}

		private void pinToRemove(IUID pinUID)
		{
			pinsToRemove.add(pinUID);
		}

		void process()
		{
			ISchemStackPin stackPin = UIDMgr.getObjectOfType(stack, ISchemStackPin.class);
			if (stackPin != null) {
				Set<IHighwaySchematic> connectedHighways = stackPin.getConnectedHighways();
				pinsToRemove.forEach(t -> {
					IAbstractPin pin = UIDMgr.getObjectOfType(t, IAbstractPin.class);
					if (pin != null) {
						stackPin.removePinFromStack(pin);
					}
				});

				pinsToAdd.forEach(t -> {
					IAbstractPin pin = UIDMgr.getObjectOfType(t, IAbstractPin.class);
					if (pin != null) {
						stackPin.addPinToStack(pin);
						addStackedConductors(connectedHighways, pin.getConductorsAsSet());
					}
				});
			}
		}
	}

	private void addStackedConductors(Set<IHighwaySchematic> connectedHighways, Set<IConductor> conductors)
	{
		for (IHighwaySchematic highway : connectedHighways) {
			IGeneralHighway cableHighway = HighwayHelper.toGeneralHighway(highway);
			for (IConductor conductor : conductors) {
				if (conductor instanceof IHighwayConductor && cableHighway != null) {
					cableHighway.addStackPinConductor((IHighwayConductor) conductor);
				}
			}
		}
	}
}