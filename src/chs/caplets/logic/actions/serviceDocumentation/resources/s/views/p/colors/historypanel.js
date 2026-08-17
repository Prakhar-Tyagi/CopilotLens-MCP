/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
define(
    ['jquery', 'underscore', 'backbone', 'recentColors', "preferences"],
    function ($, _, Backbone, recentColors, preferences)
    {
        "use strict";

        var HistoryPanel = Backbone.View.extend({
            render: function (options)
            {
                var opts;

                opts = {
                    colors: recentColors,
                    resourceKeys: {
                        title: "colorspopover.historypanel.title",
                    }
                };

                this.setElement(HistoryPanel.container);
                this.$el.append(
                    _.template(HistoryPanel.templateHTML)(opts)
                );

                return this;
            }
        });

        return HistoryPanel;
    }
)