/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.ui;


import chs.utilities.ui.messaging.Choice;
import chs.utilities.ui.messaging.IPromptContent;
import chs.utility.helpers.ConfirmChoiceDialog;
import org.jetbrains.annotations.NotNull;

/**
 * Class to generate a dialog to get user's choice in conductor generation in ICD related updates
 */
public class UpdateICDConfirmChoiceDialog extends ConfirmChoiceDialog
{

	public UpdateICDConfirmChoiceDialog(String prefKey, IPromptContent content,
			@NotNull Choice cancelChoice,
			@NotNull Choice otherChoice, String checkBoxText)
	{
		super(prefKey, content, cancelChoice, otherChoice, checkBoxText);
	}

	@Override protected void savePreference()
	{
		if (isHideMeCheckBoxSelected()) {
			m_prefNode.putBoolean(m_prefKey, !isCancelled());
		}
	}
}
