/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024-2026 Siemens
 */

package chs.caplets.logic.actions.inlineassist;

import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.view.assist.SchemConnectionCreator;
import org.jetbrains.annotations.NotNull;

/**
 * This class is responsible for terminating the shield conductor directly to the inline half pin or backhsell pin.
 */
public class ShieldConnector implements IShieldConnector
{

	@Override
	public void connectShield(@NotNull InlineHalfShieldTerminationParams terminationParams)
	{
		IPinList schemInlineHalf = terminationParams.getSchemInlineHalf();
		IShieldConductor inlineHalfShield = terminationParams.getInlineHalfShield();
		IPin schemInlineHalfPin = terminationParams.getSchemInlineHalfPin();

		inlineHalfShield.addPin(schemInlineHalfPin.getConnectivity());
		SchemConnectionCreator schemConnectionCreator =
				new SchemConnectionCreator(schemInlineHalf.getDiagram(), ConductorRouteAction.getInstance());
		schemConnectionCreator
				.shieldConnection(inlineHalfShield, schemInlineHalfPin.getAbsLocation(), schemInlineHalfPin);
	}
}
