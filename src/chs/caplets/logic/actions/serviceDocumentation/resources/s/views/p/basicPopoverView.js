/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define*/
define("BasicPopoverView", ['jquery', 'underscore', 'backbone', "ListGroupView", "PopoverFilterModel"],
    function ($, underscore, Backbone, ListGroupView, popoverFilterModel) {
        "use strict";

        var BasicPopoverView = ListGroupView.extend({
            doNotLoadOnStart : true,
            initialize : function () {
            },

            removeView : function () {
                var popoverDiv = this.getPopoverDiv();
                $(popoverDiv, this.$el).remove();
            },

            getPopoverDiv : function () {
                return $("#detailPopup");
            },

            events : {
                "keyup #relateddata_filter" : "textEntered",
                "click #relateddata_filter" : "removeTextPlaceHolder",
                "focusout #relateddata_filter" : "addTextPlaceHolder"
            },
            removeTextPlaceHolder : function (evt) {
                var ele = $(evt.target);
                if ($(ele).hasClass("placeHolderText")) {
                    $(ele).val("");
                    $(ele).removeClass("placeHolderText");
                }
                this.triggerFilter($(ele).val());
                evt.stopPropagation();
            },

            addTextPlaceHolder : function (evt) {
                var ele = $(evt.target);
                if ($(ele).val().trim() === "") {
                    this.addPlaceHolderText(ele);
                }
                evt.stopPropagation();
            },

            addPlaceHolderText : function (element) {
                //todo internationalization
                $(element).val(mentor.publisher.languageTranslator.localize('FilterPlaceholderText'));
                $(element).addClass("placeHolderText");
            },

            getPopoverFilter : function() {
              return popoverFilterModel;
            },

            triggerFilter : function (filterText) {
                if (filterText === 'Filter') {
                    //todo can this condition lead to issues?
                    filterText = '';
                }
                this.getPopoverFilter().set("searchText", filterText);
            },
			
			getFilterInput: function(evt) {
				return  $("input", $(evt.target).parent());
			},
            filterEventTriggerTimer:"",
            textEntered : function (evt) {
                var ele = this.getFilterInput(evt);
                if (ele.hasClass("placeHolderText") === false) {
                    var reference = this;
                    if(this.filterEventTriggerTimer) {
                        clearTimeout(this.filterEventTriggerTimer);
                    }
                    this.filterEventTriggerTimer = setTimeout(function () {
                        var filterText = $(ele).val() || "";
                        reference.triggerFilter(filterText);
                        this.filterEventTriggerTimer = "";
                    }.bind(this), 2000);
                }
                evt.stopPropagation();
            },

            getHeight : function () {
                //todo if filter is enabled, the height is different
                return 198;
            },

            getWidth : function () {
                return 300;
            },

            getCoordinates : function (x, y) {
                var padding = 10, popoverWidth = this.getWidth(), popoverHeight = this.getHeight();

                x = x - popoverWidth / 2;

                if (x < padding) {
                    x = padding;
                }
                //calculating width and height is expensive on IE11
                var width =  (window.heavySVGs && mentor.publisher.bodyWidth) || $('body').width();
                var height =  (window.heavySVGs && mentor.publisher.bodyHeight) || $('body').height();
                if (x + popoverWidth > width - padding) {
                    x = width - (popoverWidth + padding);
                }

                y = y + padding;
                if (y < padding) {
                    y = padding;
                }
                if (y + popoverHeight > height - padding) {
                    y = height - (popoverHeight + padding);
                }
                return {x : x, y : y};
            }
        });
        mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.CLOSE_POPOVER, function (evt) {
            $('#detailPopup').remove();
            // $('#configPopup').remove();
        });
        return BasicPopoverView;
    });
