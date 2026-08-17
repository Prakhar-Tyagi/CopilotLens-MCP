/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define*/
define(["underscore", "PopoverItemView", "ConfigurationsCollection", "ConfigurationsModel"],
    function (_, PopoverItemView, configurationsCollection, model) {
        "use strict";
        var ConfigurationsItem = PopoverItemView.extend({
            expanded: true,

            /*var contentArray = [];*/
            getData : function () {
                return configurationsCollection;
            },
            getTotalItems : function() {
                return model.getConfigurationsCount();
            },
            getTitle : function () {
                return mentor.publisher.languageTranslator.localize("ConfigurationsTitle") || "Configurations";
            },
            getClassName : function () {
                return "configurations";
            },
            events : {
                "click .listItem>.mainText" : "applyConfigurationFilter",
                "mouseover .listItem" : "showToolTip",
                "mouseout .listItem" : "removeToolTip",
                "click .titlebar" : "toggleSection"
            },

            removeToolTip : function (event) {
                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.REMOVE_TOOL_TIP,
                    event);
            },
            showToolTip : function (event) {
                var type = $(event.currentTarget).attr('data-type'),
                    config = _.escape($(event.currentTarget).attr('data-name')),
                    options = _.escape($(event.currentTarget).attr('data-value')),
                    tooltip, tts = [];

                if (type === 'configuration' || type === 'option') {
                    event.detail = {};
                    tooltip = {
                        getName : function () {
                            return config;
                        },
                        getValue : function () {
                            return options;
                        }
                    };
                    tts.push(tooltip);
                    event.detail.getToolTips = function () {
                        return tts;
                    };
                    event.detail.showToolTipAlways = true;
                    mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.SHOW_TOOL_TIP,
                        event);
                }
            },

            applyConfigurationFilter : function (event) {
                model.updateModel(event, "configuration");
            },

            isExpanded: function ()
            {
                return this.expanded;
            },

            toggleSection: function (event)
            {
                event.stopPropagation();
                this.expanded = !this.expanded;
                this.render();
            },

            shouldRenderForEmptyCollection: function()
            {
                return true;
            },

            setRenderedTemplateInElement: function (renderedTemplate)
            {
                this.$el.html(renderedTemplate);
            }
        });
        return new ConfigurationsItem();
    });
