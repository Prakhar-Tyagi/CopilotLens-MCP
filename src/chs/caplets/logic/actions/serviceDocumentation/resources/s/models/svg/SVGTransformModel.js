/**
 * Created by kayyagar on 24-01-2016.
 */
define("SVGTransformModel", [
    'jquery',
    'underscore',
    'backbone',
    'ZoomAndPanModule',
    "ZoomToolBarModel",
    "PersistenceModelFactory"
], function ($, underscore, Backbone, zoomAndPanModule, zoomToolBarModel, PersistenceModelFactory)
{
    "use strict";
    var SVGTransformModel;

    SVGTransformModel = PersistenceModelFactory.createCompatibleModel().extend({
        url: 'defineZoomLevel',
        defaults: {
            id: '',
            svgContainerId: '',
            type: 'all',
            root: '',
            transform: '',
            viewport: '',
            viewBoxWidth: '',
            viewBoxHeight: '',
            clientWidth: '',
            clientHeight: '',
            currentZoomLevel: 0,
            scale: 1,
            zoomScale: 1
        },
        fit: function ()
        {
            zoomAndPanModule.fit(this, 1);
        },
        fitLockedView: function ()
        {
            var callback, that = this;
            callback = function ()
            {
                that.updateModel();
                zoomAndPanModule.fit(that, that.get('zoomScale'));
            };
            this.fetch({'success': callback, 'error': callback});
        },
        panToMiddle: function (svgElementGBOXArray)
        {
            var callback, that = this;
            callback = function ()
            {
                that.updateModel();
                zoomAndPanModule.panToMiddle(that, svgElementGBOXArray);
            };
            this.fetch({'success': callback, 'error': callback});
        },

        saveZoomLevel: function ()
        {
            var currentZoomLevel, containerId = this.get('svgContainerId'), scaleValue;
            currentZoomLevel = zoomToolBarModel.get(containerId).get('currentZoomLevel');
            scaleValue = parseFloat(currentZoomLevel, 10) / 100;
            this.set('scale', scaleValue);
            this.updateModel();
            this.save({'id': this.get('id'), 'scale': this.get('scale')});
        },

        zoomToMiddle: function (zoomFactor)
        {
            zoomAndPanModule.zoomToMiddle(this, zoomFactor);
        },

        updateModel: function ()
        {
            var scaleValue, scale = 1, containerId = this.get('svgContainerId'), currentZoomLevel,
                    lockState = false, zoomScale;
            scaleValue = this.get('scale');
            if (scaleValue) {
                scale = parseFloat(scaleValue, 10);
                //console.log("fetching scaleValue for " + this.get('type') + " = " + scale);
                lockState = true;
            }
            else {
                lockState = false;
            }
            currentZoomLevel = (scale * 100);
            zoomScale = calculateZoomFactor((currentZoomLevel - 100) / 10);
            zoomToolBarModel.get(containerId).set("currentZoomLevel", currentZoomLevel);
            this.set('scale', scale);
            this.set('zoomScale', zoomScale);
            this.set('currentZoomLevel', currentZoomLevel);
            zoomToolBarModel.get(containerId).set({"lockState": lockState, "lockedZoomLevel": currentZoomLevel});
        },

        fetch: function (callback)
        {
            SVGTransformModel.__super__.fetch.apply(this, arguments);
        },

        save: function (attributes)
        {
            SVGTransformModel.__super__.save.apply(this, arguments);
        },

        destroy: function (options)
        {
            SVGTransformModel.__super__.destroy.apply(this, arguments);
        },
    });
    return SVGTransformModel;
});
