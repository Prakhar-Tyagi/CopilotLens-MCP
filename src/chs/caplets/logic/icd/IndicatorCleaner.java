/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.icd;

import chs.caplets.logic.DeleteHelper;
import chs.cof.drawplus.IJoint;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.schem.IShieldBody;
import chs.common.IObjectFilter;
import chs.common.SetUtils;
import chs.common.UIDObjectCollection;
import chs.utilities.SetMap;
import chs.utility.logic.IndicatorRefresherUtils;
import chs.utility.logic.MulticoreIndicatorRefresher;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Delete redundant indicators of the multicore in the diagram
 */
public class IndicatorCleaner
{

	@NotNull private IMulticore multicore;
	@NotNull private ISchemDiagram diagram;

	public IndicatorCleaner(@NotNull IMulticore multicore, @NotNull ISchemDiagram diagram)
	{
		this.multicore = multicore;
		this.diagram = diagram;
	}

	public void cleanIndicators()
	{
		Set<IShieldBody> schemShieldBodies = ShieldBuilder.getSchemShieldBodies(multicore, diagram);
		SetMap<Set<IConductor>, IShieldBody> condShieldBodyMap = buildCondShieldBodyMap(schemShieldBodies);
		Set<IShieldBody> shieldBodiesToDelete = collectShieldBodiesToDelete(condShieldBodyMap);
		if(!shieldBodiesToDelete.isEmpty()) {
			DeleteHelper.getInstance().delete(diagram, shieldBodiesToDelete, false);
		}
	}

	@NotNull
	private SetMap<Set<IConductor>, IShieldBody> buildCondShieldBodyMap(@NotNull Set<IShieldBody> schemShieldBodies)
	{
		SetMap<Set<IConductor>, IShieldBody> condShieldBodyMap = new SetMap<>();
		for (IShieldBody shieldBody : schemShieldBodies) {
			UIDObjectCollection shieldBodies = new UIDObjectCollection();
			shieldBodies.add(shieldBody);
			Set<IConductor> conductors = IndicatorRefresherUtils.getSchemConductors(multicore, shieldBodies, diagram, true);
			condShieldBodyMap.add(conductors, shieldBody);
		}
		return condShieldBodyMap;
	}

	@NotNull private Set<IShieldBody> collectShieldBodiesToDelete(@NotNull SetMap<Set<IConductor>, IShieldBody> condShieldBodyMap)
	{
		Set<IShieldBody> shieldBodiesToDelete = new HashSet<>();
		for (Set<IConductor> conductors : condShieldBodyMap.keySet()) {
			if (conductors.isEmpty()) {
				continue;
			}
			Set<IShieldBody> shieldBodies = condShieldBodyMap.get(conductors);
			int totalSBCount = shieldBodies.size();
			if (totalSBCount <= 2) {
				continue;
			}
			Set<IShieldBody> deletableSBs = shieldBodies.stream()
					.filter(MulticoreIndicatorRefresher::canDeleteShieldBody)
					.collect(Collectors.toSet());
			Set<IShieldBody> nonDeletableSBs = SetUtils.difference(shieldBodies, deletableSBs);
			if (nonDeletableSBs.size() >= 2) {
				shieldBodiesToDelete.addAll(deletableSBs);
			}
			else {
				int additionalSBsToRetain = 2 - nonDeletableSBs.size();
				Set<IJoint> condEnds = getEndsToCheck(conductors.iterator().next());
				List<IShieldBody> sortedSBs = sortShieldBodies(deletableSBs, condEnds);
				for (IShieldBody shieldBody : sortedSBs.subList(additionalSBsToRetain, sortedSBs.size())) {
					shieldBodiesToDelete.add(shieldBody);
				}
			}
		}
		return shieldBodiesToDelete;
	}

	@NotNull private Set<IJoint> getEndsToCheck(@NotNull IConductor conductor)
	{
		Set<IJoint> conductorEnds = new HashSet<>();
		IObjectFilter filter = obj -> obj instanceof ISegment && ((ISegment)obj).getConductor() == conductor;
		conductor.getSegments().stream().forEach(segment -> {
			IJoint startJoint = segment.getStartJoint();
			if (startJoint != null && startJoint.getAssociations(filter).size() < 2) {
				conductorEnds.add(startJoint);
			}
			IJoint endJoint = segment.getEndJoint();
			if (endJoint != null && endJoint.getAssociations(filter).size() < 2) {
				conductorEnds.add(endJoint);
			}
		});
		return conductorEnds;
	}

	@NotNull
	private List<IShieldBody> sortShieldBodies(@NotNull Set<IShieldBody> shieldBodies, @NotNull Set<IJoint> condEnds)
	{
		return shieldBodies.stream().sorted((sb1, sb2) -> {
			Double sb1Distance = getMinDistanceOfShieldBody(condEnds, sb1);
			Double sb2Distance = getMinDistanceOfShieldBody(condEnds, sb2);
			return Double.compare(sb1Distance, sb2Distance);
		}).collect(Collectors.toList());
	}

	@NotNull private Double getMinDistanceOfShieldBody(@NotNull Set<IJoint> condEnds, @NotNull IShieldBody shieldBody)
	{
		Double distance = Double.MAX_VALUE;
		for (IJoint condEnd : condEnds) {
			double distanceFromCondEnd = condEnd.distance(shieldBody.getAbsLocation());
			if (distanceFromCondEnd < distance) {
				distance = distanceFromCondEnd;
			}
		}
		return distance;
	}
}
