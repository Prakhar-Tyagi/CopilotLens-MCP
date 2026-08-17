/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2014-2024 Siemens
 */

package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.helpers.ui.std.DesignAbstractionHelper;
import chs.caplets.logic.Model;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IHighwaySegment;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cofUtils.cmd.CreateSchemHighwayCmd;
import chs.common.DesignAbstractionType;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.IDynamicGfxMediator;
import chs.services.dynamicgfx.ISmartPoint;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA. User: zali Date: May 25, 2009 Time: 4:32:46 PM To change this template use File | Settings
 * | File Templates.
 */
public abstract class CreateHighwayAction extends LogicMultipointCreateAction
{
	protected CreateSchemHighwayCmd m_cmd;
	private List<IHighwaySegment> m_connectingObjects;

	protected CreateHighwayAction(ICapletController controller)
	{
		super(controller, true, false);
	}

	@Nullable
	protected IGfxObject constructDisplayObject(List<ISmartPoint> smartPoints)
	{
		List points = new ArrayList(smartPoints.size());
		for (Iterator spts = smartPoints.iterator(); spts.hasNext(); ) {
			ISmartPoint spt = (ISmartPoint) spts.next();
			points.add(spt.getAbsoluteLocation());
		}

		m_cmd.setDesign(getLocalModel().getDesign());
		m_cmd.setDiagram((ISchemDiagram) getModel().getSheet());
		m_cmd.setPoints(points);
		//m_cmd.setConductorType(INetConductor.class);
		m_cmd.setPrune(true);

		m_cmd.execute();
		m_connectingObjects = m_cmd.getSegments();
//		if(m_libWire != null){
//			cond.getConnectivity().assignLibraryDetails(m_libWire);
//		}
		return m_cmd.getSchemHighway();
	}

	/**
	 * Description of the Method
	 *
	 * @return Description of the Return Value
	 */
	protected Collection connectingObjects()
	{
		return m_connectingObjects;
	}

	protected Model getLocalModel()
	{
		return (Model) super.getModel();
	}

	protected IDynamicGfx constructDynGfx(Point ref_point)
	{
		List<Point> vec = new Vector<Point>();

		vec.add(ref_point);
		return getDynamicGfxService().getFactory().constructPolyline(vec, new Point(0, 0), true, false);
	}

	public CreateSchemHighwayCmd getCommand()
	{
		return m_cmd;
	}

	public void modifySnapCandidates(Collection<IDynamicGfxMediator> snapList, Point hitPoint,
			Collection<IDynamicGfxMediator> avoidMediators)
	{
		IDynamicGfxMediator blockMed = null;
		for (IDynamicGfxMediator med : snapList) {
			if (med instanceof IPinList && ((IConnectivityRef) med).getConnectivity() instanceof IBlockDevice) {
				blockMed = med;
				break;
			}
		}
		if (blockMed != null) {
			snapList.clear();
			snapList.add(blockMed);
		}
	}
}
