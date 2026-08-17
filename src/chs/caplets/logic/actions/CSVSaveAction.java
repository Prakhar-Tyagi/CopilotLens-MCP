/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions;



import chs.caf.CAFUtils;
import chs.caplets.logic.actions.shared.batchshare.BatchShareStatusMessageTableModel;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

/**
 * Action for exporting batch share status messages to a CSV file.
 * This action is designed to be used as a button in the batch share status dialog window.
 */
public class CSVSaveAction extends AbstractAction
{
	/** The table model containing batch share status messages to be exported. */
	@NotNull private final BatchShareStatusMessageTableModel model;

	/**
	 * Creates a CSVSaveAction with the specified model.
	 *
	 * @param model the model containing batch share status messages to export
	 */
	public CSVSaveAction(@NotNull BatchShareStatusMessageTableModel model)
	{
		this.model = model;

		final String value = ResourceMgr.getString(chs.caf.helpers.ui.common.statusmessage.CSVSaveAction.class, "CSVSaveAction.saveToCSV");
		putValue(SHORT_DESCRIPTION, value);

		// Use actual icon instead of string path to prevent crashes in SF tables when multiple actions are added
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_save_active.gif"));
	}

	/**
	 * Exports the batch share/unshare status messages to a CSV file.
	 */
	@Override
	public void actionPerformed(@NotNull ActionEvent e)
	{
		model.saveToCSV(CAFUtils.getInstance().getDialogFrame());
	}

}
