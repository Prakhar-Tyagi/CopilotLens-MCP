/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.analysis.ui;

import chs.caf.CAFUtils;
import chs.ctf.caf.ui.CAFOkCancelDialog;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * @author rharring
 */
public class BackgroundSimulationSelectionDialog extends CAFOkCancelDialog implements ItemListener
{

	// /////////////// //
	// Class variables //
	// /////////////// //

	// ///////////// //
	// Class methods //
	// ///////////// //

	// ////////////////// //
	// Instance Variables //
	// ////////////////// //

	/**
	 * The radios for simulation types
	 */
	protected JRadioButton qualRadio, numericRadio;

	/**
	 * The checkbox for transient
	 */
	protected JCheckBox transientBox;

	/**
	 * The textFields for the start, stop sample if transient
	 */
	protected JTextField startField, stopField, sampleField;

	// //////////// //
	// Constructors //
	// //////////// //

	/**
	 * Creates a new instance of BackgroundSimulationSelectionDialog
	 */
	public BackgroundSimulationSelectionDialog()
	{
		super(CAFUtils.getInstance().getWindowMgr().getDialogFrame(), "Background Simulation Settings", true);
		createGui();
		pack();

		getOkButton().addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				setCancelled(false);
				dispose();
			}
		});

		getCancelButton().addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				setCancelled(true);
				dispose();
			}
		});
	}

	/**
	 * This method creates the gui
	 */
	protected void createGui()
	{

		getContentPane().add(createNorthPanel(), BorderLayout.NORTH);

		// we're not enabling transient selection in the 2005.1 release
		// will revisit this for 2005.2 ROH
		//getContentPane( ).add( createCenterPanel( ), BorderLayout.CENTER ) ;
	}

	protected JPanel createNorthPanel()
	{
		JPanel temp = new JPanel();
		temp.setLayout(new BorderLayout());
		temp.add(new JLabel("Select mode : "), BorderLayout.NORTH);

		JPanel centerPanel = new JPanel();
		ButtonGroup radioGroup = new ButtonGroup();

		qualRadio = new JRadioButton("Qualitative");
		numericRadio = new JRadioButton("Numeric");

		radioGroup.add(qualRadio);
		radioGroup.add(numericRadio);

		centerPanel.add(qualRadio);
		centerPanel.add(numericRadio);

		temp.add(centerPanel, BorderLayout.CENTER);
		qualRadio.setSelected(true);
		return temp;
	}

	protected JPanel createCenterPanel()
	{
		JPanel temp = new JPanel();
		temp.setLayout(new BorderLayout());

		transientBox = new JCheckBox("Use transient");

		JPanel transientPanel = new JPanel();
		transientPanel.add(transientBox);
		temp.add(transientPanel, BorderLayout.NORTH);

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new GridLayout(3, 1));
		startField = new JTextField(10);
		stopField = new JTextField(10);
		sampleField = new JTextField(10);

		centerPanel.add(createTransientPanel("Start time  ", startField));
		centerPanel.add(createTransientPanel("End time    ", stopField));
		centerPanel.add(createTransientPanel("Sample time ", sampleField));

		temp.add(centerPanel, BorderLayout.CENTER);

		transientBox.addItemListener(this); // add the listener last of all to
		// avoid npe during setup
		return temp;
	}

	protected JPanel createTransientPanel(String label, JTextField field)
	{
		JPanel temp = new JPanel();
		temp.setLayout(new GridLayout(1, 2));
		temp.add(new JLabel(label));
		field.setName(label);
		field.setEnabled(false);
		temp.add(field);
		return temp;
	}

	// ////////////// //
	// Initialization //
	// ////////////// //

	// //////////////////// //
	// Accessors / Mutators //
	// //////////////////// //

	public String getSelectedSimulationType()
	{
		if (qualRadio.isSelected()) {
			return "electrical.qualitative";
		}
		else {
			return "electrical.quantitative.spice";
		}
	}

	// ///////////// //
	// Other methods //
	// ///////////// //

	// ///////////////////////////////////////// //
	// Inner classes / Interface Implementations //
	// ///////////////////////////////////////// //

	public void itemStateChanged(ItemEvent ie)
	{
		System.err.println(" SF " + startField + " stF " + stopField + " smpF " + sampleField);
		startField.setEnabled(transientBox.isSelected());
		stopField.setEnabled(transientBox.isSelected());
		sampleField.setEnabled(transientBox.isSelected());
	}

	// /////////// //
	// Main / Test //
	// /////////// //
}
