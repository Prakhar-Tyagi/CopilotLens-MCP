/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions.inlineassist;

import chs.caplets.logic.actions.BackshellAddTerminationHelper;
import chs.caplets.logic.actions.BackshellTerminationHelper;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;

/**
 * Factory for creating backshell termination pins for inline halves.
 */
public class BackshellTerminationPinFactory implements ITerminationPinFactory
{

	@Override
	@NotNull
	public IAbstractPin createPin(@NotNull IPinList schemInlineHalf)
	{
		chs.cof.logical.cable.IPinList connectivity = schemInlineHalf.getConnectivity();
		if (!(connectivity instanceof IConnector connector)) {
			throw new IllegalStateException("Expected connector for backshell termination creation");
		}
		return new BackshellTerminationHelper().addBackshellTermination(connector);
	}

	@Override
	public void createSchemPin(@NotNull IPinList schemInlineHalf,
			@NotNull chs.cof.logical.cable.IPinList inlineHalf,
			@NotNull ISchemDiagram diagram,
			@NotNull Point pinPosition,
			@NotNull IAbstractPin inlineHalfPin)
	{
		if (!(inlineHalf instanceof IConnector connector)) {
			return;
		}
		IBackshell backshell = connector.getBackshell();
		if (backshell == null) {
			return;
		}
		BackshellAddTerminationHelper addPinHelper = new BackshellAddTerminationHelper(schemInlineHalf, false);
		addPinHelper.addPin(diagram, pinPosition, schemInlineHalf, backshell, inlineHalfPin, null);
	}
}
