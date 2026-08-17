/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
require(["illustrator/views/reports/reportsList"],
        function (viewUnderTest)
        {
            "use strict";
            describe("reportsListViewTest", function ()
            {

                it("should be able to load reports view", function ()
                {
                    expect(viewUnderTest).toBeTruthy();
                });
                function createTestReportContent()
                {
                    var content = {
                        mainText: "bundles",
                        path: "reports/bundle.json"
                    };
                    return content;
                }

                function createTestConfig()
                {
                    return {
                        id: "testProjectId",
                    };
                }

                it("should be able to open report in popout", function ()
                {
                    var content = createTestReportContent();
                    var config = createTestConfig();
                    var popoutDisplayed = false;
                    config.popuphandler = {
                        openPopout: function (url)
                        {
                            popoutDisplayed = true;
                            expect(url).toBe(
                                    'popout.html#/document/ChangeReport/bundles/testProjectId/searchText/reports/bundle.json');
                        }
                    };
                    viewUnderTest.openPopout(content, config);
                    expect(popoutDisplayed).toBeTruthy();
                });

                it("clicking list item should open report", function ()
                {
                    var reportOpenedInSplitPane = false;
                    var config = {
                        fileDisplayHandler: {
                            display: function (content)
                            {
                                reportOpenedInSplitPane = true;
                                expect(JSON.stringify(content)).toBe('{"mainText":"bundles-report"}');
                            }
                        }
                    };
                    var evt = createTestEvent();
                    var preCD = viewUnderTest.getClickedData;
                    viewUnderTest.getClickedData = function (e)
                    {
                        //expect(evt).toBe(e);
                        return {
                            mainText: "bundles-report"
                        };
                    }
                    viewUnderTest.clicked(evt, config);
                    expect(reportOpenedInSplitPane).toBeTruthy();
                    viewUnderTest.getClickedData = preCD;
                });
                function createTestEvent()
                {
                    return {
                        stopPropagation: function ()
                        {

                        },
                        target: {
                            parent: function ()
                            {
                                return {
                                    attr: function (name)
                                    {
                                        expect(name).toBe('data-id');
                                        return 'test-id';
                                    }
                                }
                            }
                        }
                    };
                }

                it("should be able to open the report in popout when popout button is clicked", function ()
                {
                    var evt = createTestEvent();
                    var reportOpened = false;
                    var config = {
                        id:"test-package",
                        domquerylib: function (selector)
                        {
                            expect(selector).toBe(evt.target);
                            return evt.target;
                        },

                        popuphandler: {
                            openPopout: function (url) {
                                expect(url).toBe(
                                        "popout.html#/document/ChangeReport/mainText-value/test-package/searchText/path-value");
                                reportOpened = true;
                            }
                        },

                        report: function (id) {
                            expect(id).toBe('test-id');
                            return {
                                get: function (attr)
                                {
                                    return attr + "-value";
                                }
                            };
                        }
                    };
                    viewUnderTest.popOut(evt, config);
                    expect(reportOpened).toBeTruthy();
                });

                it("should be able to render report in split panel", function ()
                {
                    var content = createTestReportContent();
                    var config = createTestConfig();
                    var viewRendered = false;
                    var testView = function (container, data)
                    {
                        expect(data.mainText).toBe("bundles");
                        expect(container).toBe("#splitter3");
                        var View = Backbone.View.extend({
                            render: function ()
                            {
                                viewRendered = true;
                            }
                        });
                        return new View();
                    };
                    config.moduleloader = function (deps, callback)
                    {
                        expect(deps[0]).toBe("illustrator/views/reports/changeReport");
                        callback(testView);
                    };
                    config.dataloader = {
                        loadFile: function (url, asyn, cache, type)
                        {
                            expect(url).toBe('reports/bundle.json');
                            expect(asyn).toBe(false);
                            expect(cache).toBe(false);
                            expect(type).toBe("json");
                            return {
                                data: {
                                    mainText: "bundle"
                                }
                            }
                        }
                    };
                    runs(function() {
                        viewUnderTest.openReport(content, config);
                    });

                    var waiting = true;
                    waitsFor(function() {
                        setTimeout(function() {
                            waiting = false;
                        }, 200);
                        return !waiting;
                    }, 1000);

                    runs(function() {
                        expect(viewRendered).toBeTruthy();
                    });
                });
            });

        }, function (err)
        {
            describe("reportsListViewTestFailed", function ()
            {
                it("failed to load reports view list", function ()
                {
                    expect(false).toBeTruthy();
                });
            });
        });