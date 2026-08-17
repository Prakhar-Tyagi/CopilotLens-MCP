/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/* globals define, require, mentor */
define(
    ['jquery', 'underscore', 'backbone', "BasicPopoverView", 'preferences', "recentColors"],
    function ($, _, Backbone, BasicPopoverView, preferences, recentColors)
    {
        var ColorsPopover,
            resetColor;

        function handleResetColorAction()
        {
            if (resetColor !== undefined) {
                preferences.set("background-color", resetColor);
            }
        }

        mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.CLOSE_POPOVER, function (evt)
        {
            if (evt.detail && evt.detail.isCancelAction) {
                handleResetColorAction();
            }

            resetColor = undefined;
        });

        ColorsPopover = BasicPopoverView.extend({

            initialize: function ()
            {
                var that = this;

                preferences.on("change:background-color", function (model, currentValue)
                {
                    var $colorItems = that.$(".cp-color-item");
                    $colorItems.parent().removeClass("cp-current-item-container");

                    that.setColorItemCurrent(currentValue);
                });
            },

            events: {
                "click .closeBtn": "onClose",
                "click .cp-grid .cp-color-item": "onColorSelection",
            },

            onClose: function (event)
            {
                handleResetColorAction();
            },

            onColorSelection: function (event)
            {
                preferences.set("background-color", event.target.style.backgroundColor);
                mentor.publisher.stopEventFlow(event);
            },

            renderHistoryPanel: function ()
            {
                var HistoryPanel = require("views/p/colors/historypanel");
                var instance = new HistoryPanel();
                instance.render();
            },

            renderPalettePanel: function ()
            {
                var PalettePanel = require("views/p/colors/palettepanel");
                var instance = new PalettePanel();
                instance.render();
            },

            render: function (options)
            {
                var adjustedCoordinates,
                    preferredX,
                    preferredY,
                    renderedTemplate;

                preferredX = (options && options.preferredX) || 0;
                preferredY = (options && options.preferredY) || 0;

                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CLOSE_POPOVER, {});

                resetColor = preferences.get("background-color") || "";

                this.setElement(ColorsPopover.container);

                adjustedCoordinates = this.getCoordinates(preferredX, preferredY);

                renderedTemplate = _.template(ColorsPopover.templateHTML)({
                    title: mentor.publisher.languageTranslator.localize("colorspopover.title"),
                    show: true,
                    x: adjustedCoordinates.x,
                    y: adjustedCoordinates.y,
                    height: 275,
                    showFilter: false,
                    showRenderConnectivityBtn: false
                });
                this.$el.append(renderedTemplate);
                this.$("#detailPopup").css("height", "auto");
                this.$("#popover-grouped-list").css("height", "auto");
                this.$("#popupfooter").remove();

                if (recentColors.length !== 0) {
                    this.renderHistoryPanel();
                }
                this.renderPalettePanel();

                this.setColorItemCurrent(resetColor);

                this.$(".iesdPopup").css("visibility", "visible");

                return this;
            },

            setColorItemCurrent: function (color)
            {
                if (!color) {
                    return;
                }

                var $colorItems = this.$(".cp-color-item");
                $colorItems
                    .filter(function ()
                    {
                        return this.style.backgroundColor === color;
                    })
                    .parent()
                    .addClass("cp-current-item-container");
            }
        });

        return ColorsPopover;
    }
);