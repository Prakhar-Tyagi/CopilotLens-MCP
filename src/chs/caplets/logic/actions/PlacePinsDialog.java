/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.logic.actions;

import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.ctf.caf.ui.PinPlaceOptionStateHandler;
import chs.ctf.caf.utils.IPinProxy;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.property.ChoiceTypeValue;
import chs.utility.ui.PinSelectionAbstractPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dialog for either placing a selection of existing connectivity pins or creating a single new connectivity+schematic
 * pin.
 */
public class PlacePinsDialog extends BasePlacePinsDialog
{

	private ConnectivityCommonPinSelectionPanel psp;
	private IPinList m_pinList;
	private static final int MinimumPanelWidth = 550;
	private static final int MinimumPanelHeight = 300;
	static final Dimension MinimumPanelSize = new Dimension(MinimumPanelWidth, MinimumPanelHeight);

	public PlacePinsDialog(@Nullable Frame frame, IPinList pinlist, boolean allowCreation, @NotNull IPlacementOptionParams params)
	{
		super(frame, allowCreation);
		m_pinList = pinlist;
		boolean bIsRingTerminal = IConnector.Statics.isRingTerminalTypeConnector(pinlist);
		this.allowCreation = allowCreation &&
				((bIsRingTerminal && pinlist.getNumPins() == 0 && !pinlist.isPartAssigned()) || !bIsRingTerminal);
		initialize(params);
		buildTypeSpecificOptions(PinListTypeEnum.from_connectivity(pinlist), params);
		setMinimumSize(MinimumPanelSize);
	}

	/**
	 * Get the list of pins that were selected in the dialog
	 *
	 * @return A possibly empty list of pins, sorted as they appear selected in the dialog
	 */
	public List<IPinProxy> getPins()
	{
		return psp.getPins();
	}

	@NotNull @Override protected String extendedSuffix()
	{
		return "_Pins";
	}

	public boolean isReference()
	{
		return m_reference;
	}

	public boolean isPlaceAsStack()
	{
		return m_placeAsStack;
	}

	protected void addItemListenerForReferencePin(JCheckBox referenceOption)
	{
		referenceOption.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				m_reference = (e.getStateChange() == ItemEvent.SELECTED);
				psp.setReference(m_reference);
				psp.reset();
			}
		});
	}

	@NotNull protected JPanel getPinTablePanel()
	{
		if (psp == null) {
			psp = new ConnectivityCommonPinSelectionPanel(m_pinList, null, this, getSelectedPinHandler(),
					getEscapeListener());
		}
		return psp;
	}

	protected Consumer<List<?>> getSelectedPinHandler()
	{
		return new Consumer<List<?>>()
		{
			@Override public void accept(List<?> pinObjects)
			{
				placeButtonStatusUpdate(pinObjects);
				placeAsStackButtonStatusUpdate(pinObjects);
			}
		};
	}

	private void placeButtonStatusUpdate(@Nullable List<?> selectedPins)
	{
		placeButton.setToolTipText(null);
		boolean bSelectionsEmpty = selectedPins == null || selectedPins.isEmpty();
		if (bSelectionsEmpty) {
			placeButton.setEnabled(false);
			placeButton.setToolTipText(
					ResourceMgr.getString(PlacePinsDialog.class, "PlacePinsDialog.placeButton.toolTip"));
		}
		else {
			placeButton.setEnabled(true);
		}
	}

	@Override protected void createAsReferenceCheckBox()
	{
		//optionsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(panel.getBackground()),
		//		ResourceMgr.getString(PlacePinsDialog.class, "PlacePinsDialog.options.title")));

		m_referenceOption = buildOption(REFERENCE_OPTION, REFERENCE_TOOLTIP, false);
		m_referenceOption.setChoiceType(ChoiceTypeValue.CHECK_BOX);
		m_referenceOption.setName("chkReference");
		m_referenceOption.setMnemonic(KeyEvent.VK_R);
		m_referenceOption.addPropertyChangeListener(new PinPlaceOptionStateHandler(this));
		m_referenceOption.addPropertyChangeListener(new PropertyChangeListener()
		{
			public void propertyChange(PropertyChangeEvent evt)
			{
				m_reference = (Boolean) evt.getNewValue();
				psp.setReference(m_reference);
				psp.reset();
			}
		});

		//m_optionsPanel.add(m_referenceOption, BorderLayout.CENTER);
	}

	@Override protected void createAsStackOption()
	{
		m_placeAsStackOption = buildOption(AS_STACK_OPTION, PLACEASSTACK_TOOLTIP, true);
		m_placeAsStackOption.setName(AS_STACK_OPTION);
		m_placeAsStackOption.setMnemonic(
				ResourceMgr.getMnemonic(PlacePinsDialog.class, "PlacePinsDialog.placeAsStack.mnemonic"));
		m_placeAsStackOption.addPropertyChangeListener(evt -> m_placeAsStack = (Boolean) evt.getNewValue());
//			JPanel pan = new JPanel();
//			pan.setLayout(new BorderLayout());
//			pan.add(m_placeAsStackOption, BorderLayout.CENTER);
//			pan.add(new JPanel(), BorderLayout.EAST);
//			m_optionsPanel.add(pan, BorderLayout.EAST);
	}

	@Override protected void hookupComponents()
	{
		super.hookupComponents();
	}

	@NotNull @Override protected JRootPane createRootPane()
	{
		return new JRootPane();
	}

	@Nullable protected PinSelectionAbstractPanel getPinSelectionPanel()
	{
		return psp;
	}
}