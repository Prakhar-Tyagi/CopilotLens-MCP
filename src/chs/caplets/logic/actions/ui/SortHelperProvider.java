/*
 * Copyright 2016 Mentor Graphics Corporation
 * All Rights Reserved
 * 
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.ui;

import chs.caplets.logic.actions.ManageConnectorConnectionsInfo;
import chs.cof.parts.ILibraryCavityContainer;
import chs.common.IDesignDescriptor;
import chs.utility.ui.PinConductorConnectionSortHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Supplier;

public class SortHelperProvider implements Supplier<PinConductorConnectionSortHelper>
{

	private PinConductorConnectionSortHelper sortHelper;
	private ILibraryCavityContainer libraryCavityContainer;
	private String name;

	private Collection<IDesignDescriptor> designsInScope;

	SortHelperProvider(@Nullable ILibraryCavityContainer libraryCavityContainer, String name,
			Collection<IDesignDescriptor> designsInScope)
	{
		this.libraryCavityContainer = libraryCavityContainer;
		this.name = name;
		this.designsInScope = designsInScope;
	}

	void resetData(@Nullable Collection<ManageConnectorConnectionsInfo> data)
	{
		if (data == null) {
			sortHelper = null;
		}
		else {
			sortHelper = new PinConductorConnectionSortHelper(data, name, designsInScope, libraryCavityContainer);
		}
	}

	@Override public PinConductorConnectionSortHelper get()
	{
		return sortHelper;
	}
}