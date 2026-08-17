/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define*/
define("BaseConfigurationsBuilderView", ['jquery', 'underscore', 'backbone', "currentPackage", "BasicPopoverView", "ConfigurationsModel"],
    function ($, underscore, Backbone, currentPackage, BasicPopoverView) {
        "use strict";
        var BasicConfigurationsBuilderView = BasicPopoverView.extend({
            initialize: function () {
                this.getModel().on("change:loadSkeleton", this.reRender, this);
                var pubSub = mentor.publisher.eventDispatcher;
                pubSub.attachEventListener(mentor.publisher.events.CLOSE_CONFIG_POPOVER, function (evt) {
                    this.cancelAndClose();
                }.bind(this));
                currentPackage.on("change:language", this.renderOnLanguageChange, this);
            },
            /*events : {
             "click #config-popover-groupedlist .titlebar" : "toggleSection"
             },*/

            getModel: function () {
            },

            renderOnLanguageChange: function () {
                if ($('#configPopup', this.$el).length > 0) {
                    $("#configPopup").remove();
                    $("#configButton").trigger("click");
                }
            },

            toggleOptionCheckBox: function (event) {
                var target = $(event.currentTarget);
                var children = target.children("input[type='checkbox']");
                var isChecked = children[0].checked;
                var isDisabled = children[0].disabled;
                if (!isDisabled) {
                    if (isChecked) {
                        children[0].checked = false;
                    }
                    else {
                        children[0].checked = true;
                    }
                    this.updateConfigurationPanels(event);
                }
            },

            closePopover: function (evt) {
                $(this.getPopoverDiv(), "#toolbar_closebtn").off();
                this.getModel().close();
                this.lastEnteredConfigName = null;
                this.undelegateEvents();
                $("#configPopup").remove();
                evt && evt.stopPropagation();
            },

            getPopoverDiv: function () {
                return $("#configPopup");
            },

            reRender: function () {
                $('#configPopup', this.$el).remove();
                this.render();
            },

            getHeight: function () {
                return this.$el.find("#configPopup").outerHeight(); // including border
            },

            getCoordinates: function (x, y) {
                var padding = 20, popoverWidth = this.getWidth(), popoverHeight = this.getHeight();

                x = x + padding;

                if (x + popoverWidth > $('body').width() - padding) {
                    //x = $('body').width() - (popoverWidth + padding);
                    //x = x - (popoverWidth + padding);
                    x = x - popoverWidth - popoverWidth - padding - padding;
                }

                y = y + padding;
                if (y < padding) {
                    y = padding;
                }
                if (y + popoverHeight > $('body').height() - padding) {
                    y = $('body').height() - (popoverHeight + padding);
                }
                return {x: x, y: y};
            },

            render: function () {
                this.setElement(this.container);
                if (!this.getModel()) {
                    return;
                }
                var x = this.getModel().get("x");
                var y = this.getModel().get("y");
                var show = this.getModel().get("show");
                var showFilter = false;
                var coordinates = this.getCoordinates(x, y);
                var template = underscore.template(this.templateHTML)({
                    title: mentor.publisher.languageTranslator.localize('ConfigurationFilterTitle') ||
                            "Configuration Filter",
                    show: show,
                    x: coordinates.x,
                    y: coordinates.y,
                    height: showFilter ? mentor.publisher.constants.popOverHeightWithFilter :
                            mentor.publisher.constants.popOverHeightWithoutFilter,
                    showFilter: showFilter
                });
                this.$el.append(template);
                return this;
            }
        });
        return BasicConfigurationsBuilderView;
    });
