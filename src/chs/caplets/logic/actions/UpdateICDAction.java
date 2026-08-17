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
import chs.caf.IOutputWindow;
import chs.caf.OutputWindowWrapper;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionIterator;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.ui.UpdateICDSingleEndedChoice;
import chs.caplets.logic.icd.UpdateICDPersistenceHandler;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.icd.IDeviceICD;
import chs.cof.icd.IICD;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.concurrency.ILogicConcurrencyController;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedDevice;
import chs.cof.project.IProject;
import chs.cof.project.buildlist.IBuildListMgr;
import chs.cog.ICOGLockable;
import chs.common.IUIDObject;
import chs.ctf.caf.utils.CTFLockUpdateHelper;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utility.ICDUtils;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.UtilsHelper;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UpdateICDAction extends ControllerActionRT implements ICtxMenuProvider
{

	@NotNull private Model m_model;
	private final IOutputWindow m_outputWindow;
	private Set<IDevice> nonUpdateableICDs = new HashSet<>();

	public UpdateICDAction(ICapletController controller)
	{
		super(controller);
		m_outputWindow = getOutputWindow();
		m_model = (Model) controller.getCapletModel();
	}

	protected String getOutputTabName()
	{
		return UpdateICDActionHelper.getOutputTabName();
	}

	protected OutputWindowWrapper getOutputWindow()
	{
		return new OutputWindowWrapper(CAFUtils.getInstance().getOutputWindow());
	}

	@Override protected IActionEnum onActivate(ActionEvent e)
	{
		nonUpdateableICDs.clear();
		return IActionEnum.eCompleted;
	}

	public boolean isEnabled()
	{
		if (super.isEnabled() && m_model.isEditable()) {
			Map<IPinList, IDeviceICD> operands = getOperands(getPreSelections(), true);
			return !operands.isEmpty();
		}

		return false;
	}

	@NotNull protected SelectSet getPreSelections()
	{
		return getController().getSelectMgr().getPreSelections();
	}

	protected Map<IPinList, IDeviceICD> getOperands(@NotNull SelectSet sset, boolean ignoreMultipleICDDefinitions)
	{
		Map<IPinList, IDeviceICD> operands = new HashMap<>();
		ILogicDesign design = CommonUtils.cast(m_model.getDesign(), ILogicDesign.class);
		IBaseDiagram diagram = getDiagram();

		if (!getController().getCapletModel().isEditable() || design == null || diagram == null) {
			return operands;
		}

		if (ILogicConcurrencyController.isUnderLogicConcurrencyLimitation(design)) {
			return operands;
		}

		SelectionIterator iter = sset.getSelected();
		while (iter.hasNext()) {
			Selection sel = iter.getNext();
			IUIDObject selObject = sel.getObject();
			if (isICD(selObject)) {
				IDeviceICD deviceICD = design.getDesignICDContainer().constructDeviceICD((IICD) selObject);
				populateOperandsForICD(deviceICD, operands, design, diagram, ignoreMultipleICDDefinitions);
			}
			else if (isDevice(selObject)) {
				populateOperandsForDevice((IPinList) selObject, operands, design, diagram,
						ignoreMultipleICDDefinitions);
			}
		}
		return operands;
	}

	private void populateOperandsForDevice(IPinList pinList, Map<IPinList, IDeviceICD> operands,
			ILogicDesign logicDesign, IBaseDiagram diagram, boolean ignoreMultipleICDDefinitions)
	{
		IDevice device = CommonUtils.cast(pinList.getConnectivity(), IDevice.class);
		IProject project = logicDesign.getProject();
		if (device != null && project != null) {
			IBuildListMgr buildListMgr = project.getBuildListMgr();

			Set<IDeviceICD> matchingICDs =
					ICDUtils.getMatchingICDs(device, logicDesign, buildListMgr.getActiveBuildList(), true);
			if (ignoreMultipleICDDefinitions) {
				if (matchingICDs.size() >= 1) {
					addOperands(operands, diagram, matchingICDs.iterator().next(), device);
				}
			}
			else {
				if (matchingICDs.size() == 1) {
					addOperands(operands, diagram, matchingICDs.iterator().next(), device);
				}
				else if (matchingICDs.size() > 1){
					String message = ResourceMgr.getString(UpdateICDAction.class,
							"UpdateICDAction.ICDHavingMultipleICDDefinitions.message", HTMLHelper.link(device));
					m_outputWindow.sendMessage(message, getOutputTabName(), true);
				}
			}
		}
	}

	protected void populateOperandsForICD(IDeviceICD icd, Map<IPinList, IDeviceICD> operands, IDesign design,
			IBaseDiagram diagram, boolean ignoreMultipleICDDefinitions)
	{
		String icdDeviceOTIName = icd.getRole();
		IConnectivity connectivity = design.getConnectivity();
		IProject project = design.getProject();
		if (design instanceof ILogicDesign && project != null) {
			IBuildListMgr buildListMgr = project.getBuildListMgr();
			if (connectivity != null && icdDeviceOTIName != null) {
				for (IDevice device : connectivity.getAllDevices()) {
					if (device.getName().equalsIgnoreCase(icdDeviceOTIName)) {
						if (ignoreMultipleICDDefinitions) {
							addOperands(operands, diagram, icd, device);
						}
						else {
							Set<IDeviceICD> matchingICDs =
									ICDUtils.getMatchingICDs(device, (ILogicDesign) design,
											buildListMgr.getActiveBuildList(), true);
							if (matchingICDs.size() == 1) {
								addOperands(operands, diagram, matchingICDs.iterator().next(), device);
							}
							else {
								String message = ResourceMgr.getString(UpdateICDAction.class,
										"UpdateICDAction.ICDHavingMultipleICDDefinitions.message",
										HTMLHelper.link(device));
								m_outputWindow.sendMessage(message, getOutputTabName(), true);
							}
						}
						break;
					}
				}
			}
		}
	}

	private boolean isDevice(IUIDObject selObject)
	{
		return selObject instanceof IPinList &&
				((IConnectivityRef) selObject).getConnectivity() instanceof IDevice;
	}

	private boolean isICD(IUIDObject selObject)
	{
		return selObject instanceof IICD;
	}

	private void addOperands(Map<IPinList, IDeviceICD> operands, IBaseDiagram diagram, @Nullable IDeviceICD icd,
			IDevice device)
	{
		IDiagramObjectIterator representations = diagram.getRepresentations(device.getUID());
		while (representations.hasNext()) {
			IDiagramObject diagramObject = representations.next();
			if (diagramObject instanceof IPinList) {
				operands.put((IPinList) diagramObject, icd);
			}
		}
	}

	@Nullable protected IBaseDiagram getDiagram()
	{
		return CAFUtils.getInstance().getActiveDiagram();
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		ISchemDiagram diagram = m_model.getDiagram();
		final Set<Map.Entry<IPinList, IDeviceICD>> entries = getOperands(getPreSelections(), false).entrySet();
		ILogicDesign design = CommonUtils.cast(m_model.getDesign(), ILogicDesign.class);
		IProject project = design != null ? design.getProject() : null;
		UpdateICDSingleEndedChoice choice = new UpdateICDSingleEndedChoice(entries, diagram, design, project);
		boolean generateSingleEnded = choice.isSingleEndedGenerationNeeded();
		UpdateICDPersistenceHandler persistenceHandler = new UpdateICDPersistenceHandler(diagram, generateSingleEnded);
		Set<IICD> icdsToRefresh = new HashSet<>();
		entries.stream().map(entry -> entry.getValue()).forEach(iDeviceICD -> {
			icdsToRefresh.addAll(iDeviceICD.getVariants());
			icdsToRefresh.add(iDeviceICD.getICD());
		});
		UtilsHelper.getCHSSystem().getPersistenceSession().batchRefresh(icdsToRefresh);
		boolean isMutiSelect = entries.size() > 1;
		boolean sharedObjectsModified = false;
		for (Map.Entry<IPinList, IDeviceICD> entry : entries) {
			IDeviceICD icd = entry.getValue();
			IPinList pinlist = entry.getKey();
			IDevice logicDevice = CommonUtils.cast(pinlist.getConnectivity(), IDevice.class);
			if (logicDevice != null && icd != null) {
				if (nonUpdateableICDs.contains(logicDevice)) {
					continue;
				}
				if (logicDevice.isShared()) {
					ISharedDevice sharedDevice = CommonUtils.cast(logicDevice.getSharedObject(), ISharedDevice.class);
					if (sharedDevice != null) {
						boolean lockSuccess = false;
						try {
							lockSuccess = LockUpdateHelper.obtainLockOnSharedObject(sharedDevice);
							if (lockSuccess) {
								if (!sharedDevice.isFrozen()) {
									if (sharedDevice.isEditable()) {
										updateSharedICDWithDisabledUndo(icd, logicDevice, pinlist, diagram, persistenceHandler, isMutiSelect);
										sharedObjectsModified = true;
									} else {
										CTFLockUpdateHelper.displayDomainRestrictionDialog(sharedDevice);
									}
								} else {
									String message = ResourceMgr.getString(UpdateICDAction.class,
											"UpdateICDAction.output.isFrozen", HTMLHelper.link(sharedDevice));
									m_outputWindow.sendMessage(message, getOutputTabName(), true);
								}
							}
						}
						finally {
							if (lockSuccess) {
								new LockUpdateHelper((ICOGLockable)sharedDevice).flushAndUnlock(sharedObjectsModified);
							}
						}
					}
				}
				else {
					performUpdateICD(icd, logicDevice, pinlist, diagram, persistenceHandler, isMutiSelect);
				}
			}
		}
		persistenceHandler.endRoutingAll();
		if (sharedObjectsModified) {
			// Editing of shared objects is not undoable
			getController().getUndoableContainer().endEdit();
			getController().getUndoableContainer().clear();
			notifyModelChange();
		}
		return true;
	}

	private void notifyModelChange() {
		ICapletController controller = CAFUtils.getInstance().getActiveCapletController();
		if (controller != null) {
			ICapletModel model = controller.getCapletModel();
			if (model != null) {
				model.notifyModelChange(new ModelChangeEvent(model, Collections.emptyList()));
			}
		}
	}

	private void updateSharedICDWithDisabledUndo(@NotNull IDeviceICD icd, @NotNull IDevice logicDevice,
			@NotNull IPinList pinlist, @NotNull ISchemDiagram diagram,
			@NotNull UpdateICDPersistenceHandler persistenceHandler, boolean isMutiSelect)
	{
		try {
			CreationDeletionHelper.getTheCreationHelper().disableUndo();
			performUpdateICD(icd, logicDevice, pinlist, diagram, persistenceHandler, isMutiSelect);
		}
		finally {
			CreationDeletionHelper.getTheCreationHelper().enableUndo();
		}
	}

	protected void performUpdateICD(@NotNull IDeviceICD icd, @NotNull IDevice logicDevice, IPinList pinlist,
			ISchemDiagram diagram, @NotNull UpdateICDPersistenceHandler persistenceHandler, boolean isMultiSelect)
	{
		UpdateICDActionHelper updateICDActionHelper = new UpdateICDActionHelper(m_outputWindow, !isMultiSelect);

		// Update only if this is a single selection or in multi-select mode this icd update is indeed possible
		if (!isMultiSelect || isUpdateICDPossible(icd, logicDevice, updateICDActionHelper)) {
			updateICDActionHelper.performUpdateICDOnDevice(icd, logicDevice, pinlist, diagram, persistenceHandler,
					nonUpdateableICDs, true);
		}
		else {
			nonUpdateableICDs.add(logicDevice);
		}
	}

	protected void performUpdateICD(@NotNull IDeviceICD icd, @NotNull IDevice logicDevice, IPinList pinlist,
			ISchemDiagram diagram, @NotNull UpdateICDPersistenceHandler persistenceHandler)
	{
		performUpdateICD(icd, logicDevice, pinlist, diagram, persistenceHandler, false);
	}

	protected boolean isUpdateICDPossible(@NotNull IDeviceICD icd, @NotNull IDevice logicDevice,
			@NotNull UpdateICDActionHelper helper)
	{
		icd.refresh();
		return helper.isUpdateICDPossible(icd, logicDevice);
	}

//	private void logMessageToOutputWindow(IICD icd, IDevice logicDevice)
//	{
//		String message = ResourceMgr.getString(UpdateICDAction.class,
//                "UpdateICDAction.ICDWithoutPart.message", icd.getName(), HTMLHelper.link(logicDevice));
//		m_outputWindow.sendMessage(message, tabName, true);
//	}

	@Override public String getActionUIClass()
	{
		return UpdateICDActionUI.class.getName();
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (isEnabled()) {
			container.add(new ActionEntry(getActionUI(), null));
		}
	}

	@Override public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	@Override public boolean shouldDisableUndoForNonUndoableChanges()
	{
		return true;
	}
}
