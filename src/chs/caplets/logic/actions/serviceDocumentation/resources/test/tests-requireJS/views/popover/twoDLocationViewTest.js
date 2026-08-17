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
    var mockPack = new (Backbone.Model.extend())(), context, stubs, xrefContent, originalGetPluginType;

    stubs = {
        currentPackage : mockPack,
        jquery : $,
        underscore : _,
        backbone : Backbone,
        TwoDLocationCollection : new (Backbone.Collection.extend())(),
        fileDisplayHandler : {
            display : function (content) {
                xrefContent = content;
            }
        }
    };
    context = createContext(stubs);

    context(['views/p/twoDLocationView'], function (twoDLocationView) {
        describe("twoDLocationViewTest", function () {
            beforeEach(function () {
                originalGetPluginType = window.getPluginType;
                window.getPluginType = function (path) {
                    if(path.indexOf(".pdf") > 0) {
                        return "application/pdf";
                    }
                    return "text/svg";
                };

                stubs.currentPackage.set("id", "projectId");
            });

            it("should be able to load twoDLocationView Module", function () {
                expect(twoDLocationView).toBeDefined();
            });

            it("it should be able to open a twoD location in popout", function () {
                var twoD = new (Backbone.Model.extend())(), popoutURL;
                twoD.set({mainText : "topView", objectId : "objectId"});
                popoutURL = twoDLocationView.createURL(twoD);
                expect(popoutURL).toBe("popout.html#/showLocation/topView/projectId/objectId");
            });

            it("it should be able to open a pdf twoD location in popout", function () {
                var twoD = new (Backbone.Model.extend())(), popoutURL;
                twoD.set({mainText : "topView", objectId : "objectId", path:"test.pdf"});
                popoutURL = twoDLocationView.createURL(twoD);
                expect(popoutURL).toBe("popout.html#/customFile/topView/projectId/test.pdf");
            });

            it("should be able to display a twoD View", function () {
                var systemURL, displayedContent, twodModel = new (Backbone.Model.extend())(), systemData = new (Backbone.Model.extend())(), stopEvent, resetContentPanel;
                twoDLocationView.getWindowObj = function () {
                    return {
                        mentor : {
                            publisher : {
                                selectedSystem : systemData
                            }
                        }
                    };
                };

                twodModel.set("objectId", "testObjectId");
                twodModel.id = "testMainText";
                stubs.TwoDLocationCollection.get = function () {
                    return twodModel;
                };
                twoDLocationView.getData = function () {
                    return stubs.TwoDLocationCollection;
                };
                displayedContent = twoDLocationView.getItemContent("testMainText");
                expect(systemData.get("objectId")).toBe("testObjectId");
                expect(JSON.stringify(displayedContent)).toBe('{"id":"testMainText","mainText":"testMainText","type":"locationviews","reset":false,"path":""}');
            });

            it("should return the Title", function () {
                expect(twoDLocationView.getTitle()).toBe("TwoDLocationViewTitle");
            });

            it("should return the ClassName", function () {
                expect(twoDLocationView.getClassName()).toBe("2dLocations");
            });

            it("should be able to display content", function ( ) {
                var windowObj={
                        mentor: {
                            publisher: {
                                fileDisplayHandler:{
                                    display: function (params) {}
                                }
                            }
                        }
                    },
                    origGetWindowObj=twoDLocationView.getWindowObj                ;

                twoDLocationView.getWindowObj=function () {return windowObj};

                spyOn(windowObj.mentor.publisher.fileDisplayHandler, "display");
                twoDLocationView.displayContent({path: "testPath/sampleFile.pdf"});
                expect(windowObj.mentor.publisher.fileDisplayHandler.display).toHaveBeenCalled();

                twoDLocationView.getWindowObj=origGetWindowObj;
            });

            it("should be able to filter items", function () {
                expect(twoDLocationView.filter(["testItems1", "testItems2"])).toEqual(["testItems1", "testItems2"]);
            });

            afterEach(function () {
                window.getPluginType = originalGetPluginType;
            });
        });
    });
})();