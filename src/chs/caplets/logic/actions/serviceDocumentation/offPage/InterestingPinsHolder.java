/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage;

import chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration.IMatedPinListGeneratorProvider;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IPin;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

public class InterestingPinsHolder
{

	private final Set<IPin> pinsOfInterest;
	private final Predicate<ILogicObject> filterOfInterest;
	private IMatedPinListGeneratorProvider matedPlugGeneratorProvider;

	public InterestingPinsHolder(Predicate<ILogicObject> filterOfInterest,
			IMatedPinListGeneratorProvider matedPlugGeneratorProvider)
	{
		pinsOfInterest = new LinkedHashSet<>();
		this.filterOfInterest = filterOfInterest;
		this.matedPlugGeneratorProvider = matedPlugGeneratorProvider;
	}

	@NotNull public Set<IPin> getPinsOfInterest()
	{
		return pinsOfInterest;
	}

	@NotNull public Predicate<ILogicObject> getFilterOfInterest()
	{
		return filterOfInterest;
	}

	@NotNull public IMatedPinListGeneratorProvider getMatedPinListGeneratorProvider()
	{
		return matedPlugGeneratorProvider;
	}

	public void add(IPin pin)
	{
		pinsOfInterest.add(pin);
	}
}
