/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic;

import chs.caplets.shared.properties.PropertiesClient;

/**
 * An abstract base class that provides functionality for handling logic for logic derivatives.
 */
public abstract class AbstractLogicDerivativePropertiesClient extends PropertiesClient
{

	AbstractLogicDerivativePropertiesClient(Model model)
	{
		super(model);
	}

	AbstractLogicDerivativePropertiesClient(Model model, boolean willEditSharedObjects)
	{
		super(model, willEditSharedObjects);
	}

	public boolean allowFixedName()
	{
		return false;
	}

	/**
	 * Does the properties client allow options, module code and/or harness level participation editing?
	 *
	 * @return false - Capture does not allow these.
	 */
	public boolean allowOptionModuleLevelEditing()
	{
		return !containNonOptionedObjects(m_propertiedSet);
	}

	public boolean isOptionEditingEnabledForDesign()
	{
		return true;
	}

	public boolean allowModuleEditing()
	{
		return false;
	}

	public boolean allowInterconnectMemberControl()
	{
		return false;
	}

	public boolean allowParameterControl()
	{
		return false;
	}
}