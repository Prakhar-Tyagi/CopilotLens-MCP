/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2018-2025 Siemens
 */

package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.IGfxModel;
import chs.caf.caplet.action.IActionEnum;

import chs.caf.caplet.selection.SelectSet;
import chs.cof.draw.IRectangle;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utility.GfxUtils;
import chs.utility.gfx.IViewInvalidationEnum;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.awt.Event;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Objects;

public abstract class QuickAddMulticoreAction extends CreateMulticoreWithAccelAction
		implements MouseListener, MouseMotionListener
{

	private SelectSet selections = null;
	@Nullable protected MouseEvent m_endDragEvent;
	@Nullable protected MouseEvent m_startDragEvent;
	@Nullable protected IRectangle m_selectRect;
	private IDynamicGfxService m_dynamics = null;
	private Cursor multicoreCursor = null;
	private String m_tooltipText = null;

	protected QuickAddMulticoreAction(@NotNull ICapletController controller)
	{
		super(controller);
		m_tooltipText = ResourceMgr.getString(QuickAddMulticoreAction.class, "QuickAddMulticoreAction.tooltip.text");
		multicoreCursor = CAFUtils.getInstance()
				.loadCursor(controller.getCaplet(), "chs/images/app/cur_wire.gif", new Point(7, 7));
	}

	@Override public IActionEnum onActivate(ActionEvent e)
	{
		selections = getCurrentSelections();
		m_dynamics = ((IGfxModel) getCapletModel()).getDynamicGfxService();
		m_dynamics.removeAllTransientGfx();
		if (selections.getSelectCount() == 0) {
			m_dynamics.removeAllDynamicGfx();
			invalidateTransientView(IViewInvalidationEnum.eFull);
			return IActionEnum.eActivated;
		}
		return IActionEnum.eCompleted;
	}

	@Override public Cursor getCursor()
	{
		return multicoreCursor;
	}

	@Override public String getStatusbarText()
	{
		return ResourceMgr.getString(QuickAddMulticoreAction.class, "QuickAddMulticoreAction.statusbar.text");
	}

	@Override public boolean onTerminate(boolean allGood)
	{
		boolean success = super.onTerminate(allGood);
		m_dynamics.removeAllDynamicGfx();
		m_dynamics.removeAllTransientGfx();
		clearTooltip();
		invalidateTransientView(IViewInvalidationEnum.eFull);
		return success;
	}

	protected void discardSelectArea()
	{
		if (m_selectRect != null) {
			m_dynamics.removeTransientGfx(m_selectRect);
			m_selectRect = null;
			invalidateTransientView(IViewInvalidationEnum.eTransient);
		}
	}

	protected void invalidateTransientView(IViewInvalidationEnum refreshType)
	{
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(refreshType);
		}
	}

	public void mousePressed(MouseEvent e)
	{
		m_startDragEvent = null;
		m_endDragEvent = null;
		discardSelectArea();
	}

	public void mouseReleased(MouseEvent e)
	{
		if (m_startDragEvent != null) {
			m_endDragEvent = e;
			processEndOfDrag(e);
		}
		discardSelectArea();
	}

	protected Point deviceToWorld(MouseEvent e)
	{
		return CAFUtils.getInstance().getWorldPoint(e.getPoint(), e.getSource());
	}

	@Override public void mouseClicked(MouseEvent e)
	{
		if (e.getClickCount() > 1) {
			getController().getActionMgr().terminateActiveAction(true);
			return;
		}
		ICapletView view = (ICapletView) e.getSource();
		SelectSet selectSet = view.OnSelectPoint(e);
		if ((e.getModifiers() & Event.CTRL_MASK) == 0) {
			selections.add(selectSet);
		}
		else {
			selections.remove(selectSet);
		}

		invalidateTransientView(IViewInvalidationEnum.eTransient);
	}

	@Override public void mouseEntered(MouseEvent e)
	{

	}

	@Override public void mouseExited(MouseEvent e)
	{

	}

	@Override public void mouseMoved(MouseEvent e)
	{
		GfxView view = clearTooltip();
		if (view == null) {
			return;
		}
		clearTooltip();
		Point currPoint = view.worldToDevice(deviceToWorld(e));
		int shift_x = currPoint.x + GfxUtils.TOOLTIP_SHIFT;
		int shift_y = currPoint.y + GfxUtils.TOOLTIP_SHIFT;
		view.showTooltipAtLocation(m_tooltipText, new Point(shift_x, shift_y));
		view.invalidate(IViewInvalidationEnum.eTransient);
	}

	@Nullable protected GfxView clearTooltip()
	{
		GfxView view = (GfxView) CAFUtils.getInstance().getActiveCapletView();
		if (view == null) {
			return null;
		}
		view.clearPopupTooltip();
		view.setToolTipText(null);
		return view;
	}

	public void mouseDragged(MouseEvent e)
	{
		if (m_startDragEvent == null) {
			m_startDragEvent = e;
		}
		discardSelectArea();
		assert m_startDragEvent != null;
		Point stPt = deviceToWorld(m_startDragEvent);
		Point endPt = deviceToWorld(e);
		m_selectRect = FactoryMgr.getDrawFactory().constructRectangle(stPt.x, stPt.y, endPt.x, endPt.y);
		assert m_selectRect != null;
		m_dynamics.addTransientGfx(m_selectRect);
		updateTransientView();
	}

	protected void processEndOfDrag(MouseEvent e)
	{
		if (m_startDragEvent != null && m_endDragEvent != null) {
			ICapletView view = (ICapletView) m_startDragEvent.getSource();
			view.OnStartDrag(m_startDragEvent);
			SelectSet areaSelections = view.OnSelectArea(m_startDragEvent, m_endDragEvent);

			if ((e.getModifiers() & Event.CTRL_MASK) == 0) {
				selections.add(Objects.requireNonNull(areaSelections));
			}
			else {
				selections.remove(Objects.requireNonNull(areaSelections));
			}

			invalidateTransientView(IViewInvalidationEnum.eTransient);
		}
	}

	protected void updateTransientView()
	{
		invalidateTransientView(IViewInvalidationEnum.eTransient);
	}
}
