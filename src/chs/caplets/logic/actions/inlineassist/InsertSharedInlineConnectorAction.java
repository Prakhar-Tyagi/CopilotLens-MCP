/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2016-2023 Siemens
 */
package chs.caplets.logic.actions.inlineassist;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.action.IActionMgr;
import chs.caplets.logic.actions.shared.AddSharedInlineConnectorAction;
import chs.caplets.logic.actions.shared.AddSharedInlineConnectorActionUI;
import chs.caplets.logic.shared.ISharedPinListInfoProvider;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinReservationView;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.caf.utils.PinProxy;
import chs.services.dynamicgfx.ISmartPoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Extension of regular "InsertSharedConnectorAction" to allow it be used for creation of inline using points
 * and direction provided.
 */
public class InsertSharedInlineConnectorAction extends AddSharedInlineConnectorAction
		implements ISharedPinListInfoProvider
{

	private ISharedConnector targetConnector;
	private Collection<ISharedPin> pinsToUse;
	private boolean enabled = false;

	public InsertSharedInlineConnectorAction(ICapletController controller,
			ISpecialSelectMgr sharedSelectMgr)
	{
		super(controller, sharedSelectMgr);
	}

	public void createConnectorInstance(@NotNull NewConnectorData connectorData)
	{
		final InlineDirection direction = connectorData.getDirection();
		final InlineExtent extent = connectorData.getExtent();
		final List<ISmartPoint> points = extent.getPoints();
		m_point_list.clear();
		m_point_list.addAll(points);

		boolean isVertical = direction.isVertical();
		pinsVertical = !isVertical;
		rotationIndicator.setVertical(pinsVertical);
		rotationIndicator.setReversePinSide(direction.isReversedPinSide());

		createConnector(getPointList());
		Collection<IPinProxy> pinsToAdd = getSelectedSharedPins();
		boolean isReference = m_addPinListDialog.getReference();
		m_addPinActionHelper.setIsReference(isReference);
		m_addPinActionHelper.setPlaceAsStack(m_addPinListDialog.getPlaceAsStack());

		m_addPinActionHelper.setUp(m_schemPlug, pinsToAdd);

		final SharedInlinePinAdder pinAdder =
				new SharedInlinePinAdder(m_addPinActionHelper, extent, pinsVertical, direction.isReversedPinSide());
		pinAdder.invoke();
	}

	public void setPins(Collection<ISharedPin> pins)
	{
		pinsToUse = pins;
	}

	public void setSharedConnector(ISharedConnector sharedConnector)
	{
		targetConnector = sharedConnector;
	}

	@Override public boolean onTerminate(boolean successful)
	{
		boolean result = super.onTerminate(successful);
		enabled = false;
		return result;
	}

	@Override public boolean isEnabled()
	{
		return enabled;
	}

	public void enable()
	{
		enabled = true;
	}

	@NotNull public ISharedPinListInfoProvider getAddSharedPinListDialog(Frame owner, PinListTypeEnum pinlistType)
	{
		return this;
	}

	@Override
	public boolean selectPinList(ILogicDesign design, ISharedPinList spl, ISharedPinReservationView sharedpinview,
			@NotNull IPlacementOptionParams params)
	{
		return true;
	}

	@Override public boolean selectPinList(ILogicDesign design, PinListTypeEnum pltype, @NotNull IPlacementOptionParams params)
	{
		return true;
	}

	@Override public ISharedPinList getSharedPinList()
	{
		return targetConnector;
	}

	@Override public boolean getAutoGenerate()
	{
		return false;
	}

	@Override public int getNumUsedPins()
	{
		return 0;
	}

	@Override public boolean getReference()
	{
		return false;
	}

	@SuppressWarnings("rawtypes")
	@Override public Collection<IPinProxy> getUsedPins()
	{
		Collection<IPinProxy> pinProxies = new ArrayList<>(pinsToUse.size());
		for (ISharedPin sharedPin : pinsToUse) {
			pinProxies.add(new PinProxy(sharedPin));
		}
		return pinProxies;
	}

	@Override public void cleanUp()
	{

	}

	@Override public boolean getPlaceAsStack()
	{
		return false;
	}

	@Override public boolean getPlaceAsGroup()
	{
		return false;
	}

	@Override public boolean getWithConductor()
	{
		return false;
	}

	public void setCursor(@Nullable Cursor cursor)
	{

	}

	public void mouseClicked(MouseEvent e)
	{

	}

	public void mouseDragged(MouseEvent e)
	{

	}

	public void mouseEntered(MouseEvent e)
	{

	}

	public void mouseExited(MouseEvent e)
	{

	}

	public void mouseMoved(MouseEvent e)
	{

	}

	public void mousePressed(MouseEvent e)
	{

	}

	public void mouseReleased(MouseEvent e)
	{

	}

	public static class UI extends AddSharedInlineConnectorActionUI
	{

		private ICapletController capletController;

		public UI(ICapletController capletController)
		{
			super(capletController.getCaplet());
			this.capletController = capletController;
		}

		public String getActionClass()
		{
			return InsertSharedInlineConnectorAction.class.getName();
		}

		public IAction getAction()
		{
			return capletController.getAction(getActionName());
		}

		public void actionPerformed(ActionEvent e)
		{
			IAction action = getAction();
			IActionMgr actionMgr = capletController.getActionMgr();
			if (action != null && actionMgr != null) {
				actionMgr.actionPerformed(action, e);
			}
		}
	}
}
