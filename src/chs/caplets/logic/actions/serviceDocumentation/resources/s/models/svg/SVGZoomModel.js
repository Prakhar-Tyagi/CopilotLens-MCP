/**
 * Created by kayyagar on 24-01-2016.
 */
define("SVGZoomModel", [
    'jquery',
    'underscore',
    'backbone',
    'ZoomAndPanModule',
    "ZoomToolBarModel",
    "PersistenceModelFactory"
], function ($, underscore, Backbone, zoomAndPanModule, zoomToolBarModel, PersistenceModelFactory)
{
    "use strict";
    var SVGZoomModel;

    SVGZoomModel = PersistenceModelFactory.createCompatibleModel().extend({
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
        }
    });
    return SVGZoomModel;
});
