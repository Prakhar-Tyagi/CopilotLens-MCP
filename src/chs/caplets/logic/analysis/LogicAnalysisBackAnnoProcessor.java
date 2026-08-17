/*
 * Copyright 2004-2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.analysis;

import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.cafmain.actions.CAFCommandListener;
import chs.caf.cafmain.actions.analysis.AnalysisBackAnnotationListener;
import chs.caf.cafmain.actions.analysis.SubsystemStressAction;
import chs.caplets.analysis.AbstractAnalysisBackAnnotationProcessor;
import chs.caplets.cmd.AnalysisBackAnnotateCmd;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IWireConductor;
import chs.cofUtils.cmd.CommandHelper;
import chs.common.IDesignContainer;
import chs.common.IReadOnlyNamedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.ctf.caf.ui.CAFOkCancelDialog;
import chs.utilities.MapMap;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author rharring
 */
public class LogicAnalysisBackAnnoProcessor extends AbstractAnalysisBackAnnotationProcessor
{

	// /////////////// //
	// Class variables //
	// /////////////// //

	/**
	 * This represents the apply to all selection
	 */
	public static final int APPLY_TO_ALL = 0;

	/**
	 * This represents the apply to wires selection
	 */
	public static final int APPLY_TO_WIRES = 1;

	/**
	 * This represents the apply to fuses selection
	 */
	public static final int APPLY_TO_FUSES = 2;

	/**
	 * This represents the apply to filter selection
	 */
	public static final int APPLY_TO_FILTER = 3;

	// ////////////////// //
	// Instance variables //
	// ////////////////// //

	/**
	 * The column name to be modified
	 */
	protected String m_columnName;

	/**
	 * A filter to identify the objects to process
	 */
	protected String m_filter;

	/**
	 * The regex pattern for filtering
	 */
	protected Pattern m_pattern;

	/**
	 * A list containing those object which were skipped during processing
	 */
	protected List<String> m_skippedList;

	/**
	 * An int indicating the type of components to apply to
	 */
	protected int m_applyTo;

	/**
	 * Have we warned the user the report being used to back propagate does not match this design ?
	 */
	protected boolean warnedUserOfInvalidReport;

	// //////////// //
	// Constructors //
	// //////////// //

	/**
	 * Creates a new instance of LogicAnalysisBackAnnoProcessor
	 */
	public LogicAnalysisBackAnnoProcessor()
	{
		super();
		m_columnName = null;
		m_filter = null;
		m_pattern = null;
		m_propertyMap = new HashMap<String,Map<String,String>>();
		m_skippedList = new ArrayList<String>(5);
		m_applyTo = APPLY_TO_WIRES;
		warnedUserOfInvalidReport = false;
	}

	// ////////////////// //
	// IBackAnnoProcessor //
	// ////////////////// //

	public void postProcess()
	{
		if (!m_skippedList.isEmpty()) {
			JList list = new JList(m_skippedList.toArray());
			// don't want it to be huge, but we do want it to be usable
			list.setVisibleRowCount(m_skippedList.size() < 10 ? 4 : 8);
			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout());
			JScrollPane pane = new JScrollPane(list);
			panel.add(pane, BorderLayout.CENTER);

			chs.utilities.ui.MessageHelper.showInformationMessage(CAFUtils.getInstance().getDialogFrame(),
					ResourceMgr.getString(LogicAnalysisBackAnnoProcessor.class, "SkippedObjects.title"),
					ResourceMgr.getString(LogicAnalysisBackAnnoProcessor.class, "SkippedObjects.message"), panel);
		}
	}

	public boolean preProcess(String selColumn)
	{

		m_columnName = selColumn;

		Frame owner = CAFUtils.getInstance().getWindowMgr().getDialogFrame();
		PropertySelectionDialog dialog = new PropertySelectionDialog(owner, m_columnName);
		dialog.setVisible(true);
		if (dialog.isCancelled()) {
			return false;
		}

		// get the property name
		m_propName = dialog.getPropName();
		if (m_propName == null || m_propName.isEmpty()) {
			return false;
		}

		// get the components to apply to.
		m_applyTo = dialog.getApplyToSelection();

		m_filter = dialog.getRegexFilter();
		m_pattern = null; // must ensure we're not using a filter set previously...
		setPropertyValueMap(m_propName);

		m_skippedList.clear();

		// everything ok...
		return true;
	}

	public void processComponent(String uid, String value)
	{
		processObject(uid, value);
	}

	public void processWire(String uid, String value)
	{
		processObject(uid, value);
	}

	// ///////////// //
	// Other methods //
	// ///////////// //

	/**
	 * This method handles processing an object from the given uid with the given value.
	 *
	 * @param uid, the uid of the object to process
	 * @param value, the value to attach
	 */
	public void processObject(String uid, String value)
	{
		if (StringUtils.isBlank(value)) {
			return;
		}
		if (m_filter != null && m_pattern == null) {

			// create the regex pattern
			m_pattern = Pattern.compile(m_filter);
		}

		if (isApplicable(uid)) {
			uidStringPropertyValMap.put(uid, value);
		}
	}

	/**
	 * This method is called to apply the edits to the components
	 *
	 * @param apply, if true the edits will be applied.
	 */
	public void applyEdits(boolean apply)
	{
		// clear the flag stating whether we have warned the user of an
		// incompatible report.
		warnedUserOfInvalidReport = false;
		if (apply && !m_propertyMap.isEmpty() && !m_propertyMap.values().isEmpty()) {
			MapMap<IDesignContainer, IUID, Map<String, String>> backAnnotationsToApply =
					getProcessedObjectPropertyMap();

			if ( backAnnotationsToApply != null && !backAnnotationsToApply.isEmpty()) {
				AnalysisBackAnnotationListener listener = new AnalysisBackAnnotationListener();

				CommandHelper cmdHelper = new CAFCommandHelper();

				@SuppressWarnings({"ConstantConditions", "unchecked"})
				final AnalysisBackAnnotateCmd cmd = new AnalysisBackAnnotateCmd(cmdHelper,
						backAnnotationsToApply.keySet().iterator().next().getProject(), Arrays
						.<Class<? extends IDesignContainer>>asList(ILogicDesign.class));

				cmd.setCommandListener(listener);

				cmd.setScopeToProcess(backAnnotationsToApply.keySet());


				cmd.setDesignObjectsAndPropertiesToBackAnnotate(backAnnotationsToApply);

				CAFCommandListener
						.executeCommandWithProgressDlg(cmd, SubsystemStressAction.class, listener.getProgress(), cmd);
			}

			m_propertyMap.clear();
		}
		// clear the flag stating whether we have warned the user of an
		// incompatible report.
		warnedUserOfInvalidReport = false;
	}

	/**
	 * This method determines whether setting a value on this uid is applicable given the current setting of m_applyTo
	 *
	 * @param uid, the string rep of the uid
	 */
	protected boolean isApplicable(String uid)
	{

        IUIDObject object = getUIDObject(uid);

		// ensure we don't touch any shared objects that are frozen...
		if (object instanceof ILogicObject) {
			ILogicObject logObj = (ILogicObject) object;
			if (logObj.getSharedObject() != null &&
					logObj.getSharedObject().isFrozen()) {
				m_skippedList.add(logObj.getName());
				return false;
			}
		}

		if (m_applyTo == APPLY_TO_ALL) {
			return true;
		}

		if (m_applyTo == APPLY_TO_FILTER &&
				object instanceof IReadOnlyNamedObject) {
			if (m_pattern.matcher(((IReadOnlyNamedObject) object).getName()).matches()) {
				return true;
			}
		}

		return m_applyTo == APPLY_TO_WIRES &&
				object instanceof IWireConductor;
	}

	@Override public void reset()
	{
		if(m_propertyMap != null){
			m_propertyMap.clear();
		}
	}

	// ///////////// //
	// Other classes //
	// ///////////// //

	/**
	 * This class represents the dialog shown to the user to allow them to change the name of the property to apply and
	 * select upon which components this property should be applied.
	 */
	private static class PropertySelectionDialog extends CAFOkCancelDialog
	{

		private String m_propName;
		private int m_applyToSelection;
		private JTextField propTextField;
		private JTextField regexField;
		private JRadioButton wiresButton;
		private JRadioButton allButton;
		private JRadioButton fuseButton;
		private JRadioButton filterButton;


		private PropertySelectionDialog(Frame owner, String propName)
		{
			super(owner,
					ResourceMgr.getString(LogicAnalysisBackAnnoProcessor.class, "PropertySelectionDialog.title"),
					true);
			doSetResizable(false);
			m_propName = propName;
			m_applyToSelection = APPLY_TO_WIRES;
			createGUI();
			initGUI();
		}

		protected void createGUI()
		{
			JPanel mainPanel = new JPanel();
			mainPanel.setLayout(new BorderLayout());

			// to the north add a textfield
			JPanel northPanel = new JPanel();
			northPanel.setLayout(new BorderLayout());
			propTextField = new JTextField(20);
			propTextField.setName("PushbackPropertyTextField");
			northPanel.add(propTextField, BorderLayout.CENTER);
			northPanel.setBorder(BorderFactory.createTitledBorder(
					ResourceMgr.getString(LogicAnalysisBackAnnoProcessor.class, "PropertySelectionDialog.textLabel")));

			// to the center add a panel containing the 3 radio buttons
			JPanel centerPanel = new JPanel();
			centerPanel.setLayout(new GridLayout(4, 1));
			ButtonGroup radioGroup = new ButtonGroup();
			wiresButton = new JRadioButton(
					ResourceMgr.getString(LogicAnalysisBackAnnoProcessor.class, "PropertySelectionDialog.wiresRadio"));
			wiresButton.setMnemonic(
					ResourceMgr.getChar(LogicAnalysisBackAnnoProcessor.class,
							"PropertySelectionDialog.wiresRadio.mnemonic"));
			radioGroup.add(wiresButton);
			allButton = new JRadioButton(
					ResourceMgr.getString(LogicAnalysisBackAnnoProcessor.class, "PropertySelectionDialog.allRadio"));
			allButton.setMnemonic(
					ResourceMgr.getChar(LogicAnalysisBackAnnoProcessor.class,
							"PropertySelectionDialog.allRadio.mnemonic"));
			radioGroup.add(allButton);
			fuseButton = new JRadioButton(
					ResourceMgr.getString(LogicAnalysisBackAnnoProcessor.class, "PropertySelectionDialog.fuseRadio"));
			fuseButton.setMnemonic(
					ResourceMgr.getChar(LogicAnalysisBackAnnoProcessor.class,
							"PropertySelectionDialog.fuseRadio.mnemonic"));
			radioGroup.add(fuseButton);
			filterButton = new JRadioButton(
					ResourceMgr.getString(LogicAnalysisBackAnnoProcessor.class, "PropertySelectionDialog.filterRadio"));
			filterButton.setMnemonic(
					ResourceMgr.getChar(LogicAnalysisBackAnnoProcessor.class,
							"PropertySelectionDialog.filterRadio.mnemonic"));
			radioGroup.add(filterButton);

			regexField = new JTextField(20);
			regexField.setEnabled(false);
			regexField.setName("RegexField");
			JPanel regexPanel = new JPanel();
			regexPanel.add(regexField);

			filterButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					regexField.setEnabled(filterButton.isSelected());
				}
			});

			centerPanel.add(wiresButton);
			//centerPanel.add( fuseButton ) ;
			centerPanel.add(allButton);
			centerPanel.add(filterButton);
			centerPanel.add(regexPanel);

			centerPanel.setBorder(BorderFactory.createTitledBorder(
					ResourceMgr.getString(LogicAnalysisBackAnnoProcessor.class, "PropertySelectionDialog.radioLabel")));

			mainPanel.add(northPanel, BorderLayout.NORTH);
			mainPanel.add(centerPanel, BorderLayout.CENTER);
			getContentPane().add(mainPanel);

			pack();
		}

		public void initGUI()
		{
			wiresButton.setSelected(true);
			propTextField.setText(m_propName);
			regexField.setText(".*");

			setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
					setCancelled(true);
					dispose();
				}
			});
			getOkButton().addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					setCancelled(false);
					m_propName = propTextField.getText();
					if (wiresButton.isSelected()) {
						m_applyToSelection = APPLY_TO_WIRES;
					}
					else if (fuseButton.isSelected()) {
						m_applyToSelection = APPLY_TO_FUSES;
					}
					else if (filterButton.isSelected()) {
						m_applyToSelection = APPLY_TO_FILTER;
					}
					else {
						m_applyToSelection = APPLY_TO_ALL;
					}
					dispose();
				}
			});
			getCancelButton().addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					setCancelled(true);
					dispose();
				}
			});
		}

		// ///////// //
		// Accessors //
		// ///////// //

		public String getPropName()
		{
			return m_propName;
		}

		public int getApplyToSelection()
		{
			return m_applyToSelection;
		}

		@Nullable public String getRegexFilter()
		{
			if (regexField.isEnabled()) {
				return regexField.getText();
			}
			return null;
		}
	}
	public boolean isComponentFromSlot()
	{
		return false;
	}
}
