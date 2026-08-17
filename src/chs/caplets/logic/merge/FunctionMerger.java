/*
 * Copyright 2015 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.merge;

import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IFunction;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.symbol.ISymbolRef;
import chs.common.IReference;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Author: creddy Date: 22-01-2015 Time: 15:09
 */
public class FunctionMerger extends PinlistMerger
{

	public FunctionMerger(ILogicObject sourceLogicObject, ILogicObject targetLogicObject,
			@NotNull IMergeActionChangeReporter reporter)
	{
		super(sourceLogicObject, targetLogicObject, reporter);
	}

	@Override protected void mergeSchematic(IConnectivityRef sourceSchemObject, ILogicObject targetlogicObject)
	{
		super.mergeSchematic(sourceSchemObject, targetlogicObject);
		resetSchematicsConnectivity(sourceSchemObject, targetlogicObject);
	}

	protected void mergeConnectivity(ILogicObject sourceLogicObject, ILogicObject targetLogicObject){
		super.mergeConnectivity(sourceLogicObject,targetLogicObject);
		mergeSymbolReference((IFunction) sourceLogicObject,(IFunction) targetLogicObject);
	}

	private void mergeSymbolReference(IFunction source, IFunction target)
	{
		Set<ISymbolRef> refs = new HashSet<ISymbolRef>(source.getSymbolReferences());
		for (ISymbolRef ref : refs) {
			source.removeSymbolRefIfCanMaintainMultipleSymbols(ref);
			target.addSymbolRefIfCanMaintainMultipleSymbols(ref);
		}
	}

	@Override protected void mergePin(chs.cof.logical.cable.IPinList sourceParent,
			chs.cof.logical.cable.IPinList targetParent,
			IAbstractPin sourcePin, IAbstractPin targetPin)
	{
		super.mergePin(sourceParent,targetParent,sourcePin,targetPin);
		if (targetPin != null && targetPin.getReference() == null && sourcePin.getReference() != null) {
			((IReference) targetPin).setReference(sourcePin.getReference());
		}
	}
}
