package chs.caplets.logic.actions;

import chs.cof.draw.HorizJustificationEnum;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IText;
import chs.cof.draw.VertJustificationEnum;
import chs.cof.drawplus.IAttributeText;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IPinList;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.IParameterized;
import chs.common.Side;
import chs.common.geom.GeometryUtils;
import chs.system.FactoryMgr;
import chs.utility.attr.AttributeUtils;
import chs.utility.helpers.ExtentHelper;
import chs.utility.helpers.TransformHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class PinNameTextJustificationHandler
{

	@Nullable public static PinNameTextJustificationHandler.TextAttrHolder getTextAttributeHolder(IAbstractSchemPin pin)
	{
		PinNameTextJustificationHandler.TextAttrHolder nt1 = null;

		// get first object in collection
		Collection<? extends IGfxObject> coll = pin.getObjects(AttributeUtils.NAME_TEXT_FILTER);
		if (!coll.isEmpty()) {
			nt1 = new PinNameTextJustificationHandler.TextAttrHolder((IText) coll.iterator().next());
		}
		return nt1;
	}

	public static class TextAttrHolder
	{

		private ILocation m_location;
		private int m_rotation;
		private HorizJustificationEnum m_hjust;
		private VertJustificationEnum m_vjust;

		TextAttrHolder(IText txt)
		{
			m_location = FactoryMgr.getCommonFactory().createLocation();
			m_location.setLocation(txt.getLocation().getX(), txt.getLocation().getY());
			m_rotation = txt.getRotation();
			m_hjust = txt.getHorizontalJustification();
			m_vjust = txt.getVerticalJustification();
		}

		public ILocation getLocation()
		{
			return m_location;
		}

		public int getRotation()
		{
			return m_rotation;
		}

		public HorizJustificationEnum getHorizontalJustification()
		{
			return m_hjust;
		}

		public VertJustificationEnum getVerticalJustification()
		{
			return m_vjust;
		}
	}

	public static int getSideAsInteger(@Nullable Side side)
	{
		int sideAsInt = ExtentHelper.SIDE_UNKNOWN;
		if (side == null) {
			return sideAsInt;
		}

		switch (side) {
			case LEFT:
				sideAsInt = ExtentHelper.SIDE_WEST;
				break;
			case RIGHT:
				sideAsInt = ExtentHelper.SIDE_EAST;
				break;
			case TOP:
				sideAsInt = ExtentHelper.SIDE_NORTH;
				break;
			case BOTTOM:
				sideAsInt = ExtentHelper.SIDE_SOUTH;
				break;
		}
		return sideAsInt;
	}

	public static int getSideAsInteger(IAbstractSchemPin pin)
	{
		return getSideAsInteger(getSide(pin));
	}

	public static void justifyPinNameText(IAbstractSchemPin pin)
	{
		Side side = getSide(pin);
		if (side == null) {
			return;
		}

		Collection<? extends IGfxObject> coll = pin.getObjects(AttributeUtils.NAME_TEXT_FILTER);
		IText nameText = null;
		if (!coll.isEmpty()) {
			nameText = ((IText) coll.iterator().next());
		}
		if (nameText == null) {
			return;
		}

		if (side.isLeft()) {
			nameText.setHorizontalJustification(HorizJustificationEnum.JustLeft);
			nameText.setVerticalJustification(VertJustificationEnum.JustCenter);
			nameText.setRotation(GeometryUtils.ZERO_DEGREES);
		}
		else if (side.isRight()) {
			nameText.setHorizontalJustification(HorizJustificationEnum.JustRight);
			nameText.setVerticalJustification(VertJustificationEnum.JustCenter);
			nameText.setRotation(GeometryUtils.ZERO_DEGREES);
		}
		else if (side.isBottom()) {
			nameText.setHorizontalJustification(HorizJustificationEnum.JustRight);
			nameText.setVerticalJustification(VertJustificationEnum.JustCenter);
			nameText.setRotation(GeometryUtils.NINETY_DEGREES);
		}
		else if (side.isTop()) {
			nameText.setHorizontalJustification(HorizJustificationEnum.JustLeft);
			nameText.setVerticalJustification(VertJustificationEnum.JustCenter);
			nameText.setRotation(GeometryUtils.NINETY_DEGREES);
		}
	}

	@Nullable private static Side getSide(IAbstractSchemPin pin)
	{
		IExtent relNonTextExtent = getParentExtent(pin);

		Side side = null;
		if (relNonTextExtent != null) {
			side = Side.getSide(relNonTextExtent, pin.getLocation());
		}
		return side;
	}

	private static IExtent getParentExtent(IAbstractSchemPin pin)
	{
		IExtent relNonTextExtent;
		IPinList parent = (IPinList) pin.getParent();
		IParameterized parameterized = parent.getParameterized();
		if (parameterized == null) {
			relNonTextExtent = ExtentHelper.getNonTextExtent(parent);
		}
		else {
			relNonTextExtent = ExtentHelper.getPinExtent(parent, null, true);
		}
		return relNonTextExtent;
	}

	public void justifyDevicePinNameText(IAbstractSchemPin pin, int oldSide, TextAttrHolder txtholder)
	{
		// If we have GOOD information [txtholder] then we use it. Otherwise we
		// try to do something reasonable.
		int newSide;

		IExtent extent = getParentExtent(pin);
		ILocation location = pin.getLocation();
		boolean isVerticallyAligned = false;
		boolean isHorizontallyAligned = false;
		if (extent.getRight() == location.getX() || extent.getLeft() == location.getX()) {
			isVerticallyAligned = true;
		}
		if (extent.getTop() == location.getY() || extent.getBottom() == location.getY()) {
			isHorizontallyAligned = true;
		}
		if (isVerticallyAligned && (oldSide == ExtentHelper.SIDE_EAST || oldSide == ExtentHelper.SIDE_WEST)) {

			if (extent.getRight() == location.getX()) {
				newSide = ExtentHelper.SIDE_EAST;
			}
			else {
				newSide = ExtentHelper.SIDE_WEST;
			}
		}
		else if (isHorizontallyAligned && (oldSide == ExtentHelper.SIDE_NORTH || oldSide == ExtentHelper.SIDE_SOUTH)) {

			if (extent.getTop() == location.getY()) {
				newSide = ExtentHelper.SIDE_NORTH;
			}
			else {
				newSide = ExtentHelper.SIDE_SOUTH;
			}
		}
		else {
			newSide = getSideAsInteger(pin);
		}

		// Device Text alignment strattegy.
		// o Text on the same side, should always keep its relative position
		// o Text moving to adjacent sides should keep its relative position
		// o Text move to opposite sides
		//   - If relative position is 0.0, just change justification to suit the new side
		//  - else, mirror the relative offset.
		//
		//	*** This algorithm works as expected for device pins, however when used for
		//	*** connector pins moved to another connector it does not work quite right.
		//	*** This is because the sides are relative. You can not compare sides of two connector since each has its
		//  *** own co-ordinate system/transform.  Connector text is handled by MovePinAction::justifyConnectorPinNameText
		Collection<? extends IGfxObject> gfxObjects = pin.getObjects(AttributeUtils.NAME_TEXT_FILTER);
		for (IGfxObject object : gfxObjects) {
			IAttributeText nt = (IAttributeText) object;
			if (txtholder != null) {
				ILocation loc = nt.getLocation();
				loc.setLocation(txtholder.getLocation().getX(), txtholder.getLocation().getY());
				nt.setLocation(loc);
				nt.setHorizontalJustification(txtholder.getHorizontalJustification());
				nt.setVerticalJustification(txtholder.getVerticalJustification());
				nt.setRotation(txtholder.getRotation());

				// The following only works for relative objects. And not moves across from one connector to another
				// since can only use relative cordinates here. See justifyTextToBeInsidePinList that handle move to different connector
				if (oldSide != newSide) {
					if (loc.getX() == 0 && loc.getY() == 0) { // same as pin location, justification sets it apart
						setOppositeJustification(oldSide, newSide, pin, nt);
					}
					else {
						int horizSide = newSide & (ExtentHelper.SIDE_WEST | ExtentHelper.SIDE_EAST);
						int vertSide = newSide & (ExtentHelper.SIDE_NORTH | ExtentHelper.SIDE_SOUTH);
						int oldHorizSide = oldSide & (ExtentHelper.SIDE_WEST | ExtentHelper.SIDE_EAST);
						int oldVertSide = oldSide & (ExtentHelper.SIDE_NORTH | ExtentHelper.SIDE_SOUTH);
						if (horizSide > 0 && oldHorizSide > 0) { // move to opposite side, mirror the offsets
							ILocation loc1 = nt.getLocation();
							loc1.setLocation(txtholder.getLocation().getX() * -1, txtholder.getLocation().getY());
							nt.setLocation(loc1);
							setOppositeJustification(oldSide, newSide, pin, nt);
						}
						if (vertSide > 0 && oldVertSide > 0) { // move to opposite side , mirror the offsets
							ILocation loc1 = nt.getLocation();
							loc1.setLocation(txtholder.getLocation().getX(), txtholder.getLocation().getY() * -1);
							nt.setLocation(loc1);
							setOppositeJustification(oldSide, newSide, pin, nt);
						}
						//
						// To reorient info so it matches original orientations.
						//
						if (vertSide > 0 && oldHorizSide > 0) {
							ILocation loc1 = nt.getLocation();
							if (vertSide == ExtentHelper.SIDE_SOUTH && oldHorizSide == ExtentHelper.SIDE_WEST) {
								loc1.setLocation(-txtholder.getLocation().getY(), txtholder.getLocation().getX());
							}
							else if (vertSide == ExtentHelper.SIDE_NORTH &&
									oldHorizSide == ExtentHelper.SIDE_EAST) {
								loc1.setLocation(-txtholder.getLocation().getY(), txtholder.getLocation().getX());
							}
							else {
								loc1.setLocation(-txtholder.getLocation().getY(), -txtholder.getLocation().getX());
								// dts0100685007 rejection - set the horizontal justification here
								nt.setHorizontalJustification(
										getOppositeHorizJustification(nt.getHorizontalJustification()));
							}
							nt.setRotation(nt.getRotation() - 90);
							nt.setLocation(loc1);
						}
						if (horizSide > 0 && oldVertSide > 0) {
							ILocation loc1 = nt.getLocation();
							if (horizSide == ExtentHelper.SIDE_EAST && oldVertSide == ExtentHelper.SIDE_SOUTH) {
								loc1.setLocation(-txtholder.getLocation().getY(), -txtholder.getLocation().getX());
								nt.setHorizontalJustification(
										getOppositeHorizJustification(nt.getHorizontalJustification()));
							}
							else if (horizSide == ExtentHelper.SIDE_WEST &&
									oldVertSide == ExtentHelper.SIDE_NORTH) {
								loc1.setLocation(-txtholder.getLocation().getY(), -txtholder.getLocation().getX());
								nt.setHorizontalJustification(
										getOppositeHorizJustification(nt.getHorizontalJustification()));
							}
							else {
								loc1.setLocation(txtholder.getLocation().getY(), -txtholder.getLocation().getX());
							}
							nt.setRotation(nt.getRotation() + 90);
							nt.setLocation(loc1);
						}
					}
				}
			}
			else {
				// Note:: Text location is relative to the pin. The justification is relative to location.
				// Pin was being MOVED not SWAPPED!
				ILocation loc = nt.getLocation();
				loc.setX(0);
				loc.setY(0);
				nt.setLocation(loc);

				setOppositeJustification(oldSide, newSide, pin, nt);
			}
		}
	}

	protected void setOppositeJustification(int oldSide, int newSide, IAbstractSchemPin pin, IAttributeText nt)
	{
		// Handle relative sides
		if (oldSide == newSide) {
			return;
		}

		// Ignore moves to adjacent sides
		TransformHelper th = TransformHelper.getTransformInfo((ICompoundObject) pin.getParent());
		int pinlistRotation = th.getRotation();
		int textRotation = nt.getRotation();
		int normRot = Math.abs(textRotation - pinlistRotation);

		int oldHorizSide = oldSide & (ExtentHelper.SIDE_WEST | ExtentHelper.SIDE_EAST);
		int newHorizSide = newSide & (ExtentHelper.SIDE_WEST | ExtentHelper.SIDE_EAST);
		if (oldHorizSide > 0 && newHorizSide > 0) {
			if (normRot == 90) {
				nt.setVerticalJustification(getOppositeVertJustification(nt.getVerticalJustification()));
			}
			else {
				nt.setHorizontalJustification(getOppositeHorizJustification(nt.getHorizontalJustification()));
			}
		}

		int newVertSide = newSide & (ExtentHelper.SIDE_NORTH | ExtentHelper.SIDE_SOUTH);
		int oldVertSide = oldSide & (ExtentHelper.SIDE_NORTH | ExtentHelper.SIDE_SOUTH);
		if (newVertSide > 0 && oldVertSide > 0) {
			if (normRot == 270 || normRot == 90) {
				nt.setHorizontalJustification(getOppositeHorizJustification(nt.getHorizontalJustification()));
			}
			else {
				nt.setVerticalJustification(getOppositeVertJustification(nt.getVerticalJustification()));
			}
		}
	}

	private HorizJustificationEnum getOppositeHorizJustification(HorizJustificationEnum justification)
	{
		if (justification == HorizJustificationEnum.JustLeft) {
			return HorizJustificationEnum.JustRight;
		}
		if (justification == HorizJustificationEnum.JustRight) {
			return HorizJustificationEnum.JustLeft;
		}
		return justification;
	}

	private VertJustificationEnum getOppositeVertJustification(VertJustificationEnum justification)
	{
		if (justification == VertJustificationEnum.JustTop) {
			return VertJustificationEnum.JustBottom;
		}
		if (justification == VertJustificationEnum.JustBottom) {
			return VertJustificationEnum.JustTop;
		}
		return justification;
	}
}