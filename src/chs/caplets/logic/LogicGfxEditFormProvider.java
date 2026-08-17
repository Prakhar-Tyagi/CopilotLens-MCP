/*
* Copyright 2014 Mentor Graphics Corporation
* All Rights Reserved
*
* THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
* INFORMATION WHICH IS THE PROPERTY OF MENTOR
* GRAPHICS CORPORATION OR ITS LICENSORS AND IS
* SUBJECT TO LICENSE TERMS.  
 */

package chs.caplets.logic;

import chs.caf.caplet.helpers.GfxEditFormBuilder;
import chs.cof.draw.IGfxAttributeContainer;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISymboledSchemPinList;

public class LogicGfxEditFormProvider extends GfxEditFormBuilder.DefaultGfxEditFormProvider
{

	//
	// Is it an instance with a symbol ref?
	//
	@Override
	protected boolean returnNull(IGfxAttributeContainer gfxObject)
	{
		boolean onSymbol = false;
		if (gfxObject instanceof IPinList) {
			if (((ISymboledSchemPinList) gfxObject).getSymbolRef() != null) {
				onSymbol = true;
			}
		}
		else if (gfxObject instanceof IPin) {
			IDiagramObject parent = ((IDiagramObject) gfxObject).getParent();
			if (parent instanceof IPinList && ((ISymboledSchemPinList) parent).getSymbolRef() != null) {
				onSymbol = true;
			}
		}
		return onSymbol;
	}
}
