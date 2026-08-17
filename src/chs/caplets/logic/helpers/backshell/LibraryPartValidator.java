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
import chs.cof.parts.ILibraryBackshell;
import chs.cof.parts.ILibraryCavity;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Validates library part restrictions before migration.
 * <p>
 * If the target connector already has a backshell with a library part
 * (an {@link ILibraryBackshell}), the total number of source backshell
 * terminations being migrated to that target must not exceed the number of
 * available (free) library cavities.
 * <p>
 * Fails fast on the first violation.
 */
public class LibraryPartValidator implements IValidator
{

	@Override public boolean validate(@NotNull IDeviceConnector sourceDeviceConnector,
			@NotNull IConnector targetPlugConnector, @NotNull BackshellTransferReporter report)
	{

		IBackshell sourceBackshell = sourceDeviceConnector.getBackshell();
		IBackshell targetBackshell = targetPlugConnector.getBackshell();
		if (sourceBackshell == null || targetBackshell == null) {
			//nothing to validate if either is null
			return true;
		}

		Object libraryObject = targetBackshell.getLibraryObject();
		if (!(libraryObject instanceof ILibraryBackshell libraryBackshell)) {
			// Backshell exists but has no library part — no restriction
			return true;
		}

		Set<ILibraryCavity> cavities = libraryBackshell.getCavities();
		// Compute free cavities
		Set<String> libraryCavityNames = new HashSet<>();
		if (cavities != null) {
			for (ILibraryCavity cavity : cavities) {
				libraryCavityNames.add(cavity.getName());
			}
		}

		for (IBackshellTermination termination : sourceBackshell.getBackshellTerminations()) {
			String sourceName = termination.getName();
			if (!libraryCavityNames.contains(sourceName)) {
				String deviceName =
						sourceDeviceConnector.getOwner() != null ? sourceDeviceConnector.getOwner().getName() : "";
				report.addMessage(deviceName, sourceDeviceConnector.getName(), targetPlugConnector.getName(),
						BackshellTransferResult.LibraryBackshellMisMatch);
				return false;
			}
		}

		return true;
	}
}



