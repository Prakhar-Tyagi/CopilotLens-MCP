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
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.IShieldBody;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedObjectModificationObserver;
import chs.cofUtils.logical.concurrency.ShareConcurrencyHelper;
import chs.common.IAttributePropertyProvider;
import chs.common.IPropertiedObject;
import chs.common.IProperty;
import chs.common.IStringIterator;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.RefreshStatusEnum;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.system.UIDMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utilities.TransactionHelper;
import chs.utilities.ui.MessageHelper;
import chs.utility.helpers.PropertyHelper;
import chs.utility.helpers.SharedConductorGroupHelper;
import chs.utility.helpers.SharedConductorHelper;
import chs.utility.persist.SharedObjectPersistenceHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Apr 14, 2004 Time: 10:41:13 AM
 */
public abstract class AbstractShareConductorGroupActionHelper implements IShareActionHelper
{

	@Nullable protected IMulticore m_multicore;
	@NotNull protected ILogicDesign m_design;
	@Nullable protected IUID m_sharedMulticore;
	@NotNull protected final Map<ILogicObject, IUID> m_multicoreToSharedHierarchyMap = new HashMap<>();
	@NotNull private Collection<IUIDObject> m_objectsToBeDeleted = new ArrayList<>();
	@Nullable protected LockUpdateHelper m_lockHelper;
	@Nullable protected IMulticoreShareContextProvider m_multicoreShareContextProvider;

	protected AbstractShareConductorGroupActionHelper(@NotNull ILogicDesign design)
	{
		m_design = design;
		m_multicore = null;
	}

	@Override
	@NotNull public IActionEnum setup(@NotNull BaseShareActionOperands operands, @Nullable String dialogTitle,
			@Nullable ISchemDiagram diagram)
	{
		if (!(operands.target instanceof IMulticore)) {
			return IActionEnum.eCanceled;
		}

		m_multicore = (IMulticore) operands.target;
		if (!attemptLockOnSourceMulticoreForShare(m_multicore, m_design, getShareFailureInMUMsg())) {
			return IActionEnum.eCanceled;
		}
		boolean isContextProviderValid = createAndValidateMulticoreShareContextProvider(m_multicore, dialogTitle);
		if (m_multicoreShareContextProvider == null || !isContextProviderValid) {
			return IActionEnum.eCanceled;
		}
		m_sharedMulticore = m_multicoreShareContextProvider.getSharedMulticoreUID();
		if (m_sharedMulticore != null) {
			m_multicoreToSharedHierarchyMap.putAll(m_multicoreShareContextProvider.getMulticoreToSharedHierarchyMap());
		}
		return IActionEnum.eCompleted;
	}

	protected abstract boolean createAndValidateMulticoreShareContextProvider(@NotNull IMulticore multicore,
			@Nullable String dialogTitle);

	public boolean doEdit()
	{
		if (m_multicore == null) {
			return false;
		}
		if (!lockObjects()) {
			return false;
		}

		// Shared actions now have their own transaction
		boolean commitSuccessful = false;
		ISharedConductorMgr sharedCondrMgr = m_design.getProject().getSharedConductorMgr();
		try {
			boolean bAlreadyLocked = sharedCondrMgr.isLocked() && isBulkPromotion();
			if (bAlreadyLocked || sharedCondrMgr.lock()) {
				if(!bAlreadyLocked) {
					sharedCondrMgr.refresh();
				}
				TransactionHelper.beginTransaction();

				doTheEdit();

				TransactionHelper.endTransaction();
				commitSuccessful = true;
			}
			else {
				reportSharedCondMgrLocked(sharedCondrMgr);
				commitSuccessful = false;
			}
		}
		finally {
			if (!commitSuccessful) {
				TransactionHelper.rollbackTransaction();

				CAFSharedUpdater sr = new CAFSharedUpdater(m_design.getProject(),
						CAFUtils.getInstance().getWindowMgr());
				sr.updateSharedConductorMgr();
			}
			if(!isBulkPromotion()) {
				sharedCondrMgr.unlock();
			}
			unlockObjects();
			if (!isBulkPromotion()) {
				sharedCondrMgr.fireChangeEvent();
			}
		}
		return commitSuccessful;
	}

	protected void doTheEdit()
	{
		if (m_multicore == null || m_multicoreShareContextProvider == null) {
			return;
		}
		final ISharedMulticore sharedMulticore = UIDMgr.getObjectOfType(m_sharedMulticore, ISharedMulticore.class);
		if (sharedMulticore == null) {
			SharedConductorGroupHelper
					.share(m_multicore, m_design, m_multicoreShareContextProvider.getSharedMulticoreName(),
							m_multicoreShareContextProvider.getSharedMulticoreRevision(),
							m_multicoreShareContextProvider.isSharedMulticoreNameGenerated(),
							m_multicoreShareContextProvider.getSharedDomains(), shouldSyncWithLibraryPart());
		}
		else {
			final ISharedObjectModificationObserver observer = new SharedObjectModificationObserver();
			Runnable resolveConflict = getConflictResolver(observer);
			for (ILogicObject srcObject : m_multicoreToSharedHierarchyMap.keySet()) {
				ISharedObject targetSharedObject =
						UIDMgr.getObjectOfType(m_multicoreToSharedHierarchyMap.get(srcObject), ISharedObject.class);
				if (targetSharedObject != null && !targetSharedObject.isFrozen()) {
					transferProperties(srcObject, targetSharedObject);
					observer.setModified();
				}
			}
			IMergeActionChangeReporter mergeActionReporter =
					ActionChangeReportMgr.getInstance().createMergeActionChangeReporter();
			IMergeComparison<IMergeActionChange, IAttributePropertyProvider> comparison = null;
			if (isChangeReportingRequired()) {
				comparison = mergeActionReporter.createComparison();
				comparison.setInitialStateOfSourceObject(m_multicore);
				comparison.setInitialStateOfTargetObject(sharedMulticore);
				for (ILogicObject obj : m_multicoreToSharedHierarchyMap.keySet()) {
					ISharedObject sharedObject =
							UIDMgr.getObjectOfType(m_multicoreToSharedHierarchyMap.get(obj), ISharedObject.class);
					if (sharedObject != null) {
						comparison.addObjectMapping(obj, sharedObject);
					}
				}
			}
			boolean shareIntoSuccess = share(m_multicore);
			if (shareIntoSuccess) {
				resolveConflict.run();
				if (isChangeReportingRequired() && comparison != null) {
					ILogicObject existingConnectivity = getExistingConnectivity(sharedMulticore);
					if (existingConnectivity != null) {
						comparison.setTransformedState(existingConnectivity);
						mergeActionReporter.reportChanges();
					}
				}
				m_objectsToBeDeleted.forEach(IUIDObject::delete);

				if (observer.isModified() && m_lockHelper != null) {
					// save the shared object
					SharedObjectPersistenceHelper.saveSharedMulticore(sharedMulticore);
				}
			}
		}
	}

	protected boolean isNewlyCreatedSharedObject()
	{
		return false;
	}

	protected abstract boolean shouldSyncWithLibraryPart();

	private void transferProperties(IPropertiedObject src, IPropertiedObject dest)
	{
		for (IProperty srcProp : src.getProperties()) {
			String propName = srcProp.getName();
			IProperty destProp = dest.findPropertyByName(propName);
			if (destProp == null) {
				// transfer to dest object
				IProperty temp = PropertyHelper.cloneProp(srcProp, dest);
				dest.addProperty(temp);
			}
		}
	}

	protected boolean lockObjects()
	{
		if (m_multicore == null) {
			return false;
		}
		final ISharedMulticore sharedMulticore = UIDMgr.getObjectOfType(m_sharedMulticore, ISharedMulticore.class);
		if (sharedMulticore != null) {
			m_lockHelper = new LockUpdateHelper(sharedMulticore, true);
			if (m_lockHelper.lock()) {
				//Checking if the shared object is newly created (Used as a check from delta share)
				if (sharedMulticore.refresh() == RefreshStatusEnum.eObjectDoesNotExist && !isNewlyCreatedSharedObject()) {
					reportSharedObjectDeleted(sharedMulticore);
					m_lockHelper.unlock();
					return false;
				}
				// disallow if there are any transient usages
				if (!ShareConcurrencyHelper.trySharedObjectPlacement(m_design,
						Collections.singleton(sharedMulticore)).contains(sharedMulticore)) {
					m_lockHelper.unlock();
					return false;
				}
				ILogicObject targetSharedMCConnectivity = getExistingConnectivity(sharedMulticore);
				if (targetSharedMCConnectivity != null &&
						!ShareConcurrencyHelper.attemptLockOnTargetObjectForShare(m_multicore,
								targetSharedMCConnectivity, m_design, getShareFailureInMUMsg())) {
					m_lockHelper.unlock();
					return false;
				}
			}
			else {
				return false;
			}
		}
		return true;
	}

	@NotNull private String getShareFailureInMUMsg()
	{
		return ResourceMgr.getString(AbstractShareConductorGroupActionHelper.class,
				"ShareConductorGroupActionHelper.ShareFailureInMU.Message.text");
	}

	protected boolean attemptLockOnSourceMulticoreForShare(@NotNull IMulticore multicore,
			@NotNull ILogicDesign logicDesign, @NotNull String failureMsg)
	{
		return ShareConcurrencyHelper.attemptLockOnSourceMulticoreForShare(multicore, logicDesign, failureMsg);
	}

	private void unlockObjects()
	{
		if (m_lockHelper != null) {
			m_lockHelper.unlock();
		}
	}

	protected void reportSharedObjectDeleted(@NotNull ISharedMulticore sharedMulticore)
	{
		MessageHelper.showWarningMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
				ResourceMgr.getString(AbstractShareConductorGroupActionHelper.class,
						"ShareConductorGroupActionHelper.SharedObjectDeleted.Heading"),
				ResourceMgr.getString(AbstractShareConductorGroupActionHelper.class,
						"ShareConductorGroupActionHelper.SharedObjectDeleted.Text", sharedMulticore.getName()));
	}

	private boolean share(@NotNull IMulticore sourceMulticore)
	{
		//LOGIC-10477 - Performance fix
		// Converted below method to multiple stages.
		// Breakdown removes recursion to multiple for loops.
		// We are trying process all the schematic related things once and connectivity related information in other stage
		// Existing one used to invalidate usages for each object iteration
		// With this change it calls only couple of times.
		// Whenever something modified on below method make sure how the usages are being modified such that
		// there will be no unnecessary calls to invalidate them
		Set<IMulticore> multicoresInHierarchy = sourceMulticore.getAllMulticoresInHierarchy();
		//Load all the diagrams where this multicore is placed. This will help in building usages properly at later part.
		loadMulticoreUsageDiagrams(sourceMulticore);
		Map<ILogicObject, ILogicObject> logicObjectWithExistingConnectivityMap = new HashMap<>();
		//Stage 1: Check for existing connectivity , validate hierarchy.
		Set<IConductor> conductorsInHierarchy = sourceMulticore.getAllConductorsInHierarchy(true);
		for (IMulticore multicore : multicoresInHierarchy) {
			ISharedObject sharedObject =
					UIDMgr.getObjectOfType(m_multicoreToSharedHierarchyMap.get(multicore), ISharedObject.class);
			if (sharedObject == null) {
				return false;
			}
			ILogicObject existingConnectivity = getExistingConnectivity(sharedObject);
			if (existingConnectivity != null) {
				logicObjectWithExistingConnectivityMap.put(multicore, existingConnectivity);
			}
			else {
				if (multicore.getParent() == null) {// check hierarchy only for parent, child is done along with that
					boolean meetPreConditions =
							SharedConductorGroupHelper
									.checkMulticoreHierarchy(multicore, m_multicoreToSharedHierarchyMap);
					if (!meetPreConditions) {
						return false;
					}
				}
			}
			for (IConductor conductor : multicore.getConductorsIncludingShields()) {
				ISharedObject sharedConductor =
						UIDMgr.getObjectOfType(m_multicoreToSharedHierarchyMap.get(conductor), ISharedObject.class);
				if (sharedConductor == null) {
					return false;
				}
				ILogicObject existingCondConnectivity = getExistingConnectivity(sharedConductor);
				if (existingCondConnectivity != null) {
					logicObjectWithExistingConnectivityMap.put(conductor, existingCondConnectivity);
				}
			}
		}

		//Stage 2: sync indicators and remove shieldbody hookups
		//this needs usages
		Set<ILogicObject> logicObjectWithExistingConnectivity = logicObjectWithExistingConnectivityMap.keySet();
		for (IMulticore multicore : multicoresInHierarchy) {
			ISharedMulticore sharedObject =
					UIDMgr.getObjectOfType(m_multicoreToSharedHierarchyMap.get(multicore), ISharedMulticore.class);
			ISharedMulticore sharedMulticore = sharedObject;
			if (sharedMulticore == null) {
				return false;
			}
			synIndicatorsAndHookUps(logicObjectWithExistingConnectivityMap, logicObjectWithExistingConnectivity,
					multicore,
					sharedMulticore);
		}

		//Stage 3: Process existing connectivity related
		//this needs usages
		SetMap<ILogicObject, IDiagramObject> connectivityToSchemMap =
				getConnectivityToSchemForExisting(multicoresInHierarchy, logicObjectWithExistingConnectivityMap,
						logicObjectWithExistingConnectivity);

		//Stage 4: Process connectivity related changes which doesnt have existing connectivty
		//this invalidates usages
		Set<ILogicObject> objectsToProcessForParentageFix = new HashSet<>();
		processSharedObjectWithoutExistingConnectivity(multicoresInHierarchy, logicObjectWithExistingConnectivity,
				objectsToProcessForParentageFix);

		//Stage 5: Process collected existing connectivity related data to change on schematic
		//this invalidates usages
		for (ILogicObject logicObject : connectivityToSchemMap.keySet()) {
			Set<IDiagramObject> diagramObjects = connectivityToSchemMap.get(logicObject);
			for (IDiagramObject diagramObject : diagramObjects) {
				reassignConnectivityForSchematic(diagramObject, logicObject);
			}
		}

		for (ILogicObject oldConnectivity : logicObjectWithExistingConnectivityMap.keySet()) {
			ILogicObject newConnectivity = logicObjectWithExistingConnectivityMap.get(oldConnectivity);
			if (newConnectivity instanceof IConductor && oldConnectivity instanceof IConductor) {
				AbstractShareConductorActionHelper.transferConnectivityForConductor((IConductor) oldConnectivity,
						(IConductor) newConnectivity);
			}
		}

		//Stage 6: Fixup multicores and conductors
		//this might invalidates usages
		fixUpMulticoresAndConductors(objectsToProcessForParentageFix);

		//Stage 7: update usages for conductors
		//this needs usages
		for (IConductor conductor : conductorsInHierarchy) {
			SharedConductorHelper.updateUsages(conductor, m_design);
		}

		m_objectsToBeDeleted.addAll(logicObjectWithExistingConnectivity);
		return true;
	}

	private void loadMulticoreUsageDiagrams(@NotNull IMulticore multicore)
	{
		// avoid loading all diagrams, just load those in which the MC is used
		Set<IUID> diagramUIDs = new HashSet<IUID>();
		m_design.getDesignWideUsageMgr().getMulticoreDiagrams(multicore, diagramUIDs);
		for (IUID uid : diagramUIDs) {
			ISchemDiagram diagram = UIDMgr.getObjectOfType(uid, ISchemDiagram.class);
			if (diagram == null) {
				// exception is better than corruption...
				throw new IllegalStateException("Diagram should exist");
			}
			if (!diagram.isFullyLoaded()) {
				diagram.loadToMemory();
			}
		}
	}

	private void processSharedObjectWithoutExistingConnectivity(@NotNull Set<IMulticore> multicoresInHierarchy,
			Set<ILogicObject> logicObjectWithExistingConnectivity, Set<ILogicObject> objectsToProcessForParentageFix)
	{
		for (IMulticore multicore : multicoresInHierarchy) {
			if (logicObjectWithExistingConnectivity.contains(multicore)) {
				for (IConductor conductor : multicore.getConductorsIncludingShields()) {
					if (!logicObjectWithExistingConnectivity.contains(conductor)) {
						ISharedConductor sharedConductor =
								UIDMgr.getObjectOfType(m_multicoreToSharedHierarchyMap.get(conductor),
										ISharedConductor.class);
						assert sharedConductor != null;
						conductor.setMulticore(null);
						conductor.setName(sharedConductor.getName());
						conductor.setSharedConductor(sharedConductor);
						objectsToProcessForParentageFix.add(conductor);
					}
				}
			}
			else {
				ISharedMulticore sharedMulticore =
						UIDMgr.getObjectOfType(m_multicoreToSharedHierarchyMap.get(multicore), ISharedMulticore.class);
				assert sharedMulticore != null;
				multicore.setParent(null);
				multicore.setName(sharedMulticore.getName());
				multicore.setSharedMulticore(sharedMulticore);
				objectsToProcessForParentageFix.add(multicore);
				for (IConductor conductor : multicore.getConductorsIncludingShields()) {
					if (!logicObjectWithExistingConnectivity.contains(conductor)) {
						ISharedConductor sharedConductor =
								UIDMgr.getObjectOfType(m_multicoreToSharedHierarchyMap.get(conductor),
										ISharedConductor.class);
						assert sharedConductor != null;
						conductor.setName(sharedConductor.getName());
						conductor.setSharedConductor(sharedConductor);
					}
				}
			}
		}
	}

	@NotNull private SetMap<ILogicObject, IDiagramObject> getConnectivityToSchemForExisting(
			@NotNull Set<IMulticore> multicoresInHierarchy,
			Map<ILogicObject, ILogicObject> logicObjectWithExistingConnectivityMap,
			Set<ILogicObject> logicObjectWithExistingConnectivity)
	{
		SetMap<ILogicObject, IDiagramObject> connectivityToSchemMap = new SetMap<>();
		for (IMulticore multicore : multicoresInHierarchy) {
			if (logicObjectWithExistingConnectivity.contains(multicore)) {
				List<IDiagramObject> shieldBodyReps = getShieldBodies(m_design, multicore);
				IMulticore targetMulticore = (IMulticore) logicObjectWithExistingConnectivityMap.get(multicore);
				connectivityToSchemMap.addAll(targetMulticore.getShieldBody(), shieldBodyReps);
				for (IConductor conductor : multicore.getConductorsIncludingShields()) {
					if (logicObjectWithExistingConnectivity.contains(conductor)) {
						IConductor targetConductor = (IConductor) logicObjectWithExistingConnectivityMap.get(conductor);
						connectivityToSchemMap.addAll(targetConductor,
								m_design.getDesignWideUsageMgr().getRepresentations(conductor));
					}
				}
			}
		}
		return connectivityToSchemMap;
	}

	private void fixUpMulticoresAndConductors(@NotNull Set<ILogicObject> objectsToProcessForParentageFix)
	{
		for (ILogicObject logicObject : objectsToProcessForParentageFix) {
			if (logicObject instanceof IMulticore) {
				SharedConductorHelper.fixupParentageForMulticore((IMulticore) logicObject, m_design);
			}
			else if (logicObject instanceof IShieldConductor) {
				SharedConductorHelper.fixupParentageForShieldConductor((IShieldConductor) logicObject, m_design);
			}
			else {
				SharedConductorHelper.fixupParentageForConductor((IConductor) logicObject, m_design);
			}
		}
	}

	private void synIndicatorsAndHookUps(Map<ILogicObject, ILogicObject> logicObjectWithExistingConnectivityMap,
			@NotNull Set<ILogicObject> logicObjectWithExistingConnectivity, IMulticore multicore,
			ISharedMulticore sharedMulticore)
	{
		if (logicObjectWithExistingConnectivity.contains(multicore)) {
			IMulticore existingMulticore = (IMulticore) logicObjectWithExistingConnectivityMap.get(multicore);
			SharedConductorGroupHelper
					.changeIndicatorType(multicore, m_design, existingMulticore.getIndicatorType());
		}
		else {
			IStringIterator sharedMulticoreIndicators = sharedMulticore.getIndicators();
			if (sharedMulticoreIndicators.hasNext()) {
				SharedConductorGroupHelper
						.changeIndicatorType(multicore, m_design, sharedMulticoreIndicators.next());
			}
			else {
				sharedMulticore.addIndicator(multicore.getIndicatorType());
			}
		}

		if (!sharedMulticore.hasShield()) {
			SharedConductorGroupHelper.removeShieldHookups(multicore, m_design);
		}
	}

	@NotNull private List<IDiagramObject> getShieldBodies(@NotNull ILogicDesign design, @NotNull IMulticore multicore)
	{
		chs.cof.logical.cable.IShieldBody sb = multicore.getShieldBody();
		// avoid loading all diagrams, just load those in which the MC is used
		Set<IUID> diagramUIDs = new HashSet<IUID>();
		design.getDesignWideUsageMgr().getMulticoreDiagrams(multicore, diagramUIDs);
		List<IDiagramObject> shieldBodies = new ArrayList<>();
		for (IUID uid : diagramUIDs) {
			ISchemDiagram diagram = UIDMgr.getObjectOfType(uid, ISchemDiagram.class);
			if (diagram == null) {
				// exception is better than corruption...
				throw new IllegalStateException("Diagram should exist");
			}
			for (IDiagramObject rep : diagram.getRepresentations(sb.getUID())) {
				shieldBodies.add(rep);
			}
		}
		return shieldBodies;
	}

	@Nullable private ILogicObject getExistingConnectivity(@NotNull ISharedObject sharedObject)
	{
		IConnectivity designConnectivity = m_design.getConnectivity();
		assert designConnectivity != null;
		ILogicObject logicObject = designConnectivity.findLogicObjectForShared(sharedObject);
		return logicObject;
	}

	private void reassignConnectivityForSchematic(@NotNull IDiagramObject diagramObject,
			@NotNull ILogicObject targetConnectivity)
	{
		if (diagramObject instanceof IShieldBody && targetConnectivity instanceof chs.cof.logical.cable.IShieldBody) {
			IShieldBody schemShieldBody = (IShieldBody) diagramObject;
			schemShieldBody.setConnectivity((chs.cof.logical.cable.IShieldBody) targetConnectivity);
		}
		else if (diagramObject instanceof chs.cof.logical.schem.IConductor &&
				targetConnectivity instanceof IConductor) {
			AbstractShareConductorActionHelper
					.reassignConnectivityForSchematicConductor((chs.cof.logical.schem.IConductor) diagramObject,
							(IConductor) targetConnectivity);
		}
	}

	@NotNull
	protected Runnable getConflictResolver(@NotNull ISharedObjectModificationObserver observer)
	{
		assert m_multicore != null;
		ShareIntoFacetConflictResolutionController controller =
				new ShareIntoFacetConflictResolutionController(m_multicore);
		ShareIntoFacetConflictResolutionDialog resolutionDialog = createConflictResolutionDialog(observer, controller);
		return () -> doConflictResolution(controller, resolutionDialog);
	}

	protected void doConflictResolution(@NotNull ShareIntoFacetConflictResolutionController controller,
			@NotNull ShareIntoFacetConflictResolutionDialog resolutionDialog)
	{
		final ISharedMulticore sharedMulticore = UIDMgr.getObjectOfType(m_sharedMulticore, ISharedMulticore.class);
		if (sharedMulticore == null) {
			return;
		}
		IConnectivity connectivity = m_design.getConnectivity();
		if (connectivity == null) {
			return;
		}
		IMulticore newConnectivity = connectivity.findSharedMulticore(sharedMulticore);
		if (newConnectivity == null) {
			return;
		}
		Map<IUID, IUID> sourceToTargetMulticoreMap = getSourceToTargetMulticoreMap(connectivity);
		controller.setupMulticoreMap(sourceToTargetMulticoreMap);
		resolutionDialog.process(newConnectivity);
	}

	@NotNull private Map<IUID, IUID> getSourceToTargetMulticoreMap(@NotNull IConnectivity connectivity)
	{
		Map<IUID, IUID> sourceToTargetMulticoreMap = new HashMap<>();
		for (Map.Entry<ILogicObject, IUID> entry : m_multicoreToSharedHierarchyMap.entrySet()) {
			ILogicObject srcObject = entry.getKey();
			ISharedObject targetSharedObject = UIDMgr.getObjectOfType(entry.getValue(), ISharedObject.class);
			if (srcObject != null && targetSharedObject != null) {
				ILogicObject targetObject = connectivity.findLogicObjectForShared(targetSharedObject);
				if (targetObject != null) {
					sourceToTargetMulticoreMap.put(srcObject.getUID(), targetObject.getUID());
				}
			}
		}
		return sourceToTargetMulticoreMap;
	}

	@NotNull protected ShareIntoFacetConflictResolutionDialog createConflictResolutionDialog(
			@NotNull ISharedObjectModificationObserver observer,
			@NotNull ShareIntoFacetConflictResolutionController controller)
	{
		return new ShareIntoFacetConflictResolutionDialog(observer, controller, controller);
	}

	public void cleanup()
	{
		m_multicore = null;
		m_sharedMulticore = null;
		m_lockHelper = null;
		m_multicoreToSharedHierarchyMap.clear();
		m_objectsToBeDeleted.clear();
		m_multicoreShareContextProvider = null;
	}

	public boolean isNewSharedObject()
	{
		return m_sharedMulticore == null;
	}

	protected abstract boolean isBulkPromotion();

	protected abstract boolean isChangeReportingRequired();

	protected abstract void reportSharedCondMgrLocked(ISharedConductorMgr sharedCondrMgr);

	@Nullable public IMulticoreShareContextProvider getMulticoreShareContextProvider()
	{
		return m_multicoreShareContextProvider;
	}

	@Override @Nullable public IUID getSharedObjectUID()
	{
		return m_sharedMulticore != null ? m_sharedMulticore :
				(m_multicore != null ? m_multicore.getSharedObjectUID() : null);
	}

	@Override public boolean isShareInto()
	{
		assert getMulticoreShareContextProvider() != null;
		ISharedObject shareIntoObject =
				UIDMgr.getObjectOfType(getMulticoreShareContextProvider().getSharedMulticoreUID(), ISharedObject.class);
		return shareIntoObject != null;
	}
}
