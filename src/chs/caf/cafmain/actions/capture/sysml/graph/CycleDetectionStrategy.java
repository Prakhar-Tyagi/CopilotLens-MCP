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

import java.util.HashSet;
import java.util.Set;

/**
 * Strategy implementation that checks whether the graph
 * contains any cycles, indicating whether it is acyclic.
 */
public class CycleDetectionStrategy implements IGraphValidationStrategy
{

	/**
	 * Validates the graph by checking if it contains cycles.
	 *
	 * @param root the root node of the graph.
	 * @return true if the graph is acyclic (no cycles detected), false otherwise.
	 */
	@Override
	public boolean validate(@NotNull ISysMLProjectNode root)
	{
		Set<ISysMLProjectNode> visited = new HashSet<>();
		Set<ISysMLProjectNode> recursionStack = new HashSet<>();
		return !hasCycle(root, visited, recursionStack);
	}

	/**
	 * Performs DFS to detect cycles in the graph.
	 *
	 * @param node           the current node in DFS traversal.
	 * @param visited        a set to keep track of visited nodes.
	 * @param recursionStack a set to keep track of nodes in the current recursion stack.
	 * @return true if a cycle is detected starting from this node; false otherwise.
	 */
	private boolean hasCycle(@NotNull ISysMLProjectNode node, @NotNull Set<ISysMLProjectNode> visited,
			@NotNull Set<ISysMLProjectNode> recursionStack)
	{
		if (recursionStack.contains(node)) {
			return true; // cycle detected
		}
		if (visited.contains(node)) {
			return false; // already processed this node, no cycle found here
		}

		visited.add(node);
		recursionStack.add(node);

		for (ISysMLProjectNode child : node.getChildren()) {
			if (hasCycle(child, visited, recursionStack)) {
				return true;
			}
		}

		recursionStack.remove(node);
		return false;
	}
}
