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

import chs.caplets.logic.Model;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.shared.ISharedConductor;
import chs.cofUtils.cmd.CreateSchemConductorCmd;
import chs.services.dynamicgfx.ISmartPoint;
import chs.utility.helpers.SharedConductorHelper;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

public class ConductorDisplayObjectConstructionHelper
{

	private final CreateSchemConductorCmd cmd;
	private final Model model;
	private final Function<List<ISmartPoint>, chs.cof.logical.schem.IConductor> schemCondProvider;

	public ConductorDisplayObjectConstructionHelper(Model logicModel, CreateSchemConductorCmd command, Function<List<ISmartPoint>, chs.cof.logical.schem.IConductor> schemCondProvider)
	{
		model = logicModel;
		cmd = command;
		this.schemCondProvider = schemCondProvider;
	}

	@NotNull public IGfxObject constructDisplayObject(List<ISmartPoint> smartPoints, ISharedConductor sharedConductor)
	{
		IConductor cableCond = model.getDesign().getConnectivity().findSharedConductor(sharedConductor);

		cmd.setCableConductor(cableCond); // if null a new connectivity is created

		chs.cof.logical.schem.IConductor schemCond = schemCondProvider.apply(smartPoints);
		SharedConductorHelper
				.assignToShared(schemCond, sharedConductor, model.getDesign(), model.getDiagram());

		return schemCond;
	}
}
