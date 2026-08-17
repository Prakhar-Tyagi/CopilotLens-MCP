/*
 * Copyright 2003-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.shared.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.symbol.Model;
import chs.cof.draw.IGrid;
import chs.cof.draw.IGriddable;
import chs.cof.symbol.IBorder;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolFactory;
import chs.cof.symbol.IZoneArea;
import chs.cof.symbol.IZoneAreaObject;
import chs.cof.symbol.ZoneArea;
import chs.common.ICommonFactory;
import chs.common.IExtent;
import chs.common.IUID;
import chs.common.IUnit;
import chs.common.UnitTypeEnum;
import chs.ctf.caf.ui.CAFOkCancelDialog;
import chs.ctf.caf.ui.UnitChooser;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.PreciseDecimalFormat;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.MessageHelper;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.GridHelper;
import chs.utility.helpers.ZoneHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;

/**
 * ModifyZoneAreaAction is an action used to modify the zoneArea associated to a border
 * <p/>
 * User: pwijaya Date: Oct 15, 2003 Time: 12:10:46 PM
 */
public class ModifyZoneAreaAction extends ControllerActionRT implements ICtxMenuProvider
{

	private static final DecimalFormat m_heightFormat = new PreciseDecimalFormat("##0.####");
	private static final float DEFAULT_TEXT_HEIGHT = 0.1875F;    // default text height
	private static final String NUMERICAL =
			ResourceMgr.getString(ModifyZoneAreaAction.class, "ModifyZoneAreaAction.Naming.Numerical");
	private static final String ALPHABETICAL =
			ResourceMgr.getString(ModifyZoneAreaAction.class, "ModifyZoneAreaAction.Naming.Alphabetical");
	private static final String BOTTOM_TOP_STRING =
			ResourceMgr.getString(ModifyZoneAreaAction.class, "ModifyZoneAreaAction.RowNumbering.BottomToTop");
	private static final String TOP_BOTTOM_STRING =
			ResourceMgr.getString(ModifyZoneAreaAction.class, "ModifyZoneAreaAction.RowNumbering.TopToBottom");
	private static final String LEFT_RIGHT_STRING =
			ResourceMgr.getString(ModifyZoneAreaAction.class, "ModifyZoneAreaAction.ColumnNumbering.LeftToRight");
	private static final String RIGHT_LEFT_STRING =
			ResourceMgr.getString(ModifyZoneAreaAction.class, "ModifyZoneAreaAction.ColumnNumbering.RightToLeft");
	private static final int DEFAULT_NUM_ROWS = 4;
	private static final int DEFAULT_NUM_COLUMNS = 4;

	// Keep the ZoneAreaDialog
	private ZoneAreaDialog m_zoneAreaDialog;
	private Model m_model;

	public ModifyZoneAreaAction(ICapletController controller)
	{
		super(controller);
		m_model = (Model) controller.getCapletModel();
	}

	/**
	 * Done modification - edit the model
	 *
	 * @return boolean
	 */
	private boolean editModel()
	{
		// Get the active border
		IStamp activeBorder = m_model.getSymbolDef();
		if (activeBorder != null) {
			if (activeBorder instanceof IZoneAreaObject) {
				// Get all the information
				boolean useZoneArea = m_zoneAreaDialog.getUseZoneArea();
				IZoneAreaObject border = (IZoneAreaObject) activeBorder;
				if (!useZoneArea) {   // Set to not using the zone area
					// Set the Border's ZoneArea to null
					border.setZoneAreaAndCreateGraphics(null);
				}
				else {    // Create a new zone Area
					int numOfRows = m_zoneAreaDialog.getRow();
					int numOfColumns = m_zoneAreaDialog.getColumn();
					int rowNaming = m_zoneAreaDialog.getRowNaming();
					int colNaming = m_zoneAreaDialog.getColumnNaming();
					int rowNumbering = m_zoneAreaDialog.getRowNumbering();
					int colNumbering = m_zoneAreaDialog.getColNumbering();
					boolean rowZeroStart = m_zoneAreaDialog.getRowZeroStart();
					boolean colZeroStart = m_zoneAreaDialog.getColZeroStart();
					int textHeight = m_zoneAreaDialog.getTextHeight();
					String excludedChars = m_zoneAreaDialog.getExcludedCharacters();

					IZoneArea currZoneArea = border.getZoneArea();
					IZoneArea zoneArea = currZoneArea;
					if (zoneArea == null) {
						// Create a ZoneArea
						ISymbolFactory symbolFac = FactoryMgr.getSymbolFactory();
						IUID zaUID = FactoryMgr.getCommonFactory().createUID();
						zoneArea = symbolFac.constructZoneArea(zaUID);
					}

					boolean regenerateZoneExtents = currZoneArea == null || currZoneArea.getNumRows() != numOfRows ||
							currZoneArea.getNumColumns() != numOfColumns;

					// Set the ZoneArea's parameters
					zoneArea.setParameters(numOfRows, numOfColumns, rowNaming, colNaming, rowNumbering, colNumbering,
							rowZeroStart, colZeroStart, excludedChars);

					zoneArea.setTextHeight(textHeight);
					zoneArea.setTextHeightUnit(m_zoneAreaDialog.getTextHeightUnit().toString());

					// Set the Border's ZoneArea, and generate its graphics
					border.setZoneAreaAndCreateGraphics(zoneArea);
				}

				// Refresh the active view
				CAFUtils.getInstance().getActiveCapletView().invalidate(IViewInvalidationEnum.eFull);
			}
		}
		CreationDeletionHelper.getTheCreationHelper().processObjects();
		return true;
	}

	/**
	 * Start the action.  If the action return eActivated it will start receiving events.
	 */
	public IActionEnum onActivate(ActionEvent e)
	{
		// Return complete
		return IActionEnum.eCompleted;
	}

	/**
	 * Stop the Action.  If the successful paramater is true then apply the edits to the model.
	 */
	public boolean onTerminate(boolean successful)
	{
		// Get the active border
		IStamp border = m_model.getSymbolDef();
		IZoneArea zoneArea = null;
		if (border instanceof IZoneAreaObject) {    // Get the ZoneArea if there is one
			zoneArea = ((IZoneAreaObject) border).getZoneArea();
		}

		m_zoneAreaDialog = new ZoneAreaDialog(CAFUtils.getInstance().getWindowMgr().getDialogFrame(), zoneArea);
		m_zoneAreaDialog.setVisible(true);
		if (m_zoneAreaDialog.isCancelled()) {
			return false;
		}

		return editModel();
	}

	public String getActionUIClass()
	{
		return ModifyZoneAreaActionUI.class.getName();
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(ModifyZoneAreaAction.class, "ModifyZoneAreaAction.StatusBar.Msg");
	}

	/**
	 * The ZoneAreaDialog
	 */
	private class ZoneAreaDialog extends CAFOkCancelDialog implements ItemListener
	{

		private IZoneArea m_zoneArea;
		private JTextField m_rowTextField;
		private JTextField m_colTextField;
		private JTextField m_exclusionsField;

		private JRadioButton m_rowNameNone;
		private JRadioButton m_rowNameNumerical;
		private JRadioButton m_rowNameAlphabetical;
		private JRadioButton m_rowNumNone;
		private JRadioButton m_rowNumBtoT;
		private JRadioButton m_rowNumTtoB;
		private JRadioButton m_rowStartWNone;
		private JRadioButton m_rowStartW0;
		private JRadioButton m_rowStartW1;

		private JRadioButton m_colNameNone;
		private JRadioButton m_colNameNumerical;
		private JRadioButton m_colNameAlphabetical;
		private JRadioButton m_colNumNone;
		private JRadioButton m_colNumLtoR;
		private JRadioButton m_colNumRtoL;
		private JRadioButton m_colStartWNone;
		private JRadioButton m_colStartW0;
		private JRadioButton m_colStartW1;

		protected UnitChooser m_heightUnitChooser;
		private JCheckBox m_useZoneAreaCB;
		@Nullable private JLabel m_userZoneLossWarning = null;

		public ZoneAreaDialog(Frame owner, IZoneArea zoneArea)
		{
			super(owner, ResourceMgr.getString(ModifyZoneAreaAction.class, "ModifyZoneAreaAction.EditZoneArea.Text"),
					true);

			setMinimumSize(new Dimension(540, 430));
			setMaximumSize(new Dimension(540, 430));
			m_zoneArea = zoneArea;
			buildDialog(zoneArea);
			pack();
		}

		/**
		 * Build the dialog
		 *
		 * @param zoneArea
		 */
		private void buildDialog(IZoneArea zoneArea)
		{
			setupWindowListener();
			setupOKCancelButtons();

			doSetResizable(false);

			int hGap = 5;    // Horizontal gap between component
			int vGap = 5;    // vertical gap between component

			// Create the grid bag constraints
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets = new Insets(3, 3, 3, 0);
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 0.0D;
			gbc.weighty = 1.0D;

			JPanel mainPanel = new JPanel();
			mainPanel.setLayout(new GridBagLayout());
			mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

			// Set up the ZoneArea Panel
			JPanel zoneAreaPanel = new JPanel();
			zoneAreaPanel.setLayout(new GridBagLayout());
			zoneAreaPanel.setBorder(BorderFactory.createTitledBorder(
					ResourceMgr.getString(ModifyZoneAreaAction.class, "ModifyZoneAreaAction.TitledBorder.text")));

			// UseZoneArea
			boolean useZoneArea = true;
			m_useZoneAreaCB = new JCheckBox(
					ResourceMgr.getString(ModifyZoneAreaAction.class, "ModifyZoneAreaAction.UseZoneArea.Label"),
					useZoneArea);
			m_useZoneAreaCB.setName("m_useZoneAreaCB");
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridwidth = 7;
			zoneAreaPanel.add(m_useZoneAreaCB, gbc);
			gbc.gridwidth = 2;
			//
			// Rows & Columns Panel
			// Row Panel
			JPanel rowPanel = new JPanel();
			rowPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 3));
			JLabel rowLabel = new JLabel(
					ResourceMgr.getStringForLabel(ModifyZoneAreaAction.class, "ModifyZoneAreaAction.rowLabel.text"));
			m_rowTextField = new JTextField(3);
			// Set it to be similar to the UnitChooser
			m_rowTextField.setPreferredSize(new Dimension(10 * m_rowTextField.getColumns(), 25));
			m_rowTextField.setName("m_rowTextField");
			NumberRowsColumnsDocument rowTextDocument = new NumberRowsColumnsDocument();
			m_rowTextField.setDocument(rowTextDocument);
			// Get the value from zoneArea
			if (zoneArea != null) {
				m_rowTextField.setText(String.valueOf(zoneArea.getNumRows()));
			}
			else {
				m_rowTextField.setText(String.valueOf(DEFAULT_NUM_ROWS));
			}
			rowPanel.add(rowLabel);
			rowPanel.add(m_rowTextField);
			gbc.gridwidth = 1;
			gbc.gridy = 1;
			zoneAreaPanel.add(rowPanel, gbc);

			// Column Panel
			JLabel colLabel = new JLabel(
					ResourceMgr.getStringForLabel(ModifyZoneAreaAction.class, "ModifyZoneAreaAction.colLabel.text"));
			m_colTextField = new JTextField(3);
			m_colTextField.setName("m_colTextField");
			// Set it to be similar to the UnitChooser
			m_colTextField.setPreferredSize(new Dimension(10 * m_colTextField.getColumns(), 25));
			NumberRowsColumnsDocument colTextDocument = new NumberRowsColumnsDocument();
			m_colTextField.setDocument(colTextDocument);
			// Get the value from zoneArea
			if (zoneArea != null) {
				m_colTextField.setText(String.valueOf(zoneArea.getNumColumns()));
			}
			else {
				m_colTextField.setText(String.valueOf(DEFAULT_NUM_COLUMNS));
			}

			gbc.gridx = 1;
			zoneAreaPanel.add(new JLabel(), gbc);

			gbc.gridx = 2;
			zoneAreaPanel.add(colLabel, gbc);
			gbc.gridx = 3;
			zoneAreaPanel.add(m_colTextField, gbc);

			gbc.gridx = 4;
			JPanel tempSpacePanel = new JPanel();
			tempSpacePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 25, 3));
			tempSpacePanel.add(new JLabel());
			zoneAreaPanel.add(tempSpacePanel, gbc);

			// Text Height UnitChooser Panel
			IUnit unit = FactoryMgr.getCommonFactory().createUnit();
			UnitTypeEnum heightUnitType = UnitTypeEnum.TypeInch;    // Default is inch
			GfxView view = (GfxView) CAFUtils.getInstance().getActiveCapletView();
			IGrid grid = ((IGriddable) view.getSheet()).getGrid();
			int textHeight = 0;
			if (zoneArea == null) {
				textHeight = getDefaultTextHeight();
			}
			else {    // Get the zoneArea Height
				textHeight = zoneArea.getTextHeight();
				heightUnitType = zoneArea.getTextHeightUnit();
				if (textHeight < 0) {    // The zoneArea has no idea about its textHeight - use the default
					textHeight = getDefaultTextHeight();
				}
			}
			// Convert the text height to the unit used
			double unitHeight = GridHelper.assignUnit(unit, grid, textHeight, heightUnitType).getValue();
			m_heightUnitChooser = new UnitChooser(unitHeight, heightUnitType, m_heightFormat);

			JLabel textHeightLabel = new JLabel(ResourceMgr.getStringForLabel(ModifyZoneAreaAction.class,
					"ModifyZoneAreaAction.textHeightLabel.text"));
			gbc.gridx = 5;
			zoneAreaPanel.add(textHeightLabel, gbc);
			gbc.gridx = 6;
			zoneAreaPanel.add(m_heightUnitChooser.getComponent(), gbc);
			gbc.gridx = 7;
			gbc.weightx = 0.10D;
			zoneAreaPanel.add(new JLabel(""), gbc);
			gbc.weightx = 0.0D;

			// Excluded characters
			JPanel excludePanel = new JPanel();
			excludePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 3));
			JLabel exclusionsLabel = new JLabel(
					ResourceMgr.getStringForLabel(ModifyZoneAreaAction.class,
							"ModifyZoneAreaAction.exclusionsLabel.text"));
			m_exclusionsField = new JTextField(27);
			m_exclusionsField.setName("m_exclusionsField");
			m_exclusionsField.setPreferredSize(new Dimension(150, 25));
			String excludeChars = "";
			if (zoneArea != null) {
				excludeChars = StringUtils.convertCollectionToString(zoneArea.getExcludedChars(),
						IZoneArea.EXCLUDED_CHARS_SEPARATOR);
			}
			m_exclusionsField.setText(excludeChars);
			m_exclusionsField.setToolTipText(ResourceMgr
					.getString(ModifyZoneAreaAction.class, "ModifyZoneAreaAction.exclusionsLabel.tooltip.text"));
			gbc.gridx = 0;
			gbc.gridy = 2;
			gbc.gridwidth = 3;
			excludePanel.add(exclusionsLabel);
			zoneAreaPanel.add(excludePanel, gbc);
			gbc.gridx = 3;
			gbc.gridwidth = 4;
			zoneAreaPanel.add(m_exclusionsField, gbc);

			GridBagConstraints mainGBC = new GridBagConstraints();
			mainGBC.weightx = 1.0D;
			mainGBC.weighty = 1.0D;
			mainGBC.gridx = 0;
			mainGBC.gridy = 0;
			mainGBC.fill = GridBagConstraints.BOTH;
			mainPanel.add(zoneAreaPanel, mainGBC);

			//
			// Set up Row Naming Panel
			JPanel rowNamingPanel = new JPanel();
			rowNamingPanel.setBorder(BorderFactory.createTitledBorder(
					ResourceMgr.getString(ModifyZoneAreaAction.class, "ModifyZoneAreaAction.TitledBorder.text_1")));
			rowNamingPanel.setLayout(new BorderLayout(hGap, vGap));

			// Setup Row Naming Content Panel
			JPanel rowNamingContentPanel = new JPanel();
			rowNamingContentPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
			rowNamingContentPanel.setLayout(new GridLayout(3, 3));
			// Create all the radio buttons
			m_rowNumNone = new JRadioButton("None");
			m_rowNumBtoT = new JRadioButton(BOTTOM_TOP_STRING);
			m_rowNumBtoT.setName("m_rowNumBtoT");
			m_rowNumTtoB = new JRadioButton(TOP_BOTTOM_STRING);
			m_rowNumTtoB.setName("m_rowNumTtoB");

			m_rowNameNone = new JRadioButton("None");
			m_rowNameNumerical = new JRadioButton(NUMERICAL);
			m_rowNameNumerical.setName("m_rowNameNumerical");
			m_rowNameAlphabetical = new JRadioButton(ALPHABETICAL);
			m_rowNameAlphabetical.setName("m_rowNameAlphabetical");

			m_rowStartWNone = new JRadioButton("None");
			m_rowStartW0 = new JRadioButton("0");
			m_rowStartW0.setName("m_rowStartW0");
			m_rowStartW1 = new JRadioButton("1");
			m_rowStartW1.setName("m_rowStartW1");

			// Set the naming action listener
			m_rowNameNumerical.addItemListener(this);
			m_rowNameAlphabetical.addItemListener(this);

			// First add Numbering components
			addNumberingComponents(rowNamingContentPanel, true, zoneArea, IZoneArea.BOTTOM_TOP);

			// Add Naming components
			addNamingComponents(rowNamingContentPanel, true, zoneArea, IZoneArea.NUMERICAL_ZONE);

			// Add start with components
			boolean defaultStartWithZero = false;
			addStartWithComponents(rowNamingContentPanel, true, zoneArea, defaultStartWithZero);

			rowNamingPanel.add(rowNamingContentPanel, BorderLayout.CENTER);
			mainGBC.insets = new Insets(5, 0, 0, 0);
			mainGBC.gridy = 1;
			mainPanel.add(rowNamingPanel, mainGBC);

			//
			// Setup Column Naming Panel
			JPanel colNamingPanel = new JPanel();
			colNamingPanel.setBorder(BorderFactory.createTitledBorder(
					ResourceMgr.getString(ModifyZoneAreaAction.class, "ModifyZoneAreaAction.TitledBorder.text_2")));
			colNamingPanel.setLayout(new BorderLayout(hGap, vGap));

			// Setup Row Naming Content Panel
			JPanel colNamingContentPanel = new JPanel();
			colNamingContentPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
			colNamingContentPanel.setLayout(new GridLayout(3, 3));
			// Create all the radio buttons
			m_colNumNone = new JRadioButton("None");
			m_colNumLtoR = new JRadioButton(LEFT_RIGHT_STRING);
			m_colNumLtoR.setName("m_colNumLtoR");
			m_colNumRtoL = new JRadioButton(RIGHT_LEFT_STRING);
			m_colNumRtoL.setName("m_colNumRtoL");

			m_colNameNone = new JRadioButton("None");
			m_colNameNumerical = new JRadioButton(NUMERICAL);
			m_colNameNumerical.setName("m_colNameNumerical");
			m_colNameAlphabetical = new JRadioButton(ALPHABETICAL);
			m_colNameAlphabetical.setName("m_colNameAlphabetical");

			m_colStartWNone = new JRadioButton("None");
			m_colStartW0 = new JRadioButton("0");
			m_colStartW0.setName("m_colStartW0");
			m_colStartW1 = new JRadioButton("1");
			m_colStartW1.setName("m_colStartW1");

			// Set the naming action listener
			m_colNameNumerical.addItemListener(this);
			m_colNameAlphabetical.addItemListener(this);

			// First add Numbering components
			addNumberingComponents(colNamingContentPanel, false, zoneArea, IZoneArea.LEFT_RIGHT);

			// Add Naming components
			addNamingComponents(colNamingContentPanel, false, zoneArea, IZoneArea.ALPHABETICAL_ZONE);

			// Add start with components
			defaultStartWithZero = false;
			addStartWithComponents(colNamingContentPanel, false, zoneArea, defaultStartWithZero);

			colNamingPanel.add(colNamingContentPanel, BorderLayout.CENTER);
			mainGBC.gridy = 2;
			mainPanel.add(colNamingPanel, mainGBC);

			// Set the listener for useZoenArea checkBox
			m_useZoneAreaCB.addItemListener(new ItemListener()
			{
				public void itemStateChanged(ItemEvent e)
				{
					if (e.getStateChange() == ItemEvent.SELECTED) {   // Selected - enable all fields
						useZoneAreaSelected(true);
					}
					else {   // Deselected - disable all fields
						useZoneAreaSelected(false);
					}
				}
			});

			//dts0100264265 Zone Area - 'Use Zone Area' is always selected.
			// needs to be un-selected if no zone area in use
			if (zoneArea == null) {
				m_useZoneAreaCB.setSelected(false);
				useZoneAreaSelected(false);
			}

			JPanel combinedPanel = new JPanel(new GridBagLayout());

			gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbc.fill = GridBagConstraints.BOTH;
			combinedPanel.add(mainPanel, gbc);

			if (m_zoneArea != null && m_zoneArea.hasUserDefinedZones()) {
				m_userZoneLossWarning = new JLabel(null, null, SwingConstants.CENTER);
				m_userZoneLossWarning.setName("m_userZoneLossWarning");
				m_userZoneLossWarning.setVisible(false);
				gbc.gridy = 1;
				gbc.fill = GridBagConstraints.HORIZONTAL;
				combinedPanel.add(m_userZoneLossWarning, gbc);
				checkLossOfUserDefinedZone();
			}
			getContentPane().add(combinedPanel);
		}

		public void itemStateChanged(ItemEvent e)
		{
			// Something is changed - check is selected only
			if (e.getStateChange() == ItemEvent.SELECTED) {
				Object source = e.getSource();
				if (source ==
						m_rowNameAlphabetical) {    // Row name is Alphabetical - disable the row start-with selection
					enableStartWithComponent(false, true);    // enable=false; row=true
				}
				else if (source ==
						m_rowNameNumerical) {    // Row name is Numerical - enable the row start-with selection
					enableStartWithComponent(true, true);    // enable=true; row=true
				}
				if (source ==
						m_colNameAlphabetical) {    // Column name is Alphabetical - disable the column start-with selection
					enableStartWithComponent(false, false);    // enable=false; row=false
				}
				else if (source ==
						m_colNameNumerical) {    // Column name is Numerical - enable the column start-with selection
					enableStartWithComponent(true, false);    // enable=true; row=false
				}
			}
		}

		/**
		 * Add numbering components to the contentPanel If forRow = true, then the numbering components are for row; for
		 * column otherwise
		 *
		 * @param forRow
		 */
		private void addNumberingComponents(JPanel contentPanel, boolean forRow, IZoneArea zoneArea,
				int defaultSelection)
		{
			JLabel numberingLabel = new JLabel(ResourceMgr.getStringForLabel(ModifyZoneAreaAction.class,
					"ModifyZoneAreaAction.numberingLabel.text"));
			contentPanel.add(numberingLabel);
			ButtonGroup numberingGroup = new ButtonGroup();
			if (forRow) {    // This is for row
				numberingGroup.add(m_rowNumNone);
				numberingGroup.add(m_rowNumBtoT);
				contentPanel.add(m_rowNumBtoT);
				numberingGroup.add(m_rowNumTtoB);
				contentPanel.add(m_rowNumTtoB);
			}
			else {   // For column
				numberingGroup.add(m_colNumNone);
				numberingGroup.add(m_colNumLtoR);
				contentPanel.add(m_colNumLtoR);
				numberingGroup.add(m_colNumRtoL);
				contentPanel.add(m_colNumRtoL);
			}

			// Set the selection
			// Get the ZoneArea numbering
			int zoneAreaNumbering = -1;
			if (zoneArea != null) {    // Get the zoneArea numbering
				if (forRow) {
					zoneAreaNumbering = zoneArea.getRowNumbering();
				}
				else {
					zoneAreaNumbering = zoneArea.getColumnNumbering();
				}
			}
			else {    // Use the default
				zoneAreaNumbering = defaultSelection;
			}

			if (zoneAreaNumbering == IZoneArea.BOTTOM_TOP) {    // Bottom to Top
				m_rowNumBtoT.setSelected(true);
			}
			else if (zoneAreaNumbering == IZoneArea.TOP_BOTTOM) {    // Top to Bottom
				m_rowNumTtoB.setSelected(true);
			}
			else if (zoneAreaNumbering == IZoneArea.LEFT_RIGHT) {    // Left to Right
				m_colNumLtoR.setSelected(true);
			}
			else if (zoneAreaNumbering == IZoneArea.RIGHT_LEFT) {    // Right to Left
				m_colNumRtoL.setSelected(true);
			}
		}

		/**
		 * Add numbering components to the contentPanel If forRow = true, then the numbering components are for row; for
		 * column otherwise
		 *
		 * @param forRow
		 */
		private void addNamingComponents(JPanel contentPanel, boolean forRow, IZoneArea zoneArea, int defaultSelection)
		{
			// Check if this is for row or column
			JRadioButton namingNone = null;
			JRadioButton namingNumerical = null;
			JRadioButton namingAlphabetical = null;
			if (forRow) {   // This is for Row
				namingNone = m_rowNameNone;
				namingNumerical = m_rowNameNumerical;
				namingAlphabetical = m_rowNameAlphabetical;
			}
			else {   // This is for column
				namingNone = m_colNameNone;
				namingNumerical = m_colNameNumerical;
				namingAlphabetical = m_colNameAlphabetical;
			}

			JLabel namingLabel = new JLabel(
					ResourceMgr.getStringForLabel(ModifyZoneAreaAction.class, "ModifyZoneAreaAction.namingLabel.text"));
			contentPanel.add(namingLabel);
			ButtonGroup namingGroup = new ButtonGroup();
			namingGroup.add(namingNone);
			namingGroup.add(namingNumerical);
			contentPanel.add(namingNumerical);
			namingGroup.add(namingAlphabetical);
			contentPanel.add(namingAlphabetical);

			// Set the selection
			// Get the ZoneArea naming
			int zoneAreaNaming = -1;
			if (zoneArea != null) {    // Get the zoneArea naming
				if (forRow) {
					zoneAreaNaming = zoneArea.getRowNaming();
				}
				else {
					zoneAreaNaming = zoneArea.getColumnNaming();
				}
			}
			else {    // Use the default
				zoneAreaNaming = defaultSelection;
			}

			if (zoneAreaNaming == IZoneArea.ALPHABETICAL_ZONE) {    // Alphabetical
				namingAlphabetical.setSelected(true);
			}
			else {    // Numerical
				namingNumerical.setSelected(true);
			}
		}

		/**
		 * Add start with components to the contentPanel If forRow = true, then the start with components are for row;
		 * for column otherwise
		 *
		 * @param forRow
		 */
		private void addStartWithComponents(JPanel contentPanel, boolean forRow, IZoneArea zoneArea,
				boolean defaultStartWithZero)
		{
			// Check if this is for row or column
			JRadioButton startWithNone = null;
			JRadioButton startWith0 = null;
			JRadioButton startWith1 = null;
			if (forRow) {   // This is for Row
				startWithNone = m_rowStartWNone;
				startWith0 = m_rowStartW0;
				startWith1 = m_rowStartW1;
			}
			else {   // This is for column
				startWithNone = m_colStartWNone;
				startWith0 = m_colStartW0;
				startWith1 = m_colStartW1;
			}

			JLabel startWithLabel = new JLabel(ResourceMgr.getStringForLabel(ModifyZoneAreaAction.class,
					"ModifyZoneAreaAction.startWithLabel.text"));
			contentPanel.add(startWithLabel);
			ButtonGroup startWithGroup = new ButtonGroup();
			startWithGroup.add(startWithNone);    // For no selection
			startWithGroup.add(startWith0);
			contentPanel.add(startWith0);
			startWithGroup.add(startWith1);
			contentPanel.add(startWith1);

			// Set the selection
			boolean startWithZero = false;
			if (zoneArea != null) {
				if (forRow) {
					startWithZero = zoneArea.getRowZeroStart();
				}
				else {
					startWithZero = zoneArea.getColumnZeroStart();
				}
			}
			else {
				startWithZero = defaultStartWithZero;
			}

			if (startWithZero) {    // Start with zero
				startWith0.setSelected(true);
			}
			else {    // Start with one
				startWith1.setSelected(true);
			}
		}

		/**
		 * When the useZoneArea is selected, then enabled all the input fields otherwise - disable them
		 *
		 * @param selected
		 */
		private void useZoneAreaSelected(boolean selected)
		{
			m_rowTextField.setEnabled(selected);
			m_colTextField.setEnabled(selected);
			m_exclusionsField.setEnabled(true);
			m_heightUnitChooser.getUnitTypeField().setEnabled(selected);
			m_heightUnitChooser.getValueTextField().setEnabled(selected);

			enableNumberingComponent(selected, true);    // row
			enableNumberingComponent(selected, false);    // column

			enableNamingComponent(selected, true);    // row
			enableNamingComponent(selected, false);    // column

			enableStartWithComponent(selected && m_rowNameNumerical.isSelected(), true);    // row
			enableStartWithComponent(selected && m_colNameNumerical.isSelected(), false);    // column

			checkLossOfUserDefinedZone();
		}

		private void enableNumberingComponent(boolean enable, boolean forRow)
		{
			if (forRow) {    // setEnabled row numbering components
				m_rowNumBtoT.setEnabled(enable);
				m_rowNumTtoB.setEnabled(enable);
			}
			else {    // setEnabled column numbering components
				m_colNumLtoR.setEnabled(enable);
				m_colNumRtoL.setEnabled(enable);
			}
		}

		private void enableNamingComponent(boolean enable, boolean forRow)
		{
			if (forRow) {    // setEnabled row naming components
				m_rowNameNumerical.setEnabled(enable);
				m_rowNameAlphabetical.setEnabled(enable);
			}
			else {    // setEnabled column naming components
				m_colNameNumerical.setEnabled(enable);
				m_colNameAlphabetical.setEnabled(enable);
			}
		}

		private void enableStartWithComponent(boolean enable, boolean forRow)
		{
			if (forRow) {    // setEnabled row start-with components
				m_rowStartW0.setEnabled(enable);
				m_rowStartW1.setEnabled(enable);
			}
			else {    // setEnabled column start-with components
				m_colStartW0.setEnabled(enable);
				m_colStartW1.setEnabled(enable);
			}
		}

		private int getDefaultTextHeight()
		{
			// This should be from preference, but since there is no preference now, use this
			//
			// Get the default text
			float defaultHeight = DEFAULT_TEXT_HEIGHT;
			GfxView view = (GfxView) CAFUtils.getInstance().getActiveCapletView();
			IGrid grid = ((IGriddable) view.getSheet()).getGrid();
			UnitTypeEnum heightUnitType = UnitTypeEnum.TypeInch;    // Default is inches

			IUnit heightInUnit = FactoryMgr.getCommonFactory().constructUnit(defaultHeight, heightUnitType);
			return GridHelper.toGridPoints(grid, heightInUnit);
		}

		/**
		 * Return true if the range is in the alphanumeric range; false otherwise
		 *
		 * @return boolean
		 */
		private boolean isValidAlphanumericNaming(int range, Set<Character> excludedChars)
		{
			return range <= IZoneArea.NUM_ALPHABET_CHARACTERS - excludedChars.size();
		}

		public boolean getUseZoneArea()
		{
			return m_useZoneAreaCB.isSelected();
		}

		public int getRow()
		{
			String rowText = StringUtils.nonNull(m_rowTextField != null ? m_rowTextField.getText() : "");
			return Integer.parseInt(rowText);
		}

		public int getColumn()
		{
			String colText = StringUtils.nonNull(m_colTextField != null ? m_colTextField.getText() : "");
			return Integer.parseInt(colText);
		}

		public String getExcludedCharacters()
		{
			return m_exclusionsField.getText();
		}

		public int getRowNaming()
		{
			int rowNaming = 0;
			if (m_rowNameNumerical.isSelected()) {   // Numerical is selected
				rowNaming = IZoneArea.NUMERICAL_ZONE;
			}
			else {   // Alphabetical is selected
				rowNaming = IZoneArea.ALPHABETICAL_ZONE;
			}
			return rowNaming;
		}

		public int getColumnNaming()
		{
			int colNaming = 0;
			if (m_colNameNumerical.isSelected()) {   // Numerical is selected
				colNaming = IZoneArea.NUMERICAL_ZONE;
			}
			else {   // Alphabetical is selected
				colNaming = IZoneArea.ALPHABETICAL_ZONE;
			}
			return colNaming;
		}

		public int getRowNumbering()
		{
			int rowNumbering = 0;
			if (m_rowNumBtoT.isSelected()) {   // Bottom to Top is selected
				rowNumbering = IZoneArea.BOTTOM_TOP;
			}
			else {    // Top to Bottom is selected
				rowNumbering = IZoneArea.TOP_BOTTOM;
			}
			return rowNumbering;
		}

		public int getColNumbering()
		{
			int colNumbering = 0;
			if (m_colNumLtoR.isSelected()) {   // Left to Right is selected
				colNumbering = IZoneArea.LEFT_RIGHT;
			}
			else {    // Right to Left is selected
				colNumbering = IZoneArea.RIGHT_LEFT;
			}
			return colNumbering;
		}

		public boolean getRowZeroStart()
		{
			return m_rowStartW0.isSelected();
		}

		public boolean getColZeroStart()
		{
			return m_colStartW0.isSelected();
		}

		/**
		 * Return the text height in grid points
		 *
		 * @return int
		 */
		public int getTextHeight()
		{
			int textHeight = 0;
			GfxView view = (GfxView) CAFUtils.getInstance().getActiveCapletView();
			IGrid grid = ((IGriddable) view.getSheet()).getGrid();

			textHeight = GridHelper.toGridPoints(grid, CAFUtils.getInstance().getCommonFactory().constructUnit(
					m_heightUnitChooser.getValue(), m_heightUnitChooser.getUnitType()));

			return textHeight;
		}

		public UnitTypeEnum getTextHeightUnit()
		{
			return m_heightUnitChooser.getUnitType();
		}

		private void setupWindowListener()
		{
			WindowListener listener = new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
					hide();
					dispose();
					setCancelled(true);
				}
			};
			addWindowListener(listener);
		}

		/**
		 * Check to see if the value of rows & columns make sense Raise a warning if it doesn't make sense.
		 *
		 * @return boolean
		 */
		private boolean checkRowsColumnsValidity()
		{
			int numOfRows = getRow();
			int numOfColumns = getColumn();
			IStamp activeBorder = m_model.getSymbolDef();
			if (activeBorder != null) {
				if (activeBorder instanceof IBorder) {
					// Check alphanumeric
					Set<Character> excludedChars = new HashSet<Character>();
					Set<String> invalidEntries = new HashSet<String>();
					if (!StringUtils.isBlank(m_zoneAreaDialog.getExcludedCharacters())) {
						invalidEntries = ZoneArea.getExcludedAlphabetCharactersFromCommaSeparatedString(
								m_zoneAreaDialog.getExcludedCharacters(), excludedChars);
					}
					if (!invalidEntries.isEmpty()) {   // Invalid excluded characters
						MessageHelper.showErrorMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
								ResourceMgr.getString(ModifyZoneAreaAction.class,
										"ModifyZoneAreaAction.TitledBorder.text"),
								ResourceMgr.getString(ModifyZoneAreaAction.class,
										"ModifyZoneAreaAction.Error.ExcludedCharacters.Title"),
								ResourceMgr.getString(ModifyZoneAreaAction.class,
										"ModifyZoneAreaAction.Error.ExcludedCharacters.Msg",
										StringUtils.convertCollectionToString(invalidEntries,
												IZoneArea.EXCLUDED_CHARS_SEPARATOR)));
						return false;
					}
					if (getRowNaming() == IZoneArea.ALPHABETICAL_ZONE) {   // Row is alphanumeric
						// Check to make sure that it make sense
						if (!(isValidAlphanumericNaming(numOfRows, excludedChars))) {
							MessageHelper.showErrorMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
									ResourceMgr.getString(ModifyZoneAreaAction.class,
											"ModifyZoneAreaAction.TitledBorder.text"),
									ResourceMgr.getString(ModifyZoneAreaAction.class,
											"ModifyZoneAreaAction.Error.Title1"),
									ResourceMgr.getString(ModifyZoneAreaAction.class,
											"ModifyZoneAreaAction.Error.Msg1"));
							return false;
						}
					}
					if (getColumnNaming() == IZoneArea.ALPHABETICAL_ZONE) {   // Column is alphanumeric
						// Check to make sure that it make sense
						if (!(isValidAlphanumericNaming(numOfColumns, excludedChars))) {
							MessageHelper.showErrorMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
									ResourceMgr.getString(ModifyZoneAreaAction.class,
											"ModifyZoneAreaAction.TitledBorder.text"),
									ResourceMgr.getString(ModifyZoneAreaAction.class,
											"ModifyZoneAreaAction.Error.Title1"),
									ResourceMgr.getString(ModifyZoneAreaAction.class,
											"ModifyZoneAreaAction.Error.Msg2"));
							return false;
						}
					}

					IBorder border = (IBorder) activeBorder;
					// Get the usableArea
					IExtent usableAreaExtent = border.getUsableArea();
					int width = usableAreaExtent.getWidth();
					int height = usableAreaExtent.getHeight();

					int columnWidth = width / numOfColumns;
					int rowHeight = height / numOfRows;
					int textHeightInGrids = m_zoneAreaDialog.getTextHeight();

					StringBuilder headerBuffer = new StringBuilder();
					StringBuilder messageBuffer = new StringBuilder();

					headerBuffer.append("Too Many ");
					messageBuffer.append("The ");
					// Check column

					boolean unreadable = false;
					if ((columnWidth / 2) < textHeightInGrids) {   // Too small ?
						headerBuffer.append("Columns");
						messageBuffer.append("column ");
						unreadable = true;
					}

					// Check the row
					if (rowHeight < textHeightInGrids) {  // Too small?
						if (unreadable) {   // Already too many columns - add spaces
							headerBuffer.append(" And ");
							messageBuffer.append(" and ");
						}
						headerBuffer.append("Rows");
						messageBuffer.append("row ");
						unreadable = true;
					}

					if (unreadable) {
						messageBuffer.append("labels might be unreadable");

						// If we want to show warning about the unreadable - uncomment this
//						return MessageHelper.showOkCancelDialog(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
//								headerBuffer.toString(), messageBuffer.toString());
						return true;
					}
				}
			}

			return true;
		}

		/**
		 * Setup the listeners for OK & Cancel button
		 */
		private void setupOKCancelButtons()
		{
			getOkButton().addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					boolean shouldContinue = checkRowsColumnsValidity();

					if (shouldContinue) {
						hide();
						dispose();
					}
				}
			});

			getCancelButton().addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					hide();
					dispose();
					setCancelled(true);
				}
			});
		}

		/**
		 * This Document class is used to verify the type value of the Rows & Columns TexField. It disable/enable the OK
		 * button appropriately
		 */
		private class NumberRowsColumnsDocument extends PlainDocument
		{

			private static final int MAX_ROWS_COLUMNS = 1000;

			/**
			 * Check the string before doing insert to check for valif value
			 *
			 * @param offset
			 * @param string
			 * @param attributes
			 *
			 * @throws BadLocationException
			 */
			public void insertString(int offset, String string,
					AttributeSet attributes) throws BadLocationException
			{
				if (string == null) {
					return;
				}
				else {
					String newValue;
					int length = getLength();
					if (length == 0) {
						newValue = string;
					}
					else {
						String currentContent = getText(0, length);
						StringBuilder currentBuffer = new StringBuilder(currentContent);
						currentBuffer.insert(offset, string);
						newValue = currentBuffer.toString();
					}
					try {
						int intVal = Integer.parseInt(newValue);

						// Only insert if the value is less then
						if ((intVal > 0) && (intVal < MAX_ROWS_COLUMNS)) {
							super.insertString(offset, string, attributes);
							// Enable the OK button
							getOkButton().setEnabled(souldEnableOKButton());
						}
					}
					catch (NumberFormatException ex) {    // This is not a number - ignore it
						// Disable the OK button? we are eating out the input so user won't
						// be able to know the outcome. hence don't disable OK button.
						//getOkButton().setEnabled(false);
					}
					checkLossOfUserDefinedZone();
				}
			}

			private boolean souldEnableOKButton()
			{
				try {
					int numRows = getRow();
					int numColumns = getColumn();
					// Only insert if the value is less then
					if ((numRows > 0) && (numColumns > 0) && (numRows < MAX_ROWS_COLUMNS) &&
							(numColumns < MAX_ROWS_COLUMNS)) {
						return true;
					}
				}
				catch (NumberFormatException ex) {    // This is not a number
					return false;
				}
				return false;
			}

			/**
			 * Disable the OK button if there is nothing in the text field
			 *
			 * @param offset
			 * @param len
			 *
			 * @throws BadLocationException
			 */
			public void remove(int offset, int len) throws BadLocationException
			{
				super.remove(offset, len);

				getOkButton().setEnabled(souldEnableOKButton());
				checkLossOfUserDefinedZone();
			}
		}

		private void checkLossOfUserDefinedZone()
		{
			JLabel zoneLossWarning = m_userZoneLossWarning;
			IZoneArea zoneArea = m_zoneArea;
			IZoneAreaObject activeBorder = CommonUtils.cast(m_model.getSymbolDef(), IZoneAreaObject.class);
			if (zoneLossWarning != null && zoneArea != null && activeBorder != null) {
				zoneLossWarning.setVisible(false);
				if (getOkButton().isEnabled()) {
					try {
						boolean row_col_enabled = m_rowTextField.isEnabled() && m_colTextField.isEnabled();
						int numRows = getRow();
						int numColumns = getColumn();
						int zoneAreaNumRows = zoneArea.getNumRows();
						int zoneAreaNumColumns = zoneArea.getNumColumns();
						boolean isUsableAreaChanged = isUsableAreaChanged(activeBorder, zoneArea);
						// Only insert if the value is less then
						if (isUsableAreaChanged || !row_col_enabled || numRows != zoneAreaNumRows ||
								numColumns != zoneAreaNumColumns) {
							zoneLossWarning.setVisible(true);
							zoneLossWarning.setText(ResourceMgr.getString(ModifyZoneAreaAction.class,
									"ModifyZoneAreaAction.userzone.warning"));
						}
					}
					catch (NumberFormatException ex) {    // This is not a number
					}
				}
			}
		}

		private boolean isUsableAreaChanged(@NotNull IZoneAreaObject activeBorder, @NotNull IZoneArea zoneArea)
		{
			int zoneAreaNumRows = zoneArea.getNumRows();
			int zoneAreaNumColumns = zoneArea.getNumColumns();
			ICommonFactory commonFactory = FactoryMgr.getCommonFactory();
			IExtent extent = commonFactory.createExtent();
			zoneArea.getZoneExtents().forEach(extent::addUnion);
			IExtent usableArea = commonFactory.constructExtent(activeBorder.getUsableArea());
			return !ZoneHelper.isZoneAreaReusable(extent, usableArea, zoneAreaNumRows, zoneAreaNumColumns);
		}
	}

	/**
	 * If it is enabled, populate the context menu with the action
	 *
	 * @param container
	 * @param selections
	 */
	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (isEnabled()) {   // Add this action to the container
			container.add(new ActionEntry(getActionUI()));
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}
}


