/*
 * Copyright 2007-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.IUpdateableAction;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.cof.logical.IAbstractMulticore;
import chs.cof.logical.shared.IFreezable;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedInline;
import chs.cof.logical.shared.ISharedMessageSignal;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedOverbraid;
import chs.cof.logical.shared.ISharedPinListMgr;
import chs.cof.logical.shared.SharedObjectMgr;
import chs.cof.project.IProject;
import chs.cof.security.FunctionalPermissionEnum;
import chs.cof.security.FunctionalPermissionMgr;
import chs.common.IUIDObject;
import chs.ctf.caf.ui.FreezeUnfreezeSharedObjectCmd;
import chs.ctf.caf.ui.SharedInlineWrapper;
import chs.utilities.permission.PermissionHelper;
import chs.utilities.ui.MessageHelper;
import chs.utility.helpers.SingleLineHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashSet;

/**
 * Created by jamesmw User: jamesmw Date: 11-Jul-2007 Time: 14:51:44
 */
public class FreezeUnfreezeSharedObjectAction extends ControllerActionRT
{

	private ISpecialSelectMgr specialSelectMgr = null;
	private FreezeUnfreezeSharedObjectCmd cmd;
	private IProject project = null;

	public FreezeUnfreezeSharedObjectAction(ICapletController controller, ISpecialSelectMgr libSelectMgr)
	{
		super(controller);
		setUndoableAction(false);
		specialSelectMgr = libSelectMgr;
		if (getActionUI() != null) {
			specialSelectMgr.contextMenuAddAction(
					new ActionEntry(getActionUI(), (String) getActionUI().getValue(Action.SHORT_DESCRIPTION))
					{
						public boolean shouldDisplay()
						{
							boolean shouldDisplay = (ActionRT.isDesignUnderConcurrentEdit() || isEnabled());
							if (!shouldDisplay) {
								Action ui = getActionUI();
								if (ui == null) {
									return false;
								}
								((IUpdateableAction) ui).updateUI();
							}
							return shouldDisplay;
						}

						public String getName()
						{
							return (String) getAction().getValue(Action.NAME);
						}
					}
			);
		}
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		IFreezable freezeObj = (IFreezable) getOperand();
		if (freezeObj instanceof ISharedConnector) {
			ISharedConnector sharedCon = (ISharedConnector) freezeObj;
			project = sharedCon.getProject();
			if (sharedCon.getType().isInline()) {
				ISharedConnector mate = (ISharedConnector) sharedCon.getMates().toArray()[0];
				freezeObj = new SharedInlineWrapper(sharedCon, mate);
			}
		}
		else if (freezeObj instanceof ISharedObject) {
			// Really must be one of these.
			project = ((ISharedObject) freezeObj).getProject();
		}

		cmd = new FreezeUnfreezeSharedObjectCmd(freezeObj, CAFUtils.getInstance().getWindowMgr().getDialogFrame());
		if (!cmd.doExecuteAllowed()) {
			MessageHelper.showPrivilegesMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
					PermissionHelper.getInternationalisedName(FunctionalPermissionMgr.permissionEnumToString(
							FunctionalPermissionEnum.UnfreezeSharedObjects)));
			return IActionEnum.eCanceled;
		}
		return IActionEnum.eCompleted;
	}

	protected boolean onTerminate(boolean successful)
	{
		boolean success = successful;
		if (success) {
			success = cmd.execute();
			ISharedPinListMgr splmgr = project.getSharedPinListMgr();
			ISharedConductorMgr conMgr = project.getSharedConductorMgr();
			SharedObjectMgr.fireChangeEventForManagersWithEventDetail(getSharedObjects(), splmgr, conMgr);
		}
		project = null;
		cmd = null;
		return success;
	}

	@NotNull private Collection<ISharedObject> getSharedObjects()
	{
		IFreezable freezable = cmd.getFreezableObject();
		Collection<ISharedObject> sharedObjects = new HashSet<>();
		if (freezable instanceof ISharedObject) {
			sharedObjects.add((ISharedObject) freezable);
		}
		else if (freezable instanceof ISharedInline) {
			ISharedInline sharedInline = (ISharedInline) freezable;
			sharedObjects.add(sharedInline.getJack());
			sharedObjects.add(sharedInline.getPlug());
		}
		return sharedObjects;
	}

	protected boolean shouldDisableUnderConcurrentEdit()
	{
		return true;
	}

	public boolean isEnabled()
	{
		ISharedObject sharedObj = (ISharedObject) getOperand();
		if (sharedObj == null) {
			return false;
		}

		if (sharedObj instanceof ISharedMessageSignal) {
			return false;
		}

		if (sharedObj instanceof ISharedConductor) {
			ISharedConductor sharedCond = (ISharedConductor) sharedObj;
			if (sharedCond.getMulticore() != null && !(sharedCond.getMulticore() instanceof ISharedOverbraid)) {
				return false;
			}
		}

		if (sharedObj instanceof ISharedMulticore && !(sharedObj instanceof ISharedOverbraid)) {
			if (((IAbstractMulticore) sharedObj).getParent() != null) {
				return false;
			}
		}

		if (sharedObj instanceof ISharedMulticore sharedMulticore &&
				SingleLineHelper.isMulticorePartOfAnySingleLine(sharedMulticore)) {
			return false;
		}

		if (sharedObj instanceof ISharedConnector) {
			if (((ISharedConnector) sharedObj).getOccupiedPosition() != null) {
				return false;
			}
		}

		if (sharedObj.isFrozen()) {
			if (!FreezeUnfreezeSharedObjectCmd.userHasUnfreezePermission()) {
				return false;
			}
		}
		return super.isEnabled();
	}

	public String getActionUIClass()
	{
		return FreezeUnfreezeSharedObjectActionUI.class.getName();
	}

	public IUIDObject getOperand()
	{
		IUIDObject uidObject = null;
		if (specialSelectMgr.getSelectedObjects().getSize() == 1) {
			uidObject = specialSelectMgr.getSelectedObjects().getNext();
			if (!(uidObject instanceof IFreezable)) {
				uidObject = null;
			}
		}
		return uidObject;
	}
}
