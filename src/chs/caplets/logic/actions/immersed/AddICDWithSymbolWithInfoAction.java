/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions.immersed;

import chs.caf.caplet.ICapletController;
import chs.caplets.logic.actions.AddICDWithSymbolAction;
import chs.cof.parts.partselector.IICDSelection;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.subsystem.immersed.impl.object.devicemodel.CreateDeviceInfo;
import org.jetbrains.annotations.NotNull;

/**
 * This class extends the AddICDWithSymbolAction to provide additional functionality for adding an ICD with symbol
 */
public class AddICDWithSymbolWithInfoAction extends AddICDWithSymbolAction
{

	@NotNull private CreateDeviceInfo m_deviceInfo;
	private boolean m_partNumberMismatch;

	public AddICDWithSymbolWithInfoAction(ICapletController controller,
			ILibraryPartSelection part, IICDSelection selection)
	{
		super(controller, part, selection);
	}

	public void setDeviceInfo(@NotNull CreateDeviceInfo deviceInfo, boolean partNumberMismatch)
	{
		m_deviceInfo = deviceInfo;
		m_partNumberMismatch = partNumberMismatch;
	}

	@Override protected boolean addInstance()
	{
		boolean addInstance = super.addInstance();
		if (m_pinlist != null) {
			m_deviceInfo.setProperties(m_pinlist, m_partNumberMismatch);
		}
		return addInstance;
	}
}
