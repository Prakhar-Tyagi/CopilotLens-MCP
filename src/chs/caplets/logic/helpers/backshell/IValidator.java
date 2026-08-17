/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */
package chs.caplets.logic.helpers.backshell;

import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDeviceConnector;
import org.jetbrains.annotations.NotNull;

/**
 * Validates a list of migration operations before execution.
 * Implementations should fail fast: return {@code false} on the first failure
 * and record a meaningful message in the {@link BackshellTransferReporter}.
 */
@FunctionalInterface
public interface IValidator
{

	/**
	 * Validates the given migration operations.
	 *
	 * @param operations the operations to validate
	 * @param report     the report to record any failure messages
	 * @return {@code true} if all operations pass validation, {@code false} on the first failure
	 */
	boolean validate(@NotNull IDeviceConnector sourceDeviceConnector, @NotNull IConnector targetPlugConnector,
			@NotNull BackshellTransferReporter report);
}

