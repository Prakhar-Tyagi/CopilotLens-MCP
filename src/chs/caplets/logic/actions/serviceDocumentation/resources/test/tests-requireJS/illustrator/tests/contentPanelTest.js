/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
require(["illustrator/views/contentPanel",
            "illustrator/views/layoutManager"],
        function (contentPanel, layoutManager)
        {
            describe("illustratorContentPanelTest", function ()
            {
                it("should be able to load contentPanel module", function ()
                {
                    expect(contentPanel).toBeDefined();
                });
                it("should have new layout strategy", function ()
                {
                    var layoutSSet = false;
                    contentPanel.configureIllustrator({
                        layoutManager: {
                            setLayoutSplitter(layoutS) {
                                layoutSSet = true;
                                expect(layoutS).toBe(layoutManager);
                            }
                        }
                    });
                    expect(layoutSSet).toBeTruthy();

                });
                it("should  open both old and new design revision  on render", function ()
                {
                    var openHar = contentPanel.openHarnessDesign;
                    var openDesigns = [];
                    var designs = [{
                        mainText: "old"
                    }, {
                        mainText: "new"
                    }]
                    contentPanel.openHarnessDesign = function (har)
                    {
                        openDesigns.push(har.mainText);
                    };
                    contentPanel.openBothDesigns({
                        designs: designs
                    });
                    expect(JSON.stringify(openDesigns)).toBe('["old","new"]');

                    contentPanel.openHarnessDesign = openHar;

                });
                it("should  both designs and report on render", function ()
                {
                    var openHar = contentPanel.openBothDesigns;
                    var openRepo = contentPanel.openFirstReport;
                    var bothDesignsOpened = false;
                    var firstRepoortOpened = false;
                    contentPanel.openBothDesigns = function ()
                    {
                        bothDesignsOpened = true;
                    };
                    contentPanel.openFirstReport = function ()
                    {
                        firstRepoortOpened = true;
                    };
                    contentPanel.createLandingPage();
                    expect(bothDesignsOpened).toBeTruthy();
                    expect(firstRepoortOpened).toBeTruthy();

                    contentPanel.openFirstReport = openRepo;
                    contentPanel.openBothDesigns = openHar;
                });
                it("should  open first report on render", function ()
                {
                    var reportDisplayed = false;
                    var isWaiting = true;
                    var config = {
                        reports: [{
                            mainText: "first-report"
                        }, {
                            mainText: "second-report"
                        }],
                        fileDisplayHandler: {
                            display: function (content)
                            {
                                reportDisplayed = true;
                                expect(JSON.stringify(content)).toBe(
                                        '{"mainText":"first-report","type":"ChangeReport"}');
                            }
                        },
                        delay: 10
                    };

                    runs(function() {
                        contentPanel.openFirstReport(config);
                        setTimeout(function() {
                            isWaiting = false;
                        }, 100);
                    });

                    waitsFor(function() {
                        return !isWaiting;
                    }, 2000);

                    runs(function() {
                        expect(reportDisplayed).toBeTruthy();
                    });
                });
                it("should be able to open harness design", function ()
                {
                    var eventsRegistered;
                    var typeIsSetOnHarness;
                    var harnessDisplayed;
                    var harness = {
                        getContent: function ()
                        {
                            return {
                                mainText: "harness-design-old"
                            };
                        },
                        set: function (type, value, config)
                        {
                            typeIsSetOnHarness = true;
                            expect(type).toBe("designType");
                            expect(value).toBe("oldDesignRevision");
                            expect(JSON.stringify(config)).toBe('{"silent":true}');
                        }
                    };
                    var index = 0;
                    var config = {
                        delay: 10,
                        fileDisplayHandler: {
                            display: function (content)
                            {
                                harnessDisplayed = true;
                                expect(JSON.stringify(content)).toBe(
                                        '{"mainText":"harness-design-old","reset":false,"type":"oldDesignRevision","doNotSaveAsHistory":true}');
                            }
                        },
                        addDocumentEventListener: function ()
                        {
                            eventsRegistered = true;
                        }
                    };
                    var isWaiting = true;

                    runs(function() {
                        contentPanel.openHarnessDesign(harness, index, config);
                        setTimeout(function() {
                           isWaiting = false;
                        }, 100);
                    });

                    waitsFor(function() {
                        return !isWaiting;
                    }, 2000);

                    runs(function() {
                        expect(harnessDisplayed).toBeTruthy();
                        expect(typeIsSetOnHarness).toBeTruthy();
                        expect(eventsRegistered).toBeTruthy();
                    });
                });
                it("should close all panels on re-render and create again", function ()
                {
                    var createLandPage = contentPanel.createLandingPage;
                    var landPageCreated = false;
                    contentPanel.createLandingPage = function ()
                    {
                        landPageCreated = true;
                    };
                    var typesClosed = [];
                    var config = {
                        id: "test-id",
                        layoutManager: {
                            close: function (type)
                            {
                                typesClosed.push(type);
                            }
                        }
                    };

                    contentPanel.reRender(config);
                    expect(JSON.stringify(typesClosed)).toBe('["oldDesignRevision","newDesignRevision","customView"]');
                    expect(landPageCreated).toBeTruthy();
                    contentPanel.createLandingPage = createLandPage;
                });

                it("should render content in container using template", function ()
                {
                    var createLandPage = contentPanel.createLandingPage;
                    var landPageCreated = false;
                    contentPanel.createLandingPage = function ()
                    {
                        landPageCreated = true;
                    };
                    contentPanel.isContentAvailable = function ()
                    {
                        return true;
                    };
                    $("body").append("<div id='illustrator'></div>");
                    contentPanel.templateHTML = "dummy-template";
                    contentPanel.container = "#illustrator";

                    contentPanel.render();
                    expect($("#illustrator").html()).toBe('dummy-template');
                    expect(landPageCreated).toBeTruthy();
                    contentPanel.createLandingPage = createLandPage;
                    $("#illustrator").remove();
                });
            });

        }, function (err)
        {
            describe("illustratorContentPanelTestFailed", function ()
            {
                it("should be able to load contentPanel module", function ()
                {
                    expect(true).toBeFalsy();
                });
            });
        });