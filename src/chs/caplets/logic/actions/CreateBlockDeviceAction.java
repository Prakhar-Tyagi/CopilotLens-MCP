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
import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.ICableFactory;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.ICommonFactory;
import chs.common.IUID;
import chs.images.CHSImages;
import chs.services.dynamicgfx.DynamicRotationIndicator;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.CompositeConnectivityFinder;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.awt.Point;

/**
 * This class exists for typing only.
 */
public class CreateBlockDeviceAction extends CreateParameterizedObjectAction
{

	private static Cursor cursor = null;

	private static final String m_objType = "block";

	public CreateBlockDeviceAction(ICapletController controller)
	{
		super(controller);
		if (cursor == null) {
			cursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), CHSImages.BLOCKDEVICE_CURSOR, new Point(7, 7));
		}
	}

	protected boolean shouldShowFeedback()
	{
		return false;
	}

	protected double calculateBorderSize()
	{
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		GfxView gview = (GfxView) view;
		ISchemDiagram diagram = (ISchemDiagram) gview.getSheet();
		return calculateBorderSize(diagram);
	}

	protected String getObjectType()
	{
		return m_objType;
	}

	/**
	 * Gets the ActionUIClass attribute of the CreateCircleAction object
	 *
	 * @return The ActionUIClass value
	 */
	public String getActionUIClass()
	{
		return CreateBlockDeviceActionUI.class.getName();
	}

	protected IGfxObject createParamObject(Point p1, Point p2)
	{
		IBlockDevice device = createConnectivityBlockDevice();

		// Create our schem device
		IPinList schemBlockDevice = createSchemBlockDevice(device, p1, p2, getRotationIndicator());
		return schemBlockDevice;
	}

	protected IBlockDevice createConnectivityBlockDevice()
	{
		final ICableFactory cblFactory = FactoryMgr.getCablePropertiedFactory();
		final ICommonFactory commonFactory = FactoryMgr.getCommonFactory();
		IUID uid = commonFactory.createUID();
		IBlockDevice device = createBlock(cblFactory, uid);
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		GfxView gview = (GfxView) view;
		ISchemDiagram diagram = (ISchemDiagram) gview.getSheet();
		addIntoConnectivity(device, diagram.getDesign().getConnectivity());
		return device;
	}

	protected void addIntoConnectivity(IBlockDevice device, IConnectivity connectivity)
	{
		IConnectivity conn = connectivity;
		assert conn != null; // to keep IJ happy
		conn.addBlockDevice(device);
	}

	protected IBlockDevice createBlock(ICableFactory cblFactory, IUID uid)
	{
		return cblFactory.createBlockDevice(uid);
	}

	public static IPinList createSchemBlockDevice(IBlockDevice device, Point pt1, Point pt2,
			DynamicRotationIndicator indicator)
	{
		return createSchemPinList(device, pt1, pt2, false, indicator, m_objType);
	}

	protected Model getLocalModel()
	{
		return (Model) getModel();
	}

	/**
	 * Return the cursor for this action
	 */
	public Cursor getCursor()
	{
		return cursor;
	}

	@Override protected void connectGfxObjectToModel(IGfxObject newObject)
	{
		if (newObject instanceof IPinList) {
			GfxView gview = (GfxView) CAFUtils.getInstance().getActiveCapletView();
			final ILogicDesign design = getDiagram().getDesign();
			assert design != null;
			CompositeConnectivityFinder finder = new CompositeConnectivityFinder(design);
			finder.connect((IPinList) newObject, gview, allowPinCreationAtPlaceholders(), true);
		}
	}

	/**
	 * Set the status text for this action
	 */
	@Nullable
	public String getStatusbarText()
	{
		return ResourceMgr.getString(CreateBlockDeviceAction.class,
				"CreateBlockDeviceAction.StatusBar.NoPins.text");
	}
}
