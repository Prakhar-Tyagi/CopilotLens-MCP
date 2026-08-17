/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caf.caplet.helpers.snapping;

import chs.caplets.logic.actions.ConnectionFlow;
import chs.cof.draw.FlipAxisEnum;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IDrawFactory;
import chs.cof.draw.IRectangle;
import chs.cof.draw.IText;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.IECAttributeResolver;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.IHarnessPlugConnector;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IPinObject;
import chs.cof.logical.schem.IPinPlaceholder;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.Side;
import chs.system.FactoryMgr;
import chs.utilities.CHSConstants;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.ConnectorHelper;
import chs.utility.helpers.CoordinateHelper;
import chs.utility.helpers.ExtentHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.TextHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * placeholder to depict connector representation against a device pin
 */
public class SchemConnectorPlaceholder
{

	public static final String NEW_CONNECTOR_TEXT =
			ResourceMgr.getString(SchemConnectorPlaceholder.class, "SchemConnectorPlaceholder.NewConnectorText");
	@NotNull private IPinObject m_targetPinObject;
	@NotNull private ConnectorPlaceholder m_owner;
	@NotNull private ICompoundObject m_gfxObject;
	@NotNull private Side m_orientation;
	private static final int WIDTH = CHSConstants.PIN_SPACING;
	private static final int HEIGHT = 2 * CHSConstants.PIN_SPACING;
	private static final int TEXT_HEIGHT = CHSConstants.PIN_SPACING / 2;
	private static final int MINUS_NINETY_DEGREE = -90;
	private boolean m_isNearByConnector;
	private IText m_newConnectorText;

	SchemConnectorPlaceholder(@NotNull IPinObject targetPinObject, @NotNull ConnectorPlaceholder connectorPlaceholder,
			boolean nearByConnector)
	{
		m_targetPinObject = targetPinObject;
		m_owner = connectorPlaceholder;
		m_isNearByConnector = nearByConnector;
		String connectorName = connectorPlaceholder.getName();
		IDrawFactory drawFactory = FactoryMgr.getDrawFactory();
		ICompoundObject compoundObject = drawFactory.createCompoundObject();
		ILocation pinLocation = CoordinateHelper.getAbsGfxLocation(targetPinObject, 0, 0);
		int x = pinLocation.getX();
		int y = pinLocation.getY() - (HEIGHT / 2);
		IRectangle rectangle = drawFactory.constructRectangle(x, y, x + WIDTH, y + HEIGHT);
		compoundObject.addObject(rectangle);
		IText nameText = drawFactory.constructText(x, y, TEXT_HEIGHT, 0, connectorName);
		compoundObject.addObject(nameText);
		if (connectorPlaceholder.getExistingConnector() == null && !nearByConnector) {
			m_newConnectorText = TextHelper.
					createTextForCurrentLocale(x, pinLocation.getY(), TEXT_HEIGHT, 0, NEW_CONNECTOR_TEXT);
			int textWidth = m_newConnectorText.getTextExtent().getWidth();
			int offSetX = textWidth + connectorPlaceholder.getDiagram().getGrid().getGridSpacing();
			m_newConnectorText.getLocation().setX(x - offSetX);
			compoundObject.addObject(m_newConnectorText);
		}
		m_orientation = getPinSide(targetPinObject);
		adjustOrientation(compoundObject, m_orientation, pinLocation);
		m_gfxObject = compoundObject;
	}

	private void adjustOrientation(@NotNull ICompoundObject compoundObject, @NotNull Side side,
			@NotNull ILocation anchor)
	{
		if (side.isLeft()) {
			compoundObject.flip(FlipAxisEnum.YAxis, anchor.getX(), anchor.getY(), 0, 0);
		}
		else if (side.isBottom()) {
			compoundObject.rotate(MINUS_NINETY_DEGREE, anchor.getX(), anchor.getY(), 0, 0);
			compoundObject.flip(FlipAxisEnum.XAxis, anchor.getX(), anchor.getY(), 0, 0);
		}
		else if (side.isTop()) {
			compoundObject.rotate(MINUS_NINETY_DEGREE, anchor.getX(), anchor.getY(), 0, 0);
		}
	}

	@NotNull public IPinObject getTargetPinObject()
	{
		return m_targetPinObject;
	}

	@NotNull private Side getPinSide(@NotNull IPinObject targetPinObject)
	{
		IDiagramObject parent = null;
		if (targetPinObject instanceof IAbstractSchemPin) {
			parent = ((IDiagramObject) targetPinObject).getParent();
		}
		else if (targetPinObject instanceof IPinPlaceholder) {
			parent = ((IPinPlaceholder) targetPinObject).getOwner();
		}
		if (parent != null) {
			IExtent parentExtent = ExtentHelper.getAbsNonTextExtent(parent);
			ILocation pinLocation = CoordinateHelper.getAbsGfxLocation(targetPinObject, 0, 0);
			return Side.getSide(parentExtent, pinLocation);
		}
		return Side.RIGHT;
	}

	@NotNull public IPinList getPinParent(@NotNull IPinObject pinObject)
	{
		IDiagramObject parent = null;
		if (pinObject instanceof IAbstractSchemPin) {
			parent = ((IDiagramObject) pinObject).getParent();
		}
		else if (pinObject instanceof IPinPlaceholder) {
			parent = ((IPinPlaceholder) pinObject).getOwner();
		}
		if (parent instanceof IPinList) {
			return (IPinList) parent;
		}
		throw new IllegalArgumentException();
	}

	@NotNull public ICompoundObject getGfxObject()
	{
		return m_gfxObject;
	}

	@NotNull public ConnectorPlaceholder getOwner()
	{
		return m_owner;
	}

	@NotNull public Point getSnapPoint()
	{
		ILocation pinLocation = CoordinateHelper.getAbsGfxLocation(m_targetPinObject, 0, 0);

		if (!isLocationTransformationNeeded()) {
			return new Point(pinLocation.getX(), pinLocation.getY());
		}

		int deltaX = 0;
		if (m_orientation.isHorizontal()) {
			deltaX = m_orientation.isLeft() ? -WIDTH : WIDTH;
		}
		int deltaY = 0;
		if (m_orientation.isVertical()) {
			deltaY = m_orientation.isBottom() ? -WIDTH : WIDTH;
		}
		return new Point(pinLocation.getX() + deltaX, pinLocation.getY() + deltaY);
	}

	private boolean isLocationTransformationNeeded()
	{
		// In Auto-GHC flow, when a connector is connected to a device backshell termination,
		// the schematic backshell termination is transferred to the connector.
		// Keep the original pin location (no transformation) so the wire connects at the correct point.
		IPinList pinParent = getPinParent(m_targetPinObject);
		return !pinParent.getConnectivity().equals(m_owner.getConnector());
	}

	@NotNull private IPinList createSchemConnector()
	{
		IHarnessPlugConnector connector = m_owner.getTranformedConnector();
		PinListTypeEnum subType = PinListTypeEnum.from_connectivity(connector);
		ISchemDiagram diagram = m_owner.getDiagram();
		ILocation pinLocation = CoordinateHelper.getAbsGfxLocation(m_targetPinObject, 0, 0);
		IPinList schemConnector = ConnectorHelper.createSchemConnector(diagram, subType, connector,
				pinLocation.getX(), pinLocation.getY(), WIDTH, 0, null);
		IAbstractPinIterator connectorPins = connector.getPins();
		if (subType == PinListTypeEnum.TypeRingTerminal && connectorPins.hasNext()) {
			schemConnector.getObjects(IPinPlaceholder.class).stream()
					.forEach(pinPlaceholder -> pinPlaceholder.transmogrify(connectorPins.getNext(), diagram.getGrid()));
		}
		adjustOrientation(schemConnector, m_orientation, pinLocation);
		IECAttributeResolver.inheritIECAttributesIfNotPresent(diagram, schemConnector);
		schemConnector.applyStyle();
		return schemConnector;
	}

	@Nullable public IPinList transformToSchemConnector()
	{
		if (m_targetPinObject instanceof IPinPlaceholder) {
			IPin transformedPin = ((IPinPlaceholder) m_targetPinObject).transmogrify(m_owner.getDiagram().getGrid());
			if (transformedPin == null) {
				return null;
			}
			m_targetPinObject = transformedPin;
		}
		IPinList schemConnector = createSchemConnector();
		ConnectionHelper.connectDeviceAndConnector(Set.of(m_targetPinObject), getPinParent(m_targetPinObject),
				schemConnector, m_owner.getDiagram(), false, null, ConnectionFlow.AutCreateConnectorConnection);
		return schemConnector;
	}

	public boolean areReferencesEditable()
	{
		ILogicDesign design = m_owner.getDesign();
		if (design.isUnderConcurrentEdit()) {
			Set<IUID> failedLocks = LogicObjectLockFinder.tryEdit(design, getAffectedObjects());
			return failedLocks.isEmpty();
		}
		return true;
	}

	@NotNull private Collection<? extends IUIDObject> getAffectedObjects()
	{
		Collection<IUIDObject> affectedObjects = new ArrayList<>();
		IPinList pinParent = getPinParent(m_targetPinObject);
		chs.cof.logical.cable.IPinList pinParentConnectivity = pinParent.getConnectivity();
		if (pinParentConnectivity != null) {
			affectedObjects.add(pinParentConnectivity);
		}
		IHarnessPlugConnector existingConnector = m_owner.getExistingConnector();
		if (existingConnector != null) {
			affectedObjects.add(existingConnector);
		}
		return affectedObjects;
	}

	public boolean isNearByConnector()
	{
		return m_isNearByConnector;
	}

	public void removeNewConnectorText()
	{
		if (m_newConnectorText != null) {
			getGfxObject().removeObject(m_newConnectorText);
		}
	}
}