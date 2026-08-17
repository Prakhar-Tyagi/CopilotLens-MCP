/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caf.cafmain.actions.capture.sysml;

import chs.aws.ui.handlers.sysmltcsoa.ISysMLSoaHandler;
import chs.aws.ui.handlers.sysmltcsoa.progressDialog.SysMLProgressChangeListener;
import chs.aws.ui.handlers.sysmltcsoa.progressDialog.SysMLSOAImportProgress;
import chs.aws.ui.handlers.sysmltcsoa.progressDialog.SysMLSOAProgressEvent;
import chs.bridges.adaptors.tcmbse.IImportConfig;
import chs.caf.CAFUtils;
import chs.cof.project.IProject;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.task.InterruptableTaskHelper;
import chs.utility.ui.progress.IProgress;
import chs.utility.ui.progress.IProgressCancelledHandler;
import chs.utility.ui.progress.ProgressGroup;
import chs.utility.ui.progress.ProgressTaskClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Frame;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeSupport;

/**
 * Imports sysml model from teamcenter
 */
public class SysMLSoaModelImporter implements ISysMLSoaModelImporter
{
	public static final String PROGRESS_GROUP_NAME = "";
	private final SysMLErrorReporter sysMLErrorReporter;
	private final SysMLSoaModelImportService importService;

	public SysMLSoaModelImporter(ISysMLSoaHandler soaHandler)
	{
		sysMLErrorReporter = new SysMLErrorReporter();
		importService = new SysMLSoaModelImportService(soaHandler, sysMLErrorReporter);
	}

	public void performImport(@Nullable String selectedObjectDetails)
	{
		IImportConfig importConfig = getImportConfig();
		if (importConfig == null) {
			return;
		}

		if (selectedObjectDetails == null) {
			sysMLErrorReporter.reportInOutputTab(PromptSeverity.INFORMATION,
					getResourceString("SysMLSoaImporter.object.not.selected"));
			return;
		}
		ProgressGroup progressGroup = new ProgressGroup(PROGRESS_GROUP_NAME);
		progressGroup.setRange(SysMLSOAProgressEvent.PROGRESS_COUNT);
		SysMLProgressChangeListener pcl = new SysMLProgressChangeListener(progressGroup);
		PropertyChangeSupport pcs = new PropertyChangeSupport(this);

		SysMLSOAImportProgress progress = new SysMLSOAImportProgress(pcl);

		Runnable importerTask = () -> importService.importSysMLModel(selectedObjectDetails, progress, pcs, importConfig);
		CancellableProgressTaskClient
				ptc = new CancellableProgressTaskClient(progressGroup, importerTask, (p) -> {
		}, pcs);
		startTaskExecution(ptc);
	}

	private void startTaskExecution(ProgressTaskClient ptc)
	{
		Frame owner = CAFUtils.getInstance().getWindowMgr().getDialogFrame();
		InterruptableTaskHelper interruptableTaskHelper = InterruptableTaskHelper.instanceReset();
		interruptableTaskHelper.executeTask(ptc,
				owner,
				"ImportSysML",
				SysMLSoaModelImporter.class,
				"progressDialog.title",
				"progressDialog.header.resource",
				"progressDialog.title",
				null, 1,
				true,
				true);
	}

	/**
	 * Retrieves the import configuration by building it using the current project property.
	 *
	 * @return the import configuration, or null if the configuration could not be built
	 */
	@Nullable
	private IImportConfig getImportConfig() {
		IProject currentProject = CAFUtils.getInstance().getCurrentProject();
		assert currentProject != null;
		IImportConfigBuilder importConfigBuilder = new FileImportConfigBuilder(currentProject, sysMLErrorReporter);
		IImportConfig importConfig = importConfigBuilder.build();
		return importConfig;
	}

	private static class CancellableProgressTaskClient extends ProgressTaskClient
	{

		private final PropertyChangeSupport pcs;

		private CancellableProgressTaskClient(IProgress progress, Runnable runnable,
				IProgressCancelledHandler cancelHandler, PropertyChangeSupport pcs)
		{
			super(progress, runnable, cancelHandler);
			this.pcs = pcs;
		}

		/**
		 * @see chs.utility.task.IInterruptableTaskClient#stopTask()
		 */
		public void stopTask()
		{
			pcs.firePropertyChange(new PropertyChangeEvent(this, "sad", false, true));
			super.stopTask();
		}
	}

	@NotNull
	private String getResourceString(String resourceKey, Object... args)
	{
		return ResourceMgr.getString(SysMLSoaModelImporter.class, resourceKey, args);
	}
}

