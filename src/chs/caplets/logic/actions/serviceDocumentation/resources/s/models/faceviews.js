/*global define, mentor, Utils */
define(["backbone"], function (Backbone)
{
    "use strict";

    return {
        getFaceViewFor: function (faceviewData)
        {
            faceviewData = faceviewData || {};
            var propertyToIdentifyFV = faceviewData.getValueToCompare || function (object)
                    {
                        return ((object && object.id) || "").toLowerCase();
                    };
            if (!faceviewData.objectId) {
                return {};
            }
            var connData = this.getObjectData(faceviewData.systemId,
                    faceviewData.objectId), content, Model = Backbone.Model.extend(
                    {}), faceviewModel = new Model(), objectView;
            if (connData) {
                content = connData.getFaceviews().listItems || [
                            {}
                        ];
                if (faceviewData["multiple-faceview-support"]) {
                    faceviewData.viewId =
                            faceviewData.viewId || mentor.publisher.urlParams.viewName || "noViewSpecified";
                }
                else {
                    faceviewData.viewId = faceviewData.id;
                }
                objectView =
                        mentor.publisher.router.getViewObjectForType(content,
                                faceviewData.viewId || mentor.publisher.urlParams.viewName,
                                propertyToIdentifyFV,
                                false);
                if (objectView) {
                    faceviewModel.set(objectView);
                    faceviewModel.type = mentor.publisher.contentType.CONNECTOR_FACE_VIEW;
                    var Coll = Backbone.Collection.extend({});
                    var faceviewsColl = new Coll();
                    content.forEach(function (element)
                    {
                        var fvModel = new Model();
                        fvModel.set(element);
                        faceviewsColl.add(fvModel);
                    });

                    faceviewModel.set("faceviews", this.getAllViews(faceviewsColl));
                    return faceviewModel;
                }
            }
        },
        getObjectData: function (systemId, connectorId)
        {
            return mentor.publisher.project.loadObjectData(systemId,
                    connectorId);
        },
        getAllViews: function (faceviews)
        {
            var views = [];
            faceviews.forEach(function (faceview)
            {

                if (faceview.get("multiple-faceview-support")) {
                    var viewName = faceview.get("view");
                    if (viewName === "noViewSpecified") {
                        viewName = "";
                    }
                    views.push({
                        mainText: Utils.translate(viewName),
                        id: faceview.get("id"),
                        path: faceview.get("path")
                    });
                }
            });
            return views;
        }
    };

});

