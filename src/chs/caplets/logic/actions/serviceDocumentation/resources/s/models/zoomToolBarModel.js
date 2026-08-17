/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, �SISW�), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer�s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global define, Utils, resizeSvg, parseInt, mentor*/
define("ZoomToolBarModel", [
    'jquery',
    'underscore',
    'backbone'
], function ($, underscore, Backbone) {
    "use strict";
    var DetailsPanelModel = Backbone.Model.extend({

        /**
         * This should be call from the parent panel once the position/size of the panel is changed
         * @param container is the parent container
         * @param x is the x value of the parent
         * @param y
         * @param width
         * @param height
         */
        updateLocation : function (container, x, y, width, height) {
            var containerData = this.get(container), parentDimensions = {};
            parentDimensions.x = x;
            parentDimensions.y = y;
            parentDimensions.width = width;
            parentDimensions.height = height;
            // TODO: check added to run the UTs, Need to remove eventually or perhaps we can live with this.
            containerData && containerData.set('parentDimensions', parentDimensions);
            this.set(container, containerData);
        },
        /**
         * This method should be called once the zoom action using scroll bar is triggered
         * @param container is the parent container
         * @param zoomx zoom value in the x dimension
         * @param zoomy zoom value in the y direction
         */
        updateZoomLevel : function (container, zoomx, zoomy) {
            var containerData = this.get(container), currentZoomLevel, newZoomLevel;
            currentZoomLevel = containerData && containerData.get('currentZoomLevel') ? containerData.get('currentZoomLevel') : 0;
            newZoomLevel = parseInt(zoomx) + parseInt(currentZoomLevel * 1);
            containerData.set('currentZoomLevel', newZoomLevel);
            this.set(container, containerData);
        },
        /**
         * This method is called from +/- button click
         * @param container is the containerId of the view
         * @param zoomLevelText is the text from the zoom  text field
         * @param stepLength is +1/-1 for zoom out/in
         */
        updateZoom : function (container, zoomLevelText, stepLength) {
            var containerData = this.get(container), zoomFactor, newZoomLevel = zoomLevelText &&
                    parseInt(zoomLevelText.replace('%', ''), 10);
            if (newZoomLevel === mentor.publisher.constants.MaxZoomPercentage) {
                return;
            }
            zoomFactor = calculateZoomFactor(stepLength);
            containerData.set('zoomFactor', zoomFactor);
            containerData.set('currentZoomLevel', newZoomLevel);
        },
        /**
         * creates a model for each container,
         * @param container is the container id of the artifect div
         * @param notchParentPosition position of the div where the notch is free to move
         * @param notchParentWidth width of the div where the notch is free to move
         * @param notchWidth width of the notch
         */
        createContainerModel : function (container, notchParentPosition, notchParentWidth, notchWidth) {
            var ZoomToolBarModelForContainer = Backbone.Model.extend({
                initialize : function () {
                    //console.log("zoom tool bar container model initialized");
                }
            }), containerData = new ZoomToolBarModelForContainer(), maxXValue, sliderWidth;
            maxXValue = notchParentPosition.left + notchParentWidth - notchWidth;
            sliderWidth = notchParentWidth - notchWidth;
            containerData.set('sliderWidth', sliderWidth);
            containerData.set('maxXValue', maxXValue);
            containerData.set('currentZoomLevel', 100);
            this.set(container, containerData);
        },

        addEventHandler : function () {
            var model = this;
            mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.ZOOM_TRIGGERED,
                function (evt) {
                    var zoomx = evt.detail.zoomx, containerId = evt.detail.containerId;
                    model.updateZoomLevel(containerId, zoomx);
                });
        },
        initialize : function () {
            this.addEventHandler();
        },

        getSliderContent : function (container, deltaX, zoomCurrVal, notchXPosition) {
            var maxXValue, sliderWidth, factor, newZoomLevel, newNotchXPosition;
            sliderWidth = this.get(container).get('sliderWidth');
            maxXValue = this.get(container).get('maxXValue');
            if (zoomCurrVal >= mentor.publisher.constants.MaxZoomPercentage && deltaX > 0) {
                return;
            } else if (zoomCurrVal <= mentor.publisher.constants.MinZoomPercentage && deltaX < 0) {
                return;
            }
            factor = mentor.publisher.constants.MaxZoomPercentage / sliderWidth;
            newNotchXPosition = Math.min(Math.max(notchXPosition + deltaX, 0), maxXValue);
            newZoomLevel = parseInt(deltaX * factor, 10) + parseInt(zoomCurrVal, 10);
            return {x : newNotchXPosition, zoom : ('' + Math.round(newZoomLevel) + '%')};
        }
    });

    return new DetailsPanelModel();
});
