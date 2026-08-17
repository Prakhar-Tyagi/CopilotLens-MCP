/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global define, mentor, _, $*/
define(['views/harnessLayoutsView',
            "illustrator/views/layoutManager",
            "fileDisplayHandler"],
        function (harnessLayoutsView,
                layoutManager,
                fileDisplayHandler)
        {
            "use strict";
            var p = mentor.publisher;
            var revisionType = {
                1: p.contentType.OLD_DESIGN_REVISION,
                2: p.contentType.NEW_DESIGN_REVISION
            };
            harnessLayoutsView.title = "Designs";

            harnessLayoutsView.displayDesign = function (harnessLayout, index)
            {
                var content = harnessLayout.getContent();
                var designType = revisionType[index];
                content.reset = false;
                content.type = designType;
                fileDisplayHandler.display(content);
            };

            harnessLayoutsView.clicked = function (event)
            {
                var clickedEle = $(event.currentTarget);
                var index = $(clickedEle).index();
                var content = {}, harnessLayout;

                harnessLayout = this.getLayoutFromEvent(event);
                if (harnessLayout) {
                    this.displayDesign(harnessLayout, index);
                }

            };

            harnessLayoutsView.getType = function (layoutToOpen, event)
            {
                var clickedEle = $(event.currentTarget);
                var index = $(clickedEle).parent().index();
                return revisionType[index];
            };

            harnessLayoutsView.render = function()
            {
                if (this.templateHTML) {
                    var template = this.renderItems(
                        {
                            header: true,
                            expand: this.expanded
                        }
                    );
                    this.$el.append(template);
                    this.amItTheFirstPanel();
                }
                return this;
            };
            return harnessLayoutsView;
        });