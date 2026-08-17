/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global mentor*/
mentor.publisher.objectModel = (function (p) {
    "use strict";
    var createObject, createSystem;

    createObject = function (id) {
        return {
            id : id
        };
    };
    createSystem = function (id) {
        var system = p.object(createObject(id));
        system.getObjects = function (objectType) {
            return p.filter.applyOptionFilter(p.dataLoader.getDesignObjects(id, objectType));
        };
        return system;
    };

    return {
        createSystemObj : function (id) {
            return createSystem(id);
        }
    };

}(mentor.publisher));
