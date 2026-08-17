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
import chs.cof.logical.IDiagramSyncAfterConnectivityRefresh;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IUID;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.function.Consumer;

public interface IDiagramSyncAfterLayoutSync extends IDiagramSyncAfterConnectivityRefresh
{

	void prepare(@NotNull ISchemDiagram diagram);

	void run(@NotNull ISchemDiagram diagram, @NotNull Set<IUID> unusedUIDs, @NotNull Consumer<IDiagramObject> callback);
}
