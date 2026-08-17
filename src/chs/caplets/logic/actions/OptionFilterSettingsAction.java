/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2007-2025 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.analysis.SubsystemBaseAction;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.action.IActionMgr;
import chs.caf.caplet.helpers.ViewActionRT;
import chs.caf.caplet.helpers.graphics.OptionFilterSettingsDialog;
import chs.caplets.logic.LogicFilterControl;
import chs.caplets.shared.BaseController;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;

import java.awt.event.ActionEvent;
import java.util.Objects;

public class OptionFilterSettingsAction extends ViewActionRT
{

	private OptionFilterSettingsDialog dialog;

	public OptionFilterSettingsAction(ICapletView view)
	{
		super(view);
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		// we need to check whether the active action is an Analysis action that
		// cannot be interrupted. If so we notify the user and cancel the actions invocation
		IActionMgr actionMgr = CAFUtils.getInstance().getActiveActionMgr();
		if (Objects.requireNonNull(actionMgr).getActiveAction() instanceof
				SubsystemBaseAction) {
			MessageHelper.showInformationMessage(CAFUtils.getInstance().getDialogFrame(),
					ResourceMgr.getString(OptionFilterSettingsAction.this,
							"OptionFilterSettingsAction.activeAnalysis.title"),
					ResourceMgr.getString(OptionFilterSettingsAction.this,
							"OptionFilterSettingsAction.activeAnalysis.message"));
			return IActionEnum.eCanceled;
		}
		BaseController controller = (BaseController) getController();
		LogicFilterControl filterControl = controller.getGraphicsFilterControl();
		if (filterControl == null) {
			return IActionEnum.eCanceled;
		}

		dialog = new LogicFilterSettingsDialog(filterControl);
		if (dialog.isCancelled()) {
			return IActionEnum.eCanceled;
		}
		else {
			return IActionEnum.eCompleted;
		}
	}

	protected boolean onTerminate(boolean successful)
	{
		if (successful) {
			dialog.applyChanges();

			// clear any selections made before filtering to avoid highlighting any
			// filtered objects dts0100545802 ...
			getController().getSelectMgr().getCurrentSelections().clear();
		}
		return successful;
	}

	public String getActionUIClass()
	{
		return OptionFilterSettingsActionUI.class.getName();
	}

	public boolean isEnabled()
	{
		return getController().getGraphicsFilterControl() != null && super.isEnabled();
	}

	@Override protected boolean checkCache()
	{
		return false;
	}
}

