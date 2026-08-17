/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global mentor, define*/
define(
        [
            "jquery",
            "underscore",
            "backbone",
            "currentPackage",
            "models/selectedSystem",
            "harnessLayouts",
            "fileDisplayHandler"
        ],
        function ($, _, Backbone, currentPackage, selectedSystem, harnessLayouts, fileDisplayHandler)
        {
            "use strict";

            var DocumentsPanel;

            DocumentsPanel = Backbone.View.extend({

                events: {
                    "click .titlebar": "onTitlebarClick",
                    "click .listItem": "onItemClick",
                    "click .popUp": "onPopoutClick"
                },

                onTitlebarClick: function (event)
                {
                    $(event.currentTarget).parent().find(".listItem").each(function ()
                    {
                        $(this).toggle();
                    });

                    event.stopPropagation();
                },

                getDocumentGroup: function ()
                {
                    return "diagrams";
                }, getDocumentType: function ()
                {
                    return mentor.publisher.contentType.HARNESS_LAYOUT_DIAGRAM;
                }, getDocumentSetId: function ()
                {
                    return selectedSystem.get("harnessLayoutId");
                }, getDocumentSetById: function (dcoumentSetId)
                {
                    return harnessLayouts.get(dcoumentSetId);
                }, closeExistingPanel: function ()
                {
                    mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CLOSE_POPOVER, {});
                }, onItemClick: function (event)
                {
                    var documentSet,
                            dcoumentSetId,
                            id,
                            documentGroup,
                            documentType,
                            documentName = this.getDocumentName();

                    dcoumentSetId = this.getDocumentSetId();
                    documentSet = this.getDocumentSetById(dcoumentSetId);

                    id = $(event.currentTarget).attr('data-id');

                    this.closeExistingPanel();
                    documentGroup = this.getDocumentGroup();
                    documentType = this.getDocumentType();

                    fileDisplayHandler.display({
                        layoutId: dcoumentSetId,
                        id: id,
                        group: documentGroup,
                        type: documentType,
                        reset: false,
                        documentName: documentName
                    });

                    event.stopPropagation();
                },

                contructDocumentURL: function (documentSetId, documentId, projectId, type)
                {
                    return "popout.html#/" + type.toLowerCase() + "/" + documentSetId + "/" +
                            documentId + "/" + projectId;
                }, onPopoutClick: function (event)
                {
                    var documentId,
                            documentSetId,
                            projectId,
                            type;

                    documentSetId = this.getDocumentSetId();
                    type = this.getDocumentType();
                    documentId = $(event.currentTarget).parent().attr('data-id');
                    projectId = currentPackage.get("id").replace("\\", "/");

                    mentor.publisher.popoutHandler.openPopout(
                            this.contructDocumentURL(documentSetId,
                                    documentId,
                                    projectId,
                                    type)
                    );

                    event.stopPropagation();
                },

                getDocumentName: function ()
                {
                    return "harnessLayoutDiagram";
                }, getActiveDocumentForDocumentSet: function ()
                {
                    return selectedSystem.get(this.getDocumentName());
                }, getDocumentCSSClass: function ()
                {
                    return "harness-layout-diagrams";
                }, /*getDataToRender: function (harnessDiagramPanel)
                 {

                 var documentSetId = harnessDiagramPanel.getDocumentSetId();
                 var documentSet = harnessDiagramPanel.getDocumentSetById(documentSetId);
                 var documents = documentSet.getDocumentsInGroupTitled(harnessDiagramPanel.getDocumentGroup());

                 var options = {};
                 options.className = harnessDiagramPanel.getDocumentCSSClass();
                 options.expand = true;
                 options.items = documents.map(function (document)
                 {
                 var clone;

                 clone = document.clone();
                 clone.isActive = "";

                 return clone;
                 });
                 options.showPopup = true;
                 options.showTitle = false;
                 options.title = "";
                 options.totalItems = documents;
                 return options;
                 },*/ render: function ()
                {
                    var options,
                            panel,
                            renderedPanel;

                    panel = this;
                    options = harnessLayouts.getDataToRender(this);
                    options.className = this.getDocumentCSSClass();
                    renderedPanel = _.template(DocumentsPanel.templateHTML)(options);
                    panel.$el.append(renderedPanel);
                    return this;
                }

            });

            return DocumentsPanel;
        }
);