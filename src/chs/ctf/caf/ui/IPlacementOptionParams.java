/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.ctf.caf.ui;

import chs.cof.project.IProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface used to configure settings to be used for building panel containing options for pinList placement
 */
public interface IPlacementOptionParams
{
	@NotNull IPlacementOptionParams enableIndividuallyOption(boolean isEnabled);

	@NotNull IPlacementOptionParams enableAutoGenerateOption(boolean isEnabled);

	@NotNull IPlacementOptionParams enableAsStackOption(boolean isEnabled);

	@NotNull IPlacementOptionParams enableAsGroupOption(boolean isEnabled);

	@NotNull IPlacementOptionParams enableWithConductorOption(boolean isEnabled, @NotNull IProject project);

	@NotNull IPlacementOptionParams enableAsReferenceOption(boolean isEnabled);

	@NotNull IPlacementOptionParams enableLoadSharedPinInfoOption(boolean isEnabled);

	@NotNull IPlacementOptionParams enableShowUsedPinsOption(boolean isEnabled);

	boolean isIndividuallyOptionEnabled();

	boolean isAutoGenerateOptionEnabled();

	boolean isAsStackOptionEnabled();

	boolean isAsGroupOptionEnabled();

	boolean isWithConductorOptionEnabled();

	boolean isAsReferenceOptionEnabled();

	boolean isLoadSharedPinInfoOptionEnabled();

	boolean isShowUsedPinsOptionEnabled();
}
