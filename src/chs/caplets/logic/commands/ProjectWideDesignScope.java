/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.commands;

import chs.cof.logical.IDesignScopeResolver;
import chs.cof.logical.ISingleUsageProvider;
import chs.cof.project.IProject;
import chs.common.IDesignMgr;
import chs.common.IUID;
import chs.utilities.suite.DesignType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Design scope resolver that includes all logic designs in the project.
 * This is the default scope when no build list is specified.
 * <p>
 * This implementation retrieves all designs of type LOGICAL from the design manager.
 */
public class ProjectWideDesignScope implements IDesignScopeResolver
{

	@NotNull @Override public Collection<IUID> getDesignsUIDInScope(@NotNull IProject project)
	{
		IDesignMgr designMgr = project.getDesignMgr();
		return designMgr.getDesignUIDsOfType(DesignType.LOGICAL).stream().filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	@NotNull
	@Override
	public ISingleUsageProvider getSingleUsageProvider()
	{
		return new ProjectSingleUsageProvider();
	}
}

