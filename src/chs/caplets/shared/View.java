/*
 * Copyright 2002-2010 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.shared;

import chs.caf.CAFUtils;
import chs.caf.ICAFSymbolLibraryMgr;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletWindow;
import chs.caf.caplet.IGfxModel;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.helpers.ModifyGridAction;
import chs.caf.caplet.helpers.SetRotationIncrementsAction;
import chs.caf.caplet.helpers.graphics.drafting.SetMouseDragIncrementsAction;
import chs.caf.caplet.selection.SelectEvent;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectSetOperations;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caf.caplet.selection.Selection;
import chs.cof.draw.IColor;
import chs.cof.draw.ICommentSymbol;
import chs.cof.draw.IDrawFactory;
import chs.cof.draw.IGfxAttribute;
import chs.cof.draw.IGfxClosedShape;
import chs.cof.draw.IGfxContext;
import chs.cof.draw.IGfxGroup;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGfxPrimitive;
import chs.cof.draw.IGridConfig;
import chs.cof.draw.IRectangle;
import chs.cof.draw.IVisitor;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IFrame;
import chs.cof.drawplus.ISecondaryRepresentation;
import chs.cof.logical.cable.ISplicePin;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IFunctionLogicDiagram;
import chs.cof.logical.schem.IInternalSchemPin;
import chs.cof.logical.schem.ILogicSegment;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.schem.IShieldBody;
import chs.cof.symbol.IAbstractLibrary;
import chs.cof.symbol.IUserDefinedZone;
import chs.common.IExtent;
import chs.common.IPreferenceMgr;
import chs.common.IProjectPreferenceMgr;
import chs.common.IUIDObject;
import chs.common.PreferenceContext;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.services.gfx.GfxView;
import chs.services.gfx.GridLayer;
import chs.system.FactoryMgr;
import chs.utilities.BuildInfo;
import chs.utilities.CommonUtils;
import chs.utilities.Pair;
import chs.utility.gfx.GfxWalker;
import chs.utility.gfx.IViewInvalidationEnum;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.prefs.Preferences;

/**
 * @author glennr December 7, 2001
 */
public abstract class View extends GfxView implements ISelectedAreaCoordinates
{

	protected GridLayer m_gridLayer = null;
	private Point startPoint;    // Start point in world coordinates of drag
	private Point endPoint;

	/**
	 * Constructor for the CapitalLogic View object
	 *
	 * @param model  The CapitalLogic model this view is representing
	 * @param window The Window to put this view in
	 */
	protected View(ICapletModel model, ICapletWindow window)
	{
		super(model.getController(), window);

		// Add this view to the window
		ICapletWindow win = getWindow();
		if (win != null) {
			win.getContainer().add(this);
		}
	}

	public void destroy()
	{
		super.destroy();
		ICapletWindow win = getWindow();
		if (win != null) {
			win.getContainer().remove(this);
		}
		m_gridLayer = null;
		//    	if (getParent() != null) {
		//    		getParent().remove(this);
		//    	}
	}

	public int getObjPriority(IUIDObject uobj)
	{
		boolean doNotConsiderAsPin = false;
		// Do not select the splice pins, instead select the splice.  This is only for parameterized splices.
		// This is done as part of 2010.1 feature 'FEAT14450 - Re-use Point-to-Point Router in Design Tools'
		// This is done to improve usability for splice selection.
		// When the diagram is zoomed to its extent, the selection of splice is not possible until zoom-in to the splice.
		if ((uobj instanceof IPin || uobj instanceof IInternalSchemPin) &&
				((IConnectivityRef) uobj).getConnectivity() instanceof ISplicePin) {
			IDiagramObject parent = ((IDiagramObject) uobj).getParent();
			if (parent instanceof IPinList && ((IPinList) parent).getParameterized() != null) {
				doNotConsiderAsPin = true;
			}
		}

		if ((uobj instanceof IPin || uobj instanceof IInternalSchemPin || uobj instanceof ISchemStackPin) &&
				!doNotConsiderAsPin) {
			return 1;
		}
		if (uobj instanceof ISecondaryRepresentation) {
			return 2;
		}
		if (uobj instanceof IFrame) {
			return 3;
		}
		if (uobj instanceof ILogicSegment) {
			return 8;
		}
		if (uobj instanceof IUserDefinedZone) {
			return (10 + 2); //avoid inspection of magic number.
		}
		if (uobj instanceof IGfxPrimitive || uobj instanceof IGfxClosedShape || uobj instanceof IGfxGroup) {
			return 4;
		}
		if (uobj instanceof ICommentSymbol) {
			return 7;
		}
		if (uobj instanceof IShieldBody) {
			return 9;
		}
		//		else if (uobj instanceof ISegment) {
		//			return 6;
		//		}
		if (uobj instanceof IPinList) {
			return 10;
		}

		return (10 + 1); //avoid inspection of magic number.
	}

	public void setDiagram(IBaseDiagram diagram)
	{
		super.setDiagram(diagram);
		m_gridLayer = makeGridLayer(diagram);
	}

	private GridLayer makeGridLayer(IBaseDiagram diagram)
	{
		IDrawFactory drawFac = FactoryMgr.getDrawFactory();
		IGfxAttribute gridcol = drawFac.constructAttribute(drawFac.lookupColor("grid"));
		GridLayer gridLayer = new GridLayer(diagram, gridcol, 5);
		if (CAFUtils.getInstance().getFIB().getRealm() == IAbstractLibrary.class) {
			// DR 442258 - symbol grid settings are user prefs and common to all libs.
			ICAFSymbolLibraryMgr mgr = CAFUtils.getInstance().getSymbolLibraryMgr();
			mgr.updateGridConfig(gridLayer);
		}
		else if (diagram instanceof ISchemDiagram) {
			// DR 442264 - logic grid settings are project prefs.
			IPreferenceMgr basePrefs = diagram.getDesignContainer().getProject().getPreferences();
			IProjectPreferenceMgr prefs = (IProjectPreferenceMgr) basePrefs;
			if (diagram instanceof IFunctionLogicDiagram) {
				gridLayer.setMajorMultiple(prefs.getFunctionMajorGridInterval());
				gridLayer.setGridCutoff(prefs.getFunctionMaxDrawnPoints());
			}
			else {
				final PreferenceContext context = PreferenceContext.determineContext(diagram);
				gridLayer.setMajorMultiple(prefs.getMajorGridInterval(context));
				gridLayer.setGridCutoff(prefs.getMaxDrawnPoints(context));
			}
		}
		return gridLayer;
	}

	public IGridConfig getGridConfig()
	{
		return m_gridLayer;
	}

	public void selectionChanged(SelectEvent e)
	{
		// Just force the view to redraw so the new selection
		// state is reflected.
		invalidate(IViewInvalidationEnum.eSelection);
	}

	public void modelChanged(ModelChangeEvent e)
	{
		// Just force the view to redraw so the new model
		// state is reflected.
		invalidate(IViewInvalidationEnum.eFull);
	}

	@Nullable protected SelectSet getFilteredSelection(MouseEvent e, IExtent hitArea, SelectSet selSet)
	{
		return bestSelection(selSet, hitArea);
	}

	/*
	 * This provides the "bestSelection" based on some criteria.  Right now it just pickes the object with the smallest
	 * extent.
	 */
	private SelectSet bestSelection(SelectSet rawSset, IExtent hitArea)
	{
		SelectSet sset;
		if (rawSset.getSelectCount() > 1) {
			sset = SelectSetOperations.filterSelections(rawSset, this, getDynamicGfxService().getFactory(), hitArea);
		}
		else {
			sset = rawSset;
		}
		SelectedUIDObjectIterator siter = sset.getSelectedUIDObjects();
		IUIDObject bestSelection = null;
		while (siter.hasNext()) {
			IUIDObject uobj = siter.getNext();
			if (bestSelection != null) {
				if (uobj instanceof IGfxObject) {
					IGfxObject gobj = (IGfxObject) uobj;
					IGfxObject oldObj = (IGfxObject) bestSelection;
					if (gobj.getExtent() != null && oldObj.getExtent() != null) {
						int enew = Math.abs(gobj.getExtent().getWidth()) + Math.abs(gobj.getExtent().getHeight());
						int eold = Math.abs(oldObj.getExtent().getWidth()) + Math.abs(oldObj.getExtent().getHeight());

						if (enew < eold) {
							bestSelection = (IUIDObject) gobj;
						}
					}
				}
			}
			else {
				bestSelection = uobj;
			}
		}
		SelectSet filteredSet;

		if (bestSelection != null) {
			filteredSet = new SelectSet();
			filteredSet.add(new Selection(bestSelection));
		}
		else {
			filteredSet = sset;
		}

		return filteredSet;
	}

	public SelectSet OnSelectArea(MouseEvent downEvent, MouseEvent upEvent)
	{
		// First cleanup the existing transient graphics
		IDynamicGfxService dgs = ((IGfxModel) getCapletModel()).getDynamicGfxService();
		dgs.removeAllTransientGfx();
		invalidate(IViewInvalidationEnum.eTransient);

		// Look in the model for objects that are contained in the
		// drag area
		endPoint = deviceToWorld(upEvent.getPoint());

		SelectSet selSet = new SelectSet();

		if (startPoint == null) {
			OnStartDrag(downEvent);
		}
		int bl_x = Math.min(startPoint.x, endPoint.x);
		int bl_y = Math.min(startPoint.y, endPoint.y);
		int tr_x = Math.max(startPoint.x, endPoint.x);
		int tr_y = Math.max(startPoint.y, endPoint.y);
		int width = tr_x - bl_x;
		int height = tr_y - bl_y;
		if (width > 0 && height > 0) {
			IExtent selectRect = CAFUtils.getInstance().getCommonFactory().constructExtent(bl_x, bl_y, width, height);
			IGfxContext gc = getGfxContext();
			createFinder(gc, selSet, selectRect).visitRoot(getSheet(), 0, 0);
			filterSelectionBasedUponAreaCoverage(selectRect, selSet);
		}

		return selSet;
	}

	protected void filterSelectionBasedUponAreaCoverage(IExtent selectRect, SelectSet selSet)
	{

	}

	public void OnDrag(MouseEvent downEvent, MouseEvent currEvent)
	{
		// Translate the points from device to world and draw
		// a rectangle
		Point currPoint = deviceToWorld(currEvent.getPoint());

		// First cleanup any existing transient graphics
		IDynamicGfxService dgs = ((IGfxModel) getCapletModel()).getDynamicGfxService();
		dgs.removeAllTransientGfx();

		if (startPoint == null) {
			OnStartDrag(downEvent);
		}
		IRectangle selectRect = FactoryMgr.getDrawFactory().
				constructRectangle(startPoint.x, startPoint.y, currPoint.x, currPoint.y);
		dgs.addTransientGfx(selectRect);

		invalidate(IViewInvalidationEnum.eTransient);
	}

	public void OnStartDrag(MouseEvent e)
	{
		startPoint = deviceToWorld(e.getPoint());
		// Do all of the transient graphics in the OnDrag
	}

	public void preCustomRender(IVisitor renderer)
	{
		super.preCustomRender(renderer);
		if (m_gridLayer == null) {
			return; // nothing to do.
		}
		if (m_gridLayer.getOwner() != getDiagram()) {
			// need to reconstruct..   Diagram must have changed from under us.
			m_gridLayer = makeGridLayer(getDiagram());
		}
		m_gridLayer.visitObject(renderer, null, 0, 0);//getGfxContext().getOffsetX(), getGfxContext().getOffsetY());
	}

	protected void createViewActions()
	{
		super.createViewActions();
		addAction(new ModifyGridAction(this, CAFUtils.getInstance().getFIB().getApplication().getGridChangeListener()));
		addAction(new SetRotationIncrementsAction(this));
		addAction(new SetMouseDragIncrementsAction(this));
	}

	@Nullable @Override public SelectSet OnSelectPoint(MouseEvent e, @Nullable SelectSet priorityObjects)
	{
		if (e.getID() == MouseEvent.MOUSE_RELEASED) {
			if (!e.isControlDown() && ((e.getModifiers() & (InputEvent.BUTTON1_MASK)) != 0)) {
				startPoint = null;
				endPoint = null;
			}
		}
		return super.OnSelectPoint(e, priorityObjects);
	}

	//
	// Use this to get a finder - other views may override this.
	//
	protected GfxWalker createFinder(IGfxContext dc, SelectSet selSet, IExtent loc)
	{
		return new Finder(dc, selSet, loc, allowSelectionOfInvisibleObjects());
	}

	public boolean allowsPrinting()
	{
		return true;
	}

	public Pair<Point, Point> getStartAndEndOfAreaSelection()
	{
		if (startPoint != null && endPoint != null) {
			return new Pair<Point, Point>(startPoint, endPoint);
		}
		return null;
	}

	@Override public void mouseMoved(MouseEvent e)
	{
		super.mouseMoved(e);
		DataTransfer transfer = getDataTransfer();
		if (transfer != null) {
			transfer.mouseMoved(e);
		}
	}

	@Override public void keyPressed(KeyEvent ke)
	{
		super.keyPressed(ke);
		DataTransfer transfer = getDataTransfer();
		if (transfer != null) {
			transfer.keyPressed(ke);
		}
	}

	@Nullable protected DataTransfer getDataTransfer()
	{
		ICapletController activeCapletController = CAFUtils.getInstance().getActiveCapletController();
		if (activeCapletController == null) {
			return null;
		}

		ICaplet caplet = activeCapletController.getCaplet();
		if (caplet == null) {
			return null;
		}
		return CommonUtils
				.cast(caplet.getDataTransfer(),
						DataTransfer.class);
	}

	@Override public void mouseClicked(MouseEvent e)
	{
		super.mouseClicked(e);
		DataTransfer transfer = getDataTransfer();
		if (transfer != null) {
			transfer.mouseClicked(e);
		}
	}

	protected boolean shouldShowMouseSnap()
	{
		return true;
	}

	protected boolean considerEnhancedSelectionAperture()
	{
		//toyota (in fact general) complaint related to pin selection difficulty.
		//reverted back. many tests failed. need to do more experiments and analysis.
		//logic/Design/Sector/Preference_Off/Object_Move/Object_Move_RegTest.csv
		//logic/Design/Sector/Propogation/Object_Move/Object_Move_RegTest.csv
		//logic/Design/Sector/Propogation/Object_Move_Shared/Object_Move_RegTest.csv
		//logic/Device/MoveConnBasic/RegTest.csv
		//logic/Device/MoveConnBasicPublisher/RegTest.csv
		//logic/GHC/GHCDevWithStudPinAndRTG/GHCDevWithStudPinAndRTG_RegTest.csv
		//logic/GHC/GHCOnDiffActnOnStudWithHous/GHCOnDiffActnOnStudWithHous_RegTest.csv
		//logic/SharedObjects/SelectParadigmForShareInconsistent/SelectParadigmForShareInconsistent_RegTest.csv
		//logic/InlineConnectors/ShareInline/ShareInline_RegTest.csv
		//logic/Table_Editor/Delete/DeleteOTIValue/DeleteOTIValue_RegTest.csv
		//logic/Conductors/FEAT14586_wires_merge/FEAT14586_wires_merge_RegTestLC00.csv
		//logic/Conductors/SP1004_dts0100668610_RegTestLC00/SP1004_dts0100668610_RegTestLC00.csv
		//logic/Multicore/ToggleIndicatorSpecial/ToggleIndicatorSpecial_RegTest.csv
		//logic/Symbols/FEAT13545_ZOrder_RegTestAS02/FEAT13545_ZOrder_RegTestAS02.csv
		//logic/DeviceConnector/DeviceConnLibPartAddSharedConn/DeviceConnLibPartAddSharedConn_RegTest.csv
		//logic/Multicore/ZeroSegmentMulticore_RegTestAS00/ZeroSegmentMulticore_RegTestAS00.csv
		//logic/Symbols/LogicUsages/replace_symbol_RegTestLC00/replace_symbol_RegTestLC00.csv
		//logic/Symbols/LogicUsages/ExportAsSymSymWirNetSpMcObDwo/ExportAsSymSymWirNetSpMcObDwoRegTest.csv
		return BuildInfo.getBuildInfo().areDeveloperExtensionsEnabled();
	}

	protected int determineApertureExpansion()
	{
		final int def = super.determineApertureExpansion();
		return Preferences.userNodeForPackage(IColor.class).getInt(IDrawFactory.HANDLE_WIDTH_KEY, def);
	}
}
