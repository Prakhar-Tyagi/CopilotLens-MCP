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
import chs.cof.project.buildlist.IBuildList;
import chs.common.IDesignDescriptor;
import chs.common.IUID;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Design scope resolver that includes only logic designs present in a specific build list.
 * This limits operations to designs within the build list boundary.
 * <p>
 * This implementation caches the design UIDs from the build list at construction time
 * for efficient lookups and filtering.
 */
public class BuildListDesignScope implements IDesignScopeResolver
{

	private final List<IBuildList> buildLists;
	private final Set<IUID> buildListDesignUIDs;

	public BuildListDesignScope(@NotNull List<IBuildList> buildLists)
	{
		this.buildLists = buildLists;
		buildListDesignUIDs =
				buildLists.stream().flatMap(bl -> bl.getDesignDescriptors().stream()).map(IDesignDescriptor::getUID)
						.collect(Collectors.toSet());
	}

	@NotNull @Override public Collection<IUID> getDesignsUIDInScope(@NotNull IProject project)
	{
		return buildListDesignUIDs;
	}

	@NotNull
	@Override
	public ISingleUsageProvider getSingleUsageProvider()
	{
		return new BuildListSingleUsageProvider(buildLists);
	}
}

