/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023 Siemens
 */

package chs.caplets.logic.shared;

import chs.cof.logical.shared.ISharedAbstractable;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConductorIterator;
import chs.cof.logical.shared.ISharedFunctionConductor;
import chs.cof.logical.shared.ISharedFunctionMessage;
import chs.cof.logical.shared.ISharedFunctionMessageIterator;
import chs.cof.logical.shared.ISharedGeneralHighway;
import chs.cof.logical.shared.ISharedGeneralHighwayIterator;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedMulticoreIterator;
import chs.cof.logical.shared.ISharedOverbraid;
import chs.cof.logical.shared.ISharedOverbraidIterator;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedSingleLine;
import chs.cof.logical.shared.ISharedSingleLineIterator;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.project.IProject;
import chs.common.IDesignAbstraction;
import chs.utilities.ListMap;
import chs.utilities.SetMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collect shared objects by abstraction
 */
public class SharedObjectsByAbstraction
{

	private Map<IDesignAbstraction, DivideSharedPinlistsByType> abstractionVsPinListsByType = new HashMap<>();
	private ListMap<IDesignAbstraction, ISharedAbstractable> abstractionVsCondMgrObjects = new ListMap<>();
	private ListMap<IDesignAbstraction, ISharedGeneralHighway> abstractionVsGeneralHighways = new ListMap<>();
	private ListMap<IDesignAbstraction, ISharedSingleLine> abstractionVsSingleLines = new ListMap<>();
	private SetMap<PinListTypeEnum, IDesignAbstraction> pinListTypeVsAbstractions = new SetMap<>();
	private SetMap<Class<? extends ISharedAbstractable>, IDesignAbstraction> condTypeVsAbstraction = new SetMap<>();

	private SharedTreeBackgroundCache sharedTreeBackgroundCache;

	public SharedObjectsByAbstraction(@NotNull IProject project)
	{
		this(new SharedTreeBackgroundCache(project));
	}

	public SharedObjectsByAbstraction(@NotNull SharedTreeBackgroundCache sharedTreeBackgroundCache)
	{
		this.sharedTreeBackgroundCache = sharedTreeBackgroundCache;
		buildPinListAbstractionMap();
		buildConductorAbstractionMap();
	}

	private void buildPinListAbstractionMap()
	{
		for (ISharedPinList sharedPinList : sharedTreeBackgroundCache.getSharedPinLists()) {
			IDesignAbstraction abstraction = sharedPinList.getDesignAbstraction();
			abstractionVsPinListsByType.putIfAbsent(abstraction, new DivideSharedPinlistsByType());
			abstractionVsPinListsByType.get(abstraction).addSharedPinList(sharedPinList);

			if (abstraction != null) {
				pinListTypeVsAbstractions.add(sharedPinList.getType(), abstraction);
			}
		}
	}

	private void buildConductorAbstractionMap()
	{
		buildSharedConductorMap(sharedTreeBackgroundCache.getSharedConductors());
		buildSharedMulticoreMap(sharedTreeBackgroundCache.getSharedMultiCores());
		buildSharedOverbraidMap(sharedTreeBackgroundCache.getSharedOverBraids());
		buildSharedFunctionMessageMap(sharedTreeBackgroundCache.getSharedFunctionMessages());
		buildSharedGeneralHighwayMap(sharedTreeBackgroundCache.getSharedHighways());
		buildSharedSingleLineMap(sharedTreeBackgroundCache.getSharedSingleLines());
	}

	private void buildSharedConductorMap(@NotNull ISharedConductorIterator conductors)
	{
		for (ISharedConductor sharedConductor : conductors) {
			IDesignAbstraction abstraction = sharedConductor.getDesignAbstraction();
			abstractionVsCondMgrObjects.add(abstraction, sharedConductor);
			if (abstraction != null && sharedConductor.getMulticore() == null) {
				if (sharedConductor instanceof ISharedFunctionConductor) {
					condTypeVsAbstraction.add(ISharedFunctionConductor.class, abstraction);
				}
				else if (!sharedConductor.isSignal()) {
					condTypeVsAbstraction.add(ISharedConductor.class, abstraction);
				}
			}
		}
	}

	private void buildSharedMulticoreMap(@NotNull ISharedMulticoreIterator sharedMulticores)
	{
		for (ISharedMulticore sharedMulticore : sharedMulticores) {
			IDesignAbstraction abstraction = sharedMulticore.getDesignAbstraction();
			abstractionVsCondMgrObjects.add(abstraction, sharedMulticore);
			if (abstraction != null && sharedMulticore.getParent() == null) {
				condTypeVsAbstraction.add(ISharedMulticore.class, abstraction);
			}
		}
	}

	private void buildSharedOverbraidMap(@NotNull ISharedOverbraidIterator sharedOverbraids)
	{
		for (ISharedOverbraid sharedOverbraid : sharedOverbraids) {
			IDesignAbstraction abstraction = sharedOverbraid.getDesignAbstraction();
			abstractionVsCondMgrObjects.add(abstraction, sharedOverbraid);
			if (abstraction != null && sharedOverbraid.getParent() == null) {
				condTypeVsAbstraction.add(ISharedOverbraid.class, abstraction);
			}
		}
	}

	private void buildSharedFunctionMessageMap(@NotNull ISharedFunctionMessageIterator sharedFunctions)
	{
		for (ISharedFunctionMessage sharedFunctionMessage : sharedFunctions) {
			IDesignAbstraction abstraction = sharedFunctionMessage.getDesignAbstraction();
			abstractionVsCondMgrObjects.add(abstraction, sharedFunctionMessage);
		}
	}

	private void buildSharedGeneralHighwayMap(@NotNull ISharedGeneralHighwayIterator sharedGeneralHighways)
	{
		for (ISharedGeneralHighway sharedHighway : sharedGeneralHighways) {
			IDesignAbstraction abstraction = sharedHighway.getDesignAbstraction();
			abstractionVsGeneralHighways.add(abstraction, sharedHighway);
			if (abstraction != null) {
				condTypeVsAbstraction.add(ISharedGeneralHighway.class, abstraction);
			}
		}
	}

	private void buildSharedSingleLineMap(@NotNull ISharedSingleLineIterator sharedSingleLines)
	{
		for (ISharedSingleLine sharedSingleLine : sharedSingleLines) {
			IDesignAbstraction abstraction = sharedSingleLine.getDesignAbstraction();
			abstractionVsSingleLines.add(abstraction, sharedSingleLine);
			if (abstraction != null) {
				condTypeVsAbstraction.add(ISharedSingleLine.class, abstraction);
			}
		}
	}

	@NotNull public List<ISharedPinList> getSharedPinLists(@Nullable IDesignAbstraction abstraction)
	{
		return getSharedPinLists(abstraction, Set.of(PinListTypeEnum.values()));
	}

	@NotNull public List<ISharedPinList> getSharedPinLists(@Nullable IDesignAbstraction abstraction,
														   @NotNull Set<PinListTypeEnum> pinListTypes)
	{
		DivideSharedPinlistsByType pinlistsByType = abstractionVsPinListsByType.get(abstraction);
		if (pinlistsByType != null) {
			return pinlistsByType.getSharedPinListsForTypes(pinListTypes);
		}
		return Collections.emptyList();
	}

	@SuppressWarnings("ConstantConditions")
	@NotNull public List<ISharedAbstractable> getSharedCondMgrObjectsByAbstraction(@Nullable IDesignAbstraction abstraction)
	{
		return abstractionVsCondMgrObjects.pullReadOnlySafeList(abstraction);
	}

	@SuppressWarnings("ConstantConditions")
	@NotNull public List<ISharedGeneralHighway> getSharedGeneralHighwaysByAbstraction(@Nullable IDesignAbstraction abstraction)
	{
		return abstractionVsGeneralHighways.pullReadOnlySafeList(abstraction);
	}

	@SuppressWarnings("ConstantConditions")
	@NotNull public List<ISharedSingleLine> getSharedSingleLinesByAbstraction(@Nullable IDesignAbstraction abstraction)
	{
		return abstractionVsSingleLines.pullReadOnlySafeList(abstraction);
	}

	@NotNull public Set<IDesignAbstraction> getAbstractionsForPinListType(@NotNull PinListTypeEnum pinListType)
	{
		return pinListTypeVsAbstractions.getOrDefault(pinListType, Collections.emptySet());
	}

	@NotNull public Set<IDesignAbstraction> getAbstractionsForTopLevelMulticores()
	{
		return condTypeVsAbstraction.pullReadOnlySafeSet(ISharedMulticore.class);
	}

	@NotNull public Set<IDesignAbstraction> getAbstractionsForTopLevelOverbraids()
	{
		return condTypeVsAbstraction.pullReadOnlySafeSet(ISharedOverbraid.class);
	}

	@NotNull public Set<IDesignAbstraction> getAbstractionsForGeneralHighways()
	{
		return condTypeVsAbstraction.pullReadOnlySafeSet(ISharedGeneralHighway.class);
	}

	@NotNull public Set<IDesignAbstraction> getAbstractionsForSingleLines()
	{
		return condTypeVsAbstraction.pullReadOnlySafeSet(ISharedSingleLine.class);
	}

	@NotNull public Set<IDesignAbstraction> getAbstractionsForLogicalConductors()
	{
		return condTypeVsAbstraction.pullReadOnlySafeSet(ISharedConductor.class);
	}

	@NotNull public Set<IDesignAbstraction> getAbstractionsForFunctionConductors()
	{
		return condTypeVsAbstraction.pullReadOnlySafeSet(ISharedFunctionConductor.class);
	}

	@NotNull public List<ISharedPinList> getAllSharedPinListsOfType(@NotNull Set<PinListTypeEnum> pinListTypes)
	{
		List<ISharedPinList> sharedPinLists = new ArrayList<>();
		for (Map.Entry<IDesignAbstraction, DivideSharedPinlistsByType> entry : abstractionVsPinListsByType.entrySet()) {
			DivideSharedPinlistsByType pinlistsByType = entry.getValue();
			sharedPinLists.addAll(pinlistsByType.getSharedPinListsForTypes(pinListTypes));
		}
		return sharedPinLists;
	}
}
