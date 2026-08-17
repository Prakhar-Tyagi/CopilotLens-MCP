/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2020-2023 Siemens
 */

package chs.caplets.logic.actions.layout.sync;

import chs.cof.logical.cable.IBlockDevice;
import chs.common.IDesignContainer;
import chs.common.IUIDObject;
import chs.common.sync.IAssociatedDesignFilterProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class LayoutAssociateDesignFilterProcessor implements IAssociatedDesignFilterProcessor
{

	@Override public void startOperation()
	{

	}

	@Override public void endOperation()
	{

	}

	@NotNull @Override
	public <T extends IUIDObject> Collection<T> applySyncFilters(@NotNull IDesignContainer design,
			@NotNull Collection<T> objects)
	{
		return objects;
	}

	@Override
	public <T extends IUIDObject> boolean isFiltered(@Nullable T obj)
	{
		return obj == null || obj instanceof IBlockDevice;
	}

	@Override
	public void resetCachesForSync()
	{

	}
}
