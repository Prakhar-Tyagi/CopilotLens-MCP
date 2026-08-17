/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.shared.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.helpers.UndoableContainerIdler;
import chs.cof.draw.IGrid;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IPinPlaceholder;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.utility.helpers.ModularSchemPinListInfo;
import chs.utility.helpers.PinPlaceholderProviderForSymbolledDeviceInMove;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Helper class to create and remove temporary pin placeholders for devices with symbols,
 * used for pin-related operations like move pin.
 * The main purpose of this class is to ensure that temporary placeholders are created for devices with symbols
 * during the operations and removed afterwards, without storing any undo state for these temporary objects.
 */
public class SymDeviceTemporaryPlaceHolderCreationHelper
{

	@NotNull private final Function<IPinList, IPinList> deviceWithSymbolFunction;
	@NotNull private final Predicate<IPinList> ignorePlaceholderCreation;

	public SymDeviceTemporaryPlaceHolderCreationHelper(@NotNull Function<IPinList, IPinList> deviceWithSymbolFunction)
	{
		this(deviceWithSymbolFunction, ignored -> false);
	}

	public SymDeviceTemporaryPlaceHolderCreationHelper(@NotNull Function<IPinList, IPinList> deviceWithSymbolFunction,
			@NotNull Predicate<IPinList> ignorePlaceholderCreation)
	{
		this.deviceWithSymbolFunction = deviceWithSymbolFunction;
		this.ignorePlaceholderCreation = ignorePlaceholderCreation;
	}

	@Nullable
	protected IPinList getDeviceWithSymbol(@NotNull IPinList candidate)
	{
		return deviceWithSymbolFunction.apply(candidate);
	}

	@NotNull private Set<IPinList> getDeviceWithSymbols(@NotNull ModularSchemPinListInfo modularSchemPinListInfo)
	{
		return modularSchemPinListInfo.getCandidates()
				.stream()
				.map(this::getDeviceWithSymbol)
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	public void addTempPlaceHolderForDevicesWithSymbols(IPinList pinList, IGrid grid,
			GeneratorParameters generatorParams)
	{
		IPinList devWithSymbol = getDeviceWithSymbol(pinList);
		if (devWithSymbol == null || ignorePlaceholderCreation.test(devWithSymbol)) {
			return;
		}
		doProcessTempPlaceHolderForDevicesWithSymbols(
				devWithSymbol,
				createAddTempPlaceholdersForDevWithSymHandler(grid, generatorParams));
	}

	public void addTempPlaceHolderForDevicesWithSymbols(@NotNull ModularSchemPinListInfo modularSchemPinListInfo,
			IGrid grid, GeneratorParameters generatorParams)
	{
		doProcessTempPlaceHolderForDevicesWithSymbols(
				modularSchemPinListInfo,
				createAddTempPlaceholdersForDevWithSymHandler(grid, generatorParams));
	}

	public void removeTempPlaceHoldersForDevicesWithSymbols(@NotNull IPinList pinList)
	{
		IPinList devWithSymbol = getDeviceWithSymbol(pinList);
		if (devWithSymbol == null || ignorePlaceholderCreation.test(devWithSymbol)) {
			return;
		}
		doProcessTempPlaceHolderForDevicesWithSymbols(
				devWithSymbol,
				createRemoveTempPlaceholdersForDevWithSymHandler());
	}

	public void removeTempPlaceHoldersForDevicesWithSymbols(@NotNull ModularSchemPinListInfo modularSchemPinListInfo)
	{
		doProcessTempPlaceHolderForDevicesWithSymbols(
				modularSchemPinListInfo,
				createRemoveTempPlaceholdersForDevWithSymHandler());
	}

	@NotNull
	private Consumer<IPinList> createAddTempPlaceholdersForDevWithSymHandler(IGrid grid,
			GeneratorParameters generatorParams)
	{
		return (deviceWithSymbol) -> {
			PinPlaceholderProviderForSymbolledDeviceInMove pinPlaceholderProviderForSymbolledDeviceInMove =
					new PinPlaceholderProviderForSymbolledDeviceInMove(deviceWithSymbol, grid, generatorParams);
			pinPlaceholderProviderForSymbolledDeviceInMove.createTempPlaceHolderForDevicesWithSymbols();
			pinPlaceholderProviderForSymbolledDeviceInMove.getPinPlaceholders()
					.forEach(deviceWithSymbol::addObject);
		};
	}

	@NotNull
	private Consumer<IPinList> createRemoveTempPlaceholdersForDevWithSymHandler()
	{
		return (deviceWithSymbol) ->
				deviceWithSymbol.getObjects()
						.stream()
						.filter(IPinPlaceholder.class::isInstance)
						.forEach(deviceWithSymbol::removeObject);
	}

	private void doProcessTempPlaceHolderForDevicesWithSymbols(@NotNull IPinList devWithSymbol,
			@NotNull Consumer<IPinList> devWithSymbolHandler)
	{
		doProcessTempPlaceHolderForDevicesWithSymbols(Set.of(devWithSymbol), devWithSymbolHandler);
	}

	private void doProcessTempPlaceHolderForDevicesWithSymbols(@NotNull ModularSchemPinListInfo modularSchemPinListInfo,
			@NotNull Consumer<IPinList> devWithSymbolHandler)
	{
		Set<IPinList> devsWithSymbol = getDeviceWithSymbols(modularSchemPinListInfo);
		doProcessTempPlaceHolderForDevicesWithSymbols(devsWithSymbol, devWithSymbolHandler);
	}

	private void doProcessTempPlaceHolderForDevicesWithSymbols(@NotNull Collection<IPinList> devsWithSymbol,
			@NotNull Consumer<IPinList> devWithSymbolHandler)
	{
		// Ensure that this method cannot store any undo state for temporary objects created
		CAFUtils.getInstance().setTempUndoableContainer(UndoableContainerIdler.instance());
		try {
			devsWithSymbol.forEach(devWithSymbolHandler::accept);
		}
		// Must ensure that clearTempUndoableContainer is called in all circumstances
		// otherwise the undo mechanism will be disabled permanently.
		finally {
			CAFUtils.getInstance().clearTempUndoableContainer();
		}
	}
}