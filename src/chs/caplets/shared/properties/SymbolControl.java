/*
 * Copyright 2003-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.shared.properties;

import chs.caf.CAFUtils;
import chs.caf.caplet.IPropertiedSet;
import chs.caf.caplet.IPropertiesClientComponent;
import chs.cof.draw.ICommentSymbol;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.symbol.IAbstractLibrary;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolLibraryMgr;
import chs.cof.symbol.ISymbolRef;
import chs.common.IUIDObject;
import chs.common.UIDUtils;
import chs.utilities.BuildInfo;
import chs.utilities.CapitalDateTimeFormatter;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.CHSColors;
import chs.utility.ui.IValidityListener;
import org.jetbrains.annotations.NotNull;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;


public class SymbolControl implements IPropertiesClientComponent
{

	public SymbolControl()
	{
	}

	/**
	 * * @return a component that may be used to edit the parameters.
	 */
	public JPanel getWidget(IPropertiedSet propset)
	{
		IUIDObject currObject = propset.getCommonRepresentingObject();
//		if (currObject == null) {
//			// PW - 03/27/03 - Defect #2862
//			// Will not display widget for multiple objects
//			return null;
//			//JPanel jp = new JPanel();
//			//jp.add(new JLabel("Multiple Objects"));
//			//return jp;
//		}
		ISymbolRef symref = null;
		if (currObject != null) {
			//
			// first off, we do some checks on the object,
			// to see if it is indeed valid.
			//
			if (!(currObject instanceof ICommentSymbol) && !(currObject instanceof IPinList)) {
				return null; // no it isn't
			}
			//
			// Get the object, see if it is a symbolled pinlist
			//
			if (currObject instanceof IPinList) {
				symref = ((IPinList) currObject).getSymbolRef();
			}
			else {
				symref = ((ICommentSymbol) currObject).getSymbolRef();
			}
		}
		if (symref == null) {
			Set<IUIDObject> selectedPinlists = UIDUtils.convertToUIDObjectSet(propset.getObjectsCollection());
			symref = SymbolControlHelper.getSymbolRefFromSelectedObjects(selectedPinlists);
		}

		if (symref == null) {
			return null; // No symbol ref
		}

		ISymbolLibraryMgr symLibMgr = CAFUtils.getInstance().getCHSSystem().getSymbolLibraryMgr();
		IStamp symdef = symLibMgr.getReferencedSymbol(symref);
		IAbstractLibrary symlib = null;
		if (symdef != null) {
			symlib = symdef.getContainerLibrary();
		}

		//
		// Right, create the component, and fill in the fields.
		//
		JPanel control = new JPanel();
		control.setLayout(new GridLayout(3, 1));

		String libname = null;
		String libuid = null;
		if (symlib == null) {
			libname = "Library not found";
			libuid = "";
		}
		else {
			libname = symlib.getName();
			libuid = symlib.getUID().getString();
		}

		Object[][] libmodel;
		if (BuildInfo.getBuildInfo().areQAExtensionsEnabled()) {
			libmodel = new Object[][]{
					{"Name", libname},
					{"UID", libuid}
			};
		}
		else {
			libmodel = new Object[][]{
					{"Name", libname}
			};
		}
		control.add(createTable(ResourceMgr.getString(SymbolControl.class, "SymbolControl.Library.Title"), libmodel));

		String symname = null;
		String symuid = null;
		String symts = null;
		String symlocts = null;
		symuid = symref.getSymbolUID().getString(); // Use the ref, as it will always be there..
		if (symdef == null) {
			symname = "Symbol not found";
			symts = "";
			symlocts = "";
		}
		else {
			symname = symdef.getName();
			symts = "" + symdef.getServerTimeModified();
			symlocts = formatTimestamp(symdef.getServerTimeModified());
		}

		Object[][] symmodel;
		if (BuildInfo.getBuildInfo().areQAExtensionsEnabled()) {
			symmodel = new Object[][]{
					{"Name", symname},
					{"UID", symuid},
					{"Timestamp", symts},
					{"Modification time", symlocts}
			};
		}
		else {
			symmodel = new Object[][]{
					{"Name", symname},
					{"Modification time", symlocts}
			};
		}
		control.add(createTable(ResourceMgr.getString(SymbolControl.class, "SymbolControl.Symbol.Title"), symmodel));

		String status = "";
		if (symdef == null) {
			status = "No symbol found to check";
		}
		else if (symref.getTimestamp() == symdef.getServerTimeModified()) {
			status = "Current";
		}
		else if (symref.getTimestamp() >= symdef.getServerTimeModified()) {
			status = "Invalid (instance time > symbol)";
		}
		else {
			status = "Needs update";
		}
		Object[][] instmodel;
		if (BuildInfo.getBuildInfo().areQAExtensionsEnabled()) {
			instmodel = new Object[][]{
					{"UID", currObject != null ? currObject.getUID().getString() : ""},
					{"Modification timestamp", "" + symref.getTimestamp()},
					{"Modification time", "" + formatTimestamp(symref.getTimestamp())},
					{"Status", status}
			};
		}
		else {
			instmodel = new Object[][]{
					{"Modification time", "" + formatTimestamp(symref.getTimestamp())},
					{"Status", status}
			};
		}
		control.add(createTable(ResourceMgr.getString(SymbolControl.class, "SymbolControl.Instance.Title"), instmodel));
		return control;
	}

	/**
	 * Returns true - this component should have it's own tab
	 */
	public boolean isPropPage()
	{
		return true;
	}

	public String getTabName(IPropertiedSet propset)
	{
		return ResourceMgr.getString(SymbolControl.class, "SymbolControl.Tab.Label");
	}

	public boolean acceptsSet(IPropertiedSet propset)
	{
		List<IUIDObject> validObjects = UIDUtils.convertToUIDObjectList(propset.getObjectsCollection());
		return propset.editType(IPinList.class) || propset.editType(ICommentSymbol.class) ||
				SymbolControlHelper.doesSelectionIncludeAMixOfSchemAndConnPinlists(validObjects);
	}

	public boolean modifiesSet(IPropertiedSet propset)
	{
		return acceptsSet(propset);
	}

	public void edit(IPropertiedSet propset)
	{
	}

	private JPanel createTable(String title, Object[][] model)
	{
		JTable table = new JTable(model, new Object[]{"name", "value"})
		{
			public boolean isCellEditable(int rowIndex, int vColIndex)
			{
				return false;
			}
		};
		table.setName(title);
		table.setBorder(BorderFactory.createLineBorder(CHSColors.getBorderColor()));
		JPanel testPanel = new JPanel();
		testPanel.setLayout(new BorderLayout());
		testPanel.setBorder(new TitledBorder(title));
		testPanel.add(table, BorderLayout.CENTER);
		return testPanel;
	}

	private String formatTimestamp(long l)
	{
		Date d = new Date(l);

		// PW - 03/26/03
		// Use DateFormat.LONG instead of DateFormat.FULL because in Locale.UK
		// DateFormat.FULL will produce the time format with the word o'clock (Defect #3193)
		return CapitalDateTimeFormatter.getDateTimeInstance(FormatStyle.MEDIUM, FormatStyle.LONG).format(d);
	}

	/**
	 * @see chs.caf.caplet.IPropertiesClientComponent#stopEditing()
	 */
	public void stopEditing(IPropertiedSet propset)
	{
	}

	/**
	 * @see chs.caf.caplet.IPropertiesClientComponent#destroy()
	 */
	public void destroy()
	{
	}

	/**
	 * @see chs.caf.caplet.IPropertiesClientComponent#isValid()
	 */
	public boolean isValid()
	{
		// todo Auto-generated method stub
		return false;
	}

	/**
	 * @see chs.caf.caplet.IPropertiesClientComponent#addValidityListener(chs.utility.ui.IValidityListener)
	 */
	public void addValidityListener(IValidityListener listener)
	{
		// todo Auto-generated method stub

	}

	/**
	 * @see chs.caf.caplet.IPropertiesClientComponent#removeValidityListener(chs.utility.ui.IValidityListener)
	 */
	public void removeValidityListener(IValidityListener listener)
	{
		// todo Auto-generated method stub

	}

	@Override public void addPropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener)
	{
	}

	public Set<ISharedObject> getSharedObjects()
	{
		return Collections.EMPTY_SET;
	}

	public Set<ISharedObject> getEditedSharedObjects()
	{
		return Collections.EMPTY_SET;
	}
}
