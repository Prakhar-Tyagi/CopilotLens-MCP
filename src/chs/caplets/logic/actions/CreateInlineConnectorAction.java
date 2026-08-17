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
import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.actions.inlineassist.InlineDirection;
import chs.caplets.logic.actions.inlineassist.InsertInlineResult;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.IECAttributeResolver;
import chs.cof.logical.schem.IPinList;
import chs.services.dynamicgfx.DynamicRectanglePair;
import chs.services.dynamicgfx.DynamicRotatableRectanglePair;
import chs.services.dynamicgfx.DynamicRotationIndicator;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.ISmartPoint;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utility.ConductorSplitter;
import chs.utility.InlineConductorSplitter;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.CreationDeletionHelper;
import org.jetbrains.annotations.NotNull;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * DOCUMENT ME!
 *
 * @author Matt Boyd
 */
public class CreateInlineConnectorAction extends CreateConnectorAction
{

	private static Cursor m_inlineCursor = null;

	/**
	 * The connector subtype.
	 */
	private DynamicRectanglePair m_dynamic;
	protected IPinList m_schemPlug;
	protected IPinList m_schemJack;
	protected int m_matePinXCoord;
	private List<IPinList> m_connectors = new ArrayList<>();

	public CreateInlineConnectorAction(ICapletController controller)
	{
		super(controller);
		if (m_inlineCursor == null) {
			m_inlineCursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/cur_inline.gif", new Point(7, 7));
		}
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		m_schemPlug = null;
		m_schemJack = null;
		m_dynamic = null;
		m_connectors.clear();
		return super.onActivate(e);
	}

	protected String getWidthStr(IGfxObject obj, int spacing)
	{
		if (obj == m_dynamic.getDrawableNoGrip()) {
			return super.getWidthStr(m_dynamic.getFirstRectangle(), spacing) + " + " +
					super.getWidthStr(m_dynamic.getSecondRectangle(), spacing);
		}
		else {
			return super.getWidthStr(obj, spacing);
		}
	}

	/**
	 * Gets the ActionUIClass attribute of the CreateCircleAction object
	 *
	 * @return The ActionUIClass value
	 */
	public String getActionUIClass()
	{
		return CreateInlineConnectorActionUI.class.getName();
	}

	public InsertInlineResult.ResultInlineConnector getResultConnector()
	{
		return new InsertInlineResult.ResultInlineConnector(m_schemPlug, m_schemJack);
	}

	protected IGfxObject constructDisplayObject(List<ISmartPoint> point_list)
	{
		return null;
	}

	protected List<? extends IGfxObject> constructDisplayObjects(List<ISmartPoint> point_list)
	{
		List<IPinList> displayObjects = constructInlineConnectorPair(point_list);
		if (m_schemJack != null && m_schemPlug != null && displayObjects.size() == 2
				&& displayObjects.contains(m_schemJack) && displayObjects.contains(m_schemPlug)) {
			generateSecondaryRepresentation(displayObjects);
			IECAttributeResolver.inheritIECAttributesIfNotPresent(getDiagram(), m_schemPlug);
			IECAttributeResolver.inheritIECAttributesIfNotPresent(getDiagram(), m_schemJack);
			return displayObjects;
		}
		else {
			return Collections.emptyList();
		}
	}

	static final ISmartPoint[] SPAR = new ISmartPoint[0];

	// To be valid, a point list for creation of an inline connector must have three points, each with a distinct X coordinate.
	protected boolean validPointList(Collection<ISmartPoint> col)
	{
		if (col.size() != 3) {
			return false;
		}
		ISmartPoint[] spar = col.toArray(SPAR);

		Point pt1 = spar[0].getAbsoluteLocation();
		Point pt2 = spar[1].getAbsoluteLocation();
		Point pt3 = spar[2].getAbsoluteLocation();

		return !(pt1 == null || pt2 == null || pt3 == null);
	}

	protected boolean canChangeRotation()
	{
		// We disable rotation while drawing the second rectangle. This is not strictly necessary but
		// conforms to the spec for FEAT3079.
		return m_dynamic.getPointsPlaced() < 2;
	}

	/**
	 * Define the connector we're adding as a jack connector.
	 */
	protected void setJackSubType()
	{
		setSubType(INLINE_JACK_CONNECTOR);
	}

	/**
	 * Define the connector we're adding as a plug connector.
	 */
	protected void setPlugSubType()
	{
		setSubType(INLINE_PLUG_CONNECTOR);
	}

	protected List<IPinList> constructInlineConnectorPair(List<ISmartPoint> point_list)
	{

		// Get upper left corner and lower right corner form coordinate list.
		Iterator<ISmartPoint> iter = point_list.iterator();
		ISmartPoint spt = iter.next();
		Point pt1 = spt.getAbsoluteLocation();

		spt = iter.next();
		Point pt2 = spt.getAbsoluteLocation();

		spt = iter.next();
		Point pt3 = spt.getAbsoluteLocation();
		return constructInlineConnectorPair(pt1, pt2, pt3);
	}

	protected final List<IPinList> constructInlineConnectorPair(Point pt1, Point pt2, Point pt3)
	{
		DynamicRotationIndicator indicator = getRotationIndicator();

		// FEAT3079: removed check for zero length connector. It's already checked in
		// validPointList() before we get here.

		int jackTop;
		int jackBottom;
		int jackLeft;
		int jackRight;
		int plugTop;
		int plugBottom;
		int plugLeft;
		int plugRight;

		// rotation and flipping are used purely to orient the connector edge with pins in the correct direction.
		// We must position the jack and plug connectors appropriately for the required orientation.
		if (indicator.getVertical()) {
			// Pins on vertical edges so jack and plug are horizontally aligned.
			jackTop = Math.max(pt1.y, pt3.y);
			plugTop = jackTop;
			jackBottom = Math.min(pt1.y, pt3.y);
			plugBottom = jackBottom;

			if (indicator.getReversePinSide()) {
				// Jack to the right.
				jackRight = Math.max(pt1.x, pt3.x);
				plugLeft = Math.min(pt1.x, pt3.x);
				plugRight = pt2.x;
				jackLeft = plugRight;
			}
			else {
				// Jack to the left.
				jackLeft = Math.min(pt1.x, pt3.x);
				plugRight = Math.max(pt1.x, pt3.x);
				jackRight = pt2.x;
				plugLeft = jackRight;
			}
		}
		else {
			// Pins on horizontal edges so jack and plug are vertically aligned.
			jackRight = Math.max(pt1.x, pt3.x);
			plugRight = jackRight;
			jackLeft = Math.min(pt1.x, pt3.x);
			plugLeft = jackLeft;

			if (indicator.getReversePinSide()) {
				// Jack above
				jackTop = Math.max(pt1.y, pt3.y);
				plugBottom = Math.min(pt1.y, pt3.y);
				jackBottom = pt2.y;
				plugTop = jackBottom;
			}
			else {
				// Jack below
				jackBottom = Math.min(pt1.y, pt3.y);
				plugTop = Math.max(pt1.y, pt3.y);
				plugBottom = pt2.y;
				jackTop = plugBottom;
			}
		}

		List<IPinList> objects = new ArrayList<IPinList>();

		//
		// Now we've got the points sorted, punt off to this method to create the real object.
		//
		m_connectors.clear();
		setJackSubType();
		m_schemJack = (IPinList) createParamObject(
				new Point(jackLeft, jackTop),
				new Point(jackRight, jackBottom));
		m_matePinXCoord = getRotationIndicator().getReversePinOrder() ? 0 : m_schemJack.getExtent().getWidth();
		objects.add(m_schemJack);
		CreationDeletionHelper.getTheCreationHelper().addCreationObject(m_schemJack);

		setPlugSubType();
		m_schemPlug = (IPinList) createParamObject(
				new Point(plugLeft, plugTop),
				new Point(plugRight, plugBottom));

		// Connect plug and jack
		ConnectionHelper.connectPinLists(m_schemJack, m_schemPlug,
				getLocalModel().getSheet());
		objects.add(m_schemPlug);
		CreationDeletionHelper.getTheCreationHelper().addCreationObject(m_schemPlug);

		return objects;
	}

	protected void connectGfxObjectToModel(IGfxObject newObject)
	{
		if (newObject instanceof IPinList) {
			m_connectors.add((IPinList) newObject);

			if (m_connectors.size() == 2) {
				ConductorSplitter splitter = getSplitter();
				GfxView gview = (GfxView) CAFUtils.getInstance().getViewForDiagram(getDiagram());
				splitter.splitConductors(m_connectors, gview, allowPinCreationAtPlaceholders(), true, isCtrlDown(), ()->{});
			}
		}
	}

	@NotNull protected ConductorSplitter getSplitter()
	{
		return new InlineConductorSplitter();
	}

	/**
	 * Description of the Method
	 *
	 * @param e Description of the Parameter
	 */
	public void mouseClicked(MouseEvent e)
	{
		if (e.getClickCount() == 2) {
			getController().getActionMgr().commitActiveAction();
		}
		else {
			super.mouseClicked(e);
		}
	}

	/**
	 * Description of the Method
	 *
	 * @param ref_point Description of the Parameter
	 *
	 * @return Description of the Return Value
	 */
	protected IDynamicGfx constructDynGfx(Point ref_point)
	{
		/** @todo Use DynamicRectanglePairAugmented when it works properly. */

		DynamicRotationIndicator indicator = new DynamicRotationIndicator(true);
		setRotationIndicator(indicator);
		m_dynamic = new DynamicRotatableRectanglePair(new Point(ref_point.x, ref_point.y),
				new Point(ref_point.x, ref_point.y), new Point(ref_point.x, ref_point.y), FactoryMgr.getDrawFactory(),
				true, indicator, this);
		return m_dynamic;
	}

	/**
	 * Description of the Method
	 *
	 * @return Description of the Return Value
	 */
	protected Class<IPinList> snappingSource()
	{
		//dts0100624058-AUTOFAIL REGRESSION: Placing an inline on the existing conductor is not splitting it.
		return IPinList.class;
	}

	/**
	 * Set the status text for this action
	 */
	public String getStatusbarText()
	{
		return ResourceMgr.getString(CreateInlineConnectorAction.class, "CreateInlineConnectorAction.StatusBar.text");
	}

	public Cursor getCursor()
	{
		return m_inlineCursor;
	}

	/**
	 * @return boolean indicating if resizing from right to left, this is only an issue when placing the second
	 * component of an inline in order that the position of the minimum size of the second part is calculated correctly
	 */
	protected boolean fromRight()
	{
		if (m_dynamic.getPointsPlaced() < 2) {
			return false;
		}
		else {
			Iterator<ISmartPoint> iter = m_point_list.iterator();
			ISmartPoint spt = iter.next();
			Point pt1 = spt.getAbsoluteLocation();
			spt = iter.next();
			Point pt2 = spt.getAbsoluteLocation();
			if (getRotationIndicator().getVertical()) {
				return pt1.x > pt2.x;
			}
			else {
				return pt1.y > pt2.y;
			}
		}
	}

	public void setPointsForPlacement(List<ISmartPoint> points, @NotNull InlineDirection direction)
	{
		m_point_list.clear();
		m_point_list.addAll(points);
		pinsVertical = !direction.isVertical();
		rotationIndicator.setVertical(pinsVertical);
		rotationIndicator.setReversePinSide(direction.isReversedPinSide());
	}
}