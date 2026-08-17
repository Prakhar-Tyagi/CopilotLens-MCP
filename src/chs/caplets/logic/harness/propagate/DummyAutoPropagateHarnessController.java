/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.harness.propagate;

import chs.common.IUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A dummy implementation of IAutoPropagateHarnessController, this is intended to be used when propagate harness is not applicable
 */
public class DummyAutoPropagateHarnessController implements IAutoPropagateHarnessController
{

	@Override public void clearHarnessPropagateWindow()
	{

	}

	@Nullable @Override public PropagationInfo getPropagationInfo()
	{
		return null;
	}

	@Override public void loadObjects(@NotNull IUID designUid, @NotNull HarnessUpdateStatusMessageTableModel tableModel,
			boolean propagateAll)
	{

	}
}