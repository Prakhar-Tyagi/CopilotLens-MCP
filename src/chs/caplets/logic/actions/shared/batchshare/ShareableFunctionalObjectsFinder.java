/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.shared.ISharedFunctionConductor;
import chs.cof.logical.shared.ISharedFunctionMessage;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.project.IProject;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * To find, group and map the functional objects of given designs to functional shared objects in scope based on provided share criteria
 */
public class ShareableFunctionalObjectsFinder extends AbstractShareableObjectsFinder
{

	public ShareableFunctionalObjectsFinder(@NotNull IProject project, @NotNull Set<ILogicDesign> designsInScope,
			@NotNull Collection<ISharedObject> sharedObjectsInScope, @NotNull IObjectInfoProvider objectInfoProvider)
	{
		super(project, designsInScope, sharedObjectsInScope, objectInfoProvider);
	}

	/**
	 * Provides functional shared objects present in the project related to specified shareable entity type
	 *
	 * @param type shareable entity type
	 * @return collection of functional shared objects related to specified shareable entity type
	 */
	@NotNull @Override protected Set<ISharedObject> getSharedObjects(@NotNull ShareableEntityTypeEnum type)
	{
		if (ShareableEntityTypeEnum.FUNCTION_MESSAGE.equals(type)) {
			return m_sharedObjectsInScope.stream()
					.filter(ISharedFunctionMessage.class::isInstance)
					.map(ISharedFunctionMessage.class::cast)
					.collect(Collectors.toSet());
		}
		if (ShareableEntityTypeEnum.FUNCTION_SIGNAL.equals(type)) {
			return m_sharedObjectsInScope.stream()
					.filter(ISharedFunctionConductor.class::isInstance)
					.map(ISharedFunctionConductor.class::cast)
					.collect(Collectors.toSet());
		}

		return Collections.emptySet();
	}
}