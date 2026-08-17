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
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caplets.logic.actions.shared.AddSharedICDAction;
import chs.caplets.logic.icd.ICDSelection;
import chs.caplets.logic.icd.ICDSelectionHelper;
import chs.cof.draw.IGfxObject;
import chs.cof.icd.IDeviceICD;
import chs.cof.icd.IICD;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.IPinList;
import chs.subsystem.immersed.impl.object.devicemodel.CreateDeviceInfo;
import chs.utilities.Pair;
import chs.utility.helpers.CompositePinConnectivityFinder;
import chs.utility.ui.SymbolProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.List;

/**
 * Action for adding a shared ICD to the logic design when sufficient information is available to support
 */
public class AddSharedICDWithInfoAction extends AddSharedICDAction
{

	@NotNull private IICD m_icd;
	@NotNull private CreateDeviceInfo m_deviceInfo;
	@NotNull private ICDSelectionHelper m_selectionHelper = new ICDSelectionHelper();
	@NotNull private ILogicDesign m_logicDesign;
	private boolean m_partNumberMismatch;

	public AddSharedICDWithInfoAction(ICapletController controller, @NotNull IICD iicd,
			@NotNull ILogicDesign logicDesign)
	{
		super(controller);
		m_icd = iicd;
		m_logicDesign = logicDesign;
	}

	@Override @NotNull protected SelectSet getSelections()
	{
		SelectSet set = new SelectSet();
		set.add(new Selection(m_icd));
		return set;
	}

	public void setDeviceInfo(CreateDeviceInfo deviceInfo, boolean partNumberMismatch)
	{
		m_deviceInfo = deviceInfo;
		m_partNumberMismatch = partNumberMismatch;
	}

	@Override @NotNull protected IGfxObject createParamObject(Point p1, Point p2)
	{
		IGfxObject paramObject = super.createParamObject(p1, p2);
		m_deviceInfo.setProperties(m_schemDevice, m_partNumberMismatch);
		return paramObject;
	}

	@Nullable @Override
	protected chs.cof.logical.cable.IPinList generatePinList(List<Pair<IPinList, SymbolProxy>> symbolInstancesToUpdate,
			List<Pair<IPinList, SymbolProxy>> instancesToUpdate, CompositePinConnectivityFinder connectivityFinder)
	{
		chs.cof.logical.cable.IPinList iPinList =
				super.generatePinList(symbolInstancesToUpdate, instancesToUpdate, connectivityFinder);
		if (iPinList != null) {
			m_deviceInfo.setProperties(iPinList, m_partNumberMismatch);
		}
		return iPinList;
	}

	@Override protected void setPartSelection()
	{
		IDeviceICD deviceICD = m_logicDesign.getDesignICDContainer().constructDeviceICD(m_icd);
		ICDSelection icdPartSelection = new ICDSelection(deviceICD, m_logicDesign);
		m_selectionHelper.selectLibraryObjects(deviceICD, icdPartSelection, null);
		partSelection = icdPartSelection;
	}
}
