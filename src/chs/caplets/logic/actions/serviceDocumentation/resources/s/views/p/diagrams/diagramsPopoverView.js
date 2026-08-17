/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define*/
define(['jquery', 'underscore', 'backbone', "BasicPopoverView", "DiagramsPopoverModel", "fileDisplayHandler"],
    function ($, underscore, Backbone, BasicPopoverView, diagramsModel, fileDisplayHandler)
    {
        "use strict";
        var DiagramsPopoverView = BasicPopoverView.extend({

            initialize: function ()
            {
                DiagramsPopoverView.__super__.initialize();
                diagramsModel.on("change:loadSkeleton", this.render, this);
                //console.log("Initializing view of diagrams popover");
            },
            render: function ()
            {
                if (!diagramsModel.get('popoverModel')) {
                    return;
                }
                $('#detailPopup', this.$el).remove();
                this.setElement(this.container);
                var x = diagramsModel.get('x');
                var y = diagramsModel.get('y');
                var showFilter = diagramsModel.get('showFilter') || false;
                var template = underscore.template(this.templateHTML)({
                    title: mentor.publisher.languageTranslator.localize("DiagramsPopoverViewTitle") ||
                            "Select Diagram",
                    show: true,
                    x: x,
                    y: y,
                    height: showFilter ? mentor.publisher.constants.popOverHeightWithFilter :
                            mentor.publisher.constants.popOverHeightWithoutFilter,
                    showFilter: showFilter,
                    showRenderConnectivityBtn: false
                });
                this.$el.append(template);
                return this;
            }
        });
        return new DiagramsPopoverView();
    });
