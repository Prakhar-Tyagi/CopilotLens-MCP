/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.commands;

import chs.capitalmanager.appserver.IUserSession;
import chs.capitalmanager.appserver.IUserSessionRemotePackage.SingleUsageSharedObjectInfo;
import chs.capitalmanager.appserver.UserSessionException;
import chs.cof.logical.ISingleUsageProvider;
import chs.cof.project.IProject;
import chs.cof.project.buildlist.IBuildList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Single usage provider for build list scope.
 * This implementation retrieves single usage shared objects that are used
 * only once within the specified build lists, with server-side filtering.
 */
class BuildListSingleUsageProvider implements ISingleUsageProvider
{
	private final List<IBuildList> buildLists;

	BuildListSingleUsageProvider(@NotNull List<IBuildList> buildLists)
	{
		this.buildLists = buildLists;
	}

	@NotNull
	@Override
	public SingleUsageSharedObjectInfo[] getSingleUsages(
			@NotNull SingleUsageSharedObjectInfo.SharedObjectType[] requestedTypes,
			@NotNull IUserSession userSession , IProject project) throws UserSessionException
	{
		String[] designUIDs = buildLists.stream()
				.flatMap(bl -> bl.getDesignDescriptors().stream())
				.map(descriptor -> descriptor.getUID().toString())
				.toArray(size -> new String[size]);

		return userSession.getSingleUsageSharedObjectsInBuildLists(designUIDs, project.getUID().getString(), requestedTypes);
	}
}

