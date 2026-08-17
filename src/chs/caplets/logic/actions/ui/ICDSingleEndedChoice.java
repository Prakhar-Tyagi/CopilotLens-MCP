/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.ui;

import chs.cof.project.IProject;
import chs.common.IProjectPreferenceMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.Choice;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Abstract class to compute user's choice in generation of single ended conductors in update ICD related actions
 */

public abstract class ICDSingleEndedChoice
{
	@Nullable private IProject project;

	protected ICDSingleEndedChoice(@Nullable IProject project)
	{
		this.project = project;
	}

	@NotNull protected abstract String getPrefKey();
	protected abstract boolean shouldGetChoiceFromUser();
	@NotNull protected abstract String getMessageResourceKey();

	public boolean isSingleEndedGenerationNeeded()
	{
		if (!isPreferenceEnabled()) {
			return false;
		}

		String prefKey = getPrefKey();
		String prefValue = getRememberedUserChoice(prefKey);
		if (prefValue != null) {
			return Boolean.parseBoolean(prefValue);
		}

		boolean userChoiceNeeded = shouldGetChoiceFromUser();
		if (userChoiceNeeded) {
			return getUserChoiceAndRememberIfRequired(prefKey, getMessageResourceKey());
		}
		return false;
	}

	@Nullable private String getRememberedUserChoice(@NotNull String prefKey)
	{
		Preferences m_prefNode = Preferences.userNodeForPackage(getClass());
		String prefValue = m_prefNode.get(prefKey, null);
		return prefValue;
	}

	private boolean isPreferenceEnabled()
	{
		IProjectPreferenceMgr preferences = project != null ? project.getPreferences() : null;
		return Objects.requireNonNull(preferences).getGenerateConductorsOnICDPins();
	}

	private boolean getUserChoiceAndRememberIfRequired(@NotNull String prefKey, @NotNull String messageResourceKey)
	{
		ResourceBasedMessageContent
				content = new ResourceBasedMessageContent(this, messageResourceKey);
		Choice createChoice =
				new Choice(this, "UpdateICDAction.userChoice.choices.create", Choice.DefaultSetting.DEFAULT);
		Choice skipChoice = new Choice(this, "UpdateICDAction.userChoice.choices.skip");
		String checkboxText = ResourceMgr.getString(this, "UpdateICDAction.userChoice.checkboxText");
		UpdateICDConfirmChoiceDialog dialog =
				createConfirmChoiceDialog(prefKey, content, skipChoice, createChoice, checkboxText);
		dialog.setVisible(true);
		return dialog.getChoiceMade() == createChoice;
	}

	@NotNull
	protected UpdateICDConfirmChoiceDialog createConfirmChoiceDialog(@NotNull String prefKey,
			@NotNull ResourceBasedMessageContent content,
			@NotNull Choice skipChoice, @NotNull Choice createChoice,
			@NotNull String checkboxText)
	{
		return new UpdateICDConfirmChoiceDialog(prefKey, content, skipChoice, createChoice, checkboxText);
	}
}
