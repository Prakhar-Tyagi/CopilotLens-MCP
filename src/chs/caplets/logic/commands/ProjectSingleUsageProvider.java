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
import org.jetbrains.annotations.NotNull;

/**
 * Single usage provider for project-wide scope.
 * This implementation retrieves single usage shared objects that are used
 * only once across the entire project, with server-side filtering.
 */
class ProjectSingleUsageProvider implements ISingleUsageProvider
{
	ProjectSingleUsageProvider()
	{
	}

	@NotNull @Override public SingleUsageSharedObjectInfo[] getSingleUsages(
			@NotNull SingleUsageSharedObjectInfo.SharedObjectType[] requestedTypes, @NotNull IUserSession userSession,
			IProject project) throws UserSessionException
	{
		return userSession.getSingleUsageSharedObjectsInProject(project.getUID().getString() , requestedTypes);
	}
}

