/*
 * Copyright 2002-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.symlib;

import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.ISymbolRef;

public interface ISymbolSelectionMgr
{

	/**
	 * * @return a symbol definition, given a reference.
	 */
	public ISymbolDef getActiveSymbol();

	public ISymbolDef getReferencedSymbol(ISymbolRef ref);
}
