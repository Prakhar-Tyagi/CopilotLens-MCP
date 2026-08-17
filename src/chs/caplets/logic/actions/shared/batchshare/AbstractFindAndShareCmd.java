/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2020-2026 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.caf.CAFUtils;
import chs.capitalmanager.appserver.ILockInfo;
import chs.caplets.logic.actions.shared.autoshare.AutoShareParams;
import chs.caplets.logic.actions.ui.DelegateFacetDataProvider;
import chs.caplets.logic.actions.ui.FacetConflictNode;
import chs.caplets.logic.commands.BulkAutoShareCmd;
import chs.caplets.logic.commands.BulkAutoShareIntoCmd;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IGroundDevice;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.footprint.IUserDeviceFootprint;
import chs.cof.logical.footprint.IUserFootprintConnector;
import chs.cof.logical.footprint.IUserFootprintMapping;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IProjectSharedUsageView;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedDevice;
import chs.cof.logical.shared.ISharedDeviceConnector;
import chs.cof.logical.shared.ISharedFullyLoadedPinListMgr;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedSplice;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.logical.shared.SharedPinListHelper;
import chs.cof.project.IProject;
import chs.cofUtils.cmd.CHSCommand;
import chs.cog.ICOGLockable;
import chs.common.DesignUtils;
import chs.common.IAttributePropertyProvider;
import chs.common.IDesignAbstraction;
import chs.common.IDesignContainer;
import chs.common.ILockable;
import chs.common.INamedPropertiedObject;
import chs.common.IProjectPreferenceMgr;
import chs.common.IRevisionedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.UIDUtils;
import chs.subsystem.immersedapp.IImmersedViewService;
import chs.subsystem.immersedapp.ImmersedAppServices;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.AlphaNumComparator;
import chs.utilities.CommonUtils;
import chs.utilities.IXMLTags;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utilities.StringUtils;
import chs.utilities.stream.StreamUtils;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.DiagramHelper;
import chs.utility.IMessageContext;
import chs.utility.IMessageReporterWithContext;
import chs.utility.SharedObjectAbstractionMatcher;
import chs.utility.SharedObjectDomainAccessibliltyChecker;
import chs.utility.attr.AttributeUtils;
import chs.utility.helpers.IFootprintMergeHandler;
import chs.utility.helpers.LockHelper;
import chs.utility.helpers.UserFootprintUtils;
import chs.utility.helpers.UtilsHelper;
import chs.utility.ui.LockInfoDialog;
import chs.utility.ui.progress.IProgress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Abstract class for find and share logical objects
 */
public abstract class AbstractFindAndShareCmd
{

	@NotNull protected IProject m_project;
	@NotNull protected Set<ILogicDesign> m_designs;
	@NotNull protected Set<ILockable> m_lockedObjects;
	@NotNull protected IMessageReporterWithContext m_reporter;
	@NotNull protected Collection<IBatchShareStatusMessage> m_feedbackMessages;

	protected AbstractFindAndShareCmd(@NotNull IProject project, @NotNull Set<ILogicDesign> designs)
	{
		m_project = project;
		m_designs = new HashSet<>(designs);
		m_lockedObjects = new HashSet<>();
		m_feedbackMessages = new ArrayList<>();
		m_reporter = new BatchShareReporter(m_feedbackMessages::add);
	}

	public void execute()
	{
		try {
			if (isExecutionAllowed()) {
				doExecute();
			}
		}
		finally {
			releaseAcquiredLocks();
			Collection<IBatchShareStatusMessage> feedbackMessages = getFeedbackMessages();
			if (!feedbackMessages.isEmpty()) {
				displayBatchShareStatusTab(feedbackMessages);
			}
		}
	}

	@NotNull protected Collection<IBatchShareStatusMessage> getFeedbackMessages()
	{
		return m_feedbackMessages.stream().sorted((msg1, msg2) -> {
			int result = Integer.compare(msg1.getStatus().getSortIndex(), msg2.getStatus().getSortIndex());
			if (result == 0) {
				result = AlphaNumComparator.compare(msg1.getObjectDetailText(), msg2.getObjectDetailText(), false);
			}
			if (result == 0) {
				result = AlphaNumComparator.compare(msg1.getDesignName(), msg2.getDesignName(), false);
			}
			if (result == 0) {
				result = AlphaNumComparator.compare(msg1.getMessage(), msg2.getMessage(), false);
			}
			return result;
		}).collect(Collectors.toList());
	}

	protected boolean acquireLocks()
	{
		boolean lockSuccess = lockObjects(m_designs);
		lockSuccess = lockObjects(Set.of(m_project.getSharedPinListMgr(), m_project.getSharedConductorMgr())) &&
				lockSuccess;
		return lockSuccess;
	}

	protected boolean isExecutionAllowed()
	{
		Set<ILogicDesign> openCandidateDesigns = getOpenDesigns(m_designs);
		return openCandidateDesigns.isEmpty() && checkDesignAccessibility(m_designs, m_project) &&
				checkDesignsEditability(m_designs) && acquireLocks();
	}

	protected boolean checkDesignAccessibility(Set<ILogicDesign> designs, IProject project)
	{
		return true;
	}

	private boolean checkDesignsEditability(@NotNull Set<ILogicDesign> designs)
	{

		Set<IUID> designUIDs =
				designs.stream().map(designContainer -> designContainer.getUID()).collect(Collectors.toSet());
		Set<IUID> accessibleSet =
				SharedObjectDomainAccessibliltyChecker.filterDesignContainersBasedOnAccessibility(designUIDs);

		boolean allDesignsEditable = true;
		for (ILogicDesign design : designs) {
			if (!design.isEditable() || !accessibleSet.contains(design.getUID())) {
				allDesignsEditable = false;
				m_reporter.report(PromptSeverity.ERROR, ResourceMgr.getString(AbstractFindAndShareCmd.class,
						"AbstractFindAndShareCmd.NonEditableDesign.msg", design.getFullName()));
			}
		}
		return allDesignsEditable;
	}

	protected void releaseAcquiredLocks()
	{
		unlockObjects(m_lockedObjects);
	}

	protected abstract void doExecute();

	@NotNull protected Set<IShareableObjectGroup> findShareableObjectGroups(
			@NotNull Set<IEntityShareCriteria> entitiesShareCriteria, @NotNull IProgress progress)
	{
		if (progress.isCancelled()) {
			return Collections.emptySet();
		}
		progress.increment(
				ResourceMgr.getString(AbstractFindAndShareCmd.class, "AbstractFindAndShareCmd.ChildProgress.find"));
		Set<ShareableEntityTypeEnum> sharedObjectTypes =
				entitiesShareCriteria.stream().map(IEntityShareCriteria::getEntityType).collect(Collectors.toSet());
		Collection<ISharedObject> sharedObjectsInScope = getSharedObjectsInScope(sharedObjectTypes);
		Set<IShareableObjectGroup> shareableObjectGroups =
				getShareableObjectsFinder(m_project, m_designs, sharedObjectsInScope,
						getObjectInfoProvider()).collectShareableObjectGroups(entitiesShareCriteria);
		shareableObjectGroups.removeIf(group -> group.getTargetSharedObjects().isEmpty() &&
				group.getShareableObjectsNum() < 2);
		return shareableObjectGroups;
	}

	@NotNull protected AbstractShareableObjectsFinder getShareableObjectsFinder(@NotNull IProject project, @NotNull Set<ILogicDesign> designs,
			@NotNull Collection<ISharedObject> sharedObjectsInScope, @NotNull IObjectInfoProvider objectInfoProvider)
	{
		return new ShareableObjectsFinder(project, designs,
				sharedObjectsInScope, objectInfoProvider);
	}

	@NotNull protected IObjectInfoProvider getObjectInfoProvider()
	{
		return new ObjectInfoProvider();
	}

	@NotNull private Set<ILogicDesign> getOpenDesigns(@NotNull Set<ILogicDesign> designs)
	{
		return designs.stream()
				.filter(design -> !CAFUtils.getInstance().getViewsForDesignContainer(design).isEmpty())
				.collect(Collectors.toSet());
	}

	protected void displayBatchShareStatusTab(@NotNull Collection<IBatchShareStatusMessage> reportedMessages)
	{
		BatchShareStatusWindowAssistant statusWindow = new BatchShareStatusWindowAssistant(
				ResourceMgr.getString(AbstractFindAndShareCmd.class, "AbstractFindAndShareCmd.StatusTab.title"),
				BatchShareFeedbackTableColumnEnum.SEVERITY.toString());
		statusWindow.addStatusMessages(reportedMessages);
	}

	protected void share(@NotNull Set<IShareableObjectGroup> shareableObjectGroups,
			@NotNull IMessageReporterWithContext reporter, @NotNull IProgress progress)
	{
		Map<IObjectInfo, IShareableObjectGroup> candidateGroupsForShare = shareableObjectGroups.stream()
				.filter(shareableObjectGroup -> shareableObjectGroup.getTargetSharedObjects().isEmpty())
				.collect(Collectors.toMap(IShareableObjectGroup::getRepresentativeObjectInfo, Function.identity()));

		Map<String, Set<IObjectInfo>> candidateObjectsInfosByDesignForShare = candidateGroupsForShare.keySet().stream()
				.collect(Collectors.groupingBy(IObjectInfo::getDesignUID, Collectors.toSet()));
		progress.setRange(candidateObjectsInfosByDesignForShare.size() + 1);
		progress.increment(StringUtils.EMPTY_STRING);
		//Batch load all the designs.
		m_project.getDesignMgr()
				.getDesigns(UIDUtils.convertStringIdsToUIDSet(candidateObjectsInfosByDesignForShare.keySet()));
		for (String designUID : candidateObjectsInfosByDesignForShare.keySet()) {
			ILogicDesign design = DesignUtils.getDesign(designUID, ILogicDesign.class);
			if (design == null) {
				continue;
			}
			if (progress.isCancelled()) {
				return;
			}
			String designFullName = design.getFullName();
			progress.increment(ResourceMgr.getString(AbstractFindAndShareCmd.class,
					"AbstractFindAndShareCmd.ChildProgress.share", designFullName));
			boolean isDesignLoadedHere = false;
			if (!design.isLoadedInMemory()) {
				design.loadToMemory();
				isDesignLoadedHere = true;
			}
			Set<IObjectInfo> candidateObjectInfos = candidateObjectsInfosByDesignForShare.get(designUID);
			Set<ILogicObject> objectsToBeShared = candidateObjectInfos.stream()
					.map(objectInfo -> UIDMgr.getObjectOfType(objectInfo.getUID(), ILogicObject.class))
					.filter(StreamUtils::notNull).collect(Collectors.toSet());
			doShare(design, objectsToBeShared, reporter);
			for (IObjectInfo objectInfo : candidateObjectInfos) {
				ILogicObject mappedObj = UIDMgr.getObjectOfType(objectInfo.getUID(), ILogicObject.class);
				if (mappedObj == null) {
					continue;
				}
				IShareableObjectGroup shareableObjectGroup = candidateGroupsForShare.get(objectInfo);
				if (shareableObjectGroup != null) {
					shareableObjectGroups.remove(shareableObjectGroup);
					if (!mappedObj.isShared()) {
						for (IObjectInfo shareableObjectInfo : shareableObjectGroup.getShareableObjectInfos()) {
							String anchorDisplayName =
									StringUtils.emptyIfBlank(objectInfo.getAttributeValue(IXMLTags.NAME));
							if (shareableObjectInfo == objectInfo) {
								reporter.report(PromptSeverity.ERROR,
										ResourceMgr.getString(AbstractFindAndShareCmd.class,
												"AbstractFindAndShareCmd.ShareFailure.msg", anchorDisplayName),
										IMessageContext.createContext(shareableObjectInfo));
							}
							else {
								String displayName =
										StringUtils.emptyIfBlank(shareableObjectInfo.getAttributeValue(IXMLTags.NAME));
								reporter.report(PromptSeverity.ERROR,
										ResourceMgr.getString(AbstractFindAndShareCmd.class,
												"AbstractFindAndShareCmd.ShareSkipDueToAnchorShareFailure.msg",
												displayName, anchorDisplayName),
										IMessageContext.createContext(shareableObjectInfo));
							}
						}
					}
					else if (shareableObjectGroup.getShareableObjectsNum() > 1) {
						IShareableObjectGroup newGroup = new ShareableObjectGroup(
								shareableObjectGroup.getShareableObjectInfos().stream()
										.filter(objInfo -> objInfo != objectInfo)
										.collect(Collectors.toSet()));
						newGroup.setCandidateTargetSharedObjects(Collections.singleton(mappedObj.getSharedObject()));
						shareableObjectGroups.add(newGroup);
					}
				}
			}

			if (isDesignLoadedHere) {
				design.unloadFromMemory();
			}
		}
	}

	protected void doShare(@NotNull ILogicDesign design, @NotNull Set<ILogicObject> candidateObjectsToBeShared,
			@NotNull IMessageReporterWithContext reporter)
	{
		SetMap<ISchemDiagram, IUIDObject> objectsToBeShared = new SetMap<>();
		SetMap<String, String> processedObjectNamesOfType = new SetMap<>();
		for (ILogicObject shareableObject : candidateObjectsToBeShared) {
			String logicKey = getLogicKey(shareableObject);
			if (processedObjectNamesOfType.contains(logicKey, shareableObject.getName())) {
				reporter.report(PromptSeverity.ERROR, ResourceMgr.getString(AbstractFindAndShareCmd.class,
								"AbstractFindAndShareCmd.MultipleObjectsWithSameName.msg", shareableObject.getName()),
						IMessageContext.createContext(shareableObject));
				continue;
			}
			if (shareableObject instanceof IGenericInlineConnector && isInlinePinNotMated(
					(IGenericInlineConnector) shareableObject)) {
				continue;
			}
			processedObjectNamesOfType.add(logicKey, shareableObject.getName());
			IDiagramObjectIterator it = design.getRepresentations(
					shareableObject instanceof IMulticore ? ((IMulticore) shareableObject).getShieldBody().getUID() :
							shareableObject.getUID());
			if (it.hasNext()) {
				IDiagramObject schemObject = it.next();
				ISchemDiagram diagram = DiagramHelper.getDiagram(schemObject);
				if (diagram != null) {
					objectsToBeShared.add(diagram, schemObject);
				}
			}
			else {
				//default map used in setmap allows null keys
				//noinspection ConstantConditions
				objectsToBeShared.add(null, shareableObject);
			}
		}
		if (objectsToBeShared.isEmpty()) {
			return;
		}
		AutoShareParams params = getAutoShareParams();
		CHSCommand cmd = getAutoShareCmd(design, reporter, objectsToBeShared, params);
		cmd.execute();
	}

	private boolean isInlinePinNotMated(@NotNull IGenericInlineConnector inlineConnector)
	{
		IConnector mate = inlineConnector.getMates().iterator().next();
		Set<IAbstractPin> pinsWithMissingMate = new HashSet<>();
		for (IAbstractPin pin : inlineConnector.getPins()) {
			if (pin.getConnectedPin(mate) == null) {
				pinsWithMissingMate.add(pin);
			}
		}
		if (pinsWithMissingMate.isEmpty()) {
			for (IAbstractPin pin : mate.getPins()) {
				if (pin.getConnectedPin(inlineConnector) == null) {
					pinsWithMissingMate.add(pin);
				}
			}
		}
		return !pinsWithMissingMate.isEmpty();
	}

	@NotNull private String getLogicKey(@NotNull ILogicObject logicObject)
	{
		String classKey = logicObject.getClass().toString();
		return logicObject instanceof IPinList ? classKey + PinListTypeEnum.from_connectivity((IPinList) logicObject) :
				classKey;
	}

	@NotNull protected AutoShareParams getAutoShareParams()
	{
		AutoShareParams params = new AutoShareParams(doShareUnplacedObjects(), false, true)
		{
			@Override public boolean usePinReservationPreferenceSetting()
			{
				return true;
			}
		};

		return params;
	}

	@NotNull
	protected CHSCommand getAutoShareCmd(@NotNull ILogicDesign design, @NotNull IMessageReporterWithContext reporter,
			@NotNull SetMap<ISchemDiagram, IUIDObject> objectsToBeShared, @NotNull AutoShareParams params)
	{
		return new BulkAutoShareCmd(objectsToBeShared, design, reporter, true, params);
	}

	protected abstract boolean doShareUnplacedObjects();

	protected void shareInto(@NotNull Set<IShareableObjectGroup> shareableObjectGroups,
			IMessageReporterWithContext reporter, @NotNull IProgress progress)
	{
		IProjectSharedUsageView prjSharedUsageView = m_project.getSharedUsageView();
		prjSharedUsageView.createDesignScopeHolder(false);
		discardIrrelevantShareableGroups(shareableObjectGroups, m_reporter);
		Map<IObjectInfo, ISharedObject> candidateObjInfoToTargetSharedObj = new HashMap<>();
		for (IShareableObjectGroup shareableObjectGroup : shareableObjectGroups) {
			if (shareableObjectGroup.getTargetSharedObjects().size() == 1) {
				ISharedObject targetSharedObject = shareableObjectGroup.getTargetSharedObjects().iterator().next();
				shareableObjectGroup.getShareableObjectInfos()
						.forEach(objInfo -> candidateObjInfoToTargetSharedObj.put(objInfo, targetSharedObject));
			}
		}

		Map<String, Set<IObjectInfo>> candidateObjInfosByDesignForShareInto = candidateObjInfoToTargetSharedObj.keySet()
				.stream().collect(Collectors.groupingBy(IObjectInfo::getDesignUID, Collectors.toSet()));
		progress.setRange(candidateObjInfosByDesignForShareInto.size() + 1);
		progress.increment(StringUtils.EMPTY_STRING);
		//Batch load all the designs.
		m_project.getDesignMgr()
				.getDesigns(UIDUtils.convertStringIdsToUIDSet(candidateObjInfosByDesignForShareInto.keySet()));
		for (String designUID : candidateObjInfosByDesignForShareInto.keySet()) {
			ILogicDesign design = DesignUtils.getDesign(designUID, ILogicDesign.class);
			if (design == null) {
				continue;
			}
			if (progress.isCancelled()) {
				return;
			}
			String designFullName = design.getFullName();
			progress.increment(ResourceMgr
					.getString(AbstractFindAndShareCmd.class, "AbstractFindAndShareCmd.ChildProgress.shareInto",
							designFullName));
			boolean isDesignLoadedHere = false;
			if (!design.isLoadedInMemory()) {
				design.loadToMemory();
				isDesignLoadedHere = true;
			}
			Map<IUIDObject, ISharedObject> objectsToBeSharedInto = new HashMap<>();

			Set<IObjectInfo> candidateObjectInfos = candidateObjInfosByDesignForShareInto.get(designUID);
			Map<IObjectInfo, ILogicObject> candidateObjInfoToRealObj = new HashMap<>();
			for (IObjectInfo objectInfo : candidateObjectInfos) {
				ILogicObject mappedObj = UIDMgr.getObjectOfType(objectInfo.getUID(), ILogicObject.class);
				if (mappedObj != null) {
					candidateObjInfoToRealObj.put(objectInfo, mappedObj);
					ISharedObject targetSharedObject = candidateObjInfoToTargetSharedObj.get(objectInfo);
					objectsToBeSharedInto.put(mappedObj, targetSharedObject);
				}
			}
			doShareInto(design, objectsToBeSharedInto, reporter);
			for (IObjectInfo objectInfo : candidateObjectInfos) {
				ILogicObject mappedObj = candidateObjInfoToRealObj.get(objectInfo);
				ISharedObject targetSharedObject = objectsToBeSharedInto.get(mappedObj);
				if (mappedObj == null || targetSharedObject == null) {
					continue;
				}
				String displayName = objectInfo.getAttributeValue(IXMLTags.NAME);
				String targetDisplayName = getSharedObjectDisplayName(targetSharedObject);
				if (!mappedObj.isShared() && !mappedObj.isDeletedObject()) {
					reporter.report(PromptSeverity.ERROR, ResourceMgr.getString(AbstractFindAndShareCmd.class,
									"AbstractFindAndShareCmd.ShareIntoFailure.msg", displayName, targetDisplayName),
							IMessageContext.createContext(mappedObj));
				}
				else {
					ILogicObject targetConnObject = null;
					IConnectivity connectivity = design.getConnectivity();
					if (connectivity != null) {
						targetConnObject = connectivity.findLogicObjectForShared(targetSharedObject);
					}
					reporter.report(PromptSeverity.INFORMATION,
							getMessage(displayName, targetDisplayName, targetSharedObject),
							IMessageContext.createContext(targetConnObject));
				}
			}
			if (isDesignLoadedHere) {
				design.unloadFromMemory();
			}
		}
		prjSharedUsageView.resetDesignScopeHolder();
	}

	@NotNull private String getMessage(@Nullable String displayName, @NotNull String targetDisplayName,
			ISharedObject targetSharedObject)
	{
		IDesignAbstraction designAbstraction = SharedObjectAbstractionMatcher.getDesignAbstraction(targetSharedObject);
		if (designAbstraction != null) {
			return ResourceMgr.getString(AbstractFindAndShareCmd.class,
					"AbstractFindAndShareCmd.ShareIntoSuccess.withAbstraction.msg", displayName, targetDisplayName,
					designAbstraction.getName());
		}
		return ResourceMgr.getString(AbstractFindAndShareCmd.class,
				"AbstractFindAndShareCmd.ShareIntoSuccess.msg", displayName, targetDisplayName);
	}
	@NotNull protected ISharedMulticoreMappingChecker getMulticoreChecker()
	{
		return new DefaultSharedMulticoreMappingChecker();
	}
	public void doShareInto(@NotNull ILogicDesign design,
			@NotNull Map<IUIDObject, ISharedObject> candidateObjectsToBeSharedInto,
			@NotNull IMessageReporterWithContext reporter)
	{
		Map<IUIDObject, ISharedObject> objectsToBeSharedInto = new HashMap<>();
		for (IUIDObject object : candidateObjectsToBeSharedInto.keySet()) {
			ISharedObject targetSharedObject = candidateObjectsToBeSharedInto.get(object);
			if (object instanceof IMulticore) {
				Map<IUIDObject, ISharedObject> multicoreToSharedMap = new HashMap<>();
				boolean matched = MulticoreMapperUtils.mapMulticoreToSharedMulticore((IMulticore) object,
						(ISharedMulticore) targetSharedObject, multicoreToSharedMap, getMulticoreChecker());
				if (matched) {
					boolean valid = true;
					for (IUIDObject multicoreObject : multicoreToSharedMap.keySet()) {
						if (multicoreObject instanceof INamedPropertiedObject) {
							INamedPropertiedObject namedPropertiedObject = (INamedPropertiedObject) multicoreObject;
							ISharedObject sharedObject = multicoreToSharedMap.get(multicoreObject);
							if (hasConflict(namedPropertiedObject, sharedObject)) {
								valid = false;
								String sharedObjectDisplayName = getSharedObjectDisplayName(sharedObject);
								reporter.report(PromptSeverity.ERROR,
										ResourceMgr.getString(AbstractFindAndShareCmd.class,
												"AbstractFindAndShareCmd.ConflictWhileSharingInto.msg",
												namedPropertiedObject.getName(), sharedObjectDisplayName),
										IMessageContext.createContext(object));
							}
						}
					}
					if (valid) {
						objectsToBeSharedInto.putAll(multicoreToSharedMap);
					}
				}
				else {
					reporter.report(PromptSeverity.ERROR,
							ResourceMgr.getString(AbstractFindAndShareCmd.class,
									"AbstractFindAndShareCmd.StructureMismatchWithTargetSharedObject.msg"),
							IMessageContext.createContext(object));
				}
			}
			else if (object instanceof IPinList && targetSharedObject instanceof ISharedPinList) {
				boolean isValid = !checkAndReportConflictsForPinlist((IPinList) object,
						(ISharedPinList) targetSharedObject, reporter);
				if (object instanceof IGenericInlineConnector && targetSharedObject instanceof ISharedConnector) {
					IGenericInlineConnector srcInlineConnector = (IGenericInlineConnector) object;
					ISharedConnector targetSharedInlineConnector = (ISharedConnector) targetSharedObject;
					IConnector srcMatedConnector = srcInlineConnector.getMates().iterator().next();
					ISharedConnector matedSharedConnector = targetSharedInlineConnector.getMates().iterator().next();
					isValid = !checkAndReportConflictsForPinlist(srcMatedConnector, matedSharedConnector, reporter) &&
							isValid;
					if (isValid) {
						isValid = !isInlinePinNotMated((IGenericInlineConnector) object);
					}
				}
				if (object instanceof IDevice && targetSharedObject instanceof ISharedDevice) {
					isValid = !checkAndReportDeviceSideConnectorConflict((IDevice) object,
							(ISharedDevice) targetSharedObject, reporter) && isValid;
				}
				if (isValid) {
					objectsToBeSharedInto.put(object, targetSharedObject);
				}
			}
			else if (object instanceof INamedPropertiedObject) {
				boolean isValid = !checkAndReportConflictsForNamedObject((INamedPropertiedObject) object,
						targetSharedObject, reporter);
				if (isValid) {
					objectsToBeSharedInto.put(object, targetSharedObject);
				}
			}
		}
		if (objectsToBeSharedInto.isEmpty()) {
			return;
		}
		ISchemDiagram diagram = design.getDiagrams(true).stream().findFirst().orElse(null);
		AutoShareParams params = getAutoShareIntoParams();
		params.setPreShareTask(this::makeSharedObjectCompatible);
		CHSCommand cmd = getAutoShareIntoCmd(design, reporter, objectsToBeSharedInto, diagram, params);
		cmd.execute();
	}

	@NotNull protected AutoShareParams getAutoShareIntoParams()
	{
		return new AutoShareParams(doShareUnplacedObjects(), false, true);
	}

	protected boolean checkAndReportConflictsForNamedObject(@NotNull INamedPropertiedObject srcObject,
			@NotNull ISharedObject targetSharedObject, @NotNull IMessageReporterWithContext reporter)
	{
		boolean hasConflict = false;
		if (hasConflict(srcObject, targetSharedObject)) {
			hasConflict = true;
			String sharedObjectDisplayName = getSharedObjectDisplayName(targetSharedObject);
			reporter.report(PromptSeverity.ERROR, ResourceMgr.getString(AbstractFindAndShareCmd.class,
					"AbstractFindAndShareCmd.ConflictWhileSharingInto.msg",
					srcObject.getName(), sharedObjectDisplayName), IMessageContext.createContext(srcObject));
		}
		return hasConflict;
	}

	private boolean checkAndReportConflictsForPinlist(@NotNull IPinList srcPinlist,
			@NotNull ISharedPinList targetSharedPinlist, @NotNull IMessageReporterWithContext reporter)
	{
		boolean hasConflict = checkAndReportConflictsForNamedObject(srcPinlist, targetSharedPinlist, reporter);
		Map<String, ISharedPin> sharedPinsByName = targetSharedPinlist.getPins().stream()
				.collect(Collectors.toMap(ISharedPin::getName, Function.identity()));
		IAbstractPinIterator srcPins = srcPinlist.getPins();
		while (srcPins.hasNext()) {
			IAbstractPin srcPin = srcPins.next();
			ISharedPin targetPin = sharedPinsByName.get(srcPin.getName());
			if (targetPin != null && hasConflict(srcPin, targetPin)) {
				hasConflict = true;
				String sharedObjectDisplayName = getSharedObjectDisplayName(targetSharedPinlist);
				reporter.report(PromptSeverity.ERROR, ResourceMgr.getString(AbstractFindAndShareCmd.class,
						"AbstractFindAndShareCmd.PinConflictWhileSharingInto.msg", srcPinlist.getName(),
						sharedObjectDisplayName, srcPin.getName()), IMessageContext.createContext(srcPinlist));
			}
		}
		return hasConflict;
	}

	private boolean checkAndReportDeviceSideConnectorConflict(@NotNull IDevice srcDevice,
			@NotNull ISharedDevice targetSharedDevice, @NotNull IMessageReporterWithContext reporter)
	{
		Map<String, IDeviceConnector> srcDeviceConnectors = srcDevice.getDeviceConnectors().stream()
				.collect(Collectors.toMap(INamedPropertiedObject::getName, Function.identity()));
		Map<String, ISharedDeviceConnector> targetDeviceConnectors = targetSharedDevice.getDeviceConnectors().stream()
				.collect(Collectors.toMap(INamedPropertiedObject::getName, Function.identity()));
		final Map<IDeviceConnector, ISharedDeviceConnector> srcToTargetDeviceConnectorMapping =
				new HashMap<>(srcDeviceConnectors.size());
		IFootprintMergeHandler mergeHandler = new IFootprintMergeHandler()
		{
			@Override public void exactMatch(@NotNull IUserFootprintConnector srcConnector,
					@NotNull IUserFootprintConnector tgtConnector)
			{
				IDeviceConnector srcDevConn = srcDeviceConnectors.get(srcConnector.getName());
				ISharedDeviceConnector targetMatch = targetDeviceConnectors.get(tgtConnector.getName());
				if (srcDevConn != null && targetMatch != null) {
					srcToTargetDeviceConnectorMapping.put(srcDevConn, targetMatch);
				}
			}

			@Override
			public void moveToTarget(@NotNull IUserFootprintMapping srcRow, @NotNull String connRename,
					@Nullable MergeFeedback feedback)
			{
			}

			@Override public void mergeWithTarget(@NotNull IUserFootprintMapping srcMapping,
					@NotNull IUserFootprintConnector targetConnector)
			{
				exactMatch(srcMapping.getConnector(), targetConnector);
			}
		};
		IUserDeviceFootprint deviceFootprint = srcDevice.getDeviceFootprint();
		IUserDeviceFootprint sharedDeviceFootprint = targetSharedDevice.getDeviceFootprint();
		boolean mergeDeviceConnectors = Optional.ofNullable(m_project.getPreferences())
				.map(IProjectPreferenceMgr::isMergeDeviceConnectorsEnabled)
				.orElse(false);
		UserFootprintUtils.processFootprintMerge(srcDevice.getName(), deviceFootprint, sharedDeviceFootprint,
				Function.identity(), mergeHandler, mergeDeviceConnectors);
		boolean hasConflict = false;
		for (IDeviceConnector srcDevConnector : srcToTargetDeviceConnectorMapping.keySet()) {
			ISharedDeviceConnector targetDevConnector = srcToTargetDeviceConnectorMapping.get(srcDevConnector);
			if (targetDevConnector != null && hasConflict(srcDevConnector, targetDevConnector)) {
				hasConflict = true;
				String sharedObjectDisplayName = getSharedObjectDisplayName(targetSharedDevice);
				reporter.report(PromptSeverity.ERROR, ResourceMgr.getString(AbstractFindAndShareCmd.class,
								"AbstractFindAndShareCmd.DeviceConnectorConflictWhileSharingInto.msg", srcDevice.getName(),
								sharedObjectDisplayName, srcDevConnector.getName()),
						IMessageContext.createContext(srcDevice));
			}
		}
		return hasConflict;
	}

	@NotNull protected CHSCommand getAutoShareIntoCmd(@NotNull ILogicDesign design,
			@NotNull IMessageReporterWithContext reporter,
			@NotNull Map<IUIDObject, ISharedObject> objectsToBeSharedInto, @Nullable ISchemDiagram diagram,
			@NotNull AutoShareParams params)
	{
		return new BulkAutoShareIntoCmd(objectsToBeSharedInto, design, diagram, reporter, true, true, params);
	}

	private void makeSharedObjectCompatible(@NotNull IUIDObject candidateObject,
			@NotNull ISharedObject sharedObject)
	{
		IPinList sourcePinlist = CommonUtils.cast(candidateObject, IPinList.class);
		ISharedPinList targetSharedPinlist = CommonUtils.cast(sharedObject, ISharedPinList.class);
		if (sourcePinlist != null && targetSharedPinlist != null && targetSharedPinlist.isLocked()) {
			makeSharedPinlistCompatible(sourcePinlist, targetSharedPinlist);
		}
	}

	private void makeSharedPinlistCompatible(@NotNull IPinList pinList, @NotNull ISharedPinList sharedPinList)
	{
		IDesignContainer designContainer = pinList.getDesign();
		if (designContainer == null) { // not sure how this could happen
			return;
		}

		assert designContainer instanceof IDesign;
		IDesign design = (IDesign) designContainer;

		if (pinList instanceof ISplice || pinList instanceof IGroundDevice ||
				(pinList instanceof IConnector && ((IConnector) pinList).isRingTerminal())) {
			return;
		}
		Collection<String> sharedPinNames =
				sharedPinList.getPins().stream().map(ISharedPin::getName).collect(Collectors.toSet());
		for (IAbstractPin pin : pinList.getPins()) {
			if (!sharedPinNames.contains(pin.getName())) {
				ISharedPin sharedPin = addSharedPin(sharedPinList, pin, design);
				if (sharedPinList instanceof ISharedConnector &&
						((ISharedConnector) sharedPinList).isInlineHalf()) {
					ISharedConnector matedInlineConnector =
							((ISharedConnector) sharedPinList).getMates().stream()
									.filter(ISharedConnector::isInlineHalf)
									.findFirst().orElse(null);
					if (matedInlineConnector != null) {
						IAbstractPin connectedPin = pin.getConnectedPins().stream().findAny().orElse(null);
						ISharedPin mateSharedPin = addSharedPin(matedInlineConnector, connectedPin, design);
						sharedPin.setMatePin(mateSharedPin);
					}
				}
			}
		}
	}

	@NotNull private ISharedPin addSharedPin(@NotNull ISharedPinList sharedPinList,
			@Nullable IAttributePropertyProvider sourcePin, @NotNull IDesign design)
	{
		ISharedPin sharedPin = FactoryMgr.getSharedFactory()
				.createSharedPinForOwner(FactoryMgr.createUID(), sharedPinList);
		if (sourcePin != null) {
			AttributeUtils.copyAttributes(sourcePin, sharedPin, Collections.emptySet());
			SharedPinListHelper.copyProperties(sourcePin, sharedPin);
		}
		setupPinReservation(sharedPin, sharedPinList, design);
		sharedPinList.addPin(sharedPin);
		return sharedPin;
	}

	private void setupPinReservation(@NotNull ISharedPin sharedPin, @NotNull ISharedPinList sharedPinList,
			@NotNull IDesign design)
	{
		ISharedPin.ReservationType applicableReservation = getApplicableReservation(sharedPinList);
		sharedPin.setReservationType(applicableReservation);
		sharedPin.addDesign(design);
	}

	@NotNull protected ISharedPin.ReservationType getApplicableReservation(@NotNull ISharedPinList sharedPinList)
	{
		if (sharedPinList instanceof ISharedSplice) {
			return ISharedPin.ReservationType.Unrestricted;
		}

		IProject project = sharedPinList.getProject();
		if (project != null) {
			return project.getPreferences().getBatchSharePinReservationType();
		}
		return ISharedPin.ReservationType.Unrestricted;
	}

	@NotNull protected String getSharedObjectDisplayName(@NotNull ISharedObject sharedObject)
	{
		String type = sharedObject.getObjectTypeForDisplay();
		String name = sharedObject instanceof IRevisionedObject ?
				((IRevisionedObject) sharedObject).getFullName() :
				sharedObject.getName();
		String displayName = type != null && name != null ? type + " " + name : StringUtils.EMPTY_STRING;
		return displayName;
	}

	private boolean hasConflict(@NotNull INamedPropertiedObject namedPropertiedObject,
			@NotNull ISharedObject sharedObject)
	{
		FacetConflictNode conflictNode =
				new FacetConflictNode(new DelegateFacetDataProvider(namedPropertiedObject),
						new DelegateFacetDataProvider(sharedObject));
		conflictNode.computeConflicts(false);
		return conflictNode.hasConflicts();
	}
	@NotNull
	protected Collection<ISharedObject> getSharedObjectsInScope(@NotNull Set<ShareableEntityTypeEnum> typesToBeConsidered)
	{
		ISharedFullyLoadedPinListMgr sharedPinListMgr = (ISharedFullyLoadedPinListMgr)m_project.getSharedPinListMgr();
		sharedPinListMgr.refresh();
		ISharedConductorMgr sharedConductorMgr = m_project.getSharedConductorMgr();
		sharedConductorMgr.refresh();
		Collection<ISharedObject> sharedObjectsInScope = new HashSet<>();

		for (ShareableEntityTypeEnum type : typesToBeConsidered) {
			if (type.equals(ShareableEntityTypeEnum.DEVICE)) {
				sharedObjectsInScope
						.addAll(sharedPinListMgr.getEditableSharedPinLists(PinListTypeEnum.TypeDevice).stream().collect(
								Collectors.toSet()));
			}
			else if (type.equals(ShareableEntityTypeEnum.GROUND)) {
				sharedObjectsInScope
						.addAll(sharedPinListMgr.getEditableSharedPinLists(PinListTypeEnum.TypeGround).stream().collect(
								Collectors.toSet()));
			}
			else if (type.equals(ShareableEntityTypeEnum.SPLICE)) {
				sharedObjectsInScope
						.addAll(sharedPinListMgr.getEditableSharedPinLists(PinListTypeEnum.TypeSplice).stream().collect(
								Collectors.toSet()));
			}
			else if (type.equals(ShareableEntityTypeEnum.PLUG)) {
				sharedObjectsInScope
						.addAll(sharedPinListMgr.getEditableSharedPinLists(PinListTypeEnum.TypePlug).stream().collect(
								Collectors.toSet()));
			}
			else if (type.equals(ShareableEntityTypeEnum.JACK)) {
				sharedObjectsInScope
						.addAll(sharedPinListMgr.getEditableSharedPinLists(PinListTypeEnum.TypeJack).stream().collect(
								Collectors.toSet()));
			}
			else if (type.equals(ShareableEntityTypeEnum.INLINE)) {
				sharedObjectsInScope
						.addAll(sharedPinListMgr.getEditableSharedPinLists(PinListTypeEnum.TypeInlineJack).stream()
								.collect(
										Collectors.toSet()));
			}
			else if (type.equals(ShareableEntityTypeEnum.RING_TERMINAL)) {
				sharedObjectsInScope
						.addAll(sharedPinListMgr.getEditableSharedPinLists(PinListTypeEnum.TypeRingTerminal).stream()
								.collect(
										Collectors.toSet()));
			}
			else if (type.equals(ShareableEntityTypeEnum.WIRE)) {
				sharedObjectsInScope
						.addAll(sharedConductorMgr.getEditableLogicSharedConductors().stream()
								.filter(ISharedConductor::isWire)
								.collect(
										Collectors.toSet()));
			}
			else if (type.equals(ShareableEntityTypeEnum.NET)) {
				sharedObjectsInScope
						.addAll(sharedConductorMgr.getEditableLogicSharedConductors().stream()
								.filter(ISharedConductor::isNet)
								.collect(
										Collectors.toSet()));
			}
			else if (type.equals(ShareableEntityTypeEnum.MULTICORE)) {
				sharedObjectsInScope
						.addAll(sharedConductorMgr.getEditableSharedMulticores().stream().collect(Collectors.toSet()));
			}
			else if (type.equals(ShareableEntityTypeEnum.OVERBRAID)) {
				sharedObjectsInScope
						.addAll(sharedConductorMgr.getEditableSharedOverbraids().stream().collect(Collectors.toSet()));
			}
			else if (type.equals(ShareableEntityTypeEnum.HIGHWAY)) {
				sharedObjectsInScope
						.addAll(sharedConductorMgr.getSharedGeneralHighways().stream().collect(Collectors.toSet()));
			}
			else if (type.equals(ShareableEntityTypeEnum.SINGLE_LINE)) {
				sharedObjectsInScope
						.addAll(sharedConductorMgr.getEditableSharedSingleLines().stream().collect(Collectors.toSet()));
			}
			else {
				assert false : "Shared Objects scope not defined for type : " + type;
			}
		}
		return sharedObjectsInScope;
	}

	private void discardIrrelevantShareableGroups(@NotNull Set<IShareableObjectGroup> shareableObjectGroups,
			@NotNull IMessageReporterWithContext reporter)
	{
		Set<IShareableObjectGroup> groupsToBeDiscarded = new HashSet<>();
		for (IShareableObjectGroup shareableObjectGroup : shareableObjectGroups) {
			Set<ISharedObject> targetSharedObjects = shareableObjectGroup.getTargetSharedObjects();
			ShareabilityStatus shareabilityStatus = shareableObjectGroup.validate();
			if (shareabilityStatus == ShareabilityStatus.MULTIPLE_TARGET_SHARED_OBJECTS) {
				shareableObjectGroup.getShareableObjectInfos()
						.forEach(shareableObject -> reporter.report(PromptSeverity.ERROR,
								ResourceMgr.getString(AbstractFindAndShareCmd.class,
										"AbstractFindAndShareCmd.MultipleTargetSharedObjects.msg"),
								IMessageContext.createContext(shareableObject)));
				groupsToBeDiscarded.add(shareableObjectGroup);
			}
			else if (shareabilityStatus == ShareabilityStatus.MULTIPLE_TARGET_SHARED_OBJECT_REVISIONS) {
				shareableObjectGroup.getShareableObjectInfos()
						.forEach(shareableObject -> reporter.report(PromptSeverity.ERROR,
								ResourceMgr.getString(AbstractFindAndShareCmd.class,
										"AbstractFindAndShareCmd.MultipleTargetSharedObjectRevisions.msg"),
								IMessageContext.createContext(shareableObject)));
				groupsToBeDiscarded.add(shareableObjectGroup);
			}
			else if (shareabilityStatus == ShareabilityStatus.FROZEN_TARGET_SHARED_OBJECT) {
				String sharedObjectDisplayName = getSharedObjectDisplayName(targetSharedObjects.iterator().next());
				shareableObjectGroup.getShareableObjectInfos()
						.forEach(shareableObject -> reporter.report(PromptSeverity.ERROR,
								ResourceMgr.getString(AbstractFindAndShareCmd.class,
										"AbstractFindAndShareCmd.FrozenTargetSharedObject.msg",
										sharedObjectDisplayName),
								IMessageContext.createContext(shareableObject)));
				groupsToBeDiscarded.add(shareableObjectGroup);
			}
		}
		shareableObjectGroups.removeAll(groupsToBeDiscarded);
	}

	protected boolean lockObjects(@NotNull Set<? extends ILockable> objectsToBeLocked)
	{
		Set<ILockable> objectsNotLocked =
				objectsToBeLocked.stream().filter(object -> !object.isLocked()).collect(Collectors.toSet());

		//batch lock cog objects
		Set<ICOGLockable> cogObjectsToBeLocked =
				objectsToBeLocked.stream().filter(ICOGLockable.class::isInstance).map(ICOGLockable.class::cast)
						.collect(Collectors.toSet());
		UtilsHelper.getPersistenceSession().batchLock(cogObjectsToBeLocked);

		//lock remaining
		objectsToBeLocked.stream().filter(obj -> !ICOGLockable.class.isInstance(obj)).forEach(ILockable::lock);

		boolean lockSuccess = true;
		for (ILockable lockable : objectsNotLocked) {
			if (lockable.isLocked()) {
				m_lockedObjects.add(lockable);
			}
			else {
				lockSuccess = false;
				reportLockFailure(lockable);
			}
		}
		return lockSuccess;
	}

	private void reportLockFailure(@NotNull ILockable lockable)
	{
		ILockInfo lockInfo = LockHelper.getLockInfo(lockable);
		if (lockInfo != null) {
			String displayName = LockInfoDialog.getCategoryAndNameForDisplay(lockable).getSecond();
			if (lockInfo.isLocked()) {
				m_reporter.report(PromptSeverity.ERROR, LockInfoDialog.getLockDetailMessage(lockInfo, displayName));
			}
			else {
				m_reporter.report(PromptSeverity.ERROR, ResourceMgr.getString(AbstractFindAndShareCmd.class,
						"AbstractFindAndShareCmd.InsufficientPriviliges.msg", displayName));
			}
		}
	}

	private void unlockObjects(@NotNull Set<ILockable> lockedObjects)
	{
		//batch unlock cog objects
		Set<ICOGLockable> cogObjectsToBeUnlocked =
				lockedObjects.stream().filter(ICOGLockable.class::isInstance).map(ICOGLockable.class::cast)
						.collect(Collectors.toSet());
		UtilsHelper.getPersistenceSession().batchUnlock(cogObjectsToBeUnlocked);

		//unlock remaining
		lockedObjects.stream().filter(obj -> !ICOGLockable.class.isInstance(obj)).forEach(ILockable::unlock);
	}
}
