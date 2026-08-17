/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.caf.CAFUtils;
import chs.caf.IOutputWindow;
import chs.caf.WaitCursor;
import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caplets.logic.actions.shared.batchshare.ui.AbstractBatchShareDialog;
import chs.caplets.logic.actions.shared.batchshare.ui.BatchShareParams;
import chs.cof.logical.ILogicDesign;
import chs.cof.project.IProject;
import chs.cof.project.naming.INameMgr;
import chs.common.IUID;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.DesignsAccessibilityCheck;
import chs.utility.persist.promise.IPromise;
import chs.utility.persist.promise.PromiseFactory;
import chs.utility.persist.promise.ResponseSize;
import chs.utility.task.InterruptableTaskHelper;
import chs.utility.ui.progress.IProgress;
import chs.utility.ui.progress.Progress;
import chs.utility.ui.progress.ProgressGroup;
import chs.utility.ui.progress.ProgressTaskClient;
import org.jetbrains.annotations.NotNull;

import java.awt.Frame;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Abstract batch share action executor
 */
public abstract class AbstractBatchShareActionExecutor extends AbstractFindAndShareCmd
		implements IBatchShareActionExecutor
{

	@NotNull private Set<IEntityShareCriteria> m_entitiesShareCriteria;

	protected AbstractBatchShareActionExecutor(@NotNull IProject project, @NotNull Set<ILogicDesign> designs,
			@NotNull Set<IEntityShareCriteria> entitiesShareCriteria)
	{
		super(project, designs);
		m_entitiesShareCriteria = new HashSet<>(entitiesShareCriteria);
	}

	@Override protected void doExecute()
	{
		final Set<IShareableObjectGroup> shareableObjectGroups = new HashSet<>();
		IProgress findProgress = new Progress(0, "");
		Runnable findShareableObjectsTask =
				() -> shareableObjectGroups.addAll(findShareableObjectGroups(m_entitiesShareCriteria, findProgress));
		Frame owner = CAFUtils.getInstance().getDialogFrame();
		executeTaskWithProgressDialog(owner, findShareableObjectsTask, findProgress, false);

		BatchShareParams batchShareParams = getBatchShareParams(shareableObjectGroups);
		AbstractBatchShareDialog batchShare = getBatchShareDialog(batchShareParams);
		boolean isCancelled = !showDialog(batchShare);
		Set<ILogicDesign> openDesigns = m_designs.stream()
				.filter(design -> !CAFUtils.getInstance().getViewsForDesignContainer(design).isEmpty())
				.collect(Collectors.toSet());
		if (isCancelled) {
			m_lockedObjects.removeAll(openDesigns);
			return;
		}
		CAFCommandHelper commandHelper = new CAFCommandHelper();
		try (WaitCursor ignored = new WaitCursor()) {
			openDesigns.forEach(design -> commandHelper.closeDesignDiscardingEdits(design));
		}
		Set<IShareableObjectGroup> modifiedGroups = batchShareParams.retrieveObjectsFromUserSelection();

		ProgressGroup shareProgressGroup = new ProgressGroup(StringUtils.EMPTY_STRING);
		IProgress shareProgress = shareProgressGroup.createChild(0, 1,
				ResourceMgr.getString(AbstractBatchShareActionExecutor.class,
						"BatchShareActionExecutor.MainProgress.ShareObjects"));
		IProgress shareIntoProgress = shareProgressGroup.createChild(0, 1,
				ResourceMgr.getString(AbstractBatchShareActionExecutor.class,
						"BatchShareActionExecutor.MainProgress.ShareIntoObjects"));
		IPromise promise = PromiseFactory.createPromise();
		promise.beginGroupChildrenOf(IProject.class, m_project.getUID())
				.requestFullLoadOf(INameMgr.class, ResponseSize.SMALL)
				.endGroup()
				.requestRefreshOf(m_project.getSharedConductorMgr(), ResponseSize.SMALL)
				.issue()
				.thenApply(() -> {
							Runnable shareTask = () -> {
								share(modifiedGroups, m_reporter, shareProgress);
								shareInto(modifiedGroups, m_reporter, shareIntoProgress);
							};
							executeTaskWithProgressDialog(owner, shareTask, shareProgressGroup, true);
						}
				);
	}

	/**
	 * Constructs batch share dialog
	 *
	 * @param batchShareParams batch share data provider
	 * @return batch share dialog
	 */
	@NotNull protected abstract AbstractBatchShareDialog getBatchShareDialog(
			@NotNull BatchShareParams batchShareParams);

	@NotNull protected BatchShareParams getBatchShareParams(@NotNull Set<IShareableObjectGroup> shareableObjectGroups)
	{
		return new BatchShareParams(shareableObjectGroups, m_entitiesShareCriteria);
	}

	protected boolean showDialog(@NotNull AbstractBatchShareDialog dialog)
	{
		return dialog.showDialog(true);
	}

	protected void executeTaskWithProgressDialog(@NotNull Frame owner, @NotNull Runnable task,
			@NotNull IProgress progress, boolean childProgressSupport)
	{
		ProgressTaskClient client = new ProgressTaskClient(progress, task, IProgress::cancel);
		InterruptableTaskHelper taskHelper = InterruptableTaskHelper.instanceReset();
		taskHelper.executeTask(client, owner, "BatchShareTask", AbstractBatchShareActionExecutor.class,
				"Progress.title", "Progress.header",
				"Progress.header", null, 100, childProgressSupport, true);
	}

	@Override protected boolean checkDesignAccessibility(@NotNull Set<ILogicDesign> designs, @NotNull IProject project)
	{
		Set<IUID> designUIDs = designs.stream()
				.map(design -> design.getUID())
				.collect(Collectors.toSet());
		if (DesignsAccessibilityCheck.hasInAccessibleDesignContent(designUIDs, project)) {
			showErrorMessage();
			return false;
		}
		return true;
	}

	private void showErrorMessage()
	{
		String msg = ResourceMgr.getString(AbstractFindAndShareCmd.class, "AbstractFindAndShareCmd.InaccessibleDesign");
		String outputTabName =
				ResourceMgr.getString(AbstractFindAndShareCmd.class, "AbstractFindAndShareCmd.StatusTab.title");
		IOutputWindow outputWindow = CAFUtils.getInstance().getOutputWindow();
		outputWindow.sendMessage(msg, outputTabName, false);
	}
}
