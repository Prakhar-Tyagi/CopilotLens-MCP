/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.inlineassist;

import chs.caplets.topology.inlineassist.GraphicalConductorComparator;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IShieldBodyHookup;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.SetMap;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides functionality to determine the placement direction of shield conductors.
 */
public class ShieldPlacementDirectionProvider
{

	public static final int SHIELD_DIR_UP = 1;
	public static final int SHIELD_DIR_DOWN = 0;

	/**
	 * Determines the direction for each shield conductor based on the hookup point in which shield is connected..
	 *
	 * @param shields A list of shield conductors.
	 * @param conductors A collection of conductors.
	 * @param isInlineVertical Indicates if the inline is vertical.
	 * @return A map of shield conductors and their respective directions.
	 */
	@NotNull public Map<IShieldConductor, Integer> getDirections(
			@NotNull List<IShieldConductor> shields,
			@NotNull Collection<IConductor> conductors,
			boolean isInlineVertical)
	{
		Map<IShieldConductor, Integer> shieldDirections = new LinkedHashMap<>();
		Set<IPinList> pinLists = getPinLists(conductors);
		SetMap<IShieldConductor, IConductor> shieldConductorToSchemShieldSetMap = collectSchemShields(pinLists);

		for (IShieldConductor shield : shields) {
			shieldDirections.put(shield,
					calculateShieldDirection(shield, shieldConductorToSchemShieldSetMap, isInlineVertical));
		}
		return shieldDirections;
	}

	@NotNull
	private Set<IPinList> getPinLists(@NotNull Collection<IConductor> conductors)
	{
		return conductors.stream()
				.flatMap(conductor -> conductor.getPins().stream())
				.map(pin -> CommonUtils.cast(pin.getParent(), IPinList.class))
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	@NotNull
	private SetMap<IShieldConductor, IConductor> collectSchemShields(@NotNull Set<IPinList> pinLists)
	{
		SetMap<IShieldConductor, IConductor> conductorMap = new SetMap<>();
		pinLists.stream()
				.flatMap(pinList -> pinList.getPins().stream())
				.map(pin -> CommonUtils.cast(pin, IPin.class))
				.filter(Objects::nonNull)
				.flatMap(pin -> pin.getConductors().stream())
				.filter(conductor -> conductor.getConnectivity() instanceof IShieldConductor)
				.forEach(conductor -> conductorMap.add(
						(IShieldConductor) conductor.getConnectivity(), conductor));
		return conductorMap;
	}

	private int calculateShieldDirection(
			@NotNull IShieldConductor shield,
			@NotNull SetMap<IShieldConductor, IConductor> conductorMap,
			boolean isInlineVertical)
	{
		Set<IConductor> instances = conductorMap.getSet(shield);
		if (instances.isEmpty()) {
			return SHIELD_DIR_DOWN; // Default direction
		}

		IConductor schemShield =
				CollectionUtils.createSortedList(instances, new GraphicalConductorComparator()).iterator().next();
		IShieldBodyHookup hookup = schemShield.getHookup();
		if (hookup == null || hookup.getOtherHookup() == null) {
			return SHIELD_DIR_DOWN; // Default direction if no hookup exists
		}

		return compareHookups(hookup, hookup.getOtherHookup(), isInlineVertical);
	}

	private int compareHookups(@NotNull IShieldBodyHookup hookup, @NotNull IShieldBodyHookup otherHookup,
			boolean isInlineVertical)
	{
		if (isInlineVertical) {
			return hookup.getAbsolutionLocation().getX() < otherHookup.getAbsolutionLocation().getX() ?
					SHIELD_DIR_DOWN : SHIELD_DIR_UP;
		}
		return hookup.getAbsolutionLocation().getY() > otherHookup.getAbsolutionLocation().getY() ?
				SHIELD_DIR_UP : SHIELD_DIR_DOWN;
	}
}
