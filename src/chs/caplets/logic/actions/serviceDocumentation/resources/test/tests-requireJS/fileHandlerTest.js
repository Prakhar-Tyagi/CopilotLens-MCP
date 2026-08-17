/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global require, describe, it, expect, beforeEach, Backbone, afterEach, createContext, sinon, mentor*/
(function ()
{
    "use strict";
    var Model = Backbone.Model.extend(), context, stubs, Collection = Backbone.Collection.extend({}), testRunSuccess;

    stubs = {
        currentPackage: new Model(),
        textSearch: {},
        systems: {},
        "collections/informations": new Collection(),
        "models/HarnessLayout": new Model(),
        FaultCodes: new Model(),
        LocationViews: new Model(),
        Harnesses: new Collection()
    };
    context = createContext(stubs);

    context(['fileDisplayHandler', "models/selectedSystem", "collections/informations"],
            function (fileDisplayHandler, selectedSystem, informations)
            {
                describe("fileDisplayHandlerTest", function ()
                {
                    var spy = sinon.spy(selectedSystem, "trigger"), systemSpy;
                    systemSpy = sinon.spy(selectedSystem, "set");
                    testRunSuccess = true;

                    // To avoid TypeErrors in UTs
                    mentor.publisher.config = mentor.publisher.config || {};

                    beforeEach(function ()
                    {
                        spy.reset();
                        systemSpy.reset();
                    });
                    it("should be able to load fileDisplayHandler Module", function ()
                    {
                        expect(fileDisplayHandler).toBeDefined();
                    });

                    it('should set content attributes or content to the model', function (done) {
                        var content = {attributes: {key: 'value'}};
                        require(["views/contentpanel/threeDXMLPanel"], function (threeDXMLPanel) {
                            var Model = new Backbone.Model(content.attributes);
                            threeDXMLPanel.openObjectThreeD();
                            expect(threeDXMLPanel.openObjectThreeD.calledOnce).toBe(true);
                        });
                        expect(content.attributes.key).toBe('value');
                    });

                    it("should be able to display a html file", function ()
                    {
                        var content = {path: "fakeFilePath.html", mainText: "fakeFile"};
                        fileDisplayHandler.display(content);
                        expect(selectedSystem.trigger.getCall(0).args[0]).toBe("change:customContent");
                    });

                    it("should be able to display a htm file", function ()
                    {
                        var content = {path: "fakeFilePath.htm", mainText: "fakeFile"};
                        fileDisplayHandler.display(content);
                        expect(selectedSystem.trigger.getCall(0).args[0]).toBe("change:customContent");
                    });

                    it("should be able to display a pdf file", function ()
                    {
                        var content = {path: "fakeFilePath.pdf", mainText: "fakeFile"};
                        fileDisplayHandler.display(content);
                        expect(selectedSystem.trigger.getCall(0).args[0]).toBe("change:customContent");
                    });

                    it("should be able to display a jpg file", function ()
                    {
                        var content = {path: "fakeFilePath.jpg", mainText: "fakeFile"};
                        fileDisplayHandler.display(content);
                        expect(selectedSystem.trigger.getCall(0).args[0]).toBe("change:customContent");
                    });

                    it("should be able to display a jpeg file", function ()
                    {
                        var content = {path: "fakeFilePath.jpeg", mainText: "fakeFile"};
                        fileDisplayHandler.display(content);
                        expect(selectedSystem.trigger.getCall(0).args[0]).toBe("change:customContent");
                    });

                    it("should be able to display a svg file", function ()
                    {
                        var content = {path: "fakeFilePath.svg", mainText: "fakeFile"};
                        fileDisplayHandler.display(content);
                        expect(selectedSystem.trigger.getCall(0).args[0]).toBe("change:customContent");
                    });

                    it("should be able to pass correct data when event is triggered to open the file", function ()
                    {
                        var content = {path: "fakeFilePath.svg", mainText: "fakeFile"};

                        fileDisplayHandler.display(content);
                        expect(selectedSystem.set.getCall(0).args[0]).toBe("customContent");
                        expect(selectedSystem.set.getCall(0).args[1].get("mainText")).toBe(content.mainText);
                        expect(selectedSystem.set.getCall(0).args[1].get("path")).toBe(content.path);
                    });

                    it("should be able to display information file", function ()
                    {
                        var content = {id: "informationContentId", type: mentor.publisher.contentType.CUSTOM_VIEW};
                        sinon.spy(informations, "get");
                        fileDisplayHandler.display(content);
                        expect(informations.get.getCall(0).args[0]).toBe(content.id);
                    });

                    it("should be able reset selected System's data when reset flag is passed as true", function ()
                    {
                        var oldVal, layoutmanReset, content = {
                            id: "informationContentId",
                            type: mentor.publisher.contentType.CUSTOM_VIEW,
                            reset: true
                        };

                        selectedSystem.set.reset();
                        oldVal = fileDisplayHandler.resetLayoutManager;
                        fileDisplayHandler.resetLayoutManager = function ()
                        {
                            layoutmanReset = true;
                        };
                        fileDisplayHandler.display(content);
                        expect(layoutmanReset).toBeTruthy();

                        expect(JSON.stringify(selectedSystem.set.getCall(0).args[0])).toBe('{"id":"","systemId":"","customContent":"","faceview":"","faultCode":""' +
                        ',"locationView":"","harness":"","reportId":"","path":"","reportPath":"","diagramId":"","objectId":"","diagnostic":"","harnessLayoutId":"",' +
                        '"harnessLayoutDiagram":"","harnessLayoutReport":""}');

                        expect(selectedSystem.trigger.getCall(0).args[0]).toBe("change:systemId");
                        fileDisplayHandler.resetLayoutManager = oldVal;
                    });

                    it("should be able to display a location view file", function ()
                    {

                        var content = {id: "locationViewId", type: mentor.publisher.contentType.LOCATION_VIEWS};
                        selectedSystem.trigger.reset();
                        var stub = sinon.stub(stubs.LocationViews, "get", function ()
                        {
                            var locationView = new (Backbone.Model.extend())();
                            locationView.set("path", "somePath");
                            return locationView;
                        });
                        fileDisplayHandler.display(content);
                        expect(selectedSystem.trigger.getCall(0).args[0]).toBe("change:locationView");
                        expect(selectedSystem.get("locationView").path).toBe("somePath");

                        spy.reset();

                        fileDisplayHandler.display(content);
                        expect(selectedSystem.trigger.getCall(2)).toBe(null);
                        stub.restore();
                    });

                    it("should be able to display a faceview", function ()
                    {
                        var content = {
                            id: "locationViewId",
                            type: mentor.publisher.contentType.CONNECTOR_FACE_VIEW,
                            systemId: "someSystemId",
                            get: function ()
                            {
                                return "somesystemId";
                            }
                        };
                        fileDisplayHandler.display(content);
                        expect(selectedSystem.get("faceview")).toBe(content);
                    });

                    it("should be able to display a faultcode", function ()
                    {
                        var content = {
                            id: "faultcodeid", type: mentor.publisher.contentType.FAULT_CODE, get: function ()
                            {
                                return "somesystemId";
                            }
                        };

                        var stub = sinon.stub(stubs.FaultCodes, "get", function (id)
                        {
                            expect(id).toBe("faultcodeid");
                            return {};
                        });
                        selectedSystem.set.reset();
                        fileDisplayHandler.display(content);
                        expect(selectedSystem.set.getCall(0).args[0]).toBe("faultCode");
                        stub.restore();
                    });

                    it("should be able to display a harness report", function ()
                    {
                        var content = {
                            id: "harnessId", type: mentor.publisher.contentType.HARNESS, get: function ()
                            {
                                return "somesystemId";
                            }
                        };

                        var stub = sinon.stub(stubs.Harnesses, "get", function (id)
                        {
                            return {};
                        });
                        selectedSystem.set.reset();
                        fileDisplayHandler.display(content);
                        expect(selectedSystem.set.getCall(0).args[0]).toBe("harness");
                    });

                    it("should be able to display a gournd report", function ()
                    {
                        var content = {
                            id: "ground-1",
                            type: mentor.publisher.contentType.CAPITAL_REPORT,
                            mainText: "Grounds",
                            get: function (key)
                            {
                                if (key == "withoutTranslation") {
                                    return {
                                        mainText: "{Grounds}",
                                    };
                                }
                                return "somesystemId";
                            },
                        };
                        selectedSystem.set.reset();
                        fileDisplayHandler.display(content);
                        let model = selectedSystem.set.getCall(0).args[1];
                        expect(model.get("type")).toBe("capitalreport");
                        expect(model.get("mainText")).toBe("{Grounds}");
                    });

                    it("should save content panel state when a new document is open", function ()
                    {
                        var saveAsHistory_actual, objectId_actual;
                        var isWaiting = true;
                        fileDisplayHandler.setMultipleDocumentRouter({
                            save: function (saveAsHistory, objectId)
                            {
                                saveAsHistory_actual = saveAsHistory;
                                objectId_actual = objectId;
                            }
                        });
                        selectedSystem.set("objectId", "testObjectId", {silent: true});
                        runs(function() {
                            fileDisplayHandler.display({
                                saveAsHistory: true,
                                path: "testPath",
                                type: "customView"
                            });
                            setTimeout(function() {
                                isWaiting = false;
                            }, 1500); // we need to wait long because code has nested timeouts of 500, 500 and 100 ms
                        });

                        waitsFor(function () {
                            return !isWaiting;
                        }, 5000);

                        runs(function() {
                            expect(saveAsHistory_actual).toBeTruthy();
                            expect(objectId_actual).toBe("testObjectId");
                        });
                    });

                    afterEach(function ()
                    {
                        spy.reset();
                    });

                });
            });
    describe("fileHandlerTestFailed", function ()
    {
        it("fileHandlerTest did not run", function ()
        {
            expect(testRunSuccess).toBeTruthy();
        });
    });
})();

