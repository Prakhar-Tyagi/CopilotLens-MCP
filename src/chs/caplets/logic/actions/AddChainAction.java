/*
 * Copyright 2004-2008 Mentor Graphics Corporation
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
import chs.caf.caplet.helpers.creation.CreateByMultipointAction;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.schem.IChainSegmentContainer;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.IShieldBodyHookup;
import chs.cofUtils.cmd.CreateSchemDaisyChainCmd;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.IDynamicGfxMediator;
import chs.services.dynamicgfx.IDynamicSnap;
import chs.services.dynamicgfx.ISmartPoint;
import chs.utilities.CollectionUtils;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utility.logic.ILogicModel;
import chs.utility.logic.DaisyChainCreationHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class AddChainAction extends CreateByMultipointAction
{

	private Cursor m_lineCursor = null;
	private CreateSchemDaisyChainCmd m_cmd;
	private List<IDynamicGfxMediator> m_connectingObjects;
	private boolean noSnap;

	/**
	 * Constructor for the CreateLineAction object
	 *
	 * @param controller Description of the Parameter
	 */
	public AddChainAction(ICapletController controller)
	{
		super(controller, true, false);

		if (m_lineCursor == null) {
			m_lineCursor = CAFUtils.getInstance()
					.loadCursorDirect(getClass(), "chs/images/general/cur_draw_chain.gif", new Point(7, 7));
		}
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		m_cmd = new CreateSchemDaisyChainCmd();
		noSnap = true;
		return super.onActivate(e);
	}

	/**
	 * Gets the ActionUIClass attribute of the CreateCircleAction object
	 *
	 * @return The ActionUIClass value
	 */
	public String getActionUIClass()
	{
		return AddChainActionUI.class.getName();
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
		List<Point> points = new ArrayList<Point>(point_list.size());
		for (ISmartPoint spt : point_list) {
			points.add(spt.getAbsoluteLocation());
		}

		m_cmd.setDesign(((ILogicModel) getModel()).getDesign());
		m_cmd.setDiagram((ISchemDiagram) getModel().getSheet());
		m_cmd.setPoints(points);
		m_cmd.setPrune(true);

		// make sure that hook ups have been set at this point
		m_cmd.execute();
		m_connectingObjects = CollectionUtils.getObjectList(m_cmd.getChainSegments(), IDynamicGfxMediator.class);

		return getCreatedDaisyChain();
	}

	protected IChainSegmentContainer getCreatedDaisyChain()
	{
		return m_cmd.getDaisyChain();
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
		List<Point> vec = new ArrayList<Point>(1);

		vec.add(ref_point);
		return getDynamicGfxService().getFactory().constructPolyline(vec, new Point(0, 0), true, false);
	}

	/**
	 * Description of the Method
	 *
	 * @return Description of the Return Value
	 */
	protected Class<?> snappingSource()
	{
		return IChainSegmentContainer.class;
	}

	@Override public void mouseReleased(MouseEvent e)
	{
		noSnap = false;
		if (getSnapHelper().getNumSnapped() == 0) {
			backup();
		}
		else if (getSnapHelper().getNumSnapped() == 2) {
			m_goingToTerminate = true;
		}
		else {
			updateStatusbarText();
		}
		super.mouseReleased(e);
	}

	@Override protected boolean connectObjects()
	{
		// Get list of snapped hook ups using Snaphelper
		List<Pair<IDynamicSnap, IShieldBodyHookup>> hookups = getSnappedHookups();

		boolean success = false;
		if (hookups.size() == 2) {
			IShieldBodyHookup h1 = hookups.get(0).getSecond();
			IShieldBodyHookup h2 = hookups.get(1).getSecond();

			success = DaisyChainCreationHelper.okToConnectDaisyChain(h1, h2);
			if (success) {
				success = super.connectObjects();
				// Now we've added the chain, make sure the connectivity is right between the mcs it links together.
				if (success) {
					success = DaisyChainCreationHelper.connectShield(h1, h2);
					if (success) {
						DaisyChainCreationHelper.connectShield(h2, h1);
					}
				}
			}
		}

		return success;
	}

	@Override protected void backup()
	{
		super.backup();
		if (m_point_list.isEmpty()) {
			getSnapHelper().retreatSnap();
			noSnap = true;
			updateStatusbarText();
		}
	}

	@Override protected boolean validPointList(Collection<ISmartPoint> col)
	{
		if (!super.validPointList(col)) {
			return false;
		}
		Point firstPt = null;
		Point lastPt = null;

		boolean isFirst = true;
		for (ISmartPoint pt : col) {
			if (isFirst) {
				firstPt = pt.getAbsoluteLocation();
				isFirst = false;
			}
			lastPt = pt.getAbsoluteLocation();
		}
		if (firstPt == null || lastPt == null) {
			return false;
		}

		List<IShieldBodyHookup> usedHookups = new ArrayList<>();
		List<Pair<IDynamicSnap, IShieldBodyHookup>> hookups = getSnappedHookups();

		IShieldBodyHookup firstHook = getMatchingSnappedHook(firstPt, usedHookups, hookups);
		if (firstHook == null) {
			return false;
		}
		usedHookups.add(firstHook);
		IShieldBodyHookup secondHook = getMatchingSnappedHook(lastPt, usedHookups, hookups);
		return !(secondHook == null || firstHook == secondHook);
	}

	@Nullable private IShieldBodyHookup getMatchingSnappedHook(@NotNull Point point,
			List<IShieldBodyHookup> usedHookups, List<Pair<IDynamicSnap, IShieldBodyHookup>> hookups)
	{
		for (Pair<IDynamicSnap, IShieldBodyHookup> hookToSnap : hookups) {
			IDynamicSnap ds = hookToSnap.getFirst();
			IShieldBodyHookup hook = hookToSnap.getSecond();
			if (ds.getPoint().equals(point) && !usedHookups.contains(hook)) {
				return hook;
			}
		}
		return null;
	}

	@NotNull private List<Pair<IDynamicSnap, IShieldBodyHookup>> getSnappedHookups()
	{
		List<Pair<IDynamicSnap, IShieldBodyHookup>> firstClassHookups =
				new ArrayList<Pair<IDynamicSnap, IShieldBodyHookup>>();
		List<Pair<IDynamicSnap, IShieldBodyHookup>> secondClassHookups =
				new ArrayList<Pair<IDynamicSnap, IShieldBodyHookup>>();

		Collection<Pair<IDynamicSnap, Integer>> snapped = getSnapHelper().getAllSnapped();
		for (Pair<IDynamicSnap, Integer> snap : snapped) {

			IDynamicSnap ds = snap.getFirst();
			boolean firstHookup = true;
			for (Iterator<IDynamicGfxMediator> mitr = ds.getMediators(); mitr.hasNext(); ) {
				IDynamicGfxMediator m = mitr.next();
				if (m instanceof IShieldBodyHookup) {
					IShieldBodyHookup hookup = (IShieldBodyHookup) m;
					if (firstHookup) {
						firstClassHookups.add(new Pair<IDynamicSnap, IShieldBodyHookup>(ds,
								hookup)); // First one goes in a special pile.
					}
					else {
						secondClassHookups.add(new Pair<IDynamicSnap, IShieldBodyHookup>(ds, hookup));
					}
					firstHookup = false;
				}
			}
		}
		//
		// The list is broken into 2 parts.
		// - Hookups from unique Mediators.
		// - All the rest.
		//
		// this allows us to handle multiple hookups that are at the same point as well as
		// non-overlapping ones.
		//
		List<Pair<IDynamicSnap, IShieldBodyHookup>> hookups = new ArrayList<Pair<IDynamicSnap, IShieldBodyHookup>>();
		hookups.addAll(firstClassHookups);
		hookups.addAll(secondClassHookups);
		return hookups;
	}

	public String getStatusbarText()
	{
		if (noSnap) {
			return ResourceMgr.getString(AddChainAction.class, "AddChainAction.start.statusbar.text");
		}
		return ResourceMgr.getString(AddChainAction.class, "AddChainAction.inprogress.statusbar.text");
	}

	/**
	 * Return the cursor for this action
	 */
	public Cursor getCursor()
	{
		return m_lineCursor;
	}

	protected Collection<IDynamicGfxMediator> connectingObjects()
	{
		return m_connectingObjects;
	}
}
