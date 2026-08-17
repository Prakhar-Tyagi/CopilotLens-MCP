/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */
package chs.caplets.logic.helpers.backshell;

import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.shared.ISharedBackshell;
import chs.cof.logical.shared.ISharedBackshellTermination;
import chs.cof.logical.shared.ISharedBackshellTerminationIterator;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPinList;
import chs.utilities.CommonUtils;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import chs.utility.logic.SharedObjectAvailabilityChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Validates shared-object editability and locking before migration.
 * <p>
 * When the target connector is shared, modifying its backshell or adding
 * terminations requires the shared object to be both editable and locked.
 * This validator:
 * <ul>
 *   <li>Checks editability of the shared connector/backshell.</li>
 *   <li>Attempts to lock shared objects that need editing (backshell creation or termination creation).</li>
 *   <li>Records locked objects in {@link BackshellTransferReporter} so callers can unlock them later.</li>
 * </ul>
 * Fails fast on the first violation.
 */
public class SharedObjectValidator implements IValidator
{

	@Override
	public boolean validate(@NotNull IDeviceConnector sourceDeviceConnector,
			@NotNull IConnector targetPlugConnector, @NotNull BackshellTransferReporter reporter)
	{

		ISharedConnector targetSharedConnector =
				CommonUtils.cast(targetPlugConnector.getSharedPinList(), ISharedConnector.class);
		IBackshell sourceBacksehll = sourceDeviceConnector.getBackshell();
		if (sourceBacksehll == null || targetSharedConnector == null) {
			return true;
		}

		IBackshell targetBackshell = targetPlugConnector.getBackshell();
		ISharedBackshell targetSharedBackshell = getSharedBackshell(targetBackshell, targetSharedConnector);

		// Determine if we need to create a new backshell on the shared connector
		boolean needsNewBackshell = (targetSharedBackshell == null);

		// Determine which source terminations need a new shared termination
		Set<String> existingSharedTermNames = collectSharedTerminationNames(targetSharedBackshell);
		boolean needsNewTermination = false;
		for (IBackshellTermination termination : sourceBacksehll.getBackshellTerminations()) {
			String sourceName = termination.getName();
			if (!existingSharedTermNames.contains(sourceName)) {
				needsNewTermination = true;
				break;
			}
		}

		// If nothing new is being created on the shared side, no lock is needed
		if (!needsNewBackshell && !needsNewTermination) {
			return true;
		}

		reporter.setSharedConnectorLockNeeded(true);

		// Validate editability — the shared connector must not be frozen or read-only
		return validateEditable(sourceDeviceConnector, targetPlugConnector, targetSharedConnector, reporter);
	}

	/**
	 * Validates that a shared object is editable (not frozen, not read-only).
	 */
	private boolean validateEditable(@NotNull IDeviceConnector sourceDeviceconnector,
			@NotNull IConnector targetPlugConnector, @NotNull ISharedObject sharedObject,
			@NotNull BackshellTransferReporter report)
	{
		if (sharedObject.isFrozen()) {
			report.addMessage(getOwnerName(sourceDeviceconnector), sourceDeviceconnector.getName(),
					targetPlugConnector.getName(), BackshellTransferResult.SharedConnectorFrozen);
			return false;
		}
		if (!new SharedObjectAvailabilityChecker().check(sharedObject, targetPlugConnector.getLogicDesign(),
				ISharedObjectAvailabilityReporter.NULL_REPORTER, false)) {
			report.addMessage(getOwnerName(sourceDeviceconnector), sourceDeviceconnector.getName(),
					targetPlugConnector.getName(), BackshellTransferResult.SharedConnectorNotEditable);
			return false;
		}
		return true;
	}

	@NotNull private String getOwnerName(@NotNull IDeviceConnector sourceDeviceConnector)
	{
		return sourceDeviceConnector.getOwner() != null ? sourceDeviceConnector.getOwner().getName() : "";
	}

	/**
	 * Retrieves the shared backshell from an existing backshell instance, if any.
	 */
	@Nullable
	private ISharedBackshell getSharedBackshell(@Nullable IBackshell backshell,
			@NotNull ISharedConnector targetSharedConnector)
	{
		if (backshell == null) {
			return targetSharedConnector.getBackshell();
		}
		ISharedPinList sharedPinList = backshell.getSharedPinList();
		return sharedPinList instanceof ISharedBackshell ? (ISharedBackshell) sharedPinList : null;
	}

	/**
	 * Collects the names of all terminations on a shared backshell.
	 */
	@NotNull
	private Set<String> collectSharedTerminationNames(@Nullable ISharedBackshell sharedBackshell)
	{
		Set<String> names = new HashSet<>();
		if (sharedBackshell == null) {
			return names;
		}
		ISharedBackshellTerminationIterator terminations = sharedBackshell.getBackshellTerminations();
		if (terminations != null) {
			while (terminations.hasNext()) {
				ISharedBackshellTermination termination = terminations.getNext();
				names.add(termination.getName());
			}
		}
		return names;
	}
}



