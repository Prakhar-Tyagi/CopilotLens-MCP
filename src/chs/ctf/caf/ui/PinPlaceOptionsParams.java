/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.ctf.caf.ui;

import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.IFunction;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.ISharedFunction;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedSplice;
import org.jetbrains.annotations.NotNull;

/**
 * Class used to configure settings to be used for building panel containing options for pin placement -
 * for pin list related dialogs, actions, etc.
 */
public class PinPlaceOptionsParams extends AbstractPlacementOptionParams
{

	/**
	 * Constructor to initialize PinPlaceOptionsParams with a given IPinList.
	 * This constructor is used when the pin list is provided and the options need to be configured
	 * based on the type of pin list.
	 *
	 * @param pinList the pin list to be used for configuring the options.
	 */
	public PinPlaceOptionsParams(@NotNull IPinList pinList)
	{
		if (pinList instanceof IBlockDevice) {
			enableIndividuallyOption(true);
			enableAsStackOption(true);
			enableAsGroupOption(true);
			enableShowUsedPinsOptionIfValid(pinList.getProject());
			return;
		}
		if (pinList instanceof IFunction) {
			enableIndividuallyOption(true);
			enableAsGroupOption(true);
			return;
		}
		if (isRingTerminal(pinList)) {
			enableAsReferenceOption(pinList.canHaveReferencePin());
			return;
		}
		enableIndividuallyOption(true);
		enableAsStackOption(true);
		enableAsGroupOption(true);
		enableAsReferenceOption(pinList.canHaveReferencePin());
	}

	/**
	 * Constructor to initialize PinPlaceOptionsParams with a given CreationType.
	 * This constructor is used when the creation type is provided and the options need to be configured
	 * based on the type of creation.
	 *
	 * @param creationType the creation type to be used for configuring the options.
	 */
	public PinPlaceOptionsParams(@NotNull CreationType creationType)
	{
		if (creationType == CreationType.FOR_STACK_PIN) {
			enableIndividuallyOption(true);
			enableAsGroupOption(true);
		}
	}

	/**
	 * Constructor to initialize PinPlaceOptionsParams with a given ISharedPinList.
	 * This constructor is used when the shared pin list is provided and the options need to be configured
	 * based on the type of shared pin list.
	 *
	 * @param sharedPinList the shared pin list to be used for configuring the options.
	 */
	public PinPlaceOptionsParams(@NotNull ISharedPinList sharedPinList)
	{
		enableLoadSharedPinInfoOptionIfValid(sharedPinList);
		if (sharedPinList instanceof ISharedSplice) {
			return;
		}
		if (sharedPinList instanceof ISharedFunction) {
			enableIndividuallyOption(true);
			enableAsGroupOption(true);
			return;
		}
		if (isSharedRingTerminal(sharedPinList)) {
			enableAsReferenceOption(true);
			return;
		}
		enableIndividuallyOption(true);
		enableAsStackOption(true);
		enableAsGroupOption(true);
		enableAsReferenceOption(true);
	}
}
