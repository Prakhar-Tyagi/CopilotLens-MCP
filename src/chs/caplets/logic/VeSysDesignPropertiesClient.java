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
 * FEAT13132 - VeSys Packaging.
 * <p/>
 * This is the properties-client used for VeSys Design.
 * <p/>
 *
 * @author rjoseph
 */
public class VeSysDesignPropertiesClient extends AbstractLogicDerivativePropertiesClient
{

	public VeSysDesignPropertiesClient(Model model)
	{
		super(model);
	}

	public VeSysDesignPropertiesClient(Model model, boolean willEditSharedObjects)
	{
		super(model, willEditSharedObjects);
	}
}