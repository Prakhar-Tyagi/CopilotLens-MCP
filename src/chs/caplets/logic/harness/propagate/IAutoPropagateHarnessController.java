/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.harness.propagate;

import chs.common.IUID;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public interface IAutoPropagateHarnessController
{

	//should this be i18n?
	String propagate_harness_tab = ResourceMgr.getString(HarnessPropagateTableWindow.class, "HarnessPropagateTableWindow.Tab.Name");

	void clearHarnessPropagateWindow();

	@Nullable PropagationInfo getPropagationInfo();

	void loadObjects(@NotNull IUID designUid, @NotNull HarnessUpdateStatusMessageTableModel tableModel, boolean propagateAll);
}
