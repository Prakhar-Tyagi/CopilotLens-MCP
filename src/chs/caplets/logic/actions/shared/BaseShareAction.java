/*
 * Copyright 2006-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.ActionContainer;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.IDataTransfer;
import chs.caf.caplet.action.IActionEnum;
import chs.utility.helpers.ConfirmChoiceDialog;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.helpers.SharedConfirmDialogHandler;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caplets.logic.Model;
import chs.cof.logical.ILogicDesign;
import chs.common.INamedUIDObject;
import chs.common.IUIDObject;
import chs.utilities.Pair;
import chs.utility.logic.LogicUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class BaseShareAction extends ControllerActionRT implements ICtxMenuProvider
{

	protected BaseShareActionOperands m_operands;
	protected IShareActionHelper m_helper;

	/**
	 * A handle to our dynamic graphics service for convenience.
	 */
	protected IShareActionHelper m_pinListHelper;
	protected IShareActionHelper m_condGroupHelper;
	protected IShareActionHelper m_conductorHelper;
	protected IShareActionHelper m_FunctionConductorHelper;
	protected IShareActionHelper m_highwayHelper;
	protected IShareActionHelper m_singleLineHelper;
	protected IShareActionHelper m_functionMessageHelper;
	protected String m_newlySharedObjName;
	protected String m_newlySharedObjUid;
	protected String m_logicObjName;

	protected ILogicDesign m_design;

	protected BaseShareAction(ICapletController controller)
	{
		super(controller);
		Model model = (Model) controller.getCapletModel();
		m_design = model.getDesign();
	}

	/**
	 * Actives this action if a selection set is eligable for either sharing or unsharing.
	 *
	 * @param e action event
	 *
	 * @return IActionEnum
	 */
	protected IActionEnum onActivate(ActionEvent e)
	{
		final BaseShareActionOperands operands = getOperands(getController().getSelectMgr().getCurrentSelections());
		if (operands == null) {
			return IActionEnum.eCanceled;
		}
		m_operands = operands;
		final Pair<INamedUIDObject, IShareActionHelper> actionHelperPair =
				BaseShareActionHelper
						.determineActionHelper(m_operands, m_pinListHelper, m_condGroupHelper, m_conductorHelper,
								m_highwayHelper, m_singleLineHelper, m_functionMessageHelper, m_FunctionConductorHelper);

		if (actionHelperPair == null) {
			return IActionEnum.eCanceled;
		}

		final IShareActionHelper helper = actionHelperPair.getSecond();
		if (helper == null) {
			return IActionEnum.eCanceled;
		}
		m_helper = helper;
		m_logicObjName = actionHelperPair.getFirst().getName();
		m_newlySharedObjName = m_logicObjName;
		m_newlySharedObjUid = actionHelperPair.getFirst().getUID().getString();
		return IActionEnum.eCompleted;
	}

	protected boolean onTerminate(boolean successful)
	{
		boolean editSuccessful = false;
		if (successful && canProceed()) {
			editSuccessful = m_helper.doEdit();
			if (editSuccessful) {
				// Clear the undo stack to avoid problems with trying to undo sharing objects
				getController().getUndoableContainer().endEdit();
				getController().getUndoableContainer().clear();
			}
		}

		if (m_operands != null) {
			// Clean up
			m_operands.target = null;
			m_operands.mate = null;
		}

		//Clear the Paste buffer after every Undo action
		IDataTransfer dt = CAFUtils.getInstance().getActiveDataTransfer();
		if (dt != null) {
			dt.clearPasteBuffer();
		}

		return editSuccessful;
	}

	@Override public boolean isPostTerminateValidationRequired()
	{
		// Since we are Clearing the undo stack in onTerminate. we are missing the post-terminate validation.
		//we do need to have post-terminate validation.
		return true;
	}

	public boolean isEnabled()
	{
		return getController().getCapletModel().isEditable() && super.isEnabled();
	}

	public String getActionUIClass()
	{
		return BaseShareActionUI.class.getName();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	/**
	 * Performs various tests to see if the objects in the selection set can be either shared or unshared. The
	 * overriding functions in (Un)ShareAction decide if the set is one way or the other.  This function will only
	 * return a set that is guarrentteed to be one or the other.
	 *
	 * @param selections SelectSet of selection.
	 *
	 * @return BaseShareActionOperands containing actionable UIDObject and it's mate pinlist if applicable.
	 */
	@Nullable public static BaseShareActionOperands getOperands(SelectSet selections)
	{
		final List<IUIDObject> uidObjects = getSelectedUIDObjectList(selections);
		final BaseShareActionOperands operands = BaseShareActionHelper.getOperands(uidObjects);
		return BaseShareActionOperandStrategy.getInstance().isShareable(operands.getShareabilityStatus()) ? operands : null;
	}

	@NotNull protected static List<IUIDObject> getSelectedUIDObjectList(@NotNull SelectSet selections)
	{
		final List<IUIDObject> uidObjects = new ArrayList<>();
		for (SelectedUIDObjectIterator iter = selections.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject uidObj = iter.getNext();
			uidObjects.add(uidObj);
		}
		return uidObjects;
	}

	protected boolean canProceed()
	{
		if(!canShowSharedWarningDialog()){
			return true;
		}
		if (!(m_helper instanceof SharePinListActionHelper || m_helper instanceof ShareConductorActionHelper ||
				m_helper instanceof ShareSingleLineActionHelper || m_helper instanceof ShareHighwayActionHelper ||
				m_helper instanceof ShareConductorGroupActionHelper)) {
			return true;
		}

		int dialogType = m_helper.isShareInto() ? SharedConfirmDialogHandler.SHARE_INTO : SharedConfirmDialogHandler.SHARE;
		return getUserResponse(dialogType);
	}

	protected boolean canShowSharedWarningDialog()
	{
		return LogicUtils.canShowSharedWarningDialog(getBaseDiagram());
	}

	protected boolean getUserResponse(int dialogType)
	{
		SharedConfirmDialogHandler dialogHandler = new SharedConfirmDialogHandler(dialogType);
		ConfirmChoiceDialog dialog = dialogHandler.getSharedConfirmDialog();

		return !dialog.userCancelled();
	}
}
