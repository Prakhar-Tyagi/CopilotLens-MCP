/*
 * Copyright 2006-2010 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caplets.logic.actions.LogicMultipointCreateAction;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.project.IProject;
import chs.common.IUIDObject;
import chs.common.RefreshStatusEnum;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.PortHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.revisioning.SharedObjectRevisionHelper;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import chs.utility.logic.SharedObjectAvailabilityChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public class CreateConductorInstanceActionHelper
{

	private final ICapletController m_controller;
	@Nullable private IConductor m_conductor;
	private boolean m_ignoreShieldConductor = true;

	public CreateConductorInstanceActionHelper(@NotNull ICapletController controller)
	{
		m_controller = controller;
	}

	public void doNotIgnoreShieldConductor()
	{
		m_ignoreShieldConductor = false;
	}

	public void ignoreShieldConductor()
	{
		m_ignoreShieldConductor = true;
	}

	public boolean onActivate(@NotNull BiFunction<ISharedConductor, IProject, Boolean> sharedConductorRefreshHandler,
			@NotNull ILogicDesign design, @NotNull Class<? extends IConductor> conductorType)
	{
		IConductor conductor = getOperand();

		if (conductor == null || !conductorType.isAssignableFrom(conductor.getClass())) {
			return false;
		}

		if (!LogicObjectLockFinder.tryEdit(conductor)) {
			return false;
		}

		boolean status = true;
		if (conductor.isShared()) {
			ISharedConductor sharedConductor = conductor.getSharedConductor();
			status = !isSharedConductorUnusable(sharedConductor, design, sharedConductorRefreshHandler);
		}
		if (status) {
			m_conductor = conductor;
		}
		return status;
	}

	public void onTerminate()
	{
		m_conductor = null;
	}

	@Nullable public IConductor getConductor()
	{
		return m_conductor;
	}

	public boolean isActive()
	{
		return m_conductor != null;
	}

	/**
	 * @return If only a single connectivity wire is selected, return it otherwise return null
	 */
	@Nullable public IConductor getSelectedConductorObject()
	{
		IConductor cond = null;
		SelectSet selections = m_controller.getSelectMgr().getPreSelections();
		for (SelectedUIDObjectIterator it = selections.getSelectedUIDObjects(); it.hasNext(); ) {
			IUIDObject obj = it.getNext();
			// we cannot add multipresentations of shields (dts0100522132) here
			// TODO jacobt FEAT13040 : still enabled for interconnect conductors - for now
			if (m_ignoreShieldConductor && obj instanceof IShieldConductor) {
				//do nothing.
			}
			else if (obj instanceof IConductor) {
				if (cond == null) {
					cond = (IConductor) obj;
				}
				else {
					cond = null; // multiple conductors selected ==> not enabled
					break;
				}
			}
		}
		if (cond != null && LogicObjectLockFinder.isLogicObjectLockedInOtherSession(cond)) {
			return null;
		}

		return cond;
	}

	/**
	 * @return If only a single connectivity wire is selected, return it otherwise return null
	 */
	@Nullable private IConductor getOperand()
	{
		return getSelectedConductorObject();
	}

	/**
	 * Refreshed the shared wire - returns if the shared wire still exists.
	 *
	 * @param sharedConductor - Shared conductor to refresh
	 * @param project - container of shared manager
	 *
	 * @return TRUE if the object exists and was refreshed  FALSE if the shared object no longer exists
	 */
	public boolean refresh(@NotNull ISharedConductor sharedConductor, @NotNull IProject project)
	{
		RefreshStatusEnum rs = sharedConductor.refresh();
		if (RefreshStatusEnum.eObjectDoesNotExist.equals(rs)) {
			ResourceBasedMessageContent content =
					new ResourceBasedMessageContent(LogicMultipointCreateAction.class,
							"LogicMultipointCreateAction.SharedConductorDeleted");
			content.setContextParameters(sharedConductor.getObjectTypeForDisplay());
			content.setMessageParameters(sharedConductor.getObjectTypeForDisplay());
			content.setImplicationsParameters(
					sharedConductor.getObjectTypeForDisplay() + " \"" + sharedConductor.getName() + "\"");
			Message.show(PromptSeverity.WARNING, content);
			return false;
		}
		project.getSharedConductorMgr().refresh();
		project.getSharedConductorMgr().fireChangeEvent();
		return true;
	}

	public boolean isConductorTypeUpdated(@NotNull ISharedConductor sharedConductor)
	{
		String oldType = sharedConductor.getType();
		if (sharedConductor.needsRefresh()) {
			sharedConductor.refresh();
		}
		String newType = sharedConductor.getType();
		if (!oldType.equals(newType)) {
			//Display message
			String header = ResourceMgr.getString(LogicMultipointCreateAction.class,
					"LogicMultipointCreateAction.SharedConductor.Header.text");
			String msg = ResourceMgr.getString(LogicMultipointCreateAction.class,
					"LogicMultipointCreateAction.SharedConductor.Msg.text");
			MessageHelper.showInformationMessage(null, header, msg);
			return false;
		}
		return true;
	}

	/**
	 * Return true of shared conductor has problems and action can't continue
	 *
	 * @param sharedConductor - Shared Conductor
	 * @param design - Design Container
	 *
	 * @return - true if problem exists. false if everything ok
	 */
	public boolean isSharedConductorUnusable(@NotNull ISharedConductor sharedConductor, @NotNull ILogicDesign design,
			@NotNull BiFunction<ISharedConductor, IProject, Boolean> sharedConductorRefreshHandler)
	{
		if (!isConductorTypeUpdated(sharedConductor)) {
			return true;
		}
		// Check that another revision of this shared object does not already exist in this design
		ISharedMulticore mc = sharedConductor.getMulticore();
		IProject project = design.getProject();
		if (project == null) {
			return true;
		}

		if (AddSharedHelper.isSharedObjectPermissionDenied()) {
			return true;
		}

		// Can't add an unfrozen shared object to a design that requires all shared objects to be frozen
		final ISharedObjectAvailabilityReporter reporter = new SharedObjectAvailabilityReporter();
		if (!new SharedObjectAvailabilityChecker().check(sharedConductor, design, reporter, true, true)) {
			return true;
		}

		boolean failure;
		if (mc == null) {
			failure = SharedObjectRevisionHelper.checkUsagesOfOtherRevisionForPlaceAction(design, sharedConductor);
		}
		else {
			// Refresh, if necessary, the shared multicore
			if (mc.needsRefresh()) {
				mc.refresh();
				project.getSharedConductorMgr().fireChangeEvent();
			}
			failure = SharedObjectRevisionHelper.testUsagesOfOtherRevisionMulticoreConductor(design, sharedConductor);
		}

		return failure || !sharedConductorRefreshHandler.apply(sharedConductor, project);
	}

	public boolean isReadyForActivation(@NotNull Class<? extends IConductor> conductorType)
	{
		// if we are in a transaction boundary, we MUST wait
		if (FactoryMgr.getSystemFactory().getCAFUtils().isWithinTransactionBoundary()) {
			return false; // wonder why this isn't in the super?
		}
		IConductor cond = getOperand();
		// for when we different UI strings for different conductor types:
//		((IUpdateableAction) getActionUI()).updateUI();

		return cond != null && conductorType.isAssignableFrom(cond.getClass());
	}

	@Nullable public chs.cof.logical.schem.IConductor processInstanceConductorCreation(
			@NotNull IConductor conductor, @NotNull ISchemDiagram diagram,
			@NotNull Supplier<chs.cof.logical.schem.IConductor> schemCondCreate
	)
	{
		chs.cof.logical.schem.IConductor schemCond = null;
		ILogicDesign design = conductor.getLogicDesign();
		if (design != null) {
			// port graphics should only be added for multiple representations of a conductor
			// count usages *before* creating the new conductor (usages will now reflect what is currently in the datamodel)
			IDesignWideUsageMgr dwum = design.getDesignWideUsageMgr();
			int usageCount = dwum.getDesignSharedUsageCount(conductor);

			// construct the new conductor
			schemCond = schemCondCreate.get();
			if (schemCond == null) {
				return null;
			}

			// handle port gfx and home reference based on usage count before the action is performed
			PortHelper.assignPortedConductor(schemCond, conductor);
			boolean home = true; // the first instance added is initially home
			if (usageCount > 0) {
				int gridSpacing = diagram.getGrid().getGridSpacing();
				PortHelper.addPortGraphics(schemCond, design, usageCount, gridSpacing,
						dwum.getRepresentations(conductor));
				home = false; // all instances after the first are initially non-home
			}
			schemCond.setHome(home); // similar rules for XRefs as shared
		}
		return schemCond;
	}
}
