/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
require(["illustrator/views/popout/harnessDiagramPanel",
            "views/contentpanel/HarnessDesignPanel"],
        function (harnessDiagramPanel, BaseHarnessDiagramPanel)
        {
            describe("IllustratorHarnessDiagramPanelTest", function ()
            {
                it("should be able to load harnessDiagramPanel", function ()
                {
                    expect(harnessDiagramPanel).toBeDefined();
                });
                it("should be derivative of harnessDiagramPanel", function ()
                {
                    expect(harnessDiagramPanel instanceof BaseHarnessDiagramPanel).toBeTruthy();
                });

                it("should return doc title with name+revision+partnumber", function ()
                {
                    var title = harnessDiagramPanel.getDocumentTitle({
                        getNameWithPartNumberAndRevision: function ()
                        {
                            return "name-partnumber-revision";
                        }
                    });
                    expect(title).toBe("name-partnumber-revision");
                });

                it("should show diagram title in heading", function ()
                {
                    harnessDiagramPanel.getSystemData = function ()
                    {
                        return {
                            get: function (title)
                            {
                                if (title === 'title') {
                                    return 'digarm-title';
                                }
                            },
                            set: function noop() {
                                console.log("dummy handler for harnessDiagramPanel.getSystemData().set() @ harnessDiagramPanelTest");
                            }
                        }
                    };
                    harnessDiagramPanel.getDocumentSet = function ()
                    {
                        return {
                            getNameWithPartNumberAndRevision: function ()
                            {
                                return "name-partnumber-revision";
                            }
                        }
                    };
                    var title = harnessDiagramPanel.getTitle();
                    expect(title).toBe("digarm-title");
                });

            });

        }, function (err)
        {
            describe("harnessDiagramPanel loading failed", function ()
            {
                it("should load harness diagram panel", function ()
                {
                    expect(false).toBeTruthy();
                });
            });
        });