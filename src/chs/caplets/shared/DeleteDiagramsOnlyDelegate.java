/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.shared;

import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.DesignCapletLifecycleHelper;
import chs.caf.caplet.helpers.LogicUpdateStyledGraphicsHandler;
import chs.caplets.logic.Model;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.cof.security.FunctionalPermissionEnum;
import chs.common.IProjectPreferenceMgr;
import chs.ctf.deletedesign.DeleteDesignHelper;
import chs.utilities.ResourceMgr;
import chs.utility.audit.AuditableEventType;
import chs.utility.audit.DiagramAuditTrialHelper;
import com.mentor.capital.profiling.Profiler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DeleteDiagramsOnlyDelegate extends AbstractDeleteDiagramsDelegate
{

	public DeleteDiagramsOnlyDelegate(@NotNull ILifeCycleChangeListener lifeCycleListener,
			Class<? extends DesignCapletLifecycleHelper> resourceClass, @NotNull ICaplet caplet,
			FunctionalPermissionEnum editDesignPermission)
	{
		super(lifeCycleListener, resourceClass, caplet, editDesignPermission);
	}

	/**
	 * @param project the project
	 * @param diagramListInMap the lsit of diagram to delete
	 * @param designInMap the design
	 *
	 * @return boolean return true if any diagram deleted
	 */
	@Override protected boolean deleteDiagrams(@NotNull DeleteDesignHelper deleteDesignHelper, IProject project,
			List<ISchemDiagram> diagramListInMap, ILogicDesign designInMap)
	{
		if (designInMap == null) {
			return true;
		}
		return deleteDiagramsOnly(project, diagramListInMap, designInMap);
	}

	public boolean deleteDiagramsOnly(@NotNull IProject project, @NotNull List<ISchemDiagram> diagramList,
			@NotNull ILogicDesign design)
	{
		if (!isEditAllowed(design, ResourceMgr.getString(getResourceClass(), "Lifecycle.Delete.text"))) {
			return false;
		}

		if (!checkEditDesignPermission()) {
			return false;
		}

		boolean originalPurgeOnSaveFlag = true;
		IProjectPreferenceMgr projectPreferenceMgr = project.getPreferences();
		if (projectPreferenceMgr != null) {
			originalPurgeOnSaveFlag = projectPreferenceMgr.getPurgeOnSave();
			projectPreferenceMgr.putPurgeOnSave(false);
		}

		boolean returnFlag = false;
		try {
			returnFlag = deleteDiagrams(project, diagramList, design);
		}
		finally {
			// reset the original value for purge on save.
			if (projectPreferenceMgr != null) {
				projectPreferenceMgr.putPurgeOnSave(originalPurgeOnSaveFlag);
			}
		}
		return returnFlag;
	}

	private boolean deleteDiagrams(IProject iproject, List<ISchemDiagram> diagramList, ILogicDesign idesign)
	{
		if (!checkEditDesignPermission()) {
			return false;
		}

		if (!checkDesignExists(iproject, idesign)) {
			return true;
		}

		// Lock design and check it exists
		boolean isLocked = idesign.lock();

		List<ISchemDiagram> diagramsToBeDeleted = new ArrayList<ISchemDiagram>();
		for (ISchemDiagram diagram : diagramList) {
			if (diagram.isDeleted()) {
				diagramRemotelyDeleted(iproject, null);
			}
			else {
				diagramsToBeDeleted.add(diagram);
			}
		}

		if (diagramsToBeDeleted.isEmpty()) {
			return true;
		}

		if (!isLocked) {
			return false;
		}

		for (ISchemDiagram theDiagramToDelete : diagramsToBeDeleted) {
			if (!deleteDiagram(theDiagramToDelete)) {
				return false;
			}
		}

		updateOpenDiagramBorder();

		new LogicUpdateStyledGraphicsHandler().updateOpenedDiagramsStyledTableGraphics(idesign);

		SaveParameters saveParameters = new SaveParameters()
				.modelModified(true)
				.runDRCs(true)
				.saveAlways(true);
		save(iproject, idesign, null, saveParameters);

		// we don't need to trigger sync of integrator if we are not changing the connectivity

		return true;
	}

	/**
	 * Deletes a single diagram only used from deleteDiagrams. Does *not* save the design.
	 *
	 * @param diagram Diagram to delete
	 *
	 * @return True iff the diagram was deleted and succesfully written to the DB
	 */

	protected boolean deleteDiagram(ISchemDiagram diagram)
	{
        IBaseDiagram preDeleteActiveDiagram = getActiveDiagram();

        Profiler profiler = startProfiling();

		/*
			1. When we add fresh content to a diagram, the undo information for these actions in empty.

			2. After adding, if we delete diagram with deleting connectivity (CAVAL exposes this flag when users have CAVAL licenses and an environ variable is Set), then the call to
				closeWindowsForDiagram is doing an UNDO. So all the connectivity gets deleted. In effect, we deleted the connectivity even though we did not want. After this users
				can't use CAVAL as there is no connectivity.

				So move the following code before class to closeWindowForDiagrm (It was after this call before this)
		*/

		ILogicDesign design = diagram.getDesign();
		DiagramAuditTrialHelper.getInstance().storeDiagramAuditTrail(diagram, AuditableEventType.DIAGRAM_DELETED, null);
		assert design != null;
		Model model = getModel(design);
		if (model != null) {
			// gdh 11/21/03 re: 3952 connectivity not deleted: leave it "as
			// is" for possible autogen
			model.setModified(false);
			model.getController().getUndoableContainer().clear();
		}
		// First close the diagram windows, if any are open. This *must* be done before we delete anything, it can have
		// many side effects which could depend on the data model being valid and shared usages being up to date
		((DesignCapletLifecycleHelper) mLifeCycleListener).closeWindowsForDiagram(diagram);

        ISchemDiagram oldCurrentDiagram = null;
        if (model != null) {
            // cache the model's current diagram now as closeWindowsForDiagram might have changed it
            oldCurrentDiagram = model.getDiagram();
        }

		clearDiagram(diagram, design);
		cleanUpModel(diagram, design, model, preDeleteActiveDiagram, oldCurrentDiagram);

		// Finally delete the diagram from the database when our in-memory model is up to date.
		boolean result = deleteDiagramFromDB(diagram);
		stopAndLogProfiler(profiler, "Delete Diagram :");
		if (result) {
			DiagramAuditTrialHelper.getInstance().postStoredEvents();
		}
		else {
			DiagramAuditTrialHelper.getInstance().discardStoredEvents();
		}
		return result;
	}
}
