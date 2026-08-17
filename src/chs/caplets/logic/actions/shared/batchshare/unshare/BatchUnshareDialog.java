/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.unshare;

import chs.caplets.logic.actions.shared.batchshare.ui.SharePanelManager;
import chs.caplets.logic.actions.shared.batchshare.ui.ShareTableLayoutManager;
import chs.caplets.logic.actions.shared.batchshare.ui.ShareTableStateManager;
import chs.caplets.logic.actions.ui.ManageConnectorsDialog;
import chs.ctf.caf.ui.SimpleOkCancelDialog;
import chs.images.CHSImageLoader;
import chs.utilities.Environment;
import chs.utilities.ResourceMgr;
import com.mentor.capital.javafx.table.Table;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;

/**
 * Dialog for batch unsharing of redundant shared objects in the logic design.
 * <p>
 * This dialog provides a user interface for managing the unsharing process of multiple
 * shared objects simultaneously.
 *
 */
public class BatchUnshareDialog extends SimpleOkCancelDialog
{

	private static final int MIN_WIDTH = 850;
	private static final int MIN_HEIGHT = 700;
	private static final String HELP_ID = "logic_action_batch_unshare_dialog";
	private static final String UNSHARE_BUTTON = "Unshare";
	private static final String UNSHARE_CSS = "unshare";

	private final IBatchUnshareParams params;
	protected BatchUnshareTableView tableView;

	public BatchUnshareDialog(@Nullable Frame frame, String title, IBatchUnshareParams params)
	{
		super(null, frame, title, !Environment.isImmersedMode());
		this.params = params;
	}

	public boolean showDialog(boolean waitForFx)
	{
		if (waitForFx) {
			ManageConnectorsDialog.waitForFX();
		}
		createDialog();
		setVisible(true);
		return !isCancelled();
	}

	protected void createDialog()
	{
		adjustMinimumSize();
		setHelpID(HELP_ID);

		SharePanelManager panelManager = createSharePanelManager();
		getContentPane().add(panelManager.getPanel(), BorderLayout.CENTER);
		panelManager.initializeScene(this::buildUI, this::applyStyleSheet);
	}

	@NotNull private VBox buildUI()
	{
		VBox warningSection = createWarningSection();
		Label selectionLabel = new Label();
		selectionLabel.setId("ObjectsSelectedLabel");
		selectionLabel.setText(ResourceMgr.getString(getClass(), "BatchUnshareDialog.objectsSelected.text", 0));

		tableView = new BatchUnshareTableView(params.getData(), selectionLabel, "batch_unshare_table");
		Table<IBatchUnshareRow> table = tableView.getTablePane();

		setupShareTableStateManager(table, selectionLabel);

		VBox tableLayout = createShareTableLayout(table, selectionLabel);
		VBox mainLayout = new VBox();
		mainLayout.getChildren().addAll(warningSection, tableLayout);
		VBox.setVgrow(tableLayout, Priority.ALWAYS);

		return mainLayout;
	}

	@Override protected void initOKButton()
	{
		super.initOKButton();
		JButton okButton = getOkButton();
		okButton.setText(ResourceMgr.getString(getClass(), "BatchUnshareDialog.unshareButton.text"));
		okButton.setName(UNSHARE_BUTTON);
	}

	private void adjustMinimumSize()
	{
		setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
	}

	@Override protected void prepareToApplyEdits()
	{
		tableView.applyEdits();
	}

	protected void applyStyleSheet(@NotNull Scene scene)
	{
		String stylesheet = ResourceMgr.getStylesheet(BatchUnshareDialog.class, UNSHARE_CSS);
		scene.getStylesheets().add(stylesheet);
	}

	@NotNull protected SharePanelManager createSharePanelManager()
	{
		return new SharePanelManager(this, "BatchUnshareFXPanel");
	}

	@NotNull protected ShareTableStateManager<IBatchUnshareRow> setupShareTableStateManager(
			@NotNull Table<IBatchUnshareRow> table, @NotNull Label selectionLabel)
	{
		return new ShareTableStateManager<>(table, getOkButton(), getCancelButton(), selectionLabel,
				IBatchUnshareRow::isSelected, BatchUnshareColumn.SELECTION.getName(), getClass(),
				"BatchUnshareDialog.objectsSelected.text", "BatchUnshareDialog.unshareButton.disabledTooltipText",
				"BatchUnshareDialog.unshareButton.enabledTooltipText");
	}

	@NotNull private VBox createWarningSection()
	{
		VBox warningBox = new VBox();
		warningBox.setId("WarningSection");
		VBox.setMargin(warningBox, new Insets(8, 8, 0, 8));

		TextFlow warningTextFlow = new TextFlow();

		ImageView warningIcon = new ImageView(
				CHSImageLoader.loadJFXImage("chs/images/general/siemens_field_warning.png"));

		// Align icon with text
		warningIcon.setTranslateY(3);

		Text warningPrefix = new Text(ResourceMgr.getString(getClass(), "BatchUnshareDialog.warning.prefix"));
		warningPrefix.setId("WarningPrefix");

		Text warningMessage = new Text(ResourceMgr.getString(getClass(), "BatchUnshareDialog.warning.message"));

		warningTextFlow.getChildren().addAll(warningIcon, new Text(" "), warningPrefix, warningMessage);
		warningBox.getChildren().addAll(warningTextFlow);

		return warningBox;
	}

	@NotNull
	protected VBox createShareTableLayout(@NotNull Table<IBatchUnshareRow> table, @NotNull Label selectionLabel)
	{
		return ShareTableLayoutManager.buildLayout(table,
				new BatchUnshareTableControlPaneProvider(this, tableView).getControlPane(),
				new BatchUnshareDetailsPane(table), selectionLabel);
	}
}


