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
import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.actions.icdbrowser.AddParametrizedDeviceFromICDAction;
import chs.caplets.logic.icd.ICDSelection;
import chs.caplets.logic.icd.ICDSelectionHelper;
import chs.cof.icd.IDeviceICD;
import chs.cof.icd.IICD;
import chs.cof.logical.ILogicDesign;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.subsystem.immersed.impl.object.devicemodel.CreateDeviceInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;

/**
 * An action to add a parameterized device from an ICD, with the additional information
 */
public class AddParameterizedDeviceFromICDWithInfoAction extends AddParametrizedDeviceFromICDAction
{

	@NotNull private ILogicDesign m_design;
	@NotNull private ICDSelection m_partSelection;
	@NotNull private ICDSelectionHelper m_selectionHelper = new ICDSelectionHelper();
	@NotNull protected CreateDeviceInfo m_deviceInfo;

	public AddParameterizedDeviceFromICDWithInfoAction(@NotNull ICapletController controller, @NotNull IICD icd,
			@NotNull ILogicDesign design)
	{
		super(controller);
		m_design = design;
		setPartSelection(icd);
	}

	@Override public boolean isValid()
	{
		return true;
	}

	private void setPartSelection(@NotNull IICD mIcd)
	{
		IDeviceICD deviceICD = m_design.getDesignICDContainer().constructDeviceICD(mIcd);
		m_partSelection = new ICDSelection(deviceICD, m_design);
		m_selectionHelper.selectLibraryObjects(deviceICD, m_partSelection, null);
	}

	@Nullable @Override protected ILibraryPartSelection pickLibraryPart()
	{
		return m_partSelection;
	}

	public void setDeviceInfo(CreateDeviceInfo deviceInfo, boolean partNumberMismatch)
	{
		m_deviceInfo = deviceInfo;
	}

	@Override protected IActionEnum activateAddWithoutSymbol(ActionEvent e, ILibraryPartSelection libraryPart)
	{
		AddParameterizedDeviceFromLibraryPartWithInfoAction action =
				new AddParameterizedDeviceFromLibraryPartWithInfoAction(getController(), libraryPart);
		action.setDeviceInfo(m_deviceInfo);
		subAction = action;
		return action.onActivate(e);
	}
}
