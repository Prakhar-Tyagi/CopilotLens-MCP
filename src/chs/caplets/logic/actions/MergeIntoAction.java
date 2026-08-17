/*
 * Copyright 2010-2012 Mentor Graphics Corporation
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
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.actions.actionreport.ActionChangeReportMgr;
import chs.caplets.logic.actions.actionreport.IMergeActionChange;
import chs.caplets.logic.actions.actionreport.IMergeActionChangeReporter;
import chs.caplets.logic.actions.actionreport.IMergeComparison;
import chs.caplets.logic.actions.shared.ShareActionUI;
import chs.caplets.logic.actions.ui.IFacetConflictResolutionModel;
import chs.caplets.logic.actions.ui.MergeIntoDialog;
import chs.caplets.logic.merge.Merger;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.common.IAttributePropertyProvider;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;

import java.awt.event.ActionEvent;

/**
 * Created by IntelliJ IDEA. User: melmorsy Date: 10-Mar-2010 Time: 13:50:15
 */
public class MergeIntoAction extends ControllerActionRT implements ICtxMenuProvider
{

	protected ILogicObject m_sourceObject = null;
	protected ILogicObject m_targetObject = null;
	protected IFacetConflictResolutionModel m_conflictResolution = null;

	public MergeIntoAction(ICapletController controller)
	{
		super(controller);
	}

	protected IActionEnum onActivate(ActionEvent e)
	{

		m_sourceObject = MergeIntoActionHelper.getOperand(getController().getSelectMgr().getPreSelections());
		if (m_sourceObject == null) {
			return IActionEnum.eCanceled;
		}

		MergeIntoDialog dialog = getMergeintoDialog();

		dialog.setVisible(true);

		if (dialog.isCancelled()) {
			return IActionEnum.eCanceled;
		}

		m_targetObject = dialog.getSelectedLogicObject();

		m_conflictResolution = dialog.getConflictResolution();
		return IActionEnum.eCompleted;
	}

	protected MergeIntoDialog getMergeintoDialog()
	{
		String dialogTitle =
				ResourceMgr.getString(MergeIntoDialog.class, "MergeIntoDialog.title.text", m_sourceObject.getName());
		return new MergeIntoDialog(CAFUtils.getInstance().getDialogFrame(), dialogTitle, m_sourceObject);
	}

	protected boolean onTerminate(boolean successful)
	{
		if (successful) {
			IMergeActionChangeReporter reporter = ActionChangeReportMgr.getInstance().createMergeActionChangeReporter();
			IMergeComparison<IMergeActionChange, IAttributePropertyProvider> comparison = reporter.createComparison();
			comparison.setInitialStateOfSourceObject(m_sourceObject);
			comparison.setInitialStateOfTargetObject(m_targetObject);
			IMergeComparison<IMergeActionChange, IAttributePropertyProvider> matecomparator = null;
			if (m_sourceObject instanceof IGenericInlineConnector) {
				matecomparator = reporter.createComparison();
				IGenericInlineConnector sourceInlineConnector =
						CommonUtils.cast(m_sourceObject, IGenericInlineConnector.class);
				IGenericInlineConnector targetInLineConnector =
						CommonUtils.cast(m_targetObject, IGenericInlineConnector.class);
				if (sourceInlineConnector != null && targetInLineConnector != null &&
						!sourceInlineConnector.getMatedInlines().isEmpty() && !targetInLineConnector.getMatedInlines().isEmpty()) {
					matecomparator.setInitialStateOfSourceObject(sourceInlineConnector.getMatedInlines().iterator().next());
					matecomparator.setInitialStateOfTargetObject(targetInLineConnector.getMatedInlines().iterator().next());
				}
			}
			Merger merger = Merger.getMerger(m_sourceObject, m_targetObject, comparison::addChange);
			merger.setupConflictResolution(m_conflictResolution);
			merger.merge();

			comparison.setTransformedState(m_targetObject);
			if (matecomparator != null) {
				IGenericInlineConnector mergedInLineConnector =
						CommonUtils.cast(m_targetObject, IGenericInlineConnector.class);
				if (mergedInLineConnector != null && !mergedInLineConnector.getMatedInlines().isEmpty()) {
					matecomparator.setTransformedState(mergedInLineConnector.getMatedInlines().iterator().next());
				}
			}
			reporter.reportChanges();
		}

		return true;
	}

	public boolean isEnabled()
	{
		if (ActionRT.isDesignUnderConcurrentEdit()) {
			setDisabledReason(ResourceMgr.getString(ActionRT.class, "ActionRT.LogicMUMode"));
			return false;
		}
		ILogicObject sourceObject = MergeIntoActionHelper.getOperand(getController().getSelectMgr().getPreSelections());
		if (sourceObject == null) {
			return false;
		}
		if (!sourceObject.isMergeable()) {
			return false;
		}
		return sourceObject.getDesignContainer().isEditable() && super.isEnabled();
	}

	public String getActionUIClass()
	{
		return MergeIntoActionUI.class.getName();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{

		ILogicObject sourceObject = MergeIntoActionHelper.getOperand(selections);

		if (MergeIntoActionHelper.isMergeable(sourceObject)) {
			container.addBefore(new ActionEntry(getActionUI()), ShareActionUI.class);
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{

	}
}
