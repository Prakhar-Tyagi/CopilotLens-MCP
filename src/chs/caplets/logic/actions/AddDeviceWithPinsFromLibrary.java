/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.BasicAddPinListDialog;
import chs.cof.draw.IGrid;
import chs.cof.icd.IDeviceICD;
import chs.cof.library.IFootprintable;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.Device;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IPinList;
import chs.cof.parts.ILibraryCavity;
import chs.cof.parts.ILibraryCavityContainer;
import chs.cof.parts.ILibraryDeviceFootprint;
import chs.cof.parts.ILibraryGraphic;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.partselector.IICDSelection;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.ISymbolRef;
import chs.cof.symbol.SymbolTypeEnum;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import chs.utility.Replicator;
import chs.utility.SymbolUtils;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.LibraryHelper;
import chs.utility.helpers.PropertyCopier;
import chs.utility.helpers.PropertyTemplateHelper;
import chs.utility.logic.ILogicModel;
import chs.utility.logic.LogicUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class AddDeviceWithPinsFromLibrary extends AddPinListAction
{

	@NotNull private ILibraryPartSelection part;
	private IPinList pinList;

	public AddDeviceWithPinsFromLibrary(ICapletController controller, @NotNull ILibraryPartSelection libraryPart)
	{
		super(controller);
		part = libraryPart;
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		return super.onActivate(e);
	}

	@NotNull @Override protected IPlacementOptionParams createPlacementOptionParams(@NotNull IPinList cablePinList,
			@Nullable ISymbolDef symDef)
	{
		IPlacementOptionParams params = super.createPlacementOptionParams(cablePinList, symDef);
		enableWithConductorOptionIfValid(params);
		return params;
	}

	private void enableWithConductorOptionIfValid(@NotNull IPlacementOptionParams params)
	{
		if (part instanceof IICDSelection) {
			params.enableWithConductorOption(true, getCurrentProject());
		}
	}

	@NotNull protected AddMultiSymbolledPinListDialog getAddMultiSymbolledPinListDialog(Frame frame,
			String title)
	{
		AddMultiSymbolledPinListDialog dialog = new AddMultiSymbolledPinListDialog(frame, title, true);
		dialog.setHelpID(AddParameterizedDeviceFromLibraryPartAction.PinSelectionDialog.class.getName());
		return dialog;
	}

	@Nullable
	@Override
	protected IPinList getOperand()
	{
		ISymbolRef symRef = null;
		ILibraryGraphic librarySymbol = part.getSelectedSymbol();
		if (librarySymbol != null) {
			symRef = LibraryHelper.getLogicalSymbol(librarySymbol);
		}
		if (symRef == null) {
			return null;
		}
		IStamp symbol = CAFUtils.getInstance().getCHSSystem().getSymbolLibraryMgr().getReferencedSymbol(symRef);
		Replicator replicator = new Replicator(Replicator.Mode.INSTANTIATE, true);
		double scale = getScale(symbol);
		chs.cof.logical.schem.IPinList schemPinList = replicator.replicate((ISymbolDef) symbol, scale);
		IGrid grid = getGrid();
		if (grid != null && !SymbolUtils.isUnitScale(scale)) {
			SymbolUtils.adjustOffGridPinsToAGridPoint(schemPinList, grid);
		}
		pinList = schemPinList.getConnectivity();
		PropertyTemplateHelper.AssociateAutoAssignProperties(pinList,
				CAFUtils.getInstance().getCurrentProject(), false);
		pinList.setSymbolRef(symRef);
		pinList.assignLibraryPart(part.getSelectedObject());
		IICDSelection icdSelection = CommonUtils.cast(part, IICDSelection.class);
		if (icdSelection != null) {
			IDeviceICD icd = icdSelection.getICD();
			if (icd != null) {
				pinList.setName(icd.getRole());
			}
		}
		IDesign design = getDesign();
		IConnectivity connectivity = design != null ? design.getConnectivity() : null;
		if (connectivity != null) {
			connectivity.addLogicObject(pinList);
		}
		ILibraryDeviceFootprint footprint = part.getSelectedFootprint();
		if (footprint != null && pinList instanceof IDevice) {
			((IFootprintable) pinList).setFootprintId(footprint.getUID());
		}
		ILibraryCavityContainer libraryCavityContainer = (ILibraryCavityContainer) part.getSelectedObject();

		FactoryMgr.getCHSSystem().getPartsLibrary().getLibraryBatchLoader().loadFully(Collections.singleton(libraryCavityContainer), false);
		Map<String, ILibraryCavity> cavityMap = new LinkedHashMap<>();

		if (libraryCavityContainer != null) {
			libraryCavityContainer.getAllCavities().stream()
					.forEach(aCavity -> cavityMap.put(aCavity.getName(), aCavity));
		}
		else {
			assert false : "Library cavity container is not supposed to be null";
		}

		Collection<IAbstractPin> notMatchedPins = new ArrayList<>();
		for (IAbstractPin devPin : pinList.getPins()) {
			ILibraryCavity libraryCavity = cavityMap.get(devPin.getName());
			if (libraryCavity != null) {
				//devPin.assignLibraryCavity(libraryCavity);
				PropertyCopier.copyCavityAttributesAndPropertiesOntoPin(devPin, libraryCavity);
				cavityMap.remove(libraryCavity.getName());
			}
			else {
				notMatchedPins.add(devPin);
			}
			LogicUtils.setMatchingShortDescriptionFromOTI(devPin, pinList.getProject());
		}
		Iterator<Map.Entry<String, ILibraryCavity>> entries = cavityMap.entrySet().iterator();
		if (notMatchedPins.size() == cavityMap.entrySet().size()) {
			for (IAbstractPin devPin : notMatchedPins) {
				Map.Entry<String, ILibraryCavity> entry = entries.next();
				//devPin.assignLibraryCavity(entry.getValue());
				PropertyCopier.copyCavityAttributesAndPropertiesOntoPin(devPin, entry.getValue());
				devPin.setName(entry.getKey());
			}
		}

		//pinList.deletePins();
		schemPinList.delete();
		CreationDeletionHelper.getTheCreationHelper().clearNewObjects();
		return pinList;
	}

	private double getScale(IStamp symbol)
	{
		IGrid grid = getGrid();
		if (symbol instanceof ISymbolDef && grid != null) {
			ISymbolDef symbolDef = (ISymbolDef) symbol;
			boolean shouldScale =
					symbolDef.getSymbolType() == SymbolTypeEnum.COMMENT || getController().getCaplet().isLayoutCaplet();
			if (shouldScale) {
				return SymbolUtils.getSymbolScale(symbolDef, grid);
			}
		}
		return 1;
	}

	@Nullable private IGrid getGrid()
	{
		ILogicModel model = getModel();
		return model != null ? model.getDiagram().getGrid() : null;
	}

	@Nullable
	private IDesign getDesign()
	{
		ILogicModel model = getModel();
		return model != null ? model.getDesign() : null;
	}

	@Nullable
	private ILogicModel getModel()
	{
		return CommonUtils.cast(getController().getCapletModel(), ILogicModel.class);
	}

	protected boolean onTerminate(boolean successful)
	{
		removeUnplacedPins(successful);
		return super.onTerminate(successful);
	}

	protected void cleanUp(boolean sucessful)
	{
		if (!sucessful) {
			IDesign design = getDesign();
			IConnectivity connectivity = design != null ? design.getConnectivity() : null;
			if (connectivity != null) {
				connectivity.removeLogicObject(pinList);
			}
			pinList.delete();
			UIDMgr.removeObject(pinList.getUID());
		}
		else {
			CreationDeletionHelper.getTheCreationHelper().addCreationObject(pinList);
			assignLibraryDetails();
		}
	}

	private void assignLibraryDetails()
	{
		ILibraryObject libraryObject = part.getSelectedObject();
		assert libraryObject != null;
		PropertyCopier.copyCavityAttributesAndProperties(pinList, libraryObject);
		pinList.assignLibraryDetails(part);
		ILibraryDeviceFootprint footprint = part.getSelectedFootprint();
		String footPrintDesc = footprint != null ? footprint.getFootprintName() : "";
		((IFootprintable) pinList).setFootprintDescription(footPrintDesc);
	}

	protected void removeUnplacedPins(boolean successful)
	{
		if (successful) {
			Set<IAbstractPin> pinNames = new LinkedHashSet<>();
			getSelectedPins().stream().forEach((proxy) -> {
				if (proxy != null) {
					pinNames.add(proxy.getCablePin());
				}
			});
			IAbstractPinIterator pinIterator = pinList.getPins();
			for (IAbstractPin pin : pinIterator) {
				if (!pinNames.contains(pin)) {
					((Device) pinList).removePinAndUpdateInterLinks(pin);
				}
			}
		}
	}

	protected void enableSymbolView(BasicAddPinListDialog m_symbolledPLDialog)
	{
		m_symbolledPLDialog.setEnablePinView(true);
	}
}
