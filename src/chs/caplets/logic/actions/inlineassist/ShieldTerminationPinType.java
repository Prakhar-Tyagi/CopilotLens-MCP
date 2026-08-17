/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions.inlineassist;

import org.jetbrains.annotations.NotNull;

/**
 * Enum representing types of termination pins for shield terminations on inline connectors.
 */
public enum ShieldTerminationPinType
{
	CABLE_PIN {
		@NotNull @Override
		public ITerminationPinFactory getFactory()
		{
			return new CablePinFactory();
		}

		@Override public boolean requiresExplicitMateCreation()
		{
			return false;
		}

		@Override public boolean canConnectWith(@NotNull ShieldTerminationPinType mateType)
		{
			return mateType == CABLE_PIN;
		}

		@Override public boolean shouldSetSharedPins()
		{
			return true;
		}
	},
	BACKSHELL_PIN {
		@NotNull @Override
		public ITerminationPinFactory getFactory()
		{
			return new BackshellTerminationPinFactory();
		}

		@Override public boolean requiresExplicitMateCreation()
		{
			return true;
		}

		@Override public boolean canConnectWith(@NotNull ShieldTerminationPinType mateType)
		{
			return mateType == BACKSHELL_PIN;
		}

		@Override public boolean shouldSetSharedPins()
		{
			return false;
		}
	};

	@NotNull
	public abstract ITerminationPinFactory getFactory();

	public abstract boolean requiresExplicitMateCreation();

	public abstract boolean canConnectWith(@NotNull ShieldTerminationPinType mateType);

	public abstract boolean shouldSetSharedPins();
}
