/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define*/
define(['jquery', 'underscore', 'backbone', "BasicPopoverView", "RelatedDataPopoverModel"],
    function ($, underscore, Backbone, BasicPopoverView, relatedDataModel) {
        "use strict";
        var RelatedDataPopoverView = BasicPopoverView.extend({
            initialize : function () {
                RelatedDataPopoverView.__super__.initialize();
                relatedDataModel.on("change:loadSkeleton", this.render, this);
            },

            addEmptyContainer : function () {
                var containers = $("<div class='ConnectorTitle auto-list' style='height: auto;width: auto;'></div><div class='DeviceTitle auto-list' style='height: auto;width: auto;'></div><div class='SignalTitle auto-list' style='height: auto;width: auto;'></div>");
                $("#popover-grouped-list", this.$el).append(containers);
            },

            render : function () {
                if (!relatedDataModel.get('popoverModel')) {
                    return;
                }
                $('#detailPopup', this.$el).remove();
                this.setElement(this.container);
                var x = relatedDataModel.get('x');
                var y = relatedDataModel.get('y');
                var template = underscore.template(this.templateHTML)({
                    title : mentor.publisher.languageTranslator.localize("RelatedDataPopoverViewTitle") || "Related Data",
                    show : true,
                    x : x,
                    y : y,
                    showFilter : true
                });

                this.$el.append(template);
                var that = this;
                that.addEmptyContainer();
                return this;
            }
        });
        return new RelatedDataPopoverView();
    });
