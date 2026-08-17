package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.actions.ghc.GenerateHarnessConnActionHelper;
import chs.cof.drawplus.IJoint;
import chs.cof.library.IFootprintable;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBlock;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.IInternalLink;
import chs.cof.logical.cable.IInternalPin;
import chs.cof.logical.cable.IInternalPinIterator;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.shared.IDesignSharedPinListUsage;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.ISymbolDefIterator;
import chs.cof.symbol.ISymbolRef;
import chs.common.IReference;
import chs.common.IUID;
import chs.common.IUIDMgr;
import chs.common.IUIDObject;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utilities.StringUtils;
import chs.utilities.ui.MessageHelper;
import chs.utility.DiagramHelper;
import chs.utility.Replicator;
import chs.utility.SymbolUtils;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.NodeHelper;
import chs.utility.helpers.PinListHelper;
import chs.utility.helpers.PropertyHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: 29 Apr, 2013 Time: 12:41:14 PM To change this template use File |
 * Settings | File Templates.
 */
public class DeviceUnshareHelper extends GenericPinListUnshareHelper
{

	private SetMap<IPin, IWireConductor> m_ValidWireConnections;
	private SetMap<IPin, IPin> m_schemPinConnectionsToMake;
	@Nullable private GenerateHarnessConnActionHelper mHarnessConnectorsGenerator;

	public DeviceUnshareHelper(IDesign theDesign, @Nullable ISchemDiagram diagram)
	{
		super(theDesign, diagram);
		mHarnessConnectorsGenerator = createHarnessConnActionHelper();
	}

	@Nullable public GenerateHarnessConnActionHelper createHarnessConnActionHelper()
	{
		if (m_diagram == null) {
			return null;
		}
		//TODO : We need to decouple GHC from diagram
		return new GenerateHarnessConnActionHelper(m_diagram);
	}

	@Override
	@NotNull public IActionEnum setup(BaseShareActionOperands operands, @Nullable ISchemDiagram diagram)
	{
		IActionEnum result = super.setup(operands, diagram);
		if (result == IActionEnum.eCompleted) {
			if (cablePinList.canMaintainMultipleSymbols()) {
				// when unsharing multi-symbolled object we may require to create new connectivity pins.
				// if the pin list is library parted, we will not be able to add the pins
				// therefore we should cancel the action.
				if (hasSymbolPinConflict(cablePinList, schemPinLists)) {
					m_schemPinConnectionsToDisconnect = new SetMap<IPin, IPin>();
					m_schemPinConnectionsToMake = new SetMap<IPin, IPin>();
					m_InvalidWireConnections = new SetMap<IPin, IWireConductor>();
					m_ValidWireConnections = new SetMap<IPin, IWireConductor>();

					// if the pin list is library parted, we can't create new pins on library parted objects
					if (cablePinList.isPartAssigned()) {

						showError(CAFUtils.getInstance().getDialogFrame()
								, ResourceMgr.getString(UnsharePinListActionHelper.class,
										"UnsharePinListActionHelper.LibrariedSymbolPinConflict.Header")
								, ResourceMgr.getString(UnsharePinListActionHelper.class,
										"UnsharePinListActionHelper.LibrariedSymbolPinConflict.Msg"));
						return IActionEnum.eCanceled;
					}
					else {

						showWarning(CAFUtils.getInstance().getDialogFrame()
								, ResourceMgr.getString(UnsharePinListActionHelper.class,
										getSymbolPinConflictHeaderKey())
								, ResourceMgr.getString(UnsharePinListActionHelper.class,
										getSymbolPinConflictMessageKey()));

						if (willNeedDisconnect(cablePinList, schemPinLists)) {
							showWarning(CAFUtils.getInstance().getDialogFrame()
									, ResourceMgr.getString(UnsharePinListActionHelper.class,
											"UnsharePinListActionHelper.SymbolPinConflictDisconnect.Header")
									, ResourceMgr.getString(UnsharePinListActionHelper.class,
											getSymbolPinConflictDisconnectKey()));
						}
					}
				}
			}
		}
		return result;
	}

	@NotNull protected String getSymbolPinConflictDisconnectKey()
	{
		return "UnsharePinListActionHelper.SymbolPinConflictDisconnect.Msg";
	}

	@NotNull protected String getSymbolPinConflictHeaderKey()
	{
		return "UnsharePinListActionHelper.SymbolPinConflict.Header";
	}

	@NotNull protected String getSymbolPinConflictMessageKey()
	{
		return "UnsharePinListActionHelper.SymbolPinConflict.Msg";
	}

	protected void showError(Frame dialogFrame, String heading, String string1)
	{
		MessageHelper.showErrorMessage(dialogFrame, heading, string1);
	}

	protected void showWarning(Frame dialogFrame, String heading, String string1)
	{
		MessageHelper.showWarningMessage(dialogFrame, heading, string1);
	}

	@Override public boolean doEdit()
	{
		boolean success = super.doEdit();

		if (success) {

			//melmorsy - FEAT12331
			//Regenerate harness connectors for a device upon unsharing
			if (!schemPinLists.isEmpty()) {
				//Connectivity object may have been changed due to this unsharing, get the new one:
				IPinList oneSchematic = schemPinLists.iterator().next();
				if (mHarnessConnectorsGenerator != null &&
						PinListHelper.isHarnessFootprintedAndAllowAutoCreation(oneSchematic)) {
					//Regenerate connectors for the current connectivity
					mHarnessConnectorsGenerator
							.generateHarnessConnectorsForDevice((IDevice) oneSchematic.getConnectivity());
				}
			}
		}

		return success;
	}

	protected void createConnectivityPinsForDifferentSymbols(IAbstractPin cablePin, Map<IPinList, IPin> symbols,
			Set<IAbstractPin> cablePins)
	{

		List<IPinList> schems = new ArrayList<IPinList>(symbols.keySet());

		for (int i = 0; i < schems.size(); i++) {
			IPinList schem = schems.get(i);
			IPin pin = schem.findPin(cablePin);
			IPin symbolPin = symbols.get(schem);

			if (pin != null && symbolPin != null) {
				IAbstractPin absPin = cablePin;
				if (symbols.size() > 1) {
					// keep the first symbol pin reserved to the connectivity we have.
					// any later symbols, create new connectivity pins for them
					if (i != 0) {
						CAFUtils.getInstance().getOutputWindow()
								.sendMessage("Connectivity pin can not reference many " +
												"symbol pins. New connectivity pin created for " + cablePin.getName() +
												" on Symbol "
												+ symbolPin.getConnectivity().getOwner().getName(),
										"Unshare Action", true, true);
						absPin = replicateConnectivityPin(cablePin.getOwner(), cablePin.getOwner(), cablePin, false);
					}
				}

				pin.setConnectivity(absPin);
				// first disconnect the pin from any connected objects.
				// do this because we don't know which of the new pins should be connected to the attached pins/conductors.
				// do this if there are multiple symbols
				breakPinConnections(pin);
				makeNewConnections(pin);

				((IReference) absPin).setReference(symbolPin.getConnectivity().getUID());
				PropertyHelper.updatePropertyTexts(pin, absPin);
				cablePins.add(absPin);
			}
		}
	}

	private void makeNewConnections(IPin pin)
	{
		if (m_ValidWireConnections != null && m_ValidWireConnections.contains(pin)) {
			for (IWireConductor cond : m_ValidWireConnections.get(pin)) {
				ConnectionHelper.connect(pin.getConnectivity(), cond);
			}
		}
		if (m_schemPinConnectionsToMake != null && m_schemPinConnectionsToMake.contains(pin)) {
			for (IPin p : m_schemPinConnectionsToMake.get(pin)) {
				if (pin.getConnectivity() instanceof IDevicePin && p.getConnectivity() instanceof IDevicePin) {
					((IDevicePin) pin.getConnectivity()).setConnectedDevicePin((IDevicePin) p.getConnectivity());
					((IDevicePin) p.getConnectivity()).setConnectedDevicePin((IDevicePin) pin.getConnectivity());
					ConnectionHelper.connectDevicePins(p, pin);
				}
				else {
					//associate more than one block with same shared pin list and connect a pin to one of the instance.
					//Un-share the pin list selecting both the blocks in the design tab, then the connector pin should mate with
					//the pin of the symbol that it was mating in shared state.
					//this is the first time that the pin will have a connected pin. Call the createConnectionToPin.
					//There are no prior connected pins to be cleared.
					p.getConnectivity().forceConnection(pin.getConnectivity());

					IPinList parent = (IPinList) pin.getParent();
					IPinList mate = (IPinList) p.getParent();

					parent.addAttachedObject(mate);
					mate.addAttachedObject(parent);
				}
			}
		}
	}

	private void breakPinConnections(IPin pin)
	{
		if (m_InvalidWireConnections != null && m_InvalidWireConnections.contains(pin)) {
			for (IWireConductor cond : m_InvalidWireConnections.get(pin)) {
				ConnectionHelper.disconnect(pin.getConnectivity(), cond, true);
				IJoint node = pin.getJoint();
				if (node != null) {
					for (ISegment seg : node.getAssociations(ISegment.class)) {
						if (seg.getConductor().getConnectivity() == cond) {
							NodeHelper.separateConductorAtNode(seg.getConductor(), node,
									FactoryMgr.getCommonFactory(), FactoryMgr.getSchemFactory());
						}
					}
				}
			}
		}

		if (m_schemPinConnectionsToDisconnect != null && m_schemPinConnectionsToDisconnect.contains(pin)) {
			for (IPin connectedPin : m_schemPinConnectionsToDisconnect.get(pin)) {
				if (pin.getConnectivity() instanceof IDevicePin
						&& ((IDevicePin) pin.getConnectivity()).getConnectedDevicePin() ==
						connectedPin.getConnectivity()) {
					ConnectionHelper.disconnectDeviceConnectedPin(pin, false);
				}
				else if (pin.getConnectivity().isConnected(connectedPin.getConnectivity())) {
					pin.getConnectivity().removeConnectedPin(connectedPin.getConnectivity());

					if (ConnectionHelper.isLastConnectedPinOnConnector(pin, (IPinList) connectedPin.getParent(),
							(IPinList) pin.getParent())) {
						((IPinList) connectedPin.getParent()).removeAttachedObject((IPinList) pin.getParent());
						((IPinList) pin.getParent()).removeAttachedObject((IPinList) connectedPin.getParent());
					}
				}
			}
		}
	}

	private boolean hasSymbolPinConflict(chs.cof.logical.cable.IPinList connectivity,
			Collection<IPinList> schems)
	{
		if (!connectivity.isShared()) {
			return false;
		}

		if (connectivity.canMaintainMultipleSymbols()) {
			ISharedPinList spl = connectivity.getSharedPinList();
			for (ISharedPin spin : spl.getPins()) {
				Map<IPinList, IPin> pinToSymbol = getSymbolsForSharedPin(schems, spl, spin);
				if (pinToSymbol.size() > 1) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean willNeedDisconnect(chs.cof.logical.cable.IPinList list, Collection<IPinList> schems)
	{
		boolean willDisconnect = false;
		Map<IPinList, IPin> pinlistToSymbolPin;
		ISharedPinList spl = list.getSharedPinList();
		for (ISharedPin spin : spl.getPins()) {
			IAbstractPin gpin = list.findSharedPin(spin);
			if (gpin != null) {
				pinlistToSymbolPin = getSymbolsForSharedPin(schems, spl, spin);

				if (pinlistToSymbolPin.size() > 1) {
					Set<IPin> schemPins = getSchemPinsForSharedPin(pinlistToSymbolPin, gpin);
					willDisconnect |= registerWiresToDisconnect(schemPins);
					willDisconnect |= registerPinsToDisconnect(schemPins, gpin);
				}
			}
		}

		return willDisconnect;
	}

	private boolean registerPinsToDisconnect(Set<IPin> pins, IAbstractPin apin)
	{
		Map<chs.cof.logical.cable.IPinList, SetMap<IPin, IPin>> cblPinListToconnectorPinsToSharedPins =
				new HashMap<chs.cof.logical.cable.IPinList, SetMap<IPin, IPin>>();
		SetMap<IPin, IPin> devicePinsToSharedPinsConnections = new SetMap<IPin, IPin>();

		// connector pin.
		if (apin.isMated()) {
			Collection<IAbstractPin> connectedPins = apin.getConnectedPins();
			for (IAbstractPin connectedPin : connectedPins) {
				for (IPin p : pins) {
					IPin cspin = getConnectedPin(p, connectedPin);
					if (cspin != null) {
						IAbstractPin connectivity = cspin.getConnectivity();
						SetMap<IPin, IPin> connectorPinsToSharedPins =
								cblPinListToconnectorPinsToSharedPins.get(connectivity.getOwner());
						if (connectorPinsToSharedPins == null) {
							connectorPinsToSharedPins = new SetMap<IPin, IPin>();
							cblPinListToconnectorPinsToSharedPins
									.put(connectivity.getOwner(), connectorPinsToSharedPins);
						}
						connectorPinsToSharedPins.add(cspin, p);
					}
				}
			}
		}

		IDevicePin cdpin = null;
		if (apin instanceof IDevicePin) {
			cdpin = ((IDevicePin) apin).getConnectedDevicePin();
		}

		if (cdpin != null) {
			for (IPin p : pins) {
				if (cdpin != null) {
					IPin csdpin = getConnectedPin(p, cdpin);
					if (csdpin != null) {
						devicePinsToSharedPinsConnections.add(csdpin, p);
					}
				}
			}
		}

		for (chs.cof.logical.cable.IPinList cblPinlist : cblPinListToconnectorPinsToSharedPins.keySet()) {
			SetMap<IPin, IPin> connectorPinsToSharedPins = cblPinListToconnectorPinsToSharedPins.get(cblPinlist);
			// get the number of all shem pins connected to the connected connectivity pin
			boolean valid = connectorPinsToSharedPins.keySet().size() <= 1;

			for (IPin p : connectorPinsToSharedPins.keySet()) {
				for (IPin schemp : connectorPinsToSharedPins.get(p)) {
					if (valid) {
						m_schemPinConnectionsToMake.add(schemp, p);
					}
					else {
						m_schemPinConnectionsToDisconnect.add(schemp, p);
					}
				}
			}
		}

		if (cdpin != null) {
			for (IPin p : devicePinsToSharedPinsConnections.keySet()) {
				boolean valid = devicePinsToSharedPinsConnections.keySet().size() <= 1;
				for (IPin schemp : devicePinsToSharedPinsConnections.get(p)) {
					if (valid) {
						m_schemPinConnectionsToMake.add(schemp, p);
					}
					else {
						m_schemPinConnectionsToDisconnect.add(schemp, p);
					}
				}
			}
		}

		return false;
	}

	private IPin getConnectedPin(IPin p, IAbstractPin apin)
	{
		if (p == null || apin == null) {
			return null;
		}
		if (p.getConnectivity() instanceof IDevicePin && apin instanceof IDevicePin) {
			IPin cspin = ConnectionHelper.getConnectedDevicePin(p);
			if (cspin != null && cspin.getConnectivity() == apin) {
				return cspin;
			}
		}
		else {
			ConnectionHelper chelper = new ConnectionHelper((IPinList) p.getParent());
			chelper.examineConnectivity(p, DiagramHelper.getDiagram(p), false);
			IPin cspin = chelper.getConnectedPin(p);
			if (cspin != null && cspin.getConnectivity() == apin) {
				return cspin;
			}
		}
		return null;
	}

	private boolean registerWiresToDisconnect(Set<IPin> pins)
	{
		SetMap<IWireConductor, IPin> conductorToSchemPinConnections = new SetMap<IWireConductor, IPin>();
		for (IPin gspin : pins) {
			for (Object obj : gspin.getConductors()) {
				IConductor cond = (IConductor) obj;
				if (cond.getConnectivity() instanceof IWireConductor) {
					conductorToSchemPinConnections.add((IWireConductor) cond.getConnectivity(), gspin);
				}
			}
		}

		for (IWireConductor cond : conductorToSchemPinConnections.keySet()) {
			// if the conductor is connected to more than two schematic pins where the number of the conductor pins is only one
			// it would be invalid
			// if the conductor is connected to two schematic pins where the number of conductor pins is 1, it would be valid
			// if the conductor is connected to one schematic pin where the number of conductor pins is 2, it would be valid
			// if the conductor is connected to two schematic pins where the number of conductor pins is 2, it would be invalid
			boolean validConnection =
					!(conductorToSchemPinConnections.getSet(cond).size() == 2 && cond.getPinSet().size() == 2
							|| conductorToSchemPinConnections.getSet(cond).size() > 2 && cond.getPinSet().size() == 1);

			for (IPin p : conductorToSchemPinConnections.get(cond)) {
				if (validConnection) {
					m_ValidWireConnections.add(p, cond);
				}
				else {
					m_InvalidWireConnections.add(p, cond);
				}
			}
		}
		return !m_InvalidWireConnections.isEmpty();
	}

	private Set<IPin> getSchemPinsForSharedPin(Map<IPinList, IPin> pinlistToSymbolPin, IAbstractPin gpin)
	{
		Set<IPin> schemPins = new HashSet<IPin>();
		if (gpin != null) {
			for (IPinList pl : pinlistToSymbolPin.keySet()) {
				IPin gspin = pl.findPin(gpin);
				if (gspin != null) {
					schemPins.add(gspin);
				}
			}
		}
		return schemPins;
	}

	@Override protected void buildOldUsagesMap()
	{
		super.buildOldUsagesMap();
		IDesignWideUsageMgr dwum = design.getDesignWideUsageMgr();
		for (IDesignSharedUsage usage : dwum.getUsages(cablePinList)) {
			if (usage instanceof IDesignSharedPinListUsage) {
				ISymbolDef symDef = ((IDesignSharedPinListUsage) usage).getSymbol();
				if (symDef != null) {
					IUID uid = usage.getDiagramObjectUID();
					oldSymDefMap.put(uid, symDef);
					oldSymInstNumMap.put(uid, ((IDesignSharedPinListUsage) usage).getSymbolInstanceNumber());
				}
			}
		}
	}

	@Override protected void unshareThisPinList()
	{
		super.unshareThisPinList();
		unshareDeviceConnectors(cablePinList);
	}

	protected Set<ILogicObject> getLockableCableObjects()
	{
		Set<ILogicObject> lockables = new HashSet<>();
		lockables.addAll(super.getLockableCableObjects());
		if (cablePinList instanceof IDevice) {
			IDevice device = (IDevice) cablePinList;
			for (IDeviceConnector devConn : device.getDeviceConnectors()) {
				lockables.add(devConn);
				IBackshell backshell = devConn.getBackshell();
				if (backshell != null) {
					lockables.add(backshell);
				}
			}
		}
		return lockables;
	}

	private void unshareDeviceConnectors(chs.cof.logical.cable.IPinList theCablePinList)
	{
		if (theCablePinList instanceof IDevice) {
			for (IDeviceConnector devConn : ((IDevice) theCablePinList).getDeviceConnectors()) {
				for (IAbstractPin pin : devConn.getPins()) {
					unsharePin(pin);
				}
				// If the device connector has a backshell, unshare its termination pins and then the backshell itself.
				unshareBackShells(devConn);
				// COG: as replicator.replicateCopyableObject(...) will affect pins
				// we need to unshare the pins first.
				unsharePinList(devConn);
			}
		}
	}

	@Override protected void copyInfoFromSharedToLogicObject(ISharedPinList spl)
	{
		super.copyInfoFromSharedToLogicObject(spl);
		if (cablePinList instanceof IDevice) {
			Replicator.copyFootprintRef(spl, (IFootprintable) cablePinList);
		}
	}

	@Override protected void handleUnplacedLogicObjects(ISharedPinList spl,
			Map<IAbstractPin, ISharedPin> connectivityToSharedPin)
	{
		//Incase of unsharing a unplaced device, symRef is not getting copied to new unshared cable. Hence, do it explicitly.
		//dts0100887749 VALIDATION FAILURE: InternalLink - LINK2 end pin should not be null
		if (schemPinLists.isEmpty() && cablePinList.canMaintainMultipleSymbols()) {
			ISymbolDefIterator symDefItr = spl.getSymbols();
			while (symDefItr.hasNext()) {
				ISymbolDef symDef = symDefItr.getNext();
				ISymbolRef symref = spl.getSymbolRef(symDef.getUID(), 0);
				cablePinList.addSymbolRefIfCanMaintainMultipleSymbols(symref);
			}

			for (IAbstractPin cablePin : cablePinList.getPins()) {
				ISharedPin spin = connectivityToSharedPin.get(cablePin);
				if (spin != null) {
					IPin symPin = spl.getSymbolPin(spin);
					if (symPin != null) {
						IUID ref = symPin.getConnectivity().getUID();
						((IReference) cablePin).setReference(ref);
					}
				}
			}
		}
	}

	protected void doPinListSpecificProcessing(ISharedPinList spl,
			Map<IAbstractPin, ISharedPin> connectivityToSharedPin, IPinList schemPinList)
	{
		// For shared pinlists the symbol is stored on the schem object as each instance of the connectivity object
		// could use a different schem. When we unshare, this needs to be put on the unshared connectivity object.
		// Note: if we are unsharing all schematics and using the old connectivity, we don't need to do anything
		// for analysis blocks since they already exists on the connectivity.
		setSymbolRef(schemPinList, spl, cablePinList, true);

		// this code removes Analysis blocks from the device if it doesn't reference a symbol
		// however, we want it to keep the block if this block is stating an analysis model without a symbol
		// such case may happen when attaching an analysis model to a parameterized device.
		if (cablePinList instanceof IDevice && cablePinList.getSymbolReferences().isEmpty()) {
			Collection<IBlock> blocks = ((IDevice) cablePinList).getBlocks();
			for (IBlock blk : blocks) {
				// if the device doesn't reference any symbols. remove all blocks that were built for symbols.
				if (blk.getBlockRefID() == null && StringUtils.isBlank(blk.getAnalysisModel())) {
					((IDevice) cablePinList).removeBlock(blk);
				}
			}
		}

		// for the unshared instance, the symbol pin references need to be on the connectivity pins and not the schem pins
		// dts0100676490, at this stage the connectivity pins will not reference shared pins that are required
		// by copySymbolPinRefs. So we need the connectivityToSharedPin map to link between the connectivity
		// pins and the shared ones.
		copySymbolPinRefs(schemPinList, spl, connectivityToSharedPin);
	}

	protected void updateInternalConnectivity(IPinList schemPinlist)
	{
		if (!(schemPinlist.getConnectivity() instanceof IDevice)) {
			return;
		}
		IDevice device = (IDevice) schemPinlist.getConnectivity();
		Collection<IUID> blocksToRemove = getBlocksToRemove(device);
		if (blocksToRemove.isEmpty()) {
			return;
		}
		for (IInternalLink internalLink : device.getInternalLinkCollection()) {
			IUID blockRef = internalLink.getBlockRef();
			if (blocksToRemove.contains(blockRef)) {
				internalLink.delete();
			}
		}

		IInternalPinIterator internalPins = device.getInternalPins();
		for (IInternalPin internalPin : internalPins) {
			IUID blockRef = internalPin.getBlockRef();
			if (blocksToRemove.contains(blockRef)) {
				internalPin.delete();
			}
		}

		for (IAbstractPin pin : device.getPinCollection()) {
			IUID blockRef = pin.getBlockRef();
			if (blocksToRemove.contains(blockRef)) {
				pin.setBlockRef(null);
			}
		}
		IUIDMgr uidMgr = UIDMgr.getUIDMgr();
		for (IUID blockuid : blocksToRemove) {
			IUIDObject object = uidMgr.getObject(blockuid);
			IBlock block = object instanceof IBlock ? (IBlock) object : null;
			if (block != null) {
				device.removeBlock(block);
				block.delete();
			}
		}
	}

	private static Collection<IUID> getBlocksToRemove(@NotNull IDevice device)
	{
		SetMap<IUID, IUID> logicVsSymbol = new SetMap<>();
		for (IBlock block : device.getBlocks()) {
			logicVsSymbol.add(block.getBlockRefID(), block.getUID());
		}
		Set<IUID> deviceSymbolRefs = device.getSymbolReferences().stream().map(symbolRef -> symbolRef.getSymbolUID())
				.collect(Collectors.toSet());
		if (deviceSymbolRefs.isEmpty()) {
			return logicVsSymbol.items();
		}
		else {
			List<IUID> blockUIDsToRemove = new ArrayList<>();
			for (IBlock block : device.getBlocks()) {
				if (block.getIsSymbol()) {
					IUID symDefUID = block.getBlockRefID();
					if (!deviceSymbolRefs.contains(symDefUID)) {
						blockUIDsToRemove.add(block.getUID());
						ISymbolDef symdef = SymbolUtils.getSymbolDef(symDefUID, true);
						if (symdef != null) {
							for (chs.cof.symbol.IBlock symbolBlock : symdef.getBlocks()) {
								blockUIDsToRemove.addAll(logicVsSymbol.pullReadOnlySafeSet(symbolBlock.getUID()));
							}
						}
					}
				}
			}
			return blockUIDsToRemove;
		}
	}

	private static void copySymbolPinRefs(IPinList thePinList, ISharedPinList spl, Map<IAbstractPin, ISharedPin> links)
	{
		for (IAbstractPin cablePin : thePinList.getCablePins(true)) {
			ISharedPin spin = links.get(cablePin);
			if (spin != null) {
				copySymbolPinRefs(thePinList, spl, spin, cablePin);
			}
		}
	}
}