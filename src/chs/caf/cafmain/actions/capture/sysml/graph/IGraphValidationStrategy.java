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

/**
 * Interface for graph validation strategies.
 * Implementations encapsulate specific validation algorithms.
 */
public interface IGraphValidationStrategy
{

	/**
	 * Validates the graph starting at the given root node.
	 *
	 * @param root the root node of the graph to validate.
	 * @return true if the graph passes the validation; false otherwise.
	 */
	boolean validate(@NotNull ISysMLProjectNode root);
}
