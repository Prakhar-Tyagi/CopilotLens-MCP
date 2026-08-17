/*
 * Copyright 2002-2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic;

import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.IConductorRouteAction;
import chs.caplets.logic.actions.ObjectConnectionsGetter;
import chs.caplets.shared.BaseLifecycle;
import chs.caplets.shared.CreateFilteredDiagramDelegate;
import chs.caplets.shared.CreateNewDelegate;
import chs.caplets.shared.CreateNewDesignDelegate;
import chs.caplets.shared.CreateNewDiagramDelegate;
import chs.caplets.shared.OpenDiagramDelegate;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ILayoutLogicDiagram;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.cof.security.FunctionalPermissionEnum;
import chs.common.IDesignContainer;
import chs.common.IProjectPreferenceMgr;
import chs.common.PreferenceContext;
import chs.ctf.caf.interfaces.IAdditionalDesignUIAttrsAndPropsContext;
import chs.ctf.caf.ui.DesignEditDialog;
import chs.ctf.caf.ui.LayoutDesignEditDialog;
import chs.ctf.caf.ui.NoReleaseLevelsException;
import chs.ctf.caf.utils.IReleaseLevelController;
import chs.utilities.IXMLTags;
import chs.utilities.suite.DesignType;
import chs.utility.logic.IAssistivePlacementProvider;
import chs.utility.ui.IconUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.awt.Frame;
import java.util.List;

public class LayoutLifecycle extends BaseLifecycle implements IAssistivePlacementProvider
{

	@Nullable private Icon m_Icon;

	public LayoutLifecycle(ICaplet caplet)
	{
		super(caplet);
		addTypesForGeneralLifecycleActivities();
	}

	protected int getDrawGridSpacing(IProjectPreferenceMgr preferenceMgr)
	{
		return preferenceMgr.getDrawGridSpacing(PreferenceContext.LAYOUT);
	}

	/**
	 * @see BaseLifecycle#createController(ICaplet, ILogicDesign, ISchemDiagram)
	 */
	protected ICapletController createController(ICaplet caplet, ILogicDesign design, ISchemDiagram diagram)
	{
		return new LayoutController(caplet, design, diagram);
	}

	@NotNull protected FunctionalPermissionEnum getEditDesignPermission()
	{
		return FunctionalPermissionEnum.EditLayoutDesigns;
	}

	@Override @NotNull protected DesignType getDesignType()
	{
		return DesignType.LAYOUT;
	}

	/**
	 * @see BaseLifecycle#getResourceClass()
	 */
	protected Class<? extends BaseLifecycle> getResourceClass()
	{
		return LayoutLifecycle.class;
	}

	protected Icon getDiagramIcon()
	{
		return IconUtils.getLayoutDiagramIcon();
	}

	@Nullable
	public Icon getDiagramTabIcon()
	{
		return m_Icon;
	}

	@Override public void setDiagramTabIcon(Icon icon)
	{
		m_Icon = icon;
	}

	protected Icon getDesignIcon()
	{
		return IconUtils.getLayoutDesignIcon();
	}

	protected Class<? extends ISchemDiagram> getLogicDiagramClass()
	{
		return ILayoutLogicDiagram.class;
	}

	protected Class<? extends ILogicDesign> getLogicDesignClass()
	{
		return ILayoutLogicDesign.class;
	}

	@NotNull protected String getLayoutDesignXMLTag()
	{
		return IXMLTags.LAYOUTDESIGN;
	}

	@Nullable public ICapletModel getModel(ISchemDiagram diagram)
	{
		return getModel(diagram.getDesign());
	}

	@Override protected DesignEditDialog createEditDialog(Frame frame, String title, IProject project,
			IDesignContainer srcDesign, boolean isDesignChanged, boolean isCopy, boolean isRevision, boolean readonly,
			IReleaseLevelController teamCenterReleaseLevelController)
			throws NoReleaseLevelsException
	{
		return new LayoutDesignEditDialog(frame, title, project, srcDesign, true,
				isDesignChanged, false, isCopy, isRevision, NOT_EVALUATION, NOT_NEW, readonly,
				teamCenterReleaseLevelController)
		{
			@Nullable @Override
			protected IAdditionalDesignUIAttrsAndPropsContext getAdditionalDesignUIAttrsAndPropsContext()
			{
				return getAdditionalDesignUIUserAttrsAndPropsContext();
			}
		};
	}

	@NotNull @Override protected OpenDiagramDelegate getOpenDiagramDelegate(IProjectPreferenceMgr preferences)
	{
		final PreferenceContext preferenceContext = getProjectPreferenceContext();
		return new OpenDiagramDelegate(this, m_caplet, getResourceClass(), getLayoutDesignXMLTag(),
				preferences.getUpdateXrefOnReadOnly(preferenceContext),
				preferences.getDrawGridSpacing(preferenceContext));
	}

	@NotNull @Override protected CreateNewDelegate getCreateNewDiagramDelegate(IProjectPreferenceMgr preferences,
			List<?> context)
	{
		final PreferenceContext preferenceContext = getProjectPreferenceContext();
		return new CreateNewDiagramDelegate(this, getResourceClass(), m_caplet, getLayoutDesignXMLTag(),
				preferences.getUpdateXrefOnReadOnly(preferenceContext),
				preferences.getDrawGridSpacing(preferenceContext));
	}

	@NotNull @Override protected CreateNewDelegate getCreateNewDesignDelegate(IProjectPreferenceMgr preferences)
	{
		final PreferenceContext preferenceContext = getProjectPreferenceContext();
		return new CreateNewDesignDelegate(this, getResourceClass(), m_caplet, getLayoutDesignXMLTag(),
				preferences.getUpdateXrefOnReadOnly(preferenceContext),
				preferences.getDrawGridSpacing(preferenceContext));
	}

	@NotNull @Override protected CreateNewDelegate getCreateFilteredDiagramDelegate(IProjectPreferenceMgr preferences)
	{
		final PreferenceContext preferenceContext = getProjectPreferenceContext();
		return new CreateFilteredDiagramDelegate(this, getResourceClass(), m_caplet, getLayoutDesignXMLTag(),
				preferences.getUpdateXrefOnReadOnly(preferenceContext),
				preferences.getDrawGridSpacing(preferenceContext));
	}

	public void createConnectionSchematics(IPinList schemPinlist, ISchemDiagram destDiagram)
	{
		ObjectConnectionsGetter.createConnectionSchematics(schemPinlist, destDiagram);
	}

	public IConductorRouteAction getConductorRouteAction()
	{
		return ConductorRouteAction.getInstance();
	}

	@NotNull private PreferenceContext getProjectPreferenceContext()
	{
		return PreferenceContext.LAYOUT;
	}
}