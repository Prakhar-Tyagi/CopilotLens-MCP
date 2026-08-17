package chs.caplets.shared;

import chs.bridges.BridgesIntegrationServices;
import chs.caf.CAFUtils;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.helpers.DesignCapletLifecycleHelper;
import chs.caf.caplet.helpers.OpenDiagramBorderUpdater;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caplets.logic.actions.AutoviewDialog;
import chs.caplets.logic.actions.FunctionalLogicDesignAutoViewDialog;
import chs.caplets.logic.actions.GenerateFilteredDiagramDialog;
import chs.caplets.logic.actions.GenerateFilteredFunctionalDiagramDialog;
import chs.caplets.logic.actions.LayoutLogicDesignAutoViewDialog;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IWriteableDSUM;
import chs.cof.project.IProject;
import chs.cof.project.folder.IFolder;
import chs.common.DiagramGenerationException;
import chs.common.IIncLoadable;
import chs.ctf.caf.interfaces.IAdditionalDesignUIAttrsAndPropsContext;
import chs.ctf.caf.ui.NoReleaseLevelsException;
import chs.utilities.Environment;
import chs.utilities.IXMLTags;
import chs.utilities.ImmutablePair;
import chs.utilities.LifecycleUtils;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.WrappingRuntimeException;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.helpers.CreationDeletionHelper;
import com.mentor.capital.profiling.Profiler;
import com.mentor.capital.profiling.ProfilingService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JOptionPane;
import java.awt.Cursor;
import java.util.ArrayList;
import java.util.List;

public abstract class CreateNewDelegate extends BaseLifecycleDelegate
{

	protected static final Pair<Boolean, IBaseDiagram> FAILURE_RESULT = new Pair<>(false, null);

	@NotNull protected ICaplet mCaplet;
	protected String mDesignTagXML;
	protected boolean mUpdateXRefOnReadOnly;
	protected int mGridSpace;

	protected CreateNewDelegate(@NotNull ILifeCycleChangeListener lifeCycleListener,
			Class<? extends DesignCapletLifecycleHelper> resourceClass, @NotNull ICaplet caplet, String designXMLTag,
			boolean updateXrefOnReadOnly, int drawGridSpacing)
	{
		super(lifeCycleListener, resourceClass);
		mUpdateXRefOnReadOnly = updateXrefOnReadOnly;
		mCaplet = caplet;
		mGridSpace = drawGridSpacing;
		mDesignTagXML = designXMLTag;
	}

	@NotNull protected IProject getProjectFromContext(@Nullable List<?> context)
	{
		IProject contextObject = LifecycleUtils.getContextObject(context, IProject.class);
		if (contextObject == null) {
			throw new IllegalArgumentException("Wrong context for createNew()");
		}
		return contextObject;
	}

	@NotNull protected ILogicDesign getDesignFromContext(List<?> context)
	{
		ILogicDesign design = DesignCapletLifecycleHelper.getContextDesignContainer(context, ILogicDesign.class);
		if (design == null) {
			throw new IllegalArgumentException("Wrong context for createNew()");
		}
		return design;
	}

	public abstract Pair<Boolean, IBaseDiagram> createNew(List<?> context);

	public abstract List<Pair<Boolean, IBaseDiagram>> createNewWithMultipleDesigns(List<List<?>> contextList);

	@NotNull protected OpenDiagramDelegate getOpenDiagramDelegate(IProject project)
	{
		return new OpenDiagramDelegate(mLifeCycleListener, mCaplet, getResourceClass(), mDesignTagXML,
				mUpdateXRefOnReadOnly, mGridSpace);
	}

	@Nullable
	protected ICapletModel openDiagram(IProject project, @NotNull ISchemDiagram diagram)
	{
		OpenDiagramDelegate lifeCycleDelegate = getOpenDiagramDelegate(project);
		return lifeCycleDelegate.openDiagram(null, project, diagram, true);
	}

	protected boolean isEditAllowed(IDesign design, String op)
	{
		if (!design.isEditable()) {
			if (GenerateFilteredDiagramDialog.isSingleDesign()) {
				JOptionPane.showMessageDialog(CAFUtils.getInstance().getWindowMgr().getDialogFrame(), ResourceMgr
								.getString(getResourceClass(), "Lifecycle.CannotEdit.Message.text", design.getName()), op,
						JOptionPane.ERROR_MESSAGE);
			}
			return false;
		}
		return true;
	}

	protected void setUpNewDiagrams(List<ISchemDiagram> diagrams)
	{
		for (ISchemDiagram diagram : diagrams) {
			if (diagram instanceof IIncLoadable) {
				// As it's fresh
				((IIncLoadable) diagram).setSkeletonizable(false);
			}
			if (diagram != null) {
				// apply the diagram level styling
				diagram.regenerateDiagramObject();
				// we need to regenerate usages for this
				final ILogicDesign logicDesign = diagram.getDesign();
				assert logicDesign != null;
				((IWriteableDSUM) (logicDesign.getSharedUsageMgr())).regenerateUsages(diagram);
			}
		}
	}

	protected AutoviewDialog getAutoviewDialogWithMultipleDesigns(IProject project, @Nullable List<ILogicDesign> designList,
			List<IFolder> folderList,
			boolean filter, boolean designAlreadyLocked, String title)
			throws DiagramGenerationException, NoReleaseLevelsException
	{
		AutoviewDialog dialog;
		if (!filter) {
			dialog = getCreateNonFilteredAutoviewDialog(project, designList, folderList, designAlreadyLocked, title);
		}
		else {
			// Generation can remove schematic objects from a diagram,
			// so ensure that no objects are selected as these may be invalid references after generation
			ISelectMgr selMgr = CAFUtils.getInstance().getActiveSelectMgr();
			if (selMgr != null) {
				selMgr.getCurrentSelections().clear();
			}
			dialog = getCreateFilteredDiagramDialog(project, designList, folderList);
		}
		return dialog;
	}

	@NotNull
	protected AutoviewDialog getCreateNonFilteredAutoviewDialog(IProject project,
			@Nullable List<ILogicDesign> logicdesignList,
			List<IFolder> folderList,
			boolean designAlreadyLocked, String title) throws NoReleaseLevelsException, DiagramGenerationException
	{
		List<IDesign> designList = new ArrayList<>();
		if(logicdesignList!=null) {
			for (ILogicDesign des : logicdesignList) {
				designList.add(des);
			}
		}
		else {
			designList = null;
		}

		AutoviewDialog dialog;
		if (mDesignTagXML.equals(IXMLTags.FUNCTIONDESIGN)) {
			dialog = new FunctionalLogicDesignAutoViewDialog(mMainFrame, title, project, folderList, designList,
					designAlreadyLocked)
			{

				@Nullable @Override
				protected IAdditionalDesignUIAttrsAndPropsContext getAdditionalDesignUIAttrsAndPropsContext()
				{
					return ((DesignCapletLifecycleHelper) mCaplet.getLifecycle())
							.getAdditionalDesignUIUserAttrsAndPropsContext();
				}
			};
		}
		else if (mDesignTagXML.equals(IXMLTags.LAYOUTDESIGN)) {
			dialog = new LayoutLogicDesignAutoViewDialog(mMainFrame, title, project, folderList, designList,
					designAlreadyLocked)
			{

				@Nullable @Override
				protected IAdditionalDesignUIAttrsAndPropsContext getAdditionalDesignUIAttrsAndPropsContext()
				{
					return ((DesignCapletLifecycleHelper) mCaplet.getLifecycle())
							.getAdditionalDesignUIUserAttrsAndPropsContext();
				}
			};
		}
		else {
			dialog = new AutoviewDialog(mMainFrame, title, project, folderList, designList, designAlreadyLocked)
			{

				@Nullable @Override
				protected IAdditionalDesignUIAttrsAndPropsContext getAdditionalDesignUIAttrsAndPropsContext()
				{
					return ((DesignCapletLifecycleHelper) mCaplet.getLifecycle())
							.getAdditionalDesignUIUserAttrsAndPropsContext();
				}
			};
		}
		return dialog;
	}

	@NotNull protected GenerateFilteredDiagramDialog getCreateFilteredDiagramDialog(IProject project,
			@Nullable List<ILogicDesign> logicdesignList, List<IFolder> folderList)
			throws NoReleaseLevelsException, DiagramGenerationException
	{
		assert logicdesignList != null;
		List<IDesign> designList = new ArrayList<>();
		for (ILogicDesign des : logicdesignList) {
			designList.add(des);
		}

		if(mDesignTagXML.equals(IXMLTags.FUNCTIONDESIGN)) {
			return new GenerateFilteredFunctionalDiagramDialog(mMainFrame, project, folderList, designList);
		}
		else {
			return new GenerateFilteredDiagramDialog(mMainFrame, project, folderList, designList);
		}
	}

	/**
	 * @param project -
	 * @param designList -
	 * @param folderList -
	 * @param filter -
	 * @param generatedDiagrams -
	 * @param regeneratedDiagrams -
	 * @param modifiedDiagrams -
	 * @param designAlreadyLocked - dts0100512798: we need to know if the design was already locked at a high-level. If
	 * it was locked, then we need not do a refresh. Doing so would remove any changes previously made to the names of
	 * diagrams.
	 *
	 * @return -
	 */
	protected ImmutablePair<Boolean, Boolean> createDiagramByFilterWithMultipleDesigns(IProject project,
			@Nullable List<ILogicDesign> designList,
			@Nullable List<IFolder> folderList,
			boolean filter, @NotNull List<ISchemDiagram> generatedDiagrams,
			@Nullable List<ISchemDiagram> regeneratedDiagrams,
			@Nullable List<ISchemDiagram> modifiedDiagrams,
			boolean designAlreadyLocked)
	{
		// Now create a new schematic diagram, and get a name for the design if
		// it is new.
		// If bNewConnectivity is false, then we should ask the
		// user if they want to generate the new diagram from the
		// connectivity.

		CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(
				ResourceMgr.getString(getResourceClass(), "Lifecycle.CreateDiagram.Output.Start.text"));

		Cursor oldCursor = mMainFrame.getCursor();
		boolean isSuccess = false;

		boolean discardDesign = false;

		Profiler createDiagramProfiler = ProfilingService.NULL_PROFILER; // IN-2512
		try {
			mMainFrame.setCursor(new Cursor(Cursor.WAIT_CURSOR));

			String title = getNewDialogTitle();

			AutoviewDialog dialog =
					getAutoviewDialogWithMultipleDesigns(project, designList, folderList, filter, designAlreadyLocked, title);
			dialog.setupTeamcenterCheckbox(true);
			dialog.setVisible(true);
			createDiagramProfiler = startProfiling();

			isSuccess = dialog.getGenerationSucceeded();
			if (dialog.isCancelled() || !isSuccess) {
				// PW - 05/27/03 - defect #2866
				// The dialog is cancelled - so anything added in the
				// CreationDeletionHelper
				// have to be cleared
				CreationDeletionHelper cdh = CreationDeletionHelper.getTheCreationHelper();
				if (cdh != null) {
					cdh.clear();
				}
				if (dialog instanceof GenerateFilteredDiagramDialog) {
					discardDesign =
							((GenerateFilteredDiagramDialog) dialog).generationFailureRequiresDesignDiscard();
				}
			}
			else {
				generatedDiagrams.addAll(dialog.getGeneratedDiagrams());
				setUpNewDiagrams(generatedDiagrams);
				ILogicDesign des = null;
				if (designList != null && !designList.isEmpty()) {
					des = designList.get(0);
				}
				setNewDesign(dialog.getDesign());

				if (filter) {
					if (modifiedDiagrams != null) {
						modifiedDiagrams.addAll(((GenerateFilteredDiagramDialog) dialog).getModifiedDiagrams());
					}
					if (regeneratedDiagrams != null) {
						regeneratedDiagrams.addAll(((GenerateFilteredDiagramDialog) dialog).getRegeneratedDiagrams());
					}
				}

				if (dialog instanceof GenerateFilteredDiagramDialog &&
						((GenerateFilteredDiagramDialog) dialog).isRunInBackground()) {
					CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(
							ResourceMgr.getString(getResourceClass(),
									"Lifecycle.CreateDiagram.Output.TaskSubmitted.text"));
				}
				else {
					// Let the user know we are done
					CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(
							ResourceMgr.getString(getResourceClass(), "Lifecycle.CreateDiagram.Output.Finish.text"));
				}
				updateDiagramBorder();
			}
		}
		catch (final DiagramGenerationException ce) {
			Message.show(PromptSeverity.ERROR, ce.getMessageContent());
		}
		catch (NoReleaseLevelsException xcpt) {
			// dts0100453997 : no need to rethrow the exception
			//the design dialog could not find any relelase levels and a message has already been displayed
			//	Environment.getExceptionDisplay().displayException(xcpt, false);
			//	throw new WrappingRuntimeException(xcpt);

		}
		catch (Exception xcpt) {
			Environment.getExceptionDisplay().displayException(xcpt, false);
			throw new WrappingRuntimeException(xcpt);
		}
		finally {
			mMainFrame.setCursor(oldCursor);
			stopAndLogProfiler(createDiagramProfiler, "Create Diagram :");
		}

		return new ImmutablePair<Boolean, Boolean>(isSuccess, discardDesign);
	}

	protected void updateDiagramBorder()
	{
		new OpenDiagramBorderUpdater(mCaplet).updateOpenDiagramBorder();
	}

	protected void setNewDesign(ILogicDesign design)
	{

	}

	protected abstract String getNewDialogTitle();
}
