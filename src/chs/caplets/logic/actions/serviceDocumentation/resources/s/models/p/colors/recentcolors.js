/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
define(
    ["preferences"],
    function (preferences) {
        var cookie = Utils.readCookie("recent-colors");
        var colors = (cookie && cookie.split("|")) || [];

        preferences.on("change:background-color", function (model, currentValue) {
            var color = model.previous("background-color");
            if (color && colors.indexOf(color) === -1) {
                colors.unshift(color);
            }

            var currentValueIndex = colors.indexOf(currentValue);
            if (currentValueIndex !== -1) {
                colors.splice(currentValueIndex, 1);
            }

            if (colors.length > 10) {
                colors.length = 10;
            }
            Utils.createCookie("recent-colors", colors.join("|"), Utils.getCookiesDuration());
        });

        return colors;
    }
);