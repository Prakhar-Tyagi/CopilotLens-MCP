/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.capture.actions.ddt;

import chs.caf.CAFUtils;
import chs.caplets.capture.actions.ddt.transmodel.DeviceFieldModel;
import chs.caplets.capture.actions.ddt.transmodel.PinFieldModel;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IDevice;
import chs.cof.project.ddtrans.IDDTType;
import chs.cof.project.ddtrans.IDDTTypeMgr;
import chs.cof.project.naming.INameMgr;
import chs.cof.project.naming.INameSpace;
import chs.ctf.caf.ui.CAFOkCancelDialog;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.StripingTableCellRenderer;
import chs.utilities.ui.property.BorderValue;
import chs.utilities.ui.property.GroupTypeValue;
import chs.utilities.ui.property.IActionProperty;
import chs.utilities.ui.property.IComponentProperty;
import chs.utilities.ui.property.IPropertyAttributes;
import chs.utilities.ui.property.IPropertyGroup;
import chs.utilities.ui.property.IStringProperty;
import chs.utilities.ui.property.OrientationValue;
import chs.utilities.ui.property.PropertyFactory;
import chs.utilities.ui.property.PropertyPanel;
import chs.utilities.ui.table.TableUtils;
import chs.utility.ui.PinNumberDialog;
import chs.utility.ui.UIUtils;
import chs.utility.ui.table.TableSorterModel;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dialog used for assigning DDT types to the device.
 */
public class AssignDDTDialog extends CAFOkCancelDialog
{

	private boolean m_validated; // Simple state flag for ok button validation.

	/**
	 * The current ddt type selected for this device.
	 */
	private IDDTType m_ddtType;

	/**
	 * Property that holds the value of the 'ddt' type name
	 */
	private IStringProperty m_ddtTypeProperty;

	/**
	 * Dialog used for selecting a DDT Type
	 */
	private SelectDDTTypeDialog m_typeSelectionDialog;

	/**
	 * Model to represent the table that holds the fields for the device section of the DDT
	 */
	private DeviceFieldModel m_deviceFieldModel;
	private JTable m_deviceFieldTable;

	/**
	 * Data memembers for the pin table
	 */
	private PinFieldModel m_pinFieldModel;
	//private TableSorterModel m_pinFieldModel;
	private JTable m_pinFieldTable;

	/**
	 * For transient pin naming.  Note the name namager isn't actually modified until the apply of the action.
	 */
	private int m_currentNameIndex;
	private String m_pinPrefix;
	private IDevice m_device;

	/**
	 * Does this damn thing have a symbol?
	 */
	private boolean m_hasSymbol = false;
	private boolean m_hasLibraryRef = false;
	private IActionProperty m_delPinBut;
	private IActionProperty m_newPinBut;

	public AssignDDTDialog(Frame frame)
	{
		this(frame, ResourceMgr.getString(AssignDDTDialog.class, "AssignDDTDialog.dialogTitle"), true);
	}

	public AssignDDTDialog(Frame frame, String title, boolean modal)
	{
		super(frame, title, modal);

		getContentPane().add(new PropertyPanel("Dialog", buildDialog()));
		getOkButton().addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m_validated = true;
				setVisible(false);
			}
		});

		getCancelButton().addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m_validated = false;
				setVisible(false);
			}
		});

		// We need to do this to get the tables to resize properly. Also, you'll be in a world of
		// hurt if you try and "pack()" the contents.
		setSize(640, 480);
	}

	public boolean wasValidated()
	{
		return m_validated;
	}

	/**
	 * Accessor to get the transient data out of the dialog.
	 *
	 * @return Model representing the device field information.
	 */
	public DeviceFieldModel getDeviceFieldModel()
	{
		return m_deviceFieldModel;
	}

	/**
	 * Accessor to get the transient data out of the dialog.
	 *
	 * @return Model representing the pin field information.
	 */
	public PinFieldModel getPinFieldModel()
	{
		return m_pinFieldModel;
	}

	/**
	 * Accessor to get the ddt type data out of the dialog
	 *
	 * @return The current DDT Type selected
	 */
	public IDDTType getCurrentType()
	{
		return m_ddtType;
	}

	/**
	 * This initializes the dialog appropriately
	 *
	 * @param dev Device that the ddt is being assigned to.
	 * @param typeMgr The ddt type manager to look up DDT types.
	 * @param withSymbol selected device has symbol.
	 *
	 * @return true if a type cannot be found to initialize the dialog.
	 */
	public boolean initialize(IDevice dev, IDDTTypeMgr typeMgr, boolean withSymbol)
	{
		m_device = dev;
		m_hasSymbol = withSymbol;

		if (m_device != null && m_device.getLibraryRef() != null) {
			m_hasLibraryRef = true;
		}

		setButtonEnablement();
		setupTransientNamespace(dev);
		m_ddtType = dev.getDDTType();
		if (m_ddtType == null) {
			if (typeMgr.getDDTTypes().iterator().hasNext()) {
				m_ddtType = (IDDTType) typeMgr.getDDTTypes().iterator().next();
			}
			else {
				return false;
			}
		}
		applyType(m_ddtType);
		return true;
	}

	/**
	 * Takes an IDDTType object and sets it as the context for the dialog.
	 *
	 * @param ddtType Type in question
	 */
	public void applyType(IDDTType ddtType)
	{

		setButtonEnablement();
		// Set the type field to reflect current type being assigned
		m_ddtTypeProperty.setValue(ddtType.getName());

		// Fix up the tables
		m_deviceFieldModel = new DeviceFieldModel(m_device, ddtType);
		m_deviceFieldTable.setModel(m_deviceFieldModel);
		m_deviceFieldTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		m_pinFieldModel = new PinFieldModel(m_device, ddtType);
		TableSorterModel pinSorterModel = new TableSorterModel(m_pinFieldModel);
		m_pinFieldTable.setModel(pinSorterModel);
		m_pinFieldTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		pinSorterModel.setupTableHeader(m_pinFieldTable);
		TableUtils.initColumnSizes(m_pinFieldTable, true,
				TableUtils.ARROW_SPACE); // Extra padding to provide space for arrows

		// Set the renderer for the pin table (for striping and the pin icon)
		DDTTableCellRenderer customRenderer = new DDTTableCellRenderer(true);
		for (int i = 0; i < m_pinFieldModel.getColumnCount(); i++) {
			TableColumn pinIconColumn = m_pinFieldTable.getColumnModel().getColumn(i);
			pinIconColumn.setCellRenderer(customRenderer);
		}

		customRenderer = new DDTTableCellRenderer(false);
		// Set the render for the device table (for striping)
		for (int i = 0; i < m_deviceFieldModel.getColumnCount(); i++) {
			TableColumn col = m_deviceFieldTable.getColumnModel().getColumn(i);
			col.setCellRenderer(customRenderer);
		}
		m_ddtType = ddtType;
	}

	/**
	 * Sets up a namespace that is local to this dialog. Upon cancel the normal name manager will not have been touched.
	 *
	 * @param dev Device to scope the pins to
	 */
	private void setupTransientNamespace(IDevice dev)
	{
		if (dev.getNumPins() > 0) {
			// Get an existing pin to find out name space info.
			IAbstractPin aPin = dev.getPins().getNext();
			INameMgr nameMgr = aPin.getNameMgr();
			INameSpace nameSpace = nameMgr.getNameSpace(aPin);
			m_currentNameIndex = nameSpace.getCurrentIndex();
			m_pinPrefix = nameSpace.getDefaultObjPrefix();
		}
		else {
			m_currentNameIndex = 0;
			INameMgr nameMgr = CAFUtils.getInstance().getCurrentProject().getNameMgr();
			m_pinPrefix = nameMgr.getObjectPrefix(INameMgr.PIN).getString();
		}
	}

	public int getDialogNameIndex()
	{
		return m_currentNameIndex;
	}

	/**
	 * Construct the dialog components.
	 *
	 * @return Property group representing the root panel of the dialog
	 */
	private IPropertyGroup buildDialog()
	{
		IPropertyGroup root = PropertyFactory.createPropertyGroup("Root");
		root.setGroupType(GroupTypeValue.LABELLED_COLUMN);

		IPropertyGroup typeGrp = root.createPropertyGroup("TypeGrp");
		String typeStr = ResourceMgr.getString(AssignDDTDialog.class, "AssignDDTDialog.typeStr.text");
		typeGrp.setLabel(typeStr);
		typeGrp.setAttribute(IPropertyAttributes.LABELLED_GROUP, Boolean.TRUE);
		typeGrp.setGroupType(GroupTypeValue.ROW);
		typeGrp.setBorder(BorderValue.NONE);

		m_ddtTypeProperty = typeGrp.createStringProperty("DeviceType");
		m_ddtTypeProperty.setAttribute(IPropertyAttributes.LABEL_TEXT, Boolean.TRUE);
		m_ddtTypeProperty.setAttribute(IPropertyAttributes.PLAIN_TEXT, Boolean.TRUE);
		m_ddtTypeProperty.setValue("VAL");

		m_ddtTypeProperty.setHorizontalFill(true);

		IActionProperty actProp = typeGrp.createActionProperty("switchTypeAction");
		String changeTypeStr = ResourceMgr.getString(AssignDDTDialog.class, "AssignDDTDialog.ChangeTypeBut.text");
		actProp.setLabel(changeTypeStr);
		actProp.setMnemonic(ResourceMgr.getMnemonic(AssignDDTDialog.class, "AssignDDTDialog.ChangeTypeBut.mnemonic"));
		actProp.setActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				IDDTTypeMgr typeMgr = CAFUtils.getInstance().getCurrentProject().getDDTTypeMgr();
				if (typeMgr != null) {
					Frame frame = CAFUtils.getInstance().getWindowMgr().getDialogFrame();
					m_typeSelectionDialog = new SelectDDTTypeDialog(frame, typeMgr);
					m_typeSelectionDialog.pack();
					m_typeSelectionDialog.setVisible(true);
					if (m_typeSelectionDialog.wasValidated()) {
						IDDTType chosenType = m_typeSelectionDialog.getSelectedType();
						applyType(chosenType);
					}
				}
			}
		});

		createDeviceFieldTable(root);
		createPinFieldTable(root);
		root.setFill(OrientationValue.NONE);
		root.setAttribute(IPropertyAttributes.PREFERRED_SIZE, new Dimension(600, 480));

		return root;
	}

	private void setButtonEnablement()
	{
		if (m_delPinBut != null) {
			m_delPinBut
					.setEnabled(!m_hasSymbol && m_pinFieldTable != null && m_pinFieldTable.getSelectedRowCount() > 0);
		}
		if (m_newPinBut != null) {
			m_newPinBut.setEnabled(!m_hasSymbol && !m_hasLibraryRef);
		}
	}

	/**
	 * Creates the pin field table at the bottom of the dialog
	 *
	 * @param root Property group to add the pin field table to
	 */
	private void createPinFieldTable(IPropertyGroup root)
	{
		// Group holding table, and buttons
		IPropertyGroup pinGrp = root.createPropertyGroup("PinGrp");
		pinGrp.setLabel("Pins");
		pinGrp.setGroupType(GroupTypeValue.COLUMN);
		pinGrp.setFill(OrientationValue.BOTH);

		// Table
		IComponentProperty pinProp = pinGrp.createComponentProperty("PinFields");
		pinProp.setLabel("Pin Fields");
		m_pinFieldTable = new JTable();
		m_pinFieldTable.setName("pinfieldtable");
		m_pinFieldTable.getTableHeader().setReorderingAllowed(false);
		m_pinFieldTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		m_pinFieldTable.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				setButtonEnablement();
			}
		});

		pinProp.setFill(OrientationValue.BOTH);
		pinProp.setValue(new JScrollPane(m_pinFieldTable));

		// Button group (new & delete pins)
		IPropertyGroup pinButGroup = pinGrp.createPropertyGroup("PinBtnGrp");
		pinButGroup.setBorder(BorderValue.NONE);
		pinButGroup.setGroupType(GroupTypeValue.ROW);
		pinButGroup.setFill(OrientationValue.HORIZONTAL);
		m_newPinBut = pinButGroup.createActionProperty("NewPinBut");
		String newPinStr = ResourceMgr.getString(AssignDDTDialog.class, "AssignDDTDialog.NewPinBut.text");
		m_newPinBut.setLabel(newPinStr);
		m_newPinBut.setMnemonic(ResourceMgr.getMnemonic(AssignDDTDialog.class, "AssignDDTDialog.NewPinBut.mnemonic"));
		m_newPinBut.setActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				Frame diagFrame = CAFUtils.getInstance().getWindowMgr().getDialogFrame();
				PinNumberDialog pnd = new PinNumberDialog(diagFrame);
				pnd.pack();
				pnd.setVisible(true);
				if (pnd.wasCanceled() == false) {
					int numPins = pnd.getNumPins();
					createSomePins(numPins);
				}
			}
		});

		m_delPinBut = pinButGroup.createActionProperty("DelPinBut");
		String deletePinStr = ResourceMgr.getString(AssignDDTDialog.class, "AssignDDTDialog.RemovePinBut.text");
		m_delPinBut
				.setMnemonic(ResourceMgr.getMnemonic(AssignDDTDialog.class, "AssignDDTDialog.DeletePinBut.mnemonic"));
		m_delPinBut.setLabel(deletePinStr);
		m_delPinBut.setActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				int selRows[] = m_pinFieldTable.getSelectedRows();
				m_pinFieldModel.deleteRows(selRows);
			}
		});

		setButtonEnablement();

		pinGrp.setFill(OrientationValue.BOTH);
		pinGrp.setVerticalWeight(.7);
	}

	private void createDeviceFieldTable(IPropertyGroup root)
	{
		IPropertyGroup devGrp = root.createPropertyGroup("DeviceGrp");
		devGrp.setLabel("Device");
		devGrp.setGroupType(GroupTypeValue.COLUMN);
		IComponentProperty devProp = devGrp.createComponentProperty("DeviceFields");
		devProp.setLabel("Device Fields");

		m_deviceFieldTable = new JTable();
		m_deviceFieldTable.setName("devicefieldtable");
		m_deviceFieldTable.getTableHeader().setReorderingAllowed(false);
		TableUtils.initColumnSizes(m_deviceFieldTable, false);
		devProp.setValue(new JScrollPane(m_deviceFieldTable));
		devProp.setFill(OrientationValue.BOTH);

		devGrp.setFill(OrientationValue.BOTH);
		devGrp.setVerticalWeight(.3);
	}

	/**
	 * Add some pins to our dialog.
	 *
	 * @param numPins
	 */
	private void createSomePins(int numPins)
	{

		for (int i = 0; i < numPins; i++) {
			String newPinName = m_pinPrefix + m_currentNameIndex;
			m_currentNameIndex++;
			m_pinFieldModel.addRow(newPinName);
		}
	}

	class DDTTableCellRenderer extends StripingTableCellRenderer
	{

		private boolean m_pinMode; // Renderer for pins?  If not, must be devices.

		public DDTTableCellRenderer(boolean forPins)
		{
			m_pinMode = forPins;
		}

		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus, int row, int column)
		{
			Component comp = super.getTableCellRendererComponent(table, value, isSelected,
					hasFocus, row, column);

			Font font = UIUtils.getFont(this);
			setFont(font);
			return comp;
		}
	}
}
