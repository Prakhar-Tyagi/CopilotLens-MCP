/*
 * Copyright 2016 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic;

import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.EmptyModifierKeyActionProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Capital logic action provider for mouse clicks with shift key pressed.
 * Extracted from a combination of SelectClientHelper and logic View implementations.
 *
 * Enables the provision of the action to be executed with mouse click and shift keys
 */
public class LogicModifierKeyActionProvider extends EmptyModifierKeyActionProvider
{

	@NotNull private final IAction mShiftModifierAction;

	public LogicModifierKeyActionProvider(@NotNull IAction action)
	{
		mShiftModifierAction = action;
	}

	@NotNull @Override
	public Optional<IAction> getActionForModifierKeyCombination(boolean shiftDown, boolean controlDown, boolean altDown,
			boolean altGraphDown, boolean metaDown)
	{
		// Currently, only accepts shift modifier. Could also have alt.
		if (shiftDown && !(altDown || altGraphDown || controlDown || metaDown)
				&& mShiftModifierAction.isEnabled()) {
			return Optional.of(mShiftModifierAction);
		}

		return Optional.empty();
	}
}
