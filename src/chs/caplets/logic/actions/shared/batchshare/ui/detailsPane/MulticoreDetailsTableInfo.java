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
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.services.gfx.GfxView;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author rmahato
 */
public class MulticoreDetailsTableInfo extends InstanceDetailsTableInfo
{

	private static final String DIAGRAM_COLOUM_NAME = ResourceMgr
			.getString(MulticoreDetailsTableInfo.class, "MulticoreDetailsTableInfo.diagramColoumnName.text");
	private static final String PINLISTS_COLOUM_NAME = ResourceMgr
			.getString(MulticoreDetailsTableInfo.class, "MulticoreDetailsTableInfo.pinlistsColoumnName.text");

	private IUID selectedObjectUID;

	@Override protected void selectAndZoomOnInstance(@NotNull GfxView gfxView)
	{
		IUIDObject object = selectedObjectUID.getObject();
		if (object != null) {
			GfxViewHelper.locateAndSelectObject(gfxView, object);
			GfxViewHelper.zoomSelected(gfxView, true);
		}
	}

	public MulticoreDetailsTableInfo()
	{
		coloumValueMap = new LinkedHashMap<>();
		coloumValueMap.put(DIAGRAM_COLOUM_NAME, "");
		coloumValueMap.put(PINLISTS_COLOUM_NAME, "");
	}

	public MulticoreDetailsTableInfo(@NotNull ISchemDiagram diagram, @NotNull ILogicObject selectedObject)
	{
		instanceDiagram = diagram;
		selectedObjectUID = selectedObject.getUID();
		coloumValueMap = new LinkedHashMap<>();
		coloumValueMap.put(DIAGRAM_COLOUM_NAME, diagram.getName());
		coloumValueMap.put(PINLISTS_COLOUM_NAME, getAllTerminatingPinlistNames(diagram, selectedObject));
	}

	/**
	 * Multicores don't have Schem Represenation, Hence overriden getTableData for seperate handling. using
	 * DesignWideUsageMgr to get Multicore diagram instances
	 */
	@NotNull @Override Collection<DetailsTableInfo> getTableData(@Nullable ILogicObject selectedObject)
	{
		List<DetailsTableInfo> rows = new ArrayList<>();
		if (selectedObject == null) {
			return rows;
		}

		ILogicDesign design = (ILogicDesign) selectedObject.getDesign();
		assert design != null;

		final IDesignWideUsageMgr dwum = design.getDesignWideUsageMgr();
		Iterator<IUID> it = selectedObject.getUIDsForSchemReps().iterator();
		if (!it.hasNext()) {
			return rows;
		}

		Set<IUID> diagramUIDs = new LinkedHashSet<IUID>();
		IMulticore multicore = (IMulticore) selectedObject;

		//get all diagrams containing Multicore instance
		dwum.getMulticoreDiagrams(multicore, diagramUIDs);

		diagramUIDs.forEach(uid -> {
			ISchemDiagram schemDiagram = design.getDiagram(uid);
			if (schemDiagram != null) {
				rows.add(new MulticoreDetailsTableInfo(schemDiagram, selectedObject));
			}
		});

		return rows;
	}

	@NotNull @Override
	protected DetailsTableInfo createInstanceDetailObject(@NotNull ISchemDiagram diagram,
			@NotNull IDiagramObject diagramObject)
	{
		return new MulticoreDetailsTableInfo();
	}

	@NotNull private String getAllTerminatingPinlistNames(@NotNull ISchemDiagram schemDiagram,
			@NotNull ILogicObject selectedObject)
	{
		Set<IPinList> pinOwners = new HashSet<>();
		IMulticore multicore = (IMulticore) selectedObject;
		Set<IConductor> conductorSet = multicore.getAllConductorsInHierarchy(true);

		//iterate through all diagram instances of the conductor
		//this is being done to collect all pinlists attached to multicore for the given diagram
		for (IConductor mcConductor : conductorSet) {
			IDiagramObjectIterator diagramObjectIterator = schemDiagram.getRepresentations(mcConductor.getUID());
			for (IDiagramObject diagramObject : diagramObjectIterator) {
				chs.cof.logical.schem.IConductor diagramCondutor = (chs.cof.logical.schem.IConductor) diagramObject;
				diagramCondutor.getPins().stream().forEach(pin -> pinOwners.add(pin.getConnectivity().getOwner()));
			}
		}
		//return all pinlist names which are connected via given multicore
		return pinOwners.stream().map(pinlist -> pinlist.getName()).collect(Collectors.joining(", "));
	}
}
