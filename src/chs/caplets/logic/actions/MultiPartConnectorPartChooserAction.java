/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2017-2026 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.IFIB;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.helpers.JScrollPopupHeaderUtils;
import chs.caf.caplet.helpers.LibraryControl;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.shared.properties.PropertiedSet;
import chs.cof.logical.IAssociateLibraryPartCommand;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IPlugConnector;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.parts.ILibraryBaseConnector;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.Library;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cofUtils.parts.PartNumberHelper;
import chs.system.FactoryMgr;
import chs.system.ILogicUtilsFactory;
import chs.utilities.AlphaNumComparator;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.IAuditTrailLogger;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.audit.AuditableEventType;
import chs.utility.helpers.LogHelper;
import chs.utility.helpers.LogTabType;
import chs.utility.helpers.MultiPartConnectorUtils;
import chs.utility.helpers.PinListHelper;
import chs.utility.ui.IconUtils;
import chs.utility.ui.menu.JScrollPopupMenu;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * * Created by nagamani on 04-07-2017.
 */

public class MultiPartConnectorPartChooserAction extends ControllerActionRT implements MouseListener
{

	private static final int mMaxCharInDescription = 32;
	protected final Point cursorLocation;   //current cursor location to launch the popup menu at
	protected IConnector m_connector = null;
	protected IPinList m_schemConnector = null;
	protected ILibraryBaseConnector choosenPartNumber = null;
	protected Component m_eventSource = null;
	protected boolean isFrozen;

	public MultiPartConnectorPartChooserAction(ICapletController controller, MouseEvent e)
	{
		super(controller);
		cursorLocation = e.getPoint();
	}

	@Override protected IActionEnum onActivate(ActionEvent e)
	{
		reset();
		SelectSet currentSelections = getController().getSelectMgr().getCurrentSelections();
		m_schemConnector = MultiPartConnectorUtils.specialConnectorInSelection(currentSelections);
		if (m_schemConnector == null) {
			return IActionEnum.eCanceled;
		}

		m_connector = (IConnector) m_schemConnector.getConnectivity();
		if (m_connector == null) {
			return IActionEnum.eCanceled;
		}
		ILogicDesign logicDesign = m_connector.getLogicDesign();
		assert logicDesign != null;
		m_eventSource = CommonUtils.cast(e.getSource(), Component.class);
		if (m_eventSource == null) {
			return IActionEnum.eCanceled;
		}
		LibraryControl libraryControl = new LibraryControl();
		//LOGIC-6682 CT171BashSPEDSI15: chs.utilities.WrappingRuntimeException: While selecting & changing Part
		//We will allow the multi-part select action only if selection count is one and the selected one is a
		//schem connector. Otherwise library control will crash if heterogenous selections are allowed.
		SelectSet filteredSelection = new SelectSet(m_schemConnector);
		PropertiedSet propertiedSet = new PropertiedSet(filteredSelection, getCapletModel(), false, true);
		libraryControl.doPartSelection(propertiedSet, false, logicDesign, null,
				Library.getInstance().getLibraryPartSelector(), new StringBuilder(), true, false);
		ILibraryPartSelection librarySelection = libraryControl.getLibrarySelection();
		Collection<ILibraryObject> selectedObjects =
				(librarySelection != null) ? librarySelection.getSelectedObjects() :
						Collections.emptyList();
		Set<ILibraryBaseConnector> partnumberchoice =
				PinListHelper.determineValidPartNumbersBasedUponMates(m_connector, p -> selectedObjects.contains(p));
		// Initialize the dialog
		ISharedPinList sharedPinList = m_connector.getSharedPinList();
		isFrozen = sharedPinList != null && sharedPinList.isFrozen();

		showDialog(partnumberchoice);
		return IActionEnum.eActivated;
	}

	private void reset()
	{
		m_connector = null;
		m_schemConnector = null;
		choosenPartNumber = null;
		m_eventSource = null;
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		boolean returnStatus = doEdit(successful);
		reset();
		return returnStatus;
	}

	private boolean doEdit(boolean successful)
	{
		if (successful && choosenPartNumber != null && !isFrozen) {
			ILogicUtilsFactory logicUtilsFactory = FactoryMgr.getLogicalFactory().getLogicUtilsFactory();
			if (logicUtilsFactory == null) {
				return false;
			}
			IAssociateLibraryPartCommand cmd =
					logicUtilsFactory.createAssociateLibraryPartCmd(m_schemConnector, choosenPartNumber);
			if (cmd == null) {
				return false;
			}
			boolean success = false;
			if (cmd.prepare()) {
				success = cmd.execute();
			}

			String key = success ? "MultiPartConnectorPartChooserAction.success.msg" :
					"MultiPartConnectorPartChooserAction.failed.msg";
			LogHelper.printMsg(LogTabType.TAB_BULP,
					ResourceMgr.getString(MultiPartConnectorPartChooserAction.class, key,
							choosenPartNumber.getPartNumber(), m_connector.getName()));
			if (m_connector.isShared()) {
				// Editing of shared objects is not undoable
				getController().getUndoableContainer().endEdit();
				getController().clearUndoQueue();
				if (success) {
					postAuditTrailForSharedObjectUpdate();
				}
			}
			return true;
		}
		return false;
	}

	private void postAuditTrailForSharedObjectUpdate()
	{
		ISharedPinList sharedPinList = m_connector.getSharedPinList();
		assert sharedPinList != null;
		IAuditTrailLogger auditLogger = getiAuditTrailLogger();
		auditLogger.postEvent(AuditableEventType.SHARED_OBJECT_MODIFIED, null,
				sharedPinList.getProject().getUID().toString(), sharedPinList.getFullName(),
				sharedPinList.getUID().toString());
	}

	@NotNull protected IAuditTrailLogger getiAuditTrailLogger()
	{
		return FactoryMgr.getSystemFactory().getCHSSystem().getAuditLogger();
	}

	protected void showDialog(Set<ILibraryBaseConnector> partnumberchoice)
	{
		MultiPartConnectorPartChooserAction.PartNumberList menu =
				new MultiPartConnectorPartChooserAction.PartNumberList(m_connector, partnumberchoice, true);
		menu.show(m_eventSource, doubleToInt(cursorLocation.getX()), doubleToInt(cursorLocation.getY()));
	}

	public String getActionUIClass()
	{
		return MultiPartConnectorPartChooserActionUI.class.getName();
	}

	@NotNull @Override public Action getActionUI()
	{
		return new MultiPartConnectorPartChooserActionUI(getController().getCaplet());
	}

	public boolean isEnabled()
	{
		return !(CAFUtils.getInstance().getFIB().isTaskActive(IFIB.TASK_SAVE) ||
				CAFUtils.getInstance().getUserSession() == null)
				&& getController().getCapletModel().isEditable()
				&& super.isEnabled();
	}

	@Override public void mouseClicked(MouseEvent e)
	{
	}

	@Override public void mousePressed(MouseEvent e)
	{

	}

	@Override public void mouseReleased(MouseEvent e)
	{
		getController().getActionMgr().terminateActiveAction(true);
	}

	@Override public void mouseEntered(MouseEvent e)
	{

	}

	@Override public void mouseExited(MouseEvent e)
	{

	}

	protected class PartNumberList extends JScrollPopupMenu
	{

		private IConnector connector;
		private static final int MAXIMUM_VISIBLE_ROWS_IN_POPUP = 25;

		PartNumberList(IConnector conn,
				Set<ILibraryBaseConnector> partnumberchoiceList, boolean sortByName)
		{
			setName("SelectObjectPopup"); //for automation function use
			mMaximumVisibleRows = MAXIMUM_VISIBLE_ROWS_IN_POPUP;
			Set<ILibraryBaseConnector> partnumberchoice = new HashSet<>(partnumberchoiceList);
			connector = conn;
			ILibraryBaseConnector libraryBaseConnector =
					CommonUtils.cast(connector.getLibraryObject(), ILibraryBaseConnector.class);
			boolean existingPartIsValid = true;
			if (libraryBaseConnector != null) {
				existingPartIsValid = partnumberchoice.contains(libraryBaseConnector);
				partnumberchoice.add(libraryBaseConnector);
			}
			JLabel header = createHeader(partnumberchoice);
			add(header);
			add(new JSeparator());
			Map<String, List<JMenuItem>> items = new HashMap<String, List<JMenuItem>>();
			int columnWidth = 0;
			for (ILibraryBaseConnector selObj : partnumberchoice) {
				JMenuItem item = createMenuItem(selObj);
				String text = item.getText();
				List<JMenuItem> itemList = items.get(text);
				if (itemList == null) {
					itemList = new ArrayList<JMenuItem>();
					items.put(text, itemList);
				}
				Integer fontStyle = null;
				if (selObj.getRevisionStatus().isTrue()) {
					fontStyle = Font.ITALIC;
					item.setToolTipText(ResourceMgr.getString(MultiPartConnectorPartChooserAction.class,
							"MultiPartConnectorPartChooserAction.latest.tooltip"));
				}
				if (libraryBaseConnector != null && libraryBaseConnector.equals(selObj)) {
					int bold = Font.BOLD;
					fontStyle = fontStyle != null ? fontStyle | bold : bold;
					if (!existingPartIsValid) {
						item.setForeground(Color.RED);
					}
					item.setToolTipText(ResourceMgr.getString(MultiPartConnectorPartChooserAction.class,
							"MultiPartConnectorPartChooserAction.assigned.tooltip"));
				}
				if (fontStyle != null) {
					item.setFont(item.getFont().deriveFont(fontStyle));
				}
				if (isFrozen) {
					item.setEnabled(false);
					item.setToolTipText(ResourceMgr.getString(MultiPartConnectorPartChooserAction.class,
							"MultiPartConnectorPartChooserAction.frozen.tooltip"));
				}
				itemList.add(item);
				columnWidth = text.length() > columnWidth ? text.length() : columnWidth;
			}

			if (isFrozen) {
				header.setEnabled(false);
				header.setToolTipText(ResourceMgr.getString(MultiPartConnectorPartChooserAction.class,
						"MultiPartConnectorPartChooserAction.frozen.tooltip"));
			}

			mMaximumVisibleCols = columnWidth;

			List<String> names = new ArrayList<String>(items.keySet());
			if (sortByName) {
				names = CollectionUtils.createSortedList(names.iterator(), new AlphaNumComparator<String>());
			}
			for (String label : names) {
				List<JMenuItem> itemList = items.get(label);
				for (JMenuItem item : itemList) {
					add(item);
				}
			}
			setSizeEnsuringHeaderIsVisible(header);
		}

		private JMenuItem createMenuItem(final ILibraryBaseConnector obj)
		{
			JMenuItem item = new JMenuItem()
			{
				@Override public String getText()
				{
					String description = obj.getDescription();
					String fullName = PartNumberHelper.partFullName(obj.getPartNumber(), obj.getPartRevision());
					if (description.length() > mMaxCharInDescription) {
						description = description.substring(0, mMaxCharInDescription) + "...";
					}
					if (fullName.length() > mMaxCharInDescription) {
						fullName = fullName.substring(0, mMaxCharInDescription) + "...";
					}
					return StringUtils.isBlank(description) ? fullName : fullName + " (" + description + ")";
				}
			};

			item.setIcon(IconUtils.getIcon(IPlugConnector.class));
			item.addMouseListener(getMouseListener(obj));
			return item;
		}

		protected MouseListener getMouseListener(ILibraryBaseConnector obj)
		{
			return new popUpMouseListener(obj);
		}

		private JLabel createHeader(Set<ILibraryBaseConnector> partnumberchoice)
		{
			//provide some leading space so that the menuItem comes after the vertical line.
			// Or you could provide an icon for the menuItem!
			boolean empty = partnumberchoice.isEmpty();
			String headerText = ResourceMgr.getString(MultiPartConnectorPartChooserAction.class,
					empty ? "MultiPartConnectorPartChooserAction.noparts.decl" :
							"MultiPartConnectorPartChooserAction.String.decl");
			JLabel header = new JLabel(headerText, SwingConstants.CENTER);
			header.setHorizontalAlignment(SwingConstants.CENTER);
			header.setFont(header.getFont().deriveFont(Font.BOLD));
			if (empty) {
				header.setForeground(Color.RED);
			}
			//allow dragging of the menu by clicking on the title bar
			header.addMouseMotionListener(JScrollPopupHeaderUtils.getHeaderMouseAdapter());

			//show move cursor while the mouse is on the header
			header.addMouseListener(JScrollPopupHeaderUtils.getHeaderMouseListener(this));
			return header;
		}

		private void setSizeEnsuringHeaderIsVisible(JLabel header)
		{
			int headerLabelWidthReqd = getWidthRequired(header);
			int actualWidth = getPreferredSize().width;
			int preferredWidth = actualWidth > headerLabelWidthReqd ? actualWidth : headerLabelWidthReqd;
			setPopupSize(preferredWidth, getPreferredSize().height);
		}

		int getWidthRequired(JLabel header)
		{
			final int PADDING = 50;
			FontMetrics fontMetrics = header.getFontMetrics(header.getFont());
			return fontMetrics.stringWidth(header.getText()) + PADDING;
		}
	}

	private int doubleToInt(double x)
	{
		//noinspection NumericCastThatLosesPrecision
		return (int) x;
	}

	protected class popUpMouseListener implements MouseListener
	{

		private ILibraryBaseConnector mObj = null;

		popUpMouseListener(ILibraryBaseConnector obj)
		{
			mObj = obj;
		}

		@Override public void mouseClicked(MouseEvent e)
		{
		}

		@Override public void mousePressed(MouseEvent e)
		{
		}

		@Override public void mouseReleased(MouseEvent e)
		{
			choosenPartNumber = mObj;
			getController().getActionMgr().terminateActiveAction(true);
		}

		@Override public void mouseEntered(MouseEvent e)
		{
		}

		@Override public void mouseExited(MouseEvent e)
		{
		}
	}
}

