/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2014-2025 Siemens
 */
package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.caf.IOutputWindow;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.helpers.CAFSharedUpdater;
import chs.caplets.logic.actions.ghc.GenerateHarnessConnActionHelper;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.ICavitiesOwner;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedObjectMgr;
import chs.cof.logical.shared.ISharedObjectModificationObserver;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinListMgr;
import chs.cof.project.IProject;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.IBlockIterator;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.ISymbolRef;
import chs.cog.ICOGLockable;
import chs.common.IProjectPreferenceMgr;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.dev.utils.DumpHelper;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.ResourceMgr;
import chs.utility.ProjectHelper;
import chs.utility.SymbolUtils;
import chs.utility.helpers.IPinListShareContext;
import chs.utility.helpers.IPinListShareHelper;
import chs.utility.helpers.PinListHelper;
import chs.utility.helpers.PinListShareContext;
import chs.utility.helpers.PinListShareHelper;
import chs.utility.helpers.SharePinListLockHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractSharePinListActionHelper implements IShareActionHelper
{

	protected boolean m_fromSymbol;
	@Nullable protected chs.cof.logical.cable.IPinList cablePinList;
	@Nullable protected IPinList m_pinList;
	@Nullable private IPinList m_mate;
	private ISharedPinListMgr m_splmgr;
	private boolean m_newSharedObject = true;
	@Nullable private GenerateHarnessConnActionHelper mHarnessGenerationHelper;
	private IProject m_project;
	@NotNull protected ILogicDesign m_design;
	@Nullable protected ISchemDiagram m_diagram;
	@Nullable private IUID m_sharedObjectUid = null;

	protected AbstractSharePinListActionHelper(@NotNull IProject project, @NotNull ILogicDesign design,
			@Nullable ISchemDiagram diagram, boolean fromSymbol)
	{
		m_project = project;
		m_splmgr = m_project.getSharedPinListMgr();
		m_design = design;
		m_diagram = diagram;
		m_pinList = null;
		m_mate = null;
		m_fromSymbol = fromSymbol;
		mHarnessGenerationHelper = m_diagram == null ? null : new GenerateHarnessConnActionHelper(m_diagram);
	}

	@Override
	@NotNull public IActionEnum setup(@NotNull BaseShareActionOperands operands, @Nullable String dialogTitle,
			@Nullable ISchemDiagram diagram)
	{
		// connectivity pinlist may be specified explicitly in the operands (FEAT13040)
		cablePinList = operands.getCablePinList();
		if (cablePinList == null) {
			return IActionEnum.eCanceled;
		}

		ILogicDesign logicDesign = cablePinList.getLogicDesign();
		if (logicDesign != null) {
			if (!SharePinListLockHandler.attemptLockOnSourceImpactedObjectForShare(cablePinList)) {
				return IActionEnum.eCanceled;
			}
		}

		if (checkBackshellsForDuplicatePinNames(cablePinList)) {
			reportDuplicatePins();
			return IActionEnum.eCanceled;
		}

		m_diagram = diagram;

		//SP2004: dts0101401523:[CH] java.lang.NullPointerException at chs.caplets.logic.actions.shared.EditSharedPinListModel.determineMaxPins(EditSharedPi
		final IUIDObject target = operands.target;
		m_pinList = target != null ? UIDMgr.getObjectOfType(target.getUID(), IPinList.class) : null;
		IPinList mate = operands.mate;
		m_mate = mate != null ? UIDMgr.getObjectOfType(mate.getUID(), IPinList.class) : null;

		// don't attempt to share a connectivity device with a missing symbol
		if (cablePinList instanceof IDevice) {
			IDevice device = (IDevice) cablePinList;
			if (!verifySymbolExists(device)) {
				return IActionEnum.eCanceled;
			}
		}

		// Update (or possibly create) the Shared PinList Manager.
		if (m_splmgr == null) {
			IUID spmuid = FactoryMgr.createUID();
			m_splmgr = FactoryMgr.getSharedFactory().createSharedPinListManagerBasedOnSetup(spmuid);
			m_project.setSharedPinListMgr(m_splmgr);
			//
			// Create it!
			//
			m_splmgr.save();
		}

		if (!isBulkPromotion()) {
			CAFSharedUpdater sr = new CAFSharedUpdater(m_project, CAFUtils.getInstance().getWindowMgr());
			sr.updateSharedPinListMgr();
		}

		ISymbolRef symRef = cablePinList.getSymbolRef();

		if (cablePinList.canMaintainMultipleSymbols()) {
			for (ISymbolRef ref : cablePinList.getSymbolReferences()) {
				if (!verifySymbolRef(ref)) {
					return IActionEnum.eCanceled;
				}
			}
		}
		else if (symRef != null) {
			// Check the symbol we are about to share is upto date with resepect to the actual symbol.
			// Also make a specific check for the number of pins as previous release may allow the timestamps to be
			// updated but the pins not be added/removed.  This can't happen anymore, but we should be aware of legacy
			// data - and it's no big deal to performt the check.
			if (!verifySymbol(symRef)) {
				return IActionEnum.eCanceled;
			}
		}

		return postSetup(dialogTitle);
	}

	private boolean checkBackshellsForDuplicatePinNames(@NotNull chs.cof.logical.cable.IPinList pinList)
	{
		Set<IBackshell> backshellsToCheck = new HashSet<>();
		if (pinList instanceof IGenericInlineConnector){
			CollectionUtils.addIfNonNull(backshellsToCheck, ((ICavitiesOwner) pinList).getBackshell());
			for (IConnector mate : ((IConnector) pinList).getMates()) {
				CollectionUtils.addIfNonNull(backshellsToCheck, mate.getBackshell());
			}
		}
		else if (pinList instanceof IConnector) {
			CollectionUtils.addIfNonNull(backshellsToCheck, ((ICavitiesOwner) pinList).getBackshell());
		}
		for (IBackshell backshellToCheck : backshellsToCheck) {
			if (checkBackshellForDuplicatePinnames(backshellToCheck)) {
				return true;
			}
		}

		return false;
	}

	private boolean checkBackshellForDuplicatePinnames(@NotNull IBackshell backshell)
	{
		Set<String> pinNames =
				backshell.getPinCollection().stream().map(pin -> pin.getName()).collect(Collectors.toSet());
		return  (pinNames.size() != backshell.getNumPins());
	}

	protected abstract IActionEnum postSetup(@Nullable String dialogTitle);

	/**
	 * Verfify that a symbol ref on the cable pinlist to be shared is valid.
	 *
	 * @param ref The symbol ref
	 * @return true if OK to share, false otherwise
	 */
	protected boolean verifySymbolRef(ISymbolRef ref)
	{
		// TODO unit tests : code in this method needs to be brought under test by further refactoring
		//                   this initial refactoring was only done to restore SymbolCreateSharedTest
		//                   after delivery of this new functionality with code under test mangled with DB and UI
		//                   duplication with the similar verifySymbol method should probably also be removed
		ISymbolDef def = SymbolUtils.getSymbolDef(ref);
		if (def == null) {
			reportSymbolNotAvailable();
			return false;
		}
		if (ref.getTimestamp() < def.getServerTimeModified()) {
			// we want to make sure that all symbol pins exists in the connectivity.
			assert cablePinList != null;
			for (IBlockIterator bitr = def.getBlocks(); bitr.hasNext(); ) {
				IBlock b = bitr.getNext();
				Set<IUID> blockPins = new HashSet<IUID>();
				for (IGenericPin gpin : b.getConnectivity().getPins()) {
					blockPins.add(gpin.getUID());
				}

				for (IGenericPin gpin : cablePinList.getPins()) {
					blockPins.remove(gpin.getUID());
				}

				if (!blockPins.isEmpty() && blockPins.size() != b.getConnectivity().getNumPins()) {
					reportSymbolOutOfDate();
					return false;
				}
			}
		}
		return true;
	}

	protected abstract void reportSymbolOutOfDate();

	protected abstract void reportSymbolNotAvailable();

	protected abstract void reportDuplicatePins();

	protected boolean verifySymbolExists(chs.cof.logical.cable.IPinList device)
	{
		// I think this is done elsewhere for all pinlist types anyway?
		// semantics of the if statement look strange but keeping it from earlier refactoring
		ISymbolRef ref = device.getSymbolRef();
		if (ref != null && SymbolUtils.getSymbolDef(ref) == null) {
			reportSymbolNotAvailable();
			return false;
		}
		return true;
	}

	protected boolean verifySymbol(ISymbolRef symRef)
	{
		ISymbolDef def = SymbolUtils.getSymbolDef(symRef);
		if (def == null) {
			reportSymbolNotAvailable();
			return false;
		}
		//FEAT00013786: stack pins are not expected in symbols
		if ((symRef.getTimestamp() < def.getServerTimeModified()) ||
				(m_pinList != null && m_pinList.getPins().size() != def.getNumPins())) {
			//dts0100620861:Unable to share instance of symbol which is part of a composite symbol
			Boolean blockMatch = false;
			for (IBlockIterator bitr = def.getBlocks(); bitr.hasNext(); ) {
				IBlock b = bitr.getNext();
				if (m_pinList != null && b.equals(m_pinList.getBlock())) {
					if (b.getNumPins() == m_pinList.getPins().size()) {
						blockMatch = true;
						break;
					}
				}
			}
			if (!blockMatch) {
				reportSymbolOutOfDate();
				return false;
			}
		}
		else if (SymbolUtils.compositeSymbolOutOfDate(def)) {
			reportCompositeSymbolOutOfDate();
			return false;
		}
		return true;
	}

	protected abstract void reportCompositeSymbolOutOfDate();

	@NotNull protected static List<String> reportDeviceSharedConnectorsDeletion(@NotNull IPinListShareHelper shareHelper)
	{
		List<String> deletionReport = new ArrayList<String>();
		Set<String> deletedDeviceConnectors = ((PinListShareHelper) shareHelper).getDeletedDeviceConnectors();
		if (deletedDeviceConnectors != null && !deletedDeviceConnectors.isEmpty()) {
			IOutputWindow outputWindow = CAFUtils.getInstance().getOutputWindow();
			DumpHelper dumpHelper = new DumpHelper(outputWindow);
			for (String connector : deletedDeviceConnectors) {
				String message = ResourceMgr.getString(AbstractSharePinListActionHelper.class,
						"AbstractSharePinListActionHelper.DSCDeletion.text", connector);
				dumpHelper.printMsg(message);
				deletionReport.add(message);
			}
		}
		return deletionReport;
	}

	public boolean doEdit()
	{
		Set<ISharedPinList> ownersPinListSet = new HashSet<ISharedPinList>();
		Collection<ISharedPin> connectedSpinSet = connectedPinsToMakeReusable();
		for (ISharedPin connectedSpin : connectedSpinSet) {
			ownersPinListSet.add(connectedSpin.getOwner());
		}
		boolean initEnableRefresh = true;
		if (m_project != null && m_project.getSharedPinListMgr() != null) {
			initEnableRefresh = m_project.getSharedPinListMgr().isRefreshEnabled();
		}
		IPinListShareHelper shareHelper;
		boolean success = false;
		ISharedObjectMgr sharedMgr = null;
		try {
			for (ISharedPinList pinlist : ownersPinListSet) {
				LockUpdateHelper lockHelper = new LockUpdateHelper((ICOGLockable) pinlist);
				if (!lockHelper.lockAndRefresh()) {
					return false;
				}
			}
			IPinListShareContext pinListShareContext = createContext();
			Runnable resolveConflict = getConflictResolver(new SharedObjectModificationObserver(), pinListShareContext);
			IShareActionChangeReporter shareActionChangesReporter = getShareActionChangeReporter(pinListShareContext);
			shareHelper = new PinListShareHelper(pinListShareContext, shareActionChangesReporter);

			success = shareHelper.share(() -> {
			}, resolveConflict);
			reportDeviceSharedConnectorsDeletion(shareHelper);

			if (success) {
				Collection<IPinList> pendingDeletions =
						((PinListShareHelper) shareHelper).getPendingSchemPinListDeletions();
				shareActionChangesReporter.reportChanges();

				if (PinListHelper.isHarnessFootprintedAndAllowAutoCreation(m_pinList)
						|| PinListHelper.isHarnessFootprintedAndAllowAutoCreation(cablePinList, m_design)) {
					// dts0100626345-AssertionError while sharing a device with auto GHC and device has multiple footprint connector
					// if we are going to generate harness connectors, then we first need to disable refreshing the shared pinlist manager
					// otherwise the new shared objects will be deleted since the data is not flushed to DB untill we exit
					// the current transaction boundary i.e. after finishing the action.
					sharedMgr = pinListShareContext.getProject().getSharedPinListMgr();
					// we are generating harness connectors, so we don't like to refresh the manager in order to
					// reserve the shared data we have already created after the share.
					initEnableRefresh = sharedMgr.isRefreshEnabled();
					sharedMgr.setRefreshEnabled(false);
				}
				//
				// For ALL the pinlists that have pending deletions, clear them out and delete them.
				//
				if (!pendingDeletions.isEmpty()) {
					//dts0100598513: Deferred deletion of schempinlist
					//Get the existing connectors before deleting
					for (IPinList pl : pendingDeletions) {
						pl.delete();
					}
				}
				//Probably if this is a composite symboled device, then the schematic/connectivity has changed!
				//Check if connectivity is not null
				IPinList newSchemPinList =
						cablePinList != null ? m_pinList : pinListShareContext.getNewTopLevelPinLst();
				//dts0100598513: If composite symbol get the primary pinlist
				if (pinListShareContext.getNewTopLevelPinLst() != null) {
					newSchemPinList = pinListShareContext.getNewTopLevelPinLst();
				}
				//melmorsy - FEAT12331
				//Regenerate harness connectors for a device upon sharing
				// TODO jacobt FEAT13040 : this should be done for all schem instances - inside the Share code
				if (PinListHelper.isHarnessFootprintedAndAllowAutoCreation(newSchemPinList)
						|| PinListHelper.isHarnessFootprintedAndAllowAutoCreation(cablePinList, m_design)) {
					if (newSchemPinList != null && newSchemPinList.getConnectivity() != null) {
						ISharedPinList sharedDevice = (ISharedPinList) newSchemPinList.getSharedObject();
						if (sharedDevice != null) {
							doGHC(sharedDevice, m_diagram);
						}
					}
					else if (cablePinList != null && cablePinList.getSharedPinList() != null) {
						// dts0100650130-Call home when undo after GHC generation with shared devices
						// we may not have a schematic pin list.
						doGHC(cablePinList.getSharedPinList(), m_diagram);
					}
					else if (pinListShareContext.getSharedPinList() != null) {
						// dts0100652237-Scrubbing occurs when "ShareInto" existing shared instance of composite symbol
						doGHC(pinListShareContext.getSharedPinList(), m_diagram);
					}
				}
			}
		}
		finally {
			if (sharedMgr != null) {
				// reset the enableness of the shared pinlist manager
				sharedMgr.setRefreshEnabled(initEnableRefresh);
			}
			for (ISharedPinList pinlist : ownersPinListSet) {
				LockUpdateHelper lockHelper = new LockUpdateHelper((ICOGLockable) pinlist);
				if (success) {
					lockHelper.flushAndUnlock(true);
				}
				else {
					lockHelper.unlock();
				}
			}
		}

		m_newSharedObject = shareHelper.isNewSharedPinList();
		m_sharedObjectUid = shareHelper.getSharedPinListUID();
		if (!success) {
			final IPinListShareHelper.ErrorCode errCode = shareHelper.getErrorCode();
			assert errCode != null;
			reportError(errCode);
		}
		return success;
	}

	private void doGHC(@NotNull ISharedPinList sharedDevice, @Nullable ISchemDiagram diagram)
	{
		if (mHarnessGenerationHelper == null || diagram == null) {
			return;
		}
		//TODO : We need to decouple GHC from diagram
		mHarnessGenerationHelper.setDiagram(diagram);
		mHarnessGenerationHelper.generateHarnessConnectorsForSharedDevice(sharedDevice, cablePinList);
	}

	@NotNull protected abstract Runnable getConflictResolver(@NotNull ISharedObjectModificationObserver observer,
			@NotNull IPinListShareContext pinListShareContext);

	protected Collection<ISharedPin> connectedPinsToMakeReusable()
	{
		final IPinListShareContextProvider provider = getPinlistShareContextProvider();
		return provider != null ? provider.getConnectedPinsToMakeReusable().values() : Collections.emptyList();
	}

	protected IPinListShareContext createContext()
	{
		IPinListShareContext pinListShareContext = getPinListShareContext();
		pinListShareContext.setDiagram(m_diagram);
		pinListShareContext.setFromSymbol(m_fromSymbol);
		pinListShareContext.setPinListMateToShare(m_mate);
		pinListShareContext.setPinListToShare(m_pinList);
		pinListShareContext.setCablePinListToShare(cablePinList);

		populateFromContextProvider(pinListShareContext);
		return pinListShareContext;
	}

	@NotNull protected IPinListShareContext getPinListShareContext()
	{
		return new PinListShareContext(isBulkPromotion());
	}

	protected boolean isBulkPromotion()
	{
		return false;
	}

	private void populateFromContextProvider(@NotNull IPinListShareContext pinListShareContext)
	{
		final IPinListShareContextProvider shareContextProvider = getPinlistShareContextProvider();
		if (shareContextProvider == null) {
			return;
		}

		pinListShareContext.setInstanceToSharedMap(shareContextProvider.getInstanceToSharedMap());
		pinListShareContext.setPreserveInternalConnectivity(shareContextProvider.preserveInternalConnectivity());
		//todo a warning may be shown here to tell the user that there are some pins that will be shared. those pins exist in the symbol definition but not on the design.
		pinListShareContext.setPlugMapInfo(shareContextProvider.getPlugMapInfo());
		pinListShareContext.setProjectPreferenceMgr(getProjectPreferences());
		pinListShareContext.setReusablePinProxies(shareContextProvider.getReusablePins());
		pinListShareContext.setSharedPinList(shareContextProvider.getSharedPinList());
		pinListShareContext.setSharedPinListMateName(shareContextProvider.getSharedObjectMateName());
		pinListShareContext.setSharedPinListMateNameGenerated(shareContextProvider.isSharedMateNameGenerated());
		pinListShareContext.setSharedPinListMateRevision(shareContextProvider.getSharedObjectMateRevision());
		pinListShareContext.setSharedPinListName(shareContextProvider.getSharedPinListName());
		pinListShareContext.setSharedPinListRevision(shareContextProvider.getSharedPinListRevision());
		pinListShareContext.setSharedPinListNameGenerated(shareContextProvider.isSharedNameGenerated());
		pinListShareContext.setSharedToInstanceMap(shareContextProvider.getSharedToInstanceMap());
		pinListShareContext.setAnalysisModel(shareContextProvider.getAnalysisModel());
		pinListShareContext.setAnalysisFunctionRealiser(shareContextProvider.getAnalysisFunctionRealiser());
		pinListShareContext.setOverriddenAnalysisInterfaces(shareContextProvider.getOverriddenAnalysisInterfaces());
		pinListShareContext.setOverriddenAnalysisFailureModes(shareContextProvider.getOverriddenAnalysisFailureModes());
		pinListShareContext.setPreservePinToPinConnections(!shareContextProvider.reusablePinErrors());
		pinListShareContext.setConnectedPinsToMakeReusable(shareContextProvider.getConnectedPinsToMakeReusable());
		pinListShareContext.setModularConnectorSharedNames(shareContextProvider.getModularConnectorToSharedNamesMap());
		pinListShareContext.setModularConnectorSharedNameGenerated(
				shareContextProvider.getModularConnectorToSharedNameGeneratedMap());
		pinListShareContext.setSharedDomains(
				shareContextProvider.getSharedDomains());

		if (shareContextProvider.getSymbolDef() == null && cablePinList != null) {
			for (ISymbolRef symbolRef : cablePinList.getSymbolReferences()) {
				ISymbolDef def = SymbolUtils.getSymbolDef(symbolRef);
				if (def != null) {
					pinListShareContext.addSymbolDef(def);
				}
			}
		}
		else {
			pinListShareContext.addSymbolDef(shareContextProvider.getSymbolDef());
		}
	}

	@Nullable protected IProjectPreferenceMgr getProjectPreferences()
	{
		return ProjectHelper.getProjectPreferences(m_project);
	}

	@Nullable protected abstract IPinListShareContextProvider getPinlistShareContextProvider();

	protected abstract void reportError(@NotNull IPinListShareHelper.ErrorCode errorCode);

	public void cleanup()
	{
		cablePinList = null;
		m_pinList = null;
		m_mate = null;
		m_newSharedObject = true;
		m_sharedObjectUid = null;
		if (m_splmgr != null && !isBulkPromotion()) {
			m_splmgr.unlock();
		}
	}

	public boolean isNewSharedObject()
	{
		return m_newSharedObject;
	}

	@Override @Nullable public IUID getSharedObjectUID()
	{
		return m_sharedObjectUid;
	}

	@Override public boolean isShareInto()
	{
		assert getPinlistShareContextProvider() != null;
		ISharedObject shareIntoObject = getPinlistShareContextProvider().getSharedPinList();
		return shareIntoObject != null;
	}

	@NotNull
	protected abstract IShareActionChangeReporter getShareActionChangeReporter(
			@NotNull IPinListShareContext pinListShareContext);
}