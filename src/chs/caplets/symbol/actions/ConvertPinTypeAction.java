/*
 * Copyright 2010-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.actions;

import chs.cof.symbol.IPinTranstypingObserver;
import chs.cof.symbol.ISymbolDef;
import chs.cof.logical.schem.IInternalSchemPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.IInternalPin;
import chs.utility.logic.ISymbolModel;
import chs.utility.SymbolUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectionFilter;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.ICtxMenuProvider;
import chs.caf.AppAction;
import chs.caplets.symbol.DevicePinTranstypingObserver;
import chs.caplets.symbol.InternalPinTranstypingObserver;
import chs.common.IUIDObject;

import java.awt.event.ActionEvent;

public class ConvertPinTypeAction extends ControllerActionRT implements ICtxMenuProvider
{

	IPinTranstypingObserver m_pinObserver;
	ISymbolModel m_symModel;
	boolean bExternalToInternal = false;
	boolean bInternalToExternal = false;

	public ConvertPinTypeAction(ICapletController controller)
	{
		super(controller);
		m_symModel = (ISymbolModel) controller.getCapletModel();
	}

	public String getActionUIClass()
	{
		return ConvertPinTypeActionUI.class.getName();
	}

	public boolean onTerminate(boolean successful)
	{
		//FEAT00013673 - Connectivity in symbols
		//delegate to the pintranstyping observer to handle the conversion of the pins that
		// was registered to be converted from internal to device pins and vice versa
		if (m_pinObserver != null) {
			m_pinObserver.convertPins();
		}

		return true;
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		registerPinsObserverForPinTranstyping();

		return IActionEnum.eCompleted;
	}

	/**
	 * FEAT00013673 - Connectivity in symbols An observer should be initialized and registered in Internal and Device Pins
	 * if all the selections are either internal pins or device pins
	 */
	private void registerPinsObserverForPinTranstyping()
	{
		SelectSet selections = getController().getSelectMgr().getPreSelections();
		SelectionFilter schemIntPinsFilter = new SelectionFilter(IInternalSchemPin.class);
		SelectionFilter schemDevPinsFilter = new SelectionFilter(IPin.class);

		int selectionCount = selections.getSelectCount();
		if (selectionCount > 0) {
			SelectedUIDObjectIterator selectedObjIter = selections.getSelectedUIDObjects();
			if (selectionCount == selections.getSelectCount(schemDevPinsFilter)) {
				//all selections are device pins
				bExternalToInternal = true;
				m_pinObserver = new DevicePinTranstypingObserver(m_symModel);

				while (selectedObjIter.hasNext()) {
					IUIDObject obj = selectedObjIter.getNext();
					if (obj instanceof IPin) {
						IAbstractPin cablePin = ((IPin) obj).getConnectivity();
						if (cablePin instanceof IDevicePin) {
							m_pinObserver.addCableSchemPinPair(cablePin, (IPin) obj);
							m_pinObserver.addPinForTranstyping(cablePin);
						}
					}
				}
			}
			else if (selectionCount == selections.getSelectCount(schemIntPinsFilter)) {
				//all selections are internal pins
				m_pinObserver = new InternalPinTranstypingObserver(m_symModel);
				bInternalToExternal = true;
				while (selectedObjIter.hasNext()) {
					IUIDObject obj = selectedObjIter.getNext();
					if (obj instanceof IInternalSchemPin) {
						IInternalPin cablePin = ((IInternalSchemPin) obj).getConnectivity();
						m_pinObserver.addCableSchemPinPair(cablePin, (IInternalSchemPin) obj);
						m_pinObserver.addPinForTranstyping(cablePin);
					}
				}
			}
		}
	}

	//
	// Context Menu methods
	//
	public boolean isEnabled()
	{
		if (!isValidSymbolTypeForThisAction()) {
			return false;
		}
		SelectSet selections = getController().getSelectMgr().getPreSelections();
		SelectionFilter schemIntPinsFilter = new SelectionFilter(IInternalSchemPin.class);
		SelectionFilter schemDevPinsFilter = new SelectionFilter(IPin.class);
		bInternalToExternal = false;
		bExternalToInternal = false;

		int selectionCount = selections.getSelectCount();
		if (selectionCount > 0) {
			if (selectionCount == selections.getSelectCount(schemDevPinsFilter)) {
				//all selections are device pins
				bExternalToInternal = true;
			}
			else if (selectionCount == selections.getSelectCount(schemIntPinsFilter)) {
				//all selections are internal pins
				bInternalToExternal = true;
			}
			if (bExternalToInternal || bInternalToExternal) {
				return super.isEnabled();
			}
		}
		return false;
	}

	protected boolean isValidSymbolTypeForThisAction()
	{
		if (m_symModel.getSymbolDef() instanceof ISymbolDef) {
			ISymbolDef symDef = (ISymbolDef) m_symModel.getSymbolDef();
			if (SymbolUtils.isDeviceSymbol(symDef)) {
				return true;
			}
		}
		return false;
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		// Ask the client if the selections are either pin or internalPin
		if (isEnabled()) {
			AppAction aui = (AppAction) getActionUI();
			if (bInternalToExternal) {
				aui.setResources("name1.decl", false, "shortDesc1.decl", "name1.longDesc", null, null,
						"chs/images/app/ico_pin_active.gif");
			}
			if (bExternalToInternal) {
				aui.setResources("name2.decl", false, "shortDesc2.decl", "name2.longDesc", null, null,
						"chs/images/app/ico_internalpin.png");
			}
			container.add(new ActionEntry(aui));
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
		// nothing to do here.
	}
}
