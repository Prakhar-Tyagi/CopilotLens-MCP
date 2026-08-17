/*
 * Copyright 2002-2008 Mentor Graphics Corporation
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
import chs.caf.caplet.helpers.browser.PartBrowserActionHelper;
import chs.caf.caplet.helpers.snapping.ModelUtils;
import chs.caplets.logic.Model;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.SheathTypeAttrVal;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.schem.IShieldBody;
import chs.cof.logical.schem.IShieldBodyHookup;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cofUtils.cmd.CreateSchemConductorCmd;
import chs.common.preferencesets.IPreferenceSet;
import chs.services.dynamicgfx.DynamicPolyline;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.IDynamicGfxMediator;
import chs.services.dynamicgfx.IDynamicSnap;
import chs.services.dynamicgfx.ISmartPoint;
import chs.utilities.CommonUtils;
import chs.utility.PortHelper;
import chs.utility.preferences.PreferenceSetHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * A create tool to make a shield conductor.
 *
 * @created April 12, 2002
 */
public class CreateShieldConductorAction extends LogicMultipointCreateAction
{

	private ILibraryPartSelection m_libWire;
	private List<ISegment> m_connectingObjects;
	private Model m_model = null;
	private IConductor conductor = null;

	private static Cursor m_shieldCursor = null;

	public CreateShieldConductorAction(ICapletController controller)
	{
		super(controller, true, false);
		m_model = (Model) controller.getCapletModel();

		if (m_shieldCursor == null) {
			m_shieldCursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/cur_shield.gif", new Point(7, 7));
		}
	}

	protected void setupSnapHelper(@Nullable IMulticore selectedMulticore)
	{
		if (m_snapHelper != null && selectedMulticore != null) {
			m_snapHelper.setSnapHelper(new ModelUtils()
			{
				@Override public boolean discardMediator(@NotNull IDynamicGfxMediator mediator)
				{
					IShieldBody schematicShieldBody = CommonUtils.cast(mediator, IShieldBody.class);
					if (schematicShieldBody != null) {
						chs.cof.logical.cable.IShieldBody shieldBody = schematicShieldBody.getConnectivity();
						IMulticore snappedMulticore = shieldBody != null ? shieldBody.getMulticore() : null;
						return snappedMulticore != selectedMulticore;
					}
					return false;
				}
			});
		}
	}

	@Nullable protected IMulticore determineSnappedMulticore(@Nullable IDynamicSnap dynSnap)
	{
		final Iterator<IDynamicGfxMediator> mediators = dynSnap != null ? dynSnap.getMediators() :
				Collections.emptyIterator();
		while (mediators.hasNext()) {
			IShieldBodyHookup shieldBodyHookup = CommonUtils.cast(mediators.next(), IShieldBodyHookup.class);
			if (shieldBodyHookup != null) {
				final IShieldBody owner = (IShieldBody) (shieldBodyHookup.getParent());
				chs.cof.logical.cable.IShieldBody shieldBody = owner != null ? owner.getConnectivity() : null;
				return shieldBody != null ? shieldBody.getMulticore() : null;
			}
		}
		return null;
	}

	protected boolean checkHookupSnap(@NotNull IDynamicSnap dynSnap, @NotNull IMulticore selectedMulticore)
	{
		IMulticore snappedMulticore = determineSnappedMulticore(dynSnap);
		return snappedMulticore == null || snappedMulticore == selectedMulticore;
	}

	public boolean checkSnap(@Nullable IDynamicSnap dynSnap)
	{
		IMulticore snappedMulticore = determineSnappedMulticore(dynSnap);
		if (snappedMulticore != null && SheathTypeAttrVal.SHEATH_TYPE_TWISTED.equals(
				SheathTypeAttrVal.fromString(snappedMulticore.getSheathType()))) {
			return false;
		}
		return super.checkSnap(dynSnap);
	}

	/**
	 * Gets the ActionUIClass attribute of the CreateCircleAction object
	 *
	 * @return The ActionUIClass value
	 */
	public String getActionUIClass()
	{
		return CreateShieldConductorActionUI.class.getName();
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		m_libWire = PartBrowserActionHelper.getSelectedBrowserPart();
		return super.onActivate(e);
	}

	protected IGfxObject constructDisplayObject(List<ISmartPoint> point_list)
	{
		List<Point> points = new ArrayList<Point>(point_list.size());
		for (ISmartPoint spt : point_list) {
			points.add(spt.getAbsoluteLocation());
		}

		CreateSchemConductorCmd cmd = new CreateSchemConductorCmd(ConductorRouteAction.getInstance());
		cmd.setDesign(getLocalModel().getDesign());
		cmd.setDiagram((ISchemDiagram) getModel().getSheet());
		cmd.setOrthoMode(m_orthoMode);
		cmd.setPoints(points);
		cmd.setConductorType(IShieldConductor.class);
		cmd.setPrune(true);

		cmd.execute();
		conductor = cmd.getConductor();
		m_connectingObjects = cmd.getSegments();
		if (m_libWire != null) {
			conductor.getConnectivity().assignLibraryDetails(m_libWire);
		}
		return cmd.getConductor();
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
		Collection<Point> vec = new ArrayList<Point>();

		vec.add(ref_point);

		DynamicPolyline dynLine = (DynamicPolyline)
				getDynamicGfxService().getFactory().constructPolyline(vec, new Point(0, 0), true, false);
		return dynLine;
	}

	/**
	 * Description of the Method
	 *
	 * @return Description of the Return Value
	 */
	protected Class<?> snappingSource()
	{
		return IShieldConductor.class;
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
			Collection<Object> v = new Vector<Object>(2);
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
		return m_shieldCursor;
	}

	public boolean onTerminate(boolean successful)
	{
		boolean ok = super.onTerminate(successful) && successful;
		if (ok && conductor.getConnectivity() != null) {
			// dts0100586826 - Wrong Cross-Reference in case of Shield Termination
			// This was due to all shield conductors being marked as the home instance.
			// Now, only the first instance will be marked as home.
			if (PortHelper.hasMoreThanOneUsage(conductor, true)) {
				conductor.setHome(false);
			}

			ISharedConductor sharedCond = conductor.getConnectivity().getSharedConductor();
			if (sharedCond != null) {
				if (!refresh(sharedCond, m_model.getDesign().getProject())) {
					ok = false;
				}
			}
			if (ok) {
				ISchemDiagram schemDiag = getLocalModel().getDiagram();
				IPreferenceSet styleSet = PreferenceSetHelper.getStyleSet(schemDiag);
				PreferenceSetHelper.applyStyleSet(conductor, styleSet, true);
			}
		}
		conductor = null;

		return ok;
	}
}

