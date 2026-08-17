/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023-2024 Siemens
 */

package chs.caplets.logic.actions.shared;

import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.CreateSingleLineAction;
import chs.caplets.logic.actions.LogicActionMessageHelper;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ISingleLine;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.shared.ISharedSingleLine;
import chs.cofUtils.cmd.CreateSharedSingleLineCmd;
import chs.cofUtils.logical.concurrency.ShareConcurrencyHelper;
import chs.common.IUIDObject;
import chs.common.RefreshStatusEnum;
import chs.services.dynamicgfx.ISmartPoint;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utility.IObjectInUseService;
import chs.utility.PortHelper;
import chs.utility.helpers.HighwayShareHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.revisioning.SharedObjectRevisionHelper;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import chs.utility.logic.SharedObjectAvailabilityChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;

/**
 * Class that represents 'Add Shared Single Line' action
 */
public class AddSharedSingleLineAction extends CreateSingleLineAction
{

	private ISpecialSelectMgr mSpecialSelectMgr;
	@Nullable private ISharedSingleLine mSharedSingleLine;
	public AddSharedSingleLineAction(ICapletController controller, ISpecialSelectMgr libSelectMgr)
	{
		super(controller);

		mSpecialSelectMgr = libSelectMgr;
		if (getActionUI() != null && libSelectMgr != null) {
			mSpecialSelectMgr.contextMenuAddAction(
					new ActionEntry(getActionUI(), (String) getActionUI().getValue(Action.SHORT_DESCRIPTION))
					{
						public boolean shouldDisplay()
						{
							return getOperand() != null;
						}
					});
		}
	}

	@NotNull protected Model getLogicModel()
	{
		return (Model) getModel();
	}

	@NotNull public IActionEnum onActivate(ActionEvent e)
	{
		mSharedSingleLine = getOperand();
		if (mSharedSingleLine == null) {
			return IActionEnum.eCanceled;
		}

		if (!LogicObjectLockFinder.tryEdit(mSharedSingleLine)) {
			return IActionEnum.eCanceled;
		}

		if (AddSharedHelper.isSharedObjectPermissionDenied()) {
			return IActionEnum.eCanceled;
		}

		if (!refresh()) {
			return IActionEnum.eCanceled;
		}

		if (!mSharedSingleLine.lockForExclusiveRead()) {
			LogicActionMessageHelper.warnLocked(mSharedSingleLine);
			return IActionEnum.eCanceled;
		}

		final ILogicDesign logicDesign = getLogicModel().getDesign();
		// Can't add an unfrozen shared object to a design that requires all shared objects to be frozen
		final ISharedObjectAvailabilityReporter reporter = new SharedObjectAvailabilityReporter();
		if (!new SharedObjectAvailabilityChecker().check(mSharedSingleLine, logicDesign, reporter, true, true)) {
			return IActionEnum.eCanceled;
		}

		// Check that another revision of this shared object does not already exist in this design
		boolean failure = SharedObjectRevisionHelper.checkUsagesOfOtherRevisionForPlaceAction(logicDesign, mSharedSingleLine);
		if (failure) {
			return IActionEnum.eCanceled;
		}

		if (!ShareConcurrencyHelper.trySharedObjectPlacement(getLogicModel().getDesign(),
				Collections.singleton(mSharedSingleLine)).contains(mSharedSingleLine)) {
			return IActionEnum.eCanceled;
		}

		IActionEnum result = super.onActivate(e);

		if(result != IActionEnum.eCanceled) {
			//create the required command
			m_cmd = new CreateSharedSingleLineCmd();
		}

		return result;
	}

	public boolean onTerminate(boolean successful)
	{
		boolean ok = successful;
		try {
			if (ok && !refresh()) {
				ok = false;
			}
			if (ok && !mSharedSingleLine.lockForExclusiveRead()) {
				LogicActionMessageHelper.warnLocked(mSharedSingleLine);
				ok = false;
			}

			ok = super.onTerminate(ok);
		}
		finally {
			mSharedSingleLine.unlock();
		}

		mSharedSingleLine = null;
		return ok;
	}

	/**
	 * Refreshed the shared cable - returns if the shared cable still exists.
	 *
	 * @return TRUE if the object exists and was refreshed  FALSE if the shared object no longer exists
	 */
	private boolean refresh()
	{
		RefreshStatusEnum rs = mSharedSingleLine.refresh();
		if (rs != RefreshStatusEnum.eRefreshNotNeeded) {
			getLogicModel().getDesign().getProject().getSharedConductorMgr().fireChangeEvent();
		}
		if (RefreshStatusEnum.eObjectDoesNotExist.equals(rs)) {
			MessageHelper.showWarningMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
					ResourceMgr.getString(AddSharedSingleLineAction.class,
							"AddSharedSingleLineAction.SharedSingleLineDeleted",
							mSharedSingleLine.getName()),
					"");
			return false;
		}
		return true;
	}

	@Nullable protected IGfxObject constructDisplayObject(List<ISmartPoint> smartPoints)
	{
		// If this isn't the first time this net has been placed in this design, use the existing connectivity.
		// design/connectivity won't be null here
		//noinspection ConstantConditions
		ISingleLine singleLine = getLogicModel().getDesign().getConnectivity().findSharedSingleLine(mSharedSingleLine);
		getCommand().setCableHighway(singleLine);

		IHighwaySchematic schemHighway = (IHighwaySchematic) super.constructDisplayObject(smartPoints);
		//why do we need it? causing issue in multi-user scenarios. trying to load all the diagrams of design.
		//HighwayShareHelper.getInstance().share(schemHighway.getConnectivity(), mSharedSingleLine);
		if (mSharedSingleLine != null && schemHighway != null) {
			if (singleLine == null) {
				HighwayShareHelper.shareHighwayInto(schemHighway.getConnectivity(), mSharedSingleLine);
			}
			PortHelper.addPortGfx(schemHighway, getLogicModel().getDiagram().getGrid().getGridSpacing());
			schemHighway.setHome(!mSharedSingleLine.isUsed(IObjectInUseService.OBJECT_IN_USE));
		}
		return schemHighway;
	}

	public boolean isEnabled()
	{
		//
		// If we are in a transaction boundary, we MUST wait
		//
		return !FactoryMgr.getSystemFactory().getCAFUtils().isWithinTransactionBoundary() && super.isEnabled() &&
				getOperand() != null;
	}

	@NotNull public String getActionUIClass()
	{
		return AddSharedSingleLineActionUI.class.getName();
	}

	@Nullable
	protected ISharedSingleLine getOperand()
	{
		if (mSpecialSelectMgr.getSelectedObjects().getSize() == 1) {
			IUIDObject uidObj = mSpecialSelectMgr.getSelectedObjects().getNext();
			if (uidObj instanceof ISharedSingleLine) {
				return (ISharedSingleLine) uidObj;
			}
		}
		return null;
	}
}
