/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, �SISW�), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer�s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, window, require, describe, expect, it, listTemplateForTest, afterEach, createContext*/
(function () {
    "use strict";
    var mockPack = new (Backbone.Model.extend())(), context, stubs, xrefContent;
    var customDataCollection = new (Backbone.Collection.extend())();
    stubs = {
        currentPackage : mockPack,
        jquery : $,
        underscore : _,
        backbone : Backbone,
        CustomDataCollection : customDataCollection,
        fileDisplayHandler : {
            display : function (content) {
                xrefContent = content;
            }
        }
    };
    context = createContext(stubs);

    context(['views/p/customDataView', "models/selectedSystem"], function (customDataView, selectedSystem) {
        describe("customDataViewTest", function () {

            beforeEach(function () {
                stubs.currentPackage.set("id", "projectId");
                selectedSystem.set("systemId", "testSystemId");
            });

            it("should be able to load customDataView Module", function () {
                expect(customDataView).toBeDefined();
            });

            it("should return the correct customDataView Title", function () {
                expect(customDataView.getTitle()).toBe("Custom Data");
            });

            it("should return the correct customDataView ClassName", function () {
                expect(customDataView.getClassName()).toBe("customData");
            });

            it("should be able to create correct url for system report such as highway report", function () {
                expect(customDataView.createURL({objectReport : "report", mainText : "highwayReport", path : "filePath"})).toBe("popout.html#/report/highwayReport/testSystemId/projectId/filePath");
            });

            it("should be able to create correct url for a custom file", function () {
                expect(customDataView.createURL({ mainText : "customFIle1", path : "filePath"})).toBe("popout.html#/customFile/customFIle1/projectId/filePath");
            });

            it("should be able to create correct content for a custom file", function () {
                expect(JSON.stringify(customDataView.extractContent({ mainText : "customFIle1", path : "filePath"}))).toBe('{"mainText":"customFIle1","path":"filePath"}');
            });

            it("should be able to create correct content for system report such as highway report", function () {
                expect(JSON.stringify(customDataView.extractContent({objectReport : "report", mainText : "highwayReport", path : "filePath"}))).toBe('{"path":"filePath","systemId":"testSystemId","reset":false,"type":"systemReport","title":"highwayReport"}');
            });

            it("should be able to get the correct model", function () {
                expect(JSON.stringify(customDataView.getModel())).toBe('{"title":"Custom Data","showTitle":true,"listItems":[],"className":"customData","showPopup":true}');
            });

            it("should be able to pop out", function () {
                var evt = {
                        stopPropagation: function () {},
                    },
                    origCreateURL=customDataView.createURL,
                    origOpenPopout=customDataView.openPopout
                ;
                spyOn(evt, "stopPropagation").andCallThrough();
                customDataView.openPopout=function (url) {};
                customDataCollection.get= function (cid) {
                    return {
                        get: function (param) {
                            return [
                                {
                                    path:'/testPath'
                                },
                            ];
                        }
                    }
                };
                customDataCollection.findDataContent = function (listContent, dataId) {
                    return {
                        path: '/testPath',
                    }
                }
                spyOn(customDataView, "openPopout").andCallThrough();
                customDataView.popOut(evt);
                expect(customDataView.openPopout).toHaveBeenCalledWith('/testPath');
                expect(evt.stopPropagation).toHaveBeenCalled();

                customDataView.createURL=function (content) {
                    return '/testUrl';
                };
                customDataCollection.get= function (cid) {
                    return {
                        get: function (param) {
                            return [
                                {
                                    path: 'testPath'
                                },
                            ];
                        }
                    }
                };
                customDataView.popOut(evt);
                expect(customDataView.openPopout).toHaveBeenCalledWith('/testPath');
                expect(evt.stopPropagation).toHaveBeenCalled();

                customDataView.createURL=function (content) {
                    return '';
                };
                customDataCollection.get= function (cid) {
                    return {
                        get: function (param) {
                            return [
                                {
                                    path: 'testPath',
                                },
                            ];
                        }
                    }
                };
                spyOn(customDataView, "extractContent").andCallThrough();
                customDataView.popOut(evt);
                expect(customDataView.extractContent).toHaveBeenCalled();
                expect(evt.stopPropagation).toHaveBeenCalled();

                customDataView.createURL=origCreateURL;
                customDataView.openPopout=origOpenPopout;
            });

            it("should be able to pop over the clicked item", function () {
                var evt = {},
                    origExtractContent=customDataView.extractContent,
                    origOpenPopout=customDataView.openPopout,
                    origDisplayContent=customDataView.displayContent
                ;

                customDataView.extractContent=function (url) {
                    return {
                        path: '/testPath',
                    }
                };
                customDataCollection.get= function (cid) {
                    return {
                        get: function (param) {
                            return [
                                {
                                    path:'/testPath'
                                },
                            ];
                        }
                    }
                };
                customDataView.openPopout=function (url) {};
                customDataView.popoverItemClicked(evt);

                customDataView.extractContent=function (url) {
                    return {
                        path: '',
                    }
                };
                customDataView.displayContent=function () {};
                customDataCollection.get= function (cid) {
                    return {
                        get: function (param) {
                            return [
                                {
                                    path:'/testPath'
                                },
                            ];
                        }
                    }
                };
                customDataView.popoverItemClicked(evt);

                customDataView.openPopout=origOpenPopout;
                customDataView.displayContent=origDisplayContent;
                customDataView.extractContent=origExtractContent;
            });

            afterEach(function () {
                selectedSystem.clear({silent : true});
            });

        });
    });
})();