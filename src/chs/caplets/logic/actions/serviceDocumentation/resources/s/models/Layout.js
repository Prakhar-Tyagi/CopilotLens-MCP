/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global define, Utils, mentor*/
define(
        ["backbone", "underscore", "views/filteredDocFinder"],
        function (Backbone, _, docFinder)
        {
            "use strict";

            var HarnessLayout = Backbone.Model.extend({
                getToolTips: function ()
                {
                    if (!this.has("tooltips")) {
                        return [];
                    }

                    var tooltips = this.get("tooltips");
                    return _.map(tooltips, function (tooltips)
                    {
                        return {
                            getName: function ()
                            {
                                return Utils.translate(tooltips.name);
                            },
                            getValue: function ()
                            {
                                return Utils.translate(tooltips.value);
                            }
                        };
                    });
                },
                getTooltipByName: function (name)
                {
                    if (this.has("tooltips")) {
                        var revision = _.filter(this.get("tooltips"), function (tooltipEle)
                        {
                            return this.getTooltip(tooltipEle, name);
                        }, this);
                        if (revision && revision.length > 0) {
                            return Utils.translate(revision[0].value);
                        }
                        return "";
                    }
                },

                getNameWithPartNumberAndRevision: function ()
                {
                    return Utils.translate(this.get("mainText")) + " : " +
                            Utils.translate(this.get("subText")) + " : " +
                            Utils.translate(this.getTooltipByName("Revision"));
                },

                getTooltip: function (tooltipEle, name)
                {
                    if (tooltipEle.name === name) {
                        return true;
                    }
                    return false;
                },

                getDocumentsInGroupTitled: function (title)
                {
                    var documentGroup = this.getDocumentGroupTitled(title),
                            documents = new Backbone.Collection(),
                            layout = this;

                    if (documentGroup && documentGroup.documents) {
                        documents.add(documentGroup.documents);
                    }

                    documents.each(function (document)
                    {
                        document.set("title", layout.get("mainText") + ", " + document.get('mainText'));
                    });

                    return documents;
                },

                getDiagramOrReportToOpen: function (layoutId)
                {
                    var p = mentor.publisher;
                    var items = docFinder.getFirstFilteredDoc(
                            layoutId,
                            p.documentCategory.HARNESS_LAYOUT_DIAGRAM,
                            p.documentCategory.DIAGRAMS,
                            "group"
                    );
                    if (items === docFinder.SHOW_FIRST_SECTION_ITEM) {
                        items = this.getDefaultDocument();
                    }
                    return items;

                },
                getContent: function ()
                {
                    var layoutDiagramToDisplay = this.getDiagramOrReportToOpen(this.get("id"));
                    var content = {
                        listItemId: this.get("id"),
                        layoutId: this.get("id"),
                        group: layoutDiagramToDisplay.group,
                        type: this.getContentTypeForDocument(layoutDiagramToDisplay),
                        reset: true
                    };
                    if (layoutDiagramToDisplay.mainText) {
                        content.mainText = layoutDiagramToDisplay.mainText;
                    }
                    else {
                        content.id = layoutDiagramToDisplay.id;
                    }
                    return content;
                },

                getContentTypeForDocument: function (theDocument)
                {
                    switch (theDocument.group) {
                        case "diagrams":
                            return mentor.publisher.contentType.HARNESS_LAYOUT_DIAGRAM;
                        case "reports":
                            return mentor.publisher.contentType.HARNESS_LAYOUT_REPORT;
                        default:
                            return mentor.publisher.contentType.CUSTOM_VIEW;
                    }
                    ;
                },

                getDefaultDocument: function ()
                {
                    var defaultDocument,
                            firstDocumentSet;

                    defaultDocument = this.getFirstDocumentInGroupTitled("diagrams");
                    if (defaultDocument) {
                        return defaultDocument;
                    }

                    defaultDocument = this.getFirstDocumentInGroupTitled("reports");
                    if (defaultDocument) {
                        return defaultDocument;
                    }

                    firstDocumentSet = this.getFirstDocumentGroup();
                    return this.getFirstDocumentInGroup(firstDocumentSet);
                },

                findDocumentInGroupTitled: function (groupTitle, attrs)
                {
                    var documentGroup;

                    documentGroup = this.getDocumentGroupTitled(groupTitle);
                    return this.findDocumentInGroup(documentGroup, attrs);
                },

                findDocumentInGroup: function (documentGroup, attrs)
                {
                    return _.findWhere(documentGroup.documents, attrs);
                },

                getFirstDocumentInGroupTitled: function (title)
                {
                    var documentGroup;

                    documentGroup = this.getDocumentGroupTitled(title);
                    return this.getFirstDocumentInGroup(documentGroup);
                },

                getFirstDocumentInGroup: function (documentGroup)
                {
                    if (!(documentGroup && documentGroup.documents)) {
                        return;
                    }

                    var firstDocument = _.first(documentGroup.documents);
                    if (!firstDocument) {
                        return;
                    }

                    firstDocument.type = documentGroup.type;
                    firstDocument.group = documentGroup.title;

                    return firstDocument;
                },

                getDocumentGroupTitled: function (title)
                {
                    var documentGroups = this.get("documentSets");

                    return _.findWhere(documentGroups, {
                        "title": title
                    });
                },

                getFirstDocumentGroup: function ()
                {
                    var documentGroups = this.get("documentSets");

                    return _.first(documentGroups);
                }
            });

            return HarnessLayout;
        }
);