/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared;

import chs.cof.logical.cable.ILogicObject;
import chs.cof.security.IDomain;
import chs.common.IUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public interface IMulticoreShareContextProvider
{

	@Nullable IUID getSharedMulticoreUID();

	@Nullable String getSharedMulticoreName();

	@Nullable String getSharedMulticoreRevision();

	boolean isSharedMulticoreNameGenerated();

	@NotNull Map<ILogicObject, IUID> getMulticoreToSharedHierarchyMap();

	@Nullable default Set<IDomain> getSharedDomains()
	{
		return null;
	}
}
