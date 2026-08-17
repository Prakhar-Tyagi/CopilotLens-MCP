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

import chs.caf.CAFUtils;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletLifecycle;
import chs.caf.caplet.helpers.CapletLifecycleHelper;
import chs.caf.caplet.helpers.DesignCapletLifecycleHelper;
import chs.caf.caplet.helpers.UndoableContainerIdler;
import chs.caplets.logic.Model;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.cof.project.folder.IFolder;
import chs.cog.ICOGLockable;
import chs.cog.IPrivilegedCOGManagedLockableChildrenContainer;
import chs.common.IUIDObject;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utility.task.ITask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CreateNewDiagramDelegate extends CreateNewDelegate
{

	public CreateNewDiagramDelegate(@NotNull ILifeCycleChangeListener lifeCycleListener,
			Class<? extends DesignCapletLifecycleHelper> resourceClass, @NotNull ICaplet caplet, String designXMLTag,
			boolean updateXrefOnReadOnly, int drawGridSpacing)
	{
		super(lifeCycleListener, resourceClass, caplet, designXMLTag, updateXrefOnReadOnly, drawGridSpacing);
	}
	public List<Pair<Boolean, IBaseDiagram>> createNewWithMultipleDesigns(List<List<?>> contextList)
	{
		List<Pair<Boolean, IBaseDiagram>> extralist = new ArrayList<>();
		return extralist;
	}

	public Pair<Boolean, IBaseDiagram> createNew(List<?> context)
	{
		IProject project = getProjectFromContext(context);
		ILogicDesign design = getDesignFromContext(context);
		if (mLifeCycleListener.projectDeleted(project)) {
			return FAILURE_RESULT;
		}

		// Check for running save
		boolean anyRunningTask = anyRunningSaveForProject(project);
		if (anyRunningTask) { // The prev save is still not completed
			String projectName = project.getName();
			if (projectName == null) {
				projectName = "";
			}
			StringBuilder buffer = new StringBuilder();
			buffer.append(ResourceMgr
					.getString(CapletLifecycleHelper.class, "CapletLifecycleHelper.project.save.message", projectName));

			MessageHelper.showWarningMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
					ResourceMgr.getString(CapletLifecycleHelper.class, "CapletLifecycleHelper.project.save.title"),
					buffer.toString());
			return FAILURE_RESULT; // Cancel the create new
		}

		// None of the changes involved in adding a new or new filtered diagram are undoable.
		// This includes changes to temporary UID objects in the dialogs
		// Must ensure we reset this in a finally method just down there somewhere  V
		CAFUtils.getInstance().setTempUndoableContainer(UndoableContainerIdler.instance());

		// this is the created/generated diagram, will be used to determin the diagram to open
		ISchemDiagram diagram = null;
		boolean success = false;

		boolean designAlreadyLocked = false;
		try {
			// Check to see if we have permission to edit this design.
			if (!isEditAllowed(design, ResourceMgr.getString(getResourceClass(), "Lifecycle.NewDiagram.text"))) {
				return FAILURE_RESULT;
			}
			// Do we have a lock on this design locked, or can we one?
			designAlreadyLocked = design.isLocked();

			if (!designAlreadyLocked) {
				if (!processDesignLocking(project, context, design, diagram)) {
					// If we couldn't lock the design,
					return FAILURE_RESULT;
				}
			}

			// Get the last context
			IFolder folder = null;
			Object lastElem = context.get(context.size() - 1);
			if (lastElem instanceof IFolder) { // Folder is the parent
				folder = (IFolder) lastElem;
			}

			// Creates a diagram,  will prompt with a dialog, ***Design exists ***
			diagram = createDiagram(project, design, folder, designAlreadyLocked);

			// Create a new Model, then create a view on it.
			boolean modifiedDiagramOpen = true;
			if (diagram != null) {
				Model model = (Model) openDiagram(project, diagram);
				//
				// We HAVE to mark the model as modified - as it will get
				// orphaned on a save if not
				//
				if (model != null) {
					modifiedDiagramOpen = model.isModified();
					model.setModified(true);
				}
			}

			if (diagram != null) {
				// save the design not just restricted to the new diagram otherwise edits on other diagrams in the design may be lost (dts0100521567 )
				// Save all open diagrams only if the model has been modified before creating the new diagram (any of the currently open diagrams).
				// Otherwise, restrict the save to the new diagram
				ISchemDiagram restrictDiagram = modifiedDiagramOpen ? null : diagram;
				SaveParameters saveParameters = new SaveParameters()
						.modelModified(modifiedDiagramOpen);
				save(project, design, restrictDiagram, saveParameters);
			}
			success = diagram != null;
		}
		finally {
			// If we failed to create a diagram, and we locked the design just
			// to create a diagram, unlock it.

			if (!success && !designAlreadyLocked && design.isLocked()) {
				design.unlock();
			}

			// If we locked the folder manager, unlock it.

			CAFUtils.getInstance().clearTempUndoableContainer();
		}

		// Tell the project manager that we have edited the project
		getFIB().getProjectMgr().projectEdited(project, context);
		return new Pair<Boolean, IBaseDiagram>(true, diagram);
	}

	/**
	 * Displays the new diagram dialog to creates a diagram. Also creates a design if <i>design </i> is
	 * <code>null</code>.
	 *
	 * @param project the project
	 * @param design the design on which to create diagram
	 * @param folder the folder
	 * @param designAlreadyLocked - dts0100512798: we need to know if the design was arleady locked at a high-level. If
	 * it was locked, then we need not do a refresh. Doing so would remove any changes previously made to the names of
	 * diagrams.
	 *
	 * @return returns the newly created diagram
	 */
	@Nullable
	private ISchemDiagram createDiagram(IProject project, @NotNull ILogicDesign design, @Nullable IFolder folder,
			boolean designAlreadyLocked)
	{
		List<ISchemDiagram> diagrams = new ArrayList<ISchemDiagram>();
		createDiagram(project, design, folder, diagrams, designAlreadyLocked);
		assert diagrams.size() < 2;
		if (!diagrams.isEmpty()) {
			return diagrams.iterator().next();
		}
		return null;
	}

	/**
	 * @param project -
	 * @param design -
	 * @param folder -
	 * @param generatedDiagrams -
	 * @param designAlreadyLocked - dts0100512798: we need to know if the design was arleady locked at a high-level. If
	 * it was locked, then we need not do a refresh. Doing so would remove any changes previously made to the names of
	 * diagrams.
	 */
	protected void createDiagram(IProject project, @NotNull ILogicDesign design,
			@Nullable IFolder folder,
			@NotNull List<ISchemDiagram> generatedDiagrams,
			boolean designAlreadyLocked)
	{
		createDiagramByFilterWithMultipleDesigns(project, new ArrayList<ILogicDesign>()
		{{
			add(design);
		}}, new ArrayList<IFolder>()
		{{
			add(folder);
		}}, false, generatedDiagrams, null, null, designAlreadyLocked);
	}

	/**
	 * Construct the appropriate title for the dialog used for new design, diagram or design+diagram
	 *
	 * @return The title of the dialog
	 */
	protected String getNewDialogTitle()
	{
		// app name is deliberately not i18n - see dts0100394488
		return ResourceMgr.getString(getResourceClass(), "Lifecycle.NewDiagramDialog.Title", mCaplet.getDesignType());
	}

	protected boolean processDesignLocking(@NotNull IProject project, @Nullable List<?> context,
			@NotNull ILogicDesign design, ISchemDiagram diagram)
	{
		boolean isMultiUserMode = CapletLifecycleHelper.projectHasTopoOpenInMultiUser(project);
		if (isMultiUserMode) {
			MessageHelper.showInformationMessage(mMainFrame,
					ResourceMgr.getString(DesignCapletLifecycleHelper.class,
							"DesignCapletLifecycleHelper.msghdr.cannotCreate"),
					ResourceMgr.getString(DesignCapletLifecycleHelper.class,
							"DesignCapletLifecycleHelper.msg.cannotCreate"));
		}
		else {
			if (((IPrivilegedCOGManagedLockableChildrenContainer) design).isWeakLocked()) {
				MessageHelper.showInformationMessage(mMainFrame,
						ResourceMgr.getString(BaseLifecycleDelegate.class,
								"Lifecycle.msghdr.cannotCreateDiagram"),
						ResourceMgr.getString(BaseLifecycleDelegate.class,
								"Lifecycle.msg.cannotCreateDiagram.DesigNotLocked"));
				return false;
			}
			// Handle the possibility that it has been deleted.
			if (checkDesignExists(project, design)) {
				if (new LockUpdateHelper((ICOGLockable) design, false).lockAndRefresh()) {
					return true;
				}
				else {
					showDesignLockedErrorMessage(design);
				}
			}
		}
		return false;
	}

	@Override @NotNull protected String getResourceKeyRoot(@Nullable IProject project)
	{
		return appendEnvToResourceKeyRoot("BaseLifecycleDelegate.newdiagram.lockfailed", project);
	}

	@Nullable
	protected ITask save(IUIDObject container, @Nullable IDesign restrictDesign,
			@Nullable ISchemDiagram restrictDiagram,
			SaveParameters saveParameters)
	{
		ICapletLifecycle lifeCycleListener = ((ICapletLifecycle) mLifeCycleListener);
		if (!saveParameters.getModelModified()) {
			//Notify the listener that we are creating a new diagram and there are no other changes to process
			lifeCycleListener.executeWhileCreatingNewDiagram(() -> super
					.save(container, restrictDesign, restrictDiagram, saveParameters));
		}

		return super.save(container, restrictDesign, restrictDiagram, saveParameters);
	}
}
