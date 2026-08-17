/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/* global Backbone, createContext, $, _, Backbone */
(function () {
    "use strict";

    var context, eventDispatcher, stubs, invoked;

    eventDispatcher = mentor.publisher.eventDispatcher;
    stubs = {
        jquery: $,
        underscore: _,
        backbone: Backbone,
        "fileDisplayHandler": {},
    };

    context = createContext(stubs);

    context(["SignalTracerModel"], function (model) {
        describe("SignalTracerModelTest", function () {
            beforeEach(function () {
                mentor.publisher.eventDispatcher = {
                    attachEventListener: function() {
                        invoked = true;
                    }
                }
            });

            it("should attach once", function () {
                model.addEventHandlers();
                invoked = false;
                model.addEventHandlers();
                expect(invoked).toBeFalsy();
            });

            it("should update the connectivityUID, designID and the signal trace files", function () {
                var connectivityUID="testConnID",
                    designID="designID",
                    signalTraceFiles={
                        signalTraceFile: {
                            name: "testFile",
                            contents: {}
                        },
                        fullInstanceFile: {
                            name: "testFile",
                            contents: {}
                        }
                    },
                    origRender=model.render
                ;
                model.altClickRender=true;
                model.render=function () {};
                model.update(signalTraceFiles, connectivityUID, designID);
                expect(model.connectivityUID).toBe(connectivityUID);
                expect(model.designID).toBe(designID);
                model.altClickRender=false;
                model.render=origRender;
            });

            it("should update the connectivityUID, designID and the signal trace files", function () {
                var connectivityUID="testConnID",
                    designID="designID",
                    signalTraceFiles={
                        signalTraceFile: {
                            name: "testFile",
                            contents: {}
                        },
                        fullInstanceFile: {
                            name: "testFile",
                            contents: {}
                        }
                    };
                model.update(signalTraceFiles, connectivityUID, designID);
                expect(model.connectivityUID).toBe(connectivityUID);
                expect(model.designID).toBe(designID);
            });

            it("should be able to render", function () {
                model.render(true);
                model.rendererLicenceAvaialable=function () {return true;};
                var origDisplayConnectivity=displayConnectivity;
                displayConnectivity=function () {};

                model.render(true);
                expect(model.flushConnectivity).toBeTruthy();

                displayConnectivity=origDisplayConnectivity;
            });

            it("should be able to reset variables", function () {
                model.reset();
                expect(model.connectivityFilePath).toBe('');
                expect(model.title).toBe('');
                expect(model.flush).toBeTruthy();
                expect(model.altClickRender).toBeFalsy();
                expect(model.altClickPop).toBeFalsy();
            });

            it("should be able to return true if the ground ST is avaialble", function () {
                expect(model.groundSTLicenceAvaialable()).toBeFalsy();
                var origUtils=Utils;
                Utils={
                    getUrlParameter: function (param) {
                        if(param==="uri") return "testUri";
                        else return 'yes';
                    }
                }
                expect(model.groundSTLicenceAvaialable()).toBeTruthy();
                Utils=origUtils;
            });

            it("should be able to check if the renderer licence is available", function () {
                spyOn(mentor.publisher.eventDispatcher, "attachEventListener");
                spyOn(Utils, "prepareFilePath");
                model.checkRendererAvailablility();
                expect(mentor.publisher.eventDispatcher.attachEventListener).toHaveBeenCalled();
                expect(Utils.prepareFilePath).toHaveBeenCalled();
            });

            it("should be able to check if the ground ST is available", function () {
                spyOn(model, "checkURL");
                spyOn(Utils, "prepareFilePath");
                model.checkGroundSTAvailablility();
                expect(model.checkURL).toHaveBeenCalled();
                expect(Utils.prepareFilePath).toHaveBeenCalled();
            });

            it("should be able to check if the ground ST is available", function () {
                spyOn(model, "checkURL");
                spyOn(Utils, "prepareFilePath");
                model.checkGroundSTAvailablility();
                expect(model.checkURL).toHaveBeenCalled();
                expect(Utils.prepareFilePath).toHaveBeenCalled();
            });

            it("should be able to get connectivityUID and return true if the fullInstance is clicked, and return true if the action is to be rendered", function () {
                var origFullInstanceTitle=mentor.publisher.constants.FullInstanceTitle;
                mentor.publisher.constants.FullInstanceTitle=model.getTitle();
                expect(model.getConnectivityUID()).toBe('testConnID');
                expect(model.isFullInstanceClicked()).toBeTruthy();
                expect(model.isRenderAction()).toBeFalsy();
                mentor.publisher.constants.FullInstanceTitle=origFullInstanceTitle;
            });

            it("should be able to update data", function() {
                var origLoad=mentor.publisher.objectDataLoader.load,
                    origUpdate=model.update;
                model.update=function (file, objID, sysID) {};
                mentor.publisher.objectDataLoader.load=function (sysID, objID, projID) {
                    return {
                        getSignalTraceFiles: function () {return {}}
                    };
                };
                spyOn(model, "update");
                model.updateData("testSystemID", "testObjectID");
                expect(model.update).toHaveBeenCalled();
            });


            xit("should be able to show rendered diagram", function () {
                var origHref=window.location.href,
                    origMentorDisplay=mentor.publisher.fileDisplayHandler.display,
                    evt={
                        detail:{
                            mainText: "testMainText",
                            path: "testPath",
                        }
                    },
                    origWindowOpener=window.opener;
                window.opener={
                    mentor: {
                        publisher: {
                            fileDisplayHandler: {
                                display: function (cnt) {},
                            }
                        }
                    }
                }
                window.location.href="testIndex/renderSignal";
                mentor.publisher.fileDisplayHandler.display=function (cnt) {};

                model.showRenderedDiagram(evt);

                window.location.href=origHref;
                mentor.publisher.fileDisplayHandler.display=origMentorDisplay;
                window.opener=origWindowOpener;
            });

            afterEach(function () {
                mentor.publisher.eventDispatcher = eventDispatcher;
            });
        });
    });
})();
