/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.CAFCommandListener;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.IUndoableContainer;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.helpers.IHarnessEditingAction;
import chs.caplets.logic.harness.propagate.AutoPropagateHarnessController;
import chs.caplets.logic.harness.propagate.HarnessPropagateTableUtils;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.IPropagateHarnessCmd;
import chs.cof.logical.IPropagationInfo;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.DesignUtils;
import chs.common.IUID;
import chs.system.FactoryMgr;
import chs.system.ILogicUtilsFactory;
import chs.utilities.AppInfo;
import chs.utilities.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;

/**
 * Action to update harness for all connected objects
 */
public abstract class PropagateHarnessAction extends ControllerActionRT implements IHarnessEditingAction
{

	protected PropagateHarnessAction(@NotNull ICapletController controller)
	{
		super(controller);
	}

	@NotNull @Override protected IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eCompleted;
	}

	@Override public boolean isEnabled()
	{
		return super.isEnabled() && AutoPropagateHarnessController.isAutoPropagateEnabled();
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		IPropagationInfo propagationInfo = getPropagationInfo();
		if (propagationInfo == null) {
			return false;
		}

		IUID designUid = propagationInfo.getDesignUid();
		ILogicDesign design = DesignUtils.getDesign(designUid, ILogicDesign.class);

		if (design == null || !AppInfo.isLogic() || isDesignClosed(designUid)) {
			reportError(designUid);
			return false;
		}

		// If there is nothing to propagate
		if (propagationInfo.getLogicObjects().isEmpty() && propagationInfo.getSharedObjects().isEmpty()) {
			return false;
		}

		if (!canPerformAction(design)) {
			reportError(designUid);
			return false;
		}

		ILogicUtilsFactory logicUtilsFactory = FactoryMgr.getLogicalFactory().getLogicUtilsFactory();

		if (logicUtilsFactory != null) {
			IPropagateHarnessCmd cmd = getPropagateHarnessCmd(design, propagationInfo, logicUtilsFactory);
			if (cmd.prepare()) {
				CAFCommandListener.executeCommandWithProgressDlg(cmd, PropagateHarnessAction.class, cmd.getProgress());
				IUndoableContainer undoableContainer = getController().getUndoableContainer();
				if (!propagationInfo.getSharedObjects().isEmpty() && undoableContainer != null) {
					undoableContainer.endEdit();
					getController().clearUndoQueue();
				}
			}
			else {
				return false;
			}
		}

		return true;
	}

	protected boolean isDesignClosed(@NotNull IUID designUid)
	{
		return !CAFUtils.getInstance().hasDiagramDisplayed(designUid);
	}

	private void reportError(@Nullable IUID designUid)
	{
		HarnessPropagateTableUtils.showPropagateErrorMessage(designUid);
	}

	private boolean canPerformAction(@NotNull ILogicDesign design)
	{
		ICapletController controller = getActiveController();

		if (controller == null) {
			return false;
		}

		ISchemDiagram diagram = CommonUtils.cast(getBaseDiagram(), ISchemDiagram.class);
		ILogicDesign currentDesign = diagram != null ? diagram.getDesign() : null;

		if (currentDesign == null || currentDesign != design) {
			return false;
		}

		if (!isModelEditable(controller)) {
			return false;
		}

		return !isReadOnly(design);
	}

	@Nullable protected ICapletController getActiveController()
	{
		return CAFUtils.getInstance().getActiveCapletController();
	}

	protected boolean isModelEditable(@NotNull ICapletController controller)
	{
		ICapletModel capletModel = controller.getCapletModel();
		return capletModel != null && capletModel.isEditable();
	}

	protected boolean isReadOnly(@NotNull ILogicDesign design)
	{
		return CAFUtils.getInstance().isDesignOpenReadOnly(design);
	}

	@NotNull protected IPropagateHarnessCmd getPropagateHarnessCmd(@NotNull ILogicDesign design,
			@NotNull IPropagationInfo propagationInfo, @NotNull ILogicUtilsFactory logicUtilsFactory)
	{
		return logicUtilsFactory.createPropagateHarnessCmd(design, propagationInfo);
	}

	@Nullable protected IPropagationInfo getPropagationInfo()
	{
		return AutoPropagateHarnessController.getInstance().getPropagationInfo();
	}

	@Override public boolean isAutoPropagateAction()
	{
		return true;
	}
}
