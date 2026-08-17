/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, $*/
define(["PopoverItemView", "fileDisplayHandler", "currentPackage"],
        function (PopoverItemView, fileDisplayHandler, currentPackage)
        {
            "use strict";
            var CustomDesignData = PopoverItemView.extend({
                getData:function ()
                {
                    return this.collection;
                },
                getTitle:function ()
                {
                    return this.title;
                },
                getClassName:function ()
                {
                    return "customData";
                },
                events:{
                    "click .listItem":"clicked",
                    "click .popUp":"openPopup"
                },
                shouldShowPopup:function ()
                {
                    return true;
                },
                createCustomFileURL:function (content)
                {
                    return "popout.html#/customFile/" +
                            content.mainText + "/" +
                            currentPackage.get("id").replace("\\", "/") + "/" +
                            content.path.replace(/\\/g, "/");
                },
                openPopup:function (event)
                {
                    var url, cid = $(event.currentTarget).parent().attr('data-id'), clickedItem, content;
                    clickedItem = this.collection.get(cid);
                    if (clickedItem) {
                        clickedItem.path = clickedItem.get("path");
                        clickedItem.mainText = clickedItem.get("mainText");
                        url = this.createCustomFileURL(clickedItem);
                        this.openPopout(url);
                        event.stopPropagation();
                        return;
                    }
                },
                clicked:function (event)
                {
                    var cid = $(event.currentTarget).attr('data-id'), clickedItem, content;
                    clickedItem = this.collection.get(cid);
                    if (clickedItem) {
                        clickedItem.path = clickedItem.get("path");
                        clickedItem.mainText = clickedItem.get("mainText");
                        fileDisplayHandler.display(clickedItem);
                    }
                }
            });
            return CustomDesignData;
        });
