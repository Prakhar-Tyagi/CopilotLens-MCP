/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor, $, getIdToHighlight*/
define(["PopoverItemView",
            "collections/harnessPopoverColl",
            "currentPackage",
            "harnessLayouts"],
        function (PopoverItemView, harnesses, selectedPackage, harnessLayouts)
        {
            "use strict";
            var HarnessLayoutPopoverItem = PopoverItemView.extend({

                getData: function ()
                {
                    return harnesses;
                },

                getDataId: function (event)
                {
                    return $(event.currentTarget).parent().attr('data-id');
                }, stopEventPropagation: function (event)
                {
                    event.stopPropagation();
                }, popoutHandler: function ()
                {
                    return mentor.publisher.popoutHandler.openPopout;
                }, popOut: function (event)
                {
                    var layout,
                            layoutId,
                            diagramId = this.getDataId(event),
                            projectId,
                            type;

                    this.stopEventPropagation(event);

                    layoutId = this.getData().get(diagramId).get("id");
                    projectId = selectedPackage.get("id").replace("\\", "/");
                    type = harnessLayouts.getType(layoutId);
                    var objectId = this.getData().get(diagramId).get("objectId");
                    if (!type) {
                        type = "harnesslayoutdiagram";
                    }
                    require(["models/selectedSystem"], function (selectedSystem)
                    {
                        selectedSystem.set("objectId", objectId, {silent: true});
                        this.popoutHandler()(
                                "popout.html#/" + type.toLowerCase() +
                                "/" + layoutId +
                                "/" + diagramId +
                                "/" + projectId +
                                "/" + selectedSystem.get("objectId")
                        );

                    }.bind(this));
                },

                getTitle: function ()
                {
                    return "HarnessLayouts";
                }
                ,
                getClassName: function ()
                {
                    return "HarnessLayouts";
                }
                ,
                firstActiveSystem: function (harnesses)
                {
                    return harnesses[0];
                }
                ,
                showHarnessLayout: function (content)
                {
                    var type = harnessLayouts.getType(content.layoutId);
                    if (content) {
                        var type = harnessLayouts.getType(content.layoutId);
                        if (type) {
                            content.type = type;
                        }
                        this.getWindowObj().mentor.publisher.fileDisplayHandler.display(content);
                    }

                    require(["models/selectedSystem"], function (selectedSystem)
                    {
                        selectedSystem.trigger("scrollNavigationPanelToTheSelectedElement");
                    });
                }
                ,

                displayContent: function (content)
                {
                    this.getWindowObj().mentor.publisher.detailLayoutManager.resetContentPanel();
                    this.showHarnessLayout(content);

                }
                ,
                events: {
                    "click .HarnessLayouts>.listItem": "popoverItemClicked",
                    "mouseover .HarnessLayouts>.listItem": "showToolTip",
                    "mouseout .HarnessLayouts>.listItem": "removeToolTip",
                    "click .HarnessLayouts>.listItem>.popUp": "popOut"
                }
                ,

                getItemContent: function (itemId)
                {
                    var content = {},
                            defaultDocument,
                            harnessLayout, diagramID, harDigXref;

                    harnessLayout = this.getData().get(itemId);
                    if (harnessLayout) {
                        diagramID = harnessLayout.get("diagramId");
                        defaultDocument = harnessLayout.getDefaultDocument();

                        content = {
                            listItemId: harnessLayout.get("id"),
                            id: diagramID,
                            layoutId: harnessLayout.get("id"),
                            group: defaultDocument.group,
                            type: mentor.publisher.contentType.HARNESS_LAYOUT_DIAGRAM,
                            objectId:harnessLayout.get("objectId"),
                            reset: false
                        }

                    }
                    return content;
                }
            });
            return new HarnessLayoutPopoverItem();
        })
;