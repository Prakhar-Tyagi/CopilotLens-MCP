/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared.autoshare;

import chs.caplets.logic.actions.shared.OperandShareabilityStatus;

import java.util.EnumSet;

public class DeltaShareIntoOperandStrategy extends FetchOffAutoShareIntoOperandStrategy
{
	@Override protected EnumSet<OperandShareabilityStatus> getSuccessShareStatuses()
	{
		return EnumSet.of(OperandShareabilityStatus.Shareable,
				OperandShareabilityStatus.PartialPlacedMulticore,
				OperandShareabilityStatus.EmptyMulticore);
	}

}
