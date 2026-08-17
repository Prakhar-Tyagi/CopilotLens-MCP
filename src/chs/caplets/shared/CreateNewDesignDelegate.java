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
import chs.caf.caplet.helpers.DesignCapletLifecycleHelper;
import chs.caf.caplet.helpers.UndoableContainerIdler;
import chs.caplets.logic.Model;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.cof.project.folder.IFolder;
import chs.common.IIncLoadable;
import chs.utilities.ImmutablePair;
import chs.utilities.LifecycleUtils;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CreateNewDesignDelegate extends CreateNewDelegate
{

	private ILogicDesign mNewDesign;

	public CreateNewDesignDelegate(@NotNull ILifeCycleChangeListener lifeCycleListener,
			Class<? extends DesignCapletLifecycleHelper> resourceClass,
			@NotNull ICaplet caplet, String designXMLTag, boolean updateXrefOnReadOnly, int drawGridSpacing)
	{
		super(lifeCycleListener, resourceClass, caplet, designXMLTag, updateXrefOnReadOnly, drawGridSpacing);
	}

	@Override public Pair<Boolean, IBaseDiagram> createNew(List<?> context)
	{
		IProject project = LifecycleUtils.getContextObject(context, IProject.class);
		if (project == null) {
			throw new IllegalArgumentException("Wrong context for createNew()");
		}

		ILogicDesign design = DesignCapletLifecycleHelper.getContextDesignContainer(context, ILogicDesign.class);

		if (design != null) {
			throw new IllegalArgumentException("Wrong context for createNew()");
		}

		if (mLifeCycleListener.projectDeleted(project)) {
			return FAILURE_RESULT;
		}

		// Check for running save
		boolean anyRunningTask = anyRunningSaveForProject(project);
		if (anyRunningTask) { // The prev save is still not completed
			return FAILURE_RESULT; // Cancel the create new
		}

		// None of the changes involved in adding a new or new filtered diagram are undoable.
		// This includes changes to temporary UID objects in the dialogs
		// Must ensure we reset this in a finally method just down there somewhere  V
		CAFUtils.getInstance().setTempUndoableContainer(UndoableContainerIdler.instance());

		// this is the created/generated diagram, will be used to determin the diagram to open
		ISchemDiagram diagram = null;

		try {
			// Creating a new diagram in a new design.
			// We'll need to update the folder manager to accomodate the new
			// design
			// - Whenever we update the folderMgr, we should also update the Design Mgr.
			project.refreshDesignListAndFolderMgr();

			// Get the last context
			IFolder folder = null;
			Object lastElem = context.get(context.size() - 1);
			if (lastElem instanceof IFolder) { // Folder is the parent
				folder = (IFolder) lastElem;
			}

			// Creates a design and diagram , will prompt with a dialog, **** Design does not exist YET ***
			diagram = createDesign(project, folder);

			if (mLifeCycleListener.projectDeleted(project)) {
				// Project was deleted while user was in dialog! .t Dont check for design as i had not been
				// created.
				return FAILURE_RESULT;
			}

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

			if (mNewDesign != null) {
				SaveParameters saveParameters = new SaveParameters()
						.saveAlways(true)
						.modelModified(modifiedDiagramOpen);
				save(project, mNewDesign, diagram, saveParameters);
				if (diagram == null) {
					if (mNewDesign instanceof IIncLoadable) {
						((IIncLoadable) mNewDesign).setSkeletonizable(true);
					}
					// There's little or nothing to unload, but there's
					// bookkeeping to be done anyway.
					mNewDesign.unloadChildren();
				}
				mNewDesign = null;
			}
			else if (diagram != null) { // Is this a valid case in New Design flow?
				// save the design not just restricted to the new diagram otherwise edits on other diagrams in the design may be lost (dts0100521567 )
				// Save all open diagrams only if the model has been modified before creating the new diagram (any of the currently open diagrams).
				// Otherwise, restrict the save to the new diagram
				ISchemDiagram restrictDiagram = modifiedDiagramOpen ? null : diagram;
				SaveParameters saveParameters = new SaveParameters()
						.modelModified(modifiedDiagramOpen);
				save(project, null, restrictDiagram, saveParameters);
			}
		}
		finally {
			CAFUtils.getInstance().clearTempUndoableContainer();
		}

		// Tell the project manager that we have edited the project
		getFIB().getProjectMgr().projectEdited(project, context);
		return new Pair<Boolean, IBaseDiagram>(true, diagram);
	}

	@Override public List<Pair<Boolean, IBaseDiagram>> createNewWithMultipleDesigns(List<List<?>> contextList)
	{
		List<Pair<Boolean, IBaseDiagram>> extralist = new ArrayList<>();
		return extralist;
	}

	/**
	 * Displays the new diagram dialog to creates a diagram. Also creates a design if <i>design </i> is
	 * <code>null</code>.
	 *
	 * @param project the project
	 * @param folder the folder
	 *
	 * @return returns the newly created diagram
	 */
	@Nullable
	private ISchemDiagram createDesign(IProject project, IFolder folder)
	{
		List<ISchemDiagram> diagrams = new ArrayList<ISchemDiagram>();
		createDesign(project, folder, diagrams);
		assert diagrams.size() < 2;
		if (!diagrams.isEmpty()) {
			return diagrams.iterator().next();
		}
		return null;
	}

	@Override protected void setNewDesign(ILogicDesign design)
	{
		mNewDesign = design;
	}

	protected ImmutablePair<Boolean, Boolean> createDesign(IProject project, IFolder folder,
			@NotNull List<ISchemDiagram> generatedDiagrams)
	{
		mNewDesign = null;
		return createDiagramByFilterWithMultipleDesigns(project, null, new ArrayList<IFolder>()
		{{
			add(folder);
		}}, false, generatedDiagrams, null, null, false);
	}

	protected String getNewDialogTitle()
	{
		// app name is deliberately not i18n - see dts0100394488
		return ResourceMgr.getString(getResourceClass(), "Lifecycle.NewDesignDialog.Title", mCaplet.getDesignType());
	}
}
