/*
 * Copyright 2016 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.cof.symbol.IBlock;
import chs.cof.symbol.ISymbolDef;
import chs.ctf.caf.utils.IPinProxy;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IPinInfoProvider
{

	boolean selectPinList();

	List<IPinProxy> getPins();

	boolean getAutogenerate();

	boolean getReference();

	boolean getPlaceAsStack();

	boolean getPlaceAsGroup();

	@Nullable
	ISymbolDef getSymbol();

	@Nullable
	IBlock getBlock();
}
