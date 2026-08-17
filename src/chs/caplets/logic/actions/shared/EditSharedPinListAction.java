/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2016-2025 Siemens
 */
package chs.caplets.logic.actions.shared;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICAFWindow;
import chs.caf.ICtxMenuProvider;
import chs.caf.IFIB;
import chs.caf.caplet.CapletViewIterator;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletWindow;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.helpers.SharedConfirmDialogHandler;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectionFilter;
import chs.capitalmanager.appserver.IUserSession;
import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.LogicActionMessageHelper;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedConnectorPin;
import chs.cof.logical.shared.ISharedDevice;
import chs.cof.logical.shared.ISharedFactory;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinIterator;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinListMgr;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.logical.shared.SharedPinListHelper;
import chs.cof.project.IProject;
import chs.cof.security.IDomain;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.ISymbolDefIterator;
import chs.cofUtils.scrubber.ScrubberFactory;
import chs.cofUtils.scrubber.SharedObjectScrubbableChecks;
import chs.common.ICommonFactory;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.RefreshStatusEnum;
import chs.ctf.caf.ui.SharedObjectDomainsHelper;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.caf.utils.PinProxy;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.utilities.Environment;
import chs.utilities.IAuditTrailLogger;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utility.SymbolUtils;
import chs.utility.audit.AuditableEventType;
import chs.utility.helpers.ConfirmChoiceDialog;
import chs.utility.helpers.UtilsHelper;
import chs.utility.logic.ILogicModel;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import chs.utility.logic.LogicUtils;
import chs.utility.logic.SharedObjectAvailabilityChecker;
import chs.utility.logic.SyncSymbolInfoFromSharedDevicetoConnectivity;
import chs.utility.persist.LockableHelper;
import chs.utility.ui.ISharedPinListSymbolInstance;
import chs.utility.ui.SharedPinListEditUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Mar 14, 2005 Time: 3:26:49 PM
 */
public class EditSharedPinListAction extends ControllerActionRT implements ICtxMenuProvider
{

	private EditSharedPinlistDialog m_dialog;
	private ISharedFactory m_sharedFactory;
	private IProject m_project;
	private ISharedPinListMgr m_splMgr;
	private IFIB m_fib;
	private ISpecialSelectMgr m_sharedSelectMgr;
	private ISharedPinList operand;
	private Set<ISharedPinList> otherModifiedSharedPinlists;
	private boolean showAnalysisTab;

	public EditSharedPinListAction(ICapletController controller, ISpecialSelectMgr sharedSelectMgr)
	{
		super(controller);
		m_sharedSelectMgr = sharedSelectMgr;
		if (getActionUI() != null) {
			m_sharedSelectMgr.contextMenuAddAction(
					new ActionEntry(getActionUI(), (String) getActionUI().getValue(Action.SHORT_DESCRIPTION))
					{
						public boolean shouldDisplay()
						{
							ISharedPinList spl = setOperand(true);

							// Side effect - change the action name just in time
							EditSharedPinListActionUI ui = (EditSharedPinListActionUI) getActionUI();
							ui.setTextAndIconFromOperand(spl);
							setName((String) ui.getValue(Action.SHORT_DESCRIPTION));
							ui.updateUI();

							return spl != null;
						}
					});
		}
		m_sharedFactory = FactoryMgr.getSharedFactory();
		m_fib = controller.getCaplet().getFIB();

		IDesign design = getLogicModel().getDesign();
		m_project = design.getProject();
		m_splMgr = m_project.getSharedPinListMgr();
		setUndoableAction(false);

		showAnalysisTab = false;
	}

	protected Model getLogicModel()
	{
		return (Model) getController().getCapletModel();
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		IActionEnum ae = IActionEnum.eCanceled;
		ISharedPinList sharedPinList = getOperand();
		otherModifiedSharedPinlists = new HashSet<ISharedPinList>();
		try {
			validateSharedPinListIsEditable(sharedPinList);
			final ISharedObjectAvailabilityReporter reporter = new SharedObjectAvailabilityReporter();
			if (sharedPinList != null && new SharedObjectAvailabilityChecker().check(sharedPinList, null, reporter)) {
				validateSharedPinListExists(sharedPinList);
				createSPLMgrIfNotExist();
				refreshSPL(sharedPinList); //dts0100976236
				if (sharedPinList.isEditable()) {
					lockSPL(sharedPinList);
					validateSPLNotFrozen(sharedPinList);
					validateUpdatedSymbols(sharedPinList);
					showDialog(sharedPinList, false);
					catchOtherModifiedPinLists();
					validateConnectedPinLists();
					ae = IActionEnum.eCompleted;
				}
				else {
					showDialog(sharedPinList, true);
				}
			}
		}
		catch (EditSPLError ex) {
			ex.showError();
			return IActionEnum.eCanceled;
		}
		finally {
			if (ae == IActionEnum.eCanceled) {
				if (sharedPinList != null) {
					SharedPinListHelper.unlock(sharedPinList);
				}
				for (ISharedPinList spinList : otherModifiedSharedPinlists) {
					SharedPinListHelper.unlock(spinList);
				}
			}
		}
		return ae;
	}

	private void validateConnectedPinLists() throws EditSPLError
	{
		for (ISharedPinList spinList : otherModifiedSharedPinlists) {
			if (!SharedPinListHelper.lock(spinList)) {
				throw new SPLLocked(spinList, true);
			}
			else if (spinList.refresh() == RefreshStatusEnum.eObjectDoesNotExist) {
				throw new SPLDoesNotExist(spinList, true);
			}
			else if (spinList.isFrozen()) {
				throw new SPLFrozen(spinList, true);
			}
		}
	}

	private void catchOtherModifiedPinLists()
	{
		for (ISharedPin spin : m_dialog.getConnectedPinsToMakeReusableValues()) {
			otherModifiedSharedPinlists.add(spin.getOwner());
		}
	}

	private void showDialog(ISharedPinList sharedPinList, boolean showAsReadOnly) throws DialogCancelled
	{
		Frame owner = m_fib.getWindowMgr().getDialogFrame();
		m_dialog = getSharedPinListDialog(sharedPinList, owner);

		m_dialog.setHelpID(EditSharedPinListAction.class.getName());

		if (showAnalysisTab) {
			m_dialog.showAnalysisTab();
		}

		if (showAsReadOnly) {
			m_dialog.showAsReadonly();
		}

		m_dialog.setVisible(shouldDisplayDialog());
		if (m_dialog.isCancelled()) {
			throw new DialogCancelled(sharedPinList);
		}
	}

	protected EditSharedPinlistDialog getSharedPinListDialog(ISharedPinList sharedPinList, Frame owner)
	{
		if (sharedPinList.isFunctionType()) {
			return new EditSharedFunctionDialog(owner, CAFUtils.getInstance().getDialogTitleByAction(this, true),
					sharedPinList, getLogicModel());
		}
		return new EditSharedPinlistDialog(owner,
				CAFUtils.getInstance().getDialogTitleByAction(this, true), sharedPinList,
				getLogicModel());
	}

	protected boolean shouldDisplayDialog()
	{
		return true;
	}

	private void validateSPLNotFrozen(ISharedPinList sharedPinList) throws SPLFrozen
	{
		if (sharedPinList.isFrozen()) {
			throw new SPLFrozen(sharedPinList);
		}
	}

	private void refreshSPL(ISharedPinList sharedPinList) throws SPLDoesNotExist, SPLTypeChanged
	{
		PinListTypeEnum type = sharedPinList.getType();

		if (sharedPinList.refresh() == RefreshStatusEnum.eObjectDoesNotExist) {
			throw new SPLDoesNotExist(sharedPinList);
		}
		if (sharedPinList.getType() != type) {
			throw new SPLTypeChanged(sharedPinList);
		}
	}

	private void lockSPL(ISharedPinList sharedPinList) throws SPLLocked, SPLTypeChanged
	{
		PinListTypeEnum type = sharedPinList.getType();
		if (!SharedPinListHelper.lock(sharedPinList)) {
			throw new SPLLocked(sharedPinList);
		}
		if (sharedPinList.getType() != type) {
			throw new SPLTypeChanged(sharedPinList);
		}
	}

	private void createSPLMgrIfNotExist()
	{
		ISharedPinListMgr splmgr = m_project.getSharedPinListMgr();
		ICommonFactory commonFactory = CAFUtils.getInstance().getCommonFactory();
		if (splmgr == null) {
			IUID spmuid = commonFactory.createUID();
			splmgr = m_sharedFactory.createSharedPinListManagerBasedOnSetup(spmuid);
			m_project.setSharedPinListMgr(splmgr);
			splmgr.flushNew(m_project.getObjType(), m_project);
		}
	}

	private void validateSharedPinListExists(ISharedPinList sharedPinList) throws SPLDoesNotExist
	{
		if (sharedPinList != null) {
			if (!LockableHelper.objectExists(CAFUtils.getInstance().getUserSession(), sharedPinList)) {
				throw new SPLDoesNotExist(sharedPinList);
			}
		}
	}

	private void validateSharedPinListIsEditable(ISharedPinList sharedPinList) throws SPLNotEditable
	{
		if (!EditSharedPinListModel.isSharedPinListEditable(sharedPinList)) {
			throw new SPLNotEditable(sharedPinList);
		}
	}

	private void validateUpdatedSymbols(ISharedPinList sharedPinList)
	{
		ISymbolDefIterator sdIt = sharedPinList.getSymbols();
		Set<ISymbolDef> symbols = new HashSet<ISymbolDef>();
		while (sdIt.hasNext()) {
			ISymbolDef def = sdIt.getNext();
			for (int i = 0; i < sharedPinList.getNumInstances(def); i++) {
				if (!SymbolUtils.isValidSymbolInstance(sharedPinList, def, i)) {
					symbols.add(def);
				}
			}
		}
		if (!symbols.isEmpty()) {
			handleSymbolNotUpdated(sharedPinList, symbols);
		}
	}

	private void handleSymbolNotUpdated(ISharedPinList sharedPinList, Set<ISymbolDef> symbols)
	{
		StringBuilder ofdSymbols = new StringBuilder("");
		for (ISymbolDef symbol : symbols) {
			ofdSymbols.append(symbol.getName());
			ofdSymbols.append(", ");
		}
		String ofdnames = "";
		if (ofdSymbols.length() > 2) {
			ofdnames = ofdSymbols.substring(0, ofdSymbols.length() - 2);
			if (!ofdnames.trim().isEmpty()) {
				ofdnames = '\"' + ofdnames + '\"';
			}
		}
		if (!sharedPinList.isPartAssigned()) {
			MessageHelper.showWarningMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
					ResourceMgr.getString(ShareAction.class,
							"EditSharedPinListAction.NotUptoDate.Heading.text"),
					getSymbolNotUpToDateMessageText(sharedPinList, ofdnames));

			createSymbolNotUpToDateAssociateInstructionsTab(sharedPinList);
		}
		else {
			//Not applicable to functions
			MessageHelper.showWarningMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
					ResourceMgr.getString(ShareAction.class,
							"EditSharedPinListAction.NotUptoDate.Heading.text"),
					ResourceMgr.getString(ShareAction.class,
							"EditSharedPinListAction.NotUptoDate_Libraried.Message.text", ofdnames,
							'\"' + sharedPinList.getName() + '\"'));
		}
	}

	private void createSymbolNotUpToDateAssociateInstructionsTab(ISharedPinList sharedPinList)
	{
		if (sharedPinList.isFunctionType()) {
			CAFUtils.getInstance().getOutputWindow().sendMessage(ResourceMgr.getString(ShareAction.class,
					"EditSharedPinListAction.NotUptoDate_Function.Associate.instructions"), "Edit Shared Function",
					true, true);
		}
		else {
			CAFUtils.getInstance().getOutputWindow().sendMessage(ResourceMgr.getString(ShareAction.class,
					"EditSharedPinListAction.NotUptoDate.Associate.instructions"), "Edit Shared Pin List", true, true);
		}
	}

	private String getSymbolNotUpToDateMessageText(ISharedPinList sharedPinList, String ofdnames)
	{
		return sharedPinList.isFunctionType() ?
				ResourceMgr.getString(ShareAction.class,
						"EditSharedPinListAction.NotUptoDate_Function.Message.text", ofdnames,
						'\"' + sharedPinList.getName() + '\"') :
				ResourceMgr.getString(ShareAction.class,
						"EditSharedPinListAction.NotUptoDate.Message.text", ofdnames,
						'\"' + sharedPinList.getName() + '\"');
	}

	protected boolean onTerminate(boolean successful)
	{
		boolean editOK = false;
		if (successful && canProceed()) {
			ISharedPinList sharedPinList = m_dialog.getSharedPinList();

			if (sharedPinList == null) {
				return false;
			}

			try {
				List<IPinProxy> sharedPins = m_dialog.getPlugMapInfo();
				List<String[]> newPins = null;

				// Process pins added
				for (IPinProxy proxy : sharedPins) {
					ISharedPin sharedPin = proxy.getSharedPin();
					if (sharedPin == null) { // New shared pin.
						SharedPinListEditUtils.createAndAddSharedPin(sharedPinList, (PinProxy) proxy);
						if (newPins == null) {
							newPins = new ArrayList<String[]>();
						}
						String[] pinPair = new String[2];
						pinPair[0] = proxy.getName();
						pinPair[1] = proxy.getSharedPin().getUID().toString();
						newPins.add(pinPair);
					}
				}
				// Process deleted shared pins
				boolean pinDeleted = false;
				for (ISharedPinIterator iter = sharedPinList.getPins(); iter.hasNext(); ) {
					// Look at all the pins on the shared pins list, and check to see if this pins is found in the
					// proxies.  If not, then it has been deleted.
					ISharedPin spin = iter.next();
					if (spin instanceof ISharedConnectorPin) {
						if (((ISharedConnectorPin) spin).isBlockedCavity()) {
							continue;
						}
					}
					boolean found = false;
					for (IPinProxy proxy : sharedPins) {
						ISharedPin spin2 = proxy.getSharedPin();
						if (spin == spin2) {
							found = true;
							break;
						}
					}
					if (!found) {
						// Not found in the proxes - the pins has been deleted.
						sharedPinList.removePin(spin);
						if (sharedPinList.getModelMapping() != null) {
							sharedPinList.getModelMapping().removePortMappingFor(spin);
						}
						SharedPinListEditUtils.deleteSharedPinSharedPin(sharedPinList, spin, true);
						pinDeleted = true;
					}
				}
				// Now that all the new pins have proper UIDs, we can apply the attachment panel changes.
				m_dialog.applyAttachmentPanelChanges(newPins);

				// Process pins renamed
				for (IPinProxy pinProxy : sharedPins) {
					if (!pinProxy.getSharedPin().getName().equals(pinProxy.getName())) {
						pinProxy.getSharedPin().setName(pinProxy.getName());
					}
				}

				// Process pins made reusable
				List<IPinProxy> reusablePins = new ArrayList<IPinProxy>(m_dialog.getReusablePins());
				if (!m_dialog.getReusablePins().isEmpty()) {
					for (IPinProxy proxy : reusablePins) {
						ISharedPin sharedPin = proxy.getSharedPin();
						if (sharedPin != null && !sharedPin.isReusable()) {
							// Make reusable
							sharedPin.setReservationType(ISharedPin.ReservationType.Unrestricted);

							if (sharedPin.getMatePin() != null) {
								sharedPin.getMatePin().setReservationType(ISharedPin.ReservationType.Unrestricted);
							}

							// If the shared pin is connected to another pin we need to make that reusable too
							// This list is build up in the ReusePanel so we know if its in here, it needs to be done.
							ISharedPin connectedSpin = m_dialog.getConnectedPinToMakeReuable(sharedPin);
							if (connectedSpin != null) {
								// Shouldn't have a mate (it attached to this spin) so don't bother testing for it.
								connectedSpin.setReservationType(ISharedPin.ReservationType.Unrestricted);
							}
						}
					}
				}
				// Some pins that were re-useable may have be made non-reuseable.
				List<IPinProxy> proxys = m_dialog.getPlugMapInfo();
				for (IPinProxy proxy : proxys) {
					// Look through all the proxies looking for re-useable shared pins.
					ISharedPin spin = proxy.getSharedPin();
					if (spin.isReusable()) {
						boolean found = false;
						for (IPinProxy proxy2 : reusablePins) {
							// Now check to ensure that is it listed as a reuseable pin according to the dialog.
							ISharedPin spin2 = proxy2.getSharedPin();
							if (spin2 == spin) {
								found = true;
								break;
							}
						}
						if (!found) {
							// If the dialog doesn't think it is re-useable, it's been "demoted"
							spin.setReservationType(ISharedPin.ReservationType.AUTOMATIC);
							if (spin.getMatePin() != null) {
								spin.getMatePin().setReservationType(ISharedPin.ReservationType.AUTOMATIC);
							}
						}
					}
				}

				// Process symbols deleted
				boolean symbolDeleted = false;
				for (ISharedPinListSymbolInstance symbolInstance : m_dialog.getSymbolInstancesForDeletion()) {
					sharedPinList
							.removeSymbolInstance(symbolInstance.getSymbolDef(), symbolInstance.getInstanceNumber());
					symbolDeleted = true;
				}

				// Process symbols added
				for (ISymbolDef symbolDef : m_dialog.getSymbolDefsToAdd()) {
					// If this is not a device, remove any pre-existing symbol mapping (there should only ever
					// be one if this is not a device.)
					if (!doesItSupportMultipleSymbols(sharedPinList)) {
						SharedPinListEditUtils.removeAllSymbols(sharedPinList);
						symbolDeleted = true;
					}
					// Add symbol pin mappings
					SharedPinListEditUtils
							.addSymbol(sharedPinList, symbolDef);
				}
				// modify pin asscociation for all symbols
				SharedPinListEditUtils.refreshPinMaps(sharedPinList, m_dialog.getSymbolDefsToPinProxyMap());

				// Process Shared Domains
				final Set<IDomain> sharedDomains = m_dialog.getSharedDomains();
				SharedObjectDomainsHelper.processSharedDomains(sharedDomains, sharedPinList);
				ISharedPinList mate = SharedPinListEditUtils.getMate(sharedPinList);
				if (mate != null) {
					SharedObjectDomainsHelper.processSharedDomains(sharedDomains, mate);
				}

				if (sharedPinList instanceof ISharedDevice) {
					SharedObjectScrubbableChecks.checkSharedDeviceConnectors(
							new ScrubberFactory().getSharedObjectScrubber(), (ISharedDevice) sharedPinList);
				}

				boolean savedModularChanges = false;
				IUserSession session = UtilsHelper.getCHSSystem().getUserSession();
				try {

					session.startClientTransaction();

					Collection<ISharedConnector> affectedModularConnectors = new LinkedHashSet<>();
					if (symbolDeleted || pinDeleted) {
						if (sharedPinList instanceof ISharedConnector) {

							ISharedConnector sharedModularConnector = (ISharedConnector) sharedPinList;
							affectedModularConnectors
									.addAll(sharedModularConnector.getChildConnectors());
							ISharedConnector parentConnector = sharedModularConnector.getParentConnector();
							if (parentConnector != null) {
								affectedModularConnectors.add(parentConnector);
							}
						}

						editOK = SharedPinListHelper.recreatePinlist(sharedPinList);
						for (ISharedConnector aChild : affectedModularConnectors) {
							SharedPinListHelper.flush(aChild);
						}
					}
					else {
						SharedPinListHelper.flush(sharedPinList);
						for (ISharedPinList connectedSpinList : otherModifiedSharedPinlists) {
							SharedPinListHelper.flush(connectedSpinList);
						}

						editOK = true;
					}
					savedModularChanges = m_dialog.saveModularChanges();
					editOK = editOK && savedModularChanges;
					if (editOK) {
						session.commitClientTransaction();
					}
				}
				catch (UserSessionException e) {
					Environment.getExceptionDisplay()
							.displayException(e, "Edit shared pinlist action failed");
				}
				finally {
					if (!editOK) {
						session.rollbackClientTransaction();
					}
				}

				notifyPinListChanges();

				//notify model to refresh shared modular connector changes
				if (savedModularChanges) {
					ICapletController controller = CAFUtils.getInstance().getActiveCapletController();
					if (controller != null) {
						ICapletModel model = controller.getCapletModel();
						model.notifyModelChange(new ModelChangeEvent(model, Collections.<IUID>emptyList()));
					}
				}

				//TODO discuss: Which is a better approach? Do it in this action "or" notify-listener (thought that
				//it would be too costly)
				if (symbolDeleted && sharedPinList instanceof ISharedDevice) { //only for devices
					List<ILogicDesign> openedDesigns = new ArrayList<ILogicDesign>();
					//bother about only designs that are currently opened in editable mode.
					//TODO discuss: Is there a ready-made utility to get this info?
					for (ICAFWindow cafWin : CAFUtils.getInstance().getWindowMgr().getWindows()) {
						if (cafWin instanceof ICapletWindow && cafWin.isDisplayed()) {
							CapletViewIterator cvIt = ((ICapletWindow) cafWin).getViews();
							while (cvIt.hasNext()) {
								GfxView view = (GfxView) cvIt.getNext();
								ICapletModel capletModel = view.getCapletModel();
								if (capletModel instanceof ILogicModel) {
									IDesign design = ((ILogicModel) capletModel).getDesign();
									if (design != null && design instanceof ILogicDesign &&
											design.getProject() == m_project && design.isEditable()) {
										openedDesigns.add((ILogicDesign) design);
									}
								}
							}
						}
					}
					for (ILogicDesign design : openedDesigns) {
						//Only designs in which this sharedpinlist is present
						IDevice device = (IDevice) design.getConnectivity().findSharedPinList(sharedPinList);
						if (device != null) {
							SyncSymbolInfoFromSharedDevicetoConnectivity
									.syncDeviceInternalConnectivityFromShared(device);
						}
					}
				}
				// Clear the undo stack to avoid problems with trying to undo editing shared objects
				getController().getUndoableContainer().endEdit();
				getController().getUndoableContainer().clear();

				String projectUid = sharedPinList.getProject().getUID().getString();
				IAuditTrailLogger auditLogger = CAFUtils.getInstance().getAuditLogger();
				auditLogger.postEvent(AuditableEventType.SHARED_OBJECT_MODIFIED, null, projectUid,
						sharedPinList.getFullName(), sharedPinList.getUID().getString());
			}
			finally {
				SharedPinListHelper.unlock(sharedPinList);
				for (ISharedPinList connectedSpinList : otherModifiedSharedPinlists) {
					SharedPinListHelper.unlock(connectedSpinList);
				}
			}
		}
		return editOK;
	}

	protected boolean canProceed()
	{
		if(!LogicUtils.canShowSharedWarningDialog(getBaseDiagram()) ||
				!SharedConfirmDialogHandler.canShowDialogForEdit()){
			return true;
		}
		return getUserResponse();
	}

	protected boolean getUserResponse()
	{
		SharedConfirmDialogHandler dialogHandler =
				new SharedConfirmDialogHandler(SharedConfirmDialogHandler.EDIT_SHARED);
		ConfirmChoiceDialog dialog = dialogHandler.getSharedConfirmDialog();

		return !dialog.userCancelled();
	}

	private boolean doesItSupportMultipleSymbols(ISharedPinList sharedPinList)
	{
		return sharedPinList.getType().equals(PinListTypeEnum.TypeDevice) || sharedPinList.isFunctionType();
	}

	protected void notifyPinListChanges()
	{
		m_project.getSharedPinListMgr().fireChangeEvent();
	}

	public String getActionUIClass()
	{
		return EditSharedPinListActionUI.class.getName();
	}

	public boolean isEnabled()
	{
		if (ActionRT.isDesignUnderConcurrentEdit()) {
			m_disabledReason = ResourceMgr.getString(ActionRT.class, "ActionRT.LogicMUMode");
			return false;
		}
		return getController().getCapletModel().isEditable() &&
				getOperand() != null && super.isEnabled();
	}

	// Allow "Edit Shared Pinlist" action from other places if it is intentional, if not allow only from shared tab
	@Nullable public ISharedPinList setOperand(boolean fromSharedTab)
	{
		ISharedPinList sharedPinlist = null;
		if (fromSharedTab && m_sharedSelectMgr.getSelectedObjects().getSize() == 1) {
			IUIDObject uidObj = m_sharedSelectMgr.getSelectedObjects().getNext();
			if (uidObj instanceof ISharedPinList) {
				sharedPinlist = (ISharedPinList) uidObj;
			}
		}
		else {
			SelectSet selectSet = getController().getSelectMgr().getPreSelections();
			if (selectSet.getSelectCount(new SelectionFilter(IPinList.class)) == 1) {
				IUIDObject uidObj = selectSet.getSelectedObjects(IPinList.class).get(0);
				if (uidObj != null) {
					uidObj = ((IConnectivityRef) uidObj).getConnectivity();

					if (uidObj instanceof chs.cof.logical.cable.IPinList) {
						sharedPinlist = ((chs.cof.logical.cable.IPinList) uidObj).getSharedPinList();
					}
				}
			}
		}
		if (sharedPinlist != null &&
				!(sharedPinlist.isFrozen() || sharedPinlist.getType().equals(PinListTypeEnum.TypeSplice)
						|| sharedPinlist.getType().equals(PinListTypeEnum.TypeDeviceConnector)
						|| sharedPinlist.getType().equals(PinListTypeEnum.TypeGround))) {
			operand = sharedPinlist;
		}
		else {
			operand = null;
		}

		return operand;
	}

	@Nullable
	public ISharedPinList getOperand()
	{
		return operand;
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		ISharedPinList spl = setOperand(false);
		if (spl != null) {
			EditSharedPinListActionUI ui = (EditSharedPinListActionUI) getActionUI();
			ui.setTextAndIconFromOperand(spl);
			ActionEntry ae = new ActionEntry(ui);
			container.add(ae);
			ae.setName((String) ui.getValue(Action.SHORT_DESCRIPTION));
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	public void setShowAnalysisTab(boolean b)
	{
		showAnalysisTab = b;
	}

	private abstract static class EditSPLError extends Throwable
	{

		protected ISharedPinList spl;

		EditSPLError(ISharedPinList spl)
		{
			this.spl = spl;
		}

		public abstract void showError();
	}

	private static class SPLNotEditable extends EditSPLError
	{

		SPLNotEditable(ISharedPinList spl)
		{
			super(spl);
		}

		public void showError()
		{
			String msg = ResourceMgr
					.getString(EditSharedPinListAction.class, "EditSharedPinListAction.SharedPinListNotEditable.msg");
			LogicActionMessageHelper.warn(msg);
		}
	}

	private static class SPLTypeChanged extends EditSPLError
	{

		SPLTypeChanged(ISharedPinList spl)
		{
			super(spl);
		}

		public void showError()
		{
			String msg = ResourceMgr.getString(EditSharedPinListAction.class,
					"EditSharedPinListAction.SharedPinlistTypeChanged.msg");
			LogicActionMessageHelper.warnDeleted(spl, msg);
		}
	}

	private static class SPLDoesNotExist extends EditSPLError
	{

		private boolean isMate = false;

		SPLDoesNotExist(ISharedPinList spl, boolean mate)
		{
			this(spl);
			isMate = mate;
		}

		SPLDoesNotExist(ISharedPinList spl)
		{
			super(spl);
		}

		public void showError()
		{
			if (isMate) {
				String msg = ResourceMgr.getString(EditSharedPinListAction.class,
						"EditSharedPinListAction.ConnectedSharedPinListNotEditable.msg");
				LogicActionMessageHelper.warnDeleted(spl, msg);
			}
			else {
				LogicActionMessageHelper.warnDeleted(spl);
			}
		}
	}

	private static class SPLLocked extends EditSPLError
	{

		private boolean isMate = false;

		SPLLocked(ISharedPinList spl, boolean mate)
		{
			this(spl);
			isMate = mate;
		}

		SPLLocked(ISharedPinList spl)
		{
			super(spl);
		}

		public void showError()
		{
			if (isMate) {
				String msg = ResourceMgr.getString(EditSharedPinListAction.class,
						"EditSharedPinListAction.ConnectedSharedPinListNotEditable.msg");
				LogicActionMessageHelper.warnLocked(spl, msg);
			}
			else {
				LogicActionMessageHelper.warnLocked(spl);
			}
		}
	}

	private static class SPLFrozen extends EditSPLError
	{

		private boolean isMate = false;

		SPLFrozen(ISharedPinList spl, boolean mate)
		{
			this(spl);
			isMate = mate;
		}

		SPLFrozen(ISharedPinList spl)
		{
			super(spl);
		}

		public void showError()
		{
			if (isMate) {
				String msg = ResourceMgr.getString(EditSharedPinListAction.class,
						"EditSharedPinListAction.ConnectedSharedPinListNotEditable.msg");
				LogicActionMessageHelper.warnFrozen(spl, msg);
			}
			else {
				LogicActionMessageHelper.warnFrozen(spl);
			}
		}
	}

	private static class DialogCancelled extends EditSPLError
	{

		DialogCancelled(ISharedPinList spl)
		{
			super(spl);
		}

		public void showError()
		{

		}
	}

	@Override
	protected boolean checkCache()
	{
		return false;
	}
}