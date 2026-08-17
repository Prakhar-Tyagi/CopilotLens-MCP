/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, window, require, describe, it, expect, createContext, Backbone*/
(function () {
    "use strict";
    var mockModel = Backbone.Model.extend(), context, stubs, popoverCollectionTest, xrefs, activeConfig;

    stubs = {
        PopoverItem : mockModel,
        currentPackage : new mockModel(),
        textSearch : new mockModel(),
        XRefActiveConfigModel : {
            getActiveConfig : function () {
                return "testConf";
            },
            getFilter : function () {
                return {
                    applyFilter : function (items, config) {
                        xrefs = items;
                        activeConfig = config;
                        return xrefs;
                    }
                };
            }
        }
    };
    context = createContext(stubs);

    popoverCollectionTest = function (testName, collectionName, designObject, baseObject) {
        baseObject = baseObject || stubs.PopoverItem;
        context([collectionName], function (collectionInstance) {
            var model = {
                getAttributes : function () {
                    return {
                        listItems : ["attr1", "attr2"]
                    };
                }
            };

            describe(testName, function () {
                it("should extend PopoverItem", function () {
                    expect(collectionInstance instanceof  baseObject).toBeTruthy();
                });

                it("should be able to fetch attributes from designObject", function () {
                    var data = collectionInstance.getData(designObject);
                    expect(data.length).toBe(1);
                });
            });

        });
    };

    popoverCollectionTest("AttributeCollectionTest", "AttributesCollection", {getAttributes : function () {
        return { listItems : [
            {}
        ] };
    }});

    popoverCollectionTest("ConnectorFaceviewsCollectionTest", "ConnectorFaceviewsCollection",
        {getFaceviews : function () {
            return { listItems : [
                {}
            ] };
        }});

    popoverCollectionTest("threeDViewCollectionTest", "ThreeDViewCollection",
        {get3DViews : function () {
            return { listItems : [
                {}
            ] };
        }});

    popoverCollectionTest("twoDLocationCollectionTest", "TwoDLocationCollection",
        {get2dLocationViews : function () {
            return { listItems : [
                {}
            ] };
        }});

    popoverCollectionTest("CustomDataCollectionTest", "CustomDataCollection",
        {getCustomData : function () {
            return  [
                {}
            ];
        }});

    popoverCollectionTest("xrefsCollectionTest", "XRefsCollection",
        {getCrossReferences : function () {
            return { listItems : [
                {}
            ] };
        }});

    popoverCollectionTest("configurationsCollTest", "ConfigurationsCollection",
        [
            {}
        ], Backbone.Collection);

    popoverCollectionTest("optionsCollectionTest", "OptionsCollection",
        [
            {}
        ], Backbone.Collection);

    popoverCollectionTest("diagramsCollectionTest", "DiagramsCollection",
        [
            {}
        ], Backbone.Collection);

    popoverCollectionTest("languagesCollectionTest", "LanguagesCollection",
        [
            {}
        ], Backbone.Collection);

    popoverCollectionTest("packagesCollectionTest", "PackagesCollection",
        [
            {}
        ], Backbone.Collection);

    popoverCollectionTest("printContentCollectionTest", "PrintContentCollection",
        [
            {}
        ], Backbone.Collection);

    popoverCollectionTest("printOptionsCollectionTest", "PrintOptionsCollection",
        [
            {}
        ], Backbone.Collection);

})();