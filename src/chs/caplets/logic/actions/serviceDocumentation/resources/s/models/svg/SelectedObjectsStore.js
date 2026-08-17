/**
 * Created by kayyagar on 21-01-2016.
 */
/*global define, Backbone, mentor*/
define("SelectedObjectsStore", [
    'underscore',
    'SVGTransformModel',
    'ZoomAndPanModule'
], function (underscore, SVGTransformModel, zoomAndPanModule)
{
    "use strict";
    /**
     * This model is a wrapper around svgTransformModel, so it always holds a reference to it.
     * This way, the wrapper is always upto date with changes to the model.
     */
    var SelectedObjectsPerContainerModel = SVGTransformModel.extend({
        defaults: {
            'objects': [],
            'zoomOnAdd': false,
            'svgTransformModel': ''
        },

        get: function (attr)
        {
            if (attr == 'svgTransformModel' || attr == 'objects' || attr == 'zoomOnAdd') {
                return Backbone.Model.prototype.get.call(this, attr);
            }
            return this.get('svgTransformModel').get(attr);
        },

        bringToFront: function (scaleDownFactor)
        {
            zoomAndPanModule.bringToFront(this, this.get('objects'), scaleDownFactor, 1);
        },
    });

    var SelectedObjectsModel = Backbone.Collection.extend({
        model: SelectedObjectsPerContainerModel
    });

    var selectedObjects = new SelectedObjectsModel([]),
            zoomOnAddMap = {},
            findSelectedObjectsModel;

    findSelectedObjectsModel = function (svgTransformModel)
    {
        var selectedObjectsModel = selectedObjects.findWhere({svgContainerId: svgTransformModel.get('svgContainerId')});
        if (!selectedObjectsModel) {
            //hoc = new SelectedObjectsPerContainerModel(svgTransformModel.attributes);
            selectedObjectsModel = new SelectedObjectsPerContainerModel({'svgTransformModel': svgTransformModel});
        }
        return selectedObjectsModel;
    };

    return {
        addObjectsForContainer: function (svgTransformModel, svgElementPositionArray, notToResetFlag)
        {
            var selectedObjectsModel = findSelectedObjectsModel(svgTransformModel);
            var objects = selectedObjectsModel.get('objects') || [];
            if (notToResetFlag) {
                objects = objects.concat(svgElementPositionArray);
            }
            else {
                objects = svgElementPositionArray;
            }
            selectedObjectsModel.set('objects', objects);
            selectedObjects.push(selectedObjectsModel);
            //todo this needs to be done more elegantly
            if (zoomOnAddMap[svgTransformModel.get('id')]) {
                zoomOnAddMap[svgTransformModel.get('id')] = false;
                selectedObjectsModel.panToMiddle(selectedObjectsModel.get('objects'));
            }
        },

        removeContainer: function (svgTransformModel)
        {
            selectedObjects.remove(selectedObjects.findWhere({svgContainerId: svgTransformModel.get('svgContainerId')}));
        },

        bringToFront: function ()
        {
            selectedObjects.each(function (c)
            {
                c.bringToFront(mentor.publisher.constants.ScaleDownFactorForZoomObject);
            });
        },

        panToMiddle: function ()
        {
            selectedObjects.each(function (c)
            {
                c.panToMiddle(c.get('objects'));
            });
        },

        getSelectedObjectForContainer: function (svgContainerId)
        {
            var store = selectedObjects.findWhere({svgContainerId: svgContainerId});
            return store ? store.get('objects') : [];
        },

        bringToFrontOnAddition: function (svgTransformModel)
        {
            zoomOnAddMap[svgTransformModel ? svgTransformModel.get('id'): ""] = true;
        }
    }
});