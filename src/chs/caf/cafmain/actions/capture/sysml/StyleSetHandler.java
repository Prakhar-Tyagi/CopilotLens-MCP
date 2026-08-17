/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caf.cafmain.actions.capture.sysml;

import chs.bridges.adaptors.tcmbse.diagramgenerator.StyleSetInfo;
import chs.common.preferencesets.IFunctionDesignPreferenceSet;
import chs.common.preferencesets.IPreferenceSet;
import chs.utilities.ResourceMgr;
import chs.utility.preferences.StyleSetLicenseUtils;
import chs.utility.preferences.StyleSetUtils;
import chs.bridges.adaptors.tcmbse.TCMbseLoggerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles the processing of style sets given in config file.
 */
public class StyleSetHandler
{

	/**
	 * Processes and retrieves the style set based on the provided style set name.
	 * If the style set name is {@code null} or the default name, it returns the default style set.
	 * If a valid style set name is provided and the styling license is available, it returns the corresponding style set.
	 * If the styling license is not available or the style set name is invalid, it returns the default style set.
	 *
	 * @param styleSetName the name of the style set to be applied. Can be {@code null}.
	 * @return a {@link StyleSetInfo} object containing the style set and a boolean indicating if the style set can be applied.
	 */
	@NotNull public StyleSetInfo processStyleSets(@Nullable String styleSetName)
	{
		IPreferenceSet defaultStyleSet = StyleSetUtils.getDefaultStyleSet(IFunctionDesignPreferenceSet.class);
		if (styleSetName == null || styleSetName.equals(IPreferenceSet.DEFAULT_NAME)) {
			return new StyleSetInfo(defaultStyleSet, true);
		}

		if (!StyleSetLicenseUtils.isStyleMgrLicenseOn()) {
			logWarning("StyleSetHandler.unavailableLicense.message");
			return new StyleSetInfo(defaultStyleSet, false);
		}

		IPreferenceSet preferenceSet = StyleSetUtils.getPreferenceSet(IFunctionDesignPreferenceSet.class, styleSetName);
		if (preferenceSet == null) {
			logWarning("StyleSetHandler.unavailableStyleSet.message");
			return new StyleSetInfo(defaultStyleSet, false);
		}

		return new StyleSetInfo(preferenceSet, true);
	}

	private void logWarning(String messageKey)
	{
		TCMbseLoggerFactory.getLogger().warn(ResourceMgr.getString(StyleSetHandler.class, messageKey));
	}
}