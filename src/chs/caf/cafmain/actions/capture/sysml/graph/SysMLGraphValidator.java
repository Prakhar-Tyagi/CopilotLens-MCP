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

/**
 * Orchestrator class responsible for validating a graph using
 * a set of pluggable validation strategies.
 */
public class SysMLGraphValidator
{

	@NotNull private final List<IGraphValidationStrategy> strategies;

	/**
	 * Constructs a GraphValidator with the given list of validation strategies.
	 *
	 * @param strategies a list of validation strategies to apply.
	 */
	public SysMLGraphValidator(@NotNull List<IGraphValidationStrategy> strategies)
	{
		this.strategies = strategies;
	}

	/**
	 * Validates the given graph root node against all configured strategies.
	 * Validation stops and returns false on the first failing strategy.
	 *
	 * @param root the root node of the graph to validate.
	 * @return true if all strategies pass; false otherwise.
	 */
	public boolean validate(@NotNull ISysMLProjectNode root)
	{
		for (IGraphValidationStrategy strategy : strategies) {
			if (!strategy.validate(root)) {
				return false;
			}
		}
		return true;
	}
}
