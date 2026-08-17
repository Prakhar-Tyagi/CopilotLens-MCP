/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare.ui.detailsPane;

import chs.caf.helpers.GfxViewHelper;
import chs.cof.drawplus.DiagramObjectIterator;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.ISchemDiagram;
import chs.services.gfx.GfxView;
import chs.utility.DiagramHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rmahato
 */
public abstract class InstanceDetailsTableInfo extends DetailsTableInfo
{

	protected IDiagramObject m_diagramObject;
	protected ISchemDiagram instanceDiagram;

	@NotNull protected IDiagramObjectIterator getDiagramObjects(@NotNull ILogicObject selectedObject)
	{
		ILogicDesign design = selectedObject.getLogicDesign();
		assert design != null;

		List<IDiagramObject> representations = new ArrayList<>();
		for (ISchemDiagram diagram : design.getDesignWideUsageMgr().getUsageDiagrams(selectedObject)) {
			representations.addAll(diagram.getRepresentations(selectedObject.getUID()).stream()
					.collect(Collectors.toSet()));
		}

		return new DiagramObjectIterator(representations);
	}

	public void zoomToInstance()
	{
		SwingUtilities.invokeLater(() ->
		{
			final GfxView gfxView = GfxViewHelper.openLogicDiagram(instanceDiagram);
			if (gfxView != null) {
				selectAndZoomOnInstance(gfxView);
			}
		});
	}

	protected void selectAndZoomOnInstance(@NotNull GfxView gfxView)
	{
		GfxViewHelper.locateAndSelectObject(gfxView, m_diagramObject, true, false, true, true);
		GfxViewHelper.zoomSelected(gfxView, true);
	}

	@NotNull @Override Collection<DetailsTableInfo> getTableData(@Nullable ILogicObject selectedObject)
	{
		List<DetailsTableInfo> rows = new ArrayList<>();
		if (selectedObject == null) {
			return rows;
		}

		IDiagramObjectIterator it = getDiagramObjects(selectedObject);
		while (it.hasNext()) {
			IDiagramObject diagramObject = it.next();
			ISchemDiagram diagram = DiagramHelper.getDiagram(diagramObject);
			assert diagram != null;
			rows.add(createInstanceDetailObject(diagram, diagramObject));
		}

		return rows;
	}

	@NotNull protected abstract DetailsTableInfo createInstanceDetailObject(@NotNull ISchemDiagram diagram,
			@NotNull IDiagramObject diagramObject);
}
