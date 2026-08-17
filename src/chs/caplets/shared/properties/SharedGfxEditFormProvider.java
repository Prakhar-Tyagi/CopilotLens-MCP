/*
* Copyright 2014 Mentor Graphics Corporation
* All Rights Reserved
*
* THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
* INFORMATION WHICH IS THE PROPERTY OF MENTOR
* GRAPHICS CORPORATION OR ITS LICENSORS AND IS
* SUBJECT TO LICENSE TERMS.  
 */

package chs.caplets.shared.properties;

import chs.caplets.logic.LogicGfxEditFormProvider;
import chs.cof.draw.IGfxAttributeContainer;
import chs.cof.logical.schem.IAssembly;
import chs.cof.logical.schem.ISchemDiagram;

public class SharedGfxEditFormProvider extends LogicGfxEditFormProvider
{

	@Override
	protected boolean returnNull(IGfxAttributeContainer gfxObject)
	{
		//Oh..Let us not show Visibility column for Cable Assembly Properties Dialog box
		if (IAssembly.class.isAssignableFrom(gfxObject.getClass())) {
			return true;
		}

		return super.returnNull(gfxObject);
	}

	@Override
	protected boolean skip(IGfxAttributeContainer gfxObject)
	{
		return gfxObject instanceof ISchemDiagram;
	}
}
