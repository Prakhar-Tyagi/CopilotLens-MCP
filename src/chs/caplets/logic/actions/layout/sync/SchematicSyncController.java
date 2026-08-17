/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout.sync;

import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IUID;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class SchematicSyncController
{

	@NotNull private final ILogicDesign mLogicDesign;
	@NotNull private final Map<ISchemDiagram, IDiagramSyncAfterLayoutSync> mDiagramSync = new HashMap<>();

	public SchematicSyncController(@NotNull ILogicDesign design)
	{
		mLogicDesign = design;
	}

	public void prepare()
	{
		if (mLogicDesign.getConnectivity() == null) {
			return;
		}

		for (ISchemDiagram diagram : mLogicDesign.getDiagrams()) {
			diagram.loadToMemory();
			final IDiagramSyncAfterLayoutSync synchronizer = new DiagramSyncAfterLayoutSync(mLogicDesign);
			synchronizer.prepare(diagram);
			mDiagramSync.put(diagram, synchronizer);
		}
	}

	public void run(@NotNull Set<IUID> unusedUIDs, @NotNull Consumer<IDiagramObject> callback)
	{
		for (ISchemDiagram schemDiagram : mDiagramSync.keySet()) {
			schemDiagram.loadToMemory();
			final IDiagramSyncAfterLayoutSync synchronizer = mDiagramSync.get(schemDiagram);
			if (synchronizer != null) {
				synchronizer.run(schemDiagram, unusedUIDs, callback);
			}
		}
	}
}
