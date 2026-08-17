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
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGfxObjectIterator;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.drawplus.IJoint;
import chs.cof.drawplus.IPropText;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemFactory;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.common.ICommonFactory;
import chs.common.IUIDObject;
import chs.system.FactoryMgr;
import chs.utility.attr.AttributeUtils;
import chs.utility.helpers.NodeHelper;
import chs.utility.helpers.SegmentHelper;
import chs.utility.helpers.SegmentHelperInfo;

import java.awt.Point;
import java.awt.event.ActionEvent;

public class TerminateAtSpliceAction extends ControllerActionRT implements ICtxMenuProvider
{

	private MyOperands m_operands;

	public TerminateAtSpliceAction(ICapletController controller)
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
			successful = terminateAtSplice();
		}
		return successful;
	}

	//
	// IAction implementation
	//

	public String getActionUIClass()
	{
		return TerminateAtSpliceActionUI.class.getName();
	}

	public boolean isEnabled()
	{
		return getOperands(getController().getSelectMgr().getPreSelections()) != null && super.isEnabled();
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (getOperands(selections) != null) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	protected MyOperands getOperands(SelectSet sset)
	{
		MyOperands operands = new MyOperands();

		// Go through the selection, validating it and picking out our operands at the same time.
		// A valid selection must have one non-shared wire, and one splice - nothing more, nothing less.

		IPin splicePin = null;
		for (SelectedUIDObjectIterator iter = sset.getSelectedUIDObjects(); iter.hasNext();) {
			IUIDObject uidObj = iter.getNext();
			if (uidObj instanceof ISegment || uidObj instanceof IConductor) {
				IConductor cond;
				if (uidObj instanceof ISegment) {
					if (operands.segment == null) {
						operands.segment = (ISegment) uidObj;
					}
					cond = ((ISegment) uidObj).getConductor();
				}
				else {
					cond = (IConductor) uidObj;
				}

				if (cond.getConnectivity() instanceof IWireConductor && !cond.getConnectivity().isShared()) {
					if (operands.cond == null) {
						operands.cond = cond;
						continue;
					}
					else if (cond == operands.cond) {
						continue;
					}
				}
			}
			else if (uidObj instanceof IWireConductor || uidObj instanceof ISplice) {
				// ignore connectivity wire or splice selection - they're probably from the selected objects in the browser (dts0100541341)
				continue;
			}
			else if (uidObj instanceof IPinList) {
				if (((IPinList) uidObj).getConnectivity() instanceof ISplice) {
					if (operands.pinList == null) {
						operands.pinList = (IPinList) uidObj;
						continue;
					}
				}
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

		if (operands.segment == null || operands.cond == null || operands.pinList == null) {
			// Didn't find a wire and a splice.
			return null;
		}

		if (splicePin != null && (operands.pinList == null || splicePin.getParent() != operands.pinList)) {
			// The only pin that can be selected is the one belonging to the splice
			return null;
		}

		IWireConductor wire = (IWireConductor) operands.cond.getConnectivity();
		ISplice splice = (ISplice) operands.pinList.getConnectivity();
		if (wire.findCenterStripSpliceById(splice.getUID()) == null) {
			// Selected conductor is not connected to selected splice - selection is invalid.
			return null;
		}

		if (!operands.isValid()) {
			return null;
		}
		return operands;
	}

	private boolean terminateAtSplice()
	{
		ICommonFactory commFact = FactoryMgr.getCommonFactory();
		ISchemFactory schemFact = FactoryMgr.getSchemFactory();

		// Get the schem pin through the schem pinlist, then get the node through the pin.
		IJoint spliceNode = null;
		IPin pin = null;
		for (IGfxObjectIterator gfxObjs = m_operands.pinList.getObjects(); gfxObjs.hasNext();) {
			IGfxObject gfxObj = gfxObjs.getNext();
			if (gfxObj instanceof IPin) {
				pin = (IPin) gfxObj;
				spliceNode = pin.getJoint();
			}
		}

		if (pin == null || spliceNode == null) {
			return false;
		}

		ISegment segmentA = null;
		ISegment segmentB = null;
		// Find the segments that belong to our wire in the node.
		for (IDiagramObjectIterator iter = spliceNode.getAssociations(); iter.hasNext();) {
			IDiagramObject diagObj = iter.getNext();
			if (diagObj instanceof ISegment) {
				ISegment seg = (ISegment) diagObj;
				if (seg.getConductor() == m_operands.cond) {
					if (segmentA == null) {
						segmentA = seg;
					}
					else if (segmentB == null) {
						segmentB = seg;
					}
				}
			}
			if (segmentA != null && segmentB != null) {
				break;
			}
		}

		if (segmentA == null || segmentB == null) {
			return false;
		}

		// We want the segment on the side of the splice opposite to the first segment selected.
		ISegment segment;
		int pathLengthA = SegmentHelper.nodePath(segmentA, m_operands.segment).size();
		int pathLengthB = SegmentHelper.nodePath(segmentB, m_operands.segment).size();
		if (pathLengthA > pathLengthB) {
			segment = segmentA;
		}
		else if (pathLengthB > pathLengthA) {
			segment = segmentB;
		}
		else {
			return false;
		}

		// Detach the segments, create a new conductor.
		Point nodePoint = new Point(spliceNode.getX(), spliceNode.getY());
		SegmentHelperInfo shInfo = SegmentHelper.splitSegment(commFact, schemFact, segment, nodePoint);

		IWireConductor newWire = null;
		if (shInfo.getSegment() != null) {
			// dts0100608202 Make sure conductor is split so that a new conductor is created
			segment.getConductor().setSplit(true);
			segment.getConductor().makeContinuousKeepSegment(shInfo.getSegment());
			newWire = (IWireConductor) segment.getConductor().getConnectivity();
			// Merge the nodes back together
			NodeHelper
					.getMergedNode(commFact, schemFact, shInfo.getNode1(), shInfo.getNode2(), nodePoint.x, nodePoint.y);
		}

		// Change the connectivity
		IWireConductor wire = (IWireConductor) m_operands.cond.getConnectivity();
		IAbstractPin cPin = pin.getConnectivity();
		ISplice splice = (ISplice) m_operands.pinList.getConnectivity();

		// Disconnect the conductor from the splice.
		splice.removeCenterStrippedWire(wire);
		wire.removeCenterStripSplice(splice);

		// Connect the wires to the splice's pin.
		cPin.addConductor(wire);
		if (newWire != null) {
			cPin.addConductor(newWire);
		}
		wire.addPin(cPin);

		if (newWire != null) {
			newWire.addPin(cPin);
		}

		return true;
	}

	private class MyOperands
	{

		boolean isValid()
		{
			if (cond == null || pinList == null) {
				return false;
			}

			// TODO jacobt FEAT13040 : this action is currently disabled for multiple design wide instances of any operand
			chs.cof.logical.cable.IConductor cableCond = cond.getConnectivity();
			chs.cof.logical.cable.IPinList cablePinlist = pinList.getConnectivity();
			ILogicDesign design = pinList.getConnectivity().getLogicDesign();
			assert design != null;
			IDesignWideUsageMgr dwum = design.getDesignWideUsageMgr();
			//noinspection RedundantIfStatement
			if (dwum.getDesignSharedUsageCount(cableCond) != 1 || dwum.getDesignSharedUsageCount(cablePinlist) != 1) {
				return false;
			}
			return true;
		}

		public IConductor cond = null;
		public ISegment segment = null;
		public IPinList pinList = null;
	}
}
