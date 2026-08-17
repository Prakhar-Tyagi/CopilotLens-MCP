/*
 * Copyright 2005-2012 Mentor Graphics Corporation
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
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.browser.PartBrowserActionHelper;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGrid;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IInterconnectDevice;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.parts.ILibraryDevice;
import chs.cof.parts.ILibraryDeviceFootprint;
import chs.cof.parts.Library;
import chs.cof.parts.LibraryCriteriaHelper;
import chs.cof.parts.configure.ConfigurationTypeEnum;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.parts.partselector.ILibraryPartSelector;
import chs.cof.parts.partselector.PartSelectionContext;
import chs.cof.project.IProject;
import chs.cofUtils.parameterized.AddPinHelper;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.GeneratorStyle;
import chs.common.ICommonFactory;
import chs.common.IParameterized;
import chs.common.IProjectPreferenceMgr;
import chs.common.IUID;
import chs.common.criteria.ICriteria;
import chs.common.preferencesets.IPreferenceSet;
import chs.services.dynamicgfx.DynamicRotationIndicator;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.CHSConstants;
import chs.utilities.ResourceMgr;
import chs.utility.ConductorSplitter;
import chs.utility.DeviceConductorSplitter;
import chs.utility.DiagramHelper;
import chs.utility.LibraryPinFacade;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.logic.ILogicModel;
import chs.utility.logic.SizeHelper;
import chs.utility.preferences.PreferenceSetHelper;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.Iterator;

/**
 * This class exists for typing only.
 */
public class CreateInterconnectDeviceAction extends CreateParameterizedObjectAction
{

	private static Cursor m_deviceCursor = null;
	private Generator m_generator;
	private LibraryPinFacade m_facade;
	private ConductorSplitter m_splitter = new DeviceConductorSplitter();
	private ILibraryPartSelection m_librarySelection;

	public CreateInterconnectDeviceAction(ICapletController controller)
	{
		super(controller);
		if (m_deviceCursor == null) {
			m_deviceCursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/cur_device.gif", new Point(7, 7));
		}
		m_generator = Generator.getGenerator();
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		IDesign des = ((ILogicModel) getModel()).getDesign();
		IProject proj = des.getProject();

		m_librarySelection = PartBrowserActionHelper.getSelectedBrowserPart();
		if (m_librarySelection == null) {
			ICriteria<ILibraryDevice> criteria = LibraryCriteriaHelper.getCriteriaForDevicesWithNonZeroPins();
			ILibraryPartSelector partSelector = Library.getInstance()
					.getLibraryPartSelector(CAFUtils.getInstance().getWindowMgr().getDialogFrame());
			PartSelectionContext partSelectionContext = new PartSelectionContext();
			partSelectionContext.setSelectionFilter(LibraryCriteriaHelper.getSelectionFilterForElectricalSymbols(
					null, null, LibraryCriteriaHelper
							.getCustomerDetailsFromScopes(CAFUtils.getInstance().getActiveDesignContainer(), proj)
			));

			//@todo used library configuration context directly, needs confirmation - kjuthi
			m_librarySelection =
					partSelector.selectPart(criteria, proj, partSelectionContext, ConfigurationTypeEnum.LOGICAL,
							CAFUtils.getInstance().getActiveDesignContainer());
		}

		if (m_librarySelection == null) {
			//
			// No part selected -> Illegal interconnect device...
			//
			return IActionEnum.eCanceled;
		}
		//
		// Process the cavities, and get the DC and regular pins.
		//
		m_facade = new LibraryPinFacade(m_librarySelection.getSelectedObject(),
				m_librarySelection.getSelectedFootprint(), IInterconnectDevice.class, true);
		//
		// Continue on to regular placement.
		//
		return super.onActivate(e);
	}

	protected double calculateBorderSize()
	{
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		GfxView gview = (GfxView) view;
		ISchemDiagram diagram = (ISchemDiagram) gview.getSheet();
		IGrid grid = diagram.getGrid();
		GeneratorParameters gp = new GeneratorParameters(grid);
		ICommonFactory cFac = FactoryMgr.getCommonFactory();
		IParameterized params = cFac.createParameterized();
		//
		// Get the m_generator, add the defaults, and go!
		//
		GeneratorStyle gs = m_generator.getStyle();
		if (gs != null) {
			gs.addDefaults(params, getObjectType(), "interconnect");
		}
		double borderSize = calculateBorderSize(gp, params);

		UIDMgr.removeObject(params.getUID());
		CreationDeletionHelper.getTheCreationHelper().removeCreationObject(params);

		return borderSize;
	}

	@Override protected String getObjectType()
	{
		return "device";
	}

	//
	// Create the interconnect object for placement
	//
	protected IGfxObject createParamObject(Point p1, Point p2)
	{
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		GfxView gview = (GfxView) view;
		ISchemDiagram diagram = (ISchemDiagram) gview.getSheet();
		IGrid grid = diagram.getGrid();
		int pinspacing = grid.getGridSpacing();
		GeneratorParameters gp = DiagramHelper.createGeneratorParameters(getDiagram());
		gp.setNewObject(true);
		DynamicRotationIndicator indicator = getRotationIndicator();
		ICommonFactory cFac = FactoryMgr.getCommonFactory();
		IParameterized params = cFac.createParameterized();
		//
		// Get the m_generator, add the defaults, and go!
		//
		GeneratorStyle gs = m_generator.getStyle();
		if (gs != null) {
			gs.addDefaults(params, "device", "interconnect");
		}
		SizeHelper sizeH = new SizeHelper(p1, p2, indicator.getVertical(), params, gp);
		sizeH.setMinModelWidth(CHSConstants.PIN_SPACING);

		//
		// Get the boundary markers
		//   1] Max number of DC pins to place down on each side.
		//   2] Graphical size of the DCs
		//   3] Loose Pins Per Side
		//
		int width = sizeH.getModelWidth();

		int uniqueRails = 2;
		if (width == 0) {
			// No width -> All on 1 line - a single rail!.
			uniqueRails = 1;
		}

		int maxDCsPerSide = (m_facade.getDevConnPinNames().size() + 1) / uniqueRails;
		int cordonAroundDCPin = 1; // 1 pin either side.
		int singleDCPinArea = (cordonAroundDCPin * 2) * pinspacing;
		int fullDCOffsetPerSide = (maxDCsPerSide * singleDCPinArea) + (cordonAroundDCPin * pinspacing);
		int loosePinsPerSide = (m_facade.getFloatingPinNames().size() + 1) / uniqueRails;
		int neededHeight = fullDCOffsetPerSide + (loosePinsPerSide * pinspacing);
		sizeH.setMinModelHeight(neededHeight);
		int height = sizeH.getModelHeight();

		Point lowerLeft = sizeH.getModelLocation();
		//
		// Now we need to place everything down, and get the graphics generated.
		//
		chs.common.IUID uid = cFac.createUID();
		chs.cof.logical.cable.IInterconnectDevice device =
				FactoryMgr.getCablePropertiedFactory().createInterconnectDevice(uid);

		diagram.getDesign().getConnectivity().addInterconnectDevice(device);

		uid = cFac.createUID();
		IPinList schem_dev = FactoryMgr.getSchemFactory().constructPinList(uid, device, lowerLeft.x, lowerLeft.y);
		diagram.addObject(schem_dev);
		schem_dev.setParameterized(params);

		IProjectPreferenceMgr preferences = CAFUtils.getInstance().getCurrentProjectPreferences();

		int ypos;
		int xpos;
		int idx = 0;
		for (Iterator itr = m_facade.getDevConnPinNames().iterator(); itr.hasNext(); idx++) {
			String name = (String) itr.next();
			ypos = height - ((cordonAroundDCPin * pinspacing) + ((idx / uniqueRails) * singleDCPinArea));
			xpos = (idx % uniqueRails == 0) ? 0 : width;

			placePin(schem_dev, grid, name, xpos, ypos, true);
		}
		//
		// Next, we do the unmatchedPins
		//
		idx = 0;
		for (Iterator itr = m_facade.getFloatingPinNames().iterator(); itr.hasNext(); idx++) {
			String name = (String) itr.next();

			ypos = height - (fullDCOffsetPerSide + ((idx / uniqueRails) * singleDCPinArea));
			xpos = (idx % uniqueRails == 0) ? 0 : width;

			placePin(schem_dev, grid, name, xpos, ypos, false);
		}
		//
		// This area is the extent of the box where the pins would go.
		//
		params.setExtent(cFac.constructExtent(0, 0, width, height));
		m_generator.generateDevice(schem_dev, gp, Generator.REGENERATE_PROPERTIES);

		// FEAT 3271: Removed pin styling. It is done in AddPinHelper.generatePin().

		sizeH.rotateModel(schem_dev);

		// Tie to library object...
		// DR 348343: We defer this till after we've generated the device, so properties inherited from the
		// library part are not displayed. Note that the same ordering is used in
		// AddParameterizedDeviceFromLibraryPartAction.onTerminate() to prevent library properties appearing
		// by default on non-interconnect devices.

		ILibraryDeviceFootprint fp = m_librarySelection.getSelectedFootprint();
		if (fp != null) {
			device.setFootprintDescription(fp.getFootprintName());
			device.setFootprintId(fp.getUID());
		}

		device.assignLibraryDetails(m_librarySelection);

		return schem_dev;
	}

	protected void connectGfxObjectToModel(IGfxObject newObject)
	{
		super.connectGfxObjectToModel(newObject);
		//
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		GfxView gview = (GfxView) view;
		ISchemDiagram diagram = (ISchemDiagram) gview.getSheet();
		GeneratorParameters gp =
				DiagramHelper.createGeneratorParameters(diagram);
		//
		IPinList pl = (IPinList) newObject;
		m_generator.rebuildDeviceConnectors(pl, gp, null);
		m_splitter.split((IPinList) newObject, ((GfxView) CAFUtils.getInstance().getActiveCapletView()).getSheet());
	}

	private IPin placePin(chs.cof.logical.schem.IPinList schem_dev, IGrid grid, String name, int x, int y,
			boolean isInterconnect)
	{
		ICommonFactory cFac = FactoryMgr.getCommonFactory();

		IUID uid = cFac.createUID();
		chs.cof.logical.cable.IPinList cpl = schem_dev.getConnectivity();
		chs.cof.logical.cable.IAbstractPin cpin = FactoryMgr.getCablePropertiedFactory().createPinForOwner(uid, cpl);
		cpin.setInterconnect(isInterconnect);
		cpin.setOwner(cpl);
		cpin.setName(name);
		cpl.addPin(cpin);

		IPreferenceSet styleSet = PreferenceSetHelper.getStyleSet(getDiagram());
		IPin pin = AddPinHelper.generatePin(schem_dev, cpl, x, y, grid, cpin, styleSet);
		return pin;
	}

	//
	// Explicitly don't return anything, as this is used for the tooltip.
	//
	public String getFeedbackText()
	{
		return null;
	}

	/**
	 * Gets the ActionUIClass attribute of the CreateCircleAction object
	 *
	 * @return The ActionUIClass value
	 */
	public String getActionUIClass()
	{
		return CreateInterconnectDeviceActionUI.class.getName();
	}

	protected Class snappingSource()
	{
		return IInterconnectDevice.class;
	}

	public String getStatusbarText()
	{
		return ResourceMgr
				.getString(CreateInterconnectDeviceAction.class, "CreateInterconnectDeviceActionUI.StatusBar.text");
	}

	/**
	 * Return the cursor for this action
	 */
	public Cursor getCursor()
	{
		return m_deviceCursor;
	}
}


