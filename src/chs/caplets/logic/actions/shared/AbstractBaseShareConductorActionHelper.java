/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2014-2025 Siemens
 */
package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.helpers.CAFSharedUpdater;
import chs.caplets.logic.actions.actionreport.ActionChangeReportMgr;
import chs.caplets.logic.actions.actionreport.IMergeActionChange;
import chs.caplets.logic.actions.actionreport.IMergeActionChangeReporter;
import chs.caplets.logic.actions.actionreport.IMergeComparison;
import chs.caplets.logic.actions.ui.ShareIntoFacetConflictResolutionController;
import chs.caplets.logic.actions.ui.ShareIntoFacetConflictResolutionDialog;
import chs.caplets.logic.merge.ConductorMerger;
import chs.cof.COFTypeEnum;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IPropText;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.IBaseShareableDiagramObject;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.IShareableDiagramObject;
import chs.cof.logical.schem.IShareableDiagramObjectWithMultipleRepresentation;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.IDesignSharedUsageMgr;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.IRevisionedSharedObject;
import chs.cof.logical.shared.IShareHelper;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedObjectMgr;
import chs.cof.logical.shared.ISharedObjectModificationObserver;
import chs.cof.project.IProject;
import chs.cofUtils.logical.concurrency.ShareConcurrencyHelper;
import chs.common.IAttributePropertyProvider;
import chs.common.IRefreshable;
import chs.common.IUID;
import chs.common.RefreshStatusEnum;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.TransactionHelper;
import chs.utilities.ui.MessageHelper;
import chs.utility.helpers.PropertyHelper;
import chs.utility.helpers.revisioning.SharedObjectRevisionHelper;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import chs.utility.logic.LogicUtils;
import chs.utility.logic.SharedObjectAvailabilityChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class AbstractBaseShareConductorActionHelper<T extends ISharedObject> implements IShareActionHelper
{

	@NotNull protected ILogicDesign m_design;
	@Nullable protected ISchemDiagram m_diagram;
	protected ILogicObject m_logicObject;
	protected IBaseShareableDiagramObject m_schemObject;
	private boolean isUnitTest = false;
	@Nullable protected IUID shareInto = null;
	protected ISharedObjectMgr m_sharedObjectMgr = null;
	private IShareHelper m_shareHelper;

	protected AbstractBaseShareConductorActionHelper(@NotNull ILogicDesign design, @Nullable ISchemDiagram diagram,
			IShareHelper shareHelper)
	{
		m_design = design;
		m_diagram = diagram;
		m_shareHelper = shareHelper;
	}

	@NotNull protected LockUpdateHelper getUpdateHelper(@NotNull ISharedObject shareIntoObject, boolean showMessageOnFail)
	{
		LockUpdateHelper lockHelper = new LockUpdateHelper(shareIntoObject, showMessageOnFail);
		return lockHelper;
	}

	@Override
	@NotNull public IActionEnum setup(@NotNull BaseShareActionOperands operands, @Nullable String dialogTitle,
			@Nullable ISchemDiagram diagram)
	{
		if (operands.target instanceof IRepresentedObject && !(operands.target instanceof IShareableDiagramObject ||
				operands.target instanceof IShareableDiagramObjectWithMultipleRepresentation)) {
			return IActionEnum.eCanceled;
		}
		/*if (!(operands.target instanceof IConductor || operands.target instanceof chs.cof.logical.cable.IConductor)) {
			return IActionEnum.eCanceled;
		}*/
		if (operands.target instanceof IShareableDiagramObject ||
				operands.target instanceof IShareableDiagramObjectWithMultipleRepresentation) {
			m_schemObject = (IBaseShareableDiagramObject) operands.target;
		}
		else {
			m_schemObject = null;
		}
		m_logicObject = operands.getLogicObject();
		m_sharedObjectMgr =
				m_logicObject instanceof IPinList ? m_design.getProject().getSharedPinListMgr() :
						m_design.getProject().getSharedConductorMgr();
		if(!isBulkPromotion() && !m_sharedObjectMgr.isLocked()) {
			m_sharedObjectMgr.refresh();
		}
		m_diagram = diagram;

		if (!isUnitTest) {
			return doSetup();
		}

		return IActionEnum.eCompleted;
	}

	@NotNull protected IActionEnum doSetup()
	{
		IProject project = m_design.getProject();
		assert project != null;
		ISharedConductorMgr condMgr = project.getSharedConductorMgr();
		return checkForDuplicate(condMgr);
	}

	@NotNull private IActionEnum checkForDuplicate(@NotNull ISharedConductorMgr condMgr)
	{
		//dts0100874550 this set must be ordered because we want design used shared conds/highways to come first.
		Set<T> shCondOrderedSet = getOrderedSharedConductors(condMgr);
		for (T shCond : shCondOrderedSet) {
			if (hasDuplicateName(shCond)) {
				String name = m_logicObject.getName();
				ISharedObjectAvailabilityReporter nullreporter = ISharedObjectAvailabilityReporter.NULL_REPORTER;
				if (!new SharedObjectAvailabilityChecker().check(shCond, m_design, nullreporter)) {
					continue;
				}
				final String objectType = getLogicObjectDisplayType();
				int answer = handleDuplicateName(name, objectType);
				if (answer == MessageHelper.RESULT_YES) {
					shareInto = null;
				}
				else if (answer == MessageHelper.RESULT_NO) {
					shareInto = shCond.getUID();
				}
				else if (answer == MessageHelper.RESULT_ALL) {
					// Result All actually corrosponds to cancel, but thanks to the wonderful static int the message
					// helper uses, we have to use result all.
					shareInto = null;
					return IActionEnum.eCanceled;
				}
				break;
			}
		}

		return IActionEnum.eCompleted;
	}

	@NotNull protected String getLogicObjectDisplayType()
	{
		return COFTypeEnum.getDisplayableTypeName(m_logicObject);
	}

	protected abstract Iterator<T> getShareObjectsUsedOnDesign();

	@NotNull protected abstract Set<T> getOrderedSharedConductors(@NotNull ISharedConductorMgr condMgr);

	protected abstract boolean hasDuplicateName(@NotNull T sharedObject);

	protected abstract int handleDuplicateName(@NotNull String name, @NotNull String objectType);

	protected int showDialogToHandleDuplicateName(String title, String heading, String msg, String[] options,
			String defaultOption)
	{
		return MessageHelper.showYesNoCancelDialog(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
				title, heading, msg, options, defaultOption);
	}

	protected abstract void reassignConnectivityForSchematic(IDiagramObject diagObj, ILogicObject logicObject);

	public boolean doEdit()
	{
		boolean commitSuccessful = false;

		try {
			if (!isUnitTest) {
				TransactionHelper.beginTransaction();
			}
			//
			// dts0100538509 Scan for Prop text.  Since the properties will change the text will need to be updated
			Map<IPropText, String> txtInst = PropertyHelper.pickPropertyText(m_schemObject, true);
			// dts0100668787 - Scan for prop texts on all representations of this object
			if (m_logicObject instanceof chs.cof.logical.cable.IConductor || m_logicObject instanceof IHighway) {
				IDesignWideUsageMgr dwum = m_design.getDesignWideUsageMgr();
				for (IDiagramObject diagramObj : dwum.getRepresentations(m_logicObject)) {
					txtInst.putAll(PropertyHelper.pickPropertyText(diagramObj, true));
				}
			}

			ILogicDesign logicDesign = CommonUtils.cast(m_design, ILogicDesign.class);
			String failureMsg = ResourceMgr.getString(AbstractBaseShareConductorActionHelper.class,
					"BaseShareActionHelper.ShareFailureInMU.Message.text");
			if (logicDesign != null) {
				if (!ShareConcurrencyHelper.attemptLockOnSourceObjectForShare(m_logicObject, logicDesign, failureMsg)) {
					return false;
				}
			}
			if (m_schemObject instanceof IConductor) {
				ConductorMerger.processCompositeDecorationTexts((IConductor) m_schemObject);
			}

			final ISharedObject shareIntoObject = UIDMgr.getObjectOfType(shareInto, ISharedObject.class);
			if (shareIntoObject != null) {
				//cases where other revisions of sharedIntoObject already exists in design - don't allow to share into
				if (logicDesign != null && shareIntoObject instanceof IRevisionedSharedObject &&
						SharedObjectRevisionHelper.checkUsagesOfOtherRevisionForShareIntoAction(logicDesign,
								(IRevisionedSharedObject) shareIntoObject)) {
					return false;
				}
				final LockUpdateHelper lockHelper = getUpdateHelper(shareIntoObject, true);
				final boolean isIRefreshable = shareIntoObject instanceof IRefreshable && !isNewlyCreatedSharedObject();
				if (lockHelper.lock()) {
					if (isIRefreshable &&
							((IRefreshable) shareIntoObject).refresh() == RefreshStatusEnum.eObjectDoesNotExist) {
						reportSharedObjectDeleted(shareIntoObject);
						lockHelper.unlock();
						return false;
					}
					// disallow if there are any transient usages
					if (logicDesign != null && !ShareConcurrencyHelper.trySharedObjectPlacement(logicDesign,
							Collections.singleton(shareIntoObject)).contains(shareIntoObject)) {
						return false;
					}

					IDesignSharedUsageMgr usageMgr = m_design.getSharedUsageMgr();
					Iterator<IDesignSharedUsage> it = usageMgr.getUsages(shareIntoObject).iterator();
					IMergeActionChangeReporter mergeActionReporter =
							ActionChangeReportMgr.getInstance().createMergeActionChangeReporter();
					IMergeComparison<IMergeActionChange, IAttributePropertyProvider> comparison = null;
					if (isChangeReportingRequired()) {
						comparison = mergeActionReporter.createComparison();
						comparison.setInitialStateOfSourceObject(m_logicObject);
						comparison.setInitialStateOfTargetObject(shareIntoObject);
					}

					final ISharedObjectModificationObserver observer = new SharedObjectModificationObserver();
					if (it.hasNext()) {
						IDesignSharedUsage dsu = it.next();
						if (logicDesign != null) {
							if (!ShareConcurrencyHelper
									.attemptLockOnTargetObjectForShare(m_logicObject, dsu.getLogicObject(), logicDesign,
											failureMsg)) {
								return false;
							}
						}
						Runnable sharingActivity = () -> reassignConnectivity(dsu, shareIntoObject);
						attemptShare(observer, sharingActivity);
						commitSuccessful = true;
					}
					else {
						IConnectivity designConnectivity = m_design.getConnectivity();
						assert designConnectivity != null;
						ILogicObject logicObject = designConnectivity.findLogicObjectForShared(shareIntoObject);
						if (logicObject != null) {
							//Unplaced case
							Runnable sharingActivity = () -> reassignConnectivity(logicObject, shareIntoObject);
							attemptShare(observer, sharingActivity);
							commitSuccessful = true;
						}
						else {
							boolean alreadyLocked = m_sharedObjectMgr.isLocked() && isBulkPromotion();
							if ( alreadyLocked || m_sharedObjectMgr.lock() ) {
								if(!alreadyLocked) {
									m_sharedObjectMgr.refresh();
								}
								Runnable sharingActivity = () -> m_shareHelper.share(observer,m_logicObject, shareIntoObject);
								attemptShare(observer, sharingActivity);
								commitSuccessful = true;
							}
							else {
								reportSharedObjectMgrLocked();
							}
						}
					}
					if (commitSuccessful && isChangeReportingRequired() && comparison != null) {
						IConnectivity designConnectivity = m_design.getConnectivity();
						assert designConnectivity != null;
						ILogicObject logicObject = designConnectivity.findLogicObjectForShared(shareIntoObject);
						if (logicObject != null) {
							comparison.setTransformedState(logicObject);
							mergeActionReporter.reportChanges();
						}
					}

					if (observer.isModified()) {
						// save and unlock the shared object
						lockHelper.flushAndUnlock(true);
					} else {
						// unlock the shared object
						lockHelper.unlock();
					}
				}
				else {
					return false;
				}
				//LOGIC-8146 Architectural cost text is incorrectly updated in share-into scenario.
				LogicUtils
						.ensureDiagramRepresentationsRegenerated(m_design, shareIntoObject, CommonUtils.getNoFilter());
			}
			else {
				boolean alreadyLocked = m_sharedObjectMgr.isLocked() && isBulkPromotion();
				if (alreadyLocked || m_sharedObjectMgr.lock()) {
					if(!alreadyLocked) {
						m_sharedObjectMgr.refresh();
					}
					m_shareHelper.share(new SharedObjectModificationObserver(),m_logicObject, null);
					commitSuccessful = true;
				}
				else {
					reportSharedObjectMgrLocked();
				}
			}
			if (m_schemObject != null) {
				m_schemObject.setHome(m_schemObject.isHome());
			}
			//
			// dts0100538509 Reassign the PropTexts based on the previous scan.
			PropertyHelper.reassignPropertyText(txtInst, m_logicObject.getSharedObject());
			if (!isUnitTest) {
				TransactionHelper.endTransaction();
			}
		}
		finally {
			if (!commitSuccessful && !isUnitTest) {
				TransactionHelper.rollbackTransaction();

				CAFSharedUpdater sr =
						new CAFSharedUpdater(m_design.getProject(), CAFUtils.getInstance().getWindowMgr());
				sr.updateSharedConductorMgr();
			}
			if(!isBulkPromotion()) {
				m_sharedObjectMgr.unlock();
			}
			if (m_diagram != null) {
				m_diagram.refreshRepresentations();
			}
			if (!isBulkPromotion()) {
				m_sharedObjectMgr.fireChangeEvent();
			}
		}
		return commitSuccessful;
	}

	protected boolean isNewlyCreatedSharedObject()
	{
		return false;
	}

	protected abstract boolean isBulkPromotion();

	protected abstract boolean isChangeReportingRequired();

	protected abstract void reportSharedObjectMgrLocked();

	protected abstract void reportSharedObjectDeleted(@NotNull ISharedObject shareIntoObj);

	protected void attemptShare(@NotNull ISharedObjectModificationObserver observer, @NotNull Runnable sharingActivity)
	{
		attemptShareWithConflictResolution(observer, sharingActivity);
	}

	private void attemptShareWithConflictResolution(@NotNull ISharedObjectModificationObserver observer, @NotNull Runnable sharingActivity)
	{
		ShareIntoFacetConflictResolutionController controller =
				new ShareIntoFacetConflictResolutionController(m_logicObject);
		ShareIntoFacetConflictResolutionDialog resolutionDialog =
				new ShareIntoFacetConflictResolutionDialog(observer, controller, controller);
		sharingActivity.run();
		resolutionDialog.process(m_logicObject);
	}

	protected void reassignConnectivity(IDesignSharedUsage dsu, final ISharedObject shareIntoObject)
	{
		// If there are already usages of this shared object in this design, then set that connectivity
		// object on this schem object
		ILogicObject logicObject = dsu.getLogicObject();
		reassignConnectivity(logicObject, shareIntoObject);
	}

	protected void reassignConnectivity(ILogicObject logicObject, final ISharedObject shareIntoObject)
	{
		//LOGIC-12693:SP2104 BashXQPT4:New properties assigned on unshared Net/Wire are
		//not propagated to the shared object after share into action(using Use existing action)
		PropertyHelper.transferProperties(m_logicObject, shareIntoObject, true);
		for (IDiagramObject diagObj : m_design.getRepresentations(m_logicObject.getUID())) {
			reassignConnectivityForSchematic(diagObj, logicObject);
		}
		transferConnectivity(logicObject);
		// Delete that dead local connectivity
		m_logicObject.delete();
		m_logicObject = logicObject;
	}

	protected abstract void transferConnectivity(ILogicObject logicObject);

	public void cleanup()
	{
		shareInto = null;
		m_logicObject = null;
		m_schemObject = null;
	}

	public boolean isNewSharedObject()
	{
		return shareInto == null;
	}

	public void setUnitTest()
	{
		isUnitTest = true;
	}

	public void setShareHelper(IShareHelper m_shareHelper)
	{
		this.m_shareHelper = m_shareHelper;
	}

	@Override @Nullable public IUID getSharedObjectUID()
	{
		return shareInto != null ? shareInto : (m_logicObject != null ? m_logicObject.getSharedObjectUID() : null);
	}

	@Override public boolean isShareInto()
	{
		ISharedObject shareIntoObject = UIDMgr.getObjectOfType(shareInto,ISharedObject.class);
		return shareIntoObject != null;
	}
}
