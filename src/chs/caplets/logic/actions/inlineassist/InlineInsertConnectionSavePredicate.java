/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.inlineassist;

import chs.caplets.logic.actions.ui.IConductorConnectionChangeSavePredicate;
import chs.cof.logical.ILogicDesign;
import chs.common.IDesignDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Implementation of {@link IConductorConnectionChangeSavePredicate} for {@link InlineInsertManageConnections}
 */
public class InlineInsertConnectionSavePredicate implements IConductorConnectionChangeSavePredicate
{

	@Override public boolean shouldSaveForeignDesigns()
	{
		return false;
	}

	@Override public boolean isCurrentDesign(IDesignDescriptor designDescriptor)
	{
		return false;
	}

	@NotNull @Override public Collection<ILogicDesign> getOpenedDesignsToBeSaved()
	{
		return List.of();
	}

	@Override public void doPostSave()
	{

	}
}
