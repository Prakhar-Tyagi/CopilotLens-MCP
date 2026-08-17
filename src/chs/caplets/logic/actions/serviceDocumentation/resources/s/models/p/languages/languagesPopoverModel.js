/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor*/
define("LanguagesPopoverModel", ["PopoverModel", "LanguagesCollection"],
    function (PopoverModel, languages) {
        "use strict";
        var LanguagesPopoverModel = PopoverModel.extend({
            loadCollections : function (model) {
                languages.fetch(model);
            },
            loadData : function (data) {
                return data.models;
            }
        }), languagesPopoverModel, call = function (evt) {
            languagesPopoverModel.loadPopoverData(evt)
        };
        languagesPopoverModel = new LanguagesPopoverModel();
        mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.OPEN_LANGUAGES_POPUP, call);
        return _.extend(languagesPopoverModel, Backbone.Events);
    });

