/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2016-2023 Siemens
 */
package chs.caplets.logic.actions.inlineassist;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.actions.AddPinListAction;
import chs.caplets.logic.actions.CreateInlineConnectorAction;
import chs.caplets.logic.actions.CreateParameterizedObjectAction;
import chs.caplets.logic.actions.IPinInfoProvider;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.ISymboledSchemPinList;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.ISymbolDef;
import chs.ctf.caf.utils.IPinProxy;
import chs.services.dynamicgfx.ISmartPoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AutoAddPinListAction extends AddPinListAction implements IPinInfoProvider
{

	private boolean enabled = false;
	private IGenericInlineConnector inlineHalf;
	private InsertExistingInlineConnectorAction lastAction;

	public AutoAddPinListAction(ICapletController controller)
	{
		super(controller);
	}

	@Override protected CreateParameterizedObjectAction createPlaceInlineConnectorAction(ICapletController controller,
			IGenericInlineConnector connector,
			List<IAbstractPin> pins, boolean autogenerate, boolean reference, boolean groupPlacement,boolean stackPlacement, Collection<IPinProxy> pinProxies)
	{
		return new InsertExistingInlineConnectorAction(controller, connector, pins, autogenerate, reference);
	}

	public void setInlineHalf(IGenericInlineConnector inlineHalf)
	{
		this.inlineHalf = inlineHalf;
	}

	@Nullable protected IPinList getOperand()
	{
		return inlineHalf;
	}

	@NotNull @Override protected IPinInfoProvider getAddPinListDialog(@Nullable Frame frame, @NotNull IPinList pinlist, @Nullable ISymbolDef symDef)
	{
		return this;
	}

	public void setPoints(List<ISmartPoint> points, InlineDirection direction)
	{
		if (subAction instanceof InsertExistingInlineConnectorAction) {
			((CreateInlineConnectorAction) subAction).setPointsForPlacement(points, direction);
		}
	}

	@Nullable
	public InsertInlineResult.ResultInlineConnector getResultConnector()
	{
		if (lastAction != null) {
			return lastAction.getResultConnector();
		}

		return null;
	}

	@Override public boolean isEnabled()
	{
		return enabled;
	}

	@Override
	protected boolean onTerminate(boolean successful)
	{
		if (subAction instanceof InsertExistingInlineConnectorAction) {
			lastAction = ((InsertExistingInlineConnectorAction) subAction);
		}

		boolean result = super.onTerminate(successful);
		enabled = false;
		return result;
	}

	public void enable()
	{
		enabled = true;
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		lastAction = null;
		return super.onActivate(e);
	}

	public void setCursor(@Nullable Cursor cursor)
	{

	}

	@Override public boolean selectPinList()
	{
		return true;
	}

	@Override public List<IPinProxy> getPins()
	{
		return Collections.emptyList();
	}

	@Override public boolean getAutogenerate()
	{
		return true;
	}

	@Override public boolean getReference()
	{
		return false;
	}

	@Override public boolean getPlaceAsStack()
	{
		return false;
	}

	@Override public boolean getPlaceAsGroup()
	{
		return false;
	}

	@Nullable @Override public ISymbolDef getSymbol()
	{
		return loadSymbolDef(inlineHalf);
	}

	@Override public IBlock getBlock()
	{
		if (inlineHalf instanceof ISymboledSchemPinList) {
			return ((ISymboledSchemPinList) inlineHalf).getBlock();
		}
		return null;
	}
}
