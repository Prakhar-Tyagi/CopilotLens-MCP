/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Backbone, mentor, _, getWindowObj, $*/
define("harnessLayouts",
        [
            "SectionCollection",
            "currentPackage",
            "models/HarnessLayout",
            "models/selectedSystem",
        ],
        function (BaseCollection,
                currentPackage,
                HarnessLayout,
                selectedSystem)
        {
            "use strict";

            var p = mentor.publisher, getDocumentsByTitle;
            getDocumentsByTitle = function (harness, type)
            {
                return _.filter(harness.documentSets, function (documentSet)
                        {
                            return documentSet && documentSet.title === type;
                        }) || [];
            };

            var HarnessLayouts = BaseCollection.extend({
                model: HarnessLayout,
                category: p.documentCategory.HARNESS_LAYOUT_DIAGRAM,

                initialize: function ()
                {
                    currentPackage.on("change:id", this.fetch, this);
                    currentPackage.on("change:language", this.fetch, this);
                },
                getDesignType: function (design)
                {
                    return p.contentType.HARNESS_LAYOUT_DIAGRAM;
                },

                parse: function (harnessDesigns)
                {
                    var harnessDiagramAsSystems = [];
                    harnessDesigns = harnessDesigns || [];
                    if (getWindowObj().diagramAsSystemsObjectFactoryImpl && harnessDesigns.length > 0) {

                        _.each(harnessDesigns, function (harness)
                        {
                            var diagramDocuments, reports;
                            diagramDocuments = getDocumentsByTitle(harness, "diagrams");
                            reports = getDocumentsByTitle(harness, "reports");

                            _.each(diagramDocuments, function (diagramSet)
                            {
                                _.each(diagramSet.documents, function (diagram)
                                {
                                    var diagramAsSystem = {}, diagrams;
                                    diagramAsSystem.id = diagram.id;
                                    diagramAsSystem.mainText = diagram.mainText;
                                    diagramAsSystem.subText = harness.mainText;
                                    diagramAsSystem.folders = harness.folders;
                                    diagramAsSystem.tooltips = [];

                                    diagramAsSystem.tooltips.push({name: "Design Name", value: harness.mainText});

                                    _.each(harness.tooltips, function (tooltip)
                                    {
                                        if (tooltip && tooltip.name !== "Name") {
                                            diagramAsSystem.tooltips.push(tooltip);
                                        }
                                    });
                                    diagramAsSystem.documentSets = [];
                                    diagramAsSystem.documentSets = diagramAsSystem.documentSets.concat(reports);
                                    diagrams = {title: "diagrams", type: "svg", documents: []};
                                    diagrams.documents.push(diagram);
                                    diagramAsSystem.documentSets.push(diagrams);
                                    harnessDiagramAsSystems.push(diagramAsSystem);
                                });

                            });

                        });
                        return harnessDiagramAsSystems;
                    }
                    else if (harnessDesigns.length > 0) {
                        _.each(harnessDesigns, function (harness)
                        {
                            if (harness.tooltips && harness.tooltips.length > 0) {
                                harness.mainText = harness.tooltips[0].value;
                            }

                            if (harness.tooltips && harness.tooltips.length > 1) {
                                harness.subText = harness.tooltips[1].value;
                            }
                        });
                    }
                    return this.afterLoad(harnessDesigns);
                },
                afterLoad: function (harnessDesigns)
                {
                    return harnessDesigns;
                },

                fetch: function (options)
                {
                    var result = [], that = this;

                    if (currentPackage.has("id")) {
                        $.ajax({
                            url: Utils.prepareFilePath(currentPackage.id + "/harnessLayouts.json"),
                            async: false,
                            success: function (data, textStatus, xhr)
                            {
                                result = that.parse(data);
                            },
                            dataType: "json"
                        });
                    }

                    this.reset(result);
                },
                getHarnessLayoutByDiagramId: function (diagramId)
                {
                    var diagram;
                    var harnesses = _.filter(this.models, function (harness)
                    {
                        if (harness && harness.attributes) {
                            var docSet = _.filter(harness.attributes.documentSets, function (documentSet)
                            {
                                var docs = _.filter(documentSet.documents, function (doc)
                                {
                                    var match = doc.id.indexOf(diagramId) >= 0;
                                    if (match) {
                                        diagram = doc;
                                    }
                                    return diagram;
                                });
                                return docs.length > 0;
                            });
                            return docSet.length > 0;
                        }
                        return false;
                    });
                    if (harnesses.length > 0) {
                        return {design: harnesses[0], diagram: diagram};
                    }
                },
                getHarnessLayout: function (content)
                {
                    var diagram,
                            diagramAttrs,
                            findAttrs,
                            harnessLayout,
                            layoutId, type;

                    type = content.type;
                    if (content.get && content.get("layoutId")) {
                        content = content.attributes;
                    }
                    findAttrs = _.omit(content, "listItemId", "group", "layoutId", "reset", "type", "documentName",
                            "doNotSaveAsHistory", "objectId", "avoidRenderingIfOpen");

                    layoutId = content.layoutId || content.get("layoutId");
                    harnessLayout = this.get(layoutId);

                    diagramAttrs = harnessLayout.findDocumentInGroupTitled(content.group, findAttrs);
                    diagram = new Backbone.Model(diagramAttrs);

                    diagram.set(content);
                    diagram.set("type", type);
                    return {
                        layoutId: layoutId,
                        diagram: diagram
                    };

                },
                setSelectedHarnessDataToRender: function (content, idAttr, harnessDataType)
                {
                    harnessDataType = harnessDataType || "harnessLayoutDiagram";
                    idAttr = idAttr || "harnessLayoutId";
                    var harnessDiagramLayout = this.getHarnessLayout(content);
                    if (!harnessDiagramLayout) {
                        return;
                    }
                    var isOpen = selectedSystem.get(harnessDataType) &&
                            selectedSystem.get(harnessDataType).get('id') === harnessDiagramLayout.diagram.get('id');
                    var shouldRender = !(isOpen && content.avoidRenderingIfOpen)
                    if (shouldRender) {
                        selectedSystem.set("objectId", content.objectId,{silent: true});
                        selectedSystem.set(idAttr, harnessDiagramLayout.layoutId);
                        selectedSystem.set(harnessDataType, harnessDiagramLayout.diagram, {silent: true});
                        selectedSystem.trigger("change:" + harnessDataType);
                    }
                },
                getType: function (layoutId)
                {
                    var harnessLayout = this.get(layoutId);
                    var index = this.indexOf(harnessLayout);
                    var designType;
                    if (index != -1 && getWindowObj().mentor.publisher.DesignComparisionType) {
                        designType = getWindowObj().mentor.publisher.DesignComparisionType[index + 1];
                    }

                    if (!designType) {
                        designType = harnessLayout.get("designType");
                    }

                    return designType;
                },
                getDataToRender: function (dataContainer)
                {

                    var documentSetId = dataContainer.getDocumentSetId();
                    var documentSet = dataContainer.getDocumentSetById(documentSetId);
                    var documents = documentSet.getDocumentsInGroupTitled(dataContainer.getDocumentGroup());

                    var options = {};

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
                }
            });

            return new HarnessLayouts();
        }
);