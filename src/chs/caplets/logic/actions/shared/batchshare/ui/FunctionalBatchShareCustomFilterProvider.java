/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.common.IAttributePropertyProvider;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Functional batch share custom filter provider
 */
public class FunctionalBatchShareCustomFilterProvider extends BatchShareCustomFilterProvider
{

	public FunctionalBatchShareCustomFilterProvider(
			Function<Predicate<IAttributePropertyProvider>, Boolean> setFilterPredicate)
	{
		super(setFilterPredicate);
		registerFilter(new OneEndedConductorFilter());
	}
}
