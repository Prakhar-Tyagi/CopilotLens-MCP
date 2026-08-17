/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
require(["illustrator/views/layoutManager"], function (layoutManager)
{
    describe("LayoutManagerTest", function ()
    {
        var elements;
        var verticalBarShown;
        var horizontalBarShown, getSplitPaneDimensionCalled;
        beforeEach(function ()
        {
            elements = {
                "#splitter1": {},
                "#splitter2": {},
                "#splitter3": {}
            };

            verticalBarShown = false;
            horizontalBarShown = false;
            getSplitPaneDimensionCalled = false;
            layoutManager.domquerylib = function (query)
            {
                var ele = elements[query];
                return {
                    height: function (h)
                    {
                        ele["height"] = h;
                        return this;
                    },
                    width: function (w)
                    {
                        ele["width"] = w;
                        return this;

                    },
                    show: function ()
                    {
                        ele["show"] = true;
                        return this;
                    }
                };
            };
            layoutManager.getHorizontalBar = function ()
            {
                return {
                    show: function ()
                    {
                        horizontalBarShown = true;
                    }
                };

            };
            layoutManager.getVerticalBar = function ()
            {
                return {
                    show: function ()
                    {
                        verticalBarShown = true;
                    },
                    hide: function ()
                    {
                        verticalBarShown = false;
                    }
                };

            };
            layoutManager.getContentPanel = function ()
            {
                return {
                    getBoundingClientRect: function ()
                    {
                        return {
                            width: 100,
                            height: 100
                        }
                    }
                };

            };
            layoutManager.getTotalSizeAvailable = function () {
                return {
                    width: 100,
                    height: 100
                }
            };
            layoutManager.getSplitPaneDimension = function (totalSize) {
                getSplitPaneDimension = true;
                return {"topSectionHeight": 50, "bottomSectionHeight": 50};
            };
            layoutManager.contentPanel = {
                showVerticalBar: function ()
                {
                    verticalBarShown = true;
                    return {
                        height: function ()
                        {
                            return this;
                        },
                        width: function ()
                        {
                            return this;
                        },
                        show: function ()
                        {
                            verticalBarShown = true;
                        },
                        hide: function ()
                        {

                        },
                        width: function ()
                        {
                            return 1;
                        }
                    }
                },
                getDimensions: function ()
                {
                    return {
                        width: 100,
                        height: 100
                    }
                }
            }
        });

        it("should be able to load layout manager", function ()
        {
            expect(layoutManager).toBeDefined();
        });
        it("should be able to set dimention of all three content panels", function ()
        {
            var d = layoutManager.openThirdDetailPanel("Reports");
            expect(JSON.stringify(d)).toBe('{"panelId":"splitter2","panelToSplit":"splitter1"}');
            expect(verticalBarShown).toBeTruthy();
            expect(horizontalBarShown).toBeTruthy();
            expect(getSplitPaneDimension).toBeTruthy();
            expect(JSON.stringify(elements)).toBe(
                    '{"#splitter1":{"height":50,"width":50,"show":true},"#splitter2":' +
                    '{"height":50,"width":50,"show":true},' +
                    '"#splitter3":{"height":50,"width":"100%","show":true}}');
        });
        it("should be able to open two panels one on top of other", function ()
        {
            var d = layoutManager.openSecondDetailPanel(mentor.publisher.contentType.OLD_DESIGN_REVISION, {
                splitter3: true,
            });
            expect(JSON.stringify(d)).toBe('{"splitter1":{"height":49,"width":"100%"},"splitter2":{"height":49,"width":"100%"},"splitter3":{"height":49,"width":"100%"}}');
            expect(verticalBarShown).toBeFalsy();
            expect(horizontalBarShown).toBeTruthy();

        });

        it("should be able to open two panels side by side", function ()
        {
            var d = layoutManager.openSecondDetailPanel(mentor.publisher.contentType.OLD_DESIGN_REVISION, {
                splitter2: true,
            });
            expect(JSON.stringify(d)).toBe('{"splitter1":{"height":"100%","width":49},"splitter2":{"height":"100%","width":49}}');
            expect(verticalBarShown).toBeTruthy();
            expect(horizontalBarShown).toBeFalsy();

        });

    });

}, function (err)
{
    describe("LayoutManagerTestFailed", function ()
    {
        it("failed to load module Layout manager for illustrator", function ()
        {
            expect(false).toBeTruthy();

        });

    });
});