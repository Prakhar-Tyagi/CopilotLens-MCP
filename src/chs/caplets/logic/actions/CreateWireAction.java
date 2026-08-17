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
import chs.caf.caplet.helpers.snapping.ISnapThroughConnectorController;
import chs.caf.caplet.helpers.snapping.SnapHelper;
import chs.caf.caplet.helpers.snapping.SnapThroughConnectorHelper;
import chs.caf.helpers.ui.std.DesignAbstractionHelper;
import chs.caf.helpers.ui.std.UIManager;
import chs.caplets.logic.Model;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cofUtils.cmd.CreateSchemConductorCmd;
import chs.common.DesignAbstractionType;
import chs.services.dynamicgfx.DynamicPolyline;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.ISmartPoint;
import chs.services.gfx.GfxView;
import chs.utilities.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * A create tool to make a conductor.
 *
 * @created April 12, 2002
 */
public class CreateWireAction extends LogicMultipointCreateAction implements ISnapThroughConnectorController
{
	public enum CONDUCTOR_TYPE
	{
		ADD_WIRE,
		ADD_NET
	}
	private CONDUCTOR_TYPE m_condType = CONDUCTOR_TYPE.ADD_WIRE;

	protected ILibraryPartSelection m_libWire;
	private List m_connectingObjects;
	private DynamicPolyline m_dynLine;

	static private Cursor m_wireCursor = null;
	private CreateSchemConductorCmd m_cmd;

	/**
	 * Constructor for the CreateWireAction object
	 *
	 * @param controller Description of the Parameter
	 */
	public CreateWireAction(ICapletController controller)
	{
		super(controller, true, false);

		if (m_wireCursor == null) {
			m_wireCursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/cur_wire.gif", new Point(7, 7));
		}
	}

	/**
	 * Wire creation uses guidelines
	 */
	protected Set<Class<?>> guideLineClasses()
	{
		Set<Class<?>> result = new HashSet<Class<?>>();
		result.add(IWireConductor.class);
		result.add(IGeneralHighway.class);

		return result;
	}

	/**
	 * Gets the ActionUIClass attribute of the CreateCircleAction object
	 *
	 * @return The ActionUIClass value
	 */
	public String getActionUIClass()
	{
		return CreateWireActionUI.class.getName();
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		m_libWire = PartBrowserActionHelper.getSelectedBrowserPart();
		m_cmd = new CreateSchemConductorCmd(ConductorRouteAction.getInstance());
		return super.onActivate(e);
	}

	@Override protected void cleanUpPostAction()
	{
		super.cleanUpPostAction();
		SnapThroughConnectorHelper.clearCachedObjects();
	}

	public CreateSchemConductorCmd getCommand()
	{
		return m_cmd;
	}

	/**
	 */
	protected IGfxObject constructDisplayObject(List<ISmartPoint> smartPoints)
	{
		List<Point> points = new ArrayList<Point>(smartPoints.size());
		for (ISmartPoint spt : smartPoints) {
			points.add(spt.getAbsoluteLocation());
		}

		m_cmd.setDesign(getLocalModel().getDesign());
		m_cmd.setDiagram((ISchemDiagram) getModel().getSheet());
		m_cmd.setOrthoMode(m_orthoMode);
		m_cmd.setPoints(points);
		if (m_condType == CONDUCTOR_TYPE.ADD_WIRE) {
			m_cmd.setConductorType(IWireConductor.class);
		}
		else {
			m_cmd.setConductorType(INetConductor.class);
		}
		m_cmd.setPrune(true);

		m_cmd.execute();
		IConductor cond = m_cmd.getConductor();
		m_connectingObjects = m_cmd.getSegments();
		if (m_libWire != null) {
			cond.getConnectivity().assignLibraryDetails(m_libWire);
		}
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
		return IWireConductor.class;
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
		return m_wireCursor;
	}

	protected void setConductorType(CONDUCTOR_TYPE condType)
	{
		m_condType = condType;
	}

	public boolean isSnapThroughConnectorEnabled()
	{
		return getLocalModel().getAutoGenerateConnectorMode();
	}

	@Override
	@Nullable public chs.cof.logical.cable.IConductor getSnapSourceObject()
	{
		return mCreateCondInstanceHelper.getConductor();
	}

	@NotNull protected SnapHelper createSnapHelper()
	{
		return new SnapThroughConnectorHelper(this, m_snapToGrid, m_snapToSubGrid);
	}

	@Override public boolean overrideLastSnapped()
	{
		return m_point_list != null && m_point_list.size() > 1 &&
				m_pointToRecordedSnap.remove(m_point_list.get(m_point_list.size() - 1)) != null;
	}

	@Override public void keyPressed(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_CONTROL && !isKeyPressed(e.getKeyCode())) {
			GfxView gfxView = CommonUtils.cast(CAFUtils.getInstance().getActiveCapletView(), GfxView.class);
			if (gfxView != null) {
				Point currentMousePoint = getCurrentMousePoint(gfxView);
				recordKeyPress(e.getKeyCode());
				triggerMouseMoveEvent(gfxView, currentMousePoint, InputEvent.CTRL_DOWN_MASK);
			}
		}
		else {
			super.keyPressed(e);
		}
	}

	@Override public void keyReleased(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_CONTROL && isKeyPressed(e.getKeyCode())) {
			GfxView gfxView = CommonUtils.cast(CAFUtils.getInstance().getActiveCapletView(), GfxView.class);
			if (gfxView != null) {
				Point currentMousePoint = getCurrentMousePoint(gfxView);
				discardKeyPress(e.getKeyCode());
				triggerMouseMoveEvent(gfxView, currentMousePoint, 0);
			}
		}
		else {
			super.keyReleased(e);
		}
	}
	/*
	Removing Smart Flows because it uses CreateConductorAction
	For logical only nets are supported and
	smart flows only highway are supported
	 */
	@Override public boolean isEnabled()
	{
		if (!super.isEnabled()) {
			return false;
		}
		DesignAbstractionType designAbstraction = DesignAbstractionHelper.getTypeOfDesignAbstraction();
		if (designAbstraction == null) {
			return true;
		}

		return shouldAllowInCurrentAbstraction(designAbstraction);
	}

	protected boolean shouldAllowInCurrentAbstraction(@NotNull DesignAbstractionType designAbstractionType)
	{
		Set<DesignAbstractionType> disallowedAbstractionTypes =
				new LinkedHashSet<>(Arrays.asList(DesignAbstractionType.LOGICAL,
						DesignAbstractionType.SYTEM_BLOCK, DesignAbstractionType.SMART_FLOWS));
		return !disallowedAbstractionTypes.contains(designAbstractionType);
	}
}