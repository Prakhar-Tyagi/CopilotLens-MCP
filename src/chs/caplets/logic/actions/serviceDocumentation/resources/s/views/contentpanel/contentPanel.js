/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor, renderer*/
define([
        "backbone",
        "underscore",
        "jquery",
        "models/detailsPanelModel",
        "fileDisplayHandler"],
    function (Backbone,
        underscore,
        $,
        detailsPanelModel,
        fileDisplayHandler)
    {
        "use strict";
        var ContentPanelView;
        ContentPanelView = Backbone.View.extend({

            initialize: function () {

            },

            showReportIfSystemDoesHaveNoDiagrams: function (content)
            {
                var diagrams, firstItem, report = content;
                if (content.type === mentor.publisher.contentType.SYSTEM_SVG) {
                    diagrams = (content.get && content.get("getDiagrams") && content.get("getDiagrams")());
                    if (diagrams && diagrams.length === 0) {
                        firstItem = content.get("getFirstDiagram")();
                        report = {
                            id: content.get('idAttribute'),
                            reset: true,
                            type: mentor.publisher.contentType.SYSTEM_REPORT,
                            reportId: firstItem.id,
                            optionExpression: content.get("getOptionExpression")(),
                            systemId: content.get('id')
                        };
                    }

                }
                return report;

            },
            showContent: function (contentToShow) {
                var firstItemInTheContent = this.showReportIfSystemDoesHaveNoDiagrams(contentToShow);
                if (firstItemInTheContent) {
                    firstItemInTheContent.reset = true;
                    fileDisplayHandler.display(firstItemInTheContent);
                }
            }, render: function ()
            {
                this.setElement(this.container);
                var template = underscore.template(this.templateHTML)();
                this.$el.append(template);
                if (detailsPanelModel.firstItem.get && detailsPanelModel.firstItem.get("mainText")) {
                    this.showContent(detailsPanelModel.firstItem);
                }

                return this;
            }
        });

        return new ContentPanelView();
    });
