/*
 * Copyright 2003-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.caplet.selection.ISelectListener;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caf.caplet.selection.SelectEvent;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.schem.ISegment;
import chs.images.CHSImageLoader;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utility.DiagramHelper;

import javax.swing.Icon;
import java.util.Collection;

/**
 * Description of the Class
 *
 * @author Darin Jackson
 * @created August 1, 2001
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_NOTCH_ACTION",
		label = "Notch",
		tooltip = "Notch",
		icon = "notch",
		buttonStyle = "MEDIUM_IMAGE_AND_TEXT")
public class RerouteSegmentActionUI extends ActionUI implements ISelectListener
{

	/**
	 * Constructor for the CreateCircleActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public RerouteSegmentActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_wire_active.gif");
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_N);

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getString(RerouteSegmentActionUI.class, "RerouteSegmentActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(RerouteSegmentActionUI.class, "RerouteSegmentActionUI.name.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(RerouteSegmentActionUI.class, "RerouteSegmentActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);

		// Add ourselves as a select listener on the AppActionMgr so
		// we can update our UI when selection states change.
		getCaplet().getFIB().getAppActionMgr().addSelectListener(this, true);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_wire_inactive.gif");
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return RerouteSegmentAction.class.getName();
	}

	public void selectionChanged(SelectEvent e)
	{
		ISelectMgr activeSM = CAFUtils.getInstance().getActiveSelectMgr();
		setEnabled(isEnabled(activeSM));
	}

	public static boolean isEnabled(ISelectMgr selectMgr)
	{
		boolean enabled = false;

		if (selectMgr != null) {
			Collection<ISegment> segmentsSelected = selectMgr.getPreSelections().getSelectedObjects(ISegment.class);
			if (segmentsSelected.size() != 1) {
				return false;
			}
			ISegment seg = segmentsSelected.iterator().next();

			if (ActionRT.isDesignUnderConcurrentEdit()) {
				IBaseDiagram diagram = DiagramHelper.getDiagram(seg);
				IBaseDiagram activeDiagram = FactoryMgr.getCAFUtils().getActiveDiagram();
				if (activeDiagram != diagram) {
					return false;
				}
			}

			boolean vertical = (seg.getStartPoint().getX() == seg.getEndPoint().getX());
			boolean horizontal = (seg.getStartPoint().getY() == seg.getEndPoint().getY());
			// Breaks if the segment isn't orthogonal

			enabled = vertical || horizontal;
		}

		return enabled;
	}
}
