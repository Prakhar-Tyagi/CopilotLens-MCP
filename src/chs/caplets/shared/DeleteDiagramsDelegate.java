/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2016-2025 Siemens
 */
package chs.caplets.shared;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.IDataTransfer;
import chs.caf.caplet.helpers.DesignCapletLifecycleHelper;
import chs.caf.caplet.helpers.LogicUpdateStyledGraphicsHandler;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caplets.logic.DeleteHelper;
import chs.caplets.logic.Model;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.cof.security.FunctionalPermissionEnum;
import chs.ctf.deletedesign.DeleteDesignHelper;
import chs.utilities.ResourceMgr;
import chs.utility.audit.AuditableEventType;
import chs.utility.audit.DiagramAuditTrialHelper;
import chs.utility.helpers.LogicDesignAssociationChecker;
import com.mentor.capital.profiling.Profiler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DeleteDiagramsDelegate extends AbstractDeleteDiagramsDelegate
{

	public DeleteDiagramsDelegate(@NotNull ILifeCycleChangeListener lifeCycleListener,
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
	protected boolean deleteDiagrams(@NotNull DeleteDesignHelper deleteDesignHelper, IProject project,
			List<ISchemDiagram> diagramListInMap, ILogicDesign designInMap)
	{
		if (designInMap == null) {
			return true;
		}

		if (!checkEditDesignPermission()) {
			return false;
		}

		if (new LogicDesignAssociationChecker(designInMap).execute()) {
			// If the design is associated with a open topology design in MU mode, we cannot delete it.  The would
			// result in us having to perform a functional sync, and this isn't allowed in MU mode.
			deleteDesignHelper.collectErrors(
					ResourceMgr.getString(getResourceClass(), "BaseLifecycle.cannotDelete.diagram.message"));
			return false;
		}

		return deleteDiagrams(project, diagramListInMap, designInMap);
	}

	private boolean deleteDiagrams(IProject iproject, List<ISchemDiagram> diagramList, ILogicDesign idesign)
	{
		boolean alreadyLockedInMemory = idesign.isLocked();

		if (!checkEditDesignPermission()) {
			return false;
		}

		if (!checkDesignExists(iproject, idesign)) {
			return true;
		}

		try {

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
				if (!deleteDiagram(iproject, theDiagramToDelete)) {
					return false;
				}
			}

			updateOpenDiagramBorder();

			new LogicUpdateStyledGraphicsHandler().updateOpenedDiagramsStyledTableGraphics(idesign);

			SaveParameters saveParameters = new SaveParameters()
					.saveAlways(true)
					.runDRCs(true)
					.modelModified(true);
			save(iproject, idesign, null, saveParameters);

			simulateWindowSwitching();
		}
		finally {

			// Unlock the design if there are no more open Models on the design, or if its not allready locked in memory
			//if we are deleting the diagrams such that there are no windows are left for design,
			//then this equivalent to close design. because even if we have deleted diagrams such
			//that no windows are left open for design, we may still have some diagrams in the model
			//which were opened but close in-between before the deletion of diagrams. see dts0100877932.
			boolean noModelOnDesign = true;
			Model model = getModel(idesign);
			boolean isEquivalentCloseDesign = false;
			if (model != null) {
				noModelOnDesign = model.getDiagrams().isEmpty();
				isEquivalentCloseDesign = CAFUtils.getInstance().getCapletWindowsForDesign(idesign).isEmpty();
			}
			if (isEquivalentCloseDesign) {
				((BaseLifecycle) mLifeCycleListener).closeDesign(iproject, idesign);
			}
			else if (noModelOnDesign || !alreadyLockedInMemory) {
				idesign.unlock();
			}
		}

		return true;
	}

	/**
	 * Deletes a single diagram only used from deleteDiagrams. Does *not* save the design.
	 *
	 * @param project Project diagram belongs to
	 * @param diagram Diagram to delete
	 *
	 * @return True iff the diagram was deleted and succesfully written to the DB
	 */

	protected boolean deleteDiagram(IProject project, ISchemDiagram diagram)
	{
		IBaseDiagram preDeleteActiveDiagram = getActiveDiagram();

		Profiler profiler = startProfiling();// Since when we are deleting connectivity we will do save
		// Check for running save
		boolean anyRunningTask = anyRunningSaveForProject(project);
		if (anyRunningTask) { // The prev save is still not completed
			return false; // Cancel the create new
		}

		/*
			1. When we add fresh content to a diagram, the undo information for these actions in empty.

			2. After adding, if we delete diagram with deleting connectivity (CAVAL exposes this flag when users have CAVAL licenses and an environ variable is Set), then the call to
				closeWindowsForDiagram is doing an UNDO. So all the connectivity gets deleted. In effect, we deleted the connectivity even though we did not want. After this users
				can't use CAVAL as there is no connectivity.

				So move the following code before class to closeWindowForDiagrm (It was after this call before this)
		*/

		ILogicDesign design = diagram.getDesign();
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

		// This will only create a model (and other stuff necessary fo the deletion of connectivity)
		// if the diagram is not already loaded, otherwise it does nothing.
		boolean cleanupSharedObjectListeners = getModel(design) == null;
		Model newModel = createNewSchematicModel(project, design, diagram);

		// Clear preselections so there will not be anything deleted on the selection list, even temporarily.
		model = getModel(diagram.getDesign());
		if (model != null) {
			ISelectMgr selMgr = model.getController().getSelectMgr();
			selMgr.getPreSelections().clear();
			clearPasteBuffer(model);
		}

		// Delete the diagram content.
		DiagramAuditTrialHelper.getInstance().storeDiagramAuditTrail(diagram, AuditableEventType.DIAGRAM_DELETED,null);
		DeleteHelper.getInstance().delete(diagram);
		clearDiagram(diagram, design);
		cleanUpModel(diagram, design, model, preDeleteActiveDiagram, oldCurrentDiagram);

		if (cleanupSharedObjectListeners && newModel != null) {
			((BaseLifecycle) mLifeCycleListener).destroyModel(newModel);
		}

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

	protected void clearPasteBuffer(@NotNull Model model)
	{
		ICapletController capletController = model.getController();
		if (capletController != null) {
			IDataTransfer dataTransfer = capletController.getDataTransfer();
			// Clear the paste buffer so that we don't paste deleted objects
			Objects.requireNonNull(dataTransfer).clearPasteBuffer();
		}
	}

	@Nullable protected final Model createNewSchematicModel(@NotNull IProject project, @NotNull ILogicDesign design,
			@NotNull ISchemDiagram diagram)
	{
		Model model = mLifeCycleListener.findModel(design);
		if (model != null && model.containsDiagram(diagram)) {
			model.setCurrentDiagram(diagram);
			return model;
		}

		//
		// If it's not a new design, we need to get a lock on behalf of the diagram.
		// Prompt user to open ready only, if existing locks exists. When we are deleting a diagram we dont want to be
		// warned about buildlist, as that only applies to designs.
		//

		// If diagram is not loaded in memory then it should be an existing diagram ?
		if (diagram.isDeleted()) {
			diagramRemotelyDeleted(project, null);
			return null;
		}

		return createModel(project, design, diagram, mCaplet, model);
	}
}
