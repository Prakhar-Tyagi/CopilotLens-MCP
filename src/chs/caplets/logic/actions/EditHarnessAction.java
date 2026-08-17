/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2006-2025 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.helpers.IHarnessEditingAction;
import chs.caf.caplet.helpers.SharedConfirmDialogHandler;
import chs.caf.caplet.helpers.editui.EditHarnessDialog;
import chs.caf.caplet.helpers.editui.ModularConnectorEditHarnessDialog;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionIterator;
import chs.capitalmanager.appserver.LockException;
import chs.caplets.logic.Model;
import chs.caplets.shared.LogicCapletUtils;
import chs.cof.changepolicy.IChangePolicyMgr;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IConnectorBase;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IShieldBody;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IRevisionedSharedObject;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedLockableUpdateableObject;
import chs.cof.logical.shared.ISharedModularConnector;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.project.IProject;
import chs.cofUtils.logical.concurrency.PropertiesConcurrencyHelper;
import chs.common.IRevisionedObject;
import chs.common.IUIDObject;
import chs.common.attr.IAttribute;
import chs.common.attr.IAttributeProvider;
import chs.common.attr.IAttributeTypes;
import chs.system.FactoryMgr;
import chs.utilities.AlphaNumComparator;
import chs.utilities.IAuditTrailLogger;
import chs.utilities.ResourceMgr;
import chs.utilities.SortedList;
import chs.utilities.StringUtils;
import chs.utilities.permission.PermissionHelper;
import chs.utilities.ui.MessageHelper;
import chs.utility.DiagramHelper;
import chs.utility.audit.AuditableEventType;
import chs.utility.helpers.BatchLockRefreshHelper;
import chs.utility.helpers.ConfirmChoiceDialog;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.SharedConductorHelper;
import chs.utility.logic.LogicUtils;
import chs.utility.preferences.StyleSetUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JLabel;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class EditHarnessAction extends ControllerActionRT implements ICtxMenuProvider, IHarnessEditingAction
{

	private Model m_model = null;
	protected String m_newHarnessValue = null;
	protected boolean m_propagateValue = false;
	protected Set<ILogicObject> m_actionables = null;
	private Set<IRepresentedObject> m_representedObjs = null;
	private String m_ctxCommand;
	@NotNull private final String m_dialogTitle;

	/**
	 * @param controller .
	 */
	public EditHarnessAction(ICapletController controller)
	{
		super(controller);
		m_model = (Model) controller.getCapletModel();
		m_dialogTitle = ResourceMgr.getString(EditHarnessDialog.class, "EditHarnessDialog.title");
	}

	/**
	 * Start the action.  If the action return eActivated it will start receiving events.
	 */
	protected IActionEnum onActivate(ActionEvent e)
	{
		m_newHarnessValue = null;
		m_representedObjs = new LinkedHashSet<IRepresentedObject>();
		SelectSet selections = getController().getSelectMgr().getPreSelections();
		m_actionables = getOperands(selections, m_representedObjs);
		if (!LogicCapletUtils.checkIfSelectionIsValidToProceedFurtherWithEditAction(selections, true)) {
			return IActionEnum.eCanceled;
		}
		return showDialog();
	}

	protected IActionEnum showDialog()
	{
		IProject project = m_model.getDesign().getProject();
		assert project != null;
		EditHarnessDialog d = getEditHarnessDialog(project);
		d.setVisible(true);
		if (d.isCancelled()) {
			m_newHarnessValue = null;
			return IActionEnum.eCanceled;
		}
		String value = d.getNewHarnessValue();
		if (StringUtils.isBlank(value)) {
			m_newHarnessValue = "";
		}
		else {
			// add harness delimters after the end of the edit/Set harness attribute
			m_newHarnessValue = value;
		}
		m_propagateValue = isPropagatable(d);
		//
		return IActionEnum.eCompleted;
	}

	private boolean isPropagatable(EditHarnessDialog dialog)
	{
		if (dialog instanceof ModularConnectorEditHarnessDialog) {
			return ((ModularConnectorEditHarnessDialog) dialog).shouldPropagateValue();
		}
		return false;
	}

	private EditHarnessDialog getEditHarnessDialog(IProject project)
	{
		Frame parentFrame = CAFUtils.getInstance().getWindowMgr().getDialogFrame();
		boolean propagationAll = false;
		if (m_actionables.size() == 1) {
			ILogicObject logObj = m_actionables.iterator().next();
			if (logObj instanceof IConnector) {
				propagationAll = shouldPropagateToChildren((IConnector) logObj);
			}
		}
		if (propagationAll) {
			return new ModularConnectorEditHarnessDialog(parentFrame, m_dialogTitle, m_actionables, project);
		}
		return new EditHarnessDialog(parentFrame, m_dialogTitle, m_actionables, project);
	}

	protected boolean shouldPropagateToChildren(IConnector connector)
	{
		ISharedPinList sharedConnector = connector.getSharedPinList();
		if (sharedConnector instanceof ISharedModularConnector) {
			return !((ISharedModularConnector) sharedConnector).getChildConnectors().isEmpty();
		}
		return !connector.getChildConnectors().isEmpty();
	}

	/**
	 * Stop the Action.  If the successful paramater is true then apply the edits to the model.
	 */
	protected boolean onTerminate(boolean successful)
	{
		if (!successful) {
			return false;
		}

		PropertiesConcurrencyHelper.collectLocksForHarnessEdit(m_actionables, m_model.getDesign(), m_propagateValue);

		Set<ISharedObject> seenSharedObjects = new HashSet<ISharedObject>();
		Set<ISharedObject> frozenObjects = new HashSet<ISharedObject>();
		Set<ISharedObject> licencePolicyShared = new HashSet<>();
		Set<ISharedObject> domainRestrictedObjects = new HashSet<ISharedObject>();
		Map<ILogicObject, String> failedToEditHarness = new HashMap<>();
		Set<ILogicObject> editableUnsharedObjects = new HashSet<>();
		Set<ISharedObject> editableSharedObjects = new HashSet<ISharedObject>();
		Set<String> sharedHarnessCollector = new HashSet<>();
		Map<ISharedLockableUpdateableObject, String> unlockableObjects =
				new HashMap<ISharedLockableUpdateableObject, String>();

		collectRequiredObjects(m_actionables, seenSharedObjects, frozenObjects, licencePolicyShared, domainRestrictedObjects,
				unlockableObjects, failedToEditHarness, editableUnsharedObjects, editableSharedObjects, sharedHarnessCollector);

		if (canProceed(editableSharedObjects, sharedHarnessCollector)) {

			applyHarnessAttribute(editableSharedObjects, editableUnsharedObjects);
			// At this point, we have a list of objects that we could not mess with (frozen or not lockable)
			outPutErrorMessages(seenSharedObjects, frozenObjects, licencePolicyShared, domainRestrictedObjects, unlockableObjects);
			unlockSharedObjects(seenSharedObjects);
			updateCompositeTexts();
		}
		else {
			unlockSharedObjects(seenSharedObjects);
			return false;
		}

		return true;
	}

	protected boolean canProceed(Set<ISharedObject> editableSharedObjects, Set<String> sharedHarnessCollector)
	{
		if(!canShowSharedWarningDialog()){
			return true;
		}
		if(editableSharedObjects.isEmpty()){
			return true;
		}
		assert !sharedHarnessCollector.isEmpty();

		if(sharedHarnessCollector.size() == 1){
			String harnessValue = sharedHarnessCollector.iterator().next();
			if(harnessValue == null){
				harnessValue = "";
			}
			if((harnessValue.trim()).equals(m_newHarnessValue.trim())){
				return true;
			}
		}

		return getUserResponse();
	}

	protected boolean canShowSharedWarningDialog()
	{
		return SharedConfirmDialogHandler.canShowDialogForEdit() &&
				LogicUtils.canShowSharedWarningDialog(getBaseDiagram());
	}

	protected boolean getUserResponse()
	{
		SharedConfirmDialogHandler dialogHandler = new SharedConfirmDialogHandler(SharedConfirmDialogHandler.EDIT_SHARED);
		ConfirmChoiceDialog dialog = dialogHandler.getSharedConfirmDialog();

		return !dialog.userCancelled();
	}

	private void applyHarnessAttribute(@NotNull Set<ISharedObject> editableSharedObjects,
			@NotNull Set<ILogicObject> editableUnsharedObjects)
	{
		Set<ISharedMulticore> rootMulticores = new LinkedHashSet<>();
		for(ISharedObject sharedObject : editableSharedObjects)	{
			setHarness(sharedObject);
			ISharedLockableUpdateableObject sharedLockableUpdateableObject= sharedObject.getLockableUpdateableRoot();
			assert sharedLockableUpdateableObject != null;

			toRootMulticore(sharedObject)
					.ifPresentOrElse(rootMulticores::add, () -> sharedLockableUpdateableObject.flush());

		}

		for(ISharedMulticore sharedMulticore : rootMulticores)
		{
			SharedConductorHelper.flushSharedMulticore(sharedMulticore, true);
		}

		for(ILogicObject unsharedObject: editableUnsharedObjects){
			setHarness(unsharedObject);
		}
	}

	@NotNull
    private Optional<ISharedMulticore> toRootMulticore(@NotNull ISharedObject sharedObject) {
		ISharedMulticore rootSharedMulticore = null;
		if (sharedObject instanceof ISharedConductor) {
			ISharedMulticore multicore = ((ISharedConductor) sharedObject).getMulticore();
			rootSharedMulticore = multicore != null ? multicore.getRootMulticore() : null;
		}
		else if (sharedObject instanceof ISharedMulticore) {
			rootSharedMulticore = ((ISharedMulticore) sharedObject).getRootMulticore();
		}
		return Optional.ofNullable(rootSharedMulticore);
	}

	private void collectRequiredObjects(@NotNull Set<ILogicObject> actionables, @NotNull Set<ISharedObject> seenSharedObjects,
			@NotNull Set<ISharedObject> frozenObjects, @NotNull Set<ISharedObject> licencePolicyShared, @NotNull Set<ISharedObject> domainRestrictedObjects,
			@NotNull Map<ISharedLockableUpdateableObject, String> unlockableObjects, @NotNull Map<ILogicObject, String> failedToEditHarness,
			@NotNull Set<ILogicObject> editableUnsharedObjects, @NotNull Set<ISharedObject> editableSharedObjects,
			@NotNull Set<String> sharedHarnessCollector)
	{
		Set<SharedObjectAndRoot> sharedObjectsToLockAndRefresh = new LinkedHashSet<>();

		for (Object m_actionable : actionables) {
			ILogicObject lo = (ILogicObject) m_actionable;
			if (!isHarnessAttributeEditable(lo)) {
				failedToEditHarness.put(lo, ResourceMgr.getString(EditHarnessAction.class,
						"EditHarnessAction.failedToEditHarness.harnessNotEditable", lo.getName()));
				continue;
			}
			if (lo instanceof IShieldBody) {
				IShieldBody sb = (IShieldBody) lo;
				if (sb.getMulticore() != null) {
					lo = sb.getMulticore();
				}
			}

			ISharedObject sobj = lo.getSharedObject();

			if (sobj == null && !allowsAttributeChange(lo)) {
				failedToEditHarness.put(lo, ResourceMgr.getString(EditHarnessAction.class,
						"EditHarnessAction.failedToEditHarness.changePolicy", lo.getName()));
				continue;
			}

			if (sobj != null) {
				if (!seenSharedObjects.add(sobj)) {
					continue;
				}
				if (sobj.isFrozen()) {
					frozenObjects.add(sobj);
					continue;
				}
				if (!allowsAttributeChange(sobj)) {
					licencePolicyShared.add(sobj);
					continue;
				}
				ISharedLockableUpdateableObject sbObj = sobj.getLockableUpdateableRoot();
				if (sbObj == null) {
					throw new IllegalArgumentException("ISharedLockableUpdateableObject not found");
				}
				if(!sbObj.isEditable()) {
					domainRestrictedObjects.add(sbObj);
					continue;
				}
				sharedObjectsToLockAndRefresh.add(new SharedObjectAndRoot(sobj, sbObj));
			}
			else {
				editableUnsharedObjects.add(lo);
			}
		}

		Set<ISharedLockableUpdateableObject> rootSharedObjects = sharedObjectsToLockAndRefresh.stream()
				.map(SharedObjectAndRoot::root)
				.collect(Collectors.toCollection(LinkedHashSet::new));

        BatchLockRefreshHelper.batchLock(rootSharedObjects);

        collectEditableSharedObjects(sharedObjectsToLockAndRefresh, unlockableObjects, editableSharedObjects,
						sharedHarnessCollector);
	}

	private void collectEditableSharedObjects(@NotNull Set<SharedObjectAndRoot> sharedObjectsToLockAndRefresh,
											  @NotNull Map<ISharedLockableUpdateableObject, String> unlockableObjects,
											  @NotNull Set<ISharedObject> editableSharedObjects, @NotNull Set<String> sharedHarnessCollector)
	{

		for(SharedObjectAndRoot objectAndRoot : sharedObjectsToLockAndRefresh) {
			ISharedLockableUpdateableObject sbObj = objectAndRoot.root();
			ISharedObject sobj = objectAndRoot.sharedObject();

			if (!sbObj.isLocked()) {
				LockException le = sbObj.getLockException();
				String msg = le != null ? le.aError : "";
				unlockableObjects.put(sbObj, msg);
			}
			else {
				editableSharedObjects.add(sobj);
				sharedHarnessCollector.add(sobj.getHarness());
				if (sbObj instanceof ISharedConnector && m_propagateValue) {
					collectChildHarness((ISharedConnector) sobj, sharedHarnessCollector);
				}
			}
		}
	}

	protected void collectChildHarness(ISharedConnector sobj, Set<String> sharedHarnessCollector)
	{
		Collection<ISharedConnector> childConnectors = sobj.getChildConnectors();
		for(ISharedConnector connector : childConnectors){
			sharedHarnessCollector.add(connector.getHarness());
			collectChildHarness(connector,sharedHarnessCollector);
		}
	}

	private void updateCompositeTexts()
	{
		if (m_representedObjs != null) {
			// Update composite texts for the selected objects
			// Introduced in 2010.2.SP1107 - dts0100766928 - the Harness is not graphically reflected in the composite wire name and it is required to performing ?Apply Style"
			StyleSetUtils.updateCompositeTexts(m_representedObjs);
		}
		else {
			assert false;
		}
	}

	private void outPutErrorMessages(@NotNull Set<ISharedObject> seenSharedObjects, @NotNull Set<ISharedObject> frozenObjects,
			@NotNull Set<ISharedObject> licencePolicyShared, @NotNull Set<ISharedObject> domainRestrictedObjects,
			@NotNull Map<ISharedLockableUpdateableObject, String> unlockableObjects)
	{
		List<ISharedObject> conglomerate = new SortedList<ISharedObject>(new AlphaNumComparator<ISharedObject>());
		conglomerate.addAll(frozenObjects);
		conglomerate.addAll(licencePolicyShared);
		conglomerate.addAll(domainRestrictedObjects);
		conglomerate.addAll(unlockableObjects.keySet());
		//Replace
		// Tweak the seen shared objects - remove the ones that we didn't tougch.
		//
		seenSharedObjects.removeAll(conglomerate);
		//
		List<String> errorMessages = new ArrayList<String>(conglomerate.size());

		handleSharedMessages(frozenObjects, licencePolicyShared, domainRestrictedObjects, unlockableObjects, conglomerate, errorMessages);

		if (!errorMessages.isEmpty()) {
			JLabel actionLabel = new JLabel();
			Font newLabelFont = actionLabel.getFont().deriveFont(Font.ITALIC, actionLabel.getFont().getSize());
			actionLabel.setFont(newLabelFont);
			StringBuilder complaintText = new StringBuilder("<html>");
			for (String msg : errorMessages) {
				complaintText.append(msg).append(".");
				complaintText.append("<br/>");
			}
			complaintText.append("</html>");
			actionLabel.setText(complaintText.toString());
			MessageHelper.showWarningMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(), actionLabel,
					m_dialogTitle,
					ResourceMgr.getString(EditHarnessAction.class, "EditHarnessAction.sharedObjects.notModified"),
					ResourceMgr.getString(EditHarnessAction.class, "EditHarnessAction.sharedObjects.reasons")
			);
		}
	}

	private void handleSharedMessages(@NotNull Set<ISharedObject> frozenObjects, @NotNull Set<ISharedObject> licencePolicyShared,
			@NotNull Set<ISharedObject> domainRestrictedObjects, @NotNull Map<ISharedLockableUpdateableObject, String> unlockableObjects,
			@NotNull List<ISharedObject> conglomerate, @NotNull List<String> errorMessages)
	{
		for (Object aConglomerate : conglomerate) {
			ISharedObject sobj = (ISharedObject) aConglomerate;
			if (frozenObjects.contains(sobj)) {
				errorMessages.add(ResourceMgr.getString(EditHarnessAction.class, "EditHarnessAction.isFrozen",
						sobj.getName()));
			}
			else if (licencePolicyShared.contains(sobj)) {
				errorMessages.add(ResourceMgr.getString(EditHarnessAction.class, "EditHarnessAction.changePolicyRestricted",
						sobj.getName()));
			}
			else if (domainRestrictedObjects.contains(sobj)) {
				errorMessages.add(ResourceMgr.getString(EditHarnessAction.class, "EditHarnessAction.isRestrictedByDomain",
						sobj.getName()));
			}
			else {
				String msg = unlockableObjects.get(sobj);
				String missingPermission = PermissionHelper.getMissingPermission(msg);
				if (missingPermission != null && !missingPermission.isEmpty()) {
					String i18nPermission = PermissionHelper.getInternationalisedName(missingPermission);
					msg = ResourceMgr.getString(EditHarnessAction.class, "EditHarnessAction.sharedObjects.permission",
									i18nPermission);
				}
				else {
					msg = ResourceMgr.getString(EditHarnessAction.class, "EditHarnessAction.sharedObjects.notLocked");
				}
				errorMessages.add(ResourceMgr.getString(EditHarnessAction.class, "EditHarnessAction.isLocked",
						sobj.getName(), msg));
			}
		}
	}

	private void unlockSharedObjects(Set<ISharedObject> seenSharedObjects)
	{
		//
		// Unlock the locked shared objects. If any, then we must flush the diagram, as well as
		// clear the undo queue.
		//
		if (!seenSharedObjects.isEmpty()) {
			IAuditTrailLogger auditLogger = FactoryMgr.getSystemFactory().getCHSSystem().getAuditLogger();
			String desc =
					ResourceMgr.getString(EditHarnessAction.class, "EditHarnessAction.sharedObjects.auditTrailDesc");
			IDesign design = m_model.getDesign();
			IProject project = design.getProject();
			assert project != null;
			String projectUIDString = project.getUID().getString();
			List<String> associatedObjectIds = new LinkedList<>();
			for (ISharedObject seenSharedObject : seenSharedObjects) {
				// dts0100406478 - need to log modification in audit trail - as usual it would be nice for this
				// to be a method, one nice clean call seenSharedObject.logAuditTrailModificationEvent(desc). It can be
				// done but need to make sure that the SharedPinListMgr/SharedConductorMgr are correct on construction
				// such that we are guaranteed to be able to get the project from the SharedObject
				String name = seenSharedObject instanceof IRevisionedSharedObject ?
						((IRevisionedObject) seenSharedObject).getFullName() : seenSharedObject.getName();
				String associatedObjectId = seenSharedObject.getUID().getString();
				associatedObjectIds.add(associatedObjectId);
				auditLogger.storeEvent(AuditableEventType.SHARED_OBJECT_MODIFIED, desc, projectUIDString, name, associatedObjectId);

				ISharedLockableUpdateableObject sbObj =
						seenSharedObject.getLockableUpdateableRoot();
				if (sbObj != null) {
					sbObj.unlock();
				}
			}
			auditLogger.postStoredEvents(associatedObjectIds);
		}
	}

	private boolean isHarnessAttributeEditable(ILogicObject logicObject)
	{
		Set<ILogicObject> objectToCheckForFacetEditability = new HashSet<>();
		objectToCheckForFacetEditability.add(logicObject);
		if (logicObject instanceof IConnector && m_propagateValue) {
			objectToCheckForFacetEditability.addAll(((IConnector) logicObject).getAllChildConnectors());
		}
		for (ILogicObject object : objectToCheckForFacetEditability) {
			IAttribute attribute = object.getAttribute(IAttributeTypes.HARNESS);
			if (attribute == null || !object.isFacetEditable(attribute)) {
				return false;
			}
		}
		return true;
	}

	protected boolean allowsAttributeChange(@NotNull IAttributeProvider attributeProvider)
	{
		return IChangePolicyMgr.Statics.allowsAttributeChange(attributeProvider, IAttributeTypes.HARNESS);
	}

	private void setHarness(ILogicObject lo)
	{
		if (lo instanceof IConnector/* && m_propagateValue*/) {
			((IConnectorBase) lo).setHarnessOnModularConnector(m_newHarnessValue, m_propagateValue);
		}
		else {
			lo.setHarness(m_newHarnessValue);
		}
	}

	private void setHarness(ISharedObject sobj)
	{
		if (sobj instanceof ISharedConnector/* && m_propagateValue*/) {
			((ISharedConnector) sobj).setHarnessOnModularConnector(m_newHarnessValue, m_propagateValue);
		}
		else {
			sobj.setHarness(m_newHarnessValue);
		}
	}

	private Set<ILogicObject> getOperands(SelectSet sset)
	{
		return getOperands(sset, null);
	}

	private Set<ILogicObject> getOperands(@NotNull SelectSet sset, @Nullable Set<IRepresentedObject> representedObjs)
	{
		if (!getController().getCapletModel().isEditable()) {// eg. read-only model
			return Collections.emptySet();
		}
		//
		Set<ILogicObject> operands = new HashSet<ILogicObject>();
		if (m_representedObjs != null) {
			m_representedObjs.clear();
		}

		if (sset.getSelectCount() > 0) {
			SelectionIterator iter = sset.getSelected();
			while (iter.hasNext()) {
				Selection sel = iter.getNext();
				IUIDObject uo = sel.getObject();
				IRepresentedObject robj = null;
				if (IRepresentedObject.class.isAssignableFrom(sel.getSelectionClass())) {
					robj = (IRepresentedObject) uo;
					uo = robj.getRawConnectivity();
				}
				//
				boolean mayAdd = false;
				// if we're a logic object and not a pin we may edit the harness attribute
				if (uo instanceof ILogicObject && !(uo instanceof IAbstractPin)) {
					if (((ILogicObject) uo).supportsEditHarnessAttribute()) {
						mayAdd = true;
					}
					// unless we have a shared object that is frozen...
					ISharedObject so = ((ILogicObject) uo).getSharedObject();
					if (so != null && so.isFrozen()) {
						mayAdd = false;
					}
				}

				if (mayAdd) {
					operands.add((ILogicObject) uo);
					if (representedObjs != null && robj != null) {
						if (robj instanceof IDiagramObject) {
							ISchemDiagram diagram = DiagramHelper.getDiagram((IDiagramObject) robj);
							if (diagram != null && diagram.isEditable()) {
								representedObjs.add(robj);
							}
						}
						else {
							representedObjs.add(robj);
						}
					}
				}
			}
		}
		return operands;
	}

	/**
	 * Should the UI for the Action be enabled
	 */
	public boolean isEnabled()
	{
		// not an option on empty selection
		Set<ILogicObject> operands = getOperands(getController().getSelectMgr().getPreSelections());
		if (operands.isEmpty()) {
			return false;
		}

		// not an option if selection only includes highways/blockdevices
		boolean containsSupportiveObjects = false;
		for (ILogicObject operand : operands) {
			if (operand.supportsEditHarnessAttribute() && !LogicObjectLockFinder
					.isLogicObjectLockedInOtherSession(operand)) {
				containsSupportiveObjects = true;
				break;
			}
		}

		return containsSupportiveObjects && super.isEnabled();
	}

	/**
	 * The Class name of the ActionUI for this Action. This is used to get back to the UI for things like it's name.
	 */
	public String getActionUIClass()
	{
		return EditHarnessActionUI.class.getName();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (isEnabled() && isBackshellandPinOnlyselected(selections)) {
			String shortDesc = (String) getActionUI().getValue(Action.SHORT_DESCRIPTION);
			if (m_ctxCommand == null || !m_ctxCommand.equalsIgnoreCase(shortDesc)) {
				// Make a private copy for command name
				m_ctxCommand = shortDesc;
			}
			container.add(new ActionEntry(getActionUI(), m_ctxCommand));
		}
	}

	private boolean isBackshellandPinOnlyselected(SelectSet selections)
	{
		return (selections.getSelectCount() - selections.getSelectedObjects(IBackshell.class).size() -
				selections.getSelectedObjects(
						IPin.class).size() > 0);
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}
}

