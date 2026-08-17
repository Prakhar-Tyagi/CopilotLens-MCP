/*
 * Copyright 2013-2015 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.actions;

import chs.caf.ActionContainer;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.IUpdateableAction;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.action.IActionUI;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.helpers.graphics.GraphicalActionHelper;
import chs.caf.caplet.helpers.graphics.IGraphicalAction;
import chs.caf.caplet.helpers.snapping.ISnapDrawingGridModel;
import chs.caf.caplet.helpers.snapping.ModelUtils;
import chs.caf.caplet.helpers.snapping.SnapHelper;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.symbol.Model;
import chs.cof.draw.HorizJustificationEnum;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGfxObjectIterator;
import chs.cof.draw.IGrid;
import chs.cof.draw.IGriddable;
import chs.cof.draw.VertJustificationEnum;
import chs.cof.drawplus.IDatumRepresentation;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramText;
import chs.cof.symbol.IBorder;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.common.IBaseDatum;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.attr.IAttributeTypes;
import chs.services.dynamicgfx.IDynamicGfxMediator;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.system.FactoryMgr;
import chs.utility.SymbolUtils;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.ExtentHelper;
import chs.utility.helpers.ISelectiveExtentFilter;
import chs.utility.helpers.TextHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractAddDatumAction extends ControllerActionRT
		implements ICtxMenuProvider, MouseListener, MouseMotionListener, IGraphicalAction
{

	protected Model m_model;
	private IGrid m_grid;
	/**
	 * A handle to our dynamic graphics service for convenience.
	 */
	protected IDynamicGfxService m_dynamics;
	private IGfxObject m_dummyGfx;
	protected Set<String> m_prevLocations;
	protected Point m_currPoint;
	private GraphicalActionHelper m_graphicalActionHelper = null;

	protected AbstractAddDatumAction(ICapletController controller, @Nullable String instanceName)
	{
		super(controller, instanceName);
		m_model = (Model) controller.getCapletModel();
		m_dynamics = m_model.getDynamicGfxService();
		m_dummyGfx = FactoryMgr.getDrawFactory().constructRectangle(0, 0, 0, 0);
		m_grid = ((IGriddable) m_model.getSheet()).getGrid();
		m_graphicalActionHelper = new GraphicalActionHelper(controller, this, false, false) {
			@Override public SnapType getRequiredSnapType()
			{
				return SnapType.SIMPLE;
			}
		};
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		// Check symbol
		if (!checkSymbol()) {
			return IActionEnum.eCanceled;
		}
		m_graphicalActionHelper.activate();
		//
		// Note where the datums already are...
		//
		recordPreviousLocations();

		return IActionEnum.eActivated;
	}

	protected boolean checkSymbol()
	{
		IStamp stamp = m_model.getSymbolDef();
		return stamp instanceof ISymbolDef;
	}

	protected void recordPreviousLocations()
	{
		m_prevLocations = new HashSet<String>();
		ICompoundObject gfx = m_model.getSheet();
		for (IGfxObjectIterator gitr = gfx.getObjects(); gitr.hasNext();) {
			IGfxObject go = gitr.getNext();
			if (go instanceof IDatumRepresentation) {
				String key = go.getLocation().getX() + "." + go.getLocation().getY();
				m_prevLocations.add(key);
			}
		}
	}

	protected void cleanUpTransientGraphics()
	{
		m_dynamics.removeAllDynamicGfx();
		m_dynamics.removeAllTransientGfx();
	}

	@NotNull protected abstract IBaseDatum newDatum();
	protected abstract void addDatumToStamp(@NotNull IStamp stamp, @NotNull IBaseDatum datum);

	protected void createPositionnedDatum(@NotNull Point position, @Nullable String nameText)
	{
		// Instantiate the datum
		IBaseDatum datum = newDatum();

		// Copy name manager from parent symbol
		IStamp stamp = m_model.getSymbolDef();
		datum.setNameMgr(stamp.getNameMgr());

		// Set the position
		datum.setLocation(position.x, position.y);

		// Add the datum to the parent symbol
		addDatumToStamp(stamp, datum);

		// Create the representation
		createDatumRepresentation(datum, position, nameText);
	}

	protected void createDatumRepresentation(@NotNull IBaseDatum datum, @NotNull Point position,
			@Nullable String nameText)
	{
		IDatumRepresentation datumRep = FactoryMgr.getSymbolFactory().createDatumRepresentation(
				FactoryMgr.createUID(), datum);
		datumRep.setLocation(FactoryMgr.getCommonFactory().constructLocation(position.x, position.y));
		m_model.getSheet().addObject(datumRep);

		// Create the name text
		createNameText(datum, datumRep, position, nameText);
	}

	protected void createNameText(@NotNull IBaseDatum datum, @NotNull IDatumRepresentation datumRep,
			@NotNull Point position, @Nullable String nameText)
	{
		//
		// Now we have the datum added, add the name text.
		//
		IDiagramText nameTextObj = FactoryMgr.getDrawPlusFactory().constructAttributeText(
				FactoryMgr.createUID(), datum, TextHelper.getDefaultHeight(m_grid), 0, 0, 0,
				IAttributeTypes.NAME);
		nameTextObj.setFont(TextHelper.getDefaultFont());
		if (nameText != null) {
			nameTextObj.setString(nameText);
		}

		//
		// Get the extent of the object (datums only).
		//
		IExtent ext = FactoryMgr.getCommonFactory().createExtent();
		ISelectiveExtentFilter sef = new ISelectiveExtentFilter()
		{
			public boolean includeObject(Object o)
			{
				return (o instanceof IDatumRepresentation);
			}

			public int getInclusionType(Object o)
			{
				return AS_LOCATION;
			}

			public boolean includeChildren(Object o)
			{
				return true;
			}
		};
		ExtentHelper.getFilteredExtent(m_model.getSheet(), ext, sef);
		//
		// Set up the justification point.
		//
		if (position.x == ext.getLeft()) {
			nameTextObj.setHorizontalJustification(HorizJustificationEnum.JustLeft);
			nameTextObj.setVerticalJustification(VertJustificationEnum.JustCenter);
		}
		else if (position.x == ext.getRight()) {
			nameTextObj.setHorizontalJustification(HorizJustificationEnum.JustRight);
			nameTextObj.setVerticalJustification(VertJustificationEnum.JustCenter);
		}
		else if (position.y == ext.getBottom()) {
			nameTextObj.setHorizontalJustification(HorizJustificationEnum.JustMiddle);
			nameTextObj.setVerticalJustification(VertJustificationEnum.JustBottom);
		}
		else if (position.y == ext.getTop()) {
			nameTextObj.setHorizontalJustification(HorizJustificationEnum.JustMiddle);
			nameTextObj.setVerticalJustification(VertJustificationEnum.JustTop);
		}
		datumRep.addObject(nameTextObj);
	}

	protected void refreshUIOnTerminate()
	{
		m_graphicalActionHelper.setFeedbackText(null);
		m_graphicalActionHelper.terminate();

		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eFull);
		}
		Action act = getActionUI();
		if (act instanceof IActionUI) {
			((IUpdateableAction) act).updateUI();
		}
	}

	@Override public boolean isEnabled()
	{
		if (super.isEnabled()) {
			// Enabled only for comment symbols or border
			IStamp stamp = m_model.getSymbolDef();
			if (stamp instanceof ISymbolDef) {
				return SymbolUtils.isCommentSymbol((ISymbolDef) stamp);
			}
			else if (stamp instanceof IBorder) {
				return isAvailableInBorder();
			}
		}
		return false;
	}

	protected boolean isAvailableInBorder()
	{
		return false;
	}

	public void mouseEntered(MouseEvent e)
	{
	}

	public void mouseExited(MouseEvent e)
	{
	}

	public void mousePressed(MouseEvent e)
	{
		m_graphicalActionHelper.mousePressed(e);
	}

	public void mouseReleased(MouseEvent e)
	{
		m_graphicalActionHelper.mouseReleased(e);
	}

	public void mouseDragged(MouseEvent e)
	{
	}

	public void mouseClicked(MouseEvent e)
	{
	}

	@NotNull protected Point createValidPointUnderMouse(@NotNull MouseEvent e)
	{
		SnapHelper snapper = m_graphicalActionHelper.getSnapHelper();

		Point snapPoint = snapper.snappedPoint(CAFUtils.getInstance().getWorldPoint(e.getPoint(), e.getSource()),
				ModelUtils.getSnapRadius(e.getSource()), getSnappingSource(), e);

		if (getSnapToSubGridSetting()) {
			snapPoint.setLocation(m_grid.snap(snapPoint.x), m_grid.snap(snapPoint.y));
		}

		return snapPoint;
	}

	protected void updateDynamicGraphics(@NotNull Point position)
	{
		IExtent ext = m_dummyGfx.getExtent();
		int rad = m_grid.getGridSpacing() / 2;
		ext.setBounds(0, 0, rad, rad);
		ILocation loc = m_dummyGfx.getLocation();
		loc.setLocation(position.x - (rad / 2), position.y - (rad / 2));
		m_dynamics.addTransientGfx(m_dummyGfx);
	}

	protected void redrawView()
	{
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		view.invalidate(IViewInvalidationEnum.eTransient);
	}

	public void mouseMoved(MouseEvent e)
	{
		m_graphicalActionHelper.mouseSnapped(e);
		m_graphicalActionHelper.mouseMoved(e);

		m_currPoint = createValidPointUnderMouse(e);

		//
		// If this really isn't valid, clear it.
		//
		if (m_prevLocations.contains(computeCurrentPointKey(m_currPoint))) {
			m_currPoint = null;
			m_dynamics.removeAllTransientGfx();
		}
		else {
			updateDynamicGraphics(m_currPoint);
		}

		redrawView();
	}

	@NotNull protected String computeCurrentPointKey(@NotNull Point position)
	{
		return position.x + "." + position.y;
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	/**
	 * Return the cursor for this action
	 */
	@Nullable public Cursor getCursor()
	{
		return CAFUtils.getInstance().loadCursor(Cursor.DEFAULT_CURSOR);
	}

	protected boolean getSnapToSubGridSetting()
	{
		ICapletView capView = CAFUtils.getInstance().getActiveCapletView();
		if (capView != null) {
			ICapletModel capModel = CAFUtils.getInstance().getActiveCapletView().getCapletModel();
			if (capModel instanceof ISnapDrawingGridModel) {
				return ((ISnapDrawingGridModel) capModel).isDrawingGridSnap();
			}
		}
		return true;
	}

	@Override public void changeCursor(Cursor cursor)
	{
	}

	@Override public void cursorChangeNotification(IDynamicGfxMediator snappedTo)
	{
	}

	@Nullable
	@Override public ICompoundObject getCurrentDragGraphics(IDynamicGfxMediator mediator)
	{
		return null;
	}

	@Override public int getGridSpacing()
	{
		return m_grid == null ? 0 : m_grid.getGridSpacing();
	}

	@Nullable
	@Override public double[] getOriginalTransform(IDynamicGfxMediator mediator)
	{
		return null;
	}

	@Override public Set<IDiagramObject> getPreselectedRestrictions()
	{
		return m_graphicalActionHelper.determinePreSelectedObjects(getPreSelections());
	}

	@Override public Class<?> getSnappingSource()
	{
		return IDatumRepresentation.class;
	}

	@Override public boolean isValidSnapObj(IDynamicGfxMediator med, ILocation snapLoc)
	{
		return true;
	}
}
