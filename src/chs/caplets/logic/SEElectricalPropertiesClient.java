/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic;

/**
 * Properties client instance for {@code SEE Electrical}
 */
public class SEElectricalPropertiesClient extends AbstractLogicDerivativePropertiesClient
{

	public SEElectricalPropertiesClient(Model model)
	{
		super(model);
	}

	public SEElectricalPropertiesClient(Model model, boolean willEditSharedObjects)
	{
		super(model, willEditSharedObjects);
	}
}