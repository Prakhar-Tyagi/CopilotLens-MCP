/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, ?SISW?), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer?s 
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */
require(["preferences",], function (Preferences) {
    "use strict";

    describe("Preferences Test", function () {
        it("should use default cookie duration when language changes", function () {
            spyOn(Utils, "createCookie");
            Preferences.set("language", "JP");
            expect(Utils.createCookie).toHaveBeenCalledWith("language", "JP", 365);
            Preferences.unset("language");
        });

        it("should use default cookie duration when background-color changes", function () {
            spyOn(Utils, "createCookie");
            Preferences.set("background-color", "red");
            expect(Utils.createCookie).toHaveBeenCalledWith("background-color", "red", 365);
            Preferences.unset("background-color");
        });

        it("should use default cookie duration when systemScope changes", function () {
            spyOn(Utils, "createCookie");
            Preferences.set("systemScope", "designs1");
            expect(Utils.createCookie).toHaveBeenCalledWith("systemScope", "designs1", 365);
            Preferences.unset("systemScope");
        });
    });
});



