/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2004-2025 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caplets.logic.icd.ICDPlacementHelper;
import chs.cof.logical.schem.IPinList;
import chs.cof.parts.ILibraryDevice;
import chs.cof.parts.ILibraryGraphic;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.ISymbolRef;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utility.helpers.LibraryHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

/**
 * Adds an instance (with a symbol) from a library part (with a symbol).
 */
public class AddLibraryPartWithSymbolAction extends AddInstanceAction
{

	@NotNull private ILibraryPartSelection librarySelection;

	private static final String WarnIncompleteTitle = ResourceMgr.getString(AddLibraryPartWithSymbolAction.class,
			"AddLibraryPartWithSymbolAction.warning.incomplete.title");
	private static final String WarnIncompleteHdr = ResourceMgr.getString(AddLibraryPartWithSymbolAction.class,
			"AddLibraryPartWithSymbolAction.warning.incomplete.heading");
	private static final String WarnIncompleteMsg = ResourceMgr.getString(AddLibraryPartWithSymbolAction.class,
			"AddLibraryPartWithSymbolAction.warning.incomplete.message");

	/**
	 * Construct the action.
	 *
	 * @param controller The controller
	 * @param part The library part.  Must have a symbol.
	 */
	public AddLibraryPartWithSymbolAction(ICapletController controller, @NotNull ILibraryPartSelection part)
	{
		super(controller);
		librarySelection = part;
		assert librarySelection.getSelectedSymbol() != null;
	}

	/**
	 * The symbol is obtained from the library part passed on construction
	 */
	protected IStamp acquireSymbol()
	{
		ISymbolRef symRef = null;
		ILibraryGraphic librarySymbol = librarySelection.getSelectedSymbol();
		if (librarySymbol != null) {
			symRef = LibraryHelper.getLogicalSymbol(librarySymbol);
		}
		IStamp symbol = CAFUtils.getInstance().getCHSSystem().getSymbolLibraryMgr().getReferencedSymbol(symRef);
		if (((ISymbolDef) symbol).getPinList() == null) {
			MessageHelper.showWarningMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
					WarnIncompleteTitle, WarnIncompleteHdr, WarnIncompleteMsg);
			return null;
		}

		// if the stamp is a composite symbol, a block may also be specified on the action (e.g. via a dialog)
		if (!setupBlock(symbol)) {
			symbol = null; // e.g. user cancelled block select dialog
		}

		return symbol;
	}

	/**
	 * Override here to update the library part once the instance is added.
	 */
	public boolean onTerminate(boolean successful)
	{
		//SP1310: dts0100957700
		if (m_pinlist == null) {
			bCreatePinList = true;
			updateTransients();
			bCreatePinList = false;
		}
		IPinList createdPL = m_pinlist;
		boolean result = super.onTerminate(successful);
		if (result) {
			Runnable updatePart = () -> {
				ILibraryObject libObj = librarySelection.getSelectedObject();
				if (libObj != null && libObj instanceof ILibraryDevice) {
					LibraryHelper.eagerLoadLibraryDeviceFullyIncludeFpConnectorsAndItsMatedConnectors(
							Collections.singleton((ILibraryDevice) libObj));
				}
				SelectedPartUpdateHelper
						.updateLibraryPart(createdPL, librarySelection, getModel().getDiagram(), true, true);
				ICDPlacementHelper.updateICDNameAndRouting(createdPL, librarySelection, getModel().getDiagram(), false);
			};
			splitConductors(createdPL, updatePart);
		}
		return result;
	}

	protected boolean shouldGenerateDeviceConnectors()
	{
		return false;
	}

	protected boolean shouldSplitConductorsWhilePlacingInstance()
	{
		return false;
	}

	@Override public boolean shouldDisableUndoForNonUndoableChanges()
	{
		return true;
	}
}
