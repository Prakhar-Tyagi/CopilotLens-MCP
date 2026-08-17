/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.merge;

import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.ISingleLine;
import chs.cof.logical.cable.ISingleLineEnd;
import chs.utility.helpers.SingleLineHelper;
import org.jetbrains.annotations.NotNull;

/**
 * SingleLineMerger class to merge two single lines.
 */
public class SingleLineMerger extends HighwayMerger
{

	public SingleLineMerger(ILogicObject sourceObject, ILogicObject targetObject,
			@NotNull IMergeActionChangeReporter reporter)
	{
		super(sourceObject, targetObject, reporter);
	}

	@Override void mergeChildrenConnectivity(ILogicObject sourceLogicObject, ILogicObject targetLogicObject)
	{
		ISingleLine sourceSingleLine = (ISingleLine) sourceLogicObject;
		ISingleLine targetSingleLine = (ISingleLine) targetLogicObject;

		SingleLineHelper.transferSingleLineEnds(sourceSingleLine, targetSingleLine);
		sourceSingleLine.removeAllEnds();
	}
}
