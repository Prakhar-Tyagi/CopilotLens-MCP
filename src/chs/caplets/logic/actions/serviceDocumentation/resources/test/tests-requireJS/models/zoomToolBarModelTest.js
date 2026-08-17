/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, window, require, describe*/
require(["ZoomToolBarModel"], function (zoomToolbarModel) {

    describe("ZoomToolbarModelTest", function () {
        it("should create a model for each container", function () {
            var notchParent = {};
            notchParent.position = function () {
                return {left : 1000};
            };
            notchParent.width = function () {
                return 200;
            };
            var notch = {};
            notch.width = function () {
                return 10;
            };
            zoomToolbarModel.createContainerModel("c1", notchParent.position(), notchParent.width(), notch.width());
            expect(zoomToolbarModel.get('c1').get('sliderWidth')).toBe(190);
            expect(zoomToolbarModel.get('c1').get('maxXValue')).toBe(1190);
        });
    });

    describe("ZoomToolbarModelTest", function () {
        it("should get the slider content", function () {
            zoomToolbarModel.get('c1').set('maxXValue', 1100);
            zoomToolbarModel.get('c1').set('sliderWidth', 100);
            var content = zoomToolbarModel.getSliderContent("c1", 10, 100, 10);
            expect(content.x).toBe(20);
            expect(content.zoom).toBe('200%');
        });
    });

    describe("ZoomToolbarModelTest", function () {
        it("should update location correctly", function () {
            zoomToolbarModel.updateLocation("c1", 10, 10, 100, 200);
            expect(zoomToolbarModel.get('c1').get('parentDimensions').x).toBe(10);
            expect(zoomToolbarModel.get('c1').get('parentDimensions').y).toBe(10);
            expect(zoomToolbarModel.get('c1').get('parentDimensions').width).toBe(100);
            expect(zoomToolbarModel.get('c1').get('parentDimensions').height).toBe(200);
        });
    });

    describe("ZoomToolbarModelTest", function () {
        it("should update zoom value correctly", function () {
            var oldResizeSVG = window.resizeSvg;
            window.resizeSvg = function () {
            };
            zoomToolbarModel.updateZoom("c1", "100%", 10);
            expect(zoomToolbarModel.get('c1').get('zoomFactor')).toBe(4.605039373300481);
            window.resizeSvg = oldResizeSVG;
        });
    });
});