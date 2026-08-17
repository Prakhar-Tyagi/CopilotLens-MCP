/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions.inlineassist;

import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;

/**
 * Interface for creating termination pins on inline halves.
 */
public interface ITerminationPinFactory
{

	/**
	 * Creates a pin for the inline half.
	 *
	 * @param schemInlineHalf the schematic inline half (jack or plug)
	 * @return the created abstract pin
	 */
	@NotNull
	IAbstractPin createPin(@NotNull IPinList schemInlineHalf);

	/**
	 * Creates the schematic representation of the pin.
	 *
	 * @param schemInlineHalf    the schematic inline half
	 * @param inlineHalf         the connectivity inline half
	 * @param diagram            the schematic diagram
	 * @param pinPosition        the position for the pin
	 * @param inlineHalfPin      the abstract pin to create schematic for
	 */
	void createSchemPin(@NotNull IPinList schemInlineHalf,
			@NotNull chs.cof.logical.cable.IPinList inlineHalf,
			@NotNull ISchemDiagram diagram,
			@NotNull Point pinPosition,
			@NotNull IAbstractPin inlineHalfPin);
}
