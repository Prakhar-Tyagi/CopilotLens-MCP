/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, �SISW�), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer�s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, setTimeout, Msg*/
define([
    'jquery',
    'underscore',
    'backbone',
    "currentPackage",
    "filters/documentContentBasedFilter",
    "views/filters/toastNotificationView"
], function ($, underscore, Backbone, selectedPackage, documentContentBasedFilter, toastNotificationView)
{
    "use strict";

    var TextSearch = Backbone.View.extend({

        addPlaceHolderText:function (element)
        {
            $(element).val(mentor.publisher.languageTranslator.localize('FilterLabel'));
            $(element).addClass("placeHolderText");
        },

        initialize:function ()
        {
            selectedPackage.on("change:language", this.translate, this);
        },

        translate : function() {
            var filterElement = $("#filterText");
              if($(filterElement).hasClass("placeHolderText") ) {
                  $(filterElement).val(mentor.publisher.languageTranslator.localize('FilterLabel'));
                  $(filterElement).attr('title', mentor.publisher.languageTranslator.localize('pressEnterAndHitEnterKey'))
              }
        },

        events:{
            "keyup #filterText":"textEntered",
            "click #filterText":"removeTextPlaceHolder",
            "focusout #filterText":"addTextPlaceHolder",
            "click #resetFilter":"resetFilter"
        },
        removeTextPlaceHolder:function (evt)
        {
            //setTimeout(function() {
            var ele = $(evt.target);

            if ($(ele).hasClass("placeHolderText")) {
                // alert(mentor.publisher.languageTranslator.localize('NotReadyPleaseWait'));

                $(ele).val(mentor.publisher.languageTranslator.localize('NotReadyPleaseWait'));
                setTimeout(function ()
                {
                    selectedPackage.trigger("loadAllObjects");
                    $(ele).val("");
                    $(ele).removeClass("placeHolderText");
                }, 100);
            }

            //}, 100);
        },

        addTextPlaceHolder:function (evt)
        {
            var ele = $(evt.target);
            if ($(ele).val().trim() === "") {
                this.addPlaceHolderText(ele);
            }
        },

        resetFilter:function (evt)
        {
            var filterText, project, ele;
            ele = $("input", $(evt.target).parent());

            this.addPlaceHolderText(ele);
            setTimeout(function ()
            {
                filterText = $(ele).val() || "";
                selectedPackage.set("searchText", "");
            }, 500);
        },
        timeoutVar:"",

        textEntered:function (event)
        {
            var keycode = event.keyCode || event.which, that = this;
            clearTimeout(this.timeoutVar);
            if (keycode === 13 || keycode === 32) {
                if ($("#filterText").hasClass("placeHolderText") === false) {
                    that.timeoutVar = setTimeout(function ()
                    {
                        if(!documentContentBasedFilter.fetchSearchIndexProgress())
                        {
                            toastNotificationView.show(mentor.publisher.languageTranslator.localize('statusMsgForSearchIndexing'), 4000);
                        }
                        var filterText = $("#filterText").val();
                        selectedPackage.set("searchText", filterText);
                    }, 500);

                }
            }
        },

        render:function ()
        {
            this.setElement(this.container);
            this.$el.html(underscore.template(this.templateHTML)());
            this.filterTextValue = mentor.publisher.languageTranslator.localize('Filter');
            setTimeout(function ()
            {
                resizeTextFilterBox();
                $("#filterText").attr('title', mentor.publisher.languageTranslator.localize('pressEnterAndHitEnterKey'));

            }, 200);

            return this;
        }

    });

    return new TextSearch();
});
