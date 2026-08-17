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
import chs.cof.logical.IPinFilter;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IGroundDevice;
import chs.cof.logical.schem.IPinList;
import chs.common.IUIDObject;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gives all the symbol pin lists which needs to be converted into parameterized
 */
public class SymbolsToConvertProvider implements ISymbolsToConvertProvider
{

	private IPinFilter pinFilter;

	public SymbolsToConvertProvider(IPinFilter pinFilter)
	{
		this.pinFilter = pinFilter;
	}

	@NotNull public List<IPinList> getPinListsToConvert(Collection<IUIDObject> schematicObjects,
			IDataTransferReplicator replicator)
	{
		return schematicObjects
				.stream()
				.filter(copiedDiagramObject -> copiedDiagramObject instanceof IPinList)
				.map(copiedDiagramObject -> (IPinList) copiedDiagramObject)
				.filter(pl -> canConvertToParameterized(pl, replicator))
				.collect(Collectors.toList());
	}

	private boolean canConvertToParameterized(IPinList pinList,
			IDataTransferReplicator replicator)
	{
		chs.cof.logical.cable.IPinList connectivity = pinList.getConnectivity();
		return isANonGroundDevice(connectivity)
				&& isSymbolDevice(pinList)
				&& ifAtLeastOnePinIsNotInSignal(pinList, replicator);
	}

	private boolean ifAtLeastOnePinIsNotInSignal(IPinList pinList,
			IDataTransferReplicator replicator)
	{
		IPinList oldPinList = replicator.getOldObject(pinList, IPinList.class);
		if (oldPinList == null) {
			return true;
		}
		return oldPinList
				.getAllPins()
				.stream()
				.anyMatch(object -> !pinFilter.accept(object));
	}

	private boolean isANonGroundDevice(chs.cof.logical.cable.IPinList connectivity)
	{
		return connectivity instanceof IDevice && !(connectivity instanceof IGroundDevice);
	}

	private boolean isSymbolDevice(IPinList pinList)
	{
		return pinList.getParameterized() == null;
	}
}
