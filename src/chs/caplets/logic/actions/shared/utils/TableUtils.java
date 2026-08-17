/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.utils;

import chs.utilities.AlphaNumComparator;
import javafx.scene.control.TableColumnBase;
import org.jetbrains.annotations.NotNull;

/**
 * Provides utility methods for UI Tables
 */
public class TableUtils
{

    private TableUtils()
    {
    }

    public static void setAlphaNumComparator(@NotNull TableColumnBase<?, ?> column)
    {
        column.setComparator(new AlphaNumComparator<>(true, true));
    }
}