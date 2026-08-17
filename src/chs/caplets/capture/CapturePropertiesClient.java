/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */
package chs.caplets.capture;

import chs.caplets.logic.Model;
import chs.caplets.shared.properties.PropertiesClient;
import chs.cof.draw.IText;

/**
 * This class is a tailored properties client for {@link CaptureCaplet}
 */
public class CapturePropertiesClient extends PropertiesClient
{

	/**
	 * @param model
	 */
	public CapturePropertiesClient(Model model)
	{
		super(model);
	}

	CapturePropertiesClient(Model model, boolean willEditSharedObjects)
	{
		super(model, willEditSharedObjects);
	}


	public IText getShortDescriptionRepresentation()
	{
		return null;
	}

	public IText getShortDescriptionRepresentationWithCreate()
	{
		return null;
	}

	/**
	 * @see chs.caplets.shared.properties.PropertiesClient#allowOptionEditing()
	 */
	public boolean allowOptionEditing()
	{
		if (containNonOptionedObjects(m_propertiedSet)) {
			return false;
		}

		return true;
	}

	/**
	 * Does the properties client allow options, module code and/or harness level participation editing?
	 *
	 * @return false - Capture does not allow these.
	 */
	public boolean allowOptionModuleLevelEditing()
	{
		if (containNonOptionedObjects(m_propertiedSet)) {
			return false;
		}

		return true;
	}

	/**
	 * @see chs.caplets.shared.properties.PropertiesClient#allowPorts()
	 */
	protected boolean allowPorts()
	{
		return false;
	}

	public boolean allowSymbolControl()
	{
		return false;
	}

	public boolean allowInterconnectMemberControl()
	{
		return false;
	}
}