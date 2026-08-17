/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions.inlineassist;

import chs.caplets.logic.actions.PinListAddPinHelper;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cofUtils.parameterized.AddPinHelper;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;

/**
 * Factory for creating cable pins for inline halves.
 */
public class CablePinFactory implements ITerminationPinFactory
{

	@Override
	@NotNull
	public IAbstractPin createPin(@NotNull IPinList schemInlineHalf)
	{
		return generateCablePin(schemInlineHalf.getConnectivity());
	}

	@Override
	public void createSchemPin(@NotNull IPinList schemInlineHalf,
			@NotNull chs.cof.logical.cable.IPinList inlineHalf,
			@NotNull ISchemDiagram diagram,
			@NotNull Point pinPosition,
			@NotNull IAbstractPin inlineHalfPin)
	{
		PinListAddPinHelper addPinHelper = new PinListAddPinHelper(schemInlineHalf, false);
		addPinHelper.addPin(diagram, pinPosition, schemInlineHalf, inlineHalf, inlineHalfPin, null);
	}

	@NotNull
	private IAbstractPin generateCablePin(@NotNull chs.cof.logical.cable.IPinList connector)
	{
		AddPinHelper.CablePinGenerator cablePinGenerator = new AddPinHelper.CablePinGenerator(connector, null, null);
		cablePinGenerator.generate();
		return cablePinGenerator.getCpin();
	}
}
