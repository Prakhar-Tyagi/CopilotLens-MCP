/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor*/
define([], function () {
    "use strict";
    var NavigationPanel = Backbone.Model.extend({});
    mentor.publisher.navigationPanelModel = new NavigationPanel();
    return new NavigationPanel();
});

