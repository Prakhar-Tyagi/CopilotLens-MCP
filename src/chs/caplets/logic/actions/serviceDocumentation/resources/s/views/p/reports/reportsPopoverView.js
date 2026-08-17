/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define*/
define(['jquery', 'underscore', 'backbone', "BasicPopoverView", "ReportsPopoverModel"],
    function ($, underscore, Backbone, BasicPopoverView, reportsModel) {
        "use strict";
        var ReportsPopoverView = BasicPopoverView.extend({
            initialize : function () {
                ReportsPopoverView.__super__.initialize();
                reportsModel.on("change:loadSkeleton", this.render, this);
                //console.log("Initializing view of reports popover");
            },
            render : function () {
                if (!reportsModel.get('popoverModel')) {
                    return;
                }
                $('#detailPopup', this.$el).remove();
                this.setElement(this.container);
                var x = reportsModel.get('x');
                var y = reportsModel.get('y');
                var showFilter = reportsModel.get('showFilter') || false;
                var template = underscore.template(this.templateHTML)({
                    title: mentor.publisher.languageTranslator.localize("ReportsPopoverViewTitle") || "Select Report",
                    show: true,
                    x: x,
                    y: y,
                    height: showFilter ? mentor.publisher.constants.popOverHeightWithFilter :
                            mentor.publisher.constants.popOverHeightWithoutFilter,
                    showFilter: showFilter,
                    showRenderConnectivityBtn: false
                });
                this.$el.append(template);
                return this;
            }
        });
        return new ReportsPopoverView();
    });
