/*
 * Copyright 2002-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.symbol.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.IUpdateableAction;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.action.IActionUI;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionIterator;
import chs.caplets.symbol.Model;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGrid;
import chs.cof.draw.IGriddable;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IDiagramText;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.schem.IGenericSchemPin;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.ISymbolDef;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.IProjectPreferenceMgr;
import chs.common.IUID;
import chs.common.Location;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.system.FactoryMgr;
import chs.utilities.SetMap;
import chs.utility.SymbolUtils;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.TextHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Set;

public class AddPinAction extends ControllerActionRT implements ICtxMenuProvider, MouseListener, MouseMotionListener
{

	protected Model m_model;
	private IGrid m_grid;
	/**
	 * A handle to our dynamic graphics service for convenience.
	 */
	private IDynamicGfxService m_dynamics;
	//private Vector m_transients;
	private IGfxObject m_dummyPin;
	protected SetMap<String, IGenericSchemPin> m_pinLocations;

	protected Point m_currValidPoint;
	private String m_ctxCommand = null;
	private IAddPinActionHelper actionHelper;

	public AddPinAction(ICapletController controller)
	{
		super(controller);
		m_model = (Model) controller.getCapletModel();
		m_dynamics = m_model.getDynamicGfxService();
		m_dummyPin = FactoryMgr.getDrawFactory().constructRectangle(0, 0, 0, 0);
		m_grid = ((IGriddable) m_model.getSheet()).getGrid();
		actionHelper = createAddPinActionHelper();
	}

	protected IAddPinActionHelper createAddPinActionHelper()
	{
		return new AddPinActionHelper(m_model);
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		if (!isValidToPerformAction()) {
			return IActionEnum.eCanceled;
		}

		m_pinLocations = SymbolUtils.getPinLocationsAndPins(m_model.getSheet());

		return IActionEnum.eActivated;
	}

	public boolean isValidToPerformAction()
	{
		return actionHelper.isValidToPerformAction();
	}

	public boolean onTerminate(boolean successful)
	{
		// Cleanup the transient graphics
		m_dynamics.removeAllDynamicGfx();
		m_dynamics.removeAllTransientGfx();
		//
		if (successful && m_currValidPoint != null) {
			// Add the pin...
			// create the new pin in the symbol...
			IGenericPin cpin = createCablePin();

			//
			// And a schematic pin
			//
			IGenericSchemPin pin = createSchemPin(cpin);

			//
			// Now we have the pin added, add the name text.
			//
			addNameText(pin);
			connectToLink(pin);
		}
		//
		// Refresh
		//
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eFull);
		}
		Action act = getActionUI();
		if (act instanceof IActionUI) {
			((IUpdateableAction) act).updateUI();
		}
		return true;
	}

	protected void connectToLink(IGenericSchemPin pin)
	{
		pin.connectToLink(new Location(m_currValidPoint.x, m_currValidPoint.y),
				(ISymbolDef) m_model.getSymbolDef());
	}

	private IGenericSchemPin createSchemPin(IGenericPin cpin)
	{
		IUID uid = FactoryMgr.getCommonFactory().createUID();
		IGenericSchemPin pin = createSchemPin(uid, cpin);
		ICompoundObject gfx = m_model.getSheet();
		gfx.addObject(pin);
		return pin;
	}

	private void addNameText(IGenericSchemPin pin)
	{
		IDiagramText nameText = TextHelper.getTextRepresentationWithCreate(pin, TextHelper.TEXT_NAME);

		if (nameText != null) {
			IProjectPreferenceMgr preferences = CAFUtils.getInstance().getCurrentProjectPreferences();
			final IBaseDiagram diagram = getBaseDiagram();
			if (preferences != null && diagram != null) {
				TextHelper.assignAttributeTextDefaults(nameText, diagram, m_grid, preferences);
			}
			nameText.setMarkedVisible(true);
			pin.addObject(nameText);
		}
	}

	protected IGenericPin createCablePin()
	{
		return ((ISymbolDef) m_model.getSymbolDef()).createPin();
	}

	protected IGenericSchemPin createSchemPin(IUID uid, IGenericPin cablePin)
	{
		return FactoryMgr.getSchemFactory().constructPin(uid, (IAbstractPin) cablePin,
				m_currValidPoint.x, m_currValidPoint.y);
	}

	/**
	 * Return our matching ActionUI class
	 */
	public String getActionUIClass()
	{
		return actionHelper.getActionUIClass();
	}

	public boolean isEnabled()
	{
		if (!super.isEnabled()) {
			return false;
		}
		return actionHelper.isEnabled();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (isEnabled() && canDisplayInContextMenu(selections)) {
			String shortDesc = (String) getActionUI().getValue(Action.SHORT_DESCRIPTION);
			if (m_ctxCommand == null || !m_ctxCommand.equalsIgnoreCase(shortDesc)) {
				// Make a private copy for command name
				m_ctxCommand = shortDesc;
			}
			container.add(new ActionEntry(getActionUI(), m_ctxCommand));
		}
	}

	protected boolean canDisplayInContextMenu(SelectSet selections)
	{
		if (selections.getSelectCount() >= 1) {
			for (SelectionIterator iter = selections.getSelected(); iter.hasNext(); ) {
				Selection sel = iter.getNext();
				if (IBlock.class.isAssignableFrom(sel.getSelectionClass())) {
					return false;
				}
				// don't offer to add pins to a pin
				if (IGenericSchemPin.class.isAssignableFrom(sel.getSelectionClass())) {
					return false;
				}
			}
		}
		return true;
	}

	public void mouseEntered(MouseEvent e)
	{
	}

	public void mouseExited(MouseEvent e)
	{
	}

	public void mousePressed(MouseEvent e)
	{
	}

	public void mouseReleased(MouseEvent e)
	{
		//
		// Commit it, and finish up here
		//
		getController().getActionMgr().terminateActiveAction(true);
	}

	public void mouseDragged(MouseEvent e)
	{
	}

	public void mouseClicked(MouseEvent e)
	{
	}

	public void mouseMoved(MouseEvent e)
	{
		int rad = setCurrentValidPoint(e);
		//
		// If this really isn't valid, clear it.
		//
		final String key = SymbolUtils.toLocationKey(m_currValidPoint.x, m_currValidPoint.y);
		if (!isAValidLocationToAddPin(key)) {
			m_currValidPoint = null;
			m_dynamics.removeAllTransientGfx();
		}
		else {
			IExtent ext = m_dummyPin.getExtent();
			ext.setBounds(0, 0, rad, rad);
			ILocation loc = m_dummyPin.getLocation();
			loc.setLocation(m_currValidPoint.x - (rad / 2), m_currValidPoint.y - (rad / 2));
			m_dynamics.addTransientGfx(m_dummyPin);
		}

		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		view.invalidate(IViewInvalidationEnum.eTransient);
	}

	protected boolean isAValidLocationToAddPin(@NotNull String location)
	{
		final Set<IGenericSchemPin> existingPinsAtThisLocation = m_pinLocations.pull(location);
		return SymbolUtils.canAddPinAtSameLocation(existingPinsAtThisLocation,
				((ISymbolDef) m_model.getSymbolDef()).getSymbolType());
	}

	protected int setCurrentValidPoint(MouseEvent e)
	{
		m_currValidPoint = CAFUtils.getInstance().getWorldPoint(e.getPoint(), e.getSource());
		m_currValidPoint = CAFUtils.getInstance().getWorldPoint(e.getPoint(), e.getSource());

		int rad = m_grid.getGridSpacing() / 2;
		m_currValidPoint.setLocation(m_grid.snap(m_currValidPoint.x), m_grid.snap(m_currValidPoint.y));
		return rad;
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	public String getStatusbarText()
	{
		return actionHelper.getStatusbarText();
	}
}
