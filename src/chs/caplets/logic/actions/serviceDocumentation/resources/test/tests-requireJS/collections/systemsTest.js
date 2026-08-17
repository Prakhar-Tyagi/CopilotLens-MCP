/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global require, describe, it, expect, beforeEach, Backbone, afterEach, createContext, sinon, define, collectionTest*/
(function () {
    "use strict";
    var /*mockCollection = Backbone.Collection.extend(), context, stubs, */fakeProject;
    fakeProject = {getSystems : function () {
    }};
    collectionTest("systemCollectionTest", fakeProject, "getSystems", "systems")();
})();




