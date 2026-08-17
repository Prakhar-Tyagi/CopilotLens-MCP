/*
 * Copyright 2002-2012 Mentor Graphics Corporation
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
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caplets.logic.Model;
import chs.cof.draw.ICircle;
import chs.cof.draw.IGrid;
import chs.cof.logical.IECAttributeResolver;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cofUtils.parameterized.AddSpliceHelper;
import chs.cofUtils.parameterized.Generator;
import chs.common.ILocation;
import chs.common.preferencesets.IPreferenceSet;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utility.ConductorSplitter;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.logic.LogicUtils;
import chs.utility.preferences.PreferenceSetHelper;
import chs.view.assist.IPinInfo;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CreateSpliceAction extends ControllerActionRT implements MouseListener, MouseMotionListener
{

	private static Cursor m_spliceCursor = null;

	protected Model m_model;
	protected IGrid m_grid;
	private IDynamicGfxService m_dynamics;
	private Point m_currValidPoint = null;
	private ICircle m_feedback;
	protected IPinList m_schemSplice;
	protected ISplice m_cableSplice;
	private List<IDynamicGfx> m_transientGfxs = new ArrayList<>();

	public CreateSpliceAction(ICapletController controller)
	{
		super(controller);
		m_model = (Model) controller.getCapletModel();
		m_dynamics = m_model.getDynamicGfxService();
		if (m_spliceCursor == null) {
			m_spliceCursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/cur_splice.gif", new Point(7, 7));
		}
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		//
		// Clear the old dynamics.
		//
		ISchemDiagram diagram = m_model.getDiagram();
		m_grid = diagram.getGrid();
		m_cableSplice = null;
		setupTransientGraphics();
		//
		return IActionEnum.eActivated;
	}

	/**
	 * Set the point directly to allow access via unit tests
	 *
	 * @param pt The point at which to add the splice
	 */
	public void setCurValidPoint(Point pt)
	{
		m_currValidPoint = pt;
	}

	protected void setupTransientGraphics()
	{
		//
		// Add the graphics for the symbol.
		//
		m_dynamics.removeAllDynamicGfx();
		m_dynamics.removeAllTransientGfx();
		//
		// If you're wondering why it's a circle, ask Greg!
		//
		int gp = m_grid.getGridSpacing() / 2;
		m_feedback = FactoryMgr.getDrawFactory().constructCircle(0, 0, gp);
		m_dynamics.addTransientGfx(m_feedback);
	}

	protected void cleanupTransientGraphics()
	{
		//
		// Clear the old dynamics.
		//
		m_dynamics.removeAllDynamicGfx();
		m_dynamics.removeAllTransientGfx();
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eFull);
		}
	}

	public boolean onTerminate(boolean successful)
	{
		if (successful) {
			ISchemDiagram diag = m_model.getDiagram();
			IPreferenceSet styleSet = PreferenceSetHelper.getStyleSet(diag);

			//
			// Build up a splice, and add it.
			//

			m_schemSplice = AddSpliceHelper.generateSplice(m_cableSplice,
					m_grid, null, m_currValidPoint.x,
					m_currValidPoint.y);

			if (m_cableSplice == null) {
				m_cableSplice = (ISplice) m_schemSplice.getConnectivity();
			}

			ILogicDesign logicDesign = m_model.getDesign();
			IConnectivity conn = logicDesign.getConnectivity();
			assert conn != null;
			conn.addSplice(m_cableSplice);
			//
			diag.addObject(m_schemSplice);
			setShortDescriptionOnSplicePins();
			IDesignWideUsageMgr usageMgr = logicDesign.getDesignWideUsageMgr();
			boolean isHome = usageMgr.getUsages(m_cableSplice).size() < 2;
			m_schemSplice.setHome(isHome);

			GfxView gview = (GfxView) CAFUtils.getInstance().getActiveCapletView();
			ConductorSplitter spliceSplitter = ConductorSplitter.createConductorSplitter(m_schemSplice);
			spliceSplitter.splitConductors(m_schemSplice, gview, false, true, false, () -> {});

			CreationDeletionHelper.getTheCreationHelper().addCreationObject(m_schemSplice);
			ObjectConnectionsGetter.createConnectionSchematics(m_schemSplice, diag);
			IECAttributeResolver.inheritIECAttributesIfNotPresent(diag, m_schemSplice);
			PreferenceSetHelper.applyStyleSet(m_schemSplice.getObjectsForStyling(), styleSet, true);
			Generator.generateSecondaryRepresentation(m_schemSplice, diag);
		}
		cleanupTransientGraphics();
		return true;
	}

	private void setShortDescriptionOnSplicePins()
	{
		m_cableSplice.getPins().stream().map(pin -> CommonUtils.cast(pin, IGenericPin.class)).filter(Objects::nonNull)
				.forEach(pin -> LogicUtils.setMatchingShortDescriptionFromOTI(pin, pin.getProject()));
	}

	/**
	 * Return our matching ActionUI class
	 */
	public String getActionUIClass()
	{
		return CreateSpliceActionUI.class.getName();
	}

	public boolean isEnabled()
	{
		return getController().getCapletModel().isEditable() && super.isEnabled();
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
	}

	public void mouseDragged(MouseEvent e)
	{
	}

	public void mouseClicked(MouseEvent e)
	{
		//
		// Commit it, and finish up here
		//
		if (m_currValidPoint == null) {   //SP1310: ExtendedFix for dts0100957700
			m_currValidPoint = CAFUtils.getInstance().getWorldPoint(e.getPoint(), e.getSource());
			m_currValidPoint.setLocation(m_grid.snap(m_currValidPoint.x), m_grid.snap(m_currValidPoint.y));
		}
		getController().getActionMgr().terminateActiveAction(true);
	}

	public void mouseMoved(MouseEvent e)
	{
		//
		// Keep the location around...
		//
		m_currValidPoint = CAFUtils.getInstance().getWorldPoint(e.getPoint(), e.getSource());
		m_currValidPoint.setLocation(m_grid.snap(m_currValidPoint.x), m_grid.snap(m_currValidPoint.y));
		m_feedback.getLocation().setX(m_currValidPoint.x);
		m_feedback.getLocation().setY(m_currValidPoint.y);
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eTransient);
		}

		addConnectionTransientGraphics();
	}

	private void addConnectionTransientGraphics()
	{
		removeConnectionTransientGraphics();

		final int snappedX = m_grid.snap(m_currValidPoint.x);
		final int snappedY = m_grid.snap(m_currValidPoint.y);

		ISplice cableSplice = getCableSplice();
		if (cableSplice != null) {
			final IAbstractPin splicePin = cableSplice.getPin();
			IPinInfo pinInfo = new IPinInfo()
			{
				@Override public ILocation getAbsLocation(String pinName)
				{
					return FactoryMgr.getCommonFactory().constructLocation(snappedX, snappedY);
				}

				@Nullable @Override public IAbstractPin getCablePin(String pinName)
				{
					return splicePin;
				}

				@Nullable @Override public IAbstractSchemPin getOriginatingSchemPin(String pinName)
				{
					return null;
				}
			};

			if (splicePin != null) {
				ObjectConnectionsGetter
						.createTransientGraphics(Collections.singleton(splicePin.getName()), m_model.getDiagram(),
								pinInfo, m_dynamics, m_transientGfxs);
			}
		}
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(CreateSpliceAction.class, "CreateSpliceAction.Statusbar.text");
	}

	/**
	 * Return the cursor for this action
	 */
	public Cursor getCursor()
	{
		return m_spliceCursor;
	}

	protected ISplice getCableSplice()
	{
		return m_cableSplice;
	}

	private void removeConnectionTransientGraphics()
	{

		for (IDynamicGfx transientGfx : m_transientGfxs) {
			m_dynamics.removeTransientGfx(transientGfx);
		}
		m_transientGfxs.clear();
	}
}