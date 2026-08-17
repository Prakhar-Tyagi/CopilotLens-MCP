/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
define(
        ["backbone", "models/selectedSystem", "preferences"],
        function (Backbone, selectedSystem, preferences) {
            "use strict";

            var LayoutButtons, p = mentor.publisher;

            LayoutButtons = Backbone.View.extend({
                el: "<div></div>",
                model: new Backbone.Model(),

                events: {
                "click .navigation-panel-toggle": "toggleNavigationPanel",
                "click .diagrams-button": "onDiagramsButtonClick",
                "click .reports-button": "onReportsButtonClick",
            },

            toggleNavigationPanel: function (event)
            {
                var navigationPanelView = require("views/navigationPanelView");
                navigationPanelView.toggleVisibility();

            },
            onDiagramsButtonClick: function (event)
            {
                this.options.handler.onDiagramsButtonClick(event);
                event.stopPropagation();
            },

            onReportsButtonClick: function (event)
            {
                this.options.handler.onReportsButtonClick(event);
                event.stopPropagation();
            },

            initialize: function ()
            {
                preferences.on("change:language", this.localizeButtons, this);
            },

            localizeButtons: function ()
            {
                var translator;

                translator = mentor.publisher.languageTranslator;

                this.$(".diagrams-button").html(translator.localize("DiagramsButtonTitle"));
                this.$(".diagrams-button").prop("title", translator.localize("DiagramsButtonTooltip"));
                this.$(".reports-button").html(translator.localize("ReportsButtonTitle"));
                this.$(".reports-button").prop("title", translator.localize("ReportsButtonToolTip"));
                this.$(".navigation-panel-toggle").prop("title", translator.localize("NavigationPanelToggleToolTip"));
                this.$(".component-label").html(Utils.translate(this.options.title || ""));
            },

            disableDocumentSets: function (selectors)
            {
                var selector;
                for (selector in selectors) {
                    this.$(selectors[selector]).hide();
                }
            },
            enableDocumentSets: function (selectors)
            {
                var selector;
                for (selector in selectors) {
                    this.$(selectors[selector]).show();
                }
            },

            render: function (options)
            {
                var template;
                if (!options) {
                    return;
                }

                this.options = options;

                this.$el.append(_.template(LayoutButtons.templateHTML)());
                this.localizeButtons();
                this.delegateEvents();

                return this;
            }
        });

        return LayoutButtons;
    }
);