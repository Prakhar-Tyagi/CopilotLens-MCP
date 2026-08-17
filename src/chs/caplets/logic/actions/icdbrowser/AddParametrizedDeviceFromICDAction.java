/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.logic.actions.icdbrowser;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.browser.ICDBrowserActionHelper;
import chs.caplets.logic.actions.AbstractAddDeviceFromLibraryPartAction;
import chs.caplets.logic.actions.AddDeviceWithPinsFromLibrary;
import chs.caplets.logic.actions.AddParameterizedICDPinListAction;
import chs.caplets.logic.actions.AddSymbolledICDPinListAction;
import chs.caplets.logic.actions.AddSymbolledPinListAction;
import chs.caplets.logic.actions.CreateParameterizedObjectAction;
import chs.cof.icd.IDeviceICD;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IPinList;
import chs.cof.parts.partselector.IICDSelection;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.ISymbolDef;
import chs.common.ISymbolledPin;
import chs.common.IUID;
import chs.ctf.caf.utils.IPinProxy;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;

public class AddParametrizedDeviceFromICDAction extends AbstractAddDeviceFromLibraryPartAction
{

	public AddParametrizedDeviceFromICDAction(ICapletController controller)
	{
		super(controller);
	}

	protected ILibraryPartSelection getPartSelection()
	{
		return ICDBrowserActionHelper.getSelectedBrowserPart();
	}

	protected IActionEnum activateAddWithSymbol(ActionEvent e, ILibraryPartSelection libraryPart)
	{
		AddDeviceWithPinsFromLibrary
				action = new AddDeviceWithPinsFromLibrary(getController(), libraryPart)
		{
			@Override
			protected CreateParameterizedObjectAction createAddParameterizedPinListAction(ICapletController controller,
					IPinList pinlist, List<IAbstractPin> pins, boolean autogenerate, boolean reference,
					boolean placeAsStack, boolean placeAsGroup, List<IPinProxy> pinProxies, boolean withConductor)
			{
				IDeviceICD selectICD = ((IICDSelection) libraryPart).getICD();
				if (selectICD == null) {
					throw new IllegalStateException("Selected ICD cannot be null");
				}
				return new AddParameterizedICDPinListAction(controller, pinlist, pins, autogenerate, reference,
						placeAsStack,
						placeAsGroup,
						pinProxies, selectICD, withConductor);
			}

			@Override
			protected AddSymbolledPinListAction createAddSymbolledPinListAction(ICapletController controller,
					IPinList pinlist, ISymbolDef symDef, IBlock block, Map<IUID, ISymbolledPin> map, boolean reference,
					boolean withConductor)
			{
				IDeviceICD selectICD = ((IICDSelection) libraryPart).getICD();
				if (selectICD == null) {
					throw new IllegalStateException("Selected ICD cannot be null");
				}
				return new AddSymbolledICDPinListAction(controller, pinlist, symDef, block, map, reference, selectICD,
						withConductor);
			}
		};
		subAction = action;
		return action.onActivate(e);
	}

	public String getActionUIClass()
	{
		return AddParametrizedDeviceFromICDActionUI.class.getName();
	}

	@Nullable @Override protected ILibraryPartSelection pickLibraryPart()
	{
		return getPartSelection();
	}
}