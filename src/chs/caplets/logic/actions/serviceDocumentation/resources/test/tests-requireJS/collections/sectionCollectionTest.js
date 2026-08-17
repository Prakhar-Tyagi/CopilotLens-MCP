/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global require, describe, it, expect, beforeEach, Backbone, afterEach, createContext*/
(function () {
    "use strict";
    var mockPack = new (Backbone.Model.extend())(), context, stubs;

    stubs = {
        currentPackage : mockPack,
        textSearch : function (collections) {
            return {
                matchText : function (text, searchText) {

                },
                filterByText : function (searchText) {
                    return collections.models;
                }
            };

        }
    };
    context = createContext(stubs);

    context(['SectionCollection', "currentPackage"], function (SectionCollection, currentPackage) {
        var designObject, DesignObject = SectionCollection.extend({
            getData : function () {
                var Model = Backbone.Model.extend({}), model1 = new Model(), model2 = new Model(), testData;
                testData = [
                    {
                        mainText : "testData",
                        getToolTips : function () {
                            return [];
                        },
                        subText : "testData"
                    },
                    {
                        mainText : "testData2",
                        getToolTips : function () {
                            return [];
                        },
                        subText : "testData2"
                    }
                ];
                model1.set(testData[0]);
                model2.set(testData[1]);
                return [model1, model2];
            }
        });
        describe("SectionCollection", function () {
            beforeEach(function () {
                designObject = new DesignObject();
            });
            it("should be able to load SectionCollection Module", function () {
                expect(designObject).toBeDefined();
            });

            it("should fetch data when project id is changed", function () {
                expect(designObject.getModels().length).toBe(0);
                currentPackage.set("id", "someId");
                expect(designObject.getModels().length).toBe(2);
            });

            it("should fetch data again when language changes", function () {
                expect(designObject.getModels().length).toBe(0);
                currentPackage.set("language", "fr");
                expect(designObject.getModels().length).toBe(2);
            });

            it("should fetch data again when vin changes", function () {
                expect(designObject.getModels().length).toBe(0);
                currentPackage.set("vin", "LHD");
                expect(designObject.getModels().length).toBe(2);
            });

            it("should fetch data again when configuration applied", function () {
                expect(designObject.getModels().length).toBe(0);
                currentPackage.set("config", "RHD_Market");
                expect(designObject.getModels().length).toBe(2);
            });

            it("should fetch data again when text filter is applied", function () {
                expect(designObject.getModels().length).toBe(0);
                currentPackage.set("searchText", "testData2");
                currentPackage.set("id", "someId2");
                expect(designObject.getModels().length).toBe(2);
            });

        });
    });
})();
