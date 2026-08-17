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
        "BasicPopoverView",
    ],
    function ($, _, Backbone, BasicPopoverView)
    {
        "use strict";

        var DocumentsPopover;

        DocumentsPopover = BasicPopoverView.extend({

            render: function (options)
            {
                var adjustedCoordinates,
                    panel,
                    renderedTemplate,
                    DocumentPanelObj,
                    DocumentPanel, popoverTitle = options.popoverTitle || "DiagramsPopoverViewTitle";

                options = options || {};
                options.preferredX = options.preferredX || 0;
                options.preferredY = options.preferredY || 0;

                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CLOSE_POPOVER, {});

                this.setElement(DocumentsPopover.container);

                adjustedCoordinates = this.getCoordinates(options.preferredX, options.preferredY);

                renderedTemplate = _.template(DocumentsPopover.templateHTML)({
                    title: mentor.publisher.languageTranslator.localize(popoverTitle),
                    show: true,
                    x: adjustedCoordinates.x,
                    y: adjustedCoordinates.y,
                    height: 198,
                    showFilter: false,
                    showRenderConnectivityBtn: false
                });
                this.$el.append(renderedTemplate);

                DocumentPanelObj = require("views/p/hld/HarnessLayoutDiagramsPanel");
                DocumentPanel = DocumentPanelObj.extend(options);
                panel = new DocumentPanel({el: this.$("#popover-grouped-list")});
                panel.render();

                this.$(".iesdPopup").css("visibility", "visible");

                return this;
            }

        });

        return DocumentsPopover;
    });