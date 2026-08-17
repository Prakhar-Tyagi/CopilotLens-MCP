/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor*/
define([
            "filters/documentContentBasedFilter",
            "currentPackage"],
        function (documentContentBasedFilter, currentPackage) {
            "use strict";
            var p = mentor.publisher;
            return {
                SHOW_FIRST_SECTION_ITEM: -1,
                setDocFinder: function (docFinder) {
                    /*used for UTs to set mock for documentContentBasedFilter*/
                    this.documentContentBasedFilter = docFinder;
                },
                setCurrentPackage: function (packageObj) {
                    //for UTs
                    this.currentPackage = packageObj;
                },
                areIndexesGen: function (currPackage) {
                    return documentContentBasedFilter.areIndexesGenerated(currPackage.get("id"));
                },
                loadFilteredDocs: function (id, category) {
                    var docFinder = this.documentContentBasedFilter || documentContentBasedFilter;
                    var currPackage = this.currentPackage || currentPackage;
                    var searchText = currPackage.get("searchText");
                    var areIndexesGen =
                            this.areIndexesGen(currPackage);
                    var result;
                    if (areIndexesGen && searchText) {
                        result = docFinder.getFilteredDataById(id, category, searchText);
                    }

                    if (!result) {
                        result = this.SHOW_FIRST_SECTION_ITEM;
                    }
                    return result;
                },
                getCustomDesignData: function (items, typeAttrName) {
                    for (var prop in items) {
                        if (items.hasOwnProperty(prop)) {
                            for (var index in items[prop]) {
                                if (items[prop].hasOwnProperty(index)) {
                                    //SC does not handle XML custom data
                                    if (items[prop][index].id.endsWith(".xml")) {
                                        index++;
                                        continue;
                                    }
                                    items = {id: items[prop][index].id};
                                    items[typeAttrName] = p.documentCategory.CUSTOM_VIEW;
                                    items.customDataType = prop;
                                    break;
                                }

                            }
                        }
                    }
                    return items;
                },
                getFirstFilteredDoc: function (id, category, diagramType, typeAttrName) {
                    diagramType = diagramType || "svg";
                    typeAttrName = typeAttrName || "type";
                    var items = this.loadFilteredDocs(id, category);
                    if (items !== this.SHOW_FIRST_SECTION_ITEM) {
                        if (items[p.documentCategory.DIAGRAMS]) {
                            items = {id: items[p.documentCategory.DIAGRAMS][0].id};
                            items[typeAttrName] = diagramType;
                        }
                        else if (items[p.documentCategory.REPORTS]) {
                            items = {id: items[p.documentCategory.REPORTS][0].id};
                            items[typeAttrName] = p.documentCategory.REPORTS;
                        }
                        else {
                            /*for custom design data*/
                            items = this.getCustomDesignData(items, typeAttrName);

                        }
                    }
                    return items;
                }
            };

        });
