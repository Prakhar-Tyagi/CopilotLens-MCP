/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, window, require, describe*/
(function () {
    "use strict";

    var stubs = {
        jquery: $,
        underscore: _,
        backbone: Backbone,
    };
    var context = createContext(stubs);

    context(["models/faceviews"], function (faceViews) {
        "use strict";
        describe("faceViewsModelTest", function () {
            it("should get face views", function () {
                var faceviewData={
                    objectId: "testObjectId",
                    systemId: "testSystemId",
                    "multiple-faceview-support": true,
                }, origLoadObjectData=mentor.publisher.project.loadObjectData;

                mentor.publisher.project.loadObjectData = function (objectId, systemId) {
                    return {
                        getFaceviews: function () {
                            return [{
                                name: "testName",
                                id: "testId",
                            }];
                        }
                    };
                };
                mentor.publisher.router={
                    getViewObjectForType: function (content, viewName, prop, bool) {
                        return {
                            get: function (prop) {
                                return "testId";
                            }
                        };
                    }
                };

                expect(faceViews.getFaceViewFor({})).toEqual({});

                faceViews.getFaceViewFor({objectId: "testObjectId", systemId: "testSystemId"});
                faceViews.getFaceViewFor(faceviewData);
                mentor.publisher.project.loadObjectData = origLoadObjectData;
                mentor.publisher.router=undefined;
            });

            it("should get all views ", function () {
                var Model = Backbone.Model.extend();
                var faceview = new Model({
                    "multiple-faceview-support": true,
                    view: "noViewSpecified",
                    id: "testId",
                    path: "testPath"}
                );
                expect(faceViews.getAllViews([faceview])).toEqual([{ mainText : 'Translated', id : 'testId', path : 'testPath' }]);
            });
        });
    },
        function (err) {
        describe("faceViewModelTest - module load Error", function () {
            it("Module load failed", function () {
                console.log(err);
                expect(false).toBeTruthy();
            });
        });
    });
})();
