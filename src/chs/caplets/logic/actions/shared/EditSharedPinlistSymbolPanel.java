/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.FindReplaceSelectSymbolDialog;
import chs.cof.draw.IColor;
import chs.cof.draw.IConfigurableVisitor;
import chs.cof.draw.IDrawFactory;
import chs.cof.draw.IGfxAttribute;
import chs.cof.draw.ISheet;
import chs.cof.draw.IVisitor;
import chs.cof.draw.LineStyle;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.logical.shared.SharedPinListHelper;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.ISymbolDefIterator;
import chs.cof.symbol.SymbolTypeEnum;
import chs.utilities.ui.BasicUIFactory;
import chs.ctf.ui.form.LibrarySymbolFilter;
import chs.ctf.ui.form.RelationalOperatorEnum;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.SortedListModel;
import chs.utility.SymbolUtils;
import chs.utility.gfx.DrawingComponent;
import chs.utility.gfx.DrawingComponentWithScrollBar;
import chs.utility.gfx.IDrawingComponentOwner;
import chs.utility.helpers.NamedObjectComparator;
import chs.utility.ui.ISharedPinListSymbolInstance;
import chs.utility.ui.SharedPinListSymbolInstance;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;

public class EditSharedPinlistSymbolPanel extends JPanel implements ListSelectionListener, IDrawingComponentOwner
{

	private EditSharedPinListModel esplModel;
	private ISheet symbolSheet = null;
	private ISheet blankSheet = null;
	private JList symbolInstanceList;
	private JButton deleteButton;
	private JButton addButton;
	private ISharedPinList sharedPinList;
	private SortedListModel<SharedPinListSymbolInstance> symbolInstanceListModel;
	private DrawingComponentWithScrollBar m_displayDrawingComponent;

	public EditSharedPinlistSymbolPanel(EditSharedPinListModel emodel)
	{
		esplModel = emodel;
		sharedPinList = esplModel.getSharedPinList();

		// Build the symbol pane
		// Add the existing symbols on the shared pin list into the dialog list.
		JScrollPane listView = fillSymbolList();

		// Build the symbol preview pane
		buildSymbolViewPane();

		// Build the buttons
		JPanel componentsPanel = buildAndAddButtons(listView);

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(Box.createVerticalStrut(10));
		Box hBox = new Box(BoxLayout.X_AXIS);
		hBox.add(Box.createHorizontalStrut(10));
		hBox.add(componentsPanel);
		hBox.add(Box.createHorizontalStrut(10));
		add(hBox);
		add(Box.createVerticalStrut(10));
	}

	private void buildSymbolViewPane()
	{
		Border border = BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140));
		IDrawFactory dfi = FactoryMgr.getDrawFactory();
		IGfxAttribute deviceSymbolAttribute =
				dfi.constructGfxAttribute(dfi.lookupColor("device"), 1, LineStyle.NO_STYLE);
		IColor background = dfi.lookupColor("background");
		symbolSheet = dfi.constructSheet(deviceSymbolAttribute, background);
		blankSheet = dfi.constructSheet(deviceSymbolAttribute, background);
		m_displayDrawingComponent = new DrawingComponentWithScrollBar(CAFUtils.getInstance().getCommonFactory(),
				FactoryMgr.getDrawFactory(),
				true,
				DrawingComponent.SCROLLBARS_BOTH,
				this, symbolSheet);
		DrawingComponent display = m_displayDrawingComponent.getDisplay();
		m_displayDrawingComponent.setBorder(border);
		display.setPreferredSize(new Dimension(EditSharedPinlistDialog.PreferredPanelSize.width / 2,
				EditSharedPinlistDialog.PreferredPanelSize.height));
		display.setName("RemoveSymbolDisplay");
	}

	private JPanel buildAndAddButtons(JScrollPane listView)
	{
		deleteButton =
				BasicUIFactory.getInstance().createSiemensCustomJButton(ResourceMgr.getString(MapPanel.class, "EditSharedPinlistSymbolPanel.removeButton.text"));
		deleteButton.setName("EditSharedPinlistSymbolPanel.removeButton");
		deleteButton.setToolTipText(
				ResourceMgr.getString(MapPanel.class, "EditSharedPinlistSymbolPanel.removeButton.tooltip"));
		deleteButton.setMnemonic(
				ResourceMgr.getMnemonic(MapPanel.class, "EditSharedPinlistSymbolPanel.removeButton.mnemonic"));
		deleteButton.setEnabled(false); // Initially disabled
		deleteButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				deleteSymbol();
			}
		});

		addButton =
				BasicUIFactory.getInstance().createSiemensCustomJButton(ResourceMgr.getString(MapPanel.class, "EditSharedPinlistSymbolPanel.addButton.text"));
		addButton.setName("EditSharedPinlistSymbolPanel.addButton");
		addButton.setToolTipText(
				ResourceMgr.getString(MapPanel.class, "EditSharedPinlistSymbolPanel.addButton.tooltip"));
		addButton.setMnemonic(
				ResourceMgr.getMnemonic(MapPanel.class, "EditSharedPinlistSymbolPanel.addButton.mnemonic"));
		if (!esplModel.canCurrentPinListHaveMultipleSymbols()) {
			addButton.setEnabled(symbolInstanceListModel.isEmpty());
		}
		else {
			addButton.setEnabled(true);
		}
		addButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				addSymbol();
			}
		});

		// Create the buttons
		JPanel componentsPanel = new JPanel(new BorderLayout(5, 5));
		JPanel listAndButtons = new JPanel(new BorderLayout(5, 5));
		// Create the buttons
		JPanel buttons = new JPanel((new BorderLayout(5, 5)));
		buttons.add(addButton, BorderLayout.WEST);
		buttons.add(deleteButton, BorderLayout.EAST);
		// add the buttons and the symbol list
		listAndButtons.add(listView, BorderLayout.CENTER);
		listAndButtons.add(buttons, BorderLayout.SOUTH);
		// Now add the list+buttons with the pretty picture.
		componentsPanel.add(m_displayDrawingComponent, BorderLayout.CENTER);
		componentsPanel.add(listAndButtons, BorderLayout.WEST);
		return componentsPanel;
	}

	private JScrollPane fillSymbolList()
	{
		// Can't genrify this as the static type CASE_INSENSITIVE_COMPARATOR can't be typed
		//noinspection unchecked
		symbolInstanceListModel =
				new SortedListModel<SharedPinListSymbolInstance>(NamedObjectComparator.caseInsensitiveComparator());
		symbolInstanceList = new JList(symbolInstanceListModel);
		symbolInstanceList.setName("SymbolList");
		symbolInstanceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		symbolInstanceList.addListSelectionListener(this);
		JScrollPane listView = new JScrollPane(symbolInstanceList);
		listView.setName("SymbolScrollPane");

		for (ISymbolDefIterator symIt = sharedPinList.getSymbols(); symIt.hasNext(); ) {
			ISymbolDef symDef = symIt.getNext();
			for (int i = 0; i < sharedPinList.getNumInstances(symDef); i++) {
				((Collection<SharedPinListSymbolInstance>) symbolInstanceList.getModel())
						.add(new SharedPinListSymbolInstance(sharedPinList, symDef, i));
			}
		}
		return listView;
	}

	private void deleteSymbol()
	{
		SharedPinListSymbolInstance inst = (SharedPinListSymbolInstance) symbolInstanceList.getSelectedValue();
		if (inst != null &&
				SharedPinListHelper.okToRemove(sharedPinList, inst.getSymbolDef(), inst.getInstanceNumber())) {
			// Remove from the list
			symbolInstanceListModel.remove(inst);
			if (esplModel.getSymbolDefsForAddition().contains(inst.getSymbolDef())) {
				// If this symdef has been added in this action, then just delete from the added list
				esplModel.getSymbolDefsForAddition().remove(inst.getSymbolDef());
			}
			else {
				// else add it to the list of defs to remove.
				esplModel.getSymbolInstancesForDeletion().add(inst);
			}
		}

		if (!esplModel.canCurrentPinListHaveMultipleSymbols()) {
			addButton.setEnabled(symbolInstanceListModel.isEmpty());
		}
	}

	private void addSymbol()
	{
		// Bring up the symbol select dialog
		LibrarySymbolFilter lsf = getSymbolFilterForSharedPinList();

		FindReplaceSelectSymbolDialog selectSymbolDialog = new FindReplaceSelectSymbolDialog(
				CAFUtils.getInstance().getDialogFrame(),
				ResourceMgr.getString(EditSharedPinlistSymbolPanel.class,
						"EditSharedPinlistSymbolPanel.dialog.title"),
				lsf);
		selectSymbolDialog.setVisible(true);
		if (!selectSymbolDialog.isCancelled() && (selectSymbolDialog.getSymbolDef() != null)) {
			// If we're not cancelled, then add this symbol
			SharedPinListSymbolInstance symInstanaceToAdd = null;
			// First find out if this def has just been marked for removal
			for (Object o : esplModel.getSymbolInstancesForDeletion()) {
				SharedPinListSymbolInstance inst = (SharedPinListSymbolInstance) o;
				if (inst.getSymbolDef() == selectSymbolDialog.getSymbolDef()) {
					// If it had been marked for removal (fikkle customer changed their mind) then remove it from
					// the list to be deleted.
					symInstanaceToAdd = inst;
					esplModel.getSymbolInstancesForDeletion().remove(symInstanaceToAdd);
					break;
				}
			}
			if (symInstanaceToAdd == null) {
				// If it hasn't previously been added - add now.
				// Note: frist setSymbolDef() and then add to the added symbol list, otherwise there is a problem
				// in event mechanism. (see code change maded for dts0100590894 in EditSharedPinListDialoag)
				esplModel.setSymbolDef(selectSymbolDialog.getSymbolDef());
				esplModel.mapSymbolDefToPinMaps();
				esplModel.getSymbolDefsForAddition().add(selectSymbolDialog.getSymbolDef());
				symInstanaceToAdd =
						new SharedPinListSymbolInstance(sharedPinList, selectSymbolDialog.getSymbolDef(), 0);
			}
			symbolInstanceListModel.add(symInstanaceToAdd);
		}

		if (!esplModel.canCurrentPinListHaveMultipleSymbols()) {
			// Only devices can have multiple symbols.
			addButton.setEnabled(symbolInstanceListModel.isEmpty());
		}
	}

	protected LibrarySymbolFilter getSymbolFilterForSharedPinList()
	{
		LibrarySymbolFilter lsf = new LibrarySymbolFilter()
		{
			public boolean accept(IStamp sym)
			{
				for (SharedPinListSymbolInstance instance : symbolInstanceListModel) {
					ISymbolDef instanceDef = instance.getSymbolDef();
					if (instanceDef == sym) {
						return false;
					}
				}
				return super.accept(sym);
			}
		};
		if (sharedPinList != null && (sharedPinList.isFunctionType())) {
			lsf.setSymbolTypes(Collections.singleton(SymbolTypeEnum.FUNCTION));
			return lsf;
		}

		if (sharedPinList != null && (sharedPinList.getType() == PinListTypeEnum.TypeRingTerminal)) {
			lsf.setPinFilter(1, RelationalOperatorEnum.EQ);
		}

		lsf.setSymbolTypes(Collections.singleton(SymbolTypeEnum.DEVICE));
		return lsf;
	}

	public boolean isUseful()
	{
		return true;
	}

	public void valueChanged(ListSelectionEvent e)
	{
		symbolSheet.removeAllObjects();
		m_displayDrawingComponent.getDisplay().clearContext();
		ISharedPinListSymbolInstance inst = (ISharedPinListSymbolInstance) symbolInstanceList.getSelectedValue();
		if (inst != null) {
			SymbolUtils.updateSymbolSheetColor(symbolSheet, inst.getSymbolDef(), FactoryMgr.getDrawFactory());
			symbolSheet.addObject(SymbolUtils.shadowCopy(inst.getSymbolDef(), inst.getInstanceNumber(), sharedPinList));
			deleteButton.setEnabled(
					SharedPinListHelper.okToRemove(sharedPinList, inst.getSymbolDef(), inst.getInstanceNumber()));
			esplModel.setSymbolDef(inst.getSymbolDef());
		}
		else {
			deleteButton.setEnabled(false);
		}
		m_displayDrawingComponent.getDisplay().repaint();
	}

	public ISheet getSheet()
	{
		return symbolInstanceList.getSelectedValue() != null ? symbolSheet : blankSheet;
	}

	public void postCustomRender(IVisitor renderer)
	{
	}

	@Override public void configureRenderer(IConfigurableVisitor renderer)
	{
		// NO-OP
	}

	public void preCustomRender(IVisitor renderer)
	{
	}

	public void updateAreaListeners()
	{
	}

	public void showAsReadOnly()
	{
		addButton.setEnabled(false);
		deleteButton.setEnabled(false);
		symbolInstanceList.setEnabled(false);
	}
}
