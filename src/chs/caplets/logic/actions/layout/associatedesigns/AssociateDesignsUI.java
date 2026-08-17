/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout.associatedesigns;

import chs.caf.IStatusBar;
import chs.caf.caplet.helpers.associatedesigns.AbstractAssociateDesignsUI;
import chs.caf.caplet.helpers.associatedesigns.AbstractAssociatedDesignsPresenter;
import chs.caf.caplet.helpers.associatedesigns.AssociatedDesignsButtons;
import chs.caf.caplet.helpers.associatedesigns.IAssociateDesignsLockChecker;
import chs.caf.caplet.helpers.associatedesigns.IAssociateDesignsModel;
import chs.caf.caplet.helpers.associatedesigns.IAssociatedDesignsUIClient;
import chs.caf.caplet.helpers.associatedesigns.SelectDesignsPresenter;
import chs.caplets.logic.Model;
import chs.cof.logical.ILayoutDesignMgr;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.project.IProject;
import chs.cof.project.buildlist.IBuildListMgr;
import chs.common.IDesignMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AssociateDesignsUI extends AbstractAssociateDesignsUI<Model>
{

	@NotNull private ILayoutLogicDesign mLayoutDesign;

	public AssociateDesignsUI(@NotNull ILayoutLogicDesign theDesign, @NotNull IAssociateDesignsLockChecker lockCheck,
			@NotNull IAssociatedDesignsUIClient<Model> client, @NotNull Model model)
	{
		super(lockCheck, client, model);
		mLayoutDesign = theDesign;
	}

	@Nullable @Override protected IProject getProject()
	{
		return mLayoutDesign.getProject();
	}

	@Override protected AbstractAssociatedDesignsPresenter getAssociatedDesignsPresenter(
			@NotNull IAssociateDesignsModel associateDesignsModel,
			@NotNull SelectDesignsPresenter selectDesignsPresenter, @NotNull AssociatedDesignsButtons buttons)
	{
		return new AssociatedDesignsPresenter(associateDesignsModel, selectDesignsPresenter, buttons);
	}

	@Override protected IAssociateDesignsModel getAssociatedDesignsModel(@NotNull IDesignMgr designMgr,
			@NotNull IBuildListMgr buildListMgr, @NotNull IStatusBar statusBar)
	{
		ILayoutDesignMgr layoutMgr = mLayoutDesign.getLayoutDesignMgr();
		return new AssociateDesignsModel(layoutMgr, buildListMgr, designMgr, statusBar);
	}
}
