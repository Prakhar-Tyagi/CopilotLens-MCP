/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.logic.actions.serviceDocumentation;

import chs.caf.cafmain.actions.ReplaceInstanceSymbolAction;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.IConductorRouteAction;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.common.cmd.replacesymbol.InstanceAdapter;
import chs.common.cmd.replacesymbol.ReplaceInstanceSymbolParams;
import chs.utility.SymbolUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class PublisherReplaceInstanceSymbolAction extends ReplaceInstanceSymbolAction
{

	private int m_selectedSymbolPinCount;
	private int m_SymbolToReplaceWithPinCount;

	public PublisherReplaceInstanceSymbolAction(ICapletController controller)
	{
		super(controller);
	}

	public PublisherReplaceInstanceSymbolAction(ICapletController controller,
			@NotNull IConductorRouteAction conductorRouteAction)
	{
		super(controller, conductorRouteAction);
	}

	@Override public String getActionUIClass()
	{
		return PublisherReplaceInstanceSymbolActionUI.class.getName();
	}

	@Override public boolean isValidSymbol(IStamp sym)
	{
		ReplaceInstanceSymbolParams param = getCmdParams();
		if (param != null) {
			InstanceAdapter inst = param.getInstanceAdapter();
			assert inst != null;
			m_selectedSymbolPinCount = inst.getSchemPins().size();
		}
		if (((ISymbolDef) sym).getConnectivity() != null) {
			Collection<IAbstractPin> pins = SymbolUtils.getConnectivityPins((ISymbolDef) sym);
			m_SymbolToReplaceWithPinCount = pins.size();
		}
		if (m_selectedSymbolPinCount == m_SymbolToReplaceWithPinCount) {
			return super.isValidSymbol(sym);
		}
		return false;
	}
}
