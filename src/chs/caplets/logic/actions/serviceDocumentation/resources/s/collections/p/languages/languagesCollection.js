/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global define, Backbone, xmlDataLoader, objectFactoryImpl, mentor*/

define("LanguagesCollection", [],
    function () {
        "use strict";
        var LanguagesModel = Backbone.Model.extend(), languageItems;
        languageItems = Backbone.Collection.extend({
            model : LanguagesModel,

            fetch : function (model) {
                this.reset(this.getData(model));
            },
            getData : function (data) {
                return data;
            }
        });

        return new languageItems();
    });/**
 * Created with IntelliJ IDEA.
 * User: kayyagar
 * Date: 3/5/13
 * Time: 8:36 AM
 * To change this template use File | Settings | File Templates.
 */
