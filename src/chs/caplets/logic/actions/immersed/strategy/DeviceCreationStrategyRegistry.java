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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Maintains an ordered list of {@link IDeviceCreationStrategy} instances and
 * resolves the first applicable strategy for a given {@link DeviceCreationContext}.
 * <p>
 * The registry implements the <b>Chain of Responsibility</b> pattern:
 * strategies are evaluated in registration order, and the first one whose
 * {@link IDeviceCreationStrategy#canHandle} returns {@code true} is selected.
 * </p>
 *
 * <h3>Extensibility</h3>
 * <ul>
 *   <li>To add a new strategy, implement {@link IDeviceCreationStrategy} and
 *       register it via {@link #register(IDeviceCreationStrategy)}</li>
 *   <li>The default chain is assembled in {@link #buildDefaultChain()}.</li>
 * </ul>
 */
public class DeviceCreationStrategyRegistry
{

	@NotNull private final List<IDeviceCreationStrategy> m_strategies;

	public DeviceCreationStrategyRegistry()
	{
		m_strategies = new ArrayList<>();
	}

	/**
	 * Appends a strategy to the end of the chain (lowest priority among current entries).
	 *
	 * @param strategy the strategy to add
	 */
	public void register(@NotNull IDeviceCreationStrategy strategy)
	{
		m_strategies.add(strategy);
	}

	/**
	 * Returns an unmodifiable view of the registered strategies (for testing/debugging).
	 *
	 * @return ordered list of strategies
	 */
	@NotNull
	public List<IDeviceCreationStrategy> getStrategies()
	{
		return Collections.unmodifiableList(m_strategies);
	}

	/**
	 * Resolves the first strategy that can handle the given context.
	 *
	 * @param context the device creation context
	 * @return the matching strategy, or {@code null} if none applies
	 */
	@Nullable
	public IDeviceCreationStrategy resolve(@NotNull DeviceCreationContext context)
	{
		for (IDeviceCreationStrategy strategy : m_strategies) {
			if (strategy.canHandle(context)) {
				return strategy;
			}
		}
		return null;
	}

	/**
	 * Builds the default strategy chain in priority order.
	 * <p>
	 * Order:
	 * <ol>
	 *   <li>{@link SharedDeviceStrategy} — Device is shared</li>
	 *   <li>{@link ICDWithLibraryDeviceStrategy} — ICD present with library device</li>
	 *   <li>{@link ICDWithoutLibraryDeviceStrategy} — ICD present, no library device</li>
	 *   <li>{@link LibraryPartDeviceStrategy} — No ICD, part number in library</li>
	 *   <li>{@link NoPinDeviceStrategy} — Catch-all fallback</li>
	 * </ol>
	 *
	 * @return a registry with the default chain configured
	 */
	@NotNull
	public static DeviceCreationStrategyRegistry buildDefaultChain()
	{
		DeviceCreationStrategyRegistry registry = new DeviceCreationStrategyRegistry();
		registry.register(new ExistingDeviceStrategy());
		registry.register(new SharedDeviceStrategy());
		registry.register(new SharedICDDeviceStrategy());
		registry.register(new ICDWithLibraryDeviceStrategy());
		registry.register(new ICDWithoutLibraryDeviceStrategy());
		registry.register(new LibraryPartDeviceStrategy());
		registry.register(new NoPinDeviceStrategy());
		return registry;
	}
}

