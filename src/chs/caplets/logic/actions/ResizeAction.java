/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2003-2025 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionFilter;
import chs.caf.caplet.selection.SelectionIterator;
import chs.caplets.logic.Model;
import chs.cof.draw.IDrawFactory;
import chs.cof.draw.IGrid;
import chs.cof.draw.IRectangle;
import chs.cof.draw.ITransform;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IDeviceLikePinlist;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.GeneratorStyle;
import chs.cofUtils.parameterized.IndicatorOrientation;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.IParameterContainer;
import chs.common.IParameterized;
import chs.common.IUIDObject;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.ResourceMgr;
import chs.utility.DiagramHelper;
import chs.utility.GfxObjectUtils;
import chs.utility.ResizeHelper;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.ConnectorHelper;
import chs.utility.helpers.ExtentHelper;
import chs.utility.logic.LogicUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

public class ResizeAction extends ControllerActionRT implements ICtxMenuProvider, MouseListener, MouseMotionListener
{

	private Model m_model;
	private IPinList m_paramObj;
	private GeneratorParameters m_genParams;
	private Generator m_generator;
	private IExtent m_minBox = null;
	private IExtent m_oldExtent = null;
	private GfxView m_view = null;
	private IGrid m_grid = null;
	private ILocation m_tempLoc = null;
	private IRectangle m_areaRect = null;
	private boolean m_outsideH;
	private boolean m_outsideV;
	private int horizAnchor = IndicatorOrientation.SOUTH;
	private int vertAnchor = IndicatorOrientation.WEST;
	private boolean m_isConnector = false;
	/**
	 * A handle to our dynamic graphics service for convenience.
	 */
	private IDynamicGfxService m_dynamics;
	private ILocation m_currValidPoint;
	private double lowerBorder;
	private double upperBorder;
	public static boolean m_bUnitTest = false;

	public ResizeAction(ICapletController controller)
	{
		super(controller);
		m_model = (Model) controller.getCapletModel();
		m_dynamics = m_model.getDynamicGfxService();
		m_generator = Generator.getGenerator();
		m_outsideH = false;
		m_outsideV = false;
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		//
		// For this object, lets calculate the constraints..
		//
		m_paramObj = getOperand(getController().getSelectMgr().getCurrentSelections());
		if (m_paramObj == null) {
			return IActionEnum.eCanceled;
		}

		ISchemDiagram diag = m_model.getDiagram();
		m_genParams = DiagramHelper.createGeneratorParameters(diag);
		m_generator = Generator.getGenerator();
		m_areaRect = null;
		//
		// Get the minimum bounding box for the object [pins only]...
		//
		m_minBox = FactoryMgr.getCommonFactory().createExtent();
		m_oldExtent = FactoryMgr.getCommonFactory().createExtent();
		m_tempLoc = FactoryMgr.getCommonFactory().createLocation();
		m_currValidPoint = FactoryMgr.getCommonFactory().createLocation();
		//
		// minimum size = pin area
		//
		chs.cof.logical.cable.IPinList plc = m_paramObj.getConnectivity();
		m_isConnector = (plc instanceof IConnector);
		ExtentHelper.getPinExtent(m_paramObj, m_minBox, ExtentHelper.IGNORE_PLACEHOLDERS);
		//
		// If nothing there, fall back onto the param extent (for no pin devices).
		//
//		if ((m_paramObj.getConnectivity() instanceof IConnector) || m_minBox.getHeight() == 0 && m_minBox.getWidth() == 0)	{
		m_minBox.addUnion(m_paramObj.getParameterized().getExtent());
//		}

		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		// We shouldn't be in the method if view is not a GfxView
		if (view != null) {
			m_view = (GfxView) view;
			ISchemDiagram diagram = (ISchemDiagram) m_view.getSheet();
			m_grid = diagram.getGrid();
		}

		double borderSize = calculateBorderSize(m_paramObj.getParameterized());
		IExtent mergedExtent = mergeExtents(m_paramObj, m_genParams);

		lowerBorder = 0.0;
		upperBorder = 0.0;

		if (!pinsOnLowerEdge(mergedExtent)) {
			m_minBox.setY(m_minBox.getY() - (int) borderSize);
			m_minBox.setHeight(m_minBox.getHeight() + (int) borderSize);
			lowerBorder = borderSize;
		}

		if (!pinsOnTopEdge(mergedExtent)) {
			m_minBox.setHeight(m_minBox.getHeight() + (int) borderSize);
			upperBorder = borderSize;
		}

		//
		// Allow for rotations...
		//
		ExtentHelper.transformExtent(m_paramObj.getTransform(), m_minBox);
		m_minBox.setX(m_minBox.getX() + m_paramObj.getLocation().getX());
		m_minBox.setY(m_minBox.getY() + m_paramObj.getLocation().getY());

		return IActionEnum.eActivated;
	}

	private double calculateBorderSize(IParameterized paramObj)
	{
		double borderSize = m_genParams.getBorder();
		if (m_isConnector) {
			borderSize = 1.0;
		}
		else {
			IParameterContainer borderParam = paramObj.findParameterContainerByName(Generator.BORDER_PARAM_TYPE);
			if (borderParam != null && borderParam.getName() != null) {
				// Try to get the value
				try {
					borderSize = Double.parseDouble(borderParam.getValue());
				}
				catch (NumberFormatException nfe) {
				}
			}
		}
		borderSize *= (double) m_genParams.getSpacing();
		return borderSize;
	}

	private boolean pinsOnTopEdge(IExtent extent)
	{
		for (IAbstractSchemPin pin : m_paramObj.getAllPins()) {
			ILocation pl = pin.getLocation();
			if (pl.getY() == extent.getTop() && !(pl.getX() == extent.getLeft() || pl.getX() == extent.getRight())) {
				return true;
			}
		}
		return false;
	}

	private boolean pinsOnLowerEdge(IExtent extent)
	{
		for (IAbstractSchemPin pin : m_paramObj.getAllPins()) {
			ILocation pl = pin.getLocation();
			if (pl.getY() == extent.getBottom() && !(pl.getX() == extent.getLeft() || pl.getX() == extent.getRight())) {
				return true;
			}
		}
		return false;
	}

	private IExtent mergeExtents(IPinList pinlist, GeneratorParameters gp)
	{
		IParameterized params = pinlist.getParameterized();
		IExtent gfxExtent = null;
		if (params != null) {
			gfxExtent = params.getExtent();
		}
		GeneratorStyle gs = m_generator.getStyle();
		IExtent pinOnlyExtent = gs.getPinOnlyExtent(pinlist, gfxExtent == null);
		if (gfxExtent == null) {
			//
			// Fall back onto the pin extent.
			//
			gfxExtent = FactoryMgr.getCommonFactory().constructExtent(pinOnlyExtent.getX(), pinOnlyExtent.getY(),
					pinOnlyExtent.getWidth(), pinOnlyExtent.getHeight());
			if (params != null) {
				params.setExtent(gfxExtent);
			}
		}
		else {
			//
			// Bring in the pin only extent too.
			//
			gfxExtent.addUnion(pinOnlyExtent);
		}
		if (gfxExtent.getWidth() == 0) {
			gfxExtent.setWidth((int) (gp.getSpacing() * gp.getWidth()));
		}
		return gfxExtent;
	}

	public boolean onTerminate(boolean successful)
	{
		if (successful) {
			IExtent origObjExt = m_paramObj.getParameterized().getExtent();
			//
			// Get the device, and do things to it. May affect mated object too.
			//
			AffineTransform at = getTransform();
			IExtent ext = getResizeExtent(at, origObjExt);
			if (ext == null) {
				return true;
			}

			ResizeHelper resizeHelper = getResizeHelper(m_paramObj, m_genParams);
			resizeHelper.doResizePinList(origObjExt, ext, at);
		}

		m_dynamics.removeAllDynamicGfx();
		m_dynamics.removeAllTransientGfx();
		m_minBox = null;
		if (m_view != null) {
			m_view.invalidate(IViewInvalidationEnum.eFull);
		}
		return true;
	}

	@NotNull protected ResizeHelper getResizeHelper(IPinList pinList, GeneratorParameters parm)
	{
		return new ResizeHelper(pinList, parm);
	}

	/**
	 * Get the new desired extent of the object constrained the absolute minumum size without any pins
	 *
	 * @param at Transform for rotation/flip
	 * @param origObjExtent
	 *
	 * @return IExtent the extent to resize to
	 */
	private IExtent getResizeExtent(AffineTransform at, IExtent origObjExtent)
	{
		if (m_areaRect == null) {
			return null;
		}
		IExtent ext = m_areaRect.getExtent();
		ext.setX(ext.getX() - m_paramObj.getLocation().getX());
		ext.setY(ext.getY() - m_paramObj.getLocation().getY());

		//noinspection UnusedCatchParameter,EmptyCatchBlock
		try {
			AffineTransform ati = at.createInverse();
			//
			// Transform back...
			//
			ExtentHelper.transformExtent(ati, ext);
		}
		catch (NoninvertibleTransformException e) {
		}
		ext.setY(ext.getY() + (int) lowerBorder);
		//SP1704_dts0101253719_The hot spots are shifted off grid and pins can’t be added to the body of the device when the param:border is set to 0.75
		//ext.setHeight(ext.getHeight() - (int) (lowerBorder + upperBorder));
		int val = ext.getHeight() - (int) (lowerBorder + upperBorder);
		ext.setHeight(m_grid != null ? m_grid.snap(val) : val);

		// No need to do same for height as the extent is from the upper to lower pin positions and this
		// already takes account for extra spacing for the upper/lower edges
		return ext;
	}

	/**
	 * Get the AffineTransform of the parameterised object
	 *
	 * @return AffineTransorm
	 */
	private AffineTransform getTransform()
	{
		ITransform tform = m_paramObj.getTransform();
		AffineTransform at = tform.getAffineTransform();
		return at;
	}

	private static void dumpAbs(String s, IPin p, ILocation curr)
	{
		System.err.println(s + ' ' + p.getConnectivity().getName() + " -- " + curr);
	}

	/**
	 * Return our matching ActionUI class
	 */
	public String getActionUIClass()
	{
		return ResizeActionUI.class.getName();
	}

	// Enabled if there are any IParameterized objects selected.
	public boolean isEnabled()
	{
		SelectionFilter filter = new SelectionFilter();
		filter.addOnlyClass(IPinList.class);

		SelectSet selections = getController().getSelectMgr().getPreSelections();
		return getOperand(selections) != null && super.isEnabled();
	}

	@Nullable private static IPinList getOperand(@NotNull SelectSet selections)
	{
		IPinList candidate = null;
		for (SelectionIterator iter = selections.getSelected(); iter.hasNext(); ) {
			Selection sel = iter.getNext();
			if (IPinList.class.isAssignableFrom(sel.getSelectionClass())) {
				//
				// Get the object, and see if it is got a reference.
				//
				IUIDObject uidObj = UIDMgr.getObject(sel.getUID());
				if (uidObj instanceof IPinList) {
					IPinList pl = (IPinList) uidObj;
					if (pl.getParameterized() != null) {
						chs.cof.logical.cable.IPinList plc = pl.getConnectivity();
						if (plc instanceof IDeviceConnector ||
								!(plc instanceof IDeviceLikePinlist || plc instanceof IConnector)) {
							continue; // not everything that is parameterized may be resized.
						}
						if (candidate == null) {
							candidate = pl;
						}
						else {
							return null; // Multiple selections
						}
					}
				}
			}
		}

		// For Logic it is possible to select objects not on the active diagram - this is not allowed here
		if (candidate != null) {
			if (!m_bUnitTest && GfxObjectUtils.getDiagram(candidate) != CAFUtils.getInstance().getActiveDiagram()) {
				candidate = null;
			}
		}

		if (candidate != null) {
			//do not allow stretch on child modular schematics.
			if (ConnectorHelper.getParentSchemPinList(candidate) != null) {
				candidate = null;
			}
		}

		return candidate;
	}

	// Put ourselves in the context menu if there are
	// any IParameterized objects selected.
	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (getOperand(selections) != null) {
			container.add(new ActionEntry(getActionUI()));
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

	/**
	 * Description of the Method
	 *
	 * @param e Description of Parameter
	 */
	public void mouseMoved(MouseEvent e)
	{
		boolean fresh = false;
		//
		if (m_areaRect == null) {
			fresh = true;
		}
		Point p = CAFUtils.getInstance().getWorldPoint(e.getPoint(), e.getSource());
		updateAreaRectangle(p);
		if (fresh) {
			m_dynamics.addTransientGfx(m_areaRect);
		}
		m_view.invalidate(IViewInvalidationEnum.eTransient);
	}

	public void updateAreaRectangle(Point p)
	{
		IExtent ae;
		if (m_areaRect == null) {
			IDrawFactory drawFactory = FactoryMgr.getDrawFactory();
			m_areaRect = drawFactory.constructRectangle(m_minBox.getX(), m_minBox.getY(),
					m_minBox.getX() + m_minBox.getWidth(), m_minBox.getY() + m_minBox.getHeight());
			//fresh = true;
			//
			// Work out the inital anchor based on the initial mouse position and its location to
			// the object...
			//
			horizAnchor = IndicatorOrientation.WEST;
			if (p.x < m_minBox.getLeft()) {
				horizAnchor = IndicatorOrientation.EAST;
			}
			vertAnchor = IndicatorOrientation.SOUTH;
			if (p.y < m_minBox.getBottom()) {
				vertAnchor = IndicatorOrientation.NORTH;
			}
			m_outsideH =
					(m_currValidPoint.getX() < m_minBox.getLeft() || m_currValidPoint.getX() > m_minBox.getRight());
			m_outsideV =
					(m_currValidPoint.getY() < m_minBox.getBottom() || m_currValidPoint.getY() > m_minBox.getTop());

			// handle the ring terminal object
			ae = m_areaRect.getExtent();
			handleResizeRestrictions(ae);
		}
		//
		if (m_grid != null) {
			m_currValidPoint.setLocation(m_grid.snap(p.x), m_grid.snap(p.y));
		}
		else {
			m_currValidPoint.setLocation(p.x, p.y);
		}
		ae = m_areaRect.getExtent();
		//
		// Outside - keep the anchor.
		//
		m_oldExtent.setBounds(ae.getX(), ae.getY(), ae.getWidth(), ae.getHeight());

		if (m_currValidPoint.getX() < m_minBox.getLeft()) {
			if (!m_outsideH) {
				horizAnchor = IndicatorOrientation.EAST;
			}
			m_outsideH = true;
		}
		else if (m_currValidPoint.getX() > m_minBox.getRight()) {
			if (!m_outsideH) {
				horizAnchor = IndicatorOrientation.WEST;
			}
			m_outsideH = true;
		}
		else {
			m_outsideH = false;
		}

		if (m_currValidPoint.getY() < m_minBox.getBottom()) {
			if (!m_outsideV) {
				vertAnchor = IndicatorOrientation.NORTH;
			}
			m_outsideV = true;
		}
		else if (m_currValidPoint.getY() > m_minBox.getTop()) {
			if (!m_outsideV) {
				vertAnchor = IndicatorOrientation.SOUTH;
			}
			m_outsideV = true;
		}
		else {
			m_outsideV = false;
		}
		//
		IExtent tied = m_minBox; // m_paramObj.getParameterized().getExtent();
		//
		ae.invalidate();
//        ae.addUnion(m_minBox);
		ae.addUnionLocation(m_currValidPoint);

		if (horizAnchor == IndicatorOrientation.EAST) {
			m_tempLoc.setLocation(tied.getRight(), m_currValidPoint.getY());
			ae.addUnionLocation(m_tempLoc);
		}
		else if (horizAnchor == IndicatorOrientation.WEST) {
			m_tempLoc.setLocation(tied.getLeft(), m_currValidPoint.getY());
			ae.addUnionLocation(m_tempLoc);
		}
		if (vertAnchor == IndicatorOrientation.SOUTH) {
			m_tempLoc.setLocation(m_currValidPoint.getX(), tied.getBottom());
			ae.addUnionLocation(m_tempLoc);
		}
		else if (vertAnchor == IndicatorOrientation.NORTH) {
			m_tempLoc.setLocation(m_currValidPoint.getX(), tied.getTop());
			ae.addUnionLocation(m_tempLoc);
		}
		ae.addUnionLocation(m_currValidPoint);

		handleResizeRestrictions(ae);
	}

	private void handleResizeRestrictions(IExtent ae)
	{
		if (IConnector.Statics.isRingTerminalTypeConnector(m_paramObj)) {
			LogicUtils.restrictRingTerminalResizeOnPinSide(ae, m_minBox, m_paramObj);
		}
		else {
			chs.cof.logical.cable.IPinList plc = m_paramObj.getConnectivity();
			if (plc instanceof IConnector && !((IConnector) plc).isInline()) {
				LogicUtils.allowResizeOnPinSide(ae, m_minBox, m_paramObj);
			}
		}
		m_tempLoc.setLocation(ae.getX(), ae.getY());
		m_areaRect.setLocation(m_tempLoc);
		m_areaRect.setWidth(ae.getWidth());
		m_areaRect.setHeight(ae.getHeight());
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(CreateDeviceAction.class, "ResizeAction.StatusBar.text");
	}

	/**
	 * Return the cursor for this action
	 */
	public Cursor getCursor()
	{
		return CAFUtils.getInstance().loadCursor(Cursor.DEFAULT_CURSOR);
	}
}
