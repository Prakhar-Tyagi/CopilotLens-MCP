/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.unshare;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link IBatchUnshareParams} that provides data for the batch unshare dialog.
 * <p>
 * This class serves as a data holder and retriever for batch unshare operations. It wraps
 * a collection of {@link IBatchUnshareRow} objects representing redundant shared objects.
 */
public class BatchUnshareParams implements IBatchUnshareParams
{

	private final Collection<IBatchUnshareRow> m_data;

	public BatchUnshareParams(@NotNull Collection<IBatchUnshareRow> data)
	{
		m_data = data;
	}

	@Override @NotNull public Collection<IBatchUnshareRow> getData()
	{
		return m_data;
	}

	@Override @NotNull public Set<IBatchUnshareRow> retrieveRowsFromUserSelection()
	{
		return m_data.stream()
				.filter(IBatchUnshareRow::isSelected)
				.collect(Collectors.toSet());
	}
}

