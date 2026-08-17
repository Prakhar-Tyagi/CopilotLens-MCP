/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caf.cafmain.actions.capture.sysml.graph;

import chs.bridges.adaptors.tcmbse.ISysMLProjectNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

/**
 * Interface for building a graph of used SysML projects.
 * The graph is constructed starting from a root project identified by the given project ID,
 * and includes all projects that are directly or indirectly used by it.
 */
public interface IUsedProjectGraphBuilder
{

	/**
	 * Builds a graph of used SysML projects starting from the specified root object ID.
	 *
	 * @param projectId            the unique identifier of the project node
	 * @param getNodeNeighborsFunction the lamda function used to fetch neighbouring project relationships given project id
	 * @return the root node of the constructed SysML project graph
	 */
	@NotNull ISysMLProjectNode buildUsedProjectGraph(@NotNull String projectId,
			@NotNull Function<String, List<String>> getNodeNeighborsFunction);
}
