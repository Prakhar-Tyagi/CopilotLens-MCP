/*
 * Copyright 2006-2010 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.commands;

import chs.common.ICommandHelper;
import chs.common.cmd.replacesymbol.ReplaceInstanceSymbolCmd;
import chs.common.cmd.replacesymbol.ReplaceInstanceSymbolParams;
import chs.common.cmd.replacesymbol.UpdateInstanceSymbolCmd;
import org.jetbrains.annotations.NotNull;

/**
 * A command to perform update an instance symbol as part of Update Librart Part
 */

public class LibraryPartUpdateSymbolCommand extends UpdateInstanceSymbolCmd
{

	public LibraryPartUpdateSymbolCommand(@NotNull ICommandHelper commandHelper)
	{
		super(commandHelper);
	}

	// dts0100395839
	// This command could have renamed pins on the instance, therefore it is out of date
	// with respect to the symbol even though the timestamps match. Ignore timestamps when
	// used from this context.

	@Override protected InternalParams createInternalParams(final ReplaceInstanceSymbolParams params)
	{
		return new InternalUpdateLibraryPartSymbolParams(params);
	}

	protected class InternalUpdateLibraryPartSymbolParams extends InternalUpdateParams
	{

		protected InternalUpdateLibraryPartSymbolParams(
				final ReplaceInstanceSymbolParams params)
		{
			super(params);
		}

		@Override protected InstanceSymbolState doGetInstanceSymbolState()
		{
			if (!isSymbolFound()) {
				return ReplaceInstanceSymbolCmd.InstanceSymbolState.SYMBOL_NOT_FOUND;
			}
			return ReplaceInstanceSymbolCmd.InstanceSymbolState.SYMBOL_OUT_OF_DATE;
		}
	}
}
