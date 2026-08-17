/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2020-2023 Siemens
 */

package chs.caplets.logic.actions.layout.associatedesigns;

import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.cafmain.actions.CAFCommandListener;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.helpers.associatedesigns.AbstractBaseAssociateDesignsCommand;
import chs.caf.caplet.helpers.associatedesigns.IAssociateDesignsLockChecker;
import chs.caf.caplet.helpers.associatedesigns.IAssociateDesignsModel;
import chs.caf.caplet.helpers.associatedesigns.IAssociatedDesignsPresenter;
import chs.caplets.logic.actions.layout.sync.LayoutAssociateDesignSync;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILayoutDesignMgr;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.project.IProject;
import chs.cof.project.buildlist.IBuildList;
import chs.cof.project.buildlist.IBuildListMgr;
import chs.utilities.ui.messaging.Choice;
import chs.utilities.ui.messaging.Question;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LayoutAssociateDesignsCommand extends AbstractBaseAssociateDesignsCommand
{

	@NotNull private final ILayoutLogicDesign mLayoutDesign;

	public LayoutAssociateDesignsCommand(@NotNull ILayoutLogicDesign theDesign,
			@NotNull IAssociatedDesignsPresenter presenter,
			@NotNull IAssociateDesignsModel associateDesignsModel,
			@NotNull IAssociateDesignsLockChecker lockChecker)
	{
		super(presenter, associateDesignsModel, lockChecker);
		mLayoutDesign = theDesign;
		initBuildlist(getLayoutMgr().getBuildList());
	}

	@NotNull private ILayoutDesignMgr getLayoutMgr()
	{
		return mLayoutDesign.getLayoutDesignMgr();
	}

	@Nullable @Override protected IBuildList getCurrentBuildList()
	{
		return getLayoutMgr().getBuildList();
	}

	@Override protected void addLockedBuildlist(@NotNull IBuildList buildList)
	{
		// no-op buildlist lock is not required for layout design association
	}

	@Nullable @Override protected IBuildListMgr getBuildListMgr()
	{
		final IProject project = getProject();
		return project != null ? project.getBuildListMgr() : null;
	}

	@Nullable @Override protected IProject getProject()
	{
		return mLayoutDesign.getProject();
	}

	@Override protected int getNumSystemLogicDesignRefs()
	{
		return getLayoutMgr().getNumSystemLogicDesignRefs();
	}

	@Override protected boolean refForSystemLogicDesignExists(@NotNull IDesign design)
	{
		return getLayoutMgr().refForSystemLogicDesignExists(design.getUID());
	}

	@Override protected void runAssociateDesignsSync(@Nullable IBuildList buildList)
	{
		ICapletView activeCapletView = CAFUtils.getInstance().getActiveCapletView();
		try {
			if (activeCapletView != null) {
				activeCapletView.lock();
			}
			LayoutAssociateDesignSync sync = new LayoutAssociateDesignSync(mLayoutDesign, new CAFCommandHelper(),
					associateDesignsModel.getAssociatedDesigns(), associateDesignsModel.getAssociatedBuildList());
			CAFCommandListener.executeCommandWithProgressDlg(sync, LayoutAssociateDesignSync.class, null, 0);
		}
		finally {
			if (activeCapletView != null) {
				activeCapletView.unlock();
			}
		}
	}

	@Override protected boolean shouldProceedWithApplyEdits()
	{
		return true;
	}

	@Override protected boolean checkForSyncRequired()
	{
		boolean syncRequired = super.checkForSyncRequired();
		if (!syncRequired) {
			return false;
		}
		final ILayoutDesignMgr layoutDesignMgr = mLayoutDesign.getLayoutDesignMgr();
		if (layoutDesignMgr.getBuildList() == null && layoutDesignMgr.getNumSystemLogicDesignRefs() == 0) {
			//If nothing is associated as of now, no need to ask for confirmation again. User must be trying to associate designs for first time.
			return true;
		}
		return showDialogToConfirmProceed();
	}

	private boolean showDialogToConfirmProceed()
	{
		ResourceBasedMessageContent messageContent =
				new ResourceBasedMessageContent(LayoutAssociateDesignSync.class,
						"LayoutAssociateDesignSync.confirmToProceedForSync");
		Choice chProceed = new Choice(LayoutAssociateDesignSync.class,
				"AbstractLayoutDesignSync.confirmToProceedForSync.choice.proceed");
		Choice chCancel = new Choice(LayoutAssociateDesignSync.class,
				"AbstractLayoutDesignSync.confirmToProceedForSync.choice.cancel");
		Choice response = Question.show(messageContent, chProceed, chCancel);
		return response != chCancel;
	}
}
