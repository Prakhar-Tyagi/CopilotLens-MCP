/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, window, require, describe, expect, it, listTemplateForTest*/
(function () {
    "use strict";
    var mockPack = new (Backbone.Model.extend())(), context, stubs, faceviewContent;

    stubs = {
        currentPackage : mockPack,
        jquery : $,
        underscore : _,
        backbone : Backbone,
        ConnectorFaceviewsCollection : new (Backbone.Collection.extend())(),
        fileDisplayHandler : {
            display : function (content) {
                faceviewContent = content;
            }
        }
    };
    context = createContext(stubs);

    context(['views/p/connectorFaceviewView'], function (connectorFaceviewView) {
        describe("connectorFaceviewViewTest", function () {

            beforeEach(function () {
                stubs.currentPackage.set("id", "projectId");
            });

            it("should be able to load twoDLocationView Module", function () {
                expect(connectorFaceviewView).toBeDefined();
            });

            it("should return the Title", function () {
                expect(connectorFaceviewView.getTitle()).toBe("FaceViewTitle");
            });

            it("should return the ClassName", function () {
                expect(connectorFaceviewView.getClassName()).toBe("faceViews");
            });

            it("should return the created URL", function () {
                var content = new (Backbone.Model.extend())({id: "testId", objectId: "testObjectId"});
                expect(connectorFaceviewView.createURL(content)).toBe("popout.html#/faceview/systemId/testObjectId/idtestId/projectId");
            });

            it("should return all the views", function () {
                var faceViews = [
                    new (Backbone.Model.extend())({
                        id: "testId",
                        path: "testPath",
                        'multiple-faceview-support': true,
                        view: "testView"
                    }),
                    new (Backbone.Model.extend())({
                        id: "testId",
                        path: "testPath",
                        'multiple-faceview-support': true,
                        view: "noViewSpecified"
                    }),
                    new (Backbone.Model.extend())({
                        id: "testId",
                        path: "testPath",
                        'multiple-faceview-support': false,
                        view: "testView"
                    })
                ];
                var expectedViewsResults = [
                    {
                        mainText: 'TranslatedtestView',
                        id: 'testId',
                        path: 'testPath'
                    },
                    {
                        mainText: 'Translated',
                        id: 'testId',
                        path: 'testPath'
                    }
                ];
                expect(connectorFaceviewView.getAllViews(faceViews)).toEqual(expectedViewsResults);
            });

            it("it should be able to open a connectorFaceviewView location in popout", function () {
                var connFaceview = new (Backbone.Model.extend())(), popoutURL;
                connFaceview.set({objectId : "objectId"});
                popoutURL = connectorFaceviewView.createURL(connFaceview);
                expect(popoutURL).toBe("popout.html#/faceview/systemId/objectId/projectId");
            });

            it("should be able to display a connectorFaceviewView", function () {
                var systemURL, displayedContent, model = new (Backbone.Model.extend())(), systemData = new (Backbone.Model.extend())(), stopEvent, resetContentPanel;

                model.set("systemId", "testSystemId");
                model.set("cavityTable", "cavityTable");
                stubs.ConnectorFaceviewsCollection.get = function () {
                    return model;
                }
                displayedContent = connectorFaceviewView.getItemContent("faceviewId");
                expect(JSON.stringify(displayedContent)).toBe('{"systemId":"testSystemId","cavityTable":"cavityTable","faceviews":[]}');
            });

        });
    });
})();