/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.shared;

import chs.caf.CAFUtils;
import chs.caf.caplet.helpers.UIResourceProvider;
import chs.caf.caplet.helpers.browser.BrowserTreeHelper;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.common.IUIDObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * This class extends {@link UIResourceProvider} and provides APIs to retrieve UI resources for Logic caplet.
 */
public class BaseUIResourceProvider extends UIResourceProvider
{

	@Nullable @Override public Icon getIcon(@NotNull IUIDObject uidObject)
	{
		BrowserTreeHelper browserTreeHelper =
				(BrowserTreeHelper) CAFUtils.getInstance().getActiveCapletController().getActionableBrowser("Diagram");
		if (browserTreeHelper != null) {
			IUIDObject diagramRepresentation = getDiagramRepresentation(uidObject);
			if (diagramRepresentation != null) {
				return browserTreeHelper.getBrowserClient().getIcon(diagramRepresentation.getUID());
			}
			return browserTreeHelper.getBrowserClient().getIcon(uidObject.getUID());
		}
		return super.getIcon(uidObject);
	}

	@Nullable private IUIDObject getDiagramRepresentation(@NotNull IUIDObject uidObject)
	{
		if (CAFUtils.getInstance().getActiveDiagram() != null) {
			IDiagramObjectIterator diagramObjects =
					CAFUtils.getInstance().getActiveDiagram().getRepresentations(uidObject.getUID());
			if (diagramObjects.getSize() == 1) {
				return diagramObjects.next();
			}
			while (diagramObjects.hasNext()) {
				IDiagramObject diagramObject = diagramObjects.next();
				if (CAFUtils.getInstance().getActiveSelectMgr() != null &&
						CAFUtils.getInstance().getActiveSelectMgr().getCurrentSelections()
								.contains(diagramObject.getUID())) {
					return diagramObject;
				}
			}
		}
		return null;
	}
}
