/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage;

import chs.caf.caplet.helpers.replication.IDataTransferReplicator;
import chs.cof.logical.schem.IPinList;
import chs.common.IUIDObject;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public interface ISymbolsToConvertProvider
{

	@NotNull default List<IPinList> getPinListsToConvert(Collection<IUIDObject> schematicObjects,
			IDataTransferReplicator replicator)
	{
		return Collections.emptyList();
	}

	@NotNull static ISymbolsToConvertProvider getDefaultProvider()
	{
		return new ISymbolsToConvertProvider()
		{
		};
	}
}
