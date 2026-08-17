/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.shared.properties;

import chs.caf.caplet.IPropertiedSet;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.Model;
import org.jetbrains.annotations.NotNull;

/**
 * A specialized properties client for the Quick Edit Panel, also known as the QuickAccessPanel.
 *
 * <p>The {@code QAPLogicPropertiesClient} class extends {@code PropertiesClient} to provide
 * specialized behavior for the Quick Edit Panel. This class is responsible for creating
 * an instance of {@link QAPLogicPropertiedSet}, which is designed to bypass the standard
 * locking operations performed by the superclass.</p>
 *
 */
public class QAPLogicPropertiesClient extends PropertiesClient
{

	private boolean willEditSharedObjects;

	public QAPLogicPropertiesClient(Model model, boolean willEditSharedObjects)
	{
		super(model, willEditSharedObjects);
		this.willEditSharedObjects = willEditSharedObjects;
	}

	@NotNull
	protected IPropertiedSet doGetPropertiedSet(@NotNull SelectSet selections, boolean checkArtificiallyReadOnly)
	{
		// Need to store the propertied set in case any client wants to check if the
		// graphics in the propertied set are editable.
		m_propertiedSet =
				new QAPLogicPropertiedSet(selections, getModel(), willEditSharedObjects, checkArtificiallyReadOnly);
		return m_propertiedSet;
	}
}