/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui.detailsPane;

import chs.cof.logical.cable.IFunctionBaseConductor;
import chs.cof.logical.cable.ILogicObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Instance details pane for functional objects
 */
public class FunctionalInstanceDetailsPane extends AbstractInstanceDetailPane
{

	public FunctionalInstanceDetailsPane(
			@Nullable ILogicObject selectedObject)
	{
		super(selectedObject);
	}

	@NotNull @Override protected DetailsTableInfo createTableInfo(@Nullable ILogicObject selectedObject)
	{
		// expecting selected object as null or selected object to be instance of function signal/function message
		// as batch share is currently supported only for function signal/function message
		assert selectedObject == null || selectedObject instanceof IFunctionBaseConductor;

		return new FunctionBaseConductorDetailsTableInfo();
	}
}
