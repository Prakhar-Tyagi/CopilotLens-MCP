/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor*/
define("currentPackage", ["backbone"], function (Backbone) {
    "use strict";
    var Package = Backbone.Model.extend({
        initialize : function () {
            this.on("change:id", this.resetData, this);
        },
        resetData : function () {
            this.set("searchText", "", {silent : true});
            this.set("vin", "", {silent : true});
        },
        getFirstSection : function () {
            var dataLoader = mentor.publisher.dataLoader, project;
            //todo should not load project here
            return mentor.publisher.project.getFirstSection();
        }
    }), selectedPackage;
    selectedPackage = new Package();
    //default value
    selectedPackage.set("language", Utils.readCookie("language") || "EN");

    return selectedPackage;
});

