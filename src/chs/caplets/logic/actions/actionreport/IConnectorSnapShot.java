/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.actionreport;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * connector snapshot
 */
public interface IConnectorSnapShot extends ICachedObject
{
  @Nullable String getBackShellUID();

  void addBackShellUID(@NotNull String uid);

  @NotNull Collection<String> getPinUIDs();

  void addConnectorPinUID(@NotNull String uid);
}
