/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions.immersed.strategy;

import org.jetbrains.annotations.NotNull;

/**
 * A strategy in the device creation chain of responsibility.
 * <p>
 * Each implementation encapsulates a single, well-defined rule for determining
 * whether it can handle a device creation request, and the logic to execute it.
 * Strategies are evaluated in priority order; the first one that
 * {@linkplain #canHandle(DeviceCreationContext) applies} wins.
 * </p>
 *
 * <h3>Adding a new strategy</h3>
 * <ol>
 *   <li>Create a class implementing {@code IDeviceCreationStrategy}.</li>
 *   <li>Implement {@link #canHandle} with the applicability predicate.</li>
 *   <li>Implement {@link #execute} with the action wiring.</li>
 *   <li>Register the new strategy in
 *       {@link DeviceCreationStrategyRegistry#buildDefaultChain()} at the
 *       appropriate priority position.</li>
 * </ol>
 */
public interface IDeviceCreationStrategy
{
	/**
	 * Determines whether this strategy is applicable for the given context.
	 *
	 * @param context the resolved device creation context
	 * @return {@code true} if this strategy should handle the request
	 */
	boolean canHandle(@NotNull DeviceCreationContext context);

	/**
	 * Executes the device creation action for the given context.
	 * <p>
	 * Callers must invoke {@link #canHandle} first and only call this method
	 * when it returns {@code true}.
	 * </p>
	 *
	 * @param context the resolved device creation context
	 */
	void execute(@NotNull DeviceCreationContext context);
}

