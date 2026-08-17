/*
 * Copyright 2017 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.debug;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.ICtxMenuProvider;
import chs.caf.IUpdateableAction;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.Model;
import chs.cof.icd.IDesignICDContainer;
import chs.cof.logical.ILogicDesign;
import chs.utility.ICDUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.awt.event.ActionEvent;

/**
 * @author pbhawsar on 17-02-2017
 */
public class VariantICDTogglePinConstraintAction extends ControllerActionRT implements ICtxMenuProvider
{

	public VariantICDTogglePinConstraintAction(@NotNull ICapletController controller)
	{
		super(controller);
	}

	@Override protected IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eCompleted;
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		if (ICDUtils.shouldVariantICDPinMatch()) {
			ICDUtils.setShouldVariantICDPinMatch(false);
		}
		else {
			ICDUtils.setShouldVariantICDPinMatch(true);
		}

		final ICapletController controller = getController();
		final Model model = (Model) controller.getCapletModel();
		ILogicDesign logicDesign = model.getDesign();
		IDesignICDContainer designICDContainer = logicDesign.getDesignICDContainer();
		designICDContainer.clearCachedICDs();
		return true;
	}

	@Override public String getActionUIClass()
	{
		return VariantICDTogglePinConstraintActionUI.class.getName();
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		Action actionUI = getActionUI();
		if (actionUI != null) {
			((IUpdateableAction) actionUI).updateUI();
			String shortDesc = (String) actionUI.getValue(Action.SHORT_DESCRIPTION);
			container.add(new ActionEntry(actionUI, shortDesc));
		}
	}

	@Override public void populateActiveCtxMenu(ActionContainer container)
	{

	}
}
