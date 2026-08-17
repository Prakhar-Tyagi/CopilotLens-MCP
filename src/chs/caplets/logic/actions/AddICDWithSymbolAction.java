/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.caplets.logic.icd.ICDPlacementHelper;
import chs.cof.draw.IGfxObject;
import chs.cof.icd.IDeviceICD;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.parts.partselector.IICDSelection;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.symbol.ISymbolDef;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.IDynamicGfxService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;
import java.util.List;

public class AddICDWithSymbolAction extends AddLibraryPartWithSymbolAction
{

	private IICDSelection m_selection;

	public AddICDWithSymbolAction(ICapletController controller, ILibraryPartSelection part, IICDSelection selection)
	{
		super(controller, part);
		m_selection = selection;
	}

	@Override protected IGfxObject updateTransients()
	{
		return updateTransients(false);
	}

	protected IGfxObject updateTransients(boolean updateTracesOnly)
	{
		if (!updateTracesOnly) {
			super.updateTransients();
		}
		updateICDGfx(m_dynamics, m_placementObject, m_selection.getICD(), getModel().getDiagram());
		return m_placementObject;
	}

	public static void updateICDGfx(IDynamicGfxService dynamicGfxService, IGfxObject placementObject,
			@Nullable IDeviceICD icd, ISchemDiagram diagram)
	{
		dynamicGfxService.removeAllTransientGfx();
		if (placementObject != null) {
			dynamicGfxService.addTransientGfx(placementObject);
		}
		if (placementObject instanceof IPinList) {
			IPinList pinList = (IPinList) placementObject;
			if (icd != null) {
				List<IDynamicGfx> dynamicGfxs = ICDPlacementHelper.updateNetTraces(pinList, icd, diagram, null);
				for (IDynamicGfx gfxObj : dynamicGfxs) {
					dynamicGfxService.addTransientGfx(gfxObj);
				}
			}
		}
	}

	@NotNull @Override protected IPlacementOptionParams createPlacementOptionParams(@NotNull ISymbolDef symDef)
	{
		IPlacementOptionParams params = super.createPlacementOptionParams(symDef);
		params.enableWithConductorOption(true, getCurrentProject());
		return params;
	}

	@Override public void mouseMoved(MouseEvent e)
	{
		super.mouseMoved(e);
		updateTransients(true);
	}
}
