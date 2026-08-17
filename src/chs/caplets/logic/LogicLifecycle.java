/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2002-2026 Siemens
 */
package chs.caplets.logic;

import chs.caf.CAFUtils;
import chs.caf.ProjectChangeEvent;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caplets.shared.BaseLifecycle;
import chs.caplets.shared.WindowCloseStatus;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.cof.project.folder.IFolderMgrListener;
import chs.common.IFolderMgrRefreshEvent;
import chs.common.RefreshStatusEnum;
import chs.utility.logic.IAssistivePlacementProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * This class is the lifecycle for the Logic Caplet.
 */
public class LogicLifecycle extends BaseSystemLogicLifecycle implements IAssistivePlacementProvider, IFolderMgrListener
{

	@Nullable private Icon m_Icon;

	public LogicLifecycle(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * @see BaseLifecycle#createController(ICaplet, ILogicDesign, ISchemDiagram)
	 */
	protected ICapletController createController(ICaplet caplet, ILogicDesign design, ISchemDiagram diagram)
	{
		return new LogicController(caplet, design, diagram);
	}

	/**
	 * @see BaseLifecycle#getResourceClass()
	 */
	protected Class<? extends BaseLifecycle> getResourceClass()
	{
		return LogicLifecycle.class;
	}

	/**
	 * @see BaseLifecycle#createUnloadDesignTask(IProject, ILogicDesign, boolean, WindowCloseStatus)
	 */
	@Override @NotNull
	protected WipeOutDesignOnSaveCompleteTask createUnloadDesignTask(@NotNull IProject project,
			@NotNull ILogicDesign design, boolean unlockDesign, @NotNull WindowCloseStatus windowCloseStatus)
	{
		return new LogicWipeOutDesignOnSaveCompleteTask(project, design, unlockDesign, windowCloseStatus);
	}

	@Nullable public Icon getDiagramTabIcon()
	{
		return m_Icon;
	}

	@Override public void setDiagramTabIcon(@Nullable Icon icon)
	{
		m_Icon = icon;
	}

	@Override public void folderMgrRefreshed(IFolderMgrRefreshEvent refreshEvent)
	{
		if (refreshEvent.refreshStatus() != RefreshStatusEnum.eRefreshNotNeeded) {
			IProject project = refreshEvent.getObjectMgr().getProject();
			CAFUtils.getInstance().getCAFProjectMgr().projectChanged(project);
		}
	}

	@Override public void projectChanged(ProjectChangeEvent e)
	{
		super.projectChanged(e);

		if (e.getChangeType() == ProjectChangeEvent.PROJECT_OPENED) {
			e.getProject().getFolderMgr().addFolderMgrListener(this);
		}
	}
}