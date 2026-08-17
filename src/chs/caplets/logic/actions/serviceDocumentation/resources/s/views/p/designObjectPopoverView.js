/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define*/
define(['jquery', 'underscore', 'backbone', "PopoverView", "DesignObjectPopoverModel"],
    function ($, underscore, Backbone, PopoverView, designObjectPopOverModel)
    {
        "use strict";
        var p = mentor.publisher, DesignObjectPopoverView = PopoverView.extend({
            doNotLoadOnStart: false,
            initialize: function ()
            {
                designObjectPopOverModel.on("change:loadSkeleton", this.reRender, this);
            },
            getModel: function ()
            {
                return designObjectPopOverModel;
            }

        });
        return new DesignObjectPopoverView();
    });
