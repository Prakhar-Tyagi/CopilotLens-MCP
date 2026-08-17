/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.merge;

import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.shared.ISharedBackshell;
import org.jetbrains.annotations.NotNull;

/**
 * Default implementation of {@link IMergerFactory} that creates merger instances
 * with a no-op change reporter.
 */
public class MergerFactory implements IMergerFactory
{

	@NotNull @Override public IBackshellMerger createBackshellMerger()
	{
		return new BackshellMerger();
	}

	@NotNull @Override public IBackshellTerminationMerger createBackshellTerminationMerger(IBackshell sourceBackshell,
			IBackshell targetBackshell)
	{
		return new BackshellTerminationMerger(sourceBackshell, targetBackshell);
	}

	@NotNull @Override
	public ISharedBackshellMerger createSharedBackshellMerger(@NotNull ISharedBackshell sharedSourceBackshell,
			@NotNull ISharedBackshell sharedTargetBackshell)
	{
		return new SharedBackshellMerger();
	}
}