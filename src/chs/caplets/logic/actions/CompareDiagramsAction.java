/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2011-2022 Siemens
 */

package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.IFIB;
import chs.caf.QAExtensionAppAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.cof.logical.ILogicDesign;
import chs.cof.project.IProject;
import chs.cof.project.buildlist.IBuildList;
import chs.common.IDesignContainer;
import chs.common.IDesignDescriptor;
import chs.common.IUID;
import chs.ctf.ui.form.AbstractEntryDialog;
import chs.ctf.ui.form.DefaultDesignTypeFilterController;
import chs.ctf.ui.form.DesignRevisionComponent;
import chs.system.FactoryMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.Environment;
import chs.utility.ILogger;
import chs.utility.ProjectSneaker;
import chs.utility.helpers.LogHelper;
import chs.utility.helpers.ProjectSneakerPluggablePermissionEnabler;
import chs.utility.task.InterruptableTaskHelper;
import chs.utility.ui.progress.ProgressGroup;
import chs.utility.ui.progress.ProgressTaskClient;
import chs.view.utils.DiagramCompareUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JDialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: ksistla Date: Aug 19, 2011 Time: 1:05:43 PM To change this template use File |
 * Settings | File Templates.
 */
@ApplicationSpecification(
		allowInQAExtensionsFor = {Application.CapitalLogicDesigner, Application.ArtisanFunction},
		allowInDevExtensionsFor = {Application.CapitalLogicDesigner, Application.ArtisanFunction}
)
public class CompareDiagramsAction extends QAExtensionAppAction
{

	public CompareDiagramsAction(IFIB fib)
	{
		super(fib);
		putValue(Action.NAME, "Compare diagrams in two designs");
	}

	public void actionPerformed(ActionEvent e)
	{
		ILogger logger = LogHelper.getLogger();
		DesignRevisionComponent dialog = getDesignRevisionComponent(logger);
		if (dialog == null) {
			return;
		}

		if (dialog.getExitStatus() == AbstractEntryDialog.CANCEL) {
			return;
		}
		Collection<IDesignDescriptor> designs =
				CollectionUtils.getObjectList(dialog.getDesigns(), IDesignDescriptor.class);
		List<IBuildList> buildLists = dialog.getSelectedBuildLists();

		if (designs.size() != 2 && buildLists.size() != 2) {
			logDebugMsg("Select two logic designs or two build lists for comparison\n");
			return;
		}

		boolean compareBuildLists = false;
		if (buildLists.size() == 2) {
			compareBuildLists = true;
		}

		String titleString = "Diagram comparison";
		final ProgressGroup progress = new ProgressGroup(titleString);

		Runnable compareRunnable = null;
		if (compareBuildLists) {
			//build list comparision
			compareRunnable = getBuildListDesignsCompareRunnable(logger, buildLists, progress);
		}
		else {
			//design comparision
			compareRunnable = getDesignCompareRunnable(logger, designs, progress);
		}

		if (compareRunnable == null) {
			return;
		}

		final Frame frame = CAFUtils.getInstance().getWindowMgr().getDialogFrame();
		if (frame == null) {
			compareRunnable.run();
			return;
		}

		ProgressTaskClient ptc = new ProgressTaskClient(progress, compareRunnable, prog -> {});
		final InterruptableTaskHelper ith = InterruptableTaskHelper.instanceReset();
		ith.setStrings(titleString, titleString, titleString);
		ith.executeTask(ptc, frame,
				titleString, CAVALUtils.getDefaultProgressMonitorInterval(), true, true);
	}

	@Nullable
	private Runnable getDesignCompareRunnable(@Nullable ILogger logger, Collection<IDesignDescriptor> designs,
			final ProgressGroup progress)
	{
		Iterator<IDesignDescriptor> iterator = designs.iterator();
		final IDesignContainer design1 = iterator.next().getDesignContainer();
		final IDesignContainer design2 = iterator.next().getDesignContainer();
		if (!(design1 instanceof ILogicDesign) || !(design2 instanceof ILogicDesign)) {
			if (logger != null) {
				logger.debugMsg("Select two logic designs for comparison\n");
			}
			return null;
		}

		final DiagramCompareUtil designComparer = DiagramCompareUtil.getDesignComparer((ILogicDesign) design1,
				(ILogicDesign) design2);
		progress.setRange(designComparer.getProgressRange());

		return (() -> {
			designComparer.compareDiagarms(
					Environment.getTemp() + File.separator + cleanName(design1.getName()) + '_' +
							cleanName(design2.getName()) + File.separator, progress);
		});
	}

	@Nullable
	private Runnable getBuildListDesignsCompareRunnable(@Nullable ILogger logger, List<IBuildList> buildLists,
			final ProgressGroup progress)
	{
		final IBuildList buildList1 = buildLists.get(0);
		final IBuildList buildList2 = buildLists.get(1);
		if (!buildList1.isLogicalBuildList() || !buildList2.isLogicalBuildList()) {
			if (logger != null) {
				logger.debugMsg("Select two logic build lists for comparison\n");
			}
			return null;
		}
		final DiagramCompareUtil buildListComparer =
				DiagramCompareUtil.getBuildListComparer(buildList1, buildList2);

		return (() -> {
			buildListComparer.compareDiagramsInBuildList(
					Environment.getTemp() + File.separator + cleanName(buildList1.getName()) + '_' +
							cleanName(buildList2.getName()) + File.separator, progress);
		});
	}

	@Nullable private DesignRevisionComponent getDesignRevisionComponent(@Nullable ILogger logger)
	{
		IProject project = getProject();

		if (project == null) {
			if (logger != null) {
				logger.debugMsg("No project is open to invoke this action\n");
			}
			return null;
		}

		DesignRevisionComponent dialog =
				DesignRevisionComponent.showDialog(new JDialog(),
						new ProjectSneaker(getFIB().getProjectMgr(),
								FactoryMgr.getSystemFactory().getCHSSystem().getProjectMgr(),
								new ProjectSneakerPluggablePermissionEnabler()),
						project,
						new HashSet<IUID>(),
						allPass -> true,
						true,
						true,
						validator -> true,
						new DefaultDesignTypeFilterController());

		if (dialog == null) {
			if (logger != null) {
				logger.debugMsg("Select two designs for comparison\n");
			}
			return null;
		}
		return dialog;
	}

	@NotNull private String cleanName(String name1)
	{
		String cleaned = name1.replace('/', '_');
		cleaned = cleaned.replace(':', '_');
		return cleaned;
	}

	private void logDebugMsg(String message)
	{
		ILogger logger = LogHelper.getLogger();
		if (logger != null) {
			logger.debugMsg(message);
		}
	}
}
