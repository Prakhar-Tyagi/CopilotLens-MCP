/*
 * Copyright 2005-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.ActionEntry;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.helpers.MulticoreEditPanel;
import chs.cof.COFTypeEnum;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.shared.ISharedOverbraid;
import chs.common.IUIDObject;
import chs.common.IUIDObjectIterator;
import chs.system.FactoryMgr;
import chs.utility.logic.ILogicModel;

import javax.swing.Action;

public class EditSharedOverbraidAction extends CreateMulticoreAction
{

	public EditSharedOverbraidAction(ICapletController controller, ISpecialSelectMgr libSelectMgr)
	{
		super(controller);
		final ISpecialSelectMgr specialSelectMgr = libSelectMgr;
		if (getActionUI() != null) {
			specialSelectMgr.contextMenuAddAction(
					new ActionEntry(getActionUI(), (String) getActionUI().getValue(Action.SHORT_DESCRIPTION))
					{
						public boolean shouldDisplay()
						{
							// Check selections in the shared object browser
							if (specialSelectMgr.getSelectedObjects().getSize() == 1) {
								// A single overbraid is selected
								IUIDObject uidObj = specialSelectMgr.getSelectedObjects().getNext();
								if (uidObj instanceof ISharedOverbraid) {
									ILogicDesign design = ((ILogicModel) getController().getCapletModel()).getDesign();
									// Only show if the shared overbraid is used in this design.
									return design.getSharedUsageMgr().getMulticore((ISharedOverbraid) uidObj) != null;
								}
								// Just the shared browser folder is selected
								for (IUIDObjectIterator uitr = specialSelectMgr.getChildren(uidObj); uitr.hasNext();) {
									IUIDObject uObj = uitr.getNext();
									if (uObj instanceof ISharedOverbraid) {
										ILogicDesign design = ((ILogicModel) getController().getCapletModel()).getDesign();
										// Only show if the shared overbraid is used in this design.
										return design.getSharedUsageMgr().getMulticore((ISharedOverbraid) uObj) != null;
									}
								}
							}
							return false;
						}
					});
		}
		setEditType(COFTypeEnum.Overbraid);
		setEditScope(MulticoreEditPanel.SHARED_SCOPE);
	}

	public String getActionUIClass()
	{
		return EditSharedOverbraidActionUI.class.getName();
	}

	public boolean isEnabled()
	{
		//
		// If we are in a transaction boundary, we MUST wait
		//
		if (FactoryMgr.getSystemFactory().getCAFUtils().isWithinTransactionBoundary()) {
			return false;
		}
		return super.isEnabled();
	}

	@Override protected boolean checkCache()
	{
		return false;
	}
}
