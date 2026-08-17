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
import chs.caf.cafmain.actions.capture.sysml.SysMLProjectNode;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Builds Used Projects as a graph
 */
public class UsedProjectGraphBuilder implements IUsedProjectGraphBuilder
{


	/**
	 * Builds a graph of used SysML projects starting from the specified root project ID.
	 * The graph is constructed recursively, where each project node is represented as an
	 * `ISysMLProjectNode` and connected to its used projects as child nodes.
	 *
	 * @param projectId               the unique identifier of the root project node
	 * @param getNodeNeighborsFunction a lambda function to fetch the list of used project IDs for a given project ID
	 * @return the root node of the constructed SysML project graph
	 */

	@Override @NotNull
	public ISysMLProjectNode buildUsedProjectGraph(@NotNull String projectId, @NotNull Function<String, List<String>> getNodeNeighborsFunction)
	{
		Map<String, ISysMLProjectNode> nodeMap = new HashMap<>();
		ISysMLProjectNode root = createGraph(projectId, getNodeNeighborsFunction, nodeMap);
		return root;
	}

	/**
	 * Recursively creates a graph of SysML projects starting from the specified project ID.
	 * Each project node is represented as an `ISysMLProjectNode` and is connected to its
	 * used projects as child nodes, forming a directed graph.
	 *
	 * @param projectId               the unique identifier of the project node for which this method is triggered
	 * @param getNodeNeighborsFunction a lambda function to fetch the list of used project IDs for a given project ID
	 * @param nodeMap                 a map to keep track of already created project nodes to avoid duplication
	 * @return the root node of the constructed SysML project graph
	 */

	@NotNull
	private ISysMLProjectNode createGraph(String projectId, @NotNull Function<String, List<String>> getNodeNeighborsFunction,
			Map<String, ISysMLProjectNode> nodeMap)
	{
		ISysMLProjectNode node = new SysMLProjectNode(projectId);
		nodeMap.put(projectId, node);
		for (String usedProject : getNodeNeighborsFunction.apply(projectId)) {
			if (nodeMap.containsKey(usedProject)) {
				node.addChild(nodeMap.get(usedProject));
				continue;
			}
			ISysMLProjectNode child = createGraph(usedProject, getNodeNeighborsFunction, nodeMap);
			node.addChild(child);
		}
		return node;
	}
}
