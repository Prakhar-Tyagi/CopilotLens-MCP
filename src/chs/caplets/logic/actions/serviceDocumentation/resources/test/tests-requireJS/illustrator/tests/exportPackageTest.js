/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

require(["illustrator/exportPackage", "preferences"], function (systemUnderTest, preferences) {
    "use strict";

    describe("ExportPackageTest", function () {
        var triggeredUrl;
        var originalOpen = window.open;
        var originalBgColor = preferences.get("background-color");

        function fakeWindowOpen(url)
        {
            triggeredUrl = url;
        }

        beforeEach(function () {
            window.open = fakeWindowOpen;
        });

        afterEach(function () {
            window.open = originalOpen;
            preferences.set("background-color", originalBgColor, {silent: true});
        });

        it("module should load", function () {
            expect(systemUnderTest).toBeDefined();
        });

        it("should generate url only for defined params", function () {
            systemUnderTest.export({
                packageId: "data\\db23",
                language: "en"
            });

            expect(triggeredUrl).toBe(
                    'export?packageId=db23&language=en&extension=xls&modifiedTableColumnColor=rgba(0%2C%200%2C%200%2C%200)&fontName=Times%20New%20Roman');
        });

        it("should generate url using preferences, if available", function () {
            preferences.set("background-color", "rgb(217, 227, 243)", {silent: true});
            systemUnderTest.export({
                packageId: "data\\db23",
                language: "jp"
            });

            expect(triggeredUrl).toBe(
                    'export?packageId=db23&language=jp&extension=xls&modifiedTableColumnColor=rgba(0%2C%200%2C%200%2C%200)&bgColor=rgb(217%2C%20227%2C%20243)&fontName=Times%20New%20Roman');
        });
    });
}, function (err) {
    describe("ExportPackageTest - module load Error", function () {
        it("Module load failed", function () {
            console.log(err.message + "::\n" + err.stack);
            expect(false).toBeTruthy();
        });
    });
});

