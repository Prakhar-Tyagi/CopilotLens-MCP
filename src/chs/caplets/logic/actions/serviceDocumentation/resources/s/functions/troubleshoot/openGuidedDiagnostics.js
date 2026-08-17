/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

define(
        ['jquery', 'models/troubleshoot/guidedDiagnosticsConfig'],
        function ($, guidedDiagnosticsConfig) {
            'use strict';

            var config = guidedDiagnosticsConfig();

            return function (activeCodes) {
                if (config.params === undefined) {
                    alert("Unable to start diagnostics session.");
                    return;
                }
                config.params.troubleshootingCodes =
                        activeCodes
                                .map(function (code) {
                                    return {
                                        codeId: code
                                    };
                                });
                return $.ajax({
                    type: "POST",
                    url: Utils.prepareFilePath("troubleshootingSession"),
                    headers: config.headers,
                    contentType: "application/json; charset=utf-8",
                    data: JSON.stringify(config.params)
                }).done(function (data) {
                    window.open(data.sessionUrl, "Troubleshooting", "noreferrer");
                });
            };
        }
);