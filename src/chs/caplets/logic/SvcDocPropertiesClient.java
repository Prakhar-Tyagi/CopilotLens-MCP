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
 * FEAT14997 - Offline Service Documentation User: kayyagar Date: Oct 12, 2010 Time: 7:28:07 PM
 */
public class SvcDocPropertiesClient extends PropertiesClient
{

	private boolean modelEditable = false;

	/**
	 * @param model
	 */
	public SvcDocPropertiesClient(Model model)
	{
		super(model);
	}

	SvcDocPropertiesClient(Model model, boolean willEditSharedObjects)
	{
		super(model, willEditSharedObjects);
	}

	@Override public boolean isModelEditable()
	{
		return super.isModelEditable();
	}

	@Override
	protected boolean shouldShowEditableConnectionsTab()
	{
		return false;
	}

	@Override public boolean isConnectivityEditable()
	{
		return super.isConnectivityEditable();
	}

	@Override public boolean areGraphicsEditable()
	{
		return super.areGraphicsEditable();
	}

	public boolean allowFixedName()
	{
		return false;
	}

	@Override public boolean allowNameEdit()
	{
		return super.allowNameEdit();
	}

	@Override public boolean allowNameTextAttributesEdit()
	{
		return super.allowNameTextAttributesEdit();
	}

	/**
	 * Does the properties client allow options, module code and/or harness level participation editing?
	 *
	 * @return false - Capture does not allow these.
	 */
	public boolean allowOptionModuleLevelEditing()
	{
		return super.allowOptionModuleLevelEditing();
	}

	/**
	 * @see chs.caplets.shared.properties.PropertiesClient#allowPorts()
	 */
	protected boolean allowPorts()
	{
		return false;
	}

	public boolean isOptionEditingEnabledForDesign()
	{
		return super.isOptionEditingEnabledForDesign();
	}

	public boolean allowModuleEditing()
	{
		return super.allowModuleEditing();
	}

	public boolean allowSymbolControl()
	{
		return true;
	}

	public boolean allowInterconnectMemberControl()
	{
		return false;
	}

	public boolean allowParameterControl()
	{
		return super.allowParameterControl();
	}

	@Override public boolean disableReassign()
	{
		return true;
	}

	@Override public boolean disableRemove()
	{
		return true;
	}

	@Override public boolean disableUpdate()
	{
		return super.disableUpdate();
	}

	@Override public boolean disablePartSelection()
	{
		return super.disablePartSelection();
	}

	@Override public boolean disablePartView()
	{
		return false;
	}

	public void setModelEditable()
	{
		modelEditable = true;
	}
}
