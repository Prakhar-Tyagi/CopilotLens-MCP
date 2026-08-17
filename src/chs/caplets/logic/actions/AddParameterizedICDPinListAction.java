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

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caplets.logic.icd.ICDPlacementHelper;
import chs.caplets.logic.icd.PlaceICDPersistenceHandler;
import chs.cof.icd.IDeviceICD;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.ctf.caf.utils.IPinProxy;
import chs.utility.gfx.IDrawingComponentOwner;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AddParameterizedICDPinListAction extends AddParameterizedPinListAction implements IICDProviderAction
{

	private IDeviceICD m_icd;
	private boolean withConductor;

	public AddParameterizedICDPinListAction(ICapletController controller,
			IPinList pinlist, List<IAbstractPin> pins,
			boolean autogenerate, boolean reference, boolean placeAsStack, boolean placeAsGroup,
			List<IPinProxy> pinProxies, IDeviceICD icd, boolean withConductor)

	{
		super(controller, pinlist, pins, autogenerate, reference, placeAsStack, placeAsGroup, pinProxies);
		m_icd = icd;
		this.withConductor = withConductor;
	}

	@Override protected void cleanup()
	{
	}

	@Override public boolean onTerminate(boolean successful)
	{
		boolean success = super.onTerminate(successful);
		if (success) {
			ISchemDiagram diagram = getSchemDiagram();
			if (createdSchematic != null && diagram != null && m_icd != null) {
				PlaceICDPersistenceHandler handler = new PlaceICDPersistenceHandler(diagram, withConductor);
				ICDPlacementHelper.updateICDNameRoutingAndProperties(createdSchematic, diagram, m_icd.getRole(), m_icd,
						handler);
			}
		}
		super.cleanup();
		return success;
	}

	protected ISchemDiagram getSchemDiagram()
	{
		return (ISchemDiagram) ((IDrawingComponentOwner) CAFUtils.getInstance().getActiveCapletView()).getSheet();
	}

	@Nullable public IDeviceICD getICD()
	{
		return m_icd;
	}

	@Override public boolean shouldDisableUndoForNonUndoableChanges()
	{
		return true;
	}
}
