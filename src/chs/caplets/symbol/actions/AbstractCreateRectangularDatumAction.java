/*
 * Copyright 2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.graphics.CreateRectangleAction;
import chs.caplets.symbol.Model;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGrid;
import chs.cof.draw.IGriddable;
import chs.cof.draw.LineStyle;
import chs.cof.drawplus.IDiagramText;
import chs.cof.drawplus.IGridDatumRepresentation;
import chs.cof.symbol.IStamp;
import chs.common.IBaseDatum;
import chs.common.attr.IAttributeTypes;
import chs.services.dynamicgfx.ISmartPoint;
import chs.system.FactoryMgr;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.TextHelper;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.awt.Point;
import java.util.List;

/**
 * An action to create rectangular datums - e.g. grid datum
 */
public abstract class AbstractCreateRectangularDatumAction extends CreateRectangleAction
{

	private Model m_model;
	private IGrid m_grid;
	private static final Color GD_COLOR = Color.BLUE;

	protected AbstractCreateRectangularDatumAction(ICapletController controller)
	{
		super(controller);
		m_model = (Model) controller.getCapletModel();
		m_grid = ((IGriddable) m_model.getSheet()).getGrid();
	}

	@NotNull protected abstract IBaseDatum newDatum();
	protected abstract void addDatumToStamp(@NotNull IBaseDatum datum);

	@NotNull protected IStamp getStamp()
	{
		return m_model.getSymbolDef();
	}

	protected IGfxObject constructDisplayObject(List<ISmartPoint> point_list)
	{
		IGridDatumRepresentation datumRep = null;
		Point lastPoint = null;

		for (Object point : point_list) {
			ISmartPoint spt = (ISmartPoint) point;
			Point pt = spt.getAbsoluteLocation();
			if (lastPoint != null) {
				// Create our schem object
				IBaseDatum datum = createDatum(pt, lastPoint);
				datumRep = createRepresentation(datum, pt, lastPoint);
			}
			lastPoint = pt;
		}

		return datumRep;
	}

	@NotNull private IBaseDatum createDatum(@NotNull Point pt1, @NotNull Point pt2)
	{
		IBaseDatum datum = newDatum();
		datum.setNameMgr(getStamp().getNameMgr());
		int bl_x = Math.min(pt2.x, pt1.x);
		int bl_y = Math.min(pt2.y, pt1.y);
		datum.setLocation(bl_x, bl_y);
		addDatumToStamp(datum);
		return datum;
	}

	@NotNull private IGridDatumRepresentation createRepresentation(@NotNull IBaseDatum datum, @NotNull Point pt1,
			@NotNull Point pt2)
	{
		IGridDatumRepresentation datumRep = FactoryMgr.getSymbolFactory()
				.createGridDatumRepresentation(FactoryMgr.createUID(), pt2.x, pt2.y, pt1.x, pt1.y, datum);

		datumRep.setAttribute(FactoryMgr.getDrawFactory().constructGfxAttribute(
				FactoryMgr.getDrawFactory().constructColorRGB(GD_COLOR.getRed(),
						GD_COLOR.getGreen(), GD_COLOR.getBlue()), 2, LineStyle.SOLID));

		ICompoundObject diag = m_model.getSheet();
		diag.addObject(datumRep);
		CreationDeletionHelper.getTheCreationHelper().addCreationObject(datumRep);

		IDiagramText nameText = FactoryMgr.getDrawPlusFactory().constructAttributeText(
				FactoryMgr.getCommonFactory().createUID(), datum, TextHelper.getDefaultHeight(m_grid), 0, 0,
				0,
				IAttributeTypes.NAME);
		datumRep.addObject(nameText);
		nameText.setFont(TextHelper.getDefaultFont());

		return datumRep;
	}
}


