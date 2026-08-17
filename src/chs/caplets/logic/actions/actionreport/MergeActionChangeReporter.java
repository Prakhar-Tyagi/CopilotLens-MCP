/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.actionreport;

import chs.common.IAttributePropertyProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * base class for merge comparison
 */
public abstract class MergeActionChangeReporter implements IMergeActionChangeReporter
{

	protected List<IMergeComparison<IMergeActionChange, IAttributePropertyProvider>> mChangeComparisonObjects;

	protected MergeActionChangeReporter()
	{
		mChangeComparisonObjects = new ArrayList<>();
	}

	@NotNull @Override public IMergeComparison<IMergeActionChange, IAttributePropertyProvider> createComparison()
	{
		IMergeComparison<IMergeActionChange, IAttributePropertyProvider> mergeComparison = new MergeComparison(null);
		mChangeComparisonObjects.add(mergeComparison);
		return mergeComparison;
	}
}
