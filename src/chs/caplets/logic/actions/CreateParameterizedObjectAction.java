/*
 * Copyright 2002-2008 Mentor Graphics Corporation
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
import chs.caf.caplet.helpers.creation.CreateByPointAction;
import chs.caplets.logic.Model;
import chs.cof.draw.HorizJustificationEnum;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGrid;
import chs.cof.draw.IGriddable;
import chs.cof.draw.VertJustificationEnum;
import chs.cof.drawplus.IDiagramText;
import chs.cof.drawplus.IDrawPlusFactory;
import chs.cof.drawplus.IMasterRepresentation;
import chs.cof.logical.IECAttributeResolver;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.ICableFactory;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IPinListShapeDescriptor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemFactory;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.logical.shared.UsageUpdateStrategy;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.GeneratorStyle;
import chs.common.ICommonFactory;
import chs.common.IExtent;
import chs.common.IParameterContainer;
import chs.common.IParameterized;
import chs.common.IProjectPreferenceMgr;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.attr.IAttributeTypes;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.ctf.caf.ui.PinListPlaceOptionParams;
import chs.services.dynamicgfx.DynamicRotationIndicator;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.IResizeConstraint;
import chs.services.dynamicgfx.ISmartPoint;
import chs.services.dynamicgfx.ISmartPointIterator;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.CHSConstants;
import chs.utilities.ResourceMgr;
import chs.utility.DiagramHelper;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.TextHelper;
import chs.utility.logic.LogicUtils;
import chs.utility.logic.SizeHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * * This class provides all of the 'point/mouse collection' functionality for * adding a parameterized object - The
 * specific object creation classes should * extend this, and provide the abstract methods.
 */
public abstract class CreateParameterizedObjectAction extends CreateByPointAction implements IResizeConstraint
{

	protected static final int STATE_LOITER = 0;
	protected static final int STATE_SYMBOL = 1;
	protected static final int STATE_PARAM = 2;
	protected static final int STATE_PINS = 3;
	protected static final int STATE_GENERATE = 4;

	private int m_state = STATE_LOITER;
	protected IDynamicGfx m_sharedAutoGenDynamic = null;
	protected IGrid m_grid;

	private Model m_model;
	private boolean m_ctrlDown = false;
	protected DynamicRotationIndicator rotationIndicator = null;
	protected boolean pinsVertical = true;
	protected double m_borderSize = 0.0;

	private static Cursor m_genericCursor = CAFUtils.getInstance().loadCursor(Cursor.CROSSHAIR_CURSOR);
	protected static final double TWO = 2.0;

	protected CreateParameterizedObjectAction(ICapletController controller)
	{
		super(controller, true, false);
		m_model = (Model) controller.getCapletModel();
		m_ctrlDown = false;
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		pinsVertical = true;
		m_borderSize = calculateBorderSize();
		return super.onActivate(e);
	}

	@Override public boolean onTerminate(boolean successful)
	{
		boolean actionSuccess = successful;
		UsageUpdateStrategy prevDWUsageStoreStrategy =
				setDesignWideUsageUpdateStrategy(UsageUpdateStrategy.UPDATE_ON_CHANGE);
		//UsageUpdateStrategy prevSharedUsageStoreStrategy =
		//		setSharedUsageUpdateStrategy(UsageUpdateStrategy.UPDATE_ON_CHANGE);
		try {
			if (actionSuccess) {
				preActionChanges();
				actionSuccess = createParamObject(actionSuccess);
				if (actionSuccess) {
					actionSuccess = postActionChanges();
				}
			}
			return actionSuccess;
		}
		finally {
			setDesignWideUsageUpdateStrategy(prevDWUsageStoreStrategy);
			//	setSharedUsageUpdateStrategy(prevSharedUsageStoreStrategy);
			clearAction(actionSuccess);
		}
	}

	protected boolean createParamObject(boolean actionSuccess)
	{
		return super.onTerminate(actionSuccess);
	}

	protected boolean postActionChanges()
	{
		return true;
	}

	protected void preActionChanges()
	{

	}

	protected void clearAction(boolean actionSuccess)
	{
		super.cleanupTrans();
		if (CAFUtils.getInstance().getActiveCapletView() != null) {
			CAFUtils.getInstance().getActiveCapletView().setToolTipProvider(m_savedToolTipProvider);
		}
	}

	protected abstract double calculateBorderSize();

	protected abstract String getObjectType();

	protected double calculateBorderSize(ISchemDiagram diagram)
	{
		IGrid grid = diagram.getGrid();
		GeneratorParameters gp = new GeneratorParameters(grid);

		final ICommonFactory commonFactory = FactoryMgr.getCommonFactory();

		Generator generator = Generator.getGenerator();
		IParameterized params = commonFactory.createParameterized();
		//
		// Get the generator, add the defaults, and go!
		//
		GeneratorStyle gs = generator.getStyle();
		if (gs != null) {
			gs.addDefaults(params, getObjectType());
		}
		double borderSize = calculateBorderSize(gp, params);

		// todo creddy: Why not use temp CDH?
		UIDMgr.removeObject(params.getUID());
		CreationDeletionHelper.getTheCreationHelper().removeCreationObject(params);

		return borderSize;
	}

	@SuppressWarnings({"UnusedCatchParameter", "EmptyCatchBlock"})
	protected double calculateBorderSize(GeneratorParameters gp, IParameterized params)
	{
		double border = gp.getBorder();
		IParameterContainer borderParam = params.findParameterContainerByName(Generator.BORDER_PARAM_TYPE);
		if (borderParam != null && borderParam.getName() != null) {
			// Try to get the value
			try {
				border = Double.parseDouble(borderParam.getValue());
			}
			catch (NumberFormatException nfe) {
			}
		}
		border *= (double) gp.getSpacing();
		return border;
	}

	protected double getBorderSize()
	{
		return m_borderSize;
	}

	protected Model getLocalModel()
	{
		return m_model;
	}

	@Nullable
	protected IGfxObject constructDisplayObject(List<ISmartPoint> point_list)
	{
		IGfxObject newObject = createDisplayObject(point_list);
		if (newObject != null) {
			if (newObject instanceof IPinList) {
				IECAttributeResolver.inheritIECAttributesIfNotPresent(getDiagram(), (IPinList) newObject);
				((IPinList) newObject).applyStyle();
			}
			generateSecondaryRepresentation(Collections.singleton(newObject));
		}
		return newObject;
	}

	protected void generateSecondaryRepresentation(@NotNull Collection<? extends IGfxObject> newObjects)
	{
		ICompoundObject diag = getModel().getSheet();
		for (IGfxObject newObject : newObjects) {
			if (newObject instanceof IMasterRepresentation) {
				Generator.generateSecondaryRepresentation((IMasterRepresentation) newObject, (ISchemDiagram) diag);
			}
		}
	}

	@Nullable
	protected IGfxObject createDisplayObject(List<ISmartPoint> point_list)
	{
		// Get upper left corner and lower right corner form coordinate list.
		Iterator<ISmartPoint> iter = point_list.iterator();
		ISmartPoint spt = iter.next();
		Point pt1 = spt.getAbsoluteLocation();

		spt = iter.next();
		Point pt2 = spt.getAbsoluteLocation();
		return createDisplayObject(pt1, pt2);
	}

	@Nullable
	protected final IGfxObject createDisplayObject(Point pt1, Point pt2)
	{
		//
		// Same point - bad object.
		//
		if (pt1.x == pt2.x && pt1.y == pt2.y) {
			return null;
		}
		//
		// Get pt1 == TL and pt2 == BR.
		//
		int tlx = Math.min(pt1.x, pt2.x);
		int tly = Math.max(pt1.y, pt2.y);

		int brx = Math.max(pt1.x, pt2.x);
		int bry = Math.min(pt1.y, pt2.y);

		pt1.setLocation(tlx, tly);
		pt2.setLocation(brx, bry);
		//
		// Now we've got the points sorted, punt off to this method to create the real object.
		//
		IGfxObject newObject = createParamObject(pt1, pt2);
		if (newObject instanceof IUIDObject) {
			CreationDeletionHelper.getTheCreationHelper().addCreationObject((IUIDObject) newObject);
		}

		assert newObject != null; // this should never be null - should it?
		return newObject;
	}

	public void mousePressed(MouseEvent e)
	{
		if (e.getButton() == MouseEvent.BUTTON3) {
			return;
		}

		int mouseMods = e.getModifiers();
		int ctrlMask = InputEvent.CTRL_MASK;
		m_ctrlDown = ((mouseMods & ctrlMask) == ctrlMask);
		//
		super.markEvent(e);
	}

	protected boolean shouldShowFeedback()
	{
		return true;
	}

	public void mouseMoved(MouseEvent e)
	{
		if (m_dynGfx != null && m_startedDrawing && shouldShowFeedback()) {
			GfxView gv = (GfxView) CAFUtils.getInstance().getActiveCapletView();
			IGfxObject dg = m_dynGfx.getDrawableNoGrip();
			IExtent ext = dg.getExtent();
			int spacing = ((IGriddable) gv.getSheet()).getGrid().getGridSpacing();
			String resource = getFeedbackResourceString();
			if (!getRotationIndicator().getVertical()) {
				resource = getRotatedFeedbackResourceString();
			}
			setFeedbackText(
					ResourceMgr.getString(CreateParameterizedObjectAction.class, resource,
							getPinCountStr(ext, spacing), getWidthStr(dg, spacing)));
		}
		super.mouseMoved(e);
	}

	/*
	 * Indicates whether rotation changes are supported. Default implementation has no restrictions. Derived classes can
	 * override this.
	 */
	protected boolean canChangeRotation()
	{
		return (m_state != STATE_GENERATE);
	}

	protected boolean canflip()
	{
		return (m_state != STATE_GENERATE);
	}

	public void keyReleased(KeyEvent e)
	{
	}

	public void keyPressed(KeyEvent e)
	{
		// canChangeRotation() is false when sizing second box of inline connector.
		// m_current_point is null when placing pins of shared connector.
		if (e.getKeyCode() == KeyEvent.VK_R
				&& rotationIndicator != null
				&& canChangeRotation()
				&& m_current_point != null) {

			pinsVertical = !pinsVertical;
			rotationIndicator.setVertical(pinsVertical);

			// Make a dummy change SmartPoint change to trigger change notification and refresh the dynamic lines.
			m_current_point.applyAbsoluteDelta(new Point(0, 0), true, null);

			// Invalidate the dynamics so they're redrawn.
			ICapletView view = CAFUtils.getInstance().getActiveCapletView();
			view.invalidate(IViewInvalidationEnum.eTransient);
		}
	}

	protected String getPinCountStr(IExtent ext, int spacing)
	{
		int m_createdPinCount = getPinCount(ext, spacing);
		return String.valueOf(m_createdPinCount);
	}

	protected int getPinCount(IExtent ext, int spacing)
	{
		double length = ext.getHeight();
		if (!pinsVertical) {
			length = ext.getWidth();
		}
		if (length >= m_borderSize * TWO) {
			length -= m_borderSize * TWO;
		}
		else if (length >= m_borderSize) {
			length -= m_borderSize;
		}
		return (int) (((length / spacing) + 1) * 2);
	}

	protected String getWidthStr(IGfxObject obj, int spacing)
	{
		IExtent ext = obj.getExtent();
		int modelWidth = ext.getWidth();
		if (!pinsVertical) {
			modelWidth = ext.getHeight();
		}

		return String.valueOf((modelWidth / spacing));
	}

	protected boolean isCtrlDown()
	{
		return m_ctrlDown;
	}

	//
	// This is one method that must be implemented
	//
	protected abstract IGfxObject createParamObject(Point p1, Point p2);

	protected boolean getIndicateBothEdges()
	{
		return true;
	}

	protected void setRotationIndicator(DynamicRotationIndicator indicator)
	{
		rotationIndicator = indicator;
	}

	protected DynamicRotationIndicator getRotationIndicator()
	{
		return rotationIndicator;
	}

	protected IDynamicGfx constructDynGfx(Point ref_point)
	{
		ICommonFactory cfac = FactoryMgr.getCommonFactory();
		IExtent normExt = cfac.constructExtent(ref_point.x, ref_point.y, 1, 1);

		DynamicRotationIndicator indicator = createRotationIndicator();
		return getDynamicGfxService().getFactory().constructRotatableRectangle(normExt, true, indicator, this);
	}

	@Nullable
	protected Class<?> snappingSource()
	{
		return IPinList.class;
	}

	/**
	 * Return the cursor for this action
	 */
	public Cursor getCursor()
	{
		return m_genericCursor;
	}

	protected Model getLogicModel()
	{
		return getLocalModel();
	}

	/**
	 * Set the status text for this action
	 */
	@Nullable
	public String getStatusbarText()
	{
		return ResourceMgr.getString(CreateParameterizedObjectAction.class,
				"CreateParameterizedObjectAction.StatusBar.NoPins.text");
	}

	/**
	 * dont allow pin creation at placeholders, if action does not support it. i.e when adding a device from the library
	 * we should not allow pin creation at placeholders. We can not use shouldAddPins() since some overrides check for
	 * CTRL button. Note: connectPinLists can be called before the library part is assigned.
	 *
	 * @return boolean
	 */
	protected boolean allowPinCreationAtPlaceholders()
	{
		return true;
	}

	protected void connectGfxObjectToModel(IGfxObject newObject)
	{
		if (newObject instanceof IPinList) {
			ConnectionHelper chelper = new ConnectionHelper(allowPinCreationAtPlaceholders());
			GfxView gview = (GfxView) CAFUtils.getInstance().getActiveCapletView();

			// gdh 10/28/03 2815 use new method in ConnectionHelper to connect multiple pinlists

			chelper.connectPinLists((IPinList) newObject, gview.getGfxContext(), gview.getSheet(),
					(IPinList) newObject);
		}
	}

	/**
	 * @return width the minimum width for hte object
	 */
	protected int getMinimumWidth()
	{
		return CHSConstants.PIN_SPACING;
	}

	/**
	 * @return boolean to indicate if sizing from right to left, defaults to false as this is not an issue except for
	 * inline connectors
	 */
	protected boolean fromRight()
	{
		ISmartPointIterator smartPoints = m_dynGfx.getAllGripPoints();
		Point lastPoint = null;
		while (smartPoints.hasNext()) {
			ISmartPoint spoint = smartPoints.next();
			if (lastPoint == null) {
				lastPoint = spoint.getAbsoluteLocation();
			}
			else {
				Point currentPoint = spoint.getAbsoluteLocation();
				if (getRotationIndicator().getVertical()) {
					return currentPoint.getX() < lastPoint.getX();
				}
				else {
					return currentPoint.getY() < lastPoint.getY();
				}
			}
		}
		return false;
	}

	/**
	 * Cosntrain the extent to the minimum size, which is usually one grid wide by twice the borde size high
	 *
	 * @param constExtent extent to constrain
	 */
	@SuppressWarnings({"NumericCastThatLosesPrecision"})
	public void constrainExtent(IExtent constExtent)
	{
		double doubleBorderSize = TWO * m_borderSize;
		if (getRotationIndicator().getVertical()) {
			if ((double) constExtent.getWidth() < getMinimumWidth()) {
				if (fromRight()) {
					// When sizing from right to left need ot adjust the origin as well as the width
					constExtent.setX(constExtent.getX() - (getMinimumWidth() - constExtent.getWidth()));
				}

				constExtent.setWidth(getMinimumWidth());
			}
			if ((double) constExtent.getHeight() < doubleBorderSize) {
				if ((double) constExtent.getHeight() <= m_borderSize) {
					// if the height is less than a border shift the origin so that it is balanaced on either side
					constExtent.setY(constExtent.getY() - (int) m_borderSize);
				}
				constExtent.setHeight((int) doubleBorderSize);
			}
		}
		else {
			if ((double) constExtent.getHeight() < getMinimumWidth()) {
				if (fromRight()) {
					// When sizing from right to left need ot adjust the origin as well as the height
					constExtent.setY(constExtent.getY() - (getMinimumWidth() - constExtent.getHeight()));
				}
				constExtent.setHeight(getMinimumWidth());
			}
			if ((double) constExtent.getWidth() <= m_borderSize) {
				// if the width is less than a border shift the origin so that it is balanaced on either side
				constExtent.setX(constExtent.getX() - (int) m_borderSize);
			}
			if ((double) constExtent.getWidth() < doubleBorderSize) {
				constExtent.setWidth((int) doubleBorderSize);
			}
		}
	}

	/*
	 * Obtains the diagram in which the created object will be placed.
	 */
	protected ISchemDiagram getDiagram()
	{
		return getLocalModel().getDiagram();
	}

	public void destroy()
	{
		super.destroy();
		m_model = null;
	}

	@NotNull protected DynamicRotationIndicator createRotationIndicator()
	{
		DynamicRotationIndicator indicator = new DynamicRotationIndicator(getIndicateBothEdges());
		setRotationIndicator(indicator);
		return indicator;
	}

	protected void constrainExtentByMaxPinCount(IExtent constExtent, int nMaxPinCount)
	{
		GfxView gv = (GfxView) CAFUtils.getInstance().getActiveCapletView();
		int spacing = ((IGriddable) gv.getSheet()).getGrid().getGridSpacing();
		int nPinCount = getPinCount(constExtent, spacing);

		if (nPinCount > 1) {
			//noinspection NumericCastThatLosesPrecision
			int nSize = (int) ((nMaxPinCount + 1) * m_borderSize);
			if (getRotationIndicator().getVertical()) {
				if (shouldAdjustBL()) {
					constExtent.setBottom(constExtent.getBottom() + (constExtent.getHeight() - nSize));
				}
				constExtent.setHeight(nSize);
			}
			else {
				if (shouldAdjustBL()) {
					constExtent.setLeft(constExtent.getLeft() + (constExtent.getWidth() - nSize));
				}
				constExtent.setWidth(nSize);
			}
		}
	}

	protected boolean shouldAdjustBL()
	{
		ISmartPointIterator smartPoints = m_dynGfx.getAllGripPoints();
		Point lastPoint = null;
		while (smartPoints.hasNext()) {
			ISmartPoint spoint = smartPoints.next();
			if (lastPoint == null) {
				lastPoint = spoint.getAbsoluteLocation();
			}
			else {
				Point currentPoint = spoint.getAbsoluteLocation();
				if (getRotationIndicator().getVertical()) {
					return currentPoint.getY() < lastPoint.getY();
				}
				else {
					return currentPoint.getX() < lastPoint.getX();
				}
			}
		}
		return false;
	}

	public int getState()
	{
		return m_state;
	}

	public void setState(int s)
	{
		m_state = s;
		updateStatusbarText();
	}

	protected static IPinList createSchemPinList(chs.cof.logical.cable.IPinList device, Point pt1, Point pt2,
			boolean addPins, DynamicRotationIndicator indicator, String objType)
	{
		// made static for easy reuse from other actions
		// ould refactor into a separate hierararchy in case it needs to be overriddent
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		GfxView gview = (GfxView) view;
		ISchemDiagram diagram = (ISchemDiagram) gview.getSheet();
		return createSchemPinList(diagram, device, pt1, pt2, addPins, indicator, objType);
	}

	@NotNull public static IPinList createSchemPinList(ISchemDiagram diagram, chs.cof.logical.cable.IPinList pinList,
			Point pt1, Point pt2, boolean addPins, DynamicRotationIndicator indicator, String objType)
	{
		GeneratorParameters gp = DiagramHelper.createGeneratorParameters(diagram);
		gp.setNewObject(true);

		// Get our factories
		final ISchemFactory schemFactory = FactoryMgr.getSchemFactory();
		final ICommonFactory commonFactory = FactoryMgr.getCommonFactory();

		Generator generator = Generator.getGenerator();
		IParameterized params = commonFactory.createParameterized();
		//
		// Get the generator, add the defaults, and go!
		//
		GeneratorStyle gs = generator.getStyle();
		if (gs != null) {
			gs.addDefaults(params, objType);
		}

		SizeHelper sizeH = new SizeHelper(pt1, pt2, indicator.getVertical(), params, gp);
		sizeH.setMinModelWidth((int) (gp.getSpacing() * gp.getWidth()));
		int height = sizeH.getModelHeight();
		int width = sizeH.getModelWidth();
		Point lowerLeft = sizeH.getModelLocation();

		// Create visible schem representation & adds pins to it as well as the connectivity.
		IUID uid = commonFactory.createUID();
		IPinList schem_dev = schemFactory.constructPinList(uid, pinList, lowerLeft.x, lowerLeft.y);
		diagram.addObject(schem_dev);
		schem_dev.setParameterized(params);

		//
		// Take the value, and let the CTRL invert it.
		//
		if (addPins) {
			doAddPins(diagram, pinList, schem_dev, indicator, height, width);
		}

		// Note: This should be called before genearting the shape of the pinlist
		if (schem_dev.supportsFill()) {
			schem_dev.setOutlineShapeVersion(IPinListShapeDescriptor.OutlineShapeVersion.getLatestVersion());
		}

		//
		// This area is the extent of the box where the pins would go.
		//
		params.setExtent(commonFactory.constructExtent(0, 0, width, height));
		generator.generateDevice(schem_dev, gp, Generator.REGENERATE_PROPERTIES);

		// Apply property Style for Pins
		for (IPin pin1 : schem_dev.getPins()) {
			Generator.applyStyleSet(pin1, gp);
		}
		for (ISchemStackPin pin1 : schem_dev.getStackPins()) {
			Generator.applyStyleSet(pin1, gp);
		}

		// Pin creation doesn't allow for device rotation, so we rotate the device after adding the pins.
		sizeH.rotateModel(schem_dev);

		return schem_dev;
	}

	protected static void doAddPins(ISchemDiagram diagram, chs.cof.logical.cable.IPinList pinList,
			IPinList schem_dev, DynamicRotationIndicator indicator, int height, int width)
	{
		IGrid grid = diagram.getGrid();
		int pinspacing = grid.getGridSpacing();
		// Get our factories
		final ICableFactory cblFactory = FactoryMgr.getCablePropertiedFactory();
		final ISchemFactory schemFactory = FactoryMgr.getSchemFactory();
		final IDrawPlusFactory drawplusFactory = FactoryMgr.getDrawPlusFactory();
		final ICommonFactory commonFactory = FactoryMgr.getCommonFactory();
		boolean reversePinOrder = indicator.getReversePinOrder();
		for (int side = 0; side <= 1; side++) {
			for (int idx = 0; (idx * pinspacing) <= height; idx++) {
				IUID uid = commonFactory.createUID();
				IAbstractPin cpin = cblFactory.createPinForOwner(uid, pinList);
				cpin.setOwner(pinList);
				//cpin.setName("P" + idx);
				pinList.addPin(cpin);
				LogicUtils.setMatchingShortDescriptionFromOTI(cpin, pinList.getProject());
				uid = commonFactory.createUID();
				int ypos;
				if (reversePinOrder) {
					ypos = height - (idx * pinspacing);
				}
				else {
					ypos = (idx * pinspacing);
				}
				IPin pin = schemFactory.constructPin(uid, cpin, side * width, ypos);
				pin.setHome(true);
				schem_dev.addObject(pin);

				IProjectPreferenceMgr preferences = CAFUtils.getInstance().getCurrentProjectPreferences();

				// If there are no styles defined, follow the existing flow
				IDiagramText nameText = drawplusFactory.constructAttributeText(commonFactory.createUID(), cpin,
						0, 0, 0, 0, IAttributeTypes.NAME);

				if (preferences != null) {
					TextHelper.assignAttributeTextDefaults(nameText, diagram, grid,
							preferences);     // gdh 12/08/03; used to be "Property"
				}
				nameText.setHorizontalJustification(
						(side == 0) ? HorizJustificationEnum.JustLeft : HorizJustificationEnum.JustRight);
				nameText.setVerticalJustification(VertJustificationEnum.JustCenter);
				pin.addObject(nameText);
			}
		}
	}

	protected String getFeedbackResourceString()
	{
		return "CreateParameterizedObjectAction.Feedback.text";
	}

	protected String getRotatedFeedbackResourceString()
	{
		return "CreateParameterizedObjectAction.RotatedFeedback.text";
	}

	@NotNull protected IPlacementOptionParams createPlacementOptionParams(@NotNull ISharedPinList sharedPinList)
	{
		return new PinListPlaceOptionParams(sharedPinList);
	}

	@NotNull
	protected IPlacementOptionParams createPlacementOptionParams(@NotNull PinListTypeEnum pinListType, boolean isShared)
	{
		return new PinListPlaceOptionParams(pinListType, isShared);
	}
}
