/*
 * Copyright 2005-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caplets.logic.Model;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.SharedObjectMgr;
import chs.cof.project.IProject;
import chs.common.IObjectFilter;
import chs.ctf.caf.ui.FreezeSharedObjectsDialog;
import chs.ctf.caf.ui.FreezeUnfreezeSharedObjectCmd;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.logic.DesignHelper;
import chs.utility.logic.ILogicModel;
import chs.utility.logic.LogicUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.LinkedHashSet;

public class FreezeSharedObjectsAction extends ControllerActionRT
{

	public FreezeSharedObjectsAction(ICapletController controller)
	{
		super(controller);
		// Undo/redo not supported for this action
		setUndoableAction(false);
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eCompleted;
	}

	protected boolean onTerminate(boolean successful)
	{
		IDesign design = ((ILogicModel) getController().getCapletModel()).getDesign();
		Collection<ISharedObject> sharedObjects = new LinkedHashSet<>();
		FreezeSharedObjectsDialog dialog = null;
		if (design != null) {
			boolean userHasUnfreezePermission = FreezeUnfreezeSharedObjectCmd.userHasUnfreezePermission();
			IObjectFilter<ISharedObject> filter =
					userHasUnfreezePermission ? CommonUtils.getNoFilter() : obj -> !obj.isFrozen();
			sharedObjects =
					CollectionUtils.getFilteredCollection(getEditableSharedObjects((ILogicDesign) design), filter);
			if (sharedObjects.isEmpty() && !userHasUnfreezePermission) {
				Message.show(PromptSeverity.INFORMATION, FreezeSharedObjectsAction.class,
						"FreezeSharedObjectsAction.freezeSharedObjects.noPermission");
				return successful;
			}
			dialog = new FreezeSharedObjectsDialog(
					getController().getCaplet().getFIB().getWindowMgr().getDialogFrame(), (ILogicDesign) design,
					sharedObjects);
			dialog.setVisible(true);
		}

		if (dialog != null) {
			if (dialog.isModelModified()) {
				Model model = (Model) getController().getCapletModel();
				IDesign des = model.getDesign();
				IProject project = des.getProject();
				assert project != null;
				SharedObjectMgr.fireChangeEventForManagersWithEventDetail(sharedObjects, project.getSharedConductorMgr(),
						project.getSharedPinListMgr());
			}
		}
		return successful;
	}

	@NotNull public Collection<ISharedObject> getEditableSharedObjects(@NotNull ILogicDesign design)
	{
		Collection<ISharedObject> usedSharedObjects = DesignHelper.getUsedSharedObjects(design);
		return LogicUtils.getEditableSharedObjects(usedSharedObjects.iterator());
	}

	public String getActionUIClass()
	{
		return FreezeSharedObjectsActionUI.class.getName();
	}

	protected boolean shouldDisableUnderConcurrentEdit()
	{
		return true;
	}

	public boolean isEnabled()
	{
		return getController().getCapletModel().isEditable() && super.isEnabled();
	}
}
