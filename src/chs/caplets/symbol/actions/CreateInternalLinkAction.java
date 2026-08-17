/*
 * Copyright 2009 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.actions;

import chs.caf.AbstractContextAction;
import chs.caf.CAFUtils;
import chs.caf.IApplicationSpecificationAction;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.IModelChangeListener;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.helpers.graphics.CreatePolylineAction;
import chs.caplets.symbol.Model;
import chs.cof.draw.HorizJustificationEnum;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGrid;
import chs.cof.draw.IGriddable;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IDiagramText;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IInternalLink;
import chs.cof.logical.schem.IInternalLinkPolyline;
import chs.cof.logical.schem.ISchemInternalLink;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.common.ILocation;
import chs.common.IProjectPreferenceMgr;
import chs.common.attr.IAttributeTypes;
import chs.services.dynamicgfx.IDynamicSnap;
import chs.services.dynamicgfx.ISmartPoint;
import chs.system.FactoryMgr;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utility.SymbolUtils;
import chs.utility.helpers.TextHelper;
import chs.utility.logic.ISymbolModel;

import javax.swing.AbstractAction;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class CreateInternalLinkAction extends CreatePolylineAction implements IModelChangeListener
{

	private Model m_model;
	private List<IInternalLinkPolyline> m_connectingObjects;
	private String m_linkType;

	/**
	 * Constructor for the CreateLineAction object
	 *
	 * @param controller Description of the Parameter
	 */
	public CreateInternalLinkAction(ICapletController controller, String linktype)
	{
		super(controller, true, false);
		m_model = (Model) controller.getCapletModel();
		m_linkType = linktype;
		m_model.addModelChangeListener(this);
	}

	/**
	 * Creates new connectivity and schematic objects, associates them with each other, and finally adds them to the
	 * owning device.
	 *
	 * @param point_list The points representing the start, end, and grip points.
	 *
	 * @return
	 */
	protected IGfxObject constructDisplayObject(List<ISmartPoint> point_list)
	{
		List<Point> points = new ArrayList<Point>(point_list.size());
		for (ISmartPoint spt : point_list) {
			points.add(spt.getAbsoluteLocation());
		}

		IInternalLink internalLinkConnectivity =
				FactoryMgr.getCableFactory().createInternalLink(FactoryMgr.createUID());
		((IDevice) ((ISymbolDef) ((ISymbolModel) getController().getCapletModel()).getSymbolDef()).getConnectivity())
				.addInternalLink(internalLinkConnectivity);
		internalLinkConnectivity.setLinktype(m_linkType);

		IStamp stamp = m_model.getSymbolDef();
		internalLinkConnectivity.setNameMgr(stamp.getNameMgr());

		ISchemInternalLink internalLink = FactoryMgr.getSchemFactory()
				.constructSchemInternalLink(FactoryMgr.createUID(), internalLinkConnectivity, points);
		getModel().getSheet().addObject(internalLink);
		//
		// Now we have the pin added, add the name text.
		//
		IGrid m_grid = ((IGriddable) m_model.getSheet()).getGrid();
		IDiagramText nameText = FactoryMgr.getDrawPlusFactory().constructAttributeText(
				FactoryMgr.getCommonFactory().createUID(), internalLinkConnectivity,
				TextHelper.getDefaultHeight(m_grid),
				0, 0, 0,
				IAttributeTypes.NAME);
		IProjectPreferenceMgr preferences = CAFUtils.getInstance().getCurrentProjectPreferences();
		final IBaseDiagram diagram = getBaseDiagram();
		if (preferences != null && diagram != null) {
			TextHelper.assignAttributeTextDefaults(nameText, diagram, m_grid, preferences);
		}
		else {
			nameText.setFont(TextHelper.getDefaultFont());
		}

		IInternalLinkPolyline poly = internalLink.getDelegate();
		poly.addObject(nameText);

		Point midpnt = poly.getMidPointOffset();
		ILocation loc = nameText.getLocation();
		loc.setX((int) midpnt.getX());
		loc.setY((int) midpnt.getY());
		nameText.setLocation(loc);
		nameText.setHorizontalJustification(HorizJustificationEnum.JustMiddle);

		m_connectingObjects = Arrays.asList(poly);

		return internalLink;
	}

	protected Collection connectingObjects()
	{
		return m_connectingObjects != null ? m_connectingObjects : Collections.EMPTY_LIST;
	}

	protected Class snappingSource()
	{
		return IInternalLinkPolyline.class;
	}

	/**
	 * Diable the action if less than two pins existed in the diagram.
	 *
	 * @return true to enable the action.
	 */
	public boolean isEnabled()
	{
		if (!super.isEnabled()) {
			return false;
		}

		IStamp stamp = m_model.getSymbolDef();
		if (stamp instanceof ISymbolDef) {
			// links can be added to blocks only
			if (!(SymbolUtils.isDeviceSymbol((ISymbolDef) stamp))) {
				return false;
			}
		}

		//MGI-FEAT00014409--connectivity.getPinCollection() gives only non-internal pins
		//IPinList connectivity = ((ISymbolDef) m_model.getSymbolDef()).getConnectivity();
		//return connectivity != null && connectivity.getPinCollection().size() >= 2;
		ISymbolDef symdef = ((ISymbolDef) m_model.getSymbolDef());
		return symdef != null && symdef.getNumInternalPins() + symdef.getNumPins() >= 2;
	}

	/**
	 * Use the disable commit action.
	 *
	 * @return
	 */
	protected AbstractAction commitAction()
	{
		return new ContextAbstractAction(this);
	}

	/**
	 * Terminate the action on double click and atleast there are two snap points (pins)
	 *
	 * @param e
	 */
	public void mouseClicked(MouseEvent e)
	{
		if (m_snapHelper.getNumSnapped() > 1 && e.getClickCount() == 2 && isValidEndPoint()) {
			getController().getActionMgr().terminateActiveAction(true);
		}
	}

	private boolean isValidEndPoint()
	{
		//EndPoint is valid if it is a pin
		if (m_snapHelper.getLastSnapped() != null) {
			return true;
		}
		//There can be a case where a pin is already snapped and clicked some where else and then came back to terminate at this pin
		//In that case, m_snapHelper.getLastSnapped() is null (don't know why -- checked for net in CLogic, same behavior)
		//Hence, extend the check..Check if the current snap point location is already present in the list of snapped objects
		//Again, make sure not to check with first snapped object...Link cannot terminate at start pin
		Collection<Pair<IDynamicSnap, Integer>> snapped = m_snapHelper.getAllSnapped();
		int cur_x = (int) this.m_current_point.getAbsoluteLocation().getX();
		int cur_y = (int) this.m_current_point.getAbsoluteLocation().getY();
		boolean bFirst = true;
		for (Pair<IDynamicSnap, Integer> snappedPin : snapped) {
			if (bFirst) {
				bFirst = false;
				continue;
			}
			IDynamicSnap snap = snappedPin.getFirst();
			int x = snap.getLocation().getX();
			int y = snap.getLocation().getY();
			if (cur_x == x && cur_y == y) {
				return true;    // This snap location corresponds to one of the already snapped object..So, link can safely terminate here.
			}
		}
		return false;
	}

	/**
	 * Overriden to prevent starting drawing except at a pin. Also, terminates the action upon double clicking a pin.
	 *
	 * @param e
	 */
	public void mousePressed(MouseEvent e)
	{
		super.mousePressed(e);
		if (m_snapHelper.getLastSnapped() == null && m_snapHelper.getNumSnapped() == 0) {
			backup();
		}
		//Commenting the below code --> Allow termination on double click
		//if (m_snapHelper.getLastSnapped() != null && m_snapHelper.getNumSnapped() > 1 && e.getClickCount() == 2) {
		//	getController().getActionMgr().terminateActiveAction(true);
		//}
	}

	public void modelPreChanged(ModelChangeEvent e)
	{

	}

	public void modelChanged(ModelChangeEvent e)
	{
		this.getActionUI().setEnabled(this.isEnabled());
	}

	private class ContextAbstractAction extends AbstractContextAction
	{

		protected ContextAbstractAction(IApplicationSpecificationAction parent)
		{
			super(parent, ResourceMgr.getString(CreatePolylineAction.class, "CreatePolylineAction.commit.action.name"));
		}

		public void actionPerformed(ActionEvent ae)
		{
		}

		/**
		 * Overriden to disable the commit action menu since we don't want to terminate in any place other than a pin.
		 *
		 * @return
		 */
		public boolean isEnabled()
		{
			return false;
		}
	}
}
