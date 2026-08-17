/* globals  */
define(
    ["preferences"],
    function (preferences)
    {
        "use strict";

        function computeBackgroundColor() {
            var color = preferences.get("background-color");
            if (color) {
                return color;
            }

            return mentor.publisher.colors["svg-background-color"] || "";
        }

        function customizeBackground(dom) {
            var color = computeBackgroundColor();

            var $dom = $(dom);
            $dom.css("background-color", color);
            $(".capital-background-fill", $dom).css("fill", color);
        }

        return {
            customizeBackground: customizeBackground
        }
    }
);