/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define*/
/*define(['router', 'jquery', 'underscore', 'backbone', "BasicPopoverView", "ConfigurationsModel"],
    function (Router, $, underscore, Backbone, BasicPopoverView, popOverModel) {
        "use strict";
        //todo duplicate of configurationsView?
        var XRefBuilderView = BasicPopoverView.extend({
            initialize : function () {
                XRefBuilderView.__super__.initialize();
                popOverModel.on("change:loadSkeleton", this.reRender, this);
                //console.log("Initializing view of XRef builder popover view");
            },
            events : {
                "click #config-popover-groupedlist .titlebar" : "toggleSection",
                "click #toolbar_closebtn" : "closePopover"
            },

            closePopover : function () {
                //popOverModel.close();
                this.removeView();
            },

            getPopoverDiv : function () {
                return $("#configPopup");
            },

            reRender : function () {
                $('#configPopup', this.$el).remove();
                this.render();
            },

            render : function () {
                this.setElement(this.container);
                if (!popOverModel) {
                    return;
                }
                var x = popOverModel.get("x");
                var y = popOverModel.get("y");
                var show = popOverModel.get("show");
                var showFilter = false;
                var coordinates = this.getCoordinates(x, y);
                var template = underscore.template(this.templateHTML, {
                    title : "Configuration Filter",
                    show : show,
                    x : coordinates.x,
                    y : coordinates.y,
                    height : showFilter ? mentor.publisher.constants.popOverHeightWithFilter : mentor.publisher.constants.popOverHeightWithoutFilter,
                    showFilter : showFilter
                });
                this.$el.append(template);
                return this;
            }
        });
        return new XRefBuilderView();
    });*/

define(["underscore", "BaseConfigurationsBuilderView", "XRefBuilderModel"],
    function (_, BaseConfigurationsBuilderView, popoverModel) {
        "use strict";
        var XrefBuilderView = BaseConfigurationsBuilderView.extend({

            events: {
                "click .listPanel.options .listItem": "toggleOptionCheckBox",
                "click .listItem>.configPanelCheckBox": "updateConfigurationPanels",
                "click #toolbar_closebtn": "closePopover",
                "click #configPopup": "popoverClickHandle",
            },

            getModel : function () {
                return popoverModel;
            },

            updateConfigurationPanels: function (event) {
                popoverModel.updateModel(event, "option");
                event && event.stopPropagation();
            },

            cancelAndClose: function (event) {
                popoverModel.rollbackToLastAppliedOptions();
                this.lastEnteredConfigName = null;
                this.undelegateEvents();
                $("#configPopup").remove();
            },

            /*closePopover : function () {
                this.removeView();
            }*/
        }), xrefBuilderView;
        xrefBuilderView = new XrefBuilderView();
        return _.extend(xrefBuilderView, Backbone.Events);
    });
