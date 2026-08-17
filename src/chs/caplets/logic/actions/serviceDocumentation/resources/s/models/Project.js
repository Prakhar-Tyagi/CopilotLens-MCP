/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
define(
    [
        "underscore",
        "backbone"
    ],
    function (_, Backbone) {
        "use strict"

        return Backbone.Model.extend({
            defaults: {
                name: "",
                description: ""
            },

            getThumbnailPath: function () {
                return 'data/thumbs/' + this.get('id') + '.png';
            }
        });
    }
);