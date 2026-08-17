/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared;

import chs.cof.logical.IDesign;
import chs.cof.logical.schem.ISchemDiagram;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FunctionUnshareHelper extends DeviceUnshareHelper
{

	public FunctionUnshareHelper(IDesign theDesign, @Nullable ISchemDiagram diagram)
	{
		super(theDesign, diagram);
	}

	@NotNull @Override protected String getNameAlreadyExistsMessageText()
	{
		return "UnsharePinListActionHelper.NameExistsError.Message.Function.text";
	}

	@NotNull @Override protected String getNameErrorForSharedObjectMessageKey()
	{
		return "UnsharePinListActionHelper.NameExistsError.SharedMessage.Function.text";
	}

	@NotNull @Override protected String getRenamePinListDialogTitleKey()
	{
		return "UnsharePinListActionHelper.Rename.Function.Title";
	}

	@NotNull @Override protected String getSymbolPinConflictDisconnectKey()
	{
		return "UnsharePinListActionHelper.SymbolPortConflictDisconnect.Msg";
	}

	@NotNull @Override protected String getSymbolPinConflictHeaderKey()
	{
		return "UnsharePinListActionHelper.SymbolPortConflict.Header";
	}

	@NotNull @Override protected String getSymbolPinConflictMessageKey()
	{
		return "UnsharePinListActionHelper.SymbolPortConflict.Msg";
	}
}
