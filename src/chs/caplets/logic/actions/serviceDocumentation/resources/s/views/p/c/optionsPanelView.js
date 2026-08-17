/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define*/
define(["PopoverItemView", "OptionsCollection", "ConfigurationsModel"],
    function (PopoverItemView, optionsCollection, model)
    {
        "use strict";

        var translator = mentor.publisher.languageTranslator;
        var OptionsItem = PopoverItemView.extend({
            expanded: true,

            /*var contentArray = [];*/
            getData: function ()
            {
                return optionsCollection;
            },
            getTitle: function ()
            {
                return translator.localize("OptionsTitle") || "Options";
            },
            getClassName: function ()
            {
                return "options";
            },
            events: {
                "mouseover .listItem": "showToolTip",
                "mouseout .listItem": "removeToolTip",
                "click .titlebar": "toggleSection"
            },

            removeToolTip: function (event)
            {
                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.REMOVE_TOOL_TIP, event);
            },
            showToolTip: function (event)
            {
                var targetElement = $(event.currentTarget);
                var type = targetElement.attr('data-type'), config = targetElement.attr('data-name'),
                    options = targetElement.attr('data-value'), ttNameFn, ttValueFn, tts = [];

                var isDisabled = targetElement.find('input[type="checkbox"]').prop('disabled');
                var isIncluded = targetElement.find('input[type="checkbox"]').prop('checked');

                function getDisplayName() {
                    return config;
                }

                function getOptionValue() {
                    return options;
                }

                if (type === 'configuration' || type === 'option') {
                    event.detail = {};
                    if (isDisabled) {
                        if (isIncluded) {
                            ttNameFn = function () {
                                return translator.localize("option.included.message") ||
                                    "Option must be included with the current selection";
                            };
                        } else {
                            ttNameFn = function () {
                                return translator.localize("option.excluded.message") ||
                                    "Option is incompatible with the current selection";
                            };
                        }
                        ttValueFn = function () {};
                        event.detail.showToolTipAlways = true;
                    } else {
                        ttNameFn =  getOptionValue.bind(this);
                        ttValueFn = getDisplayName.bind(this);
                    }

                    tts.push({
                        getName: ttNameFn,
                        getValue: ttValueFn
                    });
                    event.detail.getToolTips = function () {
                        return tts;
                    };
                    mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.SHOW_TOOL_TIP, event);
                }
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

            setRenderedTemplateInElement: function (renderedTemplate)
            {
                this.$el.html(renderedTemplate);
            }
        });
        return new OptionsItem();
    });
