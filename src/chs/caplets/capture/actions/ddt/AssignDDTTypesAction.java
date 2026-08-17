/*
 * Copyright 2005-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.capture.actions.ddt;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caplets.capture.actions.ddt.transmodel.DeviceFieldModel;
import chs.caplets.capture.actions.ddt.transmodel.PinFieldModel;
import chs.caplets.capture.actions.ddt.transmodel.PinTableRow;
import chs.cof.draw.HorizJustificationEnum;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGfxObjectIterator;
import chs.cof.draw.IGrid;
import chs.cof.draw.VertJustificationEnum;
import chs.cof.drawplus.IDiagramText;
import chs.cof.drawplus.IDrawPlusFactory;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.ICableFactory;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IPinPlaceholder;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemFactory;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.project.IProject;
import chs.cof.project.ddtrans.IDDTType;
import chs.cof.project.ddtrans.IDDTTypeMgr;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.common.ICommonFactory;
import chs.common.IProjectPreferenceMgr;
import chs.common.IPropertiedObject;
import chs.common.IProperty;
import chs.common.IUIDObject;
import chs.common.attr.IAttributeTypes;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utility.DiagramHelper;
import chs.utility.helpers.GridHelper;
import chs.utility.helpers.TextHelper;
import chs.utility.logic.LogicUtils;

import javax.swing.Action;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Action to assign a DDT Type to a device.
 */
public class AssignDDTTypesAction extends ControllerActionRT implements ICtxMenuProvider
{

	private AssignDDTDialog m_dialog;
	private String m_ctxCommand;
	private DeviceAndRep m_currentDevice = new DeviceAndRep();

	public AssignDDTTypesAction(ICapletController controller)
	{
		super(controller);

		Frame frame = CAFUtils.getInstance().getWindowMgr().getDialogFrame();
		m_dialog = new AssignDDTDialog(frame);
	}

	public void destroy()
	{
		super.destroy();    //To change body of overridden methods use File | Settings | File Templates.
		//
		// Get rid of myself...
		if (m_dialog == null || !m_dialog.isVisible()) {
			return;
		}
		m_dialog.setVisible(false);
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eCompleted;
	}

	protected boolean onTerminate(boolean successful)
	{
		Frame frame = CAFUtils.getInstance().getWindowMgr().getDialogFrame();
		m_dialog = new AssignDDTDialog(frame);
		boolean ok = true;
		if (successful) {
			IProject proj = CAFUtils.getInstance().getCurrentProject();
			getOperand(getController().getSelectMgr().getCurrentSelections(), m_currentDevice);
			IDDTTypeMgr typeMgr = proj.getDDTTypeMgr();
			if ((typeMgr == null) || (typeMgr.getDDTTypes().size() == 0)) {
				String noTypesTitle =
						ResourceMgr.getString(AssignDDTTypesAction.class, "AssignDDTTypesAction.missingTypesTitle");
				String noTypesText =
						ResourceMgr.getString(AssignDDTTypesAction.class, "AssignDDTTypesAction.missingTypesText");
				MessageHelper.showErrorMessage(frame, noTypesTitle, noTypesText);
			}
			else if (m_currentDevice.schemDevice == null) {
				String noDevTitle =
						ResourceMgr.getString(AssignDDTTypesAction.class, "AssignDDTTypesAction.missingDeviceTitle");
				String noDevText =
						ResourceMgr.getString(AssignDDTTypesAction.class, "AssignDDTTypesAction.missingDeviceText");
				MessageHelper.showErrorMessage(frame, noDevTitle, noDevText);
			}
			else {
				boolean withSymbol = false;

				if (m_currentDevice.schemDevice.getSymbolRef() != null) {
					withSymbol = true;
				}
				m_dialog.initialize(m_currentDevice.logicDevice, typeMgr, withSymbol);
				//m_dialog.pack();
				m_dialog.setVisible(true);
				if (m_dialog.wasValidated()) {
					applyChanges(m_dialog);
				}
				else {
					ok = false;
				}
			}
		}
		return ok;
	}

	/**
	 * Given the dialog to assignt the DDT types to a device, this will apply the information edited by the user in that
	 * dialog.
	 *
	 * @param diag Dialog being used for edits.
	 */
	private void applyChanges(AssignDDTDialog diag)
	{
		IDDTType ddtType = diag.getCurrentType();
		m_currentDevice.logicDevice.setDDTType(ddtType);

		DeviceFieldModel deviceFields = diag.getDeviceFieldModel();
		Map fieldValuePairs = deviceFields.getFieldValuePairs();
		applyFields(m_currentDevice.logicDevice, fieldValuePairs);
		PinFieldModel pinFields = diag.getPinFieldModel();
		List pinRows = pinFields.getPinRows();
		int side = 0;
		int pinspacing = GridHelper.getGrid(m_currentDevice.schemDevice).getGridSpacing();
		Map<IAbstractPin, IPin> pinsToDelete = createPinsDeletedMap();
		Map placeHolderMap = createPlaceHolderMap(m_currentDevice.schemDevice);
		int yloc = findLowestLoc(placeHolderMap);
		for (Iterator itr = pinRows.iterator(); itr.hasNext(); ) {
			PinTableRow prt = (PinTableRow) itr.next();

			IAbstractPin existingPin = prt.getExistingPin();
			pinsToDelete.remove(existingPin); // Don't want to delete this pin
			if (existingPin == null) {
				existingPin = createNewPin(side, yloc, placeHolderMap);
				if (side == 0) {
					side = 1;
				}
				else {
					side = 0;
					yloc -= pinspacing;
				}
			}

			if (prt.isNameOverridden()) {
				String pinName = prt.getName();
				existingPin.setName(pinName);
			}
			applyFields(existingPin, prt.getFieldValues());
		}

		// Go through all the pins to be deleted
		for (Iterator<IPin> itr = pinsToDelete.values().iterator(); itr.hasNext(); ) {
			IPin schemPin = itr.next();

			deletePin(schemPin);
		}
		regenerateDevice();
	}

	/**
	 * This was lifted the delete helper.  The DeleteHelper wasn't very helpful.
	 *
	 * @param schemPin Pin to delete
	 */
	private void deletePin(IPin schemPin)
	{
		schemPin.detach();
		IAbstractPin connPin = schemPin.getConnectivity();
		schemPin.delete();
		int rcount = LogicUtils.getLogicObjectUsageCount(connPin);
		if (rcount == 0) {
			// If this pin is not shared then we delete the underlying connectivity also.
			chs.cof.logical.cable.IPinList pl = connPin.getOwner();
			// although it seems odd, delete the pin before removing it from the
			// pin list. The name manager will try to get the name of the object
			// during delete which will require that there is an owner.
			connPin.delete();
			// now that the pin is effectively gone, remove it from the pin list
			pl.removePin(connPin);
		}
	}

	/**
	 * Creates a map of connectivy pins -> schem pins. These are all the pins currently in the device
	 *
	 * @return Pin map
	 */
	private Map<IAbstractPin, IPin> createPinsDeletedMap()
	{
		LinkedHashMap<IAbstractPin, IPin> pinsToDelete = new LinkedHashMap<IAbstractPin, IPin>();
		for (IGfxObjectIterator gobjs = m_currentDevice.schemDevice.getObjects(); gobjs.hasNext(); ) {
			IGfxObject gobj = gobjs.getNext();
			if (gobj instanceof IPin) {
				IPin schemPin = (IPin) gobj;
				pinsToDelete.put(schemPin.getConnectivity(), schemPin);
			}
		}

		return pinsToDelete;
	}

	/**
	 * Given a graphics reprsentation of a device and a pin placeholder map, this will search the device from the bottom
	 * looking for the LAST place holder it finds. Since the place holder map is organized by location, this can be
	 * achieved by returning when no more place holders can be found.
	 *
	 * @param phMap Map of point -> pin place holders
	 *
	 * @return Y location of the lowest place holder
	 */
	private int findLowestLoc(Map phMap)
	{
		if (m_currentDevice.schemDevice.getParameterized() == null) {
			return 0;
		}
		IGrid grid = GridHelper.getGrid(m_currentDevice.schemDevice);
		int pinspacing = grid.getGridSpacing();

		Point searchPoint = new Point();

		// Work way up from 0 looking for largest place holder, if we find a pin, we're done.
		searchPoint.y = m_currentDevice.schemDevice.getParameterized().getExtent().getBottom();
		searchPoint.x = 0;
		boolean leftSide = true;
		while (phMap.get(searchPoint) != null) {
			if (leftSide) {
				leftSide = false;
			}
			else if (leftSide == false) {
				leftSide = true;
				searchPoint.y += pinspacing;
			}
		}
		searchPoint.y -= pinspacing;

		return searchPoint.y;
	}

	/**
	 * Generates a new represntation of the device being worked on
	 */
	private void regenerateDevice()
	{
		// At this point we've added pins that are outside our current boundaries. We need to stretch the
		// device out
		IGrid grid = GridHelper.getGrid(m_currentDevice.schemDevice);

		Generator generator = Generator.getGenerator();

		GeneratorParameters gp = DiagramHelper.createGeneratorParameters(m_currentDevice.schemDevice);
		generator.generateDevice(m_currentDevice.schemDevice, gp, Generator.REGENERATE_NAMEONLY);
	}

	/**
	 * Creates a map of Point -> place holders
	 *
	 * @param schemDev
	 *
	 * @return Map of java Point objects to the IPinPlaceholder objects
	 */
	private Map createPlaceHolderMap(IPinList schemDev)
	{
		LinkedHashMap phMap = new LinkedHashMap();
		for (IGfxObjectIterator gobjIter = schemDev.getObjects(); gobjIter.hasNext(); ) {
			IGfxObject nextObj = gobjIter.getNext();

			if (nextObj instanceof IPinPlaceholder) {
				IPinPlaceholder placeHolder = (IPinPlaceholder) nextObj;
				Point pt = new Point(placeHolder.getLocation().getX(),
						placeHolder.getLocation().getY());

				phMap.put(pt, nextObj);
			}
		}
		return phMap;
	}

	/**
	 * Creates a new pin
	 *
	 * @param side Which side of the device.0 = left, 1 = riht
	 * @param y Vertical location where to put the pin
	 *
	 * @return The pin.
	 */
	private IAbstractPin createNewPin(int side, int y, Map placeHolderMap)
	{
		ISchemFactory schemFactory = FactoryMgr.getSchemFactory();
		ICableFactory cableFactory = FactoryMgr.getCableFactory();
		ICommonFactory commonFactory = CAFUtils.getInstance().getCommonFactory();
		IDrawPlusFactory drawplusFactory = FactoryMgr.getDrawPlusFactory();
		IGrid grid = GridHelper.getGrid(m_currentDevice.schemDevice);

		IPinList schemDev = m_currentDevice.schemDevice;

		int x;
		x = schemDev.getParameterized().getExtent().getWidth() * side;
		Point searchPoint = new Point(x, y);
		IPinPlaceholder ph = (IPinPlaceholder) placeHolderMap.get(searchPoint);
		if (ph != null) {
			IPin realizedPin = ph.transmogrify(grid);
			return realizedPin.getConnectivity();
		}

		// At this point, no empty spaces were found for the pins, so regenerate it.
		IAbstractPin cablePin = cableFactory.createDevicePin(commonFactory.createUID());
		m_currentDevice.logicDevice.addPin(cablePin);

		chs.cof.logical.schem.IPin pin = schemFactory.constructPin(commonFactory.createUID(),
				cablePin, x, y);
		m_currentDevice.schemDevice.addObject(pin);

		IProjectPreferenceMgr preferences = CAFUtils.getInstance().getCurrentProjectPreferences();

		// If there are no styles defined, follow the existing flow
		IDiagramText nameText = drawplusFactory.constructAttributeText(commonFactory.createUID(), cablePin, 0, 0, 0,
				0, IAttributeTypes.NAME);

		final ISchemDiagram diagram = DiagramHelper.getDiagram(m_currentDevice.schemDevice);
		if (preferences != null && diagram != null && grid != null) {
			TextHelper.assignAttributeTextDefaults(nameText, diagram, grid, preferences);
		}
		nameText.setHorizontalJustification(
				(side == 0) ? HorizJustificationEnum.JustLeft : HorizJustificationEnum.JustRight);
		nameText.setVerticalJustification(VertJustificationEnum.JustCenter);
		pin.addObject(nameText);

		return cablePin;
	}

	private void applyFields(IPropertiedObject propObj, Map fieldValuePairs)
	{
		ICommonFactory comFact = CAFUtils.getInstance().getCommonFactory();
		for (Iterator itr = fieldValuePairs.entrySet().iterator(); itr.hasNext(); ) {
			Map.Entry entry = (Map.Entry) itr.next();

			String field = (String) entry.getKey();
			String value = (String) entry.getValue();

			IProperty prop = propObj.findPropertyByName(field);
			if (prop != null) {
				propObj.removeProperty(prop);
			}
			prop = comFact.constructProperty(field, value, propObj);
			propObj.addProperty(prop);
		}
	}

	public String getActionUIClass()
	{
		return AssignDDTTypesActionUI.class.getName();
	}

	// Enabled if there are any IParameterized objects selected.
	public boolean isEnabled()
	{
		if (!getController().getCapletModel().isEditable()) {// eg. read-only model
			return false;
		}
		return getOperand(getController().getSelectMgr().getPreSelections(), m_currentDevice) && super.isEnabled();
	}

	private static boolean getOperand(SelectSet selections, DeviceAndRep devAndRep)
	{
		int plCount = 0;
		for (SelectedUIDObjectIterator iter = selections.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject uidObj = iter.getNext();
			if (uidObj instanceof IPinList) {
				IPinList schem = (IPinList) uidObj;
				chs.cof.logical.cable.IPinList plistConn = schem.getConnectivity();
				if (plistConn instanceof IDevice) {
					if (!canAddDeletePin(schem)) {
						continue;
					}

					IDevice dev = (IDevice) plistConn;
					plCount++;
					devAndRep.logicDevice = dev;
					devAndRep.schemDevice = schem;
				}
			}
		}

		if (plCount == 1) {
			return true;
		}
		else {
			return false;
		}
	}

	private static boolean canAddDeletePin(IPinList schem)
	{
		//dts0100777045 Disable this action if
		// 1. Device is shared OR
		// 2. Design wide instance of device  OR
		// 3. Design wide instance of pins on selected Device schem
		// This needs to be removed once support for above cases is provided in future releases
		chs.cof.logical.cable.IPinList plistConn = schem.getConnectivity();
		if (plistConn.isShared()) {
			return false;
		}

		ILogicDesign design = plistConn.getLogicDesign();
		assert design != null;
		IDesignWideUsageMgr dwum = design.getDesignWideUsageMgr();
		if (dwum.getDesignSharedUsageCount(plistConn) > 1) {
			return false;
		}

		for (IPin schemPin : schem.getPins()) {
			IAbstractPin cablePin = schemPin.getConnectivity();
			if (dwum.getDesignSharedUsageCount(cablePin) > 1) {
				return false;
			}
		}

		// Is this really required ? Stack pins are not visible for this action. Need to support this flow in future releases
		for (ISchemStackPin pinStack : schem.getStackPins()) {
			for (IAbstractPin cablePin : pinStack.getAllConnectivity()) {
				if (dwum.getDesignSharedUsageCount(cablePin) > 1) {
					return false;
				}
			}
		}
		return true;
	}

	// Put ourselves in the context menu if there are
	// any IParameterized objects selected.
	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (isEnabled()) { // && getOperand(selections) != null) {
			String shortDesc = (String) getActionUI().getValue(Action.SHORT_DESCRIPTION);
			if (m_ctxCommand == null || !m_ctxCommand.equalsIgnoreCase(shortDesc)) {
				// Make a private copy for command name
				m_ctxCommand = shortDesc;
			}
			container.add(new ActionEntry(getActionUI(), m_ctxCommand));
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	/**
	 * Just to encapsulate the representation and logic we are working with. Ease of use thing.
	 */
	class DeviceAndRep
	{

		public IDevice logicDevice;
		public IPinList schemDevice;

		public DeviceAndRep()
		{
		}
	}
}
