/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
define(
    ["backbone", "underscore"],
    function (Backbone, _) {
        "use strict";

        var DefaultBarHandler;

        DefaultBarHandler = Backbone.Model.extend({
            onDiagramsButtonClick: function (event) {
                // default implementation
            },

            onReportsButtonClick: function (event) {
                // default implementation
            },
        });

        return DefaultBarHandler;
    }
);