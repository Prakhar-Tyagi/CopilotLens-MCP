/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, window, $, mentor, Utils*/
define(["PopoverItemView", "ConnectorFaceviewsCollection", "fileDisplayHandler", "currentPackage"],
        function (PopoverItemView, faceViews, fileDisplayHandler, currentPackage)
        {
            "use strict";
            var FaceviewItem = PopoverItemView.extend({
                getData: function ()
                {
                    return faceViews;
                },
                getTitle: function ()
                {
                    return splitPanelView.getConnectorViewTitle();
                },
                getClassName: function ()
                {
                    return "faceViews";
                },
                events: {
                    "click .faceViews>.listItem": "popoverItemClicked",
                    "click .faceViews>.listItem>.popUp": "popOut"
                },

                createURL: function (content)
                {
					var id =  content && content.get("id");
					if(id) {
                    return "popout.html#/faceview/" +
                            "systemId" + "/" + content.get("objectId") + "/" +
								"id"+id +"/" +
                            currentPackage.get("id").replace("\\", "/");
					} else {
						return "popout.html#/faceview/" +
								"systemId" + "/" + content.get("objectId") + "/" +
								currentPackage.get("id").replace("\\", "/");
					}
                },

                getItemContent: function (itemId)
                {
                    var content = this.getData().get(itemId);
                    content.type = mentor.publisher.contentType.CONNECTOR_FACE_VIEW;
                    content.systemId = content.systemId || content.get("systemId");
					content.set("faceviews", this.getAllViews(this.getData()));
                    return content;
                },getAllViews: function (faceviews)
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
            });
            return new FaceviewItem();
        });
