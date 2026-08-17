/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caplets.logic.Model;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.cable.IInterconnectConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cofUtils.cmd.CreateSchemConductorCmd;
import chs.services.dynamicgfx.DynamicPolyline;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.ISmartPoint;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

/**
 * A create tool to make a conductor.
 *
 * @created April 12, 2002
 */
public class CreateInterconnectAction
		extends LogicMultipointCreateAction
{

	private List m_connectingObjects;
	private DynamicPolyline m_dynLine;

	static private Cursor m_interconnectCursor = null;
	private CreateSchemConductorCmd m_cmd;

	/**
	 * Constructor for the CreateInterconnectAction object
	 *
	 * @param controller Description of the Parameter
	 */
	public CreateInterconnectAction(ICapletController controller)
	{
		super(controller, true, false);

		if (m_interconnectCursor == null) {
			m_interconnectCursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/cur_interconnect.gif", new Point(7, 7));
		}
	}

	/**
	 * Gets the ActionUIClass attribute of the CreateCircleAction object
	 *
	 * @return The ActionUIClass value
	 */
	public String getActionUIClass()
	{
		return CreateInterconnectActionUI.class.getName();
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		m_cmd = new CreateSchemConductorCmd(ConductorRouteAction.getInstance());
		return super.onActivate(e);
	}

	public CreateSchemConductorCmd getCommand()
	{
		return m_cmd;
	}

	/**
	 * @param point_list Description of the Parameter
	 *
	 * @return Description of the Return Value
	 */
	protected IGfxObject constructDisplayObject(List<ISmartPoint> point_list)
	{
		List<Point> points = new ArrayList<Point>(point_list.size());
		for (ISmartPoint spt : point_list) {
			points.add(spt.getAbsoluteLocation());
		}

		m_cmd.setDesign(getLocalModel().getDesign());
		m_cmd.setDiagram((ISchemDiagram) getModel().getSheet());
		m_cmd.setOrthoMode(m_orthoMode);
		m_cmd.setPoints(points);
		m_cmd.setConductorType(IInterconnectConductor.class);
		m_cmd.setPrune(true);

		m_cmd.execute();
		IConductor cond = m_cmd.getConductor();
		m_connectingObjects = m_cmd.getSegments();
		return m_cmd.getConductor();
	}

	protected Model getLocalModel()
	{
		return (Model) super.getModel();
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
		Collection<Point> vec = new Vector<Point>();

		vec.add(ref_point);

		m_dynLine = (DynamicPolyline)
				getDynamicGfxService().getFactory().constructPolyline(vec, new Point(0, 0), true, false);
		return m_dynLine;
	}

	/**
	 * Description of the Method
	 *
	 * @return Description of the Return Value
	 */
	protected Class snappingSource()
	{
		return IInterconnectConductor.class;
	}

	/**
	 * Description of the Method
	 *
	 * @return Description of the Return Value
	 */
	protected Collection connectingObjects()
	{
		// We only want to connect the first and last segments.
		if (m_connectingObjects.size() <= 2) {
			return m_connectingObjects;
		}
		else {
			Collection v = new Vector(2);
			v.add(m_connectingObjects.get(0));
			v.add(m_connectingObjects.get(m_connectingObjects.size() - 1));
			return v;
		}
	}

	/**
	 * Return the cursor for this action
	 */
	public Cursor getCursor()
	{
		return m_interconnectCursor;
	}
}

