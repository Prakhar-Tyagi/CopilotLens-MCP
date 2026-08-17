/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.IUpdateableAction;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.shared.actions.SymDeviceTemporaryPlaceHolderCreationHelper;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.icd.IDeviceICD;
import chs.cof.logical.IECAttributeResolver;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.Backshell;
import chs.cof.logical.cable.BackshellTermination;
import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IBaseDevice;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IDeviceOwnedConnector;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedBackshell;
import chs.cof.logical.shared.ISharedBackshellOwner;
import chs.cof.logical.shared.ISharedBackshellTermination;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.SharedPinListHelper;
import chs.cof.project.IProject;
import chs.cof.project.naming.HwmNameSpaceKey;
import chs.cof.project.naming.INameMgr;
import chs.cof.project.naming.INameSpace;
import chs.cof.project.naming.RegularNameSpaceKey;
import chs.cof.symbol.ISymbolRef;
import chs.cofUtils.logical.concurrency.LogicConcurrencyLogger;
import chs.cofUtils.parameterized.BackshellGraphicsRebuilder;
import chs.cofUtils.parameterized.Generator;
import chs.common.INamedPropertiedObject;
import chs.common.IObjectFilter;
import chs.common.IProjectPreferenceMgr;
import chs.common.IPropertiedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.ctf.caf.utils.CTFLockUpdateHelper;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.caf.utils.PinProxy;
import chs.system.UIDMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.StringUtils;
import chs.utility.AssemblyUtils;
import chs.utility.DiagramHelper;
import chs.utility.ICDUtils;
import chs.utility.ResizeHelper;
import chs.utility.helpers.CompositePinConnectivityFinder;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.PinPlaceholderProviderForSymbolledDeviceInMove;
import chs.utility.helpers.PropertyTemplateHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public abstract class AbstractBackshellAction extends ControllerActionRT
		implements ICtxMenuProvider, MouseListener, MouseMotionListener, IAddBackshellController
{

	@Nullable protected IPinList m_pinlist;

	protected AddBSTerminationActionHelper m_addPinActionHelper; // Delegate action after shared device has been created

	protected Set<IUIDObject> newObjects = new HashSet<IUIDObject>();

	protected List<IPinProxy> m_bterms = new ArrayList<>();

	protected String m_backshellName;

	@Nullable protected ISymbolRef m_backshellSymbol;

	private String m_ctxCommand;

	protected IConnector conn;

	@Nullable protected ISharedPinList spl;

	private Collection<INamedPropertiedObject> existingBackehellTerminations = new HashSet<>();

	@Nullable private INameSpace backshellNameSpace = null;

	@Nullable private IBackshell temporaryBackshell;

	protected Set<INamedPropertiedObject> temporaryTerminations = new HashSet<>();

	private int backshellNameIndexOriginal = 1;

	@Nullable private INameSpace backshellTerminationNameSpace;

	private int terminationNameIndexOriginal = 1;

	protected AbstractBackshellAction(ICapletController controller)
	{
		super(controller);

		setUpActionHelper();
	}

	protected void setUpActionHelper()
	{
		m_addPinActionHelper = new AddBSTerminationActionHelper(this);
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (isEnabled() && getOperand(selections) != null) {
			String shortDesc = getValue(Action.SHORT_DESCRIPTION);
			if (m_ctxCommand == null || !m_ctxCommand.equalsIgnoreCase(shortDesc)) {
				// Make a private copy for command name
				m_ctxCommand = shortDesc;
			}
			container.add(new ActionEntry(Objects.requireNonNull(getActionUI()), m_ctxCommand));
		}
	}

	public String getValue(String actionAttribute)
	{
		return (String) Objects.requireNonNull(getActionUI()).getValue(actionAttribute);
	}

	protected void createSharedTeminations()
	{
		//
		// Create the terminations.
		//
		for (int idx = 0; idx < m_bterms.size(); idx++) {
			IPinProxy o = m_bterms.get(idx);
			boolean doCreate = false;
			ISharedBackshellTermination sterm = (ISharedBackshellTermination) o.getSharedPin();
			String stermName = o.getName();
			if (sterm == null && !StringUtils.isBlank(stermName)) {
				//
				// create...
				//
				sterm = createnewSharedBackshellTermination();
				autoAssignPropertiesFromCurrentProject(sterm);
				setNameIfDifferent(sterm, stermName);
				doCreate = true;
				//
			}
			else if (sterm != null) {

				// if we have a shared backshell termination then let's check the connectivity
				// to see if this is already referenced. If so then we re-use that connectivity
				IBackshellTermination bt = findTerminationReferencingSharedPin(conn.getBackshell(), sterm);
				if (bt != null) {
					// Swap in the connectivity backshell termination we've found...
					m_bterms.set(idx, clonePinProxy(o, bt));
				}
				else {
					// If not, create one...
					doCreate = true;
				}
			}
			if (doCreate) {
				IBackshellTermination bt = createNewBackshellTermination();
				bt.setSharedPin(sterm);
				m_bterms.set(idx, clonePinProxy(o, bt));
			}
		}
	}

	@NotNull private IPinProxy clonePinProxy(@NotNull IPinProxy o, @NotNull IBackshellTermination bt)
	{
		IPinProxy pinProxy = new PinProxy(bt);
		chs.cof.logical.cable.IPinList cablePinList = o.getCablePinList();
		if (cablePinList != null) {
			pinProxy.setCablePinList(cablePinList);
		}
		return pinProxy;
	}

	private void autoAssignPropertiesFromCurrentProject(IPropertiedObject propertiedObject)
	{
		final IProject currentProject = CAFUtils.getInstance().getCurrentProject();
		PropertyTemplateHelper.AssociateAutoAssignProperties(propertiedObject, currentProject, false);
	}

	/**
	 * Does the backshell contain a termination that references the given shared pin? If so, return it...
	 *
	 * @param bs, the backshell -- this may be null if no backshell is already present....
	 * @param sp, the shared pin
	 *
	 * @return IBackshellTermination, the backshell's termination that references the shared pin null if there is no
	 * backshell or no reference...
	 */
	@Nullable IBackshellTermination findTerminationReferencingSharedPin(@Nullable IBackshell bs,
			@NotNull ISharedPin sp)
	{
		if (bs != null) {
			for (IBackshellTermination term : bs.getBackshellTerminations()) {
				if (sp.equals(term.getSharedPin())) {
					return term;
				}
			}
		}
		return null;
	}

	protected boolean lockObject(IUIDObject iuidObject)
	{
		if (!LogicObjectLockFinder.tryEdit(iuidObject)) {
			assert m_pinlist != null;
			final ILogicDesign design = m_pinlist.getDiagram().getDesign();
			assert design != null;
			LogicConcurrencyLogger.getInstance()
					.reportLockFailure(design, getActionTitle(), Collections.singleton(iuidObject.getUID()),
							message -> reportLockFailures(message));
			return false;
		}
		return true;
	}

	String getActionTitle()
	{
		return getValue(Action.NAME);
	}

	void reportLockFailures(String message)
	{
		CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(message);
	}

	/**
	 * Determine whether we're editing a frozen shared pin list.
	 *
	 * @return boolean is the shared pinlist frozen
	 */
	protected boolean checkFrozen()
	{
		return spl != null && ((ISharedBackshellOwner) spl).getBackshell() == null && spl.isFrozen();
	}

	protected void commitTerminate()
	{
		if (spl != null) {
			spl.saveAndUnlock();
		}
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		if (!hasAllRequiredDataForActionActivation()) {
			return IActionEnum.eCanceled;
		}

		boolean keepLock = false;
		try {

			//Collect existing backshell terminations before we go further
			populateExistingBackshellAndTerminationsInfo();

			//Collect new backshell and termination related modifications
			if (!hasCollectedAllRequiredDataForActivation()) {
				return IActionEnum.eCanceled;
			}

			if (spl instanceof ISharedBackshellOwner) {
				createSharedTeminations();
			}

			final boolean anyTerminationsSelected = !m_bterms.isEmpty();

			if (anyTerminationsSelected) {
				// Terminations selected. Select placeholders for each termination on pinlist
				assert m_pinlist != null;
				if (getActionHelper().setUp(m_pinlist, m_bterms)) {
					keepLock = true;
					return IActionEnum.eActivated;
				}
				else {
					return IActionEnum.eCanceled;
				}
			}
			else {

				return IActionEnum.eCompleted;
			}
		}
		finally {
			if (spl != null && !keepLock) {
				SharedPinListHelper.unlock(spl);
			}

			// Remove any temporary backshell and terminations created for displaying names
			removeTemporaryBackshellAndTerminations();
		}
	}

	protected boolean hasAllRequiredDataForActionActivation()
	{
		m_pinlist = getOperand(getController().getSelectMgr().getPreSelections());
		if (m_pinlist == null) {
			return false;
		}

		if (!lockObject(m_pinlist)) {
			return false;
		}

		conn = (IConnector) m_pinlist.getConnectivity();

		if (conn == null) {
			return false;
		}

		IAssembly assemblyToUpdate = getAssemblyToUpdate();
		if (assemblyToUpdate != null && !lockObject(assemblyToUpdate)) {
			return false;
		}

		spl = conn.getSharedPinList();
		if (spl != null) {
			if (!spl.isEditable()) {
				CTFLockUpdateHelper.displayDomainRestrictionDialog(spl);
				return false;
			}
			if (!SharedPinListHelper.lock(spl)) {
				LogicActionMessageHelper.warnLocked(spl);
				return false;
			}
		}

		return true;
	}

	//Collect new backshell and termination related modifications
	protected abstract boolean hasCollectedAllRequiredDataForActivation();

	@Nullable private IAssembly getAssemblyToUpdate()
	{
		IAssembly assembly = conn.getAssembly();
		if (assembly != null && conn.getBackshell() == null) {
			assert !conn.isShared() : "Connector should not be shared";
			IProject project = conn.getProject();
			IProjectPreferenceMgr prefMgr = project.getPreferences();
			if (prefMgr.getIncludeConnectorBackshellInLogicAssembly()) {
				return assembly;
			}
		}
		return null;
	}

	/**
	 * Creates the backshell if the passed Connector is a shared pinlist
	 *
	 * @return the shared backshell if created
	 */
	@Nullable protected ISharedBackshell createBackshellFromShared()
	{
		if (conn == null || spl == null) {
			return null;
		}

		//
		// CREATE Shared/non-shared objects
		//
		IBackshell connBS = conn.getBackshell();
		//
		ISharedBackshellOwner sharedBackshellOwner = (ISharedBackshellOwner) spl;
		ISharedBackshell sbs = sharedBackshellOwner.getBackshell();
		if (sbs == null) {
			//
			// Create the backshell...
			//
			sbs = createNewSharedBackshell();
			autoAssignPropertiesFromCurrentProject(sbs);
			sbs.setIncludeOnBOM(true);
			sharedBackshellOwner.setBackshell(sbs);
		}

		if (!sbs.lock()) {
			LogicActionMessageHelper.warnLocked(sbs);
			return null;
		}

		//
		// Always make these changes
		//
		setNameIfDifferent(sbs, m_backshellName);

		if (!sbs.isFrozen()) {
			//noinspection ConstantConditions
			sbs.setPinSymbolRef(m_backshellSymbol);
		}
		// else DR 391722: don't update the symbol reference of a frozen backshell.

		if (connBS == null) {
			connBS = createNewBackshell();
		}
		// else AddBackshellDialog.jbInit() also does backshell creation, so may already have the connectivity
		// backshell.

		connBS.setSharedPinList(sbs);
		conn.setBackshell(connBS);

		//
		// Set the Backshell termination
		//
		for (IPinProxy bterm : m_bterms) {
			IBackshellTermination bt = (IBackshellTermination) bterm.getCablePin();
			ISharedBackshellTermination sterm = (ISharedBackshellTermination) bt.getSharedPin();

			// If the termination to add does not have shared termination then create one
			if (sterm == null) {
				sterm = createnewSharedBackshellTermination();
				//Get the name before setting shared pin
				final String terminationName = bt.getName();
				bt.setSharedPin(sterm);
				setNameIfDifferent(sterm, terminationName);
			}
			// if the shared backshell doesn't already have the shared termination then
			// add it to the shared object.
			if (!sbs.containsPin(sterm)) {
				sbs.addBackshellTermination(sterm);
			}
			// Perform a similar check here for the backshell terminations
			if (!connBS.getPinCollection().contains(bt)) {
				connBS.addBackshellTermination(bt);
			}
		}

		return sbs;
	}

	public boolean onTerminate(boolean successful)
	{
		// Temporary placeholders must be removed before any data model changes.
		// Otherwise they may be captured in the undo snapshot.
		removeTempPlaceHoldersForDevicesWithSymbols();

		Set<IPin> newSchemTerms = new HashSet<>();
		// DR 391722: don't regenerate graphics for frozen shared connectors.
		if (successful && !checkFrozen()) {
			//
			// If the backshell has a symbol, then we go through the pinlist, looking for the
			// backshell pins, and copy the graphics to it.
			//

			if (!beginTerminate()) {
				return false;
			}

			ISharedBackshell sbs = null;

			try {
				//dts0100486801 - exception when undoing add back shell termination
				//Fix: Create the backshell in the onTerminate, to take a valid snapshot!
				sbs = createBackshellFromShared();

				// Have to do this as we may be changing selected objects.
				CreationDeletionHelper.getTheCreationHelper().addCreationObjects(newObjects);
				getController().getSelectMgr().getCurrentSelections().clear();

				createBackshell();
				//
				// Update from the form...
				//
				processNewSchemTerms(newSchemTerms);
			}
			finally {
				if (sbs != null) {
					sbs.saveAndUnlock();
				}
				commitTerminate();
			}
		}
		else {

			//Do we ever get here?
			processNewObjects();
		}
		newObjects.clear();

		getActionHelper().cleanUp(successful);

		((IUpdateableAction) Objects.requireNonNull(getActionUI())).updateUI();

		if (successful) {
			updateICDRouting(newSchemTerms::contains);
		}
		return true;
	}

	private void removeTempPlaceHoldersForDevicesWithSymbols()
	{
		if (getActionHelper().hasTempPlaceHolderForDevicesWithSymbols()) {
			SymDeviceTemporaryPlaceHolderCreationHelper helper =
					new SymDeviceTemporaryPlaceHolderCreationHelper(
							PinPlaceholderProviderForSymbolledDeviceInMove::getDeviceWithSymbol);
			helper.removeTempPlaceHoldersForDevicesWithSymbols(m_pinlist);
		}
	}

	protected void processNewSchemTerms(Set<IPin> newSchemTerms)
	{
		addTerminationsAndUpdateGraphics(newSchemTerms);
	}

	public void addTerminationsAndUpdateGraphics(Set<IPin> newSchemTerms)
	{
		assert m_pinlist != null;
		final ISchemDiagram diagram = DiagramHelper.getDiagram(m_pinlist);
		//
		// Get the diagram; if not null - do all on this diagram...
		//
		if (diagram != null) {
			CompositePinConnectivityFinder connectivityFinder = new CompositePinConnectivityFinder(diagram);
			Set<IPin> existingPins = new HashSet<>(m_pinlist.getPins());
			getActionHelper().addPins(m_pinlist, diagram, connectivityFinder);
			connectivityFinder.connect();
			Generator generator = Generator.getGenerator();

			rebuildBackshellGraphics(diagram, generator);

			m_pinlist.regenerateDiagramObject();
			final Iterator<IPinList> attachedPinlistIterator = m_pinlist.getAttachedPinListObjects().iterator();
			if (attachedPinlistIterator.hasNext()) {
				ResizeHelper.resizeInlineForBackshellTerminations(attachedPinlistIterator.next(), m_pinlist);
			}
			getActionHelper().createConnectionSchematics(m_pinlist);
			newSchemTerms.addAll(m_pinlist.getPins());
			newSchemTerms.removeAll(existingPins);
			newSchemTerms.forEach(
					bsTermination -> IECAttributeResolver.inheritIECAttributesIfNotPresent(diagram, bsTermination));
		}
	}

	@SuppressWarnings("ConstantConditions") public void processNewObjects()
	{
		for (IUIDObject obj : newObjects) {
			if (obj instanceof ISharedBackshell) {
				ISharedBackshell sb = (ISharedBackshell) obj;
				final ISharedBackshellOwner sharedBackshellOwner = sb.getOwner();
				if (sharedBackshellOwner != null) {
					sharedBackshellOwner.setBackshell(null);
				}
			}
			else if (obj instanceof IBackshell) {
				IBackshell bs = (IBackshell) obj;
				final IConnector connector = bs.getOwner();
				if (connector != null) {
					connector.setBackshell(null);
				}
			}
			else if (obj instanceof ISharedBackshellTermination) {
				ISharedBackshellTermination sbt = (ISharedBackshellTermination) obj;
				ISharedBackshell sb = (ISharedBackshell) sbt.getOwner();
				if (sb != null) {
					sb.removeBackshellTermination(sbt);
				}
			}
			else if (obj instanceof IBackshellTermination) {
				IBackshellTermination bt = (IBackshellTermination) obj;
				IBackshell bs = (IBackshell) bt.getOwner();
				if (bs != null) {
					bs.removeBackshellTermination(bt);
				}
			}
			UIDMgr.removeObject(obj.getUID());
		}
	}

	/**
	 * Rebuilds backshell graphics for all schematic representations of the connector.
	 *
	 * @param diagram   The schematic diagram containing the representations
	 * @param generator The generator used to rebuild backshell graphics
	 */
	protected void rebuildBackshellGraphics(ISchemDiagram diagram, Generator generator)
	{
		if (conn instanceof IDeviceConnector deviceConnector) {
			IBaseDevice device = deviceConnector.getOwner();
			if (device != null) {
				for (IDiagramObject representation : diagram.getRepresentations(device.getUID())) {
					new BackshellGraphicsRebuilder()
							.rebuildBackshellGraphicsForDeviceConnectors((IPinList) representation);
				}
			}
		}
		else {
			for (IDiagramObjectIterator itr = diagram.getRepresentations(conn.getUID()); itr.hasNext(); ) {
				IPinList schemPL = (IPinList) itr.next();
				//noinspection ConstantConditions
				new BackshellGraphicsRebuilder().rebuildAllBackshellGraphics(schemPL, m_backshellSymbol);
			}
		}
	}

	protected void createBackshell()
	{
		IBackshell bshell = conn.getBackshell();
		if (bshell == null) {
			//
			// We need to create it, get the information from the dialog.
			//

			if (spl == null) {
				//
				// regular non-shared backshell.
				//
				createRegularNonSharedBackshell();
			}
		}
		else {
			if (bshell.getSharedPinList() == null) {
				//SP1706 dts0101259019: Fixed check box for backshell name remain checked even for default name if user opens edit backshell dialog twice
				setNameIfDifferent(bshell, m_backshellName);

				if (m_backshellSymbol != bshell.getSymbolRef()) {
					//noinspection ConstantConditions
					bshell.setSymbolRef(m_backshellSymbol);
				}
			}
		}
	}

	private void setNameIfDifferent(INamedPropertiedObject namedObject, String changeNameTo)
	{
		if (namedObject != null && changeNameTo != null) {
			final String existingName = namedObject.getName();
			if (existingName == null || existingName.compareToIgnoreCase(changeNameTo) != 0) {
				namedObject.setName(changeNameTo);
			}
		}
	}

	protected void createRegularNonSharedBackshell()
	{
		IBackshell bshell = createNewBackshell();
		conn.setBackshell(bshell);
		setNameIfDifferent(bshell, m_backshellName);
		//noinspection ConstantConditions
		bshell.setSymbolRef(m_backshellSymbol);
		AssemblyUtils.updateBackshellAsPerPrefs(conn, false);
	}

	// Put ourselves in the context menu if there are
	// any IParameterized objects selected.
	@Nullable protected IPinList getOperand(SelectSet selections)
	{
		return getActionHelper().getPinListThatAllowsBackshellAddition(selections);
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	public void mouseEntered(MouseEvent e)
	{
		getActionHelper().mouseEntered(e);
	}

	public void mouseExited(MouseEvent e)
	{
		getActionHelper().mouseExited(e);
	}

	public void mousePressed(MouseEvent e)
	{
		collectAndClearNewObjectsToProcess();

		getActionHelper().mousePressed(e);
	}

	public void collectAndClearNewObjectsToProcess()
	{
		// sometimes this action creates stuff a bit too early
		// need to mess with CDH to avoid warnings about creating stuff before onTerminate
		CreationDeletionHelper cdh = CreationDeletionHelper.getTheCreationHelper();
		if (cdh.getPendingCount() > 0) {
			assert newObjects.isEmpty();
			newObjects.addAll(CollectionUtils.createList(cdh.getNewObjectsToProcess()));
			cdh.clearNewObjects();
		}
	}

	public void mouseReleased(MouseEvent e)
	{
		getActionHelper().mouseReleased(e);
	}

	public void mouseDragged(MouseEvent e)
	{
		getActionHelper().mouseDragged(e);
	}

	public void mouseClicked(MouseEvent e)
	{
		getActionHelper().mouseClicked(e);
	}

	public void mouseMoved(MouseEvent e)
	{
		getActionHelper().mouseMoved(e);
	}

	public AddBSTerminationActionHelper getActionHelper()
	{
		return m_addPinActionHelper;
	}

	// Enabled if there are any IParameterized objects selected.

	public boolean isEnabled()
	{
		if (!getController().getCapletModel().isEditable()) {// eg. read-only model
			return false;
		}
		//
		// Connectors only for Add backshell action and backshell only for add backshell termination action
		//
		return getOperand(getController().getSelectMgr().getPreSelections()) != null && super.isEnabled();
	}

	protected boolean beginTerminate()
	{
		if (spl == null) {
			return true;
		}
		if (!spl.lock()) {
			LogicActionMessageHelper.warnLocked(spl);
			return false;
		}
		return true;
	}

	@Nullable public IDeviceICD getICD()
	{
		IDevice ownerDevice = getOwnerDevice();
		if (ownerDevice != null) {
			final Set<IDeviceICD> matchingICDs = ICDUtils.getMatchingICDs(ownerDevice);
			if (!matchingICDs.isEmpty()) {
				return matchingICDs.iterator().next();
			}
		}

		return null;
	}

	@Nullable private IDevice getOwnerDevice()
	{
		if (m_pinlist != null) {
			final IDeviceOwnedConnector connector =
					CommonUtils.cast(m_pinlist.getConnectivity(), IDeviceOwnedConnector.class);
			if (connector != null) {
				return CommonUtils.cast(connector.getOwner(), IDevice.class);
			}
		}

		return null;
	}

	private void updateICDRouting(@NotNull IObjectFilter<IPin> pinFilter)
	{
		final IDevice ownerDevice = getOwnerDevice();
		final Optional<IPinList> attachedDevice =
				Objects.requireNonNull(m_pinlist).getAttachedPinListObjects().stream()
						.filter(p -> p.getConnectivity() == ownerDevice).findFirst();

		if (attachedDevice.isPresent()) {
			IDeviceICD icd = getICD();
			if (icd != null) {
				getActionHelper().updateICDRouting(attachedDevice.get(), icd, pinFilter, false);
			}
		}
	}

	@NotNull public IConnector getConnector()
	{
		return conn;
	}

	@Override public void selectedBackshellTerminations(List<IPinProxy> selectedBackshellTerminations)
	{
		m_bterms = selectedBackshellTerminations;
	}

	@Override public void selectedBackshellName(String backshellName)
	{
		m_backshellName = backshellName;
	}

	@Override public void selectedBackshellSymbol(@Nullable ISymbolRef backshellSymbol)
	{
		m_backshellSymbol = backshellSymbol;
	}

	@NotNull public IBackshell createTemporaryBackshell()
	{

		if (temporaryBackshell == null) {
			temporaryBackshell = new Backshell(getNewUID());
			final IConnector connector = getConnector();
			temporaryBackshell.setConnectivity(connector.getConnectivity());
		}

		return temporaryBackshell;
	}

	@NotNull public IBackshellTermination createNewTemporaryBackshellTermination(@NotNull IBackshell backshell)
	{
		final IBackshellTermination newBackshellTermination = new BackshellTermination(getNewUID());
		newBackshellTermination.setOwner(backshell);
		newBackshellTermination.setConnectivity(backshell.getConnectivity());
		temporaryTerminations.add(newBackshellTermination);
		return newBackshellTermination;
	}

	@NotNull @Override public IBackshellTermination createNewTemporaryBackshellTermination()
	{
		final IBackshell backshell = getExistingOrTemporaryBackshell();

		return createNewTemporaryBackshellTermination(backshell);
	}

	public void populateExistingBackshellAndTerminationsInfo()
	{
		//Collect existing backshell terminaitons info, before new termination are added
		existingBackehellTerminations = getExistingBackshellTerminations();

		//Track current index from backshell name space
		backshellNameSpace = getBackshellNamespace();
		backshellNameIndexOriginal = backshellNameSpace != null ? backshellNameSpace.getCurrentIndex() : 1;

		//Track current index of terminations from namespace using existing backshell
		backshellTerminationNameSpace = getBackshellTerminationNameSpace();
		terminationNameIndexOriginal =
				backshellTerminationNameSpace != null ? backshellTerminationNameSpace.getCurrentIndex() : 1;
	}

	@Nullable public INameSpace getBackshellTerminationNameSpace()
	{
		final IBackshell backshell = getExistingBackshell();
		if (backshell != null) {
			// Termination name index are stored against owner UID, along with its type
			final IUID nameSpaceOwnerUid = backshell.getUID();
			final int nameSpaceType = INameSpace.BACKSHELL_TERMINATION;
			final RegularNameSpaceKey key = new RegularNameSpaceKey(nameSpaceOwnerUid, nameSpaceType);
			final INameMgr nameMgr = getConnector().getConnectivity().getNameMgr();
			return nameMgr.getNameSpace(key);
		}
		return null;
	}

	@Nullable public INameSpace getBackshellNamespace()
	{
		final INameMgr nameMgr = getConnector().getConnectivity().getNameMgr();
		final String objectType = IBackshell.class.getName();
		final int backshellType = INameSpace.BACKSHELL;
		final HwmNameSpaceKey hwmNameSpaceKey = new HwmNameSpaceKey(objectType, backshellType);
		return nameMgr.getNameSpace(hwmNameSpaceKey);
	}

	public Optional<INamedPropertiedObject> findExistingBackshellTermination(String name)
	{
		return getFirstObjectMatchingByName(existingBackehellTerminations, name);
	}

	public Optional<INamedPropertiedObject> getFirstObjectMatchingByName(Collection<INamedPropertiedObject> objects,
			String name)
	{
		return objects
				.stream()
				.filter(t -> t != null && t.getName().equalsIgnoreCase(name))
				.findFirst();
	}

	@Override public void removeTemporaryBackshellAndTerminations()
	{

		//Reset indexes for both backshell and its terminations
		resetNameSpaceIndexes();

		if (temporaryBackshell != null) {
			//noinspection ConstantConditions
			temporaryBackshell.delete();
		}

		//Take care of temporary termination removal as well. Temporary backshell may not be referencing these
		for (INamedPropertiedObject termination : temporaryTerminations) {
			termination.delete();
		}

		clearTemporaryObjects();
	}

	private void resetNameSpaceIndexes()
	{
		if (temporaryBackshell != null) {

			// Reset backshell namespace to previous state, as temporary one will get deleted
			if (backshellNameSpace == null) {
				// Perhaps, for the first time namespace got created. Get it now to reset
				backshellNameSpace = getBackshellNamespace();
				assert backshellNameSpace != null;
				final int resultingIndex = backshellNameSpace.getCurrentIndex() - 1;
				final int resetIndex = resultingIndex > 0 ? resultingIndex : backshellNameIndexOriginal;
				backshellNameSpace.setCurrentIndex(resetIndex);
			}
			else {
				// Backshell nameapace available before action activation. So, reset
				backshellNameSpace.setCurrentIndex(backshellNameIndexOriginal);
			}

			//No need to reset index for terminations as namespace is owned against temporary backshell
		}
		else {

			if (backshellTerminationNameSpace == null) {
				// By now, namespace must have been created for existing backshell
				backshellTerminationNameSpace = getBackshellTerminationNameSpace();
			}

			// Temporary terminations are added to existing backshell. Reset index for terminations
			if (backshellTerminationNameSpace != null) {
				backshellTerminationNameSpace.setCurrentIndex(terminationNameIndexOriginal);
			}
		}
	}

	@Override public void clearTemporaryObjects()
	{
		clearTempoaryBackshell();
		clearTemporaryTerminations();
	}

	@Override public void clearTempoaryBackshell()
	{
		temporaryBackshell = null;
	}

	@Override public void clearTemporaryTerminations()
	{
		temporaryTerminations.clear();
	}

	protected Frame getFrame()
	{
		return getController().getCaplet().getFIB().getWindowMgr().getDialogFrame();
	}

	protected String getTitle()
	{
		return CAFUtils.getInstance().getDialogTitleByAction(this);
	}

	public String getStatusbarText()
	{
		return getActionHelper().getStatusbarText();
	}
}