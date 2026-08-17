/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caf.cafmain.actions.capture.sysml;

import chs.bridges.adaptors.tcmbse.IImportConfig;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for import configuration builder
 * This interface is used to build the import configuration
 */
public interface IImportConfigBuilder {

    /**
     * Builds the import configuration.
     *
     * @return the import configuration, or null if the configuration could not be built
     */
    @Nullable
    IImportConfig build();
}
