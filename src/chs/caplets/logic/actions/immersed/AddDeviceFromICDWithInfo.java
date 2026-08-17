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
import chs.caplets.logic.actions.AddLibraryPartWithSymbolAction;
import chs.caplets.logic.actions.icdbrowser.AddDeviceFromICDAction;
import chs.caplets.logic.icd.ICDSelection;
import chs.caplets.logic.icd.ICDSelectionHelper;
import chs.cof.icd.IDeviceICD;
import chs.cof.icd.IICD;
import chs.cof.logical.ILogicDesign;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.subsystem.immersed.impl.object.devicemodel.CreateDeviceInfo;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;

/**
 * This class is responsible for adding a device from an ICD (Interface Control Document) with specific information.
 * NOTE: Without symbol is taken care by default implementation of AbstractAddDeviceFromLibraryPartAction
 */
public class AddDeviceFromICDWithInfo extends AddDeviceFromICDAction
{

	@NotNull private ICDSelection m_partSelection;
	@NotNull private ICDSelectionHelper m_selectionHelper = new ICDSelectionHelper();
	@NotNull private CreateDeviceInfo m_deviceInfo;
	private boolean m_partNumberMismatch;

	public AddDeviceFromICDWithInfo(@NotNull ICapletController controller, @NotNull IICD iicd,
			@NotNull ILogicDesign logicDesign)
	{
		super(controller);
		setPartSelection(iicd, logicDesign);
	}

	private void setPartSelection(@NotNull IICD iicd, @NotNull ILogicDesign logicDesign)
	{
		IDeviceICD deviceICD = logicDesign.getDesignICDContainer().constructDeviceICD(iicd);
		m_partSelection = new ICDSelection(deviceICD, logicDesign);
		m_selectionHelper.selectLibraryObjects(deviceICD, m_partSelection, null);
	}

	@Override @NotNull protected ILibraryPartSelection pickLibraryPart()
	{
		return m_partSelection;
	}

	@Override protected AddLibraryPartWithSymbolAction getAddWithSymbolAction(ILibraryPartSelection libraryPart)
	{
		AddICDWithSymbolWithInfoAction action =
				new AddICDWithSymbolWithInfoAction(getController(), libraryPart, m_partSelection);
		action.setDeviceInfo(m_deviceInfo, m_partNumberMismatch);
		return action;
	}

	@Override public boolean isValid()
	{
		return true;
	}

	public void setDeviceInfo(CreateDeviceInfo deviceInfo, boolean partNumberMismatch)
	{
		m_deviceInfo = deviceInfo;
		m_partNumberMismatch = partNumberMismatch;
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
