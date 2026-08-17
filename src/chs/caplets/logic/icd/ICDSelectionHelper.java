/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.icd;

import chs.cof.icd.IDeviceICD;
import chs.cof.library.IICDComponentSearchController;
import chs.cof.logical.GeneralReportValidationHandler;
import chs.cof.parts.ILibraryBaseObject;
import chs.cof.parts.ILibraryCustomerPartNumber;
import chs.cof.parts.ILibraryDevice;
import chs.cof.parts.ILibraryDeviceFootprint;
import chs.cof.parts.ILibraryGraphic;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.ILibrarySupplierPartNumber;
import chs.cof.parts.LibraryCriteriaHelper;
import chs.cof.parts.partselector.ILibrarySelectionFilter;
import chs.common.IUID;
import chs.utilities.StringUtils;
import chs.utility.ICDUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Encapsulates ICD-based library selection logic that can be reused
 * across different UI contexts.
 * <p>
 * Stateless with respect to {@link ICDSelection} ? the current selection
 * is passed as a parameter to each method, eliminating the need to
 * synchronize state when the outer class reassigns its selection.
 * </p>
 */
public class ICDSelectionHelper
{

	private ILibrarySelectionFilter icdSelectionfilter;

	public ICDSelectionHelper()
	{
	}

	/**
	 * Creates an {@link ILibrarySelectionFilter} that excludes footprints,
	 * supplier part numbers, and customer part numbers already specified by
	 * the ICD on the given part selection.
	 *
	 * @param partSelection the current ICD selection whose ICD drives the filter
	 * @return the selection filter, never null
	 */
	@NotNull
	ILibrarySelectionFilter getSelectionFilter(@NotNull ICDSelection partSelection)
	{
		if (icdSelectionfilter != null) {
			return icdSelectionfilter;
		}
		final ILibrarySelectionFilter defaultFilter =
				LibraryCriteriaHelper.getSelectionFilterForElectricalSymbols(null, null, new ArrayList<IUID>());
		icdSelectionfilter = new ILibrarySelectionFilter()
		{
			@Override
			public boolean include(ILibraryBaseObject obj)
			{
				if (obj instanceof ILibraryDeviceFootprint) {
					IDeviceICD icd = partSelection.getICD();
					if (icd != null && !StringUtils.isBlank(icd.getFootprintName())) {
						return false;
					}
				}
				if (obj instanceof ILibrarySupplierPartNumber) {
					IDeviceICD icd = partSelection.getICD();
					if (icd != null && !StringUtils.isBlank(icd.getSupplierPartNumber())) {
						return false;
					}
				}
				if (obj instanceof ILibraryCustomerPartNumber) {
					IDeviceICD icd = partSelection.getICD();
					if (icd != null && !StringUtils.isBlank(icd.getCustomerPartNumber())) {
						return false;
					}
				}
				return defaultFilter.include(obj);
			}

			@Override
			@Nullable
			public ILibraryBaseObject getDefaultSelection(Collection<ILibraryBaseObject> objs)
			{
				return defaultFilter.getDefaultSelection(objs);
			}

			@Override
			public void setSelectedCustomerpartNumber(ILibraryCustomerPartNumber custpartNumber)
			{
				defaultFilter.setSelectedCustomerpartNumber(custpartNumber);
			}

			@Override
			public void setSelectedSupplierpartNumber(ILibrarySupplierPartNumber supppartNumber)
			{
				defaultFilter.setSelectedSupplierpartNumber(supppartNumber);
			}
		};
		return icdSelectionfilter;
	}

	/**
	 * Resolves and selects the library objects for the given ICD device,
	 * initializing defaults and updating the search controller.
	 *
	 * @param icd                           the device ICD to select library objects for
	 * @param partSelection                 the current ICD selection to operate on
	 * @param libraryCompSearchController the search controller to update with the selected objects (can be null if not applicable)
	 */
	public void selectLibraryObjects(@NotNull IDeviceICD icd, @NotNull ICDSelection partSelection,
			@Nullable IICDComponentSearchController libraryCompSearchController)
	{
		try {
			GeneralReportValidationHandler.beginScope();
			ILibraryDevice libraryDevice = icd.getLibraryDevice();
			ILibraryObject existingObj = partSelection.getSelectedObject();
			if (libraryDevice == existingObj && libraryDevice != null) {
				return;
			}
			initializeDefaults(partSelection);

			libraryDevice =
					libraryDevice != null ? libraryDevice : ICDUtils.createTransientLibraryDevice(icd);
			partSelection.setSelectedLibraryObject(libraryDevice);
			if (libraryCompSearchController != null) {
				List<ILibraryBaseObject> selectedObjs = new ArrayList<>();
				selectedObjs.add(libraryDevice);
				libraryCompSearchController.selectObjects(selectedObjs);
			}
			Collection<ILibraryObject> col = new ArrayList<>();
			col.add(libraryDevice);
			partSelection.setSelectedObjects(col);
		}
		finally {
			GeneralReportValidationHandler.clearScope();
		}
	}

	/**
	 * Initializes the given part selection with default values derived from
	 * the ICD: library object, footprint, supplier/customer part numbers,
	 * and the default symbol.
	 *
	 * @param partSelection the ICD selection to initialize
	 */
	void initializeDefaults(@NotNull ICDSelection partSelection)
	{
		IDeviceICD icd = partSelection.getICD();
		if (icd == null) {
			return;
		}

		ILibraryDevice device = icd.getLibraryDevice();
		ILibrarySelectionFilter selectionFilter = getSelectionFilter(partSelection);

		if (device != null) {
			partSelection.setSelectedLibraryObject(device);
			partSelection.assignDefaultValues();
			partSelection.setSelectedFootprint(null);

			String footprintName = icd.getFootprintName();
			for (ILibraryDeviceFootprint aFootprint : device.getDeviceFootprints()) {
				if (aFootprint.getFootprintName().equals(footprintName)) {
					partSelection.setSelectedFootprint(aFootprint);
					break;
				}
			}

			ICDBrowserPanel.updateSupplierPartNumber(partSelection, icd, device);
			ICDBrowserPanel.updateCustomerPartNumber(partSelection, icd, device);

			partSelection.setSelectedSymbol(null);

			List<ILibraryGraphic> selectedSymbols = new ArrayList<>();
			for (ILibraryGraphic aLibGraphic : device.getLibraryGraphics()) {
				if (selectionFilter.include(aLibGraphic)) {
					selectedSymbols.add(aLibGraphic);
				}
			}
			ILibraryGraphic defaultGraphic =
					(ILibraryGraphic) selectionFilter
							.getDefaultSelection(new HashSet<ILibraryBaseObject>(selectedSymbols));

			partSelection.setSelectedSymbol(defaultGraphic);
		}
	}
}
