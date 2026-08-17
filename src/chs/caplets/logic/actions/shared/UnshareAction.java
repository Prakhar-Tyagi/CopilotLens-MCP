/*
 * Copyright 2006-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.Model;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IFunctionConductor;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IHarnessPlugConnector;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.cable.IInterconnectConnector;
import chs.cof.logical.cable.IInterconnectDevice;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedLockableUpdateableObject;
import chs.cof.logical.shared.ISharedObject;
import chs.common.RefreshStatusEnum;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.ProjectHelper;
import chs.utility.helpers.PinListHelper;
import chs.utility.helpers.UnsharePinListLockHandler;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import chs.utility.logic.LogicObjectUtils;
import chs.utility.logic.PinUtils;
import chs.utility.logic.SharedObjectAvailabilityChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

public class UnshareAction extends BaseShareAction
{

	@Nullable private IActionEnum unShareHelperSetUpResult;

	public UnshareAction(ICapletController controller)
	{
		super(controller);
		m_conductorHelper = new UnshareConductorActionHelper(m_design);
		m_FunctionConductorHelper = new UnshareFunctionBaseConductorActionHelper(m_design);
		m_pinListHelper = new UnsharePinListActionHelper(m_design);
		m_condGroupHelper = new UnshareConductorGroupActionHelper(m_design);
		m_highwayHelper = new UnshareHighwayActionHelper(m_design);
		m_singleLineHelper = new UnshareSingleLineActionHelper(m_design);
		m_functionMessageHelper = new UnshareFunctionBaseConductorActionHelper(m_design);
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		super.onActivate(e);

		//dts0101439710 - restrict unshare if domain restricted on shared object
		if (!isSharedObjectAccessible()) {
			return IActionEnum.eCanceled;
		}
		unShareHelperSetUpResult = setUpActionHelper(
				Objects.requireNonNull(CommonUtils.cast(getController().getCapletModel(), Model.class)));
		return unShareHelperSetUpResult;
	}

	@NotNull protected IActionEnum setUpActionHelper(@NotNull Model model)
	{
		return m_helper.setup(m_operands, CAFUtils.getInstance().getDialogTitleByAction(this), model.getDiagram());
	}

	protected void showDomainAccessInfoDialog()
	{
		String resourceKeyRoot = "UnshareAction.noDomainAccess";
		ResourceBasedMessageContent content = new ResourceBasedMessageContent(this, resourceKeyRoot);
		Message.show(PromptSeverity.ERROR, content);
	}

	protected boolean hasSharedObjectDomainAccess()
	{
		if (m_operands == null) {
			return true;
		}
		ILogicObject logicObject = m_operands.getLogicObject();
		if (!refreshAndCheckDomainAccess(logicObject)) {
			return false;
		}

		if (logicObject instanceof IGenericInlineConnector) {
			IGenericInlineConnector cablePinListMate = m_operands.getCablePinListMate();
			return refreshAndCheckDomainAccess(cablePinListMate);
		}

		return true;
	}

	protected boolean refreshAndCheckDomainAccess(@Nullable ILogicObject logicObject)
	{
		if (logicObject == null) {
			return true;
		}

		ISharedLockableUpdateableObject sharedObject =
				CommonUtils.cast(logicObject.getSharedObject(), ISharedLockableUpdateableObject.class);

		if (sharedObject == null) {
			return true;
		}

		RefreshStatusEnum refresh = sharedObject.refresh();
		if (refresh == RefreshStatusEnum.eObjectDoesNotExist) {
			return true;
		}

		return sharedObject.isAccesible();
	}

	protected boolean onTerminate(boolean successful)
	{
		boolean editSuccessful = false;
		try {
			editSuccessful = super.onTerminate(successful);
			if (editSuccessful) {
				getController().getSelectMgr().notifySelectionChanged();
			}
		}
		finally {
			if (m_helper != null && unShareHelperSetUpResult != null) {
				m_helper.cleanup();
				unShareHelperSetUpResult = null;
			}
		}
		return editSuccessful;
	}

	public boolean isEnabled()
	{
		if (!super.isEnabled()) {
			return false;
		}
		BaseShareActionOperands operands = getUnshareOperands(getController().getSelectMgr().getPreSelections());

		if (operands != null) {
			if (!isValidByLockingContraint(operands)) {
				return false;
			}
		}

		//we can have cases of unplaced objects where menu will be shown
		//in context menu but as disabled and with reason as tooltip
		return (operands != null && !shouldShowMenuAsDisabled(operands));
	}

	private boolean isSharedObjectAccessible()
	{
		if (m_operands == null) {
			return true;
		}

		ILogicObject logicObject = m_operands.getLogicObject();
		if (logicObject == null) {
			return true;
		}

		ISharedLockableUpdateableObject sharedObject =
				CommonUtils.cast(logicObject.getSharedObject(), ISharedLockableUpdateableObject.class);

		if (sharedObject != null) {
			RefreshStatusEnum refresh = sharedObject.refresh();
			if (refresh == RefreshStatusEnum.eObjectDoesNotExist) {
				return true;
			}
			return new SharedObjectAvailabilityChecker()
					.check(sharedObject, m_design, new UnshareReporter(this::showDomainAccessInfoDialog), false);
		}

		return true;
	}

	private static class UnshareReporter implements ISharedObjectAvailabilityReporter
	{

		@NotNull private Runnable reporter;

		UnshareReporter(@NotNull Runnable reporter)
		{
			this.reporter = reporter;
		}

		@Override
		public void report(@NotNull FailureReason reason, @NotNull ISharedObject sharedObject, @Nullable IDesign design)
		{
			if (reason == FailureReason.DOMAIN_ON_SHARED_OBJECT) {
				reporter.run();
			}
			else {
				new SharedObjectAvailabilityReporter().report(reason, sharedObject, design);
			}
		}
	}

	public boolean isValidByLockingContraint(@NotNull BaseShareActionOperands shareOperands)
	{
		chs.cof.logical.cable.IPinList cablePinList = shareOperands.getCablePinList();
		if (cablePinList != null) {
			UnsharePinListLockHandler handler = new UnsharePinListLockHandler();
			UnsharePinListLockHandler.ILockStatus iLockStatus =
					handler.checkLockOnSourceImpactedObjectForUnshare(getUnshareContext(shareOperands));
			if (iLockStatus.isLocked()) {
				m_disabledReason = iLockStatus.getReason();
				return false;
			}
		}
		return true;
	}

	private UnsharePinListLockHandler.IUnshareContext getUnshareContext(@NotNull BaseShareActionOperands shareOperands)
	{

		Model model = (Model) getController().getCapletModel();
		ISchemDiagram diagram = model.getDiagram();
		chs.cof.logical.cable.IPinList cablePinList = shareOperands.getCablePinList();
		assert cablePinList != null;
		GenericPinListUnshareHelper helper = ((UnsharePinListActionHelper) m_pinListHelper).createUnshareHelper(
				cablePinList, diagram);
		helper.initializePinList(diagram, cablePinList, shareOperands.getCablePinListMate(),
				shareOperands.getPinListRepresentations());
		return new UnsharePinListLockHandler.IUnshareContext()
		{
			@Override public chs.cof.logical.cable.IPinList getPinListToUnShare()
			{
				return cablePinList;
			}

			@Override public boolean unshareAll()
			{
				return helper.determineToUnshareAll(m_design.getDesignWideUsageMgr());
			}

			@Override public Collection<IPinList> getPinListsToUnshare()
			{
				List<IPinList> pinlists = new ArrayList<>();
				pinlists.addAll(shareOperands.getPinListRepresentations());
				pinlists.addAll(helper.getAdditionalSchemObjectsToProcess());
				return pinlists;
			}
		};
	}

	public String getActionUIClass()
	{
		return UnshareActionUI.class.getName();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		// Disabled for normal use
		//
		if (getUnshareOperands(selections) != null) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	/**
	 * Checks the current selection to ensure the single item can be unshared.  In the case of inlines, expects both the
	 * plug and jack to be selected.
	 *
	 * @param selections SelectSet
	 *
	 * @return BaseShareActionOperands has reference to UID object and it's mate if applicable.  May return null if
	 * nothng selected is eligible for unsharing
	 */
	@Nullable public static BaseShareActionOperands getUnshareOperands(SelectSet selections)
	{
		BaseShareActionOperands operands = getOperands(selections);
		if (operands == null) {
			return null;
		}

		// TODO jacobt FEAT13040 : probably should get rid of "target" and "mate" from BaseShareOperands
		// keeping all this stuff for now in case of older clients still using it
		if (operands.target instanceof IPinList) {
			IPinList pinList = (IPinList) operands.target;
			// Already shared?
			chs.cof.logical.cable.IPinList cpl = pinList.getConnectivity();
			if (cpl.getSharedPinList() == null) {
				return null;
			}

			if (pinList.getConnectivity() instanceof IInterconnectDevice ||
					pinList.getConnectivity() instanceof IInterconnectConnector) {
				// Don't allow unshare on interconnect devices or connectors.
				return null;
			}

			if (operands.mate != null && !pinList.hasAttachedObject(operands.mate)) {
				return null;
			}
		}
		else if (operands.target instanceof IMulticore) {
			IMulticore mc = (IMulticore) operands.target;
			// Already shared?
			if (mc.getSharedMulticore() == null) {
				return null;
			}
		}
		else if (operands.target instanceof IConductor) {
			IConductor conductor = (IConductor) operands.target;
			chs.cof.logical.cable.IConductor connectivityConductor = conductor.getConnectivity();
			if (connectivityConductor.getSharedConductor() == null) {
				return null;
			}
		}
		else if (operands.target instanceof chs.cof.logical.cable.IConductor) {
			chs.cof.logical.cable.IConductor conductor = (chs.cof.logical.cable.IConductor) operands.target;
			if (!conductor.isShared()) {
				return null;
			}
		}
		else if (operands.getLogicObject() instanceof IFunctionConductor &&
				((IFunctionConductor) operands.getLogicObject()).isAssociatedMessageSignal()) {
			//This refers to the signal inside a message
			return null;
		}

		else if (operands.getLogicObject() instanceof IHighway) {
			IHighway highway = (IHighway) operands.getLogicObject();
			if (!highway.isShared()) {
				return null;
			}
		}
		else if (operands.target instanceof IHighwaySchematic) {
			IHighwaySchematic highwaySchematic = (IHighwaySchematic) operands.target;
			IHighway highway = highwaySchematic.getConnectivity();
			if (!highway.isShared()) {
				return null;
			}
		}
		else if (operands.getLogicObject() != null) {
			// this should be the normal case instead of all that checking intanceof of operands.target
			// currently this means that we're unsharing an object with no representations
			ILogicObject logicObject = operands.getLogicObject();
			assert logicObject != null;
			if (!logicObject.isShared()) {
				return null;
			}
		}
		else {
			// Empty selection?
			return null;
		}

		// FEAT13040 : Unshare of multiple instances
		// we can unshare multiple instances of a shared object but only if all instances are selected
		// we can always still unshare a single instance here but only according to the old functionality where a new connectivity would be created
		ILogicObject logicObject = operands.getLogicObject();
		if (logicObject != null) { // really we should return if its null
			ILogicDesign design = logicObject.getLogicDesign();
			if (design != null) {
				int repCount = operands.getRepresentations().size();
				//
				//this footprint restriction was basically meant for auto-ghc only and since
				//we are now allowing ghc on device connector footprint the unshare has become
				//even more restrictive. so limiting the restriction to auot-ghc only. due to
				//this restruction test on dts0100922143 was also getting affected in unshareTest.
				boolean footprinted = (logicObject instanceof chs.cof.logical.cable.IPinList) &&
						PinListHelper.isHarnessFootprintedAndAllowAutoCreation(
								(chs.cof.logical.cable.IPinList) logicObject);
				if (footprinted) {
					//
					// If the instance is Footprinted, then we need to look for any attached harness connectors.
					// If there are none, then we can treat it as if it was not footprinted as there is no external
					// object that needs to be changed.
					//
					boolean attachedHarness = false;
					for (IPinList pl : operands.getPinListRepresentations()) {
						for (IPinList att : pl.getAttachedPinListObjects()) {
							if (att.getConnectivity() instanceof IHarnessPlugConnector) {
								attachedHarness = true;
							}
						}
					}
					if (!attachedHarness) {
						footprinted = false; // Treat as if it was not footprinted
					}
				}
				//
				// If there are 3 regular, non footprinted instances (usages), and we select 1 instance, then all is OK.
				// however if we select 2, then it's not OK - we only allow selecting 1 or all.
				//
				// As to the  footprint - if the logic object has a harness footprint, then
				// we need to select all in order to be able to unshare, as the harness connectors
				// will need to be processed in the correct context.
				//
				if ((repCount > 1 || footprinted) &&
						design.getDesignWideUsageMgr().getDesignSharedUsageCount(logicObject) > repCount) {
					return null;
				}
			}
		}
		return operands;
	}

	private boolean shouldShowMenuAsDisabled(@NotNull BaseShareActionOperands operands)
	{
		//check if we will produce duplicate pins after unshare operation.
		chs.cof.logical.cable.IPinList cablePinList = operands.getCablePinList();
		if (cablePinList != null) {
			if (PinUtils.allowDuplicatePinsOnDesign(ProjectHelper.getProject(cablePinList))) {
				return false;
			}
			if (PinUtils.areHavingPinWithMultipleInstances(operands.getPinListRepresentations(), true, true)) {
				m_disabledReason = ResourceMgr.getString(UnshareAction.class,
						"UnshareAction.DisableReason.PinList.text",
						LogicObjectUtils.getLogicObjectType(cablePinList));
				return true;
			}
		}
		return false;
	}
}