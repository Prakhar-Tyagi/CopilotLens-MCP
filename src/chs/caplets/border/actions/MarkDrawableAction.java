/*
 * Copyright 2003-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.border.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.creation.CreateByPointAction;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IRectangle;
import chs.cof.drawplus.IDrawPlusFactory;
import chs.cof.drawplus.IPropertiedRectangle;
import chs.common.ICommonFactory;
import chs.common.IUID;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.ISmartPoint;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.CreationDeletionHelper;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.List;

public class MarkDrawableAction extends CreateByPointAction
{

	private static Cursor m_rectCursor = null;

	/**
	 * Constructor for the CreateRectangleAction object
	 *
	 * @param controller Description of the Parameter
	 */
	public MarkDrawableAction(ICapletController controller)
	{
		super(controller, false, true);

		if (m_rectCursor == null) {
			m_rectCursor = CAFUtils.getInstance()
					.loadCursorDirect(getClass(), "chs/images/general/cur_draw_rect.gif", new Point(7, 7));
		}
	}

	/**
	 * Gets the ActionUIClass attribute of the CreateCircleAction object
	 *
	 * @return The ActionUIClass value
	 */
	public String getActionUIClass()
	{
		return MarkDrawableActionUI.class.getName();
	}

	/**
	 * Description of the Method
	 *
	 * @param point_list Description of the Parameter
	 *
	 * @return Description of the Return Value
	 */
	protected IGfxObject constructDisplayObject(List<ISmartPoint> point_list)
	{
		// Get our factories
		IDrawPlusFactory drawplusFact =
				FactoryMgr.getDrawPlusFactory();
		ICommonFactory commonFactory = FactoryMgr.getCommonFactory();

		IPropertiedRectangle propRectangle = null;

		Point lastPoint = null;
		for (ISmartPoint spt : point_list) {
			Point pt = spt.getAbsoluteLocation();
			if (lastPoint != null) {
				// Create our schem object
				IUID uid = commonFactory.createUID();
				propRectangle =
						drawplusFact.constructPropRectangle(uid, lastPoint.x, lastPoint.y, pt.x, pt.y);
				ICompoundObject diag = getModel().getSheet();
				diag.addObject(propRectangle);
				CreationDeletionHelper.getTheCreationHelper().addCreationObject(propRectangle);
			}
			lastPoint = pt;
		}

		return propRectangle;
	}

	/**
	 * Description of the Method
	 *
	 * @param e Description of the Parameter
	 */
	public void mouseClicked(MouseEvent e)
	{
		if (e.getClickCount() == 2) {
			getController().getActionMgr().commitActiveAction();
		}
		else {
			super.mouseClicked(e);
		}
	}

	/**
	 * Description of the Method
	 *
	 * @param ref_point Description of the Parameter
	 *
	 * @return Description of the Return Value
	 */
	protected IDynamicGfx constructDynGfx(Point ref_point)
	{
		return getDynamicGfxService().getFactory().constructRectangle(new Point(ref_point.x, ref_point.y),
				new Point(1, 1),
				new Point(0, 0), true);
	}

	/**
	 * Description of the Method
	 *
	 * @return Description of the Return Value
	 */
	protected Class<IRectangle> snappingSource()
	{
		return IRectangle.class;
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(MarkDrawableAction.class, "MarkDrawableAction.Statusbar.Text");
	}

	/**
	 * Return the cursor for this action
	 */
	public Cursor getCursor()
	{
		return m_rectCursor;
	}
}

