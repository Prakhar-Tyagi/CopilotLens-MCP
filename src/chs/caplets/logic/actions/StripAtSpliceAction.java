/*
 * Copyright 2003-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.cof.drawplus.IAttributeText;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.drawplus.IJoint;
import chs.cof.drawplus.IPropText;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.IHighwayConductor;
import chs.cof.logical.cable.IPhysicalConductor;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemFactory;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.common.ICommonFactory;
import chs.common.IUIDObject;
import chs.common.styles.IStyleableDiagram;
import chs.common.styles.IStyleableObject;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utility.DiagramHelper;
import chs.utility.attr.AttributeUtils;
import chs.utility.helpers.ConductorHelper;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.HighwayHelper;
import chs.utility.helpers.NodeHelper;
import chs.utility.helpers.SchemConductorHelper;
import chs.utility.helpers.SegmentHelper;
import chs.utility.helpers.TextHelper;
import chs.utility.preferences.PreferenceSetHelper;
import chs.utility.preferences.StyleSetUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class StripAtSpliceAction extends ControllerActionRT implements ICtxMenuProvider
{

	private MyOperands m_operands;
	private static final Cursor m_cursor = CAFUtils.getInstance().loadCursor(Cursor.DEFAULT_CURSOR);

	public StripAtSpliceAction(ICapletController controller)
	{
		super(controller);
		m_operands = null;
	}

	//
	// ActionRT implementation
	//

	protected IActionEnum onActivate(ActionEvent e)
	{
		m_operands = getOperands(getController().getSelectMgr().getCurrentSelections());
		if (m_operands != null) {
			return IActionEnum.eCompleted;
		}
		else {
			return IActionEnum.eCanceled;
		}
	}

	protected boolean onTerminate(boolean successful)
	{
		if (successful) {
			return stripAtSplice();
		}
		return false;
	}

	//
	// IAction implementation
	//

	public String getActionUIClass()
	{
		return StripAtSpliceActionUI.class.getName();
	}

	public Cursor getCursor()
	{
		return m_cursor;
	}

	public String getStatusbarText()
	{
		return "Convert two wires terminating at a splice to one wire, center-stripped at the splice";
	}

	public boolean isEnabled()
	{
		return getOperands(getController().getSelectMgr().getPreSelections()) != null && super.isEnabled();
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{/* Do nothing */}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		m_operands = getOperands(selections);
		if (m_operands != null) {
			if (m_operands.getPinList().getConnectivity() instanceof ISplice) {
				ISplice splice = (ISplice) m_operands.getPinList().getConnectivity();
				if (splice.getSymbolRef() != null) {
					return;
				}
			}
			container.add(new ActionEntry(getActionUI()));
		}
	}

	//
	// Helper methods
	//

	/**
	 * Goes through the selection set, validating it and picking out our operands at the same time.
	 *
	 * @param sset The selection set
	 * @return The operands for this action class.
	 */
	@Nullable private MyOperands getOperands(SelectSet sset)
	{

		MyOperands operands = new MyOperands();
		IPin splicePin = null;
		// A valid selection must have two non-shared wires, and may have one splice - nothing else is allowed.
		for (SelectedUIDObjectIterator iter = sset.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject uidObj = iter.getNext();
			if (uidObj instanceof ISegment || uidObj instanceof IConductor) {
				IConductor cond;
				if (uidObj instanceof ISegment) {
					cond = ((ISegment) uidObj).getConductor();
				}
				else {
					cond = (IConductor) uidObj;
				}

				if (cond.getConnectivity() instanceof IWireConductor && !cond.getConnectivity().isShared()) {
					if (cond == operands.getCond1() || cond == operands.getCond2()) {
						continue;
					}
					if (operands.getCond1() == null) {
						operands.setCond1(cond);
						continue;
					}
					if (operands.getCond2() == null) {
						operands.setCond2(cond);
						continue;
					}
				}
			}
			else if (uidObj instanceof IWireConductor || uidObj instanceof ISplice) {
				// ignore connectivity wire or splice selection - they're probably from the selected objects in the browser (dts0100541341)
				continue;
			}
			else if (uidObj instanceof IPinList
					&& ((IConnectivityRef) uidObj).getConnectivity() instanceof ISplice
					&& operands.getPinList() == null) {
				operands.setPinList((IPinList) uidObj);
				continue;
			}
			else if (uidObj instanceof IPin && splicePin == null) {
				splicePin = (IPin) uidObj;
				continue;
			}
			else if (AttributeUtils.isNameText(uidObj) || uidObj instanceof IPropText) {
				// Ignore any selected name or property text
				continue;
			}
			// Found too many splices or wires, or something besides a wire or splice.
			return null;
		}

		if (operands.getCond1() == null || operands.getCond2() == null) {
			// Didn't find two wires.
			return null;
		}

		if (splicePin != null && (operands.getPinList() == null || splicePin.getParent() != operands.getPinList())) {
			// The only pin that can be selected is the one belonging to the splice
			return null;
		}

		// Verify that the two conductors terminate at the same splice.

		Collection<IPinList> splices1 = new HashSet<IPinList>();

		// Find splices at which the first wire terminates.
		List<IJoint> terminalNodes = SchemConductorHelper.getTerminalNodes(operands.getCond1());
		for (IJoint node : terminalNodes) {
			IPin pin = NodeHelper.getPin(node);
			if (pin != null && ((IPinList) pin.getParent()).getConnectivity() instanceof ISplice) {
				splices1.add((IPinList) pin.getParent());
			}
		}

		// Find splices at which the second wire terminates, and try to match the first wire's splices.
		terminalNodes = SchemConductorHelper.getTerminalNodes(operands.getCond2());
		IJoint commonNode = null;
		IPinList commonSplice = null;
		for (IJoint node : terminalNodes) {
			IPin pin = NodeHelper.getPin(node);
			if (pin != null && ((IPinList) pin.getParent()).getConnectivity() instanceof ISplice) {
				if (splices1.contains(pin.getParent())) {
					if (commonSplice == null) {
						commonSplice = (IPinList) pin.getParent();
						commonNode = node;
					}
					else {
						// Silly boundary condition - Wires have more than one splice in common. This can
						// only mean that both wires are terminated at both ends on the same two splices.
						if (operands.getPinList() != null) {
							// Resolve the conflict by the user-specified splice.
							if (pin.getParent() == operands.getPinList()) {
								commonSplice = operands.getPinList();
								commonNode = node;
							}
						}
						else {
							// Can't resolve.
							return null;
						}
					}
				}
			}
		}

		if (commonSplice == null) {
			// Wires do not have a splice in common.
			return null;
		}

		if (operands.getPinList() == null) {
			operands.setPinList(commonSplice);
		}
		else if (commonSplice != operands.getPinList()) {
			// Splice found does not match user specified splice.
			return null;
		}

		operands.setNode(commonNode);

		// jyang - There is a bug in schem.Conductor.mergeConductor(..) and it is not possible to fix for 2005.1.
		// The below process will make it work.
		chs.cof.logical.cable.IConductor wire2 = operands.getCond2().getConnectivity();
		if (wire2.getMulticore() != null) {
			IConductor tempCond = operands.getCond1();
			operands.setCond1(operands.getCond2());
			operands.setCond2(tempCond);
		}

		if (!operands.isValid()) {
			return null;
		}
		return operands;
	}

	private boolean stripAtSplice()
	{

		ICommonFactory commFact = FactoryMgr.getCommonFactory();
		ISchemFactory schemFact = FactoryMgr.getSchemFactory();

		// Find the segments that belong to our two wires in the node.
		ISegment seg1 = null;
		ISegment seg2 = null;
		final IConductor tgtSchem = m_operands.getCond1();
		final IConductor srcSchem = m_operands.getCond2();
		for (IDiagramObjectIterator iter = m_operands.getNode().getAssociations(); iter.hasNext(); ) {
			IDiagramObject diagObj = iter.getNext();
			if (diagObj instanceof ISegment) {
				ISegment seg = (ISegment) diagObj;
				if (seg.getConductor() == tgtSchem) {
					seg1 = seg;
				}
				else if (seg.getConductor() == srcSchem) {
					seg2 = seg;
				}
			}
		}

		if (seg1 == null || seg2 == null) {
			return false;
		}

		chs.cof.logical.cable.IConductor removedWire = srcSchem.getConnectivity();

		ConductorHelper.PhysicalConductorHandler physicalConductorHandler = removedWire instanceof IPhysicalConductor ?
				new ConductorHelper.PhysicalConductorHandler((IPhysicalConductor) removedWire) : null;
		// Merge the wires.
		Point nodePoint = new Point(m_operands.getNode().getX(), m_operands.getNode().getY());
		SegmentHelper.connectSegment(commFact, schemFact, seg2, seg1, nodePoint);

		m_operands.setNode(seg1.getConnectedJoint(seg2));

		// Get the connectivity
		IWireConductor wire1 = (IWireConductor) tgtSchem.getConnectivity();
		IPin pin = NodeHelper.getPin(m_operands.getNode());
		IAbstractPin cPin = pin.getConnectivity();
		ISplice splice = (ISplice) m_operands.getPinList().getConnectivity();

		// Disconnect the merged wire from the splice's pin.
		cPin.removeConductor(wire1);
		wire1.removePin(cPin);

		if (physicalConductorHandler != null) {
			physicalConductorHandler.updateWireEndDetails(wire1);
		}
		// Hook the wire to the splice as center-stripped.
		splice.addCenterStrippedWire(wire1);
		wire1.addCenterStripSplice(splice);

		// Merges two wires
		mergeStrippedWires(m_operands.getCond1().getConnectivity(), removedWire);
		//srcSchem.mergeConductor(tgtSchem);

		// Update name text in merged wire.
		IAttributeText oldNameText = TextHelper.findSegmentContainerNameText(tgtSchem);
		TextHelper.removeSegmentContainerNameText(tgtSchem, true);
		TextHelper.addSegmentContainerNameText(tgtSchem, oldNameText, DiagramHelper.getBaseDiagram(tgtSchem));

		if (wire1 != removedWire) {
			CreationDeletionHelper.getTheCreationHelper().addDeletionObject(removedWire);
		}

		// Update composite texts for the selected objects
		// Introduced in 2010.2.SP1107 - dts0100766928 - the Harness is not graphically reflected in the composite wire name and it is required to performing “Apply Style"
		Set<IStyleableObject> diagObjs = new LinkedHashSet<IStyleableObject>(4);
		diagObjs.add(tgtSchem);
		diagObjs.add(srcSchem);
		diagObjs.add(m_operands.getPinList());
		diagObjs.add(pin);
		StyleSetUtils.updateCompositeTexts(diagObjs);
		IStyleableDiagram diagram = CommonUtils.cast(DiagramHelper.getBaseDiagram(tgtSchem), IStyleableDiagram.class);
		if (diagram != null) {
			PreferenceSetHelper.applyStyleSet(diagObjs, diagram, true);
		}


		return true;
	}

	/**
	 * Merges stripped wires and updates all the design wide wires present in the design
	 *
	 * @param srcCond  Source wire
	 * @param destWire Destination wire
	 */
	private void mergeStrippedWires(chs.cof.logical.cable.IConductor destWire, chs.cof.logical.cable.IConductor srcCond)
	{
		IDesign design = srcCond.getLogicDesign();

		// Merge two cable wires
		ConductorHelper.mergeCableConductors(destWire, srcCond);

		//Process DW wires present in all diagrams to update its connectivity and properties
		if (design != null) {
			IDesignWideUsageMgr sharedMgr = ((ILogicDesign) design).getDesignWideUsageMgr();
			Set<ISchemDiagram> usageDiagrams = new HashSet<ISchemDiagram>();
			for (IDesignSharedUsage designSharedUSage : sharedMgr.getUsages(srcCond)) {
				usageDiagrams.add(designSharedUSage.getDiagram());
			}
			for (ISchemDiagram diagram : usageDiagrams) {
				IDiagramObjectIterator representations = diagram.getRepresentations(srcCond.getUID());
				while (representations.hasNext()) {
					IDiagramObject repObject = representations.getNext();
					if (repObject instanceof IConductor) {
						IConductor conductorTobeMerged = (IConductor) repObject;
						// Sets the schem conductor to refer to new cable conductor
						ConductorHelper.mergeSchemToCableConductor(conductorTobeMerged, destWire);

						// Update the highway connectivity
						if (srcCond instanceof IHighwayConductor &&
								destWire instanceof IHighwayConductor) {
							Set<IHighwaySchematic> highwaySchems =
									HighwayHelper.getSchematicHighways(conductorTobeMerged);
							for (IHighwaySchematic highwaySchem : highwaySchems) {
								IGeneralHighway generalHighway = HighwayHelper.toGeneralHighway(highwaySchem);
								if (generalHighway != null) {
									generalHighway.removeConductor((IHighwayConductor) srcCond);
									generalHighway.addConductor((IHighwayConductor) destWire);
								}
							}
						}
					}
				}
			}
		}
	}

	private static class MyOperands
	{

		boolean isValid()
		{
			if (getCond1() == null || getCond2() == null || getPinList() == null) {
				return false;
			}

			chs.cof.logical.cable.IPinList cablePinlist = getPinList().getConnectivity();
			ILogicDesign design = getPinList().getConnectivity().getLogicDesign();
			assert design != null;
			IDesignWideUsageMgr dwum = design.getDesignWideUsageMgr();

			// StripAtSpliceAction is enabled for design wires as well (dts0100715768 )
			return dwum.getDesignSharedUsageCount(cablePinlist) == 1;
		}

		private IConductor cond1 = null;
		private IConductor cond2 = null;
		private IPinList pinList = null;
		private IJoint node = null;

		public IPinList getPinList()
		{
			return pinList;
		}

		public void setPinList(IPinList pinList)
		{
			this.pinList = pinList;
		}

		public IConductor getCond1()
		{
			return cond1;
		}

		public void setCond1(IConductor cond1)
		{
			this.cond1 = cond1;
		}

		public IConductor getCond2()
		{
			return cond2;
		}

		public void setCond2(IConductor cond2)
		{
			this.cond2 = cond2;
		}

		public IJoint getNode()
		{
			return node;
		}

		public void setNode(IJoint node)
		{
			this.node = node;
		}
	}
}
