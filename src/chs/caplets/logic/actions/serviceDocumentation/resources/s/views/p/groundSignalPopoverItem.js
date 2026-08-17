/*global define, $, mentor*/
define(["PopoverItemView", "collections/p/groundPathCollection", "fileDisplayHandler", "currentPackage"],
    function (PopoverItemView, groundPathCollection, fileDisplayHandler, currentPackage) {
        "use strict";
        var ThreeDViewItem = PopoverItemView.extend({
            getData : function () {
                return groundPathCollection;
            },
            events : {
                "click .listItem" : "popoverItemClicked",
                "click  .listItem>.popUp" : "popOut"
            },
            createURL : function (content) {
                return "popout.html#/renderSignal/"  + content.id + "/" +
                    currentPackage.get("id").replace("\\", "/");
            },
            displayContent : function (content) {
                this.getWindowObj().mentor.publisher.detailLayoutManager.resetContentPanel();
                this.getWindowObj().mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.GROUND_PATH_TRACE,
                    {id : content.id});
            },

            getItemContent : function (itemId) {
                return this.getData().get(itemId);
            },
            getTitle : function () {
                return "groundAndPowerSignalTitle";
            },
            getClassName : function () {
                return "groundAndPowerSignalTitle";
            },
            shouldShowPopup: function () {
                return false;
            }
        });
        return new ThreeDViewItem();
    });
