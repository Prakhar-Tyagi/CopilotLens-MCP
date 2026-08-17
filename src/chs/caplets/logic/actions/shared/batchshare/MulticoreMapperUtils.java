/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IOverbraid;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedOverbraid;
import chs.common.IUIDObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility to check and map multicore to shared multicore hierarchy
 */
class MulticoreMapperUtils
{

	private MulticoreMapperUtils()
	{
	}

	public static boolean mapMulticoreToSharedMulticore(@NotNull IMulticore multicore,
			@NotNull ISharedMulticore sharedMulticore,
			@Nullable Map<IUIDObject, ISharedObject> mapping,
			@NotNull ISharedMulticoreMappingChecker mappingChecker)
	{
		if (multicore instanceof IOverbraid ^ sharedMulticore instanceof ISharedOverbraid) {
			return false;
		}
		if (multicore.getParent() != null && !multicore.getName().equals(sharedMulticore.getName())) {
			return false;
		}
		if (!mappingChecker.mapShields(multicore, sharedMulticore, mapping)) {
			return false;
		}
		if (!mappingChecker.matchChildren(multicore, sharedMulticore)) {
			return false;
		}

		Set<ISharedConductor> unmappedSharedConductors =
				sharedMulticore.getConductors().stream().collect(Collectors.toSet());
		for (IConductor conductor : multicore.getConductors()) {
			ISharedConductor matchingConductor = null;
			for (ISharedConductor candidateMatchingConductor : unmappedSharedConductors) {
				if (mapConductorToSharedConductor(conductor, candidateMatchingConductor, mapping)) {
					matchingConductor = candidateMatchingConductor;
					break;
				}
			}
			if (matchingConductor == null) {
				ISharedConductor expandedSharedMulticore = mappingChecker.handleUnmatchedConductor(conductor, sharedMulticore, mapping);
				if (expandedSharedMulticore == null) {
					return false;
				}
				continue;
			}
			unmappedSharedConductors.remove(matchingConductor);
		}

		Set<ISharedMulticore> unmappedSharedMulticores =
				sharedMulticore.getMulticores().stream().collect(Collectors.toSet());
		for (IMulticore innerMC : multicore.getMulticores()) {
			ISharedMulticore matchingInnerMC = null;
			for (ISharedMulticore candidateMatchingInnerMC : unmappedSharedMulticores) {
				if (mapMulticoreToSharedMulticore(innerMC, candidateMatchingInnerMC, mapping, mappingChecker)) {
					matchingInnerMC = candidateMatchingInnerMC;
					break;
				}
			}
			if (matchingInnerMC == null) {
				return false;
			}
			unmappedSharedConductors.remove(matchingInnerMC);
		}
		if (mapping != null) {
			mapping.put(multicore, sharedMulticore);
		}
		return true;
	}

	private static boolean mapConductorToSharedConductor(@NotNull IConductor conductor,
			@NotNull ISharedConductor sharedConductor, @Nullable Map<IUIDObject, ISharedObject> mapping)
	{
		if (sharedConductor.isShield() ^ conductor instanceof IShieldConductor ||
				sharedConductor.isNet() ^ conductor instanceof INetConductor ||
				sharedConductor.isWire() ^ conductor instanceof IWireConductor) {
			return false;
		}
		if (!conductor.getName().equals(sharedConductor.getName())) {
			return false;
		}
		if (mapping != null) {
			mapping.put(conductor, sharedConductor);
		}
		return true;
	}
}
