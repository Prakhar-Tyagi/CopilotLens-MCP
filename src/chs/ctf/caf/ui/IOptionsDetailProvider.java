/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.ctf.caf.ui;

import chs.utilities.CollectionUtils;
import chs.utilities.ui.property.IBooleanProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

/**
 * Implementations of this interface should define handlers for specific property.
 */
public interface IOptionsDetailProvider
{

	@Nullable public IBooleanProperty getWithConductorOption();

	@Nullable public IBooleanProperty getReferenceOption();

	@Nullable public IBooleanProperty getPlaceAsStackOption();

	@Nullable IBooleanProperty getIndividualOption();

	boolean isSymbolSelected();

	/**
	 * Retrieves the mutually exclusive options when the pin panel is selected.
	 * This method returns a collection of options that are mutually exclusive to the specified
	 * source option when the pin panel is selected.
	 *
	 * @param sourceOption the source option that triggered the check.
	 * @return a collection of IBooleanProperty instances representing mutually exclusive options.
	 */
	@NotNull default Collection<IBooleanProperty> getMutuallyExclusiveOptionsWhenPinPanelSelected(
			@NotNull IBooleanProperty sourceOption)
	{
		if (sourceOption.equals(getReferenceOption())) {
			return CollectionUtils.createListNoNulls(getWithConductorOption(), getPlaceAsStackOption());
		}
		if (sourceOption.equals(getWithConductorOption())) {
			return CollectionUtils.createListNoNulls(getReferenceOption(), getPlaceAsStackOption());
		}
		return Set.of();
	}

	/**
	 * Retrieves the mutually exclusive options when the symbol panel is selected.
	 * This method returns a collection of options that are mutually exclusive to the specified
	 * source option when the symbol panel is selected.
	 *
	 * @param sourceOption the source option that triggered the check.
	 * @return a collection of IBooleanProperty instances representing mutually exclusive options.
	 */
	@NotNull default Collection<IBooleanProperty> getMutuallyExclusiveOptionsWhenSymbolPanelSelected(
			@NotNull IBooleanProperty sourceOption)
	{
		if (sourceOption.equals(getReferenceOption())) {
			return CollectionUtils.createListNoNulls(getWithConductorOption());
		}
		if (sourceOption.equals(getWithConductorOption())) {
			return CollectionUtils.createListNoNulls(getReferenceOption());
		}
		return Set.of();
	}
}

