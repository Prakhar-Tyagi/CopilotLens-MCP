/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
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
    function ($, _, Backbone, BasicPopoverView) {
        "use strict";

        var ProjectsPopoverView;

        ProjectsPopoverView = BasicPopoverView.extend({

            render: function (options)
            {
                var adjustedCoordinates,
                    panel,
                    renderedTemplate,
                    ProjectsPanel;

                options = options || {};
                options.preferredX = options.preferredX || 0;
                options.preferredY = options.preferredY || 0;

                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CLOSE_POPOVER, {});

                this.setElement(ProjectsPopoverView.container);

                adjustedCoordinates = this.getCoordinates(options.preferredX, options.preferredY);

                renderedTemplate = _.template(ProjectsPopoverView.templateHTML)({
                    title: mentor.publisher.languageTranslator.localize("ProjectsPopoverViewTitle"),
                    show: true,
                    x: adjustedCoordinates.x,
                    y: adjustedCoordinates.y,
                    height: 198,
                    showFilter: false,
                    showRenderConnectivityBtn: false
                });
                this.$el.append(renderedTemplate);

                ProjectsPanel = require("ProjectsPanel");
                panel = new ProjectsPanel({
                    el: this.$("#popover-grouped-list")
                });
                panel.render();

                this.$(".iesdPopup").css("visibility", "visible");

                return this;
            }

        });

        return ProjectsPopoverView;
    });
