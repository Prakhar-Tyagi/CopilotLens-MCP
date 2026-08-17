/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/** globals createContext, describe */
(function () {
    "use strict";

    var context,
        preferences,
        stubs;

    preferences = new Backbone.Model();
    stubs = {
        preferences: preferences
    }

    context = createContext(stubs);

    context(
        ["models/p/colors/recentcolors"],
        function (recentColors) {
            describe("recentcolors", function () {
                beforeEach(function () {
                    preferences.clear();
                    recentColors.length = 0;
                });

                it("should store the previous color.", function () {
                    preferences.set("background-color", "red");
                    preferences.set("background-color", "white");

                    expect(recentColors.toString()).toBe("red");
                });

                it("should not store the current color if it was used recently.", function () {
                    preferences.set("background-color", "red");
                    preferences.set("background-color", "white");
                    preferences.set("background-color", "red");

                    expect(recentColors.toString()).toBe("white");
                });

                it("should not store more than 10 colors", function () {
                    preferences.set("background-color", "red");
                    recentColors.push("A", "B", "C", "D", "E", "F", "G", "H", "I", "J");
                    preferences.set("background-color", "white");

                    expect(recentColors.toString()).toBe("red,A,B,C,D,E,F,G,H,I");
                });
                it("should use default cookie duration when window.mentor is undefined", function () {
                    var original=window.mentor;
                    window.mentor = undefined;
                    preferences.set("background-color", "red");
                    spyOn(Utils, 'createCookie');
                    preferences.set("background-color", "blue");
                    expect(Utils.createCookie).toHaveBeenCalledWith("recent-colors","red",365);
                    window.mentor=original;
                });
                it("should use default cookie duration when serverConfig is missing", function () {
                    var original=window.mentor;
                    window.mentor = {publisher:{}};
                    preferences.set("background-color", "red");
                    spyOn(Utils, 'createCookie');
                    preferences.set("background-color", "blue");
                    expect(Utils.createCookie).toHaveBeenCalledWith("recent-colors","red",365);
                    window.mentor=original;
                });
            })
        },
        function (err) {
            describe("recentcolors", function ()
            {
                it("failed to load", function ()
                {
                    expect(err).toBeUndefined();
                });
            });
        }
    );
})();