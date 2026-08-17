/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caf.caplet.helpers.snapping;

import chs.cof.draw.IGfxObject;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.schem.IPinList;
import chs.common.IExtent;
import chs.common.Side;
import chs.utility.helpers.ExtentHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Provides candidate schematic pinLists on the same side of a schematic device relative to a reference object.
 */
public class SameSidePinListFinder
{

	/**
	 * Returns candidate schematic connectors attached to the given schematic device
	 * that match the specified cable connector, lie on the same side as the reference object,
	 * and satisfy the candidate filter.
	 * <p>
	 *
	 * Results are sorted by distance to the reference object (nearest first).
	 *
	 * @param schemDevice            the schematic device whose attached pin lists are searched
	 * @param matchingCableConnector the cable connector that candidates must match via connectivity
	 * @param referenceObject        the diagram object used to determine the target side and sort order
	 * @param candidateFilter        additional predicate to include or exclude candidates
	 * @return a list of matching {@link IPinList} instances sorted by proximity to the reference object
	 */
	@NotNull
	public List<IPinList> findMatchingPinListsOnSameSide(@NotNull IPinList schemDevice,
			@NotNull IConnector matchingCableConnector, @NotNull IDiagramObject referenceObject,
			@NotNull Predicate<IPinList> candidateFilter)
	{
		Function<IDiagramObject, Side> sideResolverRelToSchemDev = sideResolverRelativeTo(schemDevice);
		Side referenceObjectSide = sideResolverRelToSchemDev.apply(referenceObject);

		return schemDevice.getAttachedPinListObjects(IPinList.EXCLUDE_MODULAR)
				.stream()
				.filter(candidateFilter)
				.filter(pinList -> matchingCableConnector.equals(pinList.getConnectivity()))
				.filter(pinList -> referenceObjectSide.equals(sideResolverRelToSchemDev.apply(pinList)))
				.sorted(distanceFrom(referenceObject))
				.collect(Collectors.toList());
	}

	@NotNull
	private Comparator<IDiagramObject> distanceFrom(@NotNull IDiagramObject referenceObj)
	{
		return Comparator.comparingDouble(
				obj -> obj.getAbsLocation().distance(referenceObj.getAbsLocation()));
	}

	/**
	 * Returns a function that determines which {@link Side} of the given graphics object a diagram object
	 * is located on, based on its absolute location relative to the non-text extent of the graphics object.
	 *
	 * @param gfxObject the graphics object whose extent defines the side boundaries
	 * @return a function mapping a {@link IDiagramObject} to its {@link Side} relative to the graphics object
	 */
	@NotNull
	private Function<IDiagramObject, Side> sideResolverRelativeTo(@NotNull IGfxObject gfxObject)
	{
		IExtent gfxObjectExtent = ExtentHelper.getAbsNonTextExtent(gfxObject);
		return diagramObject -> getRelativeSide(gfxObjectExtent, diagramObject);
	}

	@NotNull
	private Side getRelativeSide(@NotNull IExtent extent, @NotNull IDiagramObject obj)
	{
		return Side.getSide(extent, obj.getAbsLocation());
	}
}
