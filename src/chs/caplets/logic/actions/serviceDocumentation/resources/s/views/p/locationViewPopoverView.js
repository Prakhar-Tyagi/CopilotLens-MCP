/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define*/

define(['jquery', 'underscore', 'backbone', "PopoverView", "LocationViewObjectPopoverModel"],
    function ($, underscore, Backbone, PopoverView, locationViewObjectPopOverModel)
    {
        "use strict";
        var LocationViewObjectPopOverView = PopoverView.extend({
            initialize: function ()
            {
                locationViewObjectPopOverModel.on("change:loadSkeleton", this.reRender, this);
            },
            getModel: function ()
            {
                return locationViewObjectPopOverModel;
            },
            events: {}
        });
        return new LocationViewObjectPopOverView();
    });
