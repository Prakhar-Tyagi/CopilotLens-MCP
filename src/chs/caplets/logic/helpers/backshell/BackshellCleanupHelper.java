/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.helpers.backshell;

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IBaseDevice;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.system.FactoryMgr;
import chs.system.IDeleteHelper;
import chs.utilities.CommonUtils;
import chs.utility.helpers.CreationDeletionHelper;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Helper class to clean up BackshellTerminations and empty Backshells after pin transfer from DeviceConnector.
 * <p>
 * Tracks source backshell terminations that had their pins transferred
 * and affected PinLists that need regeneration.
 */
public class BackshellCleanupHelper
{

	private final Map<IBackshell, BackshellTransferContext> m_affectedBackshells;

	public BackshellCleanupHelper()
	{
		m_affectedBackshells = new HashMap<>();
	}

	/**
	 * Tracks a source backshell termination whose schem pin has been transferred
	 * (reused) to the target connector. The termination will be cleaned up if it
	 * has no remaining design-wide usage.
	 */
	public void addAffectedBackshell(@NotNull IBackshell backshell, @NotNull BackshellTransferContext context)
	{
		m_affectedBackshells.put(backshell, context);
	}


	/**
	 * Cleans up source BackshellTerminations and empty Backshells after pin transfer.
	 * 1) Deletes source backshell terminations that have no remaining design-wide usage
	 * 2) Deletes empty backshells (backshells with no remaining terminations)
	 * 3) Regenerates affected PinLists (source devices whose pins were transferred out)
	 */
	public void cleanupBackshellTerminations(BackshellTransferReporter m_reporter)
	{
		if (m_affectedBackshells.isEmpty()) {
			return;
		}

		IDeleteHelper deleteHelper = FactoryMgr.getCAFUtils().getLogicDeleteHelper();
		ILogicDesign logicDesign = m_affectedBackshells.keySet().iterator().next().getLogicDesign();
		if (logicDesign == null) {
			return;
		}

		IDesignWideUsageMgr dwum = logicDesign.getDesignWideUsageMgr();
		Set<IBackshellTermination> terminationsToDelete = new HashSet<>();
		// Step 1: delete all source backshell terminations with no remaining usage
		for (IBackshell mAffectedBackshell : m_affectedBackshells.keySet()) {
			for (IBackshellTermination backshellTermination : mAffectedBackshell.getBackshellTerminations()) {
				if (dwum.getUsages(backshellTermination).isEmpty()) {
					terminationsToDelete.add(backshellTermination);
				}
			}
		}

		// Step 1.5: for each termination being deleted, register a transfer message report
		terminationsToDelete.stream()
				.map(term -> CommonUtils.cast(term.getOwner(), IBackshell.class))
				.filter(Objects::nonNull)
				.distinct()
				.map(m_affectedBackshells::get)
				.filter(Objects::nonNull)
				.forEach(context -> recordBackshellTransferMessages(context, m_reporter));

		// Then delete the termination
		deleteHelper.delete(logicDesign, terminationsToDelete, null);

		// Step 2: for each collected backshell, check if all its terminations are about to be deleted
		Set<IBackshell> backshellsToDelete = new HashSet<>();
		for (IBackshell backshell : m_affectedBackshells.keySet()) {
			boolean allTerminationsDeleted = true;
			for (IBackshellTermination bt : backshell.getBackshellTerminations()) {
				if (!CreationDeletionHelper.getTheCreationHelper().goingToDelete(bt)) {
					allTerminationsDeleted = false;
					break;
				}
			}
			if (allTerminationsDeleted) {
				backshellsToDelete.add(backshell);
			}
		}

		// Delete terminations and empty backshells together
		deleteHelper.delete(logicDesign, backshellsToDelete, null);
		m_affectedBackshells.clear();
	}

	private void recordBackshellTransferMessages(@NotNull BackshellTransferContext context,
			@NotNull BackshellTransferReporter reporter)
	{
		IDeviceConnector sourceDeviceConnector = context.getSourceDeviceConnector();
		IBaseDevice device = sourceDeviceConnector.getOwner();
		IConnector targetConnector = context.getTargetPlugConnector();

		String deviceName = device != null ? device.getName() : "";
		String targetConnectorName = targetConnector.getName();
		String sourceConnectorName = sourceDeviceConnector.getName();
		reporter.addMessage(deviceName, sourceConnectorName, targetConnectorName, BackshellTransferResult.Success);
	}
}

