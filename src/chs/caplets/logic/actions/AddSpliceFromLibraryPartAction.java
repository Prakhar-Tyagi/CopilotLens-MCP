/*
 * Copyright 2005-2008 Mentor Graphics Corporation
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
import chs.caf.caplet.action.IActionEnum;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.parts.ILibraryCavity;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.ILibrarySplice;
import chs.cof.parts.ILibrarySolderSleeve;
import chs.cof.parts.ILibraryUltrasonicWeld;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utility.DiagramHelper;
import chs.utility.logic.LogicUtils;

import java.awt.event.ActionEvent;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA. User: hebae Date: Sep 7, 2005 Time: 11:26:20 AM To change this template use File | Settings
 * | File Templates.
 */
public class AddSpliceFromLibraryPartAction extends AddDeviceFromLibraryPartAction
{

	public AddSpliceFromLibraryPartAction(ICapletController controller)
	{
		super(controller);
	}

	/**
	 * Return a filter for splice parts only.
	 */
	protected ILibraryObject.GroupType getPartFilter()
	{
		return ILibraryObject.GroupType.SPLICE;
	}

	/**
	 * Activates the sub-action for adding a library part without a symbol to create a parameterized device and prompt the
	 * user for pin positions.
	 */
	protected IActionEnum activateAddWithoutSymbol(ActionEvent e, ILibraryPartSelection libraryPart)
	{
		ILibraryObject libObj = libraryPart.getSelectedObject();
		// TODO jacobt FEAT 3099.1 : Should configure the PSD to only allow single pinned splice without symbol
		// PSD would actually have to allow all splices with symbol and only single pinned splices without symbol
		int numCavities = libObj.getNumCavities();
		if (numCavities != 1 && libObj instanceof ILibrarySplice) {
			String heading = ResourceMgr.getString(getClass(),
					"AddSpliceFromLibraryPartActionUI.error.cannotAddSpliceFromPart",
					libraryPart.getSelectedObject().getPartNumber());
			String key = numCavities == 0 ?
					"AddSpliceFromLibraryPartActionUI.error.spliceWithoutSymbolNoPins" :
					"AddSpliceFromLibraryPartActionUI.error.spliceWithoutSymbolMultPins";
			String reason = ResourceMgr.getString(getClass(), key);

			MessageHelper.showErrorMessage(CAFUtils.getInstance().getDialogFrame(), heading, reason);
			return IActionEnum.eCanceled;
		}

		CreateSpliceAction action = new AddSpliceFromLibraryPartWithoutSymbolAction(getController(), libraryPart);
		subAction = action; // must set subAction before activating it so events are forwarded
		return action.onActivate(e);
	}

	/**
	 * This action is only needed when we add a splice from library part without a symbol.
	 * <p/>
	 * We have to derive from the CreateSpliceAction because we need to update the library part of the splice during the
	 * onTerminate method.
	 */
	private static class AddSpliceFromLibraryPartWithoutSymbolAction extends CreateSpliceAction
	{

		private ILibraryPartSelection libraryPart;

		AddSpliceFromLibraryPartWithoutSymbolAction(ICapletController controller, ILibraryPartSelection libObj)
		{
			super(controller);
			libraryPart = libObj;
		}

		public boolean onTerminate(boolean successful)
		{
			boolean status = super.onTerminate(successful);
			if (status && successful) {
				changePinName(); // change the pin name from the default name to the library cavity name (before update part)
				ISchemDiagram diagram = DiagramHelper.getDiagram(m_schemSplice);
				SelectedPartUpdateHelper.updateLibraryPart(m_schemSplice, libraryPart, diagram);
			}
			return status;
		}

		/**
		 * Change the newly-created splice pin name to the name of the library cavity
		 */
		private void changePinName()
		{
			ILibraryObject libraryObject = libraryPart.getSelectedObject();
			if (libraryObject instanceof ILibrarySplice) {
				ILibrarySplice librarySplice = (ILibrarySplice) libraryObject;
				IPinList cableSplice = m_schemSplice.getConnectivity();

				// actually we should only currently get here for single pinned splice,
				// but this should work in future if we ever do >1 pin
				boolean changed = false;
				Iterator<ILibraryCavity> cavIt = librarySplice.getCavities().iterator();
				IAbstractPinIterator pinIt = cableSplice.getPins();
				while (pinIt.hasNext() && cavIt.hasNext()) {
					IAbstractPin pin = pinIt.next();
					ILibraryCavity cavity = cavIt.next();
					pin.setName(cavity.getName());
					LogicUtils.setMatchingShortDescriptionFromOTI(pin, pin.getProject());
					changed = true;
				}
				assert changed : "Single-pinned splice should be added from single-pinned splice part";
			}
			else if (!(libraryObject instanceof ILibrarySolderSleeve ||
					libraryObject instanceof ILibraryUltrasonicWeld)) {
				assert false : "Library splice should be added";
			}
		}
	}

	public String getActionUIClass()
	{
		return AddSpliceFromLibraryPartActionUI.class.getName();
	}
}
