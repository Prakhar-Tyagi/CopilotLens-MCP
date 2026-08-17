/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.IOutputWindow;
import chs.caplets.logic.actions.ghc.GenerateHarnessConnActionHelper;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.AbstractPinIterator;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IPrivilegedDevicePin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.parts.ILibraryCavity;
import chs.cof.parts.ILibraryDevice;
import chs.cof.parts.ILibraryDeviceFootprint;
import chs.cof.parts.ILibraryDevicePin;
import chs.cof.parts.ILibraryGraphic;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.LibraryDevicePinPropertiesLoader;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.project.IProject;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.ISymbolRef;
import chs.cofUtils.parameterized.DefaultGeneratorDCFeedback;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.common.IAnalysable;
import chs.common.IBasePreferencesKeys;
import chs.common.IReadOnlyNamedObject;
import chs.common.PreferenceContext;
import chs.ctf.caf.ui.LibraryPinMapperDialog;
import chs.ctf.caf.ui.ReplaceSymbolPinMapperDialog;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.caf.utils.LibraryPinMapProvider;
import chs.ctf.caf.utils.PinMapProviderFactory;
import chs.ctf.caf.utils.PinMapper;
import chs.ctf.caf.utils.PinMappingInfoHelper;
import chs.ctf.editui.helpers.PinListUtils;
import chs.utilities.CollectionUtils;
import chs.utility.DiagramHelper;
import chs.utility.SymbolUtils;
import chs.utility.helpers.LibraryHelper;
import chs.utility.helpers.NamedObjectComparator;
import chs.utility.helpers.PinListHelper;
import chs.utility.helpers.PropertyCopier;
import chs.utility.preferences.PreferenceSetHelper;
import org.jetbrains.annotations.NotNull;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SelectedPartUpdateHelper
{

	private SelectedPartUpdateHelper()
	{
	}

	/**
	 * This method is called to ensure that we enforce the selected library object's cavity names if a symbol has been
	 * applied to the library object.
	 *
	 * @param pl, the pin list to be placed
	 * @param libSel, the selected library object
	 */
	private static void enforceLibraryPinNames(IPinList pl, ILibraryPartSelection libSel, boolean autoMapPins)
	{

		ILibraryObject libObj = libSel.getSelectedObject();
		assert libObj != null;

		ILibraryGraphic sym = libSel.getSelectedSymbol();
		ISymbolRef symRef = SymbolUtils.getLogicalSymbol(sym);

		if (symRef != null) {
			IStamp stamp = CAFUtils.getInstance().getCHSSystem().getSymbolLibraryMgr().getReferencedSymbol(symRef);
			if (stamp instanceof ISymbolDef) {
				ISymbolDef symbol = (ISymbolDef) stamp;

				boolean displayDeviceConnCavityName = false;
				chs.cof.logical.cable.IPinList cablePinList = pl.getConnectivity();
				if (cablePinList instanceof IDevice) {
					ILibraryDeviceFootprint diviceFootPrint = libSel.getSelectedFootprint();
					if (diviceFootPrint != null && diviceFootPrint.getFootprintType() ==
							ILibraryDeviceFootprint.FootprintType.DEVICE_CONNECTOR) {
						ISchemDiagram diagram = DiagramHelper.getDiagram(pl);
						if (diagram != null) {
							ILogicDesign design = diagram.getDesign();
							if (design != null) {
								IProject project = design.getProject();
								if (project != null) {
									final PreferenceContext context = PreferenceContext.determineContext(design);
									int dpn = project.getPreferences().getDeviceDispPinName(context);
									if (dpn == IBasePreferencesKeys.DSPDEVCONNPINNAME) {
										displayDeviceConnCavityName = true;
									}
								}
							}
						}
					}
				}

				Map<IReadOnlyNamedObject, IPinProxy> map = null;
				chs.cof.logical.cable.IPinList connectivity = symbol.getConnectivity();
				if (!autoMapPins) {
					LibraryPinMapProvider pinMapProvider = PinMapProviderFactory.instance()
							.createLibraryPinMapperProvider(libObj, connectivity, libSel.getSelectedFootprint());
					PinMapper pm = new PinMapper(pinMapProvider, libObj.getPartNumber(),
							connectivity.getName(), libSel.getSelectedFootprint(),
							displayDeviceConnCavityName);

					// show the dialog to allow the user to specify the mapping.
					PinMappingInfoHelper infoHelper =
							ReplaceSymbolPinMapperDialog.createSymbolPinMappingInfoHelper(pl, "");
					Frame owner = CAFUtils.getInstance().getWindowMgr().getDialogFrame();
					map = LibraryPinMapperDialog.promptPinMapping(owner, pm, infoHelper, false);
				}

				// if the user does not map the pins or cancels the dialog we must automatically map
				// the pins.
				if (map == null || map.isEmpty()) {
					List<IAbstractPin> symConnectivityPins = CollectionUtils.createList(connectivity.getPins());
					for (IBlock blk : symbol.getBlocks()) {
						symConnectivityPins.addAll(CollectionUtils.createList(blk.getConnectivity().getPins()));
					}
					map = mapPins(libObj, symConnectivityPins);
				}

				for (IReadOnlyNamedObject name : map.keySet()) {
					IPinProxy proxy = map.get(name);
					String pName = proxy.getName();
					for (IAbstractPin plPin : pl.getConnectivity().getPins()) {
						if (pName.equals(plPin.getName())) {
							plPin.setName(name.getName());
							break;
						}
					}
				}
			}
		}
	}

	@NotNull public static Map<IReadOnlyNamedObject, IPinProxy> mapPins(ILibraryObject libObj,
			@NotNull List<IAbstractPin> symConnectivityPins)
	{
		//dts0100905414 : we need to have pins in sorted order by name.
		//to have consistent result along with other flows.
        Collections.sort(symConnectivityPins, NamedObjectComparator.caseInsensitiveComparator());
        Collection<IPinProxy> pinProxiesCollection =
				PinMapper.createPinProxies(new AbstractPinIterator(symConnectivityPins));
		//getCavities returns cavities in sorted order by SortOrder defined in library part.
		Collection<IReadOnlyNamedObject> cavs =
				PinMapper.createSourceProxies(LibraryHelper.getCavities(libObj).iterator());

		List<IPinProxy> pinProxiesList = new ArrayList<IPinProxy>(pinProxiesCollection);
		List<IReadOnlyNamedObject> cavsList = new ArrayList<IReadOnlyNamedObject>(cavs);
		Map<IReadOnlyNamedObject, IPinProxy> map = PinListUtils.generateMapping(cavsList, pinProxiesList);

		if (map == null || map.isEmpty()) {
			Iterator<IPinProxy> symPins = pinProxiesCollection.iterator();
			map = new HashMap<IReadOnlyNamedObject, IPinProxy>();
			for (IReadOnlyNamedObject cav : cavs) {
				if (symPins.hasNext()) {
					map.put(cav, symPins.next());
				}
			}
		}
		return map;
	}

	/**
	 * Utility to update a pinlist with a library part.
	 *
	 * @param pinlist The pinlist
	 * @param librarySelection The library part
	 * @param diagram The diagram needed to update some properties
	 */
	public static void updateLibraryPart(IPinList pinlist, ILibraryPartSelection librarySelection,
			ISchemDiagram diagram)
	{
		updateLibraryPart(pinlist, librarySelection, diagram, false);
	}

	public static void updateLibraryPart(IPinList pinlist, ILibraryPartSelection librarySelection,
			ISchemDiagram diagram, boolean autoMapPins)
	{
		updateLibraryPart(pinlist, librarySelection, diagram, autoMapPins, false);
	}

	public static void updateLibraryPart(IPinList pinlist, ILibraryPartSelection librarySelection,
			ISchemDiagram diagram, boolean autoMapPins, boolean autoGHCOnAllInstances)
	{
		chs.cof.logical.cable.IPinList pl = pinlist.getConnectivity();

		ILibraryObject libObj = librarySelection.getSelectedObject();

		pl.assignLibraryDetails(librarySelection);

		enforceLibraryPinNames(pinlist, librarySelection, autoMapPins);

		PropertyCopier.copyAllAsReferencedProperties(pl, libObj); // Copy All, and Make them referenced

		//CS-4733 AddLibraryPartWithSymbolAction: Retrieving library cavity info in loop
		LibraryDevicePinPropertiesLoader loader = new LibraryDevicePinPropertiesLoader();
		loader.loadPinProperties(LibraryHelper.getCavities(libObj));
		PropertyCopier.copyCavityAttributesAndProperties(pl, libObj);

		// copy the analysis model to the instance if the library object has one
		// we test to check the library object's model as if it doesn't
		// have an analysis attachment we don't want to override the symbol's
		if (libObj != null && !isAnalysisModelBlank(libObj)) {
			pl.setAnalysisModel(libObj.getAnalysisModel());

			// now iterate over the cavities transferring A.M. to pins...
			for (Object cavity : LibraryHelper.getCavities(libObj)) {
				ILibraryCavity lcav = (ILibraryCavity) cavity;
				String cavName = lcav.getName();
				IAbstractPinIterator pins = pl.getPins();
				while (pins.hasNext()) {
					IAbstractPin pin = pins.getNext();
					if (pin.getName().equals(cavName)) {
						pin.setAnalysisModel(lcav.getAnalysisModel());
						break;
					}
				}
			}
		}

		if (pl instanceof IDevice) {
			for (Object cavity : LibraryHelper.getCavities(libObj)) {
				ILibraryCavity lcav = (ILibraryCavity) cavity;
				String cavName = lcav.getName();
				IAbstractPinIterator pins = pl.getPins();
				while (pins.hasNext()) {
					IAbstractPin pin = pins.getNext();
					if (pin.getName().equals(cavName) && pin instanceof IPrivilegedDevicePin &&
							lcav instanceof ILibraryDevicePin) {
						((IPrivilegedDevicePin) pin).setStud(((ILibraryDevicePin) lcav).getStud().toBoolean());
						break;
					}
				}
			}
		}

		GeneratorParameters gp = DiagramHelper.createGeneratorParameters(diagram);
		if (pl instanceof IDevice) {
			IDevice dev = (IDevice) pl;
			ILibraryDeviceFootprint footprint = librarySelection.getSelectedFootprint();
			if (footprint != null) {
				dev.setFootprintDescription(footprint.getFootprintName());
				dev.setFootprintId(footprint.getUID());
			}

			Generator generator = Generator.getGenerator();
			//FEAT12331 - Auto create harness connectors, using GenerateHarnessConnActionHelper
			GenerateHarnessConnActionHelper generateHarnessConnActionHelper =
					new GenerateHarnessConnActionHelper(diagram);

			final IOutputWindow output = CAFUtils.getInstance().getOutputWindow();

			DefaultGeneratorDCFeedback feedback = new DefaultGeneratorDCFeedback()
			{
				public void outputMessage(String s, boolean writtenSomething)
				{
					output.sendMessage(s, getOutputTabName(), writtenSomething);
				}
			};

			// Syles get applied indirectly when the device connectors are rebuilt, but only for
			// the device connectors
			generator.rebuildDeviceConnectors(pinlist, gp, feedback, true, Generator.REGENERATE_PROPERTIES);

			if (PinListHelper.isHarnessFootprintedAndAllowAutoCreation(pinlist, diagram)) {
				if (autoGHCOnAllInstances) {
					generateHarnessConnActionHelper.generateHarnessConnectorsForDevice((IDevice) pl);
				}
				else {
					generateHarnessConnActionHelper.generateHarnessConnectorsForPinlist(pinlist);
				}
			}

			// Restyle the pins on the generated device connectors.
			PreferenceSetHelper.applyStyleSet(pinlist.getObjectsForStyling(), gp.getStyleSet(), true);
		}
	}

	private static boolean isAnalysisModelBlank(IAnalysable analysable)
	{
		// Not sure how we can get a null parameter,
		// all call paths seem to guarantee a non-null value but it has been seen
		if (analysable == null) {
			return true;
		}
		String analysisModel = analysable.getAnalysisModel();
		return analysisModel == null || analysisModel.trim().isEmpty();
	}
}
