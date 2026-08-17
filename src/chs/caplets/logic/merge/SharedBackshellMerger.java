/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.merge;

import chs.cof.logical.shared.ISharedBackshell;
import chs.cof.logical.shared.ISharedBackshellTermination;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
/**
 * Responsible for merging backshell data from a source shared backshell to a target shared backshell.
 * The merge is based on matching backshell terminations by name. If a match is found, the source termination is deleted.
 * If no match is found, the source termination is transferred to the target shared backshell.
 */
public class SharedBackshellMerger implements ISharedBackshellMerger
{

	@Override public void mergeSharedBackshell(@NotNull ISharedBackshell sourceSharedBackshell,
			@NotNull ISharedBackshell targetSharedBackshell)
	{
		List<ISharedBackshellTermination> sharedBSTsToBeMoved = mergeMatchingSharedBST(sourceSharedBackshell, targetSharedBackshell);

		for (ISharedBackshellTermination sharedBSTToBeMoved : sharedBSTsToBeMoved) {
			sourceSharedBackshell.removeBackshellTermination(sharedBSTToBeMoved);
			targetSharedBackshell.addBackshellTermination(sharedBSTToBeMoved);
		}
	}

	@NotNull
	private List<ISharedBackshellTermination> mergeMatchingSharedBST(@NotNull ISharedBackshell sourceSharedBackshell,
			@NotNull ISharedBackshell targetSharedBackshell)
	{
		List<ISharedBackshellTermination> sharedBSTsToBeTransfered = new LinkedList<>();
		Map<ISharedBackshellTermination, ISharedBackshellTermination> sharedBSTMapping =
				getSharedBSTMappingForMerge(sourceSharedBackshell, targetSharedBackshell);
		for (Map.Entry<ISharedBackshellTermination, ISharedBackshellTermination> entry : sharedBSTMapping.entrySet()) {
			ISharedBackshellTermination sourceSharedBST = entry.getKey();
			ISharedBackshellTermination targetSharedBST = entry.getValue();
			if (targetSharedBST != null) {
				deleteOldPin(sourceSharedBST);
			}
			else {
				sharedBSTsToBeTransfered.add(sourceSharedBST);
			}
		}

		return sharedBSTsToBeTransfered;
	}

	private void deleteOldPin(@NotNull ISharedBackshellTermination sourceSharedBST)
	{
		sourceSharedBST.delete();
	}

	@NotNull
	private Map<ISharedBackshellTermination, ISharedBackshellTermination> getSharedBSTMappingForMerge(
			@NotNull ISharedBackshell sourceSharedBackshell, @NotNull ISharedBackshell targetSharedBackshell)
	{
		Map<ISharedBackshellTermination, ISharedBackshellTermination> sharedBSTMapping = new LinkedHashMap<>();
		for (ISharedBackshellTermination sharedBST : sourceSharedBackshell.getBackshellTerminations()) {
			findSharedBSTByName(targetSharedBackshell, sharedBST.getName())
					.ifPresentOrElse(
							matchingTargetSharedBST -> sharedBSTMapping.put(sharedBST, matchingTargetSharedBST),
							() -> sharedBSTMapping.put(sharedBST, null));
		}
		return sharedBSTMapping;
	}

	@NotNull
	private Optional<ISharedBackshellTermination> findSharedBSTByName(@NotNull ISharedBackshell targetSharedBackshell,
			@NotNull String sharedBSTName)
	{
		return targetSharedBackshell.getBackshellTerminations()
				.stream()
				.filter(targetBST -> sharedBSTName.equalsIgnoreCase(targetBST.getName()))
				.findFirst();
	}
}
