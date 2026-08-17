/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
require(["illustrator/collection/reports/reportsList", "SectionCollection", "currentPackage"],
        function (reportsList, SectionCollection, currentPackage)
        {
            var fetchCalled;
            describe("reportsListTest", function ()
            {
                beforeEach(function ()
                {
                    reportsList.dataLoader = {
                        load: function (type, id)
                        {
                            expect(type).toBe("Reports");
                            expect(id).toBe("id");
                            return ["bundles.json"];
                        }
                    }

                    reportsList.fetch = function ()
                    {
                        fetchCalled = true;
                    };
                });
                afterEach(function ()
                {
                    fetchCalled = false;
                });
                it("should be able to load reportsList module", function ()
                {
                    expect(reportsList).toBeDefined();
                });
                it("should be able to load all comparision reports", function ()
                {
                    var reports = reportsList.getData({
                        getId: function ()
                        {
                            return "id";
                        }
                    });
                    expect(reports[0]).toBe("bundles.json");
                });

                it("should be of type BaseCollection", function ()
                {
                    expect(reportsList instanceof SectionCollection).toBeTruthy();
                });

            });

        }
        , function (err)
        {
            describe("reportsList loading failed", function ()
            {
                it("should load module", function ()
                {
                    expect(false).toBeTruthy();
                });
            });
        });