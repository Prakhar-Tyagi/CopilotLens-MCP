/*
 * Copyright 2003-2008 Mentor Graphics Corporation
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
import chs.caf.AppAction;
import chs.caf.CAFUtils;
import chs.caf.ICAFProjectMgr;
import chs.caf.ICtxMenuProvider;
import chs.caf.IFIB;
import chs.caf.OutputWindowWrapper;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletLifecycle;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.helpers.CapletLifecycleHelper;
import chs.caf.caplet.helpers.ConfirmSaveDialog;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caplets.logic.Caplet;
import chs.caplets.logic.DeleteHelper;
import chs.caplets.logic.ILogicLifecycle;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IInterconnectObject;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.cof.project.folder.IFolderMgr;
import chs.common.DiagramGenerationException;
import chs.common.ICHSIterator;
import chs.common.IDesignMgr;
import chs.common.IIncLoadable;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.ReleaseLevelCategoryEnum;
import chs.ctf.caf.ui.NoReleaseLevelsException;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.images.CHSImageLoader;
import chs.system.FactoryMgr;
import chs.utilities.IBoundaryTransactionMarshaller;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.helpers.UtilsHelper;
import chs.utility.logic.ILogicModel;
import chs.utility.persist.PersistPayload;
import chs.utility.persist.ProjectStorageHelper;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner})
public class GenerateWiringDiagramInteractiveAction extends AppAction implements ICtxMenuProvider
{

	private ILogicLifecycle m_lifecycle;
	private ILogicModel m_model;
	private OutputWindowWrapper m_outputWindow;
	private GenerateWiringDiagramHelper m_diagramGeneratorHelper;
	private int m_overwriteStatus;

	private static final String LIBRARY_ERROR_HEADING = ResourceMgr.getString(
			GenerateWiringDiagramInteractiveAction.class,
			"GenerateWiringDiagramInteractiveAction.LibraryError.heading");
	private static final String LIBRARY_ERROR_MESSAGE = ResourceMgr.getString(
			GenerateWiringDiagramInteractiveAction.class,
			"GenerateWiringDiagramInteractiveAction.LibraryError.message");

	protected ConfirmSaveDialog m_confirmMissingReservationsDialog;
	protected ConfirmSaveDialog m_confirmNoMembersDialog;

	public GenerateWiringDiagramInteractiveAction(IFIB fib)
	{
		super(fib);
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		putValue(NAME, ResourceMgr.getStringForMenu(GenerateWiringDiagramInteractiveAction.class,
				"GenerateWiringDiagramInteractiveAction.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(GenerateWiringDiagramInteractiveAction.class,
				"GenerateWiringDiagramInteractiveAction.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(GenerateWiringDiagramInteractiveAction.class,
				"GenerateWiringDiagramInteractiveAction.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, new Integer(ResourceMgr.getMnemonic(GenerateWiringDiagramInteractiveAction.class,
				"GenerateWiringDiagramInteractiveAction.mnemonic.decl")));

		m_outputWindow = new OutputWindowWrapper(CAFUtils.getInstance().getOutputWindow());
		m_confirmNoMembersDialog =
				new ConfirmSaveDialog(ResourceMgr.getString(MessageHelper.class, "MessageHelper.OptionPane_3.text"),
						ResourceMgr.getString(GenerateWiringDiagramInteractiveAction.class,
								"GenerateWiringDiagramInteractiveAction.name.decl"),
						ResourceMgr.getString(GenerateWiringDiagramInteractiveAction.class,
								"GenerateWiringDiagramInteractiveAction.ConfirmNoMembers.Message"), false);
		m_confirmMissingReservationsDialog =
				new ConfirmSaveDialog(ResourceMgr.getString(MessageHelper.class, "MessageHelper.OptionPane_3.text"),
						ResourceMgr.getString(GenerateWiringDiagramInteractiveAction.class,
								"GenerateWiringDiagramInteractiveAction.name.decl"),
						ResourceMgr.getString(GenerateWiringDiagramInteractiveAction.class,
								"GenerateWiringDiagramInteractiveAction.ConfirmMissingReservations.Message"), false);
	}

	public void actionPerformed(ActionEvent e)
	{
		IBoundaryTransactionMarshaller btm = UtilsHelper.getCHSSystem().getBoundaryTransactionMarshaller();
		btm.enterTransactionBoundary(this, IBoundaryTransactionMarshaller.Nesting.MAIN);
		boolean success = false;
		try {
			doWork();
			success = true;
		}
		finally {
			btm.exitTransactionBoundary(this, success);
		}
	}

	private void doWork()
	{
		m_diagramGeneratorHelper = new GenerateWiringDiagramHelper();
		ICapletController controller = CAFUtils.getInstance().getActiveCapletController();
		m_lifecycle = (ILogicLifecycle) controller.getCaplet().getLifecycle();
		m_model = (ILogicModel) controller.getCapletModel();
		IProject project = CAFUtils.getInstance().getCurrentProject();
		IFIB fib = controller.getCaplet().getFIB();
		assert fib != null : "Null FIB!";
		ICAFProjectMgr projectMgr = fib.getProjectMgr();

		if (!runcChecks(controller, m_model)) {
			return;
		}

		m_overwriteStatus = -1;
		boolean isChanged = false;
		if (m_diagramGeneratorHelper.getHarnessList().size() == 1) {
			ICXHarness harness = m_diagramGeneratorHelper.getHarnessList().get(0);
			if (createSingleDesignAndDiagram(harness, project, harness.getName())) {
				isChanged = true;
				// FEAT 2882: removed calls to open diagram and activate To-Do browser. It's already been done
				// and the duplication meant there were two tabs showing the same diagram.

				// We generate the connectors and populate theto do list after opening the window.
				m_diagramGeneratorHelper.generateDiagram(harness, project);
				generateMessage(harness);
			}
		}
		else {
			for (ICXHarness harness : m_diagramGeneratorHelper.getHarnessList()) {
				if (createDesignAndDiagram(harness, project, harness.getName())) {
					isChanged = true;
					m_diagramGeneratorHelper.generateDiagram(harness, project);
				}

				if (m_overwriteStatus == MessageHelper.RESULT_CANCEL) {
					break;
				}
				else if (m_overwriteStatus == MessageHelper.RESULT_YES ||
						m_overwriteStatus == MessageHelper.RESULT_ALL) {
					generateMessage(harness);
				}
			}
		}
		projectMgr.projectChanged(project);
		if (((ICapletModel) m_model).isModified()) {
			// Saves the interconnect diagram.
			ICapletLifecycle lcycle = controller.getCaplet().getLifecycle();
			IDesign des = m_model.getDesign();
			List<ISchemDiagram> diagrams = Collections.singletonList(m_model.getDiagram());
			// dts0100562289 we would like to keep the design unsaved if we didn't createDesignAndDiagram (i.e. Cancelled)
			// nor generateDiagram so that the user can continue his work as if nothing happend.
			if (isChanged) {
				lcycle.save(des.getProject(), des, diagrams, true, false);
			}
		}
		// dts0100564459,dts0100562278
		// After generating the diagram and saving the generated wiring to the DB we need to set the skeletonizable flag to true
		// We also need to update the SharedUsageMgr with the data that has been saved
		if (isChanged) {
			((IIncLoadable) m_diagramGeneratorHelper.getGeneratedDiagram()).setSkeletonizable(true);
			((IIncLoadable) m_diagramGeneratorHelper.getGeneratedDesign()).setSkeletonizable(true);
			m_diagramGeneratorHelper.getGeneratedDesign().getSharedUsageMgr().designSaved();
		}
		m_diagramGeneratorHelper = null;

		// DR 348412 - All the connectors added to the wiring diagrams have been recorded in CreationDeletionHelper.
		// We need to stop them being processed as part of the next controller action.
		CreationDeletionHelper.getTheCreationHelper().processImportedObjects();
	}

	private void generateMessage(ICXHarness harness)
	{
		String message = ResourceMgr.getString(GenerateWiringDiagramInteractiveAction.class,
				"GenerateWiringDiagramInteractiveAction.generation.message",
				m_diagramGeneratorHelper.getGeneratedDesign().getName(),
				m_diagramGeneratorHelper.getGeneratedDiagram().getName(),
				harness.getName());
		m_outputWindow.sendApplicationMessage(message);
	}

	boolean runcChecks(ICapletController controller, ILogicModel model)
	{
		Frame msgFrame = CAFUtils.getInstance().getWindowMgr().getDialogFrame();
		try {
			String icxName = "";
			if (!m_diagramGeneratorHelper.findHarnesses(controller.getSelectMgr().getPreSelections(), model.getDesign(),
					model.getDiagram(), icxName)) {
				String hdg = ResourceMgr.getString(GenerateWiringDiagramInteractiveAction.class,
						"GenerateWiringDiagramInteractiveAction.UnconnectedConnectorError.heading");
				String msg = ResourceMgr.getString(GenerateWiringDiagramInteractiveAction.class,
						"GenerateWiringDiagramInteractiveAction.UnconnectedConnectorError.message", icxName);
				MessageHelper.showErrorMessage(msgFrame, hdg, msg);
				return false;
			}
		}
		catch (ICXHarnessSet.OverconnectedInterconnectException oie) {
			String hdg = ResourceMgr.getString(GenerateWiringDiagramInteractiveAction.class,
					"GenerateWiringDiagramInteractiveAction.OverconnectedConductoError.heading");
			String msg = ResourceMgr.getString(GenerateWiringDiagramInteractiveAction.class,
					"GenerateWiringDiagramInteractiveAction.OverconnectedConductoError.message", oie.getMessage());
			MessageHelper.showErrorMessage(msgFrame, hdg, msg);
			return false;
		}
		catch (ICXHarnessSet.UnderconnectedInterconnectException uie) {
			String hdg = ResourceMgr.getString(GenerateWiringDiagramInteractiveAction.class,
					"GenerateWiringDiagramInteractiveAction.UnderconnectedConductorError.heading");
			String msg = ResourceMgr.getString(GenerateWiringDiagramInteractiveAction.class,
					"GenerateWiringDiagramInteractiveAction.UnderconnectedConductoError.message", uie.getMessage());
			MessageHelper.showErrorMessage(msgFrame, hdg, msg);
			return false;
		}

		if (m_diagramGeneratorHelper.missingLibraryData()) {
			MessageHelper.showErrorMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
					LIBRARY_ERROR_HEADING, LIBRARY_ERROR_MESSAGE);
			return false;
		}
		else if (m_diagramGeneratorHelper.missingMembers() && m_confirmNoMembersDialog.userCanceled() ||
				(m_diagramGeneratorHelper.missingRepresentations() &&
						m_confirmMissingReservationsDialog.userCanceled())) {
			return false;
		}
		return true;
	}

	/**
	 * Determines whether this action can be used in the current context.
	 */
	public boolean isEnabled()
	{
		ICapletController controller = CAFUtils.getInstance().getActiveCapletController();
		if (controller == null
				|| !(controller.getCapletModel() instanceof ILogicModel)
				|| !((ILogicModel) controller.getCapletModel()).getDesign().isLocked()) {
			return false;
		}
		return getOperands(controller.getSelectMgr().getPreSelections());
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{/* Do nothing */}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (getOperands(selections)) {
			container.add(new ActionEntry(this));
		}
	}

	@Nullable private ISchemDiagram getDiagramByName(ILogicDesign design, String name)
	{
		ICHSIterator<ISchemDiagram> iter = design.getDiagrams();
		while (iter.hasNext()) {
			ISchemDiagram diagram = iter.getNext();
			if (name.equals(diagram.getName())) {
				return diagram;
			}
		}

		return null;
	}

	/*
	 * @see chs.caf.AppAction#updateUI()
	 */
	public void updateUI()
	{
		if (getFIB().getRealm() == IProject.class) {
			setEnabled(isEnabled());
			return;
		}
		setEnabled(false);
	}

	public boolean createDesignAndDiagram(ICXHarness harness, IProject project, String designName)
	{
		IDesignMgr designMgr = project.getDesignMgr();
		IFolderMgr folderMgr = project.getFolderMgr();

		String diagramName = "Diagram1";
		ISchemDiagram diagram = null;
		ICapletModel model;

		// Get the design by name if it already exists!
		ILogicDesign design = (ILogicDesign) designMgr.getDesign(designName);
		if (design != null) {
			// lock the design
			if (!design.lock()) {
				LogicActionMessageHelper.warnLocked(design);
				return false;
			}
			diagram = getDiagramByName(design, diagramName);
			if (diagram != null) {
				if (m_overwriteStatus != MessageHelper.RESULT_ALL) {
					// Display Overwrite dialog

					String heading = ResourceMgr.getString(GenerateWiringDiagramInteractiveAction.class,
							"GenerateWiringDiagramInteractiveAction.diagramOverwrite.heading");
					String message = ResourceMgr.getString(GenerateWiringDiagramInteractiveAction.class,
							"GenerateWiringDiagramInteractiveAction.diagramOverwrite.message",
							design.getFullName() + ':' + diagramName);
					m_overwriteStatus = MessageHelper
							.showYesNoAllCancelDialog(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
									heading, message);

					if (m_overwriteStatus == MessageHelper.RESULT_NO ||
							m_overwriteStatus == MessageHelper.RESULT_CANCEL) {
						// Don't overwrite
						return false;
					}
				}

				// Delete the diagram content.
				model = m_lifecycle.getModel(diagram);
				if (model != null) {
					ISelectMgr selMgr = model.getController().getSelectMgr();
					selMgr.getPreSelections().clear();
					selMgr.getPreviewSelections().clear();

					// wipe out any changes to the generated diagrams
					model.getController().getUndoableContainer().clear();
					model.setModified(false);
				}

				// todo: temporary until containers of Connectivity transition to COG
				//IPersistentLoadListener listener = UtilsHelper.getPersistentLoadListener();
				//listener.beginLoadingFromManager();

				DeleteHelper.getInstance().delete(diagram);

				//listener.endLoadingFromManager();

				project.flush();
				designMgr.flush();
			}
		}
		else // design == null
		{
			// Next create the design if there was not one to start with
			try {
				if (!LockUpdateHelper.lock(designMgr)) {
					return false;
				}

				if (!LockUpdateHelper.lock(folderMgr)) {
					return false;
				}

				IUID uid = FactoryMgr.getCommonFactory().createUID();
				chs.cof.logical.ILogicalFactory logicalFactory = FactoryMgr.getLogicalFactory();

				design = logicalFactory.constructSystemLogicDesign(uid, designName);
				if (design instanceof IIncLoadable) {
					((IIncLoadable) design).setSkeletonizable(false);
					// As it's fresh
				}

				designMgr.addDesign(design);

				// Now create the connectivity for the new design
				uid = FactoryMgr.getCommonFactory().createUID();
				chs.cof.logical.cable.ICableFactory cableFactory = FactoryMgr.getCableFactory();
				IConnectivity connectivity = cableFactory.createLogicConnectivity(uid);
				design.setConnectivity(connectivity);

				design.setReleaseLevel(project.getReleaseLevelMgr().findDraftReleaseLevel(
						ReleaseLevelCategoryEnum.DESIGN_RELEASE_LEVEL));
				design.setRevision("1");

				// Checks for local mode
				if (UtilsHelper.getCHSSystem().getData() != null) {
					// Saves the design

					// we should ONLY save the new Design/Diagram, not the complete project
					PersistPayload payload = PersistPayload.createPayload();
					ProjectStorageHelper.saveLogicDesignRequest(payload, design, true);
					payload.close();
					boolean updateSuccess = CapletLifecycleHelper.updateServerData(payload);
					if (!updateSuccess) {
						// Failed to update the server
						// Discard the changes
						Caplet.getCaplet().getLifecycle().discard(project);
					}
				}

				// Update the folder manager
				// CFE-ROM: designs now automatically added to folderMgr when added to designMgr
				// folderMgr.addDesign(folderMgr, design);

				// Flush the folderMgr to the database
				folderMgr.flush();
				designMgr.flush();
			}
			finally {
				// Check if needs to unlock folderMgr
				if (folderMgr.isLocked()) {
					// Unlock the folderMgr
					LockUpdateHelper.unlock(folderMgr);
				}

				if (designMgr.isLocked()) {
					LockUpdateHelper.unlock(designMgr);
				}
			}
		}

		// Now create the diagram if there was not one to start with
		if (diagram == null) {
			diagram = m_lifecycle.createDiagramWithName(design, diagramName);
		}

		m_diagramGeneratorHelper
				.setInterconnectSourceInfo(design, diagram, harness, m_model.getDesign(), m_model.getDiagram());

		model = m_lifecycle.openDiagram(project, diagram);

		ICapletController newController = model.getController();
		newController.activateBrowser("ToDo");

		return true;
	}

	public boolean createSingleDesignAndDiagram(ICXHarness harness, IProject project, String designName)
	{
		ISchemDiagram diagram = null;
		ILogicDesign design = null;

		Frame parent = CAFUtils.getInstance().getWindowMgr().getDialogFrame();
		String title = ResourceMgr.getString(GenerateWiringDiagramInteractiveAction.class,
				"GenerateWiringDiagramInteractiveAction.AutoViewDialog.title");
		AutoviewDialog dialog;
		try {
			dialog = new AutoviewDialog(parent, title, project, null, null);
		}
		catch (NoReleaseLevelsException e) {
			//the design dialog could not find any relelase levels and a message has already been displayed
			return false;
		}
		catch (DiagramGenerationException e) {
			// an exception was thrown during construction of the dialog, a message should already have been shown.
			return false;
		}
		dialog.setDesignName(designName);
		dialog.setVisible(true);

		if (dialog.isCancelled()) {
			CreationDeletionHelper cdh = CreationDeletionHelper.getTheCreationHelper();
			if (cdh != null) {
				cdh.flush();
			}
			return false;
		}

		diagram = dialog.getDiagram();
		if (diagram instanceof IIncLoadable) {
			// As it's fresh
			((IIncLoadable) diagram).setSkeletonizable(false);
		}
		design = dialog.getDesign();

		if (diagram == null) {
			return false;
		}

		m_diagramGeneratorHelper
				.setInterconnectSourceInfo(design, diagram, harness, m_model.getDesign(), m_model.getDiagram());

		ICapletModel newModel = m_lifecycle.openDiagram(project, diagram);

		ICapletController newController = newModel.getController();
		newController.activateBrowser("ToDo");

		return true;
	}

	/**
	 * Goes through the selection set, validating it and picking out our operands at the same time.
	 *
	 * @param sset The selection set
	 *
	 * @return The operands for this action class.
	 */
	private boolean getOperands(SelectSet sset)
	{
		SelectedUIDObjectIterator objIter = sset.getSelectedUIDObjects();
		while (objIter.hasNext()) {
			IUIDObject obj = ReferenceHelper.reduceToLogicObject(objIter.getNext());
			if (obj instanceof IInterconnectObject && (obj instanceof IConductor || obj instanceof IConnector)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Can this action change the lock count - typically this would be no, as that would be a lock being lost. However, if
	 * the action legitimately changes the lock count (e.g. generate wiring will open/lock new designs) then this should be
	 * set to true.
	 */
	public boolean mayChangeOpenLockCount()
	{
		return true; // one of the few that can
	}

	private void disposeGenerateWiringDiagramInteractiveAction()
	{
		if (m_confirmMissingReservationsDialog != null) {
			m_confirmMissingReservationsDialog.dispose();
			m_confirmMissingReservationsDialog = null;
		}

		if (m_confirmNoMembersDialog != null) {
			m_confirmNoMembersDialog.dispose();
			m_confirmNoMembersDialog = null;
		}

		m_lifecycle = null;
		m_model = null;
		m_outputWindow = null;
		m_diagramGeneratorHelper = null;
	}
}
