/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
define(
    [
        "jquery",
        "underscore",
        "backbone",
        "preferences"
    ],
    function ($, _, Backbone, preferences) {
        "use strict";

        var SystemScopePanel;

        SystemScopePanel = Backbone.View.extend({

            events: {
                "click .system-scope-panel .titlebar": "onTitlebarClick",
                "click .system-scope-panel .listItem": "onItemClick"
            },

            onTitlebarClick: function (event) {
                $(event.currentTarget).parent().find(".listItem").each(function () { $(this).toggle(); });

                event.stopPropagation();
            },

            onItemClick: function (event) {
                var isPreference,
                    scope;

                isPreference = $(event.currentTarget).hasClass("preferred");
                if (!isPreference) {
                    scope = $(event.currentTarget).data("scope");
                    if (scope === "designs") {
                        preferences.set("systemScope", "designs");
                        window.location.href = window.location.href.replace(/index1(-touch)?(\.html)/g, "index$1$2");
                    }
                    else {
                        preferences.set("systemScope", "diagrams");
                        window.location.href = window.location.href.replace(/index(-touch)?(\.html)/g, "index1$1$2");
                    }
                }

                event.preventDefault();
            },

            render: function () {
                var panel,
                    renderedPanel,
                    scope,
                    translator;

                panel = this;

                translator = mentor.publisher.languageTranslator;

                renderedPanel = _.template(SystemScopePanel.templateHTML)({
                    title: translator.localize("SystemScopePanelTitle"),
                    count: "2",
                    designsItemTitle: translator.localize("SystemScopeDesignsItemTitle"),
                    diagramsItemTitle: translator.localize("SystemScopeDiagramsItemTitle")
                });
                panel.$el.append(renderedPanel);

                panel.$(".system-scope-panel .listItem").each(function () {
                    scope = $(this).data("scope");
                    if (scope === "designs") {
                        if (window.location.href.match(/index(-touch)?(\.html)/g)) {
                            $(this).addClass("preferred");
                        }
                    }
                    else {
                        if (window.location.href.match(/index1(-touch)?(\.html)/g)) {
                            $(this).addClass("preferred");
                        }
                    }
                });

                return this;
            }

        });

        return SystemScopePanel;
    }
)