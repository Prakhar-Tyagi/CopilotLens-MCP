/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.common.IUIDObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface ISharedMulticoreMappingChecker
{
	boolean matchChildren(IMulticore multicore, ISharedMulticore sharedMulticore);

	boolean mapShields(IMulticore multicore, ISharedMulticore sharedMulticore, Map<IUIDObject, ISharedObject> mapping);


	@Nullable default ISharedConductor handleUnmatchedConductor(@NotNull IConductor conductor,
			@NotNull ISharedMulticore sharedMulticore, @Nullable Map<IUIDObject, ISharedObject> mapping)
	{
		return null;
	}
}
