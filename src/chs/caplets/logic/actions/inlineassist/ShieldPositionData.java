/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions.inlineassist;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class to hold the position of a shield and its terminations for the place inline action.
 */
public class ShieldPositionData
{

	private final List<ShieldTerminationInfo> mShieldTerminations = new ArrayList<>();

	public void addTermination(@NotNull ShieldTerminationInfo info)
	{
		mShieldTerminations.add(info);
	}

	@NotNull
	public List<ShieldTerminationInfo> getTerminations()
	{
		return Collections.unmodifiableList(mShieldTerminations);
	}
}
