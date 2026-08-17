/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, mentor, setTimeout, initResizebars*/
define([
    'jquery',
    'underscore',
    "models/Effectivity",
    'backbone'
], function ($, underscore, Effectivity, Backbone) {
    "use strict";
    var Viewer = Backbone.View.extend({
        render: function () {
            this.setElement(this.container);
            this.$el.html(underscore.template(this.templateHTML)({
                configLicence: mentor.publisher.configurationsManager.hideOrShowConfigBuilderButton(),
                isEffectivityPacket: Effectivity.isEffectivityProj
            }));
            setTimeout(function () {
                initResizebars();
            }, 500);
            return this;
        }
    });

    return new Viewer();
});

