/*
 * Copyright 2002-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol;

import chs.caf.IStatusBar;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletWindow;
import chs.caf.caplet.helpers.snapping.ISnapDrawingGridModel;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.shared.Finder;
import chs.caplets.symbol.actions.ViewRelatedSymbolAction;
import chs.cof.draw.IConfigurableVisitor;
import chs.cof.draw.IGfxContext;
import chs.cof.draw.IVisitor;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.symbol.IBorder;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.SymbolTypeEnum;
import chs.common.IExtent;
import chs.common.IUnit;
import chs.services.ui.DrawingModePanel;
import chs.services.ui.IStatusPanel;
import chs.services.ui.ToggleLinksVisibilityPanel;
import chs.utilities.ResourceMgr;
import chs.utility.gfx.GfxWalker;
import chs.utility.logic.ISymbolModel;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public class View extends chs.caplets.shared.View
{

	private ToggleLinksVisibilityPanel m_LinksVisibilityPanel = null;
	private DrawingModePanel m_snapToGridPanel = null;
	private DrawingModePanel m_snapToObjectPanel = null;
	private ViewStatusBarController m_localStatusbarController = null;

	private ISymbolModel m_model = null;
	private boolean mShowLinksVisibility = false;

	public View(ICapletModel model, ICapletWindow window)
	{
		super(model, window);
		m_model = (ISymbolModel) model;
		m_localStatusbarController = new ViewStatusBarController();
		ISnapDrawingGridModel gridModel = (ISnapDrawingGridModel) m_model;
		m_snapToGridPanel = new DrawingModePanel(
				gridModel.isDrawingGridSnap(), "SNAP", m_localStatusbarController,
				ResourceMgr.getString(chs.caplets.symbol.View.class, "View.GridSnap.tooltip.on"),
				ResourceMgr.getString(chs.caplets.symbol.View.class, "View.GridSnap.tooltip.off"),
				"chs/images/general/ico_snap_to_grid_on.png",
				"chs/images/general/ico_snap_to_grid_off.png");

		m_snapToObjectPanel = new DrawingModePanel(
				gridModel.isDrawingObjectSnap(), "OSNAP", m_localStatusbarController,
				ResourceMgr.getString(chs.caplets.symbol.View.class, "View.ObjectSnap.tooltip.on"),
				ResourceMgr.getString(chs.caplets.symbol.View.class, "View.ObjectSnap.tooltip.off"),
				"chs/images/general/ico_snap_to_object_on.png",
				"chs/images/general/ico_snap_to_object_off.png");
		checkLinksVisibility();

		IVisitor renderer = m_drawingCanvas.getRenderer();
		if (renderer instanceof IConfigurableVisitor) {
			configureRenderer((IConfigurableVisitor) renderer);
		}
	}

	@NotNull protected Collection<AbstractLocationDisplayControl> getGraphicalDimensionDisplayControl(
			double dPosX, double dPosY, int gridSpacing, IUnit phyUnit)
	{

		return Collections.singleton(new TooltipLocationDisplayControl(phyUnit, gridSpacing, dPosX, dPosY));
	}

	/**
	 * Overridden here to enable the "world area intersection" check to be done in the renderer for borders
	 * (dts0100927806)
	 *
	 * @param renderer The renderer to configure if this is a border view
	 */
	@Override public void configureRenderer(IConfigurableVisitor renderer)
	{
		if (m_model != null && m_model.getSymbolDef() instanceof IBorder) {
			renderer.setCheckWorldAreaIntersection(true);
		}
	}

	private void checkLinksVisibility()
	{
		IStamp modelStamp = m_model.getSymbolDef();
		if (modelStamp instanceof ISymbolDef) {
			SymbolTypeEnum symbolType = ((ISymbolDef) modelStamp).getSymbolType();
			if (SymbolTypeEnum.DEVICE.equals(symbolType)) {
				mShowLinksVisibility = true;
				m_LinksVisibilityPanel = new ToggleLinksVisibilityPanel(m_model);
			}
		}
	}

	public void setDiagram(IBaseDiagram diagram)
	{
		super.setDiagram(diagram);
		m_gridLayer.setDrawingOrigin(true);
	}

	public IStatusPanel getVisibilityPanel()
	{
		return m_LinksVisibilityPanel;
	}

	public void setStatusPanels(IStatusBar statusBar)
	{
		super.setStatusPanels(statusBar);

		m_statusBar.addPanel(m_snapToGridPanel);
		m_statusBar.addPanel(m_snapToObjectPanel);
	}

	//
	// Use this to get a finder - other views may override this.
	//
	protected GfxWalker createFinder(IGfxContext dc, SelectSet selSet, IExtent loc, int modifiers)
	{
		// Set to true for pin name text selection
		return new Finder(dc, selSet, loc, allowSelectionOfInvisibleObjects());
	}

	public void setLinksVisibilityMode(boolean mode)
	{
		if (mShowLinksVisibility) {
			m_LinksVisibilityPanel.updateMode(mode);
		}
	}

	private class ViewStatusBarController implements DrawingModePanel.IStatusBarObjectsController
	{

		public void actionPerformed(DrawingModePanel ui)
		{
			boolean isOn = ui.getCurrentState();
			if (ui == m_snapToGridPanel) {
				((ISnapDrawingGridModel) m_model).setDrawingGridSnap(isOn);
			}
			else if (ui == m_snapToObjectPanel) {
				((ISnapDrawingGridModel) m_model).setDrawingObjectSnap(isOn);
			}
		}
	}

	public void setSnapToObjectMode(boolean mode)
	{
		m_snapToObjectPanel.updateDrawingMode(mode);
	}

	public void setSnapToGridMode(boolean mode)
	{
		m_snapToGridPanel.updateDrawingMode(mode);
	}

	protected void createViewActions()
	{
		ViewRelatedSymbolAction crossLinkAction = new ViewRelatedSymbolAction(this);
		addAction(crossLinkAction);
		super.createViewActions();
	}
}
