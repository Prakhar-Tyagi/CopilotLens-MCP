/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout;

import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.cafmain.actions.CAFCommandListener;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.layout.sync.LayoutDesignReSync;
import chs.cof.logical.ILayoutDesignMgr;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.ILogicDesign;
import chs.system.FactoryMgr;
import chs.system.IProjectMemorySnapshot;
import chs.utilities.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;

/**
 * Action for layout design resynchronization with associated wiring designs
 */
public class LayoutDesignResyncAction extends ControllerActionRT
{

	@Nullable private LayoutDesignReSync mSynchronizer;

	public LayoutDesignResyncAction(@NotNull ICapletController controller)
	{
		super(controller);
	}

	@Override public boolean isEnabled()
	{
		return super.isEnabled() && hasDesignsAssociated();
	}

	private boolean hasDesignsAssociated()
	{
		final ILayoutDesignMgr layoutDesignMgr = getLayoutDesign().getLayoutDesignMgr();
		return layoutDesignMgr.getBuildList() != null || layoutDesignMgr.getNumSystemLogicDesignRefs() > 0;
	}

	@Override protected boolean checkCache()
	{
		return false;
	}

	@NotNull private ILayoutLogicDesign getLayoutDesign()
	{
		final ILogicDesign logicDesign = getModel().getDesign();
		final ILayoutLogicDesign layoutDesign = CommonUtils.cast(logicDesign, ILayoutLogicDesign.class);
		if (layoutDesign == null) {
			throw new IllegalArgumentException("Illegal design type");
		}
		return layoutDesign;
	}

	@NotNull private Model getModel()
	{
		return (Model) getCapletModel();
	}

	@NotNull @Override protected IActionEnum onActivate(ActionEvent e)
	{
		mSynchronizer = createLayoutDesignResynchronizer(getLayoutDesign(), new CAFCommandHelper());
		return IActionEnum.eCompleted;
	}

	@NotNull protected LayoutDesignReSync createLayoutDesignResynchronizer(@NotNull ILayoutLogicDesign layoutDesign,
			@NotNull CAFCommandHelper commandHelper)
	{
		return new LayoutDesignReSync(layoutDesign, commandHelper);
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		ICapletView activeCapletView = CAFUtils.getInstance().getActiveCapletView();
		try (IProjectMemorySnapshot ignored = FactoryMgr.getMemoryManager().snapshot(getCurrentProject(), true)) {
			if (activeCapletView != null) {
				activeCapletView.lock();
			}
			if (successful) {
				if (mSynchronizer != null) {
					performSynchronization(mSynchronizer);
					if (mSynchronizer.getExecuteResult()) {
						return true;
					}
				}
			}
		}
		finally {
			mSynchronizer = null;
			if (activeCapletView != null) {
				activeCapletView.unlock();
			}
		}
		return false;
	}

	@Override public boolean onPostTerminate(boolean onTerminateResult)
	{
		super.onPostTerminate(onTerminateResult);
		if (onTerminateResult) {
			getController().clearUndoQueue();
		}
		return true;
	}

	protected void performSynchronization(@NotNull LayoutDesignReSync resync)
	{
		CAFCommandListener.executeCommandWithProgressDlg(resync, LayoutDesignReSync.class, null, 0);
	}

	@NotNull @Override public String getActionUIClass()
	{
		return LayoutDesignResyncActionUI.class.getName();
	}

	@Override public boolean isPostTerminateValidationRequired()
	{
		return true;
	}
}
