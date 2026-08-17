/*
 * Copyright 2002-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.shared;

import chs.caf.CAFUtils;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinReservationView;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.ctf.caf.ui.PinListPlaceOptionsDialog;
import chs.ctf.caf.ui.PinPlaceOptionStateHandler;
import chs.ctf.caf.utils.IPinProxy;
import chs.system.FactoryMgr;
import chs.utilities.Environment;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.property.ChoiceTypeValue;
import chs.utility.ui.PinSelectionAbstractPanel;
import chs.utility.ui.PinSelectionCapabilities;
import chs.utility.ui.PinSelectionCommonPanel;
import chs.utility.ui.PinSelectionUserOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * A dialog for shared pin usage in capital logic.
 *
 * @created October 3, 2002
 */
public class AddSharedPinDialog extends PinListPlaceOptionsDialog
{

	private boolean m_success;
	private ILogicDesign m_curDesign;
	private ISharedPinList m_sharedPinlist;
	private static final int MinimumPanelWidth = 550;
	private static final int MinimumPanelHeight = 300;
	static final Dimension MinimumPanelSize = new Dimension(MinimumPanelWidth, MinimumPanelHeight);

	/**
	 * Constructor for the AddSharedPinListDialog object
	 *
	 * @param frame Description of the Parameter
	 * @param title Description of the Parameter
	 */
	public AddSharedPinDialog(Frame frame,
			String title, @Nullable ISharedPinList sharedPinList)
	{
		super(frame, title, true);

		m_curDesign = null;
		m_sharedPinlist = sharedPinList;
		m_reference = false;
		m_placeAsStack = false;
		m_success = false;
		buildDialog(getPinSelectionCommonPanel());
	}

	protected void buildDialog(PinSelectionCommonPanel pinSelectionCommonPanel)
	{
		try {
			addComponents(pinSelectionCommonPanel);
			hookupButtons();
			pack();
			setMinimumSize(MinimumPanelSize);
		}
		catch (Exception ex) {
			Environment.getExceptionDisplay().displayException(ex, false);
		}
	}

	/**
	 * Constructor for the AddSharedPinListDialog object
	 */
	public AddSharedPinDialog()
	{
		this(null, "", null);
	}

	/**
	 * Brings up this dialog in a modal fashion to figure out what shared pinlist and sharedpin are being added to the
	 * diagram.
	 *
	 * @return Description of the Return Value
	 */
	public boolean selectPins(@NotNull ISharedPinList sharedPinList, ILogicDesign designContext,
			ISharedPinReservationView pinview, @NotNull IPlacementOptionParams params)
	{
		m_curDesign = designContext;
		m_sharedPinlist = sharedPinList;
		PinSelectionUserOptions userOptions = createUserSelectionOption();
		userOptions.setSharedPinListChanged(true);
		if (!m_pinSelectionPanel.reset(sharedPinList, designContext, pinview, userOptions, Collections.singleton(
				PinSelectionCapabilities.SymbolTreeCapability))) {
			return false;
		}
		buildTypeSpecificOptions(m_sharedPinlist.getType(), params);

		setVisible(true);

		return m_success;
	}

	@Override protected void createLoadSharedPinConnectionInfoCheckBox()
	{
		addLoadSharedPinConnectionInfo(
				t -> m_pinSelectionPanel.loadSharedPinConnectionInformationFromOtherDesigns(t));
	}

	public Collection<IPinProxy> getUsedPins()
	{
		return m_pinSelectionPanel.getPins();
	}

	private void addComponents(PinSelectionCommonPanel pinSelectionCommonPanel)
	{

		Border lineBorder = BorderFactory.createLineBorder(Color.black);

		m_pinSelectionPanel = pinSelectionCommonPanel;

		JPanel core = new JPanel();
		core.setLayout(new BorderLayout());
		core.add(new JPanel(), BorderLayout.NORTH);
		core.add(m_pinSelectionPanel, BorderLayout.CENTER);

		m_optionsPanel = new JPanel();
		m_optionsPanel.setLayout(new BorderLayout());
		initOptionsPropertyGroup();
		getOkButton().setEnabled(false);

		core.add(m_optionsPanel, BorderLayout.SOUTH);
		getContentPane().add(core, BorderLayout.CENTER);
	}

	@NotNull protected PinSelectionCommonPanel getPinSelectionCommonPanel()
	{
		return new PinSelectionCommonPanel(this, FactoryMgr.getDrawFactory(),
				CAFUtils.getInstance().getCommonFactory(),
				new SharedDeviceSymbolTreeController(),
				new Consumer<List<?>>()
				{

					@Override public void accept(List<?> objects)
					{
						getOkButton().setEnabled(!objects.isEmpty());
						placeAsStackButtonStatusUpdate(objects);
					}
				}, getEscapeListener());
	}

	protected void createAsStackOption()
	{
		m_placeAsStackOption = buildOption(AS_STACK_OPTION, PLACEASSTACK_TOOLTIP, true);
		m_placeAsStackOption.setName("AddSharedPinPlaceAsStackOption");
		m_placeAsStackOption.setMnemonic(ResourceMgr
				.getMnemonic(AddSharedPinDialog.class, "AddSharedPinDialog.placeAsStackCheckBox.mnemonic"));
		m_placeAsStackOption.addPropertyChangeListener(evt -> m_placeAsStack = (boolean) evt.getNewValue());
	}

	@Override protected void createAsReferenceCheckBox()
	{
		m_referenceOption = buildOption(REFERENCE_OPTION, REFERENCE_TOOLTIP, false);
		m_referenceOption.setName("AddSharedPinReferenceOption");
		m_referenceOption.setChoiceType(ChoiceTypeValue.CHECK_BOX);
		m_referenceOption.setMnemonic(java.awt.event.KeyEvent.VK_R);
		m_referenceOption.addPropertyChangeListener(new PinPlaceOptionStateHandler(this));
		m_referenceOption.
				addPropertyChangeListener(
						new PropertyChangeListener()
						{
							@Override public void propertyChange(PropertyChangeEvent evt)
							{
								m_reference = (boolean) evt.getNewValue();

								if ((m_sharedPinlist != null) && (m_curDesign != null)) {
									PinSelectionUserOptions userOptions = createUserSelectionOption();
									if (!m_pinSelectionPanel
											.reset(m_sharedPinlist, m_curDesign, m_pinSelectionPanel.getSharedPinView(),
													userOptions)) {
										m_success = false;
										savePrefs();
										setVisible(false);
									}
								}
							}
						});
	}

	private void hookupButtons()
	{
		getOkButton().addActionListener(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent evt)
					{
						m_success = true;
						savePrefs();
						setVisible(false);
						dispose();
					}
				}
		);

		getCancelButton().addActionListener(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent evt)
					{
						m_success = false;
						savePrefs();
						setVisible(false);
						dispose();
					}
				}
		);
	}

	public boolean isReference()
	{
		return m_reference;
	}

	public boolean isWithConductor()
	{
		return m_withConductorOption != null && m_withConductorOption.getValue();
	}

	public boolean isPlaceAsStack()
	{
		return m_placeAsStack;
	}

	public boolean isPlaceAsGroup()
	{
		return m_placeAsGroup;
	}

	public void cleanup()
	{
		m_pinSelectionPanel.cleanUp();
	}

	@NotNull @Override protected JRootPane createRootPane()
	{
		return new JRootPane();
	}

	@Nullable protected ISharedPinList getSharedPinList()
	{
		return m_sharedPinlist;
	}

	@Nullable protected PinSelectionAbstractPanel getPinSelectionPanel()
	{
		return m_pinSelectionPanel;
	}
}
