/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024-2025 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.caplets.logic.actions.shared.batchshare.ui.detailsPane.AbstractShareDetailsPane;
import chs.caplets.logic.actions.ui.ManageConnectorsDialog;
import chs.ctf.caf.ui.SimpleOkCancelDialog;
import chs.utilities.ResourceMgr;
import com.mentor.capital.javafx.table.Table;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;

/**
 * Abstract batch share dialog
 */
public abstract class AbstractBatchShareDialog extends SimpleOkCancelDialog
{

	public static final int MIN_WIDTH = 800;
	public static final int MIN_HEIGHT = 600;
	public static final String BATCH_SHARE_CSS = "batchshare";
	public static final String HELP_ID = "logic_action_batchshare_dialog";
	public static final String SHARE_BUTTON = "Share";
	protected IBatchShareParams params;
	protected BatchShareTableView batchShareTableView;

	private ShareTableStateManager<IBatchShareRow> tableStateManager;

	protected AbstractBatchShareDialog(@Nullable Frame frame, @Nullable String title, IBatchShareParams params)
	{
		super(frame, title);
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
		Label selectionLabel = new Label();
		selectionLabel.setId("ObjectsSelectedLabel");
		selectionLabel.setText(ResourceMgr.getString(getClass(), "BatchShareDialog.objectsSelected.text", 0));
		batchShareTableView = createBatchShareTableView(selectionLabel);
		Table<IBatchShareRow> table = batchShareTableView.getTablePane();
		tableStateManager = createShareTableStateManager(table, selectionLabel);
		return createShareTableLayout(table, selectionLabel);
	}

	@Override protected void initOKButton()
	{
		super.initOKButton();
		JButton okButton = getOkButton();
		okButton.setText(ResourceMgr.getString(AbstractBatchShareDialog.class, "BatchShareDialog.shareButton.text"));
		okButton.setName(SHARE_BUTTON);
	}

	@Override protected boolean vetoOKAction()
	{
		if (super.vetoOKAction()) {
			return true;
		}
		return ShareConfirmationHandler.confirmAction(this, "BatchShareDialog.confirmationPrompt",
				"BatchShareDialog.confirmationPrompt.choice.proceed",
				"BatchShareDialog.confirmationPrompt.choice.cancel", tableStateManager.getSelectedCount());
	}

	private void adjustMinimumSize()
	{
		setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
	}

	@Override protected void prepareToApplyEdits()
	{
		batchShareTableView.applyEdits();
	}

	/**
	 * Constructs batch share table
	 *
	 * @param objectSelectedLabel label for providing objects selected information
	 * @return batch share table view
	 */
	@NotNull protected abstract BatchShareTableView createBatchShareTableView(@NotNull Label objectSelectedLabel);

	/**
	 * Constructs batch share table control pane provider
	 *
	 * @return batch share table control pane provider
	 */
	@NotNull protected abstract AbstractShareTableControlPaneProvider<IBatchShareRow, IShareTableView<IBatchShareRow>> createBatchShareTableControlPaneProvider();

	/**
	 * Constructs batch share table details pane
	 *
	 * @param tablePane batch share table
	 * @return batch share table details pane
	 */
	@NotNull protected abstract AbstractShareDetailsPane<IBatchShareRow> createBatchShareDetailsPane(
			@NotNull Table<IBatchShareRow> tablePane);

	protected void applyStyleSheet(@NotNull Scene scene)
	{
		String stylesheet = ResourceMgr.getStylesheet(AbstractBatchShareDialog.class, BATCH_SHARE_CSS);
		scene.getStylesheets().add(stylesheet);
	}

	@NotNull protected SharePanelManager createSharePanelManager()
	{
		return new SharePanelManager(this, "BatchShareFXPanel");
	}

	@NotNull
	protected ShareTableStateManager<IBatchShareRow> createShareTableStateManager(@NotNull Table<IBatchShareRow> table,
			@NotNull Label selectionLabel)
	{
		return new ShareTableStateManager<>(table, getOkButton(), getCancelButton(), selectionLabel,
				IBatchShareRow::isSelected, BatchShareColumn.SELECTION.getName(), getClass(),
				"BatchShareDialog.objectsSelected.text", "BatchShareDialog.shareButton.disabledTooltipText", null);
	}

	@NotNull protected VBox createShareTableLayout(@NotNull Table<IBatchShareRow> table, @NotNull Label selectionLabel)
	{
		return ShareTableLayoutManager.buildLayout(table, createBatchShareTableControlPaneProvider().getControlPane(),
				createBatchShareDetailsPane(table), selectionLabel);
	}
}
