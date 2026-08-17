/**
 * Created by kayyagar on 20-01-2016.
 */
define("ZoomAndPanModule", [
    'jquery',
    'underscore',
    'backbone'
], function ($, underscore, Backbone) {
    "use strict";
    var zoomToScale, autoFocusSVGElement, getAggregateBoundingBox, getViewPortBBox, lineElementPresentAsImmediateChild,
            svgContainerId,
            getGElementToFocus, getBBoxForLineElement, getCombinedBBox, autoZoomSVGElement, getFitScale, zoomFit,
            initialize, calculateScale,
            root, zoomfactor, viewport, viewBoxHeight, viewBoxWidth, clientWidth, clientHeight,
            getBBoxAfterTransformation,
            model, scale, calculateZoomLimit, zoomToMiddle;

    zoomToMiddle = function (zoomFactor) {
        var g = viewport;
        var p = root.createSVGPoint();
        p.x = clientWidth / 2;
        p.y = clientHeight / 2;
        //p.x = 0;
        //p.y = 0;
        p = p.matrixTransform(g.getCTM().inverse());
        // Compute new scale matrix in current mouse position
        var k = root.createSVGMatrix().translate(p.x, p.y).scale(zoomFactor).translate(-p.x, -p.y);
        //
        setCTM(g, g.getCTM().multiply(k));
    };
    zoomToScale = function (svgElementGBOXArray, scaleDownFactor) {
        try {
            if (!Utils.notNull(scaleDownFactor)) {
                scaleDownFactor = mentor.publisher.constants.ScaleDownFactorForZoomAll;
            }

            var bbox = getAggregateBoundingBox(svgElementGBOXArray);

            autoZoomSVGElement(null, getFitScale(bbox), scaleDownFactor);
        }
        catch (e) {

        }
    };
    autoFocusSVGElement = function (svgElementGBOXArray) {
        try {
            var g = viewport;

            var windowMIDPoint = root.createSVGPoint();
            windowMIDPoint.x = clientWidth / 2;
            windowMIDPoint.y = clientHeight / 2;

            var svgPoint = root.createSVGPoint();
            var bbox = '';
            var matrix = '';
            var t = true;
            if (Utils.notNull(svgElementGBOXArray) && svgElementGBOXArray.length == 1) {
                var svgElementGBOX = svgElementGBOXArray[0];
                var gElementToFocus = getGElementToFocus(svgElementGBOX);
                matrix = gElementToFocus.getScreenCTM();
                //if (gElementToFocus.getBBox().x == 0 && gElementToFocus.getBBox().y == 0) {
                if (lineElementPresentAsImmediateChild(gElementToFocus)) {
                    var firstshape = $("line", gElementToFocus);
                    if (Utils.notNull(firstshape) && Utils.notNull(firstshape[0]) && firstshape.length == 1) {
                        bbox = getBBoxForLineElement(firstshape[0], false);
                        t = false;
                        if (bbox.height == 0 && bbox.width == 0) {
                            bbox = gElementToFocus.getBBox();
                            t = true;
                        }
                    }
                    else {
                        bbox = gElementToFocus.getBBox();
                    }
                }
                else {
                    bbox = gElementToFocus.getBBox();
                    if ($(gElementToFocus).attr('id') == 'viewport') {
                        bbox = getViewPortBBox();
                    }
                }

                svgPoint.x = bbox.x + (bbox.width / 2);
                svgPoint.y = bbox.y + (bbox.height / 2);

                if (t) {
                    svgPoint = svgPoint.matrixTransform(matrix);
                    svgPoint = svgPoint.matrixTransform(g.getCTM().inverse());
                }
            }
            else if (Utils.notNull(svgElementGBOXArray) && svgElementGBOXArray.length > 1) {

                var bbox = getCombinedBBox(svgElementGBOXArray, false);
                svgPoint.x = bbox.x + (bbox.width / 2);
                svgPoint.y = bbox.y + (bbox.height / 2);
            }
            else {
                return;
            }
            //this.drawRectangle(bbox,'violet','none');
            //this.drawRectangle(this.getTotalBBox(),'red','none');
            //this.drawRectangle(this.getBBoxAfterTransformation(this.viewport),'red');
            windowMIDPoint = windowMIDPoint.matrixTransform(g.getCTM().inverse());

            var diffX = windowMIDPoint.x - svgPoint.x;
            var diffY = windowMIDPoint.y - svgPoint.y;

            // Compute new scale matrix in current mouse position
            var k = root.createSVGMatrix().translate(diffX, diffY);//.scale(2).translate(-diffX,
            // -diffY);//.translate(, windowMIDPoint.y -
            // y1);

            //todo set transform on model should change the view
            setCTM(g, g.getCTM().multiply(k));
            //model.set("transform", g.getCTM().multiply(k));
        }
        catch (e) {

        }
    };
    getAggregateBoundingBox = function (svgElementGBOXArray) {
        var bbox = '';
        if (Utils.notNull(svgElementGBOXArray) && svgElementGBOXArray.length == 1) {
            var svgElementGBOX = svgElementGBOXArray[0];
            var gElement = getGElementToFocus(svgElementGBOX);

            if (lineElementPresentAsImmediateChild(gElement)) {
                var firstshape = $("line", gElement);
                if (Utils.notNull(firstshape) && Utils.notNull(firstshape[0]) && firstshape.length == 1) {
                    bbox = getBBoxForLineElement(firstshape[0], true);
                    if (bbox.height == 0 && bbox.width == 0) {
                        bbox = getBBoxAfterTransformation(gElement);
                    }
                }
                else {
                    //bbox = gElement.getBBox();
                    bbox = getBBoxAfterTransformation(gElement);
                }
            }
            else {
                bbox = getBBoxAfterTransformation(gElement);
            }
        }
        else if (Utils.notNull(svgElementGBOXArray) && svgElementGBOXArray.length > 1) {
            bbox = getCombinedBBox(svgElementGBOXArray, true);
        }
        return bbox;
    };
    getViewPortBBox = function () {
        var bbox = {};
        bbox.x = 0;
        bbox.y = 0;
        bbox.height = Math.max(viewBoxHeight, viewport.getBBox().height);
        bbox.width = Math.max(viewBoxWidth, viewport.getBBox().width);
        return bbox;
    };
    lineElementPresentAsImmediateChild = function (gElementToFocus) {
        if (typeof(gElementToFocus.getBBox) != "undefined" && Utils.notNull($("line", gElementToFocus)) &&
                Utils.notNull($("line", gElementToFocus)[0]) &&
                $("line", gElementToFocus)[0].parentNode === gElementToFocus) {
            return true;
        }
        return false;
    };
    getGElementToFocus = function (svgElementGBOX) {
        var topGNode = $(svgElementGBOX).closest('g');
        //if the gElement's height and width are 0 and if there is a line element, then return the same gElement
        if (typeof(svgElementGBOX.getBBox) != "undefined" && Utils.notNull($("line", svgElementGBOX)) &&
                Utils.notNull($("line", svgElementGBOX)[0]) && ($("line", svgElementGBOX)).length == 1) {
            return svgElementGBOX;
        }
        //else return the topGNode
        else if (topGNode.length > 0 && Utils.notNull(topGNode[0].getBBox()) && topGNode[0].getBBox().height != 0 &&
                topGNode[0].getBBox().width != 0) {
            return topGNode[0];
        }
        //if the BBox of the element is fine, then return the element
        else if (typeof (svgElementGBOX.getBBox) != "undefined" && !(svgElementGBOX.getBBox().x == 0 &&
                svgElementGBOX.getBBox().y == 0)) {
            return svgElementGBOX;
        }
        else if (typeof (svgElementGBOX[0]) == "undefined") {
            var parent = $(svgElementGBOX).parent()[0];
            return parent;
        }
        else {
            return svgElementGBOX[0];
        }
    };
    getBBoxForLineElement = function (lineElement, transform) {
        var x1 = lineElement.getAttribute("x1");
        var y1 = lineElement.getAttribute("y1");

        var x2 = lineElement.getAttribute("x2");
        var y2 = lineElement.getAttribute("y2");

        var point = viewport.parentNode.createSVGPoint();
        point.x = x1;
        point.y = y1;

        var point1 = viewport.parentNode.createSVGPoint();

        point1.x = x2;
        point1.y = y2;

        point = point.matrixTransform(lineElement.getScreenCTM());
        point1 = point1.matrixTransform(lineElement.getScreenCTM());

        if (!transform) {
            point = point.matrixTransform(viewport.getCTM().inverse());
            point1 = point1.matrixTransform(viewport.getCTM().inverse());
        }

        var obj = {};

        obj.x = Math.min(point.x, point1.x);
        obj.y = Math.min(point.y, point1.y);

        obj.width = Math.abs(point1.x - point.x);
        obj.height = Math.abs(point1.y - point.y);

        return obj;
    };
    getCombinedBBox = function (svgElementGBOXArray, transform) {
        var minX = '';
        var minY = '';
        var maxX = '';
        var maxY = '';
        var bBoxReturn = {};
        var bbox;
        for (var k = 0; k < svgElementGBOXArray.length; k++) {
            var gElement = getGElementToFocus(svgElementGBOXArray[k]);
            //if (gElement.getBBox().x == 0 && gElement.getBBox().y == 0) {
            if (lineElementPresentAsImmediateChild(gElement)) {
                var firstshape = $("line", gElement);
                if (Utils.notNull(firstshape) && Utils.notNull(firstshape[0]) && firstshape.length == 1) {
                    bbox = getBBoxForLineElement(firstshape[0], transform);
                    if (bbox.height == 0 && bbox.width == 0) {
                        if (transform) {
                            bbox = getBBoxAfterTransformation(gElement);
                        }
                        else {
                            bbox = gElement.getBBox();
                        }
                    }
                }
                else {
                    if (transform) {
                        bbox = getBBoxAfterTransformation(gElement);
                    }
                    else {
                        bbox = gElement.getBBox();
                    }
                }
            }
            else {
                if (transform) {
                    bbox = getBBoxAfterTransformation(gElement);
                }
                else {
                    bbox = gElement.getBBox();
                }
            }
            //bBoxReturn = bbox;
            var gMinX = bbox.x;
            var gMinY = bbox.y;
            var gMaxX = bbox.x + bbox.width;
            var gMaxY = bbox.y + bbox.height;
            if ((gMinX < minX || minX == '' ) && gMinX != 0) {
                minX = gMinX;
            }
            if ((gMinY < minY || minY == '') && gMinY != 0) {
                minY = gMinY;
            }
            if ((gMaxX > maxX || maxX == '') && gMaxX != 0) {
                maxX = gMaxX;
            }
            if ((gMaxY > maxY || maxY == '') && gMaxY != 0) {
                maxY = gMaxY;
            }
        }
        bBoxReturn.x = minX;
        bBoxReturn.y = minY;
        bBoxReturn.width = maxX - minX;
        bBoxReturn.height = maxY - minY;
        return bBoxReturn;
    };
    autoZoomSVGElement = function (svgElementGBOX, zoomFactor, scaleDownFactor) {
        if (!zoomFactor) {
            return;
        }

        try {
            var g = viewport;
            if (g != null) {
                var gMat = g.getCTM();
                if (zoomFactor == Infinity) {
                    zoomFactor = 1;
                }
                var k = root.createSVGMatrix().scale(zoomFactor * scaleDownFactor * scale);
                //todo set transform on model should change the view
                setCTM(g, gMat.multiply(k));
                //model.set("transform", gMat.multiply(k));
                zoomfactor = zoomFactor * scaleDownFactor * scale;
            }
        }
        catch (e) {

        }
    };
    /*
     This function calculates the scale by which the svg could be zoomed in/out
     so that all the highlighted elements will be in focus
     it takes the combined bbox of all the highlighted elements and the svg object element
     */
    getFitScale = function (bb) {
        var boundingBoxHeight = bb.height;
        var boundingBoxWidth = bb.width;
        var parentHeight = clientHeight;
        var parentWidth = clientWidth;
        //bounding box needs to be fit into the parent rectangle
        return calculateScale(boundingBoxWidth, boundingBoxHeight, parentWidth, parentHeight);
    };
    calculateScale = function (w1, h1, w2, h2) {
        if (h1 <= h2 && w1 <= w2) {
            var heightScale = h2 / h1;
            var widthScale = w2 / w1;
            if (heightScale > widthScale) {
                return widthScale;
            }
            else {
                return heightScale;
            }
        }
        else if (h1 >= h2 && w1 >= w2) {
            var heightScale = h1 / h2;
            var widthScale = w1 / w2;
            if (heightScale > widthScale) {
                return 1 / heightScale;
            }
            else {
                return 1 / widthScale;
            }
        }
        else if (h1 <= h2 && w1 >= w2) {
            return w2 / w1;
        }
        else if (h1 >= h2 && w1 <= w2) {
            return h2 / h1;
        }
        else {
            return 1;
        }
    };
    getBBoxAfterTransformation = function (svgElementGBOX) {
        try {
            var ct = svgElementGBOX.getScreenCTM();
            var bb = svgElementGBOX.getBBox();
            if ($(svgElementGBOX).attr('id') == 'viewport') {
                bb = getViewPortBBox();
            }
            //get the coordinates of the four corners,construct points
            var leftTop = root.createSVGPoint();
            leftTop.x = bb.x;
            leftTop.y = bb.y;
            var leftBottom = root.createSVGPoint();
            leftBottom.x = bb.x;
            leftBottom.y = bb.y + bb.height;
            var rightTop = root.createSVGPoint();
            rightTop.x = bb.x + bb.width;
            rightTop.y = bb.y;
            var rightBottom = root.createSVGPoint();
            rightBottom.x = bb.x + bb.width;
            rightBottom.y = bb.y + bb.height;
            //transform the points with the currentelement transformation
            leftTop = leftTop.matrixTransform(ct);
            //leftTop = leftTop.matrixTransform(g.getCTM().inverse());
            leftBottom = leftBottom.matrixTransform(ct);
            //leftBottom = leftBottom.matrixTransform(g.getCTM().inverse());
            rightTop = rightTop.matrixTransform(ct);
            //rightTop = rightTop.matrixTransform(g.getCTM().inverse());
            rightBottom = rightBottom.matrixTransform(ct);
            //rightBottom = rightBottom.matrixTransform(g.getCTM().inverse());

            var bbBoxObj = {};
            bbBoxObj.x = Math.min(leftTop.x, leftBottom.x, rightTop.x, rightBottom.x);
            bbBoxObj.y = Math.min(leftTop.y, leftBottom.y, rightTop.y, rightBottom.y);
            var xMax = Math.max(leftTop.x, leftBottom.x, rightTop.x, rightBottom.x);
            var yMax = Math.max(leftTop.y, leftBottom.y, rightTop.y, rightBottom.y);
            bbBoxObj.height = yMax - bbBoxObj.y;
            bbBoxObj.width = xMax - bbBoxObj.x;
            if (Utils.is_msie() || Utils.isEdge()) {
                return bbBoxObj;
            }
            else {
                bb.x = bbBoxObj.x;
                bb.y = bbBoxObj.y
                bb.height = bbBoxObj.height;
                bb.width = bbBoxObj.width;
            }
        }
        catch (e) {

        }
        return bb;
    };
    zoomFit = function () {
        //var obj = svgContainerObject;
        //if (obj) {
        //obj.setAttribute('width', '100%');
        //obj.setAttribute('height', '100%');
        var a = [];
        a.push(viewport);
        zoomToScale(a, mentor.publisher.constants.ScaleDownFactorForZoomAll);
        autoFocusSVGElement(a);
        //setZoomSliderLevel(this.svgContainerId, '100');
        return 1;
        //}
    };
    initialize = function (svgTransformModel, scaleMultiplier) {
        scale = scaleMultiplier ? scaleMultiplier : 1;
        model = svgTransformModel;
        svgContainerId = svgTransformModel.get('svgContainerId');
        root = svgTransformModel.get('root');
        viewport = svgTransformModel.get('viewport');
        clientWidth = svgTransformModel.get('clientWidth');
        clientHeight = svgTransformModel.get('clientHeight');
        viewBoxWidth = svgTransformModel.get('viewBoxWidth');
        viewBoxHeight = svgTransformModel.get('viewBoxHeight');

    };
    calculateZoomLimit = function (zoomfactor, scale) {
        var step = Math.round(calculateSliderStep(zoomfactor) * 10),
                currentZoomLevel = model.get('currentZoomLevel'), limit, newZoomLevel;
        newZoomLevel = parseInt(step) + parseInt(currentZoomLevel * 1);
        return ((scale * 100) > newZoomLevel) ? newZoomLevel : (scale * 100);
    }
    return {
        bringToFront: function (svgTransformModel, svgElementGBOXArray, scaleDownFactor, scaleMultiplier) {
            initialize(svgTransformModel, scaleMultiplier);
            zoomToScale(svgElementGBOXArray, scaleDownFactor);
            autoFocusSVGElement(svgElementGBOXArray);
            var step = Math.round(calculateSliderStep(zoomfactor) * 10);
            moveZoomSlider(step, step, svgContainerId);
            //this.set('zoomfactor', zoomfactor);
        },
        panToMiddle: function (svgTransformModel, svgElementGBOXArray) {
            initialize(svgTransformModel);
            autoFocusSVGElement(svgElementGBOXArray);
        },
        fit: function (svgTransformModel, scaleMultiplier) {
            initialize(svgTransformModel, scaleMultiplier);
            zoomFit();
            //this.set('zoomfactor', zoomfactor);
        },
        zoomToMiddle: function (svgTransformModel, zoomFactor) {
            initialize(svgTransformModel);
            zoomToMiddle(zoomFactor);
        },
        /**
         * When rect1 with dimensions w1,h1 tries to fit into rect2 with dimensions w2,h2,
         * this method calculates the scale which needs to be applied to w1,h1 such that
         * it fits into rect2 without loosing the aspect ratio
         * @param w1 width of the rectangle to be adjusted
         * @param h1 height of the rectangle to be adjusted
         * @param w2 width of the rectangle into which the other rectangle should be fit into
         * @param h2 height of the rectangle into which the other rectangle should be fit into
         * @returns {number} the scale with which the width and height of rect1 should be scaled
         */
        calculateScale: function (w1, h1, w2, h2) {
            return calculateScale(w1, h1, w2, h2);
        }
    }
});