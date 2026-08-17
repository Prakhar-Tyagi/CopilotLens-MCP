/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2020-2025 Siemens
 */

package chs.caplets.logic.commands;

import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caplets.logic.actions.BatchUpdateICDAction;
import chs.caplets.logic.actions.UpdateICDActionHelper;
import chs.caplets.logic.actions.ui.BatchUpdateICDSingleEndedChoice;
import chs.caplets.logic.icd.ICDPlacementHelper;
import chs.caplets.logic.icd.UpdateICDPersistenceHandler;
import chs.cof.icd.IDeviceICD;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.ISharedDevice;
import chs.cofUtils.cmd.CommandHelper;
import chs.common.IUID;
import chs.common.IUIDProvider;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.IAuditTrailLogger;
import chs.utilities.MapMap;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.Choice;
import chs.utilities.ui.messaging.Question;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.audit.AuditableEventType;
import chs.utility.helpers.UtilsHelper;
import chs.utility.logic.IndicatorRefresherUtils;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Command to update ICDs present in a set of designs
 */
public class BatchUpdateICDCmd extends AbstractBatchUpdateICDCmd<ILogicDesign>
{

	private UpdateICDActionHelper updateICDActionHelper;
	private boolean generateSingleEnded;
	private List<String> m_storeEventObjectIds = new ArrayList<>();
	private Collection<ISharedDevice> updatedSharedDevices = new HashSet<>();

	public BatchUpdateICDCmd(CommandHelper commandHelper, Set<ILogicDesign> designs)
	{
		super(commandHelper, designs, true);
		updateICDActionHelper = new UpdateICDActionHelper(outputWindow);
	}

	protected boolean isTransactionBoundaryNeededForDesignProcessing()
	{
		//[CH] java.lang.IllegalStateException: Attempting to work with DB without being in a transaction boundary
		//This command invokes GHC also. GHC needs transaction boundary for shared devices.
		return true;
	}

	public boolean prepare()
	{
		ResourceBasedMessageContent content =
				new ResourceBasedMessageContent(BatchUpdateICDCmd.class, "BatchUpdateICDCmd.CannotUndo");
		final Choice saveAndContinue = new Choice(getClass(), "BatchUpdateICDCmd.CannotUndo.choice.saveAndContinue");
		final Choice cancel = new Choice(getClass(), "BatchUpdateICDCmd.CannotUndo.choice.cancel");
		Choice selectedChoice = Question.show(content, saveAndContinue, cancel);

		return selectedChoice != cancel;
	}

	@Override protected boolean doExecute()
	{
		generateSingleEnded = isSingleEndedGenerationNeeded();
		desProgressGroup.start();
		return super.doExecute();
	}

	protected boolean isSingleEndedGenerationNeeded()
	{
		BatchUpdateICDSingleEndedChoice choice = createICDSingleEndedChoice();
		return choice.isSingleEndedGenerationNeeded();
	}

	@NotNull protected BatchUpdateICDSingleEndedChoice createICDSingleEndedChoice()
	{
		return new BatchUpdateICDSingleEndedChoice(getProject(designsToProcess));
	}

	public boolean processDesign(@NotNull ILogicDesign design)
	{
		String message =
				ResourceMgr.getString(BatchUpdateICDCmd.class, "BatchUpdateICDCmd.BeginUpdate", design.getFullName());
		outputWindow.sendMessage(message, getOutputTabName(), true);

		Map<IDevice, IDeviceICD> icdsToProcess = getICDsToProcess(design);
		if (icdsToProcess.isEmpty()) {
			String msg = ResourceMgr.getString(BatchUpdateICDCmd.class, "BatchUpdateICDCmd.NoICDsToProcess",
					design.getFullName());
			outputWindow.sendMessage(msg, getOutputTabName(), true);
			return false;
		}

		Set<IUID> failedLocks = getFailedLocks();
		Map<IDevice, ISharedDevice> sharedDeviceMap = getSharedDeviceMap();
		IAuditTrailLogger auditLogger = FactoryMgr.getSystemFactory().getCHSSystem().getAuditLogger();
		Set<IDeviceICD> icdsToUpdate = getICDsToUpdate();

		for(IDevice device : icdsToProcess.keySet()) {
			IDeviceICD icd = icdsToProcess.get(device);
			if (!icdsToUpdate.contains(icd)) {
				continue;
			}

			boolean isDeviceShared = device.isShared();
			if (isDeviceShared) {
				ISharedDevice sharedDevice = sharedDeviceMap.get(device);
				if (sharedDevice == null) {
					continue;
				}

				if (isLockFailurePresentForThisSharedObject(failedLocks, sharedDevice)) {
					String msg = ResourceMgr.getString(getClass(), "BatchUpdateICDCmd.SharedDeviceLocked",
							HTMLHelper.link(design, device), design.getFullName());
					outputWindow.sendMessage(msg, getOutputTabName(), true);
				}
				else {
					performUpdateICDOnDevice(design, device, icd);
					storeAuditTrailEventForSharedObjectUpdate(auditLogger, sharedDevice);
					LockUpdateHelper.flushAndUnlockSharedObject(sharedDevice);
					updatedSharedDevices.add(sharedDevice);
				}
			}
			else {
				performUpdateICDOnDevice(design, device, icd);
			}
		}

		auditLogger.postStoredEvents(m_storeEventObjectIds);
		Set<IUID> candidateDiagrams =
				design.getDiagrams().stream().map(IUIDProvider::getUID).collect(Collectors.toSet());
		IndicatorRefresherUtils.refreshOutOfDateMulticoreIndicators(design, candidateDiagrams);

		// LOGIC-12225 java.lang.ClassCastException when Deleting the Shield Fetched using Update devices from ICD action
		ConductorRouteAction.getInstance().processAction(true, true);

		return true;
	}

	/**
	 *  For checking the failure of a shared object, we are checking the following:
	 *
	 *  failedLocks.contains(sharedDeviceUID) - if the shared device UID is present in the set of failed locks.
	 *
	 *  sharedDevice.isLocked() - if the shared device is already locked or not. Ideally it should be locked as part of
	 *  pre-batch lock operation.
	 *
	 *  updatedSharedDevices.contains(sharedDevice) - if the shared device is present in the set of updated
	 *  shared devices.
	 *
	 * @param failedLocks - set of failed locks, during pre-batch lock operation
	 * @param sharedDevice - shared device object for which we are trying to check the lock failure
	 * @return true if there is lock failure, false otherwise
	 */
	private boolean isLockFailurePresentForThisSharedObject(@NotNull Set<IUID> failedLocks,
															@NotNull ISharedDevice sharedDevice)
	{
		if(updatedSharedDevices.contains(sharedDevice)) {
			// If the shared device is already updated, then we don't need to check for lock failure.
			// Lock the shared device again, so that it can be modified during update icd.
			LockUpdateHelper.obtainLockOnSharedObject(sharedDevice, false);
			return false;
		}
		boolean isLockFailed = failedLocks.contains(sharedDevice.getUID()) || !sharedDevice.isLocked();
		return isLockFailed;
	}

	@NotNull protected Map<IDevice, IDeviceICD> getICDsToProcess(@NotNull ILogicDesign design)
	{
		MapMap<ILogicDesign, IUID, IDeviceICD> designToICDMap = getDesignToICDMap();
		Map<IUID, IDeviceICD> deviceUIDToICDMap = designToICDMap.get(design);

		Map<IDevice, IDeviceICD> icdsToProcess = new HashMap<>();
		for(Map.Entry<IUID, IDeviceICD> entry : deviceUIDToICDMap.entrySet() ) {
			IUID uid = entry.getKey();
			IDeviceICD deviceICD = entry.getValue();
			IDevice device = CommonUtils.cast(uid.getObject(), IDevice.class);
			if(device != null) {
				icdsToProcess.put(device, deviceICD);
			}
		}
        return icdsToProcess;
	}

	private void storeAuditTrailEventForSharedObjectUpdate(@NotNull IAuditTrailLogger auditLogger,
														   @NotNull ISharedDevice sharedDevice)
	{
		auditLogger.storeEvent(AuditableEventType.SHARED_OBJECT_MODIFIED,
				ResourceMgr.getString(BatchUpdateICDAction.class, "BatchUpdateICDAction.name"),
				sharedDevice.getProject().getUID().toString(), sharedDevice.getFullName(),
				sharedDevice.getUID().toString(), UtilsHelper.getServerTime());
		m_storeEventObjectIds.add(sharedDevice.getUID().getString());
	}

	public void performUpdateICDOnDevice(ILogicDesign design, IDevice device, IDeviceICD icd)
	{
		IDesignWideUsageMgr designWideUsageMgr = design.getDesignWideUsageMgr();
		List<IDesignSharedUsage> usages = designWideUsageMgr.getUsages(device);
		if (usages.isEmpty()) {
			ICDPlacementHelper.ensureICDRefPropOnDevice(icd, device);
		}
		boolean logMessage = true;
		for (IDesignSharedUsage usage : usages) {
			ISchemDiagram diagram = design.getDiagram(usage.getDiagramUID());
			assert diagram != null;
			diagram.loadToMemory();
			IPinList schemDevice = CommonUtils.cast(usage.getDiagramObject(), IPinList.class);
			if (schemDevice == null) {
				continue;
			}

			UpdateICDPersistenceHandler persistenceHandler = new UpdateICDPersistenceHandler(diagram, generateSingleEnded);
			updateICDActionHelper.performUpdateICDOnDevice(icd, device, schemDevice, diagram, persistenceHandler,
					new HashSet<>(),
					logMessage);
			persistenceHandler.endRoutingAll();
			logMessage = false;
		}
	}

	@NotNull protected String getOutputTabName()
	{
		return UpdateICDActionHelper.getOutputTabName();
	}

	@NotNull protected String getInaccessibleDesignsOutputMessage()
	{
		return ResourceMgr.getString(BatchUpdateICDCmd.class, "BatchUpdateICDCmd.InaccessibleDesign");
	}

	@NotNull protected String getDesignLockedByOtherUserMsg(String fullName, String lockedByUser)
	{
		return ResourceMgr.getString(BatchUpdateICDCmd.class, "BatchUpdateICDCmd.DesignLockedByOtherUser", fullName,
				lockedByUser);
	}

	@NotNull protected String getCannotLockDesignMsg(String fullName)
	{
		return ResourceMgr.getString(BatchUpdateICDCmd.class, "BatchUpdateICDCmd.CannotLockDesign", fullName);
	}

	@NotNull protected String getInAccessibleSharedObjectsMsg(ILogicDesign design)
	{
		return ResourceMgr.getString(BatchUpdateICDCmd.class, "BatchUpdateICDCmd.DesignHasInaccessibleSharedObjects",
				design.getFullName());
	}

	@NotNull protected String getCannotEditDesignMsg(ILogicDesign design)
	{
		return ResourceMgr.getString(BatchUpdateICDCmd.class, "BatchUpdateICDCmd.CannotEditDesign",
				design.getFullName());
	}
}
