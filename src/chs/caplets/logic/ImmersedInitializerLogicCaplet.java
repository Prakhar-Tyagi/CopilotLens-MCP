/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic;

import chs.caf.CAFUtils;
import chs.caf.helpers.ui.std.IImmersedMDIManager;
import chs.subsystem.immersedapp.IImmersedInitializer;
import chs.utilities.CommonUtils;

public class ImmersedInitializerLogicCaplet implements IImmersedInitializer
{

	@Override public void initialize()
	{
		IImmersedMDIManager manager =
				CommonUtils.cast(CAFUtils.getInstance().getWindowMgr().getMDIManager(), IImmersedMDIManager.class);
		if (manager != null) {
			manager.addSupportedViewType(View.class);
		}
	}
}
