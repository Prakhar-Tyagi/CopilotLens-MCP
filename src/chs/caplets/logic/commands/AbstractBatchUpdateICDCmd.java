/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023-2025 Siemens
 */

package chs.caplets.logic.commands;

import chs.caf.CAFUtils;
import chs.caf.IOutputWindow;
import chs.caf.OutputWindowWrapper;
import chs.caf.caplet.cmd.IProjectTraverserTransactionHandler;
import chs.caf.caplet.cmd.ProjectTraverserDesignTransactionHandler;
import chs.capitalmanager.appserver.ILockInfo;
import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.logic.actions.UpdateICDAction;
import chs.caplets.logic.actions.UpdateICDActionHelper;
import chs.cof.icd.IDeviceICD;
import chs.cof.icd.IICD;
import chs.cof.logical.IBatchUpdateICDCmd;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.LogicDesignBatchLoadIterator;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.shared.ISharedDevice;
import chs.cof.project.IProject;
import chs.cofUtils.cmd.CHSCommand;
import chs.cofUtils.cmd.CommandHelper;
import chs.common.IUID;
import chs.common.IUIDProvider;
import chs.refresh.LockType;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import chs.utilities.MapMap;
import chs.utilities.ResourceMgr;
import chs.utility.DesignsAccessibilityCheck;
import chs.utility.ICDUtils;
import chs.utility.SharedObjectDomainAccessibliltyChecker;
import chs.utility.helpers.BatchLockRefreshHelper;
import chs.utility.helpers.LockHelper;
import chs.utility.helpers.UtilsHelper;
import chs.utility.ui.HTMLHelper;
import chs.utility.ui.progress.IProgress;
import chs.utility.ui.progress.ProgressGroup;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Common class to update from Dictionary and ICD
 */
public abstract class AbstractBatchUpdateICDCmd<T extends ILogicDesign> extends CHSCommand implements IBatchUpdateICDCmd
{

	protected Set<T> designsToProcess;
	protected ProgressGroup desProgressGroup;
	protected final IOutputWindow outputWindow;
	protected Map<T, String> lockUsersInfo;
	private boolean isBatchUpdateICDAction;
	private MapMap<ILogicDesign, IUID, IDeviceICD> designToICDMap = new MapMap<>();
	private Set<IUID> failedLocks = new HashSet<>();
	private Map<IUID, ISharedDevice> deviceUIDToSharedDeviceMap = new HashMap<>();
	private Set<IDeviceICD> icdsToUpdate = new HashSet<>();

    protected AbstractBatchUpdateICDCmd(CommandHelper commandHelper, Set<T> designs, boolean isBatchUpdateICD)
	{
		super(commandHelper);
		designsToProcess = designs;
		desProgressGroup = new ProgressGroup("");
		desProgressGroup.setRange(designsToProcess.size());
		outputWindow = getOutputWindow();
		lockUsersInfo = new HashMap<>();
		isBatchUpdateICDAction = isBatchUpdateICD;
	}

	@NotNull protected OutputWindowWrapper getOutputWindow()
	{
		return new OutputWindowWrapper(CAFUtils.getInstance().getOutputWindow());
	}

	@Nullable protected IProject getProject(@NotNull Collection<T> designs)
	{
		if (designs.isEmpty()) {
			return null;
		}
		return designs.iterator().next().getProject();
	}

	@NotNull protected Collection<IUID> getDesignUIDs(@NotNull Set<T> designs)
	{
		return designs.stream()
				.map(design -> design.getUID())
				.collect(Collectors.toSet());
	}

	@NotNull protected MapMap<ILogicDesign, IUID, IDeviceICD> getDesignToICDMap()
	{
		return designToICDMap;
	}

	@NotNull protected Map<IDevice, ISharedDevice> getSharedDeviceMap()
	{
		Map<IDevice, ISharedDevice> deviceToSharedDeviceMap = new HashMap<>();
		for (Map.Entry<IUID, ISharedDevice> entry : deviceUIDToSharedDeviceMap.entrySet()) {
			IUID deviceUID = entry.getKey();
			ISharedDevice sharedDevice = entry.getValue();
			IDevice device = CommonUtils.cast(deviceUID.getObject(), IDevice.class);
			if(device != null) {
				deviceToSharedDeviceMap.put(device, sharedDevice);
			}
		}
		return deviceToSharedDeviceMap;
	}

	@NotNull protected Set<IUID> getFailedLocks()
	{
		return failedLocks;
	}

	@NotNull protected Set<IDeviceICD> getICDsToUpdate()
	{
		return icdsToUpdate;
	}

	@Override protected boolean doExecute()
	{
		try {
			Set<T> lockFailedDesignsFirstPass = processDesigns(designsToProcess, false);
			if (!lockFailedDesignsFirstPass.isEmpty()) {
				Set<T> lockFailedDesignsSecondPass = processDesigns(lockFailedDesignsFirstPass, true);
				logLockMessages(lockFailedDesignsSecondPass);
			}
			desProgressGroup.complete();
		}
		catch (UserSessionException ex) {
			ex.printStackTrace();
			return false;
		}

		return true;
	}

	@NotNull Set<T> processDesigns(@NotNull Set<T> designsToWorkOn, boolean isLastPass) throws UserSessionException
	{
		Set<T> lockFailedDesigns;
		Set<IUID> failedDesignLocks = new HashSet<>();
        try {
			Set<T> filteredDesigns = collectDesignsToProcess(designsToWorkOn);
			failedDesignLocks = lockDesigns(filteredDesigns);
			if(isBatchUpdateICDAction) {
                processFilteredDesigns(filteredDesigns);
            }
			lockFailedDesigns = processDesigns(filteredDesigns, failedDesignLocks, isLastPass);
		}
         finally {
			unlockAndUnload(designsToWorkOn, failedDesignLocks);
        }
		return lockFailedDesigns;
	}

	@NotNull private Set<IUID> lockDesigns(@NotNull Set<T> filteredDesigns)
	{
		Set<IUID> failedDesignLocks = UtilsHelper.getPersistenceSession().batchLock(filteredDesigns);
		return failedDesignLocks;
	}

	@NotNull private Set<T> collectDesignsToProcess(@NotNull Set<T> designsToWorkOn)
	{
		Set<IUID> designUIDs = designsToWorkOn.stream().map(IUIDProvider::getUID).collect(Collectors.toSet());
		Set<IUID> accessibleDesigns = SharedObjectDomainAccessibliltyChecker.filterDesignContainersBasedOnAccessibility(designUIDs);
		Set<T> designsToPerformOperationOn = new HashSet<>();
		for (T design : designsToWorkOn) {
			if (!design.isEditable()) {
				String msg = getCannotEditDesignMsg(design);
				outputWindow.sendMessage(msg, getOutputTabName(), true);
				continue;
			}
			if (!accessibleDesigns.contains(design.getUID())) {
				String msg = getInAccessibleSharedObjectsMsg(design);
				outputWindow.sendMessage(msg, getOutputTabName(), true);
				continue;
			}
			designsToPerformOperationOn.add(design);
		}
		return designsToPerformOperationOn;
	}

	private void processFilteredDesigns(@NotNull Set<T> filteredDesigns)
	{
		batchLoadAndRefreshICDs(filteredDesigns);
		batchLockSharedDevices();
	}

	private void batchLoadAndRefreshICDs(@NotNull Set<T> filteredDesigns)
	{
		Set<IICD> icdsToRefresh = new HashSet<>();
		Set<ILogicDesign> filteredLogicDesigns = filteredDesigns.stream()
				.filter(Objects::nonNull)
				.map(ILogicDesign.class::cast)
				.collect(Collectors.toSet());

		collectObjects(filteredLogicDesigns, icdsToRefresh);
		UtilsHelper.getPersistenceSession().batchRefresh(icdsToRefresh);
	}

	private void batchLockSharedDevices()
	{
		Collection<ISharedDevice> objectsToLock = deviceUIDToSharedDeviceMap.values();
		failedLocks = UtilsHelper.getPersistenceSession().batchLock(objectsToLock);
	}

	private void collectDesignInfos(@NotNull T design, @NotNull Map<IUID, IDeviceICD> deviceICDsToProcess)
	{
		UpdateICDActionHelper updateICDActionHelper = new UpdateICDActionHelper(outputWindow);
		for (Map.Entry<IUID, IDeviceICD> entry : deviceICDsToProcess.entrySet()) {
			IUID uid = entry.getKey();
			IDeviceICD icd = entry.getValue();

			IDevice device = Objects.requireNonNull(UIDMgr.getObjectOfType(uid, IDevice.class));
			if (!updateICDActionHelper.isUpdateICDPossible(icd, device)) {
				continue;
			}
			icdsToUpdate.add(icd);
			if (device.isShared()) {
				ISharedDevice sharedDevice = CommonUtils.cast(device.getSharedObject(), ISharedDevice.class);
				if (sharedDevice != null) {
					if (!canEditSharedDevice(device, sharedDevice, design)) {
						continue;
					}
					deviceUIDToSharedDeviceMap.put(uid, sharedDevice);
				}
			}
		}
	}

	@NotNull private Map<IUID, IDeviceICD> collectICDsToBeProcessed(@NotNull ILogicDesign design)
	{
		Map<IUID, IDeviceICD> deviceVsICD = new HashMap<>();
		IConnectivity connectivity = design.getLoadedConnectivity();
		if (connectivity == null) {
			return deviceVsICD;
		}
		for (IDevice device : connectivity.getDevices()) {
			Set<IDeviceICD> matchingICDs = ICDUtils.getMatchingICDs(device);
			if (!matchingICDs.isEmpty()) {
				if (matchingICDs.size() == 1) {
					deviceVsICD.put(device.getUID(), matchingICDs.iterator().next());
				}
				else {
					String msg = ResourceMgr
							.getString(UpdateICDAction.class, "UpdateICDAction.ICDHavingMultipleICDDefinitions.message",
									HTMLHelper.link(design, device));
					outputWindow.sendMessage(msg, getOutputTabName(), true);
				}
			}
		}
		return deviceVsICD;
	}

	@SuppressWarnings("noinspection unchecked")
	private void collectObjects(@NotNull Set<ILogicDesign> filteredDesigns, @NotNull Set<IICD> icdsToRefresh)
	{
		try(LogicDesignBatchLoadIterator iterator = new LogicDesignBatchLoadIterator(filteredDesigns)) {
			while (iterator.hasNext()) {
				ILogicDesign design = iterator.next();
				if (design == null) {
					continue;
				}
				Map<IUID, IDeviceICD> deviceICDsToProcess = collectICDsToBeProcessed(design);
				designToICDMap.put(design, deviceICDsToProcess);
				deviceICDsToProcess.values().stream().forEach(iDeviceICD -> {
					icdsToRefresh.addAll(iDeviceICD.getVariants());
					icdsToRefresh.add(iDeviceICD.getICD());
				});
				collectDesignInfos((T) design, deviceICDsToProcess);
			}
		}
	}

	private boolean canEditSharedDevice(@NotNull IDevice device, @NotNull ISharedDevice sharedDevice,
										@NotNull ILogicDesign design)
	{
		if (sharedDevice.isFrozen() || !sharedDevice.isEditable()) {
			String msg = ResourceMgr.getString(BatchUpdateICDCmd.class, "BatchUpdateICDCmd.SharedDeviceNotEditable",
					HTMLHelper.link(design, device), design.getFullName());
			outputWindow.sendMessage(msg, getOutputTabName(), true);
			return false;
		}
		return true;
	}

	@SuppressWarnings("noinspection unchecked")
	@NotNull private Set<T> processDesigns(@NotNull Set<T> filteredDesigns,
										   @NotNull Set<IUID> failedDesignLocks,
										   boolean isLastPass) throws UserSessionException
	{
		Set<ILogicDesign> filteredLogicDesigns = filteredDesigns.stream()
				.filter(Objects::nonNull)
				.map(ILogicDesign.class::cast)
				.collect(Collectors.toSet());

		Set<T> lockFailedDesigns = new HashSet<>();
		try (LogicDesignBatchLoadIterator iterator = new LogicDesignBatchLoadIterator(filteredLogicDesigns)) {
			while (iterator.hasNext()) {
				ILogicDesign design = iterator.next();
				if (design == null) {
					continue;
				}
				desProgressGroup.add(0, design.getName());
				if (desProgressGroup.isCancelled()) {
					break;
				}
				IUID designUID = design.getUID();
				if (!failedDesignLocks.contains(designUID)) {
					boolean designProcessed;
					try (IProjectTraverserTransactionHandler transactionHandler =
								 isTransactionBoundaryNeededForDesignProcessing() ?
										 new ProjectTraverserDesignTransactionHandler(design, getCommandHelper(),
												 this) : null) {
						designProcessed = processDesign((T) design);
						if (designProcessed) {
							getCommandHelper().saveDesign(design);
							if (transactionHandler != null) {
								transactionHandler.setCommitTransaction(true);
							}
						}
					}
					if (designProcessed) {
						desProgressGroup.increment();
					}
				}
				else {
					lockFailedDesigns.add((T) design);
					if (isLastPass) {
						List<ILockInfo> lockInfos = LockHelper.getWeakLockableLockInfo(design);
						if (lockInfos != null) {
							lockUsersInfo.put((T) design, LockHelper.getLockUserNames(lockInfos));
						}
					}
				}
			}
		}

		return lockFailedDesigns;
	}

	protected void unlockAndUnload(@NotNull Set<T> designsToWorkOn, @NotNull Set<IUID> failedDesignLocks)
	{
		BatchLockRefreshHelper.batchUnlock(designsToWorkOn, LockType.FULL);
		for (T design : designsToWorkOn) {
			design.unloadFromMemory();
		}
	}

	protected boolean isTransactionBoundaryNeededForDesignProcessing()
	{
		return false;
	}

	@Override public boolean doExecuteAllowed()
	{
		IProject project = getProject(designsToProcess);
		if (project != null &&
				DesignsAccessibilityCheck.hasInAccessibleDesignContent(getDesignUIDs(designsToProcess), project)) {
			String msg = getInaccessibleDesignsOutputMessage();
			outputWindow.sendMessage(msg, getOutputTabName(), false);
			return false;
		}
		return true;
	}

	public boolean saveAndCloseOpenDesigns()
	{
		try {
			Set<T> openDesigns = getOpenedDesigns();
			for (T design : openDesigns) {
				saveAndClose(design);
			}
		}
		catch (UserSessionException ex) {
			ex.printStackTrace();
			return false;
		}
		return getOpenedDesigns().isEmpty();
	}

	@NotNull protected Set<T> getOpenedDesigns()
	{
		return designsToProcess.stream()
				.filter(design -> CAFUtils.getInstance().hasDiagramDisplayed(design.getUID()))
				.collect(Collectors.toSet());
	}

	public void saveAndClose(@NotNull T design) throws UserSessionException
	{
		if (design.isLocked()) {
			getCommandHelper().saveDesign(design);
			getCommandHelper().setDesignModifiedFlag(design, false);
		}
		getCommandHelper().closeDesignDiscardingEdits(design);
	}

	protected void logLockMessages(@NotNull Set<T> lockFailedDesigns)
	{
		for (T design : lockFailedDesigns) {
			String fullName = design.getFullName();
			String msg = getCannotLockDesignMsg(fullName);
			String lockedByUser = lockUsersInfo.get(design);
			if (lockedByUser != null) {
				msg = getDesignLockedByOtherUserMsg(fullName, lockedByUser);
			}
			outputWindow.sendMessage(msg, getOutputTabName(), true);
		}
	}

	@NotNull @Override public IProgress getProgress()
	{
		return desProgressGroup;
	}

	@NotNull abstract String getOutputTabName();

	public abstract boolean processDesign(@NotNull T design);

	@NotNull abstract String getInAccessibleSharedObjectsMsg(T design);

	@NotNull abstract String getCannotEditDesignMsg(T design);

	@NotNull abstract String getInaccessibleDesignsOutputMessage();

	@NotNull abstract String getDesignLockedByOtherUserMsg(String fullName, String lockedByUser);

	@NotNull abstract String getCannotLockDesignMsg(String fullName);
}
