/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global require, describe, it, expect, Backbone, beforeEach, afterEach, mentor, Utils, _, createContext, $*/
(function () {
    "use strict";
    var context, bootStrap = {
        isComponentLoadingComplete: function () {
            return true;
        },
        renderComponents: function () {

        }
    }, Model = Backbone.Model.extend(), Collection = Backbone.Collection.extend(), View = Backbone.View.extend();
    var fakeFileDisplayHandler = {
        display: function (content) {
            this.fileHandles[content.type] = (content);
        },
        addFileHandler: function (type, fileHandler) {
            this.fileHandles = this.fileHandles || {};
            this.fileHandles[type] = fileHandler;
        },
        fileHandles: {}
    };

    var stubs = {
        backbone: Backbone,
        bootstrap: bootStrap,
        currentPackage: new Model(),
        fileDisplayHandler: fakeFileDisplayHandler,
        DesignObjectPopoverModel: new Model(),
        LocationViewObjectPopoverModel: new Model(),
        LocationViews: new Collection(),
        Harnesses: new Collection(),
        "collections/informations": new Collection(),
        "preferences": {},
        "models/HarnessLayout": new Model(),
        "models/detailsPanelModel": new Model(),
        EULA: new View(),
        Diagnostics: new View(),
        underscore: _,
        jquery: $,
        Packages: new Collection(),
        Package: new Model(),
        ComponentLoader: {
            loadComponents: function () {
            }
        }

    };

    stubs.currentPackage.set({id: "packageId"});
    context = createContext(stubs);
    var router, eulaShown, org_render;

    function isURLMatchingARoute(URLFragment)
    {
        var handlers = Backbone.history.handlers.splice(0, Backbone.history.handlers.length - 2);
        var matched = _.any(handlers, function (handler) {
            if (handler.route.test(URLFragment)) {
                return true;
            }
        });
        return matched;
    }

    context(["router"],
            function (Router) {

                describe("RouterTests", function () {

                    beforeEach(function () {
                        router = new Router();
                        // Backbone.history.start();

                        var faceviews = [];
                        faceviews.push({mainText: "P1-Front", id: "faceviewId", view: "Front"});
                        faceviews.push({mainText: "P1-Top", id: "faceviewId", view: "Top"});
                        router.getObjectData = function () {
                            return {
                                getFaceviews: function () {
                                    return {
                                        listItems: faceviews
                                    };
                                }
                            };
                        };

                        stubs.EULA.render = function () {
                            eulaShown = true;
                            Utils.createCookie("eula", "accepted", 1);
                        };
                    });

                    afterEach(function () {
                        eulaShown = false;
                        Utils.createCookie("eula", "accepted", -1);
                    });

                    it("should be able to load router module", function () {
                        expect(Router).toBeDefined();

                    });

                    it("should be able to search faceview by viewName", function () {
                        var object = router.findObjectByName(
                                [{mainText: "faceview1", view: "Front"}, {mainText: "faceview1", view: "Side"}],
                                "Side",
                                function (object) {
                                    return object.view.toLocaleLowerCase();
                                }
                        );
                        expect(JSON.stringify(object)).toBe('{"mainText":"faceview1","view":"Side"}');
                    });

                    it("should be able to load face view via faceview URL", function () {
                        router.openFaceView("testSystemId", "testConnId", "Front");
                        expect(JSON.stringify(
                                fakeFileDisplayHandler.fileHandles[mentor.publisher.contentType.CONNECTOR_FACE_VIEW]))
                                .toBe('{"objectId":"testSystemId","systemId":"testConnId","viewId":"Front","id":"Front","type":"connectorFaceView"}');

                    });

                    it("should be able to load single split panel using hash fragments", function () {
                        var objectObj,
                                testContentPanel = {id: "testId", path: "testPath/subPath"}, actualObjectId;
                        router.setDocumentRouter({
                            render: function (objectId, docInformationObj) {
                                actualObjectId = objectId;
                                objectObj = docInformationObj;
                            }
                        });
                        var URLFragment = "document_views/projectId/packageName/objectId/" +
                                JSON.stringify(testContentPanel);
                        var matched = isURLMatchingARoute(URLFragment);
                        expect(matched).toBeTruthy();

                        router.renderDocuments("projectId", "packageName", "testObjectId", testContentPanel);

                        expect(actualObjectId).toBe("testObjectId");
                        expect(JSON.stringify(testContentPanel)).toBe(JSON.stringify(objectObj));

                    });
                    it("should be able to load two documents in split panels using hash fragments", function () {
                        var objectObj,
                                testContentPanel1 = {id: "testId", path: "testPath1/subPath1"},
                                testContentPanel2 = {id: "testId2", path: "testPath2/subPath2"},
                                objectObj1,
                                objectObj2,
                                actualObjectId;
                        router.setDocumentRouter({
                            render: function (objectId, doc1InformationObj, doc2InformationObj) {
                                actualObjectId = objectId;
                                objectObj1 = doc1InformationObj;
                                objectObj2 = doc2InformationObj;
                            }
                        });
                        var URLFragment = "document_views/projectId/packageName/objectId/" +
                                JSON.stringify(testContentPanel1) + "/" +
                                JSON.stringify(testContentPanel2);
                        var matched = isURLMatchingARoute(URLFragment);
                        expect(matched).toBeTruthy();

                        router.renderDocuments("projectId", "packageName", "testObjectId", testContentPanel1,
                                testContentPanel2);

                        expect(actualObjectId).toBe("testObjectId");
                        expect(JSON.stringify(testContentPanel1)).toBe(JSON.stringify(objectObj1));
                        expect(JSON.stringify(testContentPanel2)).toBe(JSON.stringify(objectObj2));

                    });

                    it("should be able to load three documents in split panels using hash fragments", function () {
                        var objectObj,
                                testContentPanel1 = {id: "testId", path: "testPath1/subPath1"},
                                testContentPanel2 = {id: "testId2", path: "testPath2/subPath2"},
                                testContentPanel3 = {id: "testId3", path: "testPath2/subPath3"},
                                objectObj1,
                                objectObj2,
                                objectObj3,
                                actualObjectId;
                        router.setDocumentRouter({
                            render: function (objectId, doc1InformationObj, doc2InformationObj, doc3InformationObj) {
                                actualObjectId = objectId;
                                objectObj1 = doc1InformationObj;
                                objectObj2 = doc2InformationObj;
                                objectObj3 = doc3InformationObj;
                            }
                        });
                        var URLFragment = "document_views/projectId/packageName/objectId/" +
                                JSON.stringify(testContentPanel1) +
                                "/" + JSON.stringify(testContentPanel2) +
                                "/" + JSON.stringify(testContentPanel3);

                        var matched = isURLMatchingARoute(URLFragment);
                        expect(matched).toBeTruthy();

                        router.renderDocuments("projectId", "packageName", "testObjectId",
                                testContentPanel1,
                                testContentPanel2,
                                testContentPanel3);

                        expect(actualObjectId).toBe("testObjectId");
                        expect(JSON.stringify(testContentPanel1)).toBe(JSON.stringify(objectObj1));
                        expect(JSON.stringify(testContentPanel2)).toBe(JSON.stringify(objectObj2));
                        expect(JSON.stringify(testContentPanel3)).toBe(JSON.stringify(objectObj3));

                    });

                    it("should trigger ShowViewerEvent when viewer page is shown", function () {
                        var shown = false;
                        stubs.currentPackage.once("ShowViewerEvent", function () {
                            shown = true;
                        })
                        router.showViewerPage();
                        expect(shown).toBeTruthy();
                    });

                });

            });
})();

