/*
 * Copyright 2010 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.function;

import chs.caf.cafmain.actions.TCSearchableObjects;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ILifecycleType;
import chs.caf.caplet.helpers.FileTypeHolder;
import chs.caplets.logic.LogicLifecycle;
import chs.caplets.shared.BaseLifecycle;
import chs.caplets.shared.CreateFilteredDiagramDelegate;
import chs.caplets.shared.CreateNewDelegate;
import chs.caplets.shared.CreateNewDesignDelegate;
import chs.caplets.shared.CreateNewDiagramDelegate;
import chs.caplets.shared.OpenDiagramDelegate;
import chs.cof.logical.IFunctionLogicDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.IFunctionLogicDiagram;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.cof.project.folder.IDesignFolder;
import chs.cof.security.FunctionalPermissionEnum;
import chs.common.IDesignContainer;
import chs.common.IProjectPreferenceMgr;
import chs.ctf.caf.ui.DesignEditDialog;
import chs.ctf.caf.ui.NoReleaseLevelsException;
import chs.ctf.caf.ui.TCFunctionDesignEditDialog;
import chs.ctf.caf.utils.IReleaseLevelController;
import chs.utilities.AppInfo;
import chs.utilities.IXMLTags;
import chs.utilities.suite.DesignType;
import chs.utility.ui.IconUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.awt.Frame;
import java.util.List;

public class FunctionLifeCycle extends BaseLifecycle
{

	@Nullable private Icon m_Icon;

	public FunctionLifeCycle(ICaplet caplet)
	{
		super(caplet);
		addTypesForGeneralLifecycleActivities();

		// View Related Blocks Types
		ILifecycleType viewRelatedBlocksType = getLifecycleType(IDesignFolder.class, "Lifecycle.ViewRelatedBlocks.");
		addTypeForViewRelatedBlocks(viewRelatedBlocksType);
		viewRelatedBlocksType = getLifecycleType(IFunctionLogicDesign.class, "Lifecycle.ViewRelatedBlocks.");
		addTypeForViewRelatedBlocks(viewRelatedBlocksType);
		addTypesForFilter(getLifecycleType(IFunctionLogicDesign.class, "Lifecycle.FilteredDiagram."));

		//Publish design to TC types
		ILifecycleType publishDesignType =
				getLifecycleType(IFunctionLogicDesign.class, "Lifecycle.PublishDesign.");
		addTypeForPublish(publishDesignType);
		new TCSearchableObjects().getLifecycleTypesHolderForSearch().forEach(this::addTypeForSearch);
		ILifecycleType extpublishDesignType =
				getLifecycleType(IFunctionLogicDesign.class, "Lifecycle.ExtendedPublishDesign.");
		addTypeForExtendedPublish(extpublishDesignType);
	}

	protected Class<? extends ISchemDiagram> getLogicDiagramClass()
	{
		return IFunctionLogicDiagram.class;
	}

	protected Class<? extends ILogicDesign> getLogicDesignClass()
	{
		return IFunctionLogicDesign.class;
	}

	@Override protected ICapletController createController(ICaplet caplet, ILogicDesign design, ISchemDiagram diagram)
	{
		return new FunctionController(caplet, design, diagram);
	}

	@Override protected Class<? extends BaseLifecycle> getResourceClass()
	{
		//for function caplet the resource entries are not duplicated to a different package.
		return LogicLifecycle.class;
	}

	@Nullable public ICapletModel getModel(ISchemDiagram diagram)
	{
		return getModel(diagram.getDesign());
	}

	protected FileTypeHolder createFileTypeHolder()
	{
		return new FileTypeHolder(AppInfo.getFullApplicationName(AppInfo.App.CONCORDFUNCTIONS) + " - XML format",
				"xml", "application/x-ArtisanFunctions-xml");
	}

	protected Icon getDesignIcon()
	{
		return IconUtils.getFunctionDesignIcon();
	}

	protected Icon getDiagramIcon()
	{
		return IconUtils.getFunctionDiagramIcon();
	}

	@Nullable public Icon getDiagramTabIcon()
	{
		return m_Icon;
	}

	@Override public void setDiagramTabIcon(@Nullable Icon icon)
	{
		m_Icon = icon;
	}

	@NotNull protected FunctionalPermissionEnum getEditDesignPermission()
	{
		return FunctionalPermissionEnum.EditFunctionalDesigns;
	}

	protected int getDrawGridSpacing(IProjectPreferenceMgr preferenceMgr)
	{
		return preferenceMgr.getFunctionDrawGridSpacing();
	}

	@Override protected DesignEditDialog createEditDialog(Frame frame, String title, IProject project,
			IDesignContainer srcDesign, boolean isDesignChanged, boolean isCopy, boolean isRevision, boolean readonly,
			IReleaseLevelController teamCenterReleaseLevelController)
			throws NoReleaseLevelsException
	{
		return new TCFunctionDesignEditDialog(frame, title, project, srcDesign, true,
				isDesignChanged, false, isCopy, isRevision, NOT_EVALUATION, NOT_NEW, readonly,
				teamCenterReleaseLevelController,
				(() -> getAdditionalDesignUIUserAttrsAndPropsContext()));
	}

	@NotNull @Override protected OpenDiagramDelegate getOpenDiagramDelegate(IProjectPreferenceMgr preferences)
	{
		return new OpenDiagramDelegate(this, m_caplet, getResourceClass(), IXMLTags.FUNCTIONDESIGN,
				preferences.getFunctionDesignUpdateXrefOnReadOnly(), preferences.getFunctionDrawGridSpacing());
	}

	@NotNull @Override protected CreateNewDelegate getCreateNewDiagramDelegate(IProjectPreferenceMgr preferences,
			List<?> context)
	{
		return new CreateNewDiagramDelegate(this, getResourceClass(), m_caplet, IXMLTags.FUNCTIONDESIGN,
				preferences.getFunctionDesignUpdateXrefOnReadOnly(), preferences.getFunctionDrawGridSpacing());
	}

	@NotNull @Override protected CreateNewDelegate getCreateNewDesignDelegate(IProjectPreferenceMgr preferences)
	{
		return new CreateNewDesignDelegate(this, getResourceClass(), m_caplet, IXMLTags.FUNCTIONDESIGN,
				preferences.getFunctionDesignUpdateXrefOnReadOnly(), preferences.getFunctionDrawGridSpacing());
	}

	@NotNull @Override protected CreateNewDelegate getCreateFilteredDiagramDelegate(IProjectPreferenceMgr preferences)
	{
		return new CreateFilteredDiagramDelegate(this, getResourceClass(), m_caplet, IXMLTags.FUNCTIONDESIGN,
				preferences.getFunctionDesignUpdateXrefOnReadOnly(), preferences.getFunctionDrawGridSpacing());
	}

	@Override @NotNull protected DesignType getDesignType()
	{
		return DesignType.FUNCTIONS;
	}
}
