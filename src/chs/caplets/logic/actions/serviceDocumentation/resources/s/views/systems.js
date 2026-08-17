/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, mentor*/
define([
    'jquery',
    'underscore',
    'backbone',
    "currentPackage",
    "systems",
    "fileDisplayHandler",
    "ListView",
    "views/filteredDocFinder",
    "filehandlers/systemSVGHandler"
], function ($,
        underscore,
        Backbone,
        selectedPackage,
        designs,
        fileDisplayHandler,
        listView,
        docFinder,
        systemSVGHandler)
{
    "use strict";
    var SystemsView, p = mentor.publisher;
    SystemsView = listView(designs).extend({
        title: "system",
        cssClass: "Systems",
        listItemClicked: function (event)
        {
            var isSystemDiagramOpen = mentor.publisher.detailLayoutManager.isContentActive(
                    mentor.publisher.contentType.SYSTEM_SVG);
            if ($(event.currentTarget).hasClass("highlight") && isSystemDiagramOpen) {
                event.stopPropagation();
                return;
            }
            this.clicked(event);
        },
        // mouseout: function (event)
        // {
        //     mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.REMOVE_TOOL_TIP,
        //             event);
        //     this.hideCollapseAll(event);
        // },
        // mouseover: function (event)
        // {
        //     var clickedSystem, id;
        //     id = $(event.currentTarget).attr('data-id');
        //     clickedSystem = designs.get(id);
        //     event.detail = clickedSystem;
        //     mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.SHOW_TOOL_TIP,
        //             event);
        //     this.showCollapseAll(event);
        // },
        clicked: function (evt)
        {
            var id = $(evt.currentTarget).attr('data-id'), content;
            content = this.showXref({systemId: id});
            fileDisplayHandler.display(content);
        },

        popOut: function (event)
        {
            var id = $(event.target).parent().attr('data-id'), content, system;
            var p = mentor.publisher;
            //content = this.showXref({systemId: id, addDiagramId: true});
            content = systemSVGHandler.getFirstDiagramOrReportToOpen(id);
            //system = mentor.publisher.project.getObjectById(content.systemId);
            event.stopPropagation();

            if (content.reportId) {
                mentor.publisher.popoutHandler.openPopout("popout.html#/report/" + content.systemId + "/" +
                        content.reportId + "/" +
                        selectedPackage.get("id").replace("\\", "/"));
            }
            else {

                var url = p.popoutHandler.createURL({
                    type: p.contentType.SYSTEM_SVG,
                    systemId: content.systemId,
                    diagramId: content.diagramId,
                    projectId: selectedPackage.get("id").replace("\\", "/")
                });
                mentor.publisher.popoutHandler.openPopout(url);
            }

        },
        docFinder: "",
        getDiagramOrReportToOpen: function (clickedSystem, systemId)
        {
            var p = mentor.publisher;
            var searchedDocFinder = this.docFinder || docFinder;
            return searchedDocFinder.getFirstFilteredDoc(systemId, p.documentCategory.SYSTEMS);
        }, showXref: function (options)
        {
            var clickedSystem, firstDiagramOrReport, content, optionExpression, idAttribute;
            options = options || {};
            clickedSystem = designs.get(options.systemId);
            firstDiagramOrReport = this.getDiagramOrReportToOpen(clickedSystem, options.systemId);
            if (firstDiagramOrReport === docFinder.SHOW_FIRST_SECTION_ITEM) {
                firstDiagramOrReport = systemSVGHandler.getFirstDiagramOrReportToOpen(options.systemId)
            }
            optionExpression =
                    clickedSystem.attributes.getActiveConfiguration ?
                            clickedSystem.attributes.getActiveConfiguration() :
                            "";
            options.diagramId = firstDiagramOrReport.id;

            if (firstDiagramOrReport.type && firstDiagramOrReport.type === "svg") {
                content = {
                    //the systemModel should be fetched from the system collection based on idAttribute and not id
                    //because in the case of configuration filtering dynamic mode->the id attribute is the one which is
                    // unique
                    id: clickedSystem.get('idAttribute'),
                    // diagramId: options.diagramId,
                    reset: true,
                    type: mentor.publisher.contentType.SYSTEM_SVG,
                    optionExpression: optionExpression,
                    systemId: clickedSystem.get('id')
                };
                if (diagramAsSystemsObjectFactoryImpl || firstDiagramOrReport !== docFinder.SHOW_FIRST_SECTION_ITEM) {
                    content.diagramId = options.diagramId;
                }

            }
            else if (firstDiagramOrReport.type && firstDiagramOrReport.type ===
                    mentor.publisher.contentType.CUSTOM_VIEW) {
                content = {
                    id: clickedSystem.get('idAttribute'),
                    reset: true,
                    type: firstDiagramOrReport.type,
                    reportId: firstDiagramOrReport.id,
                    optionExpression: optionExpression,
                    systemId: clickedSystem.get('id'),
                    customDataType: firstDiagramOrReport.customDataType,
                    mainText: firstDiagramOrReport.mainText || firstDiagramOrReport.id
                }
            }
            else {
                content = {
                    id: clickedSystem.get('idAttribute'),
                    reset: true,
                    type: mentor.publisher.contentType.SYSTEM_REPORT,
                    reportId: firstDiagramOrReport.id,
                    optionExpression: optionExpression,
                    systemId: clickedSystem.get('id')
                };
            }
            return content;

        }
    });
    return new SystemsView();
});