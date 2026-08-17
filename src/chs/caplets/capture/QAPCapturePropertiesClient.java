/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.capture;

import chs.caf.caplet.IPropertiedSet;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.Model;
import chs.caplets.shared.properties.QAPLogicPropertiedSet;
import org.jetbrains.annotations.NotNull;

/**
 * A specialized properties client of the {@link CaptureCaplet} for the Quick Edit Panel,
 * also known as the QuickAccessPanel.
 *
 * <p>The {@code QAPLogicPropertiesClient} class extends {@code PropertiesClient} to provide
 * specialized behavior for the Quick Edit Panel. This class is responsible for creating
 * an instance of {@link QAPLogicPropertiedSet}, which is designed to bypass the standard
 * locking operations performed by the superclass.</p>
 */
public class QAPCapturePropertiesClient extends CapturePropertiesClient
{

	QAPCapturePropertiesClient(Model model, boolean willEditSharedObjects)
	{
		super(model, willEditSharedObjects);
	}

	@NotNull
	protected IPropertiedSet doGetPropertiedSet(@NotNull SelectSet selections, boolean checkArtificiallyReadOnly)
	{
		m_propertiedSet =
				new QAPLogicPropertiedSet(selections, getModel(), willEditSharedObjects(), checkArtificiallyReadOnly);
		return m_propertiedSet;
	}
}