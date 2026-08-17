/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.harness.propagate;

import chs.caf.CAFUtils;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.action.IActionMgr;
import chs.caplets.logic.actions.PropagateAllHarnessAction;
import chs.caplets.logic.actions.PropagateSelectedHarnessAction;
import chs.caplets.logic.actions.shared.CheckBoxMenuItemProvider;
import chs.caplets.logic.actions.shared.utils.StatusMessageTableWindow;
import chs.common.IUID;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
import chs.utility.ui.HelpProviderHandler;
import chs.utility.ui.IHelpProvider;
import com.mentor.capital.javafx.table.Table;
import com.mentor.lookandfeel.JavaFXLib.JFXFlatUIUtils;
import com.sun.javafx.application.PlatformImpl;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.SwingUtilities;
import java.awt.BorderLayout;

/**
 * Output window tab for showing messages in FX table
 */
public class HarnessPropagateTableWindow extends StatusMessageTableWindow<IHarnessPropagateStatusMessage>
		implements IHelpProvider
{

	private static final String HARNESS_PROPAGATE_HELP_ID = "logic_harness_propagate_window";
	private static final String HARNESS_PROPAGATE_CSS = "harness_propagate";
	private static final int BUTTON_SIZE = 30;
	//private Button m_previewButton;
	private Button m_propagateSelectedButton;
	private Button m_propagateAllButton;
	@NotNull private HarnessUpdateStatusMessageTableModel m_tableModel;
	@NotNull private IUID m_designUid;

	public HarnessPropagateTableWindow(@NotNull String title, @NotNull IUID designUid,
			@NotNull HarnessUpdateStatusMessageTableModel tableModel)
	{
		super(new BorderLayout(), tableModel);
		m_designUid = designUid;
		m_tableModel = tableModel;
		JFXPanel jfxPanel = new JFXPanel();
		buildUIComponents(jfxPanel);
		add(jfxPanel, BorderLayout.CENTER);
		addAsOutputWindowTab(title, true);
	}

	private void buildUIComponents(JFXPanel jfxPanel)
	{
		PlatformImpl.runAndWait(() -> {
			jfxPanel.setName("HarnessPropagateWindowFXPanel");
			VBox rootVBox = new VBox();
			HBox hBox = createActionPane();
			rootVBox.getChildren().add(hBox);
			Table<IHarnessPropagateStatusMessage> table = getTable();
			if (table != null) {
				rootVBox.getChildren().add(table);
				table.prefWidthProperty().bind(rootVBox.widthProperty());
				table.prefHeightProperty().bind(rootVBox.heightProperty());
				customizeTableHandling(table);
			}
			Scene scene = new Scene(rootVBox);
			applyStyleSheet(scene);
			jfxPanel.setScene(scene);
		});
	}

	private void customizeTableHandling(Table<IHarnessPropagateStatusMessage> table)
	{
		table.addValueChangeListener((sourceItem, sourceColumnInfo, oldValue, newValue) -> {
			if (HarnessPropagateColumn.Propagate.getName().equals(sourceColumnInfo.getName())) {

			}
		});
		table.addValueChangeListener(new HarnessPropagateTableCellValueChangeListener(table));
		table.setCellStateHandler(new HarnessPropagateTableCellStateHandler());
		table.addMenuItemProvider(new HarnessPropagateCheckBoxMenuItemProvider(table,
				HarnessPropagateColumn.Propagate.getName(), CheckBoxMenuItemProvider.Action.SELECT_ALL));
		table.addMenuItemProvider(new HarnessPropagateCheckBoxMenuItemProvider(table,
				HarnessPropagateColumn.Propagate.getName(), CheckBoxMenuItemProvider.Action.CLEAR_ALL));
	}

	private void applyStyleSheet(Scene scene)
	{
		JFXFlatUIUtils.getInstance().setFlatUIFor(scene);
		String stylesheet = ResourceMgr.getStylesheet(HarnessPropagateTableWindow.class, HARNESS_PROPAGATE_CSS);
		scene.getStylesheets().add(stylesheet);
	}

	@NotNull private HBox createActionPane()
	{
		HBox hBox = new HBox(5);
		//hBox.getChildren().add(constructPreviewButton());
		hBox.getChildren().add(constructPropagateSelectedButton());
		hBox.getChildren().add(constructPropagateAllButton());
		Pane rightSpacer = new Pane();
		HBox.setHgrow(rightSpacer, Priority.ALWAYS);
		hBox.getChildren().add(rightSpacer);
		hBox.getChildren().add(constructHelpButton());
		return hBox;
	}

//	@NotNull private Node constructPreviewButton()
//	{
//		String fullPath = "chs/images/app/show.jpg";
//		String name = "HanressPropagatePreviewButton";
//		String tooltip = ResourceMgr.getString(this, "HarnessPropagateTableWindow.previewButton.tooltip");
//		m_previewButton = constructButton(fullPath, name, tooltip, this::onPreviewButtonPressed);
//		return m_previewButton;
//	}

	@NotNull private Node constructPropagateSelectedButton()
	{
		String fullPath = "chs/images/javafx_ui/propagate-selected-small.png";
		String name = "HarnessPropagateSelectedButton";
		String tooltip = ResourceMgr.getString(this, "HarnessPropagateTableWindow.propagateSelectedButton.tooltip");
		m_propagateSelectedButton = constructButton(fullPath, name, tooltip, this::onPropagateSelectedButtonPressed);
		return m_propagateSelectedButton;
	}

	@NotNull private Node constructPropagateAllButton()
	{
		String fullPath = "chs/images/javafx_ui/propagate-all-small.png";
		String name = "HarnessPropagateAllButton";
		String tooltip = ResourceMgr.getString(this, "HarnessPropagateTableWindow.propagateAllButton.tooltip");
		m_propagateAllButton = constructButton(fullPath, name, tooltip, this::onPropagateAllButtonPressed);
		return m_propagateAllButton;
	}

	@NotNull private Node constructHelpButton()
	{
		String fullPath = "chs/images/javafx_ui/infohub-small.png";
		String name = "HarnessPropagateHelpButton";
		String tooltip = ResourceMgr.getString(this, "HarnessPropagateTableWindow.helpButton.tooltip");
		return constructButton(fullPath, name, tooltip, this::onHelpButtonPressed);
	}

//	private void onPreviewButtonPressed(ActionEvent event)
//	{
//		SwingUtilities.invokeLater(() -> {
//		});
//	}

	private void onPropagateSelectedButtonPressed(ActionEvent event)
	{
		SwingUtilities.invokeLater(() -> {
			IActionMgr activeActionMgr = getActiveActionMgr();
			IAction action =
					activeActionMgr != null ? activeActionMgr.findAction(PropagateSelectedHarnessAction.class.getName()) : null;
			if (action != null) {
				AutoPropagateHarnessController.getInstance().loadObjects(m_designUid, m_tableModel, false);
				java.awt.event.ActionEvent
						ae = new java.awt.event.ActionEvent(this, java.awt.event.ActionEvent.ACTION_PERFORMED, "");
				activeActionMgr.actionPerformed(action, ae);
			}
			else {
				reportError();
			}
		});
	}

	private void onPropagateAllButtonPressed(ActionEvent event)
	{
		SwingUtilities.invokeLater(() -> {
			IActionMgr activeActionMgr = getActiveActionMgr();
			IAction action =
					activeActionMgr != null ? activeActionMgr.findAction(PropagateAllHarnessAction.class.getName()) : null;
			if (action != null) {
				AutoPropagateHarnessController.getInstance().loadObjects(m_designUid, m_tableModel, true);
				java.awt.event.ActionEvent
						ae = new java.awt.event.ActionEvent(this, java.awt.event.ActionEvent.ACTION_PERFORMED, "");
				activeActionMgr.actionPerformed(action, ae);
			}
			else {
				reportError();
			}
		});
	}

	@Nullable private IActionMgr getActiveActionMgr()
	{
		return CAFUtils.getInstance().getActiveActionMgr();
	}

	private void reportError()
	{
		HarnessPropagateTableUtils.showPropagateErrorMessage(m_designUid);
	}

	private void onHelpButtonPressed(ActionEvent event)
	{
		SwingUtilities.invokeLater(() -> {
			new HelpProviderHandler(this).handleHelpEvent();
		});
	}

	@NotNull private Button constructButton(@NotNull String fullPath, @NotNull String name, @NotNull String tooltip,
			@NotNull EventHandler<ActionEvent> value)
	{
		Button previewButton = new Button();
		previewButton.setId(name);
		previewButton.setGraphic(new ImageView(CHSImageLoader.loadJFXImage(fullPath)));
		previewButton.setTooltip(new Tooltip(tooltip));
		previewButton.setOnAction(value);
		previewButton.setMaxSize(BUTTON_SIZE, BUTTON_SIZE);
		previewButton.setMinSize(BUTTON_SIZE, BUTTON_SIZE);
		return previewButton;
	}

	@NotNull @Override public String getHelpID()
	{
		return HARNESS_PROPAGATE_HELP_ID;
	}

	@Override public void setHelpID(String helpID)
	{

	}

	public void clearHarnessPropagateTable()
	{
		Table<IHarnessPropagateStatusMessage> table = getTable();
		if (table != null) {
			table.removeData(m_tableModel.getRows());
			m_tableModel.clearAllRows();
		}
	}
}