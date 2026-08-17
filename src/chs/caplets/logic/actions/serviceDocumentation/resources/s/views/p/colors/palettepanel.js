/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

define(
    ['jquery', 'underscore', 'backbone', 'allColors', 'preferences'],
    function ($, _, Backbone, allColors, preferences)
    {
        "use strict";

        var PalettePanel = Backbone.View.extend({

            events: {
                "click .cp-default-item": "onDefaultColorSelection"
            },

            onDefaultColorSelection: function (event) {
                preferences.set("background-color", "");
                mentor.publisher.stopEventFlow(event);
            },

            render: function ()
            {
                var opts;

                opts = {
                    gridColors: allColors,
                    defaultColor: mentor.publisher.colors["svg-background-color"] || "white",
                    resourceKeys: {
                        title: "colorspopover.palettepanel.title",
                        defaultColorTitle: "colorspopover.palettepanel.defaultcolor.title"
                    }
                };


                this.setElement(PalettePanel.container);
                this.$el.append(
                    _.template(PalettePanel.templateHTML)(opts)
                );

                return this;
            }
        });

        return PalettePanel;
    }
)