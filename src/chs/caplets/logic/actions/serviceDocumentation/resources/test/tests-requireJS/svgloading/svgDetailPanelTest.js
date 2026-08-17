/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

describe("svgLoaderTest", function () {
    "use strict"
    var dataLoader, orgUtils, isIE;

    beforeEach(function () {
        dataLoader = mentor.publisher.dataLoader;

        mentor.publisher.dataLoader = {
            createOptionExpressionMap: function () {

            }
        }

    });

    it("should not filter twoD location views", function () {
        var svgLoader = mentor.publisher.svgLoader();
        expect(svgLoader.filterSVGByOptions("op1", new TwoDSVGEventHandler(), "svg")).toBeFalsy();
    });
    it("should  filter system SVGs location views", function () {
        var svgLoader = mentor.publisher.svgLoader();
        expect(svgLoader.filterSVGByOptions("op1", new SVGEventHandler(), "svg")).toBeTruthy();
    });

    function getMockScriptEle(callback)
    {
        return {
            setAttributeNS: callback
        };
    }

    function getMockSVGEle()
    {
        return {
            removeAttributeNS: function () {

            },
            setAttributeNS: function () {

            }
        };
    }

    it("should  be able to get svgPan.js absolute path using url location path name", function () {
        var svgLoader = mentor.publisher.svgLoader();
        var input = ['/', '/index.html', '/packets/index.html', '/packets', "/packets/", "packets"];
        var expected = ['/s/SVGPan.js', '/s/SVGPan.js', '/packets/s/SVGPan.js', '/packets/s/SVGPan.js',
            '/packets/s/SVGPan.js', '/packets/s/SVGPan.js'];

        for (var i = 0; i < input.length; i++) {
            var libPath;
            svgLoader.loadZoomPanLibUsingPathName(input[i], getMockScriptEle(function (namespace, ele, scriptPath) {
                libPath = scriptPath;
            }), getMockSVGEle(), false);
            expect(libPath).toBe(expected[i]);
        }
    });

    afterEach(function () {
        mentor.publisher.dataLoader = dataLoader;
    });

});
