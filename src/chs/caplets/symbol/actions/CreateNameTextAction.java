/*
 * Copyright 2002-2012 Mentor Graphics Corporation
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
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.action.IActionUI;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectionFilter;
import chs.caf.caplet.selection.SelectionIterator;
import chs.caplets.symbol.Model;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGrid;
import chs.cof.draw.IGriddable;
import chs.cof.drawplus.IAttributeText;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramText;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.schem.IGenericSchemPin;
import chs.cof.logical.schem.IPin;
import chs.cof.symbol.ISymbolDef;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.IProjectPreferenceMgr;
import chs.common.IReadOnlyNamedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.attr.IAttributeProvider;
import chs.common.attr.IAttributeTypes;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utility.SymbolUtils;
import chs.utility.attr.AttributeUtils;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.CoordinateHelper;
import chs.utility.helpers.TextHelper;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Collection;

public class CreateNameTextAction extends ControllerActionRT
		implements ICtxMenuProvider, MouseListener, MouseMotionListener
{

	private Model m_model;
	private IGrid m_grid;
	/**
	 * A handle to our dynamic graphics service for convenience.
	 */
	private IDynamicGfxService m_dynamics;
	private IGfxObject m_dummyText;
	private IDiagramObject m_target;

	private Point m_currValidPoint;
	private String m_ctxCommand = null;

	public CreateNameTextAction(ICapletController controller)
	{
		super(controller);
		m_model = (Model) controller.getCapletModel();
		m_dynamics = m_model.getDynamicGfxService();
		m_dummyText = FactoryMgr.getDrawFactory().constructRectangle(0, 0, 0, 0);
		m_grid = ((IGriddable) m_model.getSheet()).getGrid();
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		m_target = getTarget();
		return IActionEnum.eActivated;
	}

	public boolean onTerminate(boolean successful)
	{
		// Cleanup the transient graphics
		m_dynamics.removeAllDynamicGfx();
		m_dynamics.removeAllTransientGfx();
		//
		if (successful && m_currValidPoint != null) {
			// Adding name text....
			//dts0100841670:We can have block pins also as target and we need to convert the symbol sheet
			//location relative to the target object, taking care of transformation etc.
			ILocation loc = CoordinateHelper.getRelativeLocation(m_target, m_currValidPoint.x, m_currValidPoint.y);
			int x = loc.getX();
			int y = loc.getY();

			IReadOnlyNamedObject nobj = (IReadOnlyNamedObject) ((IRepresentedObject) m_target).getRawConnectivity();

			IDiagramText nameText = null;
			//
			// If we are on a pin, and we already have a name text, then make it visible, and use that.
			//
			if (m_target instanceof IPin) {
				Collection coll = ((IPin) m_target).getObjects(AttributeUtils.NAME_TEXT_FILTER);
				if (coll.size() != 0) {
					nameText = (IAttributeText) coll.iterator().next();
					nameText.setMarkedVisible(true);
					ILocation ntLoc = nameText.getLocation();
					nameText.setLocation(ntLoc);
					ntLoc.setLocation(x, y);
				}
			}
			//
			// Couldn't find one we had made earlier - make one...
			//
			if (nameText == null) {
				nameText = FactoryMgr.getDrawPlusFactory().constructAttributeText(
						FactoryMgr.getCommonFactory().createUID(), (IAttributeProvider) nobj,
						TextHelper.getDefaultHeight(m_grid), 0, x, y, IAttributeTypes.NAME);
				// we should make sure any attributes on symbol def are considered placeholders.
				if (nameText instanceof IAttributeText) {
					//dts0100841670:we will delete permanently the pin nametexts also. so we can't
					//have them as placeholder, otherwise the pin names will be lost from the text.
					((IAttributeText) nameText).setPlaceholder(!(m_target instanceof IPin));
				}
			}
			IProjectPreferenceMgr preferences = CAFUtils.getInstance().getCurrentProjectPreferences();
			final IBaseDiagram diagram = getBaseDiagram();
			if (preferences != null && diagram != null) {
				TextHelper.assignAttributeTextDefaults(nameText, diagram, m_grid, preferences);
			}
			else {
				nameText.setFont(TextHelper.getDefaultFont());
			}
			((ICompoundObject) m_target).addObject(nameText);
		}
		//
		// Refresh
		//
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eFull);
		}
		((IActionUI) getActionUI()).updateUI();
		return true;
	}

	/**
	 * Return our matching ActionUI class
	 */
	public String getActionUIClass()
	{
		return CreateNameTextActionUI.class.getName();
	}

	public boolean isEnabled()
	{
		if (getTarget() == null || !super.isEnabled()) {
			return false;
		}
		if (getTarget() instanceof IGenericSchemPin) {
			//
			// Only allow 1 property on pins.
			//
			IPin pin = (IPin) getTarget();
			Collection coll = pin.getObjects(AttributeUtils.NAME_TEXT_FILTER);
			if (coll.size() == 0) {
				return true;
			}
			//
			// Name text not visible? - allow the addition...
			// - we will make it visible at the end.
			//
			IAttributeText nt = (IAttributeText) coll.iterator().next();
			return (!nt.isMarkedVisible());
		}
		if (m_model.getSymbolDef() instanceof ISymbolDef) {
			ISymbolDef symDef = (ISymbolDef) m_model.getSymbolDef();
			// simons - allow Name Text in comment symbols for now.  We should do something clever in replicator ;)
			//if (SymbolUtils.isCommentSymbol(symDef)) {
			//	return false;
			//}
			if (SymbolUtils.isBackshellSymbol(symDef)) {
				return false;
			}
		}
		return true;
	}

	private IDiagramObject getTarget()
	{
		//
		// It is enabed if:
		// 1. Nothing is selected [then it goes on the symbol]
		// 2. A pin is selected
		//
		IDiagramObject result = null;
		//
		SelectSet sset = getController().getSelectMgr().getPreSelections();
		if (sset.getSelectCount() == 0) {
			//Preventive fix for dts0100603580 - [CH] java.lang.NullPointerException at chs.caplets.symbol.actions.CreateNameTextAction.getTarget
			ISymbolDef symDef = (ISymbolDef) m_model.getSymbolDef();
			assert symDef != null;
			if (symDef != null) {
				result = symDef.getPinList();
			}
		}
		else {
			SelectSet pinset = new SelectSet();
			SelectionFilter filter = new SelectionFilter();
			filter.addOnlyClass(IPin.class);
			pinset.setSelectionFilter(filter);
			pinset.setSelections(sset);
			//
			if (pinset.getSelectCount() == 1) {
				SelectionIterator sitr = pinset.getSelected();
				IUID pinuid = sitr.getNext().getUID();
				//
				// Have to do this as this may be called if the select set changes, but
				// it gets out of sync with undo and there may be undone objects on the
				// select set.
				//
				IUIDObject cand = chs.system.UIDMgr.getObject(pinuid);
				if (cand instanceof IPin) {
					result = (IPin) cand;
				}
			}
		}
		//
		// Fall back onto the symbol itself...
		//
		if (result == null) {
			//Preventive fix for dts0100603580 - [CH] java.lang.NullPointerException at chs.caplets.symbol.actions.CreateNameTextAction.getTarget
			//result = ((ISymbolDef) m_model.getSymbolDef()).getPinLists();
			ISymbolDef symDef = (ISymbolDef) m_model.getSymbolDef();
			assert symDef != null;
			if (symDef != null) {
				result = symDef.getPinList();
			}
		}
		return result;
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		//
		// Only allow if nothing selected...
		//
		if (isEnabled()) {
			container.add(new ActionEntry(getActionUI(), m_ctxCommand));
		}
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
		m_currValidPoint = CAFUtils.getInstance().getWorldPoint(e.getPoint(), e.getSource());
		m_currValidPoint = CAFUtils.getInstance().getWorldPoint(e.getPoint(), e.getSource());

		int rad = m_grid.getGridSpacing() / 2;
		m_currValidPoint.setLocation(m_grid.snap(m_currValidPoint.x), m_grid.snap(m_currValidPoint.y));

		IExtent ext = m_dummyText.getExtent();
		ext.setBounds(0, 0, rad, rad);
		ILocation loc = m_dummyText.getLocation();
		loc.setLocation(m_currValidPoint.x - (rad / 2), m_currValidPoint.y - (rad / 2));
		m_dynamics.addTransientGfx(m_dummyText);

		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		view.invalidate(IViewInvalidationEnum.eTransient);
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(CreateNameTextAction.class, "CreateNameTextAction.StatusBar.Msg");
	}
}
