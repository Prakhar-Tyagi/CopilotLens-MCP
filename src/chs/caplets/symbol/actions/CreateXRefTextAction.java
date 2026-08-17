/*
 * Copyright 2005-2012 Mentor Graphics Corporation
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
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectionFilter;
import chs.caf.caplet.selection.SelectionIterator;
import chs.caplets.symbol.Model;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGrid;
import chs.cof.draw.IGriddable;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IXRefPlaceholder;
import chs.cof.logical.schem.IPin;
import chs.cof.symbol.ISymbolDef;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.IProjectPreferenceMgr;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utility.SymbolUtils;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.TextHelper;

import javax.swing.Action;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class CreateXRefTextAction extends ControllerActionRT
		implements ICtxMenuProvider, MouseListener, MouseMotionListener
{

	private Model m_model;
	private IGrid m_grid;
	/**
	 * A handle to our dynamic graphics service for convenience.
	 */
	private IDynamicGfxService m_dynamics;
	//    private Vector m_transients;
	private IGfxObject m_dummyText;
	private IDiagramObject m_target;

	private Point m_currValidPoint;
	private String m_ctxCommand = null;

	public CreateXRefTextAction(ICapletController controller)
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
			ILocation loc = m_target.getLocation();
			int x = m_currValidPoint.x - loc.getX();
			int y = m_currValidPoint.y - loc.getY();

			IXRefPlaceholder xrefPh =
					FactoryMgr.getDrawPlusFactory().constructXRefPlaceholder(FactoryMgr.getCommonFactory().createUID(),
							TextHelper.getDefaultHeight(m_grid), 0, x, y);
			IProjectPreferenceMgr preferences = CAFUtils.getInstance().getCurrentProjectPreferences();
			if (preferences != null) {
				if (m_target instanceof IPin) {
					preferences.assignPinXRefTextDefaults(xrefPh, m_grid);
				}
				else {
					preferences.assignPinListXRefTextDefaults(xrefPh, m_grid);
				}
			}
			else {
				xrefPh.setFont(TextHelper.getDefaultFont());
			}
			((ICompoundObject) m_target).addObject(xrefPh);
		}
		//
		// Refresh
		//
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eFull);
		}
		return true;
	}

	/**
	 * Return our matching ActionUI class
	 */
	public String getActionUIClass()
	{
		return CreateXRefTextActionUI.class.getName();
	}

	public boolean isEnabled()
	{
		return getTarget() != null && super.isEnabled();
	}

	private IDiagramObject getTarget()
	{
		//
		// It is enabed if:
		// 1. Nothing is selected [then it goes on the symbol]
		// 2. A pin is selected
		// 3. The symbol is not a comment
		//
		IDiagramObject result = null;
		//
		SelectSet sset = getController().getSelectMgr().getPreSelections();
		ISymbolDef symbolDef = (ISymbolDef) m_model.getSymbolDef();
		if (sset.getSelectCount() == 0) {
			result = symbolDef.getPinList();
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
			result = symbolDef.getPinList();
		}
		// Not comment symbols
		if (SymbolUtils.isCommentSymbol(symbolDef)) {
			result = null;
		}
		return result;
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		//
		// Only allow if nothing selected...
		//
		if (selections.getSelectCount() == 0) {
			String shortDesc = (String) getActionUI().getValue(Action.SHORT_DESCRIPTION);
			if (m_ctxCommand == null || !m_ctxCommand.equalsIgnoreCase(shortDesc)) {
				// Make a private copy for command name
				m_ctxCommand = shortDesc;
			}
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
		return ResourceMgr.getString(CreateXRefTextAction.class, "CreateXRefTextAction.StatusBar.Msg");
	}
}
