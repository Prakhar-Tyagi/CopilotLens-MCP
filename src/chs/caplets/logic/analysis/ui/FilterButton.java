/*
 * Copyright 2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.analysis.ui;

import javax.swing.Icon;
import javax.swing.JButton;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements a generic filter button and its associated actions.
 *
 * @author Mick Gilford
 * @version 1.0 created on 15-Jun-2007
 */
public class FilterButton extends JButton implements ActionListener
{

	// //////////////// //
	// Class variables. //
	// //////////////// //

	/**
	 * Resources for this class.
	 */

	// ////////////// //
	// Class methods. //
	// ////////////// //

	// /////////////////// //
	// Instance variables. //
	// /////////////////// //

	/**
	 * This field holds the current state of the button, typically 0 or 1 (or maybe 2).
	 */
	protected int currentState;
	/**
	 * This field holds the icon to display depending on the current state of the button.
	 */
	protected List<Icon> icons;
	/**
	 * This field holds the tooltip to display depending on the current state of the button.
	 */
	protected List<String> tooltips;
	/**
	 * This field holds the valid types that are displayable in each state. Use "" for no types displayable in the state,
	 * else a comma separated list, e.g. "node,pin".
	 */
	protected List<String> validTypes;
	/**
	 * This field holds parent table panel, so that we can apply the filters to the table rows.
	 */

	IAnalysisFilterableBrowserComponent table;
	// ////////////////////// //
	// Instance constructors. //
	// ////////////////////// //

	public FilterButton(IAnalysisFilterableBrowserComponent table)
	{
		//super();
		// Make the JButton act and look how we want.
		addActionListener(this);
		//setBorderPainted(true);
		//setUI(new BasicButtonUI());

		// Initialize the filter-specific data.

		currentState = 0;
		icons = new ArrayList<Icon>();
		tooltips = new ArrayList<String>();
		validTypes = new ArrayList<String>();
		this.table = table;
	}

	// ///////////////// //
	// Instance methods. //
	// ///////////////// //

	public void addIcon(Icon icon)
	{
		icons.add(icon);
	}

	public void addToolTipText(String tip)
	{
		tooltips.add(tip);
	}

	public void addValidTypes(String types)
	{
		validTypes.add(types);
	}

	public void updateIcon()
	{
		if (icons.isEmpty()) {
			// The buton has not yet been initialized.
			return;
		}
		if (currentState < 0 || currentState >= icons.size()) {
			currentState = 0;
		}
		setIcon(icons.get(currentState));
		if (currentState < tooltips.size()) {
			setToolTipText(tooltips.get(currentState));
		}
		repaint();
	}

	public void actionPerformed(ActionEvent e)
	{

		currentState++;
		updateIcon();
		String newState = validTypes.get(currentState);
		table.setFilter(this, newState);
	}

	public void resetFilters()
	{
		currentState = 0;
		updateIcon();
	}
}
