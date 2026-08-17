/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.utilities.ResourceMgr;
import com.mentor.capital.javafx.table.Table;
import javafx.scene.control.Label;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import java.util.function.Function;

/**
 * Manager for the state and behavior of UI components in share/unshare table dialogs.
 * <p>
 * This class coordinates the state of OK/Cancel buttons and selection label in batch share/unshare operations
 * It maintains the state consistency of these components based on user selections and table events.
 */
public class ShareTableStateManager<T>
{

	private final Table<T> table;
	private final JButton okButton;
	private final JButton cancelButton;
	private final Label selectionLabel;
	private final Function<T, Boolean> isSelectedFunction;
	private final String selectionColumnName;
	private final String objectsSelectedKey;
	private final String disabledTooltipKey;
	private final String enabledTooltipKey;
	private final Class<?> resourceContext;

	public ShareTableStateManager(@NotNull Table<T> table, @NotNull JButton okButton, @NotNull JButton cancelButton,
			@NotNull Label selectionLabel, @NotNull Function<T, Boolean> isSelectedFunction,
			@NotNull String selectionColumnName, @NotNull Class<?> resourceContext, @NotNull String objectsSelectedKey,
			@NotNull String disabledTooltipKey, @Nullable String enabledTooltipKey)
	{
		this.table = table;
		this.okButton = okButton;
		this.cancelButton = cancelButton;
		this.selectionLabel = selectionLabel;
		this.isSelectedFunction = isSelectedFunction;
		this.selectionColumnName = selectionColumnName;
		this.resourceContext = resourceContext;
		this.objectsSelectedKey = objectsSelectedKey;
		this.disabledTooltipKey = disabledTooltipKey;
		this.enabledTooltipKey = enabledTooltipKey;

		setupAllListeners();
		//Update label and button state at initialization
		updateCompleteSelectionState();
	}

	private void setupAllListeners()
	{
		setupSelectionChangeListener();
		setupBulkEditListener();
		setupTableClosingListener();
	}

	private void setupSelectionChangeListener()
	{
		table.addValueChangeListener((sourceItem, sourceColumnInfo, oldValue, newValue) -> {
			if (selectionColumnName.equals(sourceColumnInfo.getName())) {
				boolean selectedRowValue = newValue instanceof Boolean ? (Boolean) newValue : false;
				boolean okButtonEnabled = selectedRowValue ||
						table.getData().stream().filter(row -> row != sourceItem).anyMatch(isSelectedFunction::apply);
				setOkButtonEnabled(okButtonEnabled);
			}
		});
	}

	private void setupBulkEditListener()
	{
		table.addBulkEditCompleteListener(() -> {
			updateCompleteSelectionState();
		});
	}

	private void setupTableClosingListener()
	{
		table.tableClosingProperty().addListener((observable, oldValue, newValue) -> cancelButton.doClick());
	}

	private void updateCompleteSelectionState()
	{
		int selectedCount = getSelectedCount();
		setOkButtonEnabled(selectedCount > 0);
		updateSelectionLabel(selectedCount);
	}

	private void setOkButtonEnabled(boolean enable)
	{
		okButton.setEnabled(enable);

		String tooltip = enable ? getEnabledTooltip() : getDisabledTooltip();

		okButton.setToolTipText(tooltip);
	}

	@Nullable private String getEnabledTooltip()
	{
		return enabledTooltipKey != null ? ResourceMgr.getString(resourceContext, enabledTooltipKey) : null;
	}

	@NotNull private String getDisabledTooltip()
	{
		return ResourceMgr.getString(resourceContext, disabledTooltipKey);
	}

	private void updateSelectionLabel(int count)
	{
		selectionLabel.setText(ResourceMgr.getString(resourceContext, objectsSelectedKey, count));
	}

	public int getSelectedCount()
	{
		return table.getData().filtered(isSelectedFunction::apply).size();
	}
}