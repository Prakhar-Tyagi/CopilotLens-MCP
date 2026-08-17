/*
 * Copyright 2002-2010 Mentor Graphics Corporation
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
import chs.caf.caplet.helpers.browser.PartBrowserActionHelper;
import chs.caf.caplet.helpers.snapping.ISnapThroughConnectorController;
import chs.caf.caplet.helpers.snapping.SchemConnectorPlaceholder;
import chs.caf.caplet.helpers.snapping.SnapHelper;
import chs.caf.caplet.helpers.snapping.SnapThroughConnectorHelper;
import chs.caf.helpers.ui.std.DesignAbstractionHelper;
import chs.cof.logical.cable.IFunctionConductor;
import chs.cof.logical.cable.IFunctionMessage;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cofUtils.cmd.CreateSchemConductorCmd;
import chs.common.DesignAbstractionType;
import chs.images.CHSImages;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.IDynamicGfxMediator;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A create tool to make a conductor.
 * <p/>
 * created April 12, 2002
 */
public class CreateConductorAction extends CreateBaseConductorAction implements ISnapThroughConnectorController
{

	private ILibraryPartSelection m_libWire;

	/**
	 * Constructor for the CreateConductorAction object
	 *
	 * @param controller Description of the Parameter
	 */
	public CreateConductorAction(ICapletController controller)
	{
		super(controller, true, false);
	}

	/**
	 * Gets the ActionUIClass attribute of the CreateCircleAction object
	 *
	 * @return The ActionUIClass value
	 */
	public String getActionUIClass()
	{
		return CreateConductorActionUI.class.getName();
	}

	/**
	 * Access the command object which contains the details of the conductor to be created.
	 * <p/>
	 * This is protected as will only be valid at certain points in the action's lifecyle - it is created on activation
	 * and deleted on action termination
	 *
	 * @return the command object
	 */
	protected CreateSchemConductorCmd getCommand()
	{
		return m_cmd;
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		m_libWire = PartBrowserActionHelper.getSelectedBrowserPart();
		m_cmd = new CreateSchemConductorCmd();
		return super.onActivate(e);
	}

	@NotNull protected Class<? extends chs.cof.logical.cable.IConductor> getConductorType()
	{
		return getSelectedConductorObject() instanceof IWireConductor ?
				IWireConductor.class : INetConductor.class;
	}

	@Override protected void assignLibraryDetails()
	{
		IConductor cond = m_cmd.getConductor();
		if (m_libWire != null) {
			cond.getConnectivity().assignLibraryDetails(m_libWire);
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
		if (getSelectedConductorObject() instanceof IWireConductor) {
			return IWireConductor.class;
		}
		return INetConductor.class;
	}

	/**
	 * Description of the Method
	 *
	 * @return Description of the Return Value
	 */
	protected Collection<IDynamicGfxMediator> connectingObjects()
	{
		return m_connectingObjects;
	}

	/**
	 * Return the cursor for this action
	 */
	public Cursor getCursor()
	{
		return CAFUtils.getInstance()
				.loadCursor(getController().getCaplet(), getCursorImage(), new Point(7, 7));
	}

	@Nullable protected chs.cof.logical.cable.IConductor getSelectedConductorObject()
	{
		return null;
	}

	protected String getCursorImage()
	{
		chs.cof.logical.cable.IConductor selectedConductorObject = getSelectedConductorObject();
		if (selectedConductorObject instanceof IWireConductor) {
			return "chs/images/app/cur_wire.gif";
		}
		if (selectedConductorObject instanceof IFunctionConductor) {
			return CHSImages.FUNCTIONCODUCTOR_ADD_CURSOR;
		}
		if (selectedConductorObject instanceof IFunctionMessage) {
			return CHSImages.FUNCTIONMESSAGE_ADD_CURSOR;
		}
		return "chs/images/app/cur_net.gif";
	}

	@Override public boolean isSnapThroughConnectorEnabled()
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
		return IWireConductor.class.isAssignableFrom(snappingSource()) && m_point_list != null &&
				m_point_list.size() > 1 &&
				m_pointToRecordedSnap.remove(m_point_list.get(m_point_list.size() - 1)) != null;
	}

	@Override public boolean isConnectionAvailable(@NotNull SchemConnectorPlaceholder connectorSnap)
	{
		return SnapThroughConnectorHelper.isConnectionAvailable(connectorSnap, getSnapSourceObject());
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

	@Override protected void cleanUpPostAction()
	{
		super.cleanUpPostAction();
		SnapThroughConnectorHelper.clearCachedObjects();
	}

	/*
	For Smart Flows, add line action uses CreateConductorAction
	For System block, we don't use nets
	 */
	@Override public boolean isEnabled()
	{
		DesignAbstractionType designAbstraction = DesignAbstractionHelper.getTypeOfDesignAbstraction();
		Set<DesignAbstractionType> abstractionTypes =
				new LinkedHashSet<>(
						Arrays.asList(DesignAbstractionType.SYTEM_BLOCK, DesignAbstractionType.FLUID));
		if (designAbstraction != null && abstractionTypes.contains(designAbstraction)) {
			return false;
		}
		return super.isEnabled();
	}
}