/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define*/
define(['jquery', 'underscore', 'backbone', "BasicPopoverView", "PrintOptionsPopoverModel"],
    function ($, underscore, Backbone, BasicPopoverView, printModel) {
        "use strict";
        var PrintPopoverView = BasicPopoverView.extend({
            initialize : function () {
                PrintPopoverView.__super__.initialize();
                printModel.on("change:loadSkeleton", this.render, this);
                //console.log("Initializing view of print popover");
            },
            render : function () {
                if (!printModel.get('popoverModel')) {
                    return;
                }
                $('#detailPopup', this.$el).remove();
                this.setElement(this.container);
                var x = printModel.get('x');
                var y = printModel.get('y');
                var showFilter = printModel.get('showFilter') || false;
                var coordinates = this.getCoordinates(x, y);
                var template = underscore.template(this.templateHTML)({
                    title: mentor.publisher.languageTranslator.localize("PrintOptionsMenuTitle") || "Print Options",
                    show: true,
                    height: showFilter ? mentor.publisher.constants.popOverHeightWithFilter :
                            mentor.publisher.constants.popOverHeightWithoutFilter,
                    x: coordinates.x,
                    y: coordinates.y,
                    showFilter: showFilter,
                    showRenderConnectivityBtn: false
                });
                this.$el.append(template);
                return this;
            }
        });
        return new PrintPopoverView();
    });
