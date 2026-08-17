/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, ?SISW?), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer?s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor, $, getIdToHighlight*/
define("XRefsViewItem", ["PopoverItemView", "currentPackage", "fileDisplayHandler"],
        function (PopoverItemView, selectedPackage, fileDisplayHandler) {
            "use strict";
            var p = mentor.publisher;
            return function (xrefs) {
                return PopoverItemView.extend({
                    title: "XRefTitle",
                    className: "Links",
                    getData: function () {
                        return xrefs;
                    },
                    getTitle: function () {
                        return this.title;
                    },
                    getClassName: function () {
                        return this.className;
                    },
                    firstActiveSystem: function (xrefs) {
                        return xrefs[0];
                    },
                    shouldProcessDataBeforeDisplay: function () {
                        return true;
                    },

                    processDataBeforeRender: function (models) {
                        this.processXrefToShowCorrectSystemName(models);
                    },

                    getSystemObject: function (systemId) {
                        return mentor.publisher.project.getObjectById(systemId);
                    },
                    getDiagramName: function (xrefDisplayName) {
                        var diagramNameStartingIndex;
                        xrefDisplayName = xrefDisplayName || "";
                        diagramNameStartingIndex = xrefDisplayName.lastIndexOf(":");
                        if (diagramNameStartingIndex >= 0) {
                            return xrefDisplayName.substring(diagramNameStartingIndex + 1);
                        }
                        return "";
                    },

                    processGroupedXrefsToAddCorrectSystemName: function (systemIdInMap, systemIdToXrefMap, systems,
                            systemObject,
                            diagramWithinSystem) {
                        var p = mentor.publisher;
                        for (systemIdInMap in systemIdToXrefMap) {
                            if (systemIdToXrefMap.hasOwnProperty(systemIdInMap)) {
                                systems = systemIdToXrefMap[systemIdInMap];
                                systemObject = this.getSystemObject(systemIdInMap);
                                for (diagramWithinSystem in systems) {
                                    if (systems.hasOwnProperty(diagramWithinSystem)) {
                                        if (diagramAsSystemsObjectFactoryImpl) {
                                            //only show diagram name as main text for system as diagram flow
                                            systems[diagramWithinSystem].set("mainText",
                                                    this.getDiagramName(systems[diagramWithinSystem].get("mainText")));

                                        }
                                        else {

                                            systems[diagramWithinSystem].set("mainText",
                                                    Utils.handleTranslation(systemObject.mainText,true) + ":" +
                                                    Utils.handleTranslation(this.getDiagramName(
                                                            systems[diagramWithinSystem].get("mainText")),true));
                                        }
                                    }
                                }

                            }
                        }
                    },

                    processXrefToShowCorrectSystemName: function (crossRefs) {
                        var systemIdToXrefMap = {}, xref, systemIdInMap, systems, systemObject, diagramWithinSystem;
                        crossRefs = crossRefs || [];
                        if (crossRefs.length > 0) {
                            this.groupXrefsBySystemId(xref, crossRefs, systemIdToXrefMap);
                            this.processGroupedXrefsToAddCorrectSystemName(systemIdInMap, systemIdToXrefMap, systems,
                                    systemObject,
                                    diagramWithinSystem);
                        }
                    },
                    groupXrefsBySystemId: function (xref, crossRefs, systemIdToXrefMap) {
                        for (xref in crossRefs) {
                            if (crossRefs.hasOwnProperty(xref)) {
                                if (!systemIdToXrefMap[crossRefs[xref].get("systemId")]) {
                                    systemIdToXrefMap[crossRefs[xref].get("systemId")] = [];
                                }
                                systemIdToXrefMap[crossRefs[xref].get("systemId")].push(crossRefs[xref]);
                            }
                        }
                    },

                    displayContent: function (content) {
                        var type, xrefToShow;
                        //this.getWindowObj().mentor.publisher.detailLayoutManager.resetContentPanel();
                        if (!content.diagramId) {
                            type = content.type;
                            xrefToShow = this.fetchCrossReference(content.systemId, content.objectId);
                            if (!xrefToShow) {
                                return;
                            }
                            else {
                                content = xrefToShow;
                                content.type = type;
                            }

                        }
                        p.crossReferenceHandler.showXref(content);

                    },
                    events: {
                        "click .listItem": "popoverItemClicked",
                        "mouseover .listItem": "showToolTip",
                        "mouseout .listItem": "removeToolTip",
                        "click .listItem>.popUp": "popOut"
                    },
                    getDiagramId: function (content) {
                        var diagramId = content.get("diagramId") || "", path, length, startIndex, crossReference;
                        if (!diagramId) {
                            path = content.get("path") || "";
                            if (path) {
                                length = path.length;
                                startIndex = path.lastIndexOf("\\");
                                diagramId = path.substr(startIndex + 1).replace(".svg", "");
                            }
                            else {
                                crossReference =
                                        this.fetchCrossReference(content.get("systemId"), content.get("objectId"));
                                if (crossReference && crossReference.diagramId) {
                                    diagramId = crossReference.diagramId;
                                }
                            }
                        }
                        return diagramId;
                    },

                    fetchCrossReference: function (systemId, objectId) {
                        var designObject, crossReferences, crossReference;
                        designObject = p.project.loadObjectData(systemId, objectId);
                        crossReferences = [];
                        if (designObject.getCrossReferences) {
                            crossReferences = designObject.getCrossReferences({
                                includeOpenedDiagrams: true
                            }).listItems;
                        }
                        if (!crossReferences || crossReferences.length === 0) {
                            return undefined;
                        }
                        crossReference = _.findWhere(crossReferences, {id: systemId});
                        if (crossReference) {
                            return crossReference;
                        }
                        return crossReferences[0];
                    },

                    createURL: function (content) {
                        return p.popoutHandler.createURL({
                            type: p.contentType.SYSTEM_SVG,
                            systemId: content.get("systemId"),
                            objectId: content.get("objectId"),
                            diagramId: this.getDiagramId(content),
                            projectId: selectedPackage.get("id").replace("\\", "/")
                        });

                        // return "popout.html#/system/" + content.get("systemId") + "/" +
                        //     this.getDiagramId(content) + "/" +
                        //     selectedPackage.get("id").replace("\\", "/") + "/" + content.get("objectId");
                    },

                    getItemContent: function (itemId) {
                        var clickedSystem, content, path, optionExpression;
                        clickedSystem = xrefs.get(itemId);
                        if (!clickedSystem) {
                            return null;
                        }
                        path = clickedSystem.get('path');
                        optionExpression =
                                clickedSystem.attributes.getActiveConfiguration ?
                                        clickedSystem.attributes.getActiveConfiguration() : "";
                        content = {
                            id: this.getWindowObj().getIdToHighlight({
                                id: clickedSystem.get('idToHighlight'),
                                diagramId: clickedSystem.get('diagramId')
                            }),
                            systemId: clickedSystem.get('id'),
                            diagramId: clickedSystem.get('diagramId'),
                            objectId: clickedSystem.get('objectId'),
                            reset: false,
                            type: mentor.publisher.contentType.SYSTEM_SVG,
                            path: path,
                            optionExpression: optionExpression
                        };
                        p.contentArea.closeAllSplitPanelsIfNewSystemIsOpened(content);
                        return content;
                    }
                });
            }
        });

