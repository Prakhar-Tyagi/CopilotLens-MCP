/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.ctf.caf.ui;

import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.project.IProject;
import chs.utility.ICDUtils;
import chs.utility.logic.PinUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

/**
 * Class used to configure settings to be used for building panel containing options for pinList placement
 */
public abstract class AbstractPlacementOptionParams implements IPlacementOptionParams
{

	private boolean isIndividuallyOptionEnabled = false;
	private boolean isAutoGenerateOptionEnabled = false;
	private boolean isAsStackOptionEnabled = false;
	private boolean isAsGroupOptionEnabled = false;
	private boolean isWithConductorOptionEnabled = false;
	private boolean isAsReferenceOptionEnabled = false;
	private boolean isLoadSharedPinInfoOptionEnabled = false;
	private boolean isShowUsedPinsOptionEnabled = false;
	private static final Set<PinListTypeEnum> supportedTypesForLoadSharedDetails =
			Set.of(PinListTypeEnum.TypeInlinePlug,
					PinListTypeEnum.TypeInlineJack,
					PinListTypeEnum.TypePlug,
					PinListTypeEnum.TypeJack,
					PinListTypeEnum.TypeDevice,
					PinListTypeEnum.TypeGround,
					PinListTypeEnum.TypeRingTerminal,
					PinListTypeEnum.TypeFunction);

	@Override @NotNull public IPlacementOptionParams enableIndividuallyOption(boolean isEnabled)
	{
		isIndividuallyOptionEnabled = isEnabled;
		return this;
	}

	@Override @NotNull public IPlacementOptionParams enableAutoGenerateOption(boolean isEnabled)
	{
		isAutoGenerateOptionEnabled = isEnabled;
		return this;
	}

	@Override @NotNull public IPlacementOptionParams enableAsStackOption(boolean isEnabled)
	{
		isAsStackOptionEnabled = isEnabled;
		return this;
	}

	@Override @NotNull public IPlacementOptionParams enableAsGroupOption(boolean isEnabled)
	{
		isAsGroupOptionEnabled = isEnabled;
		return this;
	}

	@NotNull @Override public IPlacementOptionParams enableWithConductorOption(boolean isEnabled, @NotNull IProject project)
	{
		isWithConductorOptionEnabled = isEnabled && ICDUtils.getGenerateConductorsOnICDPins(project);
		return this;
	}

	@Override @NotNull public IPlacementOptionParams enableAsReferenceOption(boolean isEnabled)
	{
		isAsReferenceOptionEnabled = isEnabled;
		return this;
	}

	@Override @NotNull public IPlacementOptionParams enableLoadSharedPinInfoOption(boolean isEnabled)
	{
		isLoadSharedPinInfoOptionEnabled = isEnabled;
		return this;
	}

	@Override @NotNull public IPlacementOptionParams enableShowUsedPinsOption(boolean isEnabled)
	{
		isShowUsedPinsOptionEnabled = isEnabled;
		return this;
	}

	@Override public boolean isIndividuallyOptionEnabled()
	{
		return isIndividuallyOptionEnabled;
	}

	@Override public boolean isAutoGenerateOptionEnabled()
	{
		return isAutoGenerateOptionEnabled;
	}

	@Override public boolean isAsStackOptionEnabled()
	{
		return isAsStackOptionEnabled;
	}

	@Override public boolean isAsGroupOptionEnabled()
	{
		return isAsGroupOptionEnabled;
	}

	@Override public boolean isWithConductorOptionEnabled()
	{
		return isWithConductorOptionEnabled;
	}

	@Override public boolean isAsReferenceOptionEnabled()
	{
		return isAsReferenceOptionEnabled;
	}

	@Override public boolean isLoadSharedPinInfoOptionEnabled()
	{
		return isLoadSharedPinInfoOptionEnabled;
	}

	@Override public boolean isShowUsedPinsOptionEnabled()
	{
		return isShowUsedPinsOptionEnabled;
	}

	protected boolean isRingTerminal(@NotNull IPinList pinList)
	{
		return pinList instanceof IConnector && ((IConnector) pinList).isRingTerminal();
	}

	protected boolean isSharedRingTerminal(@NotNull ISharedPinList sharedPinList)
	{
		return sharedPinList instanceof ISharedConnector && sharedPinList.getType() == PinListTypeEnum.TypeRingTerminal;
	}

	protected void enableLoadSharedPinInfoOptionIfValid(@NotNull ISharedPinList sharedPinList)
	{
		if (isValidForShowSharedDetails(sharedPinList.getType())) {
			enableLoadSharedPinInfoOption(true);
		}
	}

	protected boolean isValidForShowSharedDetails(@NotNull PinListTypeEnum plType)
	{
		return supportedTypesForLoadSharedDetails.contains(plType);
	}

	protected void enableShowUsedPinsOptionIfValid(@Nullable IProject project)
	{
		enableShowUsedPinsOption(PinUtils.allowDuplicatePinsOnDesign(project));
	}
}