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
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.helpers.associatedesigns.IAssociateDesignsCommand;
import chs.caf.caplet.helpers.associatedesigns.IAssociateDesignsLockChecker;
import chs.caf.caplet.helpers.associatedesigns.IAssociateDesignsModel;
import chs.caf.caplet.helpers.associatedesigns.IAssociatedDesignsPresenter;
import chs.caf.caplet.helpers.associatedesigns.IAssociatedDesignsUIClient;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.layout.associatedesigns.AssociateDesignsUI;
import chs.caplets.logic.actions.layout.associatedesigns.LayoutAssociateDesignsCommand;
import chs.cof.logical.ILayoutLogicDesign;
import chs.system.FactoryMgr;
import chs.system.IProjectMemorySnapshot;
import chs.utilities.AppInfo;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;

public class LayoutAssociateDesignsAction extends ControllerActionRT implements IAssociateDesignsLockChecker,
		IAssociatedDesignsUIClient<Model>
{

	private AssociateDesignsUI designsUI = null;
	@Nullable private IProjectMemorySnapshot mMemorySnapshot;

	public LayoutAssociateDesignsAction(@NotNull ICapletController controller)
	{
		super(controller);
	}

	@Override protected boolean shouldDisableUnderConcurrentEdit()
	{
		return true;
	}

	@NotNull @Override protected IActionEnum onActivate(ActionEvent e)
	{

		final Model model = getModel();
		final ILayoutLogicDesign layoutDesign = CommonUtils.cast(model.getDesign(), ILayoutLogicDesign.class);
		if (layoutDesign == null) {
			return IActionEnum.eCanceled;
		}
		designsUI = new AssociateDesignsUI(layoutDesign, this, this, model);
		mMemorySnapshot = FactoryMgr.getMemoryManager().snapshot(getCurrentProject(), true);
		return designsUI.prepare();
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		boolean bEditOk = false;
		if (successful) {
			// Do the actual changing of the model
			bEditOk = designsUI.execute();
			if (bEditOk) {
				clearSelections();
			}
		}
		if (mMemorySnapshot != null) {
			mMemorySnapshot.close();
			mMemorySnapshot = null;
		}
		CAFUtils.getInstance().tickleUI(getController().getCaplet().getFIB());
		return bEditOk;
	}

	@Override public boolean onPostTerminate(boolean onTerminateResult)
	{
		super.onPostTerminate(onTerminateResult);
		if (onTerminateResult) {
			getController().clearUndoQueue();
		}
		return true;
	}

	@Override public boolean isPostTerminateValidationRequired()
	{
		return true;
	}

	private void clearSelections()
	{
		// Clear the selection to avoid possibility of selection containing objects from
		// unassociated designs
		SelectSet selections = getController().getSelectMgr().getCurrentSelections();
		selections.clear();
	}

	@NotNull @Override public String getActionUIClass()
	{
		return LayoutAssociateDesignsActionUI.class.getName();
	}

	@Override public boolean shouldLockBuildList()
	{
		return false;
	}

	@Override public boolean shouldLockBuildListMgr()
	{
		return false;
	}

	@Override public boolean isReplaceEnabled()
	{
		return false;
	}

	@Override public boolean canChangeAssociations()
	{
		return isAssociationEditable();
	}

	@Override public boolean canUpdateAssociations()
	{
		return isAssociationEditable();
	}

	private boolean isAssociationEditable()
	{
		if (isModelEditable()) {
			return !getModel().getDesign().isUnderConcurrentEdit();
		}
		return false;
	}

	@NotNull @Override public Model getModel()
	{
		return (Model) getCapletModel();
	}

	@NotNull @Override public String getAssociatedDesignsTitle()
	{
		if (AppInfo.isCapitalDerivative()) {
			return ResourceMgr.getString(LayoutAssociateDesignsAction.class,
					"LayoutAssociateDesignsAction.AssociatedSourceDesigns.derivative.title");
		}
		return ResourceMgr.getString(LayoutAssociateDesignsAction.class,
				"LayoutAssociateDesignsAction.AssociatedSourceDesigns.title");
	}

	@NotNull @Override public String getSelectedDesignsTitle()
	{
		if (AppInfo.isCapitalDerivative()) {
			return ResourceMgr.getString(LayoutAssociateDesignsAction.class,
					"LayoutAssociateDesignsAction.SelectedSourceDesigns.derivative.title");
		}
		return ResourceMgr
				.getString(LayoutAssociateDesignsAction.class,
						"LayoutAssociateDesignsAction.SelectedSourceDesigns.title");
	}

	@NotNull @Override public IAssociateDesignsCommand createCommand(@NotNull Model model,
			@NotNull IAssociatedDesignsPresenter designsPresenter, @NotNull IAssociateDesignsModel designsModel,
			@NotNull IAssociateDesignsLockChecker lockChecker)
	{
		final ILayoutLogicDesign layoutDesign = CommonUtils.cast(model.getDesign(), ILayoutLogicDesign.class);
		if (layoutDesign == null) {
			return invalidDesignAssociationCommand();
		}
		return new LayoutAssociateDesignsCommand(layoutDesign, designsPresenter, designsModel, lockChecker);
	}

	@NotNull private IAssociateDesignsCommand invalidDesignAssociationCommand()
	{
		return new IAssociateDesignsCommand()
		{
			@Override public boolean prepare()
			{
				return false;
			}

			@Override public boolean execute()
			{
				return false;
			}
		};
	}

	@Override public boolean isBuildListEnabled()
	{
		return !AppInfo.isCapitalDerivative();
	}

	@Override public boolean isUnplaceDevicesEnabled()
	{
		return false;
	}
}
