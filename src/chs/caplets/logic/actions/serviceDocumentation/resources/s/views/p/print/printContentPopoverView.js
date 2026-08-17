/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define*/
define([ 'jquery', 'underscore', 'backbone', "BasicPopoverView", "PrintContentPopoverModel",
    "SelectedPrintContentModel"],
    function ($, underscore, Backbone, BasicPopoverView, prinContentModel, selectedPrintContentModel) {
        "use strict";
        var PrintContentPopoverView = BasicPopoverView.extend({

            initialize : function () {
                PrintContentPopoverView.__super__.initialize();
                prinContentModel.on("change:loadSkeleton", this.render, this);
                selectedPrintContentModel.reset();
                //console.log("Initializing view of print popover");
            },
            events : {
                "click #detailPopup>#popupheader>#toolbar_printbtn" : "printSelectedContents",
                "click #detailPopup>#popupheader>#toolbar_closebtn" : "closePopup"
            },
            printSelectedContents : function () {
                var contents = selectedPrintContentModel.contents;
                mentor.publisher.printer.initiatePrinting(contents);
            },
            closePopup : function () {
                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CLOSE_POPOVER, {});
            },
            render : function () {
                if (!prinContentModel.get('popoverModel')) {
                    return;
                }
                //reset the selections before the print selections view is rendered
                selectedPrintContentModel.reset();
                $('#detailPopup', this.$el).remove();
                this.setElement(this.container);
                var x = prinContentModel.get('x');
                var y = prinContentModel.get('y');
                var coordinates = this.getCoordinates(x, y);
                var showFilter = prinContentModel.get('showFilter') || false;
                var template = underscore.template(this.templateHTML)({
                    title: mentor.publisher.languageTranslator.localize("printMsg") || "Print Items",
                    show: true,
                    height: showFilter ? mentor.publisher.constants.popOverHeightWithFilter :
                            mentor.publisher.constants.popOverHeightWithoutFilter,
                    x: coordinates.x,
                    y: coordinates.y,
                    showFilter: showFilter
                });
                this.$el.append(template);
                return this;
            }
        });
        return new PrintContentPopoverView();
    });
