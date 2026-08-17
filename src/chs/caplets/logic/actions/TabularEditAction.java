/*
 * Copyright 2017 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.IFIB;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.helpers.TabularEditActionHelper;
import chs.caf.caplet.helpers.tabulareditor.IFilterableObjectType;
import chs.caf.caplet.helpers.tabulareditor.TabularEditor;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.helpers.tabulareditor.LogicTabularEditActionHelper;
import chs.caplets.logic.helpers.tabulareditor.LogicTabularEditor;
import chs.caplets.logic.helpers.tabulareditor.LogicTabularEditorStyleRefresher;
import chs.common.IAttributePropertyProvider;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.Set;

/**
 * @author pbhawsar on 03-05-2017
 */
public class TabularEditAction extends ControllerActionRT implements ICtxMenuProvider
{

	@NotNull private TabularEditActionHelper mTabularEditActionHelper;

	public TabularEditAction(@NotNull ICapletController controller,
			@NotNull IFilterableObjectType.ObjectClass objectClass)
	{
		super(controller);
		mTabularEditActionHelper = new LogicTabularEditActionHelper(() -> createTabularEditor(controller, objectClass));
	}

	@NotNull protected TabularEditor createTabularEditor(@NotNull ICapletController controller,
			@NotNull IFilterableObjectType.ObjectClass objectClass)
	{
		return new LogicTabularEditor(controller, objectClass);
	}

	@Override public boolean isEnabled()
	{
		if (CAFUtils.getInstance().getFIB().isTaskActive(IFIB.TASK_SAVE)) {
			return false;
		}

		if (super.isEnabled() && getCapletModel().isEditable()) {
			if (mTabularEditActionHelper.isEnabled(getPreSelections())) {
				return true;
			}
			else {
				setDisabledReason(
						ResourceMgr.getString(TabularEditAction.class, "TabularEditAction.noselection.tooltip"));
			}
		}
		return false;
	}

	@Override protected boolean shouldDisableUnderConcurrentEdit()
	{
		return true;
	}

	@Override protected IActionEnum onActivate(ActionEvent e)
	{
		final boolean success = mTabularEditActionHelper.activate();
		return success ? IActionEnum.eCompleted : IActionEnum.eCanceled;
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		addChangeListener();
		mTabularEditActionHelper.terminate();

		CAFUtils.getInstance().tickleUI(getController().getCaplet().getFIB());
		return true;
	}

	private void addChangeListener()
	{
		TabularEditor editor = mTabularEditActionHelper.getTabularEditor();
		if (editor != null) {
			Set<IAttributePropertyProvider> editedObjects = editor.getTableDataStorage().getEditedObjects();
			editor.addAppliedChangeListner(new LogicTabularEditorStyleRefresher(getController(), editedObjects));
		}
	}

	@Override public String getActionUIClass()
	{
		return TabularEditActionUI.class.getName();
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (isEnabled()) {
			final Action actionUI = getActionUI();
			if (actionUI != null) {
				container.add(new ActionEntry(actionUI, null));
			}
		}
	}

	@Override public void populateActiveCtxMenu(ActionContainer container)
	{

	}
}
