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
    "currentPackage",
    "systems",
    "baseTreeView"
], function ($, selectedPackage, designs, baseTreeView)
{
    "use strict";
    var SystemsView;
    var p = mentor.publisher;
    var baseTreeViewObject = baseTreeView(designs);
    SystemsView = baseTreeViewObject.extend({
        title: "system",
        cssClass: "Systems",

        getDesingfolders: function (design)
        {
            var folders = [];
            var folder = design.get("getFolders")();
            if (folder) {
                folders.push(folder);
            }
            return folders;
        },

        getModelIdString: function ()
        {
            return "idAttribute";
        },

        getFileLabel: function (fileObj)
        {
            function getDesignSpec(thisDesign)
            {
                var designSpecText = thisDesign.get('getRevision') ? thisDesign.get('getRevision')() : "";
                var designShortDesc = Utils.handleTranslation(thisDesign.get("subText"));
                if (designShortDesc) {
                    designSpecText = designSpecText + ":" + designShortDesc;
                }
                return designSpecText;
            }

            var fileLabel;

            if (getWindowObj().diagramAsSystemsObjectFactoryImpl) {
                fileLabel = fileObj.get("mainText") + " (" + fileObj.get("nameAttr") + ":" + getDesignSpec(fileObj) + ")";
            }
            else {
                fileLabel = fileObj.get("mainText") + ":" + getDesignSpec(fileObj);
            }

            return fileLabel;
        },

        listItemClicked: function (event)
        {
            var isSystemDiagramOpen = p.detailLayoutManager.isContentActive(
                p.contentType.SYSTEM_SVG);
            if ($(event.currentTarget).hasClass("highlight") && isSystemDiagramOpen) {
                event.stopPropagation();
                return;
            }
            this.clicked(event);
        },

        popOut: function (event)
        {
            var id = $(event.target).parent().attr('data-id'), selectedElement, content, system;
            content = this.showXref({systemId: id});
            event.stopPropagation();

            if (content.reportId) {
                mentor.publisher.popoutHandler.openPopout("popout.html#/report/" + content.systemId + "/" +
                    content.reportId + "/" +
                    selectedPackage.get("id").replace("\\", "/"));
            }
            else {
                mentor.publisher.popoutHandler.openPopout("popout.html#/system/" + content.systemId + "/" +
                    content.diagramId + "/" +
                    selectedPackage.get("id").replace("\\", "/"));
            }

        },

        getContent: function (designId)
        {
            return this.showXref({systemId: designId});
        },

        showXref: function (options)
        {
            var clickedSystem, firstDiagramOrReport, content, optionExpression, idAttribute;
            clickedSystem = designs.get(options.systemId);
            firstDiagramOrReport = clickedSystem.attributes.getFirstDiagram() || {};
            optionExpression =
                clickedSystem.attributes.getActiveConfiguration ?
                    clickedSystem.attributes.getActiveConfiguration() :
                    "";
            options.diagramId = options.diagramId || firstDiagramOrReport.id;

            if (firstDiagramOrReport.type && firstDiagramOrReport.type === "svg") {
                content = {
                    //the systemModel should be fetched from the system collection based on idAttribute and not id
                    //because in the case of configuration filtering dynamic mode->the id attribute is the one which is unique
                    id: clickedSystem.get('idAttribute'),
                    diagramId: options.diagramId,
                    reset: true,
                    type: mentor.publisher.contentType.SYSTEM_SVG,
                    optionExpression: optionExpression,
                    systemId: clickedSystem.get('id')
                };
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