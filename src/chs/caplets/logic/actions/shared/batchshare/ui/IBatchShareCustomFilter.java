/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import javafx.scene.image.Image;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Interface for batch share custom filter
 */
public interface IBatchShareCustomFilter
{

	@NotNull String getId();

	@NotNull String getTooltipString();

	@NotNull Predicate<? super IBatchShareRow> getPredicate();

	@Nullable Image getImage();
}
