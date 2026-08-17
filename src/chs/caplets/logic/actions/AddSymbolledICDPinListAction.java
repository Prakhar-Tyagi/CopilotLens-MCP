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
import chs.caplets.logic.icd.PlaceICDPersistenceHandler;
import chs.cof.draw.IGfxObject;
import chs.cof.icd.IDeviceICD;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.ISymbolDef;
import chs.common.ISymbolledPin;
import chs.common.IUID;

import java.awt.event.MouseEvent;
import java.util.Map;

public class AddSymbolledICDPinListAction extends AddSymbolledPinListAction
{

	private IDeviceICD m_icd;
	protected chs.cof.logical.schem.IPinList diagramPinList = null;
	private boolean withConductor;

	public AddSymbolledICDPinListAction(ICapletController controller, IPinList pinlist,
			ISymbolDef symDef, IBlock block,
			Map<IUID, ISymbolledPin> symbolledPinMap, boolean reference, IDeviceICD icd, boolean withConductor)
	{
		super(controller, pinlist, symDef, block, symbolledPinMap, reference);
		m_icd = icd;
		this.withConductor = withConductor;
	}

	@Override protected boolean addInstance()
	{
		boolean addInstance = super.addInstance();
		diagramPinList = m_pinlist;
		return addInstance;
	}

	@Override public boolean onTerminate(boolean successful)
	{
		boolean sucess = super.onTerminate(successful);
		if (sucess && diagramPinList != null && m_icd != null) {
			ISchemDiagram diagram = getModel().getDiagram();
			PlaceICDPersistenceHandler handler = new PlaceICDPersistenceHandler(diagram, withConductor);
			ICDPlacementHelper.updateICDNameRoutingAndProperties(diagramPinList, diagram, m_icd.getRole(), m_icd,
					handler);
		}
		return sucess;
	}

	@Override protected IGfxObject updateTransients()
	{
		return updateTransients(false);
	}

	protected IGfxObject updateTransients(boolean updateTracesOnly)
	{
		super.updateTransients(updateTracesOnly);
		AddICDWithSymbolAction.updateICDGfx(m_dynamics, m_placementObject, m_icd, getModel().getDiagram());
		return m_placementObject;
	}

	@Override public void mouseMoved(MouseEvent e)
	{
		super.mouseMoved(e);
		updateTransients(true);
	}

	@Override public boolean shouldDisableUndoForNonUndoableChanges()
	{
		return true;
	}
}
