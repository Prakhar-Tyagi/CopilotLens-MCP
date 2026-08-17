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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SysML project graph utils
 */
public class SysMLGraphUtils
{

	public boolean validateGraph(@NotNull ISysMLProjectNode root)
	{
		SysMLGraphValidator validator =
				new SysMLGraphValidator(List.of(new CycleDetectionStrategy()));

		return validator.validate(root);
	}
}
