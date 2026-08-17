/*
 * Copyright (c) 2018. Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 *  SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.shared;

import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.DesignCapletLifecycleHelper;
import chs.caplets.logic.actions.AutoviewDialog;
import chs.caplets.logic.actions.IPartitionDiagramContext;
import chs.caplets.logic.actions.PartitionDiagramContext;
import chs.caplets.logic.actions.PartitionDiagramDialog;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.cof.project.folder.IFolder;
import chs.common.DiagramGenerationException;
import chs.ctf.caf.ui.NoReleaseLevelsException;
import chs.utilities.LifecycleUtils;
import chs.utilities.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CreateNewPartitionLogicDiagramDelegate extends CreateNewLogicDiagramDelegate
{

	private IPartitionDiagramContext m_diagramContext = new PartitionDiagramContext();
	@NotNull private final Class<? extends ISchemDiagram> m_logicDiagramClass;

	public CreateNewPartitionLogicDiagramDelegate(
			@NotNull ILifeCycleChangeListener lifeCycleListener,
			Class<? extends DesignCapletLifecycleHelper> resourceClass,
			@NotNull ICaplet caplet, String designXMLTag,
			boolean updateXrefOnReadOnly, int drawGridSpacing,
			@NotNull Class<? extends ISchemDiagram> logicDiagramClass)
	{
		super(lifeCycleListener, resourceClass, caplet, designXMLTag, updateXrefOnReadOnly, drawGridSpacing);
		m_logicDiagramClass = logicDiagramClass;
	}

	@Override public Pair<Boolean, IBaseDiagram> createNew(List<?> context)
	{
		m_diagramContext =
				new PartitionDiagramContext(LifecycleUtils.getContextObject(context, m_logicDiagramClass));
		List<?> contextUptoDesignNode = context.subList(0, context.size() - 1);
		return doCreateNewLogicDiagram(contextUptoDesignNode);
	}

	protected Pair<Boolean, IBaseDiagram> doCreateNewLogicDiagram(@NotNull List<?> contextUptoDesignNode)
	{
		return super.createNew(contextUptoDesignNode);
	}

	@NotNull @Override
	protected AutoviewDialog getCreateNonFilteredAutoviewDialog(IProject project,
			@Nullable List<ILogicDesign> designList,
			List<IFolder> folderList, boolean designAlreadyLocked, String title)
			throws NoReleaseLevelsException, DiagramGenerationException
	{
		List<IDesign> designList2 = null;
		if (designList != null) {
			designList2=new ArrayList<>();
			designList2.addAll(designList);
		}
		PartitionDiagramDialog dialog =
				new PartitionDiagramDialog(mMainFrame, title, project, folderList, designList2, designAlreadyLocked,
						m_diagramContext);
		return dialog;
	}

	protected IPartitionDiagramContext getPartitionDiagramContext()
	{
		return m_diagramContext;
	}
}
