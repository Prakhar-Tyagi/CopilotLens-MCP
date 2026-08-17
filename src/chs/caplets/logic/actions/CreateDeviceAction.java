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
import chs.caf.caplet.ICapletView;
import chs.caplets.logic.Model;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ICableFactory;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.ICommonFactory;
import chs.common.IUID;
import chs.services.dynamicgfx.DynamicRotationIndicator;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.utility.ConductorSplitter;
import chs.utility.DeviceConductorSplitter;
import chs.utility.helpers.LogicObjectLockFinder;
import org.jetbrains.annotations.NotNull;

import java.awt.Cursor;
import java.awt.Point;

/**
 * This class exists for typing only.
 */
public class CreateDeviceAction extends CreateParameterizedObjectAction
{

	private ConductorSplitter m_splitter = new DeviceConductorSplitter();

	private static Cursor m_deviceCursor = null;

	private static final String m_objType = "device";

	public CreateDeviceAction(ICapletController controller)
	{
		super(controller);
		if (m_deviceCursor == null) {
			m_deviceCursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/cur_device.gif", new Point(7, 7));
		}
	}

	@Override protected String getObjectType()
	{
		return m_objType;
	}

	@Override protected double calculateBorderSize()
	{
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		GfxView gview = (GfxView) view;
		ISchemDiagram diagram = (ISchemDiagram) gview.getSheet();
		return calculateBorderSize(diagram);
	}

	/**
	 * Gets the ActionUIClass attribute of the CreateCircleAction object
	 *
	 * @return The ActionUIClass value
	 */
	@Override public String getActionUIClass()
	{
		return CreateDeviceActionUI.class.getName();
	}

	@Override protected IGfxObject createParamObject(Point p1, Point p2)
	{
		// Create our connectivity device
		IDevice device = createConnectivityDevice();

		// Create our schem device
		IPinList schemPinList = createSchemDevice(device, p1, p2, shouldAddPins(), getRotationIndicator());
		return schemPinList;
	}

	@NotNull private IDevice createConnectivityDevice()
	{
		final ICableFactory cblFactory = FactoryMgr.getCablePropertiedFactory();
		final ICommonFactory commonFactory = FactoryMgr.getCommonFactory();
		IUID uid = commonFactory.createUID();
		IDevice device = cblFactory.createDevice(uid);
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		GfxView gview = (GfxView) view;
		ISchemDiagram diagram = (ISchemDiagram) gview.getSheet();
		ILogicDesign logicDesign = diagram.getDesign();
		assert logicDesign != null;
		IConnectivity conn = logicDesign.getConnectivity();
		assert conn != null; // to keep IJ happy
		conn.addDevice(device);
		LogicObjectLockFinder.tryEdit(logicDesign, device);
		return device;
	}

	public static IPinList createSchemDevice(IDevice device, Point pt1, Point pt2, boolean addPins,
			DynamicRotationIndicator indicator)
	{
		return createSchemPinList(device, pt1, pt2, addPins, indicator, m_objType);
	}

	/**
	 * Should this action automatically add pins to the device?
	 * <p>
	 * The default behaviour of Add Device with/without Pins is toggled by pressing Ctrl as the device is placed.
	 *
	 * @return boolean
	 */
	protected boolean shouldAddPins()
	{
		return true;
	}

	/**
	 * Should this action support addition of pins on split. Some action should never allow addition of pins on split.
	 * i.e When we are adding a device from a library part we should not allow addition of pins on split.
	 *
	 * @return boolean default to true
	 */
	protected boolean allowAddPinsOnSplit()
	{
		return true;
	}

	@Override protected void connectGfxObjectToModel(IGfxObject newObject)
	{
//		super.connectGfxObjectToModel(newObject);
		if (newObject instanceof IPinList) {
			GfxView gview = (GfxView) CAFUtils.getInstance().getActiveCapletView();
			final ILogicDesign design = getDiagram().getDesign();
			assert design != null;
			m_splitter.allowPinAddition(allowAddPinsOnSplit());

			m_splitter
					.splitConductors((IPinList) newObject, gview, allowPinCreationAtPlaceholders(), true, isCtrlDown(),
							() -> {});
		}
	}

	@Override protected Model getLocalModel()
	{
		return (Model) getModel();
	}

//	protected Class snappingSource()
//	{
//		return IPinList.class;
//	}

	/**
	 * Return the cursor for this action
	 */
	@Override public Cursor getCursor()
	{
		return m_deviceCursor;
	}
}
