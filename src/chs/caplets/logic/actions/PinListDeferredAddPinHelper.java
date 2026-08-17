package chs.caplets.logic.actions;

import chs.cof.draw.IGrid;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.ICommonFactory;
import chs.common.IExtent;
import chs.common.IParameterized;
import chs.system.FactoryMgr;
import chs.utilities.CHSConstants;
import chs.utility.helpers.CompositePinConnectivityFinder;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;

public class PinListDeferredAddPinHelper extends PinListAddPinHelper
{

	public PinListDeferredAddPinHelper(@NotNull IPinList pinlist, boolean isReference)
	{
		super(pinlist, isReference);
	}

	public void addPins(ISchemDiagram diagram, Collection<IAbstractPin> pins,
			CompositePinConnectivityFinder connectivityFinder)
	{
		IExtent ext = m_pinList.getExtent();
		int width = ext.getWidth();
		IGrid grid = diagram.getGrid();
		width = grid.snap(width);
		int pinspacing = grid.getGridSpacing();

		IParameterized params = m_pinList.getParameterized();
		if (params != null) {
			IExtent pExt = params.getExtent();
			pExt.setWidth(width);
		}

		final ICommonFactory commonFactory = FactoryMgr.getCommonFactory();

		int numPins = pins.size();

		int side = 0;
		int height = PinListAddPinHelper.adjustHeight(numPins, pinspacing, false, m_pinList);
		int y = height;
		int i = 1;
		Collection<IPin> newSchemPins = new ArrayList<IPin>(pins.size());
		for (IAbstractPin pin : pins) {
			newSchemPins.add(createPin(pin, new Point(side * width, y)));

			if ((side == 0) && (i >= numPins / 2)) {
				side = 1;
				y = height;
			}
			else {
				y -= pinspacing;
			}
			i++;
		}

		regeneratePinsOnly(m_pinList, diagram, m_isReference, newSchemPins);
		collectConnectionMakers(newSchemPins, connectivityFinder);
		//
		// This area is the extent of the box where the pins would go.
		//
		params = m_pinList.getParameterized();
		if (params != null) {
			int calcHeight = 0;
			if (numPins > 2) {
				calcHeight = ((numPins / 2) - 1) * CHSConstants.PIN_SPACING;
			}
			params.setExtent(commonFactory.constructExtent(0, 0, width, calcHeight));
		}
	}
}
