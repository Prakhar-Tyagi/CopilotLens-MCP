/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils*/
define("EULA",
    [
        'jquery',
        'underscore',
        'backbone',
        "currentPackage",
        "text!templates/EULATemplate.html",
        "views/appNameAndLogo/appNameAndLogoView"
    ],
    function ($, _, Backbone, selectedPackage, template, appNameAndLogoView) {
        "use strict";

        var EULAView = Backbone.View.extend({
            el : "#eualContainer",

            events : {
                "click input" : "eulaAccepted"
            },

            render: function () {
                var applicationName = mentor.publisher.constants.clientTypeToNameMap[mentor.publisher.clientType].toUpperCase();
                this.$el.html(_.template(template)({applicationName}));
                appNameAndLogoView.updateApplicationNameAndLogo(this);
                return this;
            },

            eulaAccepted : function () {
                mentor.publisher.eulaVarified = true;
                Utils.createCookie("eula_rev_210520", "accepted", 365);
                this.trigger("AcceptedEULA");
                this.remove();
            }

        });

        return new EULAView();
    }
);
