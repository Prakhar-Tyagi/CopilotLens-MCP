/*
 * Copyright 2006-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.logic.actions.tests;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caplets.logic.Model;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IGfxObject;
import chs.cof.drawplus.IGfxView;
import chs.cof.logical.schem.IConductor;
import chs.common.IUIDObject;
import chs.common.IUIDObjectIterator;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.utilities.BuildInfo;
import chs.utility.gfx.GfxObjectWalker;
import chs.utility.gfx.IGfxObjectVisitor;

import java.awt.event.ActionEvent;

public class HackVisibilityAction extends ControllerActionRT
{

	private Model m_model;
	private IDynamicGfxService m_dynamics;
	protected boolean strict;

	public HackVisibilityAction(ICapletController controller)
	{
		super(controller);
		m_model = (Model) controller.getCapletModel();
		m_dynamics = m_model.getDynamicGfxService();
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		strict = ((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0);
		return IActionEnum.eCompleted;
	}

	public boolean onTerminate(boolean successful)
	{
		IUIDObjectIterator sitr = CAFUtils.getInstance().getActiveSelectMgr().getCurrentSelections().getUIDObjects();
		if (sitr.getSize() > 0) {
			// Something is selected. Make all selected gfxobjects invisible. 
			while (sitr.hasNext()) {
				IUIDObject uidObj = sitr.getNext();
				if (uidObj instanceof IGfxObject && strict || !(uidObj instanceof IConductor)) {
					((IGfxObject) uidObj).setMarkedVisible(false);
				}
			}
		}
		else if (CAFUtils.getInstance().getActiveCapletView() instanceof IGfxView) {
			// Nothing is selected. Make everything on the diagram visible.
			IGfxView view = (IGfxView) CAFUtils.getInstance().getActiveCapletView();
			new GfxObjectWalker().visit(view.getDiagram(), new IGfxObjectVisitor()
			{
				public boolean visitCompoundObject(ICompoundObject compObj)
				{
					return true;
				}

				public boolean visitGfxObject(IGfxObject gfxObj)
				{
					gfxObj.setMarkedVisible(true);
					return false;
				}
			});
		}
		return true;
	}

	/**
	 * Return our matching ActionUI class
	 */
	public String getActionUIClass()
	{
		return HackVisibilityActionUI.class.getName();
	}

	public boolean isEnabled()
	{
		return BuildInfo.getBuildInfo().areDeveloperExtensionsEnabled() && super.isEnabled();
	}
}
