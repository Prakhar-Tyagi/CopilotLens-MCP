/*
 * Copyright 2007-2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.actions;

import chs.caf.caplet.ICapletController;
import chs.cof.COFTypeEnum;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.common.IBaseDatum;
import chs.common.IDatum;
import chs.common.reln.IRelatedEntityType;
import chs.system.FactoryMgr;
import chs.utility.SymbolUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Action to create a rectangular grid datum
 */
public class CreateGridDatumAction extends AbstractCreateRectangularDatumAction
{

	public CreateGridDatumAction(ICapletController controller)
	{
		super(controller);
	}

	public String getActionUIClass()
	{
		return CreateGridDatumActionUI.class.getName();
	}

	@Override @NotNull protected IBaseDatum newDatum()
	{
		IDatum datum = FactoryMgr.getCommonFactory().createDatum(FactoryMgr.createUID());
		datum.setType(COFTypeEnum.Unknown);
		return datum;
	}

	@Override protected void addDatumToStamp(@NotNull IBaseDatum datum)
	{
		getStamp().addDatum(IRelatedEntityType.Unknown, (IDatum) datum, null, -1);
	}

	public boolean isEnabled()
	{
		// todo ActionHierarchy this action does not call super.isEnabled - is this correct
		// This will make enabling and disabling from the framework difficult
		if (!isModeEnabled()) {
			return false;
		}

		IStamp stamp = getStamp();
		if (stamp instanceof ISymbolDef) {
			ISymbolDef symDef = (ISymbolDef) stamp;
			if (SymbolUtils.isCommentSymbol(symDef)) {
				Map<IRelatedEntityType, List<IDatum>> result = stamp.getTopLevelDatums();
				List<IDatum> gridDatum = result.get(IRelatedEntityType.Unknown);
				if (gridDatum == null) {
					return true;
				}
				else {
					return gridDatum.isEmpty();
				}
			}
		}
		return false;
	}
}


