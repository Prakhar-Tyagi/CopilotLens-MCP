/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout;

import chs.cof.draw.HorizJustificationEnum;
import chs.cof.draw.IGrid;
import chs.cof.draw.VertJustificationEnum;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.common.UnitTypeEnum;
import chs.utilities.CommonUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author chandras on 19-10-2019.
 */
public interface IBasicDevicePlacementControl
{

	double TRANS_TEXT_SIZE = 150; //reference of 0.25 inch.

	@NotNull ISchemDiagram getDiagram();

	@NotNull IGrid getGrid();

	@NotNull IProject getProject();

	@NotNull VertJustificationEnum getVerticalJustification();

	@NotNull HorizJustificationEnum getHorizontalJustification();

	@NotNull PlacementAxisRotation getPlacementRotation();

	boolean isOriginAligned();

	static int computeTransientTextSize(@NotNull ISchemDiagram diagram)
	{
		//the return value would be multiplied w.r.t 0.25 inch.
		final double diagramPhyUnit = diagram.getPhysicalMapping().getInMeters();
		final double typeInchInMeters = UnitTypeEnum.TypeInch.getInMeters();
		return CommonUtils.toInteger(TRANS_TEXT_SIZE * typeInchInMeters / diagramPhyUnit);
	}
}
