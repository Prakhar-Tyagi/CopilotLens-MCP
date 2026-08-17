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
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.shared.ISharedBackshell;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.parts.ILibraryBaseObject;
import chs.cof.parts.ILibraryGraphic;
import chs.cof.parts.ILibraryObject;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.ISymbolLibraryMgr;
import chs.cof.symbol.ISymbolRef;
import chs.cof.symbol.SymbolLibraryTypeEnum;
import chs.cof.symbol.SymbolTypeEnum;
import chs.utilities.ui.BasicUIFactory;
import chs.ctf.ui.form.LibrarySymbolFilter;
import chs.ctf.ui.form.LibraryTreeModel;
import chs.ctf.ui.form.SymbolSelectionPanel;
import chs.system.FactoryMgr;
import chs.utilities.CHSConstants;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.property.IBooleanProperty;
import chs.utilities.ui.property.IProperty;
import chs.utilities.ui.property.IPropertyGroup;
import chs.utilities.ui.property.IPropertyValidator;
import chs.utilities.ui.property.IStringProperty;
import chs.utilities.ui.property.PropertyFactory;
import chs.utilities.ui.property.PropertyPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class AddBackshellDialog extends BaseBackshellDialog
{

	private static final int MIN_DIALOG_WIDTH = 350;
	private static final int MIN_DIALOG_HEIGHT = 350;
	private static final int PREFERRED_DIALOG_WIDTH = 500;
	private static final int PREFERRED_DIALOG_HEIGHT = 600;

	@Nullable private final ISharedPinList sharedPinList;
	private final boolean isSharedPinlistFrozen;

	private IStringProperty m_bsNameProp;
	private IBooleanProperty m_bsDefaultNameProp = null;
	private String m_name = "";
	private boolean m_compositeName = false;
	private JTextField terminationNameTF;
	private SymbolSelectionPanel m_symPanel;
	private Class<AddBackshellDialog> dialogClass = AddBackshellDialog.class;

	AddBackshellDialog(@Nullable Frame frame, String atitle,
			@NotNull IAddBackshellController controller, boolean amodal)
	{
		super(frame, atitle, amodal, controller);

		IConnector conn = controller.getConnector();
		sharedPinList = conn.getSharedPinList();
		isSharedPinlistFrozen = controller.isSharedPinListFrozen();

		jbInit();
		addListeners();
		pack();
	}

	private void jbInit()
	{
		setMinimumSize(new Dimension(MIN_DIALOG_WIDTH, MIN_DIALOG_HEIGHT));
		setPreferredSize(new Dimension(PREFERRED_DIALOG_WIDTH, PREFERRED_DIALOG_HEIGHT));

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout(4, 4));

		mainPanel.add(getBackshellNamePanel(), BorderLayout.NORTH);
		mainPanel.add(getSymbolPanel(), BorderLayout.CENTER);
		mainPanel.add(getTerminationsPanel(), BorderLayout.SOUTH);

		getContentPane().add(mainPanel);
	}

	@NotNull protected ActionListener getCancelActionListener()
	{
		return new AddBackshellCancelActionListener(controller);
	}

	@NotNull protected ActionListener getOKActionListener()
	{
		return new AddBackshellOKActionListener(controller);
	}

	@NotNull private PropertyPanel getBackshellNamePanel()
	{
		// dts0101349348
		//
		// The holders for the properties...
		//
		IPropertyGroup backshellGroup = PropertyFactory.createPropertyGroup(
				ResourceMgr.getString(dialogClass, "AddBackshellDialog.backshells.title"));
		m_bsNameProp = backshellGroup.createStringProperty("nameProp",
				ResourceMgr.getStringForLabel(dialogClass, "AddBackshellDialog.name.label"), "");
		if (sharedPinList == null) {
			m_bsDefaultNameProp = backshellGroup.createBooleanProperty("DefaultName",
					ResourceMgr.getString(dialogClass, "AddBackshellDialog.checkbox.default.label"), true);
			m_bsDefaultNameProp.addValidator(new BackshellDefaultNamePropertyValidator());
		}
		else {
			m_bsNameProp.setEnabled(!isSharedPinlistFrozen);
		}
		backshellGroup
				.setLabel(ResourceMgr.getString(dialogClass, "AddBackshellDialog.backshellName.title"));
		PropertyPanel backshellNamePanel = new PropertyPanel("BackshellNamePanel", backshellGroup);
		m_bsNameProp.addValidator(new BackshellNamePropertyValidator());

		setNameProperty();

		return backshellNamePanel;
	}

	@NotNull private JPanel getSymbolPanel()
	{
		Set<String> librarySymbolIds = new HashSet<>();
		CollectLibrarySymbolUIDs(librarySymbolIds);

		LibrarySymbolFilter filter = new LibrarySymbolFilter(){
			public boolean accept(IStamp sym)
			{
				if(!librarySymbolIds.isEmpty() && !librarySymbolIds.contains(sym.getUID().getString())){
					return false;
				}
				return super.accept(sym);
			}
		};
		filter.setSymbolTypes(Collections.singleton(SymbolTypeEnum.BACKSHELL));
		final ISymbolLibraryMgr symbolLibraryMgr = CAFUtils.getInstance().getCHSSystem().getSymbolLibraryMgr();
		LibraryTreeModel model = new LibraryTreeModel(symbolLibraryMgr, SymbolLibraryTypeEnum.SYMBOL, filter);

		final String objName = ResourceMgr.getString(dialogClass, "AddBackshellDialog.Selection.Object.Name");
		m_symPanel = new SymbolSelectionPanel(model, null, objName, true, null, true, false, true, false);
		//
		// If the connector is frozen, do not allow user to change the symbol [NOTE, tree will be null if there are no libraries!]
		//
		JTree tree = m_symPanel.getTree();
		if (tree != null) {
			tree.setEnabled(!isSharedPinlistFrozen);
		}

		JPanel symbolPanel = new JPanel();
		final String terminations = ResourceMgr.getString(dialogClass, "AddBackshellDialog.symbolChooser.label");
		symbolPanel.setBorder(BorderFactory.createTitledBorder(terminations));
		symbolPanel.setLayout(new BorderLayout(4, 4));

		symbolPanel.add(m_symPanel);

		// If we have a backshell symbol, select this by default.
		setCurrentSymbolByID();

		return symbolPanel;
	}

	private void CollectLibrarySymbolUIDs(Set<String> librarySymbolIds)
	{
		ILibraryBaseObject libraryBackshell = controller.getLibraryBackshell();
		ILibraryObject libObj = CommonUtils.cast(libraryBackshell, ILibraryObject.class);
		if(libObj!=null){
			Set<ILibraryGraphic> libraryGraphics = libObj.getLibraryGraphics(ILibraryGraphic.ContextType.ELECTRICAL);
			librarySymbolIds.addAll(
					libraryGraphics.stream().filter(iLibraryGraphic -> iLibraryGraphic.getLibrarySymbol() != null)
							.map(iLibraryGraphic -> iLibraryGraphic.getLibrarySymbol_Id()).collect(
							Collectors.toSet()));
		}
	}

	@NotNull private JPanel getTerminationsPanel()
	{
		JPanel terminationsPanel = new JPanel();
		final String terminations = ResourceMgr.getString(dialogClass, "AddBackshellDialog.title.terminations");
		terminationsPanel.setBorder(BorderFactory.createTitledBorder(terminations));
		terminationsPanel.setLayout(new BorderLayout(4, 4));

		//Do populate existing backshell terminations in the scroll pane, first
		terminationsPanel.add(getBackshellTerminationsScrollPane(), BorderLayout.CENTER);

		//Do populate the name panel with new termination object, next. Do not reverse order
		terminationsPanel.add(getBackshellTerminationNamePanel(), BorderLayout.NORTH);

		return terminationsPanel;
	}

	@NotNull private JScrollPane getBackshellTerminationsScrollPane()
	{
		populateExistingBackshellTerminations();

		return prepareTerminationScrollPane("addBackshelldialog.bsList");
	}

	@NotNull private JPanel getBackshellTerminationNamePanel()
	{
		JPanel terminationNamePanel = new JPanel();
		terminationNamePanel.setLayout(new BorderLayout(4, 4));
		final String nameText =
				ResourceMgr.getStringForLabel(dialogClass, "AddBackshellDialog.name.text");

		terminationNamePanel.add(new JLabel(nameText), BorderLayout.WEST);
		terminationNamePanel.add(getBackshellTerminationNameTextField(), BorderLayout.CENTER);
		terminationNamePanel.add(getAddTerminationButton(), BorderLayout.EAST);

		updateBackshellTerminationNameField();

		return terminationNamePanel;
	}

	private JTextField getBackshellTerminationNameTextField()
	{
		terminationNameTF = new JTextField();
		terminationNameTF.setName("addBackshelldialog.nameTF");
		return terminationNameTF;
	}

	@NotNull private JButton getAddTerminationButton()
	{
		final JButton addButton =
				BasicUIFactory.getInstance().createSiemensCustomJButton(ResourceMgr.getString(dialogClass, "AddBackshellDialog.add.text"));
		addButton.setMnemonic(ResourceMgr.getMnemonic(dialogClass, "AddBackshellDialog.add.mnemonic"));
		addButton.setName("addBackshelldialog.add");
		addButton.setEnabled(false);

		terminationNameTF.getDocument()
				.addDocumentListener(new BackshellTerminationNameTextDocumentListener(terminationNameTF, addButton));
		addButton.addActionListener(new AddBackshellTerminationActionListener(terminationNameTF, addButton));

		return addButton;
	}

	private void updateBackshellTerminationNameField()
	{
		//
		// If the connector is frozen, do not allow user to add terminations.
		//
		if (isSharedPinlistFrozen) {
			terminationNameTF.setEnabled(false);
			terminationNameTF.setToolTipText(ResourceMgr.getString(dialogClass,
					"AddBackshellDialog.connectorFrozenNoTerminationChange.text"));
		}
		else if (controller.getLibraryBackshell() != null) {
			terminationNameTF.setEnabled(false);
			terminationNameTF.setToolTipText(ResourceMgr.getString(dialogClass,
					"AddBackshellDialog.backshellPartedNoTerminationChange.text"));
		}
		else {
			terminationNameTF.setEnabled(true);
			terminationNameTF.setToolTipText("");
			setDefaultTerminationName();
		}
	}

	private void setDefaultTerminationName()
	{
		final String terminationName = controller.getUnusedDefaultBackshellTerminationName();

		if (!StringUtils.isEmpty(terminationName)) {
			terminationNameTF.setText(terminationName);
		}
	}

	private void setNameProperty()
	{
		IBackshell bs = controller.getExistingBackshell();
		ISharedBackshell sbs = controller.getSharedBackshell();

		//
		// If we have a backshell, then get the values.. .
		//
		if (bs != null || sbs != null) {
			//
			// Initialize the backshell info values
			//
			if (bs != null) {
				m_bsNameProp.setValue(bs.getName());
			}
			else {
				m_bsNameProp.setValue(sbs.getName());
			}
		}
	}

	private void setCurrentSymbolByID()
	{
		ISymbolRef sref = controller.getBackshellSymbolRef();

		if (sref == null) {
			m_symPanel.setCurrentSymbolByID(null);
		}
		else {
			m_symPanel.setCurrentSymbolByID(sref.getSymbolUID());
		}
	}

	private String getBackshellName()
	{
		return m_bsNameProp.getValue();
	}

	@Nullable private ISymbolRef getBackshellSymbol()
	{
		ISymbolDef sdef = (ISymbolDef) m_symPanel.getCurrentSymbol();
		if (sdef == null) {
			return null;
		}
		else {
			return FactoryMgr.getSymbolFactory().constructSymbolRef(sdef);
		}
	}

	IStringProperty getBackshellNameProperty()
	{
		return m_bsNameProp;
	}

	IBooleanProperty getBackshellDefaultNameProperty()
	{
		return m_bsDefaultNameProp;
	}

	private class BackshellDefaultNamePropertyValidator implements IPropertyValidator
	{

		@Override public boolean validate(@Nullable IProperty property)
		{
			if (m_bsDefaultNameProp != null && m_bsDefaultNameProp.getValue()) {

				if (StringUtils.isBlank(m_name)) {
					IBackshell backshell = controller.getExistingOrTemporaryBackshell();
					m_name = backshell.getName();
					m_compositeName = backshell.isComposedDefaultName();
				}

				m_bsNameProp.setValue(m_name);

				if (m_compositeName) {
					getOkButton().setEnabled(true);
					m_bsDefaultNameProp.setName("CompositeName");
					m_bsDefaultNameProp.setLabel(ResourceMgr.getString(dialogClass,
							"AddBackshellDialog.checkbox.composite.label"));
				}
				m_bsNameProp.setEnabled(false);
				return true;
			}
			else {
				m_bsNameProp.setEnabled(true);
				if (m_compositeName) {
					getOkButton().setEnabled(!m_bsNameProp.getValue().isEmpty());
				}
				return m_bsNameProp.getValue() != null;
			}
		}

		@Nullable @Override public String getValidityReason()
		{
			return null;
		}
	}

	private class BackshellNamePropertyValidator implements IPropertyValidator
	{

		public boolean validate(IProperty property)
		{
			String name = m_bsNameProp.getValue();
			boolean isBlank = StringUtils.isBlank(name);
			if ((isBlank && sharedPinList != null) || name.length() > CHSConstants.DIAGRAM_OBJECT_NAME_LENGTH) {
				getOkButton().setEnabled(false);
				return false;
			}
			if (m_compositeName) {
				if (m_bsNameProp.isEnabled() && isBlank) {
					getOkButton().setEnabled(false);
					return false;
				}
			}
			else {
				if ((isBlank && m_bsNameProp.isEnabled())) {
					getOkButton().setEnabled(false);
					return false;
				}
			}
			getOkButton().setEnabled(true);
			return true;
		}

		public String getValidityReason()
		{
			return ResourceMgr.getString(dialogClass, "AddBackshellDialog.invalidname.text");
		}
	}

	private class BackshellTerminationNameTextDocumentListener implements DocumentListener
	{

		private final JTextField nameTF;
		private final JButton addButton;

		private BackshellTerminationNameTextDocumentListener(JTextField nameTF, JButton addButton)
		{
			this.nameTF = nameTF;
			this.addButton = addButton;
		}

		public void changedUpdate(DocumentEvent e)
		{
			handleUpdates();
		}

		public void insertUpdate(DocumentEvent e)
		{
			handleUpdates();
		}

		public void removeUpdate(DocumentEvent e)
		{
			handleUpdates();
		}

		private void handleUpdates()
		{
			String backshellTerminationNameEntered = StringUtils.trim(nameTF.getText());
			if (m_existingTerminations.contains(backshellTerminationNameEntered)) {
				backshellTerminationNameEntered = "";
			}

			final boolean enableAddButton = controller.getLibraryBackshell() == null &&
					!StringUtils.isBlank(backshellTerminationNameEntered) &&
					!isSharedPinlistFrozen &&
					backshellTerminationNameEntered.length() <= CHSConstants.DIAGRAM_OBJECT_NAME_LENGTH;

			addButton.setEnabled(enableAddButton);
		}
	}

	private class AddBackshellTerminationActionListener implements ActionListener
	{

		private final JTextField nameTF;
		private final JButton addButton;

		private AddBackshellTerminationActionListener(JTextField nameTF, JButton addButton)
		{
			this.nameTF = nameTF;
			this.addButton = addButton;
		}

		public void actionPerformed(ActionEvent e)
		{
			@SuppressWarnings("deprecation") Set<Object> selected =
					new HashSet<>(Arrays.asList(m_termList.getSelectedValues()));
			String nm = StringUtils.trim(nameTF.getText());
			selected.add(nm);
			m_terms.add(nm);
			m_existingTerminations.add(nm);
			addButton.setEnabled(false);

			m_termList.clearSelection();
			ListModel<Object> lm = m_termList.getModel();
			for (int i = 0; i < lm.getSize(); i++) {
				if (selected.contains(lm.getElementAt(i))) {
					m_termList.getSelectionModel().addSelectionInterval(i, i);
				}
			}

			//clear text field value
			nameTF.setText(null);
		}
	}

	private static class AddBackshellOKCancelActionListener implements ActionListener
	{

		final IAddBackshellController backshellController;

		private AddBackshellOKCancelActionListener(IAddBackshellController controller)
		{
			backshellController = controller;
		}

		@Override public void actionPerformed(ActionEvent e)
		{

		}
	}

	private class AddBackshellCancelActionListener extends AddBackshellOKCancelActionListener
	{

		private AddBackshellCancelActionListener(IAddBackshellController controller)
		{
			super(controller);
		}

		public void actionPerformed(ActionEvent e)
		{

			setCancelled(true);
			setVisible(false);

			super.actionPerformed(e);
		}
	}

	private class AddBackshellOKActionListener extends AddBackshellOKCancelActionListener
	{

		private AddBackshellOKActionListener(IAddBackshellController controller)
		{
			super(controller);
		}

		public void actionPerformed(ActionEvent e)
		{
			//
			// Get the list of backshell terminations [IBackshellTermination or String (if new)]
			//
			backshellController
					.selectedBackshellTerminations(getSelectedBackshellTerminations());

			backshellController.selectedBackshellName(getBackshellName());

			backshellController.selectedBackshellSymbol(getBackshellSymbol());

			setCancelled(false);
			setVisible(false);

			super.actionPerformed(e);
		}
	}
}
