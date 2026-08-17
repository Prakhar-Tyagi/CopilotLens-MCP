package chs.caplets.logic.actions;

import chs.caplets.logic.BasicAddPinListDialog;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.shared.ISharedPinReservationView;
import chs.common.IMultiSymbolledPinlist;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.ctf.caf.ui.PinPlaceOptionStateHandler;
import chs.utilities.Environment;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.property.ChoiceTypeValue;
import chs.utility.ui.PinSelectionUserOptions;
import org.jetbrains.annotations.NotNull;

import java.awt.Frame;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.function.Consumer;

/*
 * Copyright 2010 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

public class AddMultiSymbolledPinListDialog extends BasicAddPinListDialog
{

	private IMultiSymbolledPinlist m_multiSymbolledPinlist;

	public AddMultiSymbolledPinListDialog(Frame frame, String title, boolean modal)
	{
		super(frame, title, modal);
		try {
			addComponents();
			hookupButtons();
			pack();
		}
		catch (Exception ex) {
			Environment.getExceptionDisplay().displayException(ex, false);
		}
	}

	@Override public void createLoadSharedPinConnectionInfoCheckBox()
	{
		addLoadSharedPinConnectionInfo(
				t -> m_pinSelectionPanel.loadSharedPinConnectionInformationFromOtherDesigns(t));
	}

	@Override public Consumer<List<?>> getSelectedPinsHandler()
	{
		return new Consumer<List<?>>()
		{
			@Override public void accept(List<?> objects)
			{
				placeAsStackButtonStatusUpdate(objects);
			}
		};
	}

	@Override protected void createAsReferenceCheckBox()
	{
		m_referenceOption = buildOption(REFERENCE_OPTION, REFERENCE_TOOLTIP, false);
		m_referenceOption.setName("chkReference");
		m_referenceOption.setChoiceType(ChoiceTypeValue.CHECK_BOX);
		m_referenceOption.setMnemonic(ResourceMgr.getMnemonic(AddMultiSymbolledPinListDialog.class,
				"AddMultiSymbolledPinListDialog.referenceCheckBox.mnemonic"));
		m_referenceOption.addPropertyChangeListener(new PinPlaceOptionStateHandler(this));
		m_referenceOption.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override public void propertyChange(PropertyChangeEvent evt)
			{
				// setReferece(e.getStateChange());
				m_reference = (boolean) evt.getNewValue();
				if (m_pinSelectionPanel != null) {
					PinSelectionUserOptions userOptions = createUserSelectionOption();
					if (!m_pinSelectionPanel.reset(m_multiSymbolledPinlist, m_curDesign,
							m_pinSelectionPanel.getSharedPinView(), userOptions)) {
						m_success = false;
						setVisible(false);
					}
				}
			}
		});
//			m_optionsPanel.add(m_referenceOption, BorderLayout.CENTER);
//			setReferenceCheckBoxResources(ResourceMgr.getString(AddMultiSymbolledPinListDialog.class,
//							"AddMultiSymbolledPinListDialog.referenceCheckBox.title"),
//					"chkReference", ResourceMgr.getMnemonic(AddMultiSymbolledPinListDialog.class,
//							"AddMultiSymbolledPinListDialog.referenceCheckBox.mnemonic"));
	}

	/**
	 * Overridden here to set a name for automation
	 *
	 * @param name The name - ignored here
	 */
	@Override public void setName(String name)
	{
		super.setName("AddPinListDialog");
	}

	public boolean selectPinList(IMultiSymbolledPinlist multiSymbolledPinlist, ISharedPinReservationView sharedPinView,
			@NotNull IPlacementOptionParams params)
	{
		if (multiSymbolledPinlist instanceof ILogicObject) {
			m_curDesign = ((ILogicObject) multiSymbolledPinlist).getLogicDesign();
		}
		m_multiSymbolledPinlist = multiSymbolledPinlist;
		return super.selectPinList(multiSymbolledPinlist, sharedPinView, params);
	}
}

