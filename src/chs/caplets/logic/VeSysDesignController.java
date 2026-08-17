/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic;

import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.IPropertiesClient;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import org.jetbrains.annotations.NotNull;

/**
 * FEAT13132 - VeSys Packaging.
 * <p>
 * This is the caplet controller used for VeSys Design.
 * <p>
 *
 * @author rjoseph
 */

public class VeSysDesignController extends AbstractLogicDerivativeController
{

	public VeSysDesignController(ICaplet caplet, ILogicDesign design, ISchemDiagram diagram)
	{
		super(caplet, design, diagram);
	}

	@Override @NotNull public IPropertiesClient createPropertiesClient()
	{
		return new VeSysDesignPropertiesClient(getCapletModel());
	}

	@NotNull @Override public IPropertiesClient createPropertiesClientForQep(boolean willLockSharedObject)
	{
		throw new UnsupportedOperationException(
				"This operation is not supported as Quick Access Panel is not supported in VeSys (Essentials)");
	}

	@NotNull @Override public IPropertiesClient createPropertiesClientForQep()
	{
		throw new UnsupportedOperationException(
				"This operation is not supported as Quick Access Panel is not supported in VeSys (Essentials)");
	}
}