/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.ctf.caf.ui;

import chs.utilities.CommonUtils;
import chs.utilities.ui.property.IBooleanProperty;
import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;

/**
 * Handles the state changes of pin place options and ensures mutually exclusive options
 * are managed correctly based on the current state and symbol selection.
 */
public class PinPlaceOptionStateHandler implements PropertyChangeListener
{

	private final IOptionsDetailProvider m_detailProvider;

	public PinPlaceOptionStateHandler(@NotNull IOptionsDetailProvider detailProvider)
	{
		m_detailProvider = detailProvider;
	}

	/**
	 * Responds to property changes in pin place options.
	 * It adjusts the state of mutually exclusive options based on the current selection and
	 * the type of panel (pin or symbol) that is active.
	 *
	 * @param evt the event that describes the change.
	 */
	@Override public void propertyChange(@NotNull PropertyChangeEvent evt)
	{
		IBooleanProperty sourceOption = CommonUtils.cast(evt.getSource(), IBooleanProperty.class);
		if (sourceOption == null) {
			return;
		}
		boolean currentSourceOptionValue = (boolean) evt.getNewValue();
		boolean isPinPanelSelected = !m_detailProvider.isSymbolSelected();

		Collection<IBooleanProperty> mutuallyExclusiveOptions =
				isPinPanelSelected ?
						m_detailProvider.getMutuallyExclusiveOptionsWhenPinPanelSelected(sourceOption) :
						m_detailProvider.getMutuallyExclusiveOptionsWhenSymbolPanelSelected(sourceOption);

		mutuallyExclusiveOptions.forEach(option -> option.setEnabled(!currentSourceOptionValue));
		if (isPinPanelSelected && currentSourceOptionValue) {
			resetToIndividualFromPlaceAsStack();
		}
	}

	/**
	 * Resets the options to individual from place as stack.
	 * Ensures that the 'place as stack' option is unSelected and the 'individual' option is selected.
	 */
	protected void resetToIndividualFromPlaceAsStack()
	{
		IBooleanProperty placeAsStackOption = m_detailProvider.getPlaceAsStackOption();
		IBooleanProperty individualOption = m_detailProvider.getIndividualOption();

		if (individualOption != null && placeAsStackOption != null && placeAsStackOption.getValue()) {
			placeAsStackOption.setValue(false);
			individualOption.setValue(true);
		}
	}
}
