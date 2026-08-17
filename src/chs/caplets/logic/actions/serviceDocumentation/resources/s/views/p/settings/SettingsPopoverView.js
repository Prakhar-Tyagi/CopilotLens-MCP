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
        "BasicPopoverView"
    ],
    function ($, _, Backbone, BasicPopoverView) {
        "use strict";

        var SettingsPopoverView = BasicPopoverView.extend({

            render : function (options) {
                options = options || {};
                options.preferredX = options.preferredX || 0;
                options.preferredY = options.preferredY || 0;

                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CLOSE_POPOVER, {});

                this.setElement(SettingsPopoverView.container);

                var adjustedCoordinates = this.getCoordinates(options.preferredX, options.preferredY);

                var renderedTemplate = _.template(SettingsPopoverView.templateHTML)({
                    title: mentor.publisher.languageTranslator.localize("SettingsPopoverViewTitle"),
                    show: true,
                    x: adjustedCoordinates.x,
                    y: adjustedCoordinates.y,
                    height: 198,
                    showFilter: false,
                    showRenderConnectivityBtn: false
                });
                this.$el.append(renderedTemplate);

                this.addSystemScopePanel();
                this.addLanguagesPanel();

                this.$(".iesdPopup").css("visibility", "visible");

                return this;
            },

            addSystemScopePanel: function () {
                var panel,
                    SystemScopePanel;

                SystemScopePanel = require("SystemScopePanel");

                panel = new SystemScopePanel({
                    el: this.$("#popover-grouped-list")
                });
                panel.render();
            },

            addLanguagesPanel: function () {
                var panel,
                    LanguagesPanel;

                LanguagesPanel = require("LanguagesPanel");

                panel = new LanguagesPanel({
                    el: this.$("#popover-grouped-list")
                });
                panel.render();
            }

        });

        return SettingsPopoverView;
    });
