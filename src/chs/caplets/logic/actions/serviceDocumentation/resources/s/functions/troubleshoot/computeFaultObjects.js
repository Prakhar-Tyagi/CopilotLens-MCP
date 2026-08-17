/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

define(
        ['underscore', 'collections/faults'],
        function (_, faults) {
            return function (allCodes, activeCodes) {
                var objectMap = {};
                var countMap = {};
                var codesMap = {};
                activeCodes.forEach(function (code) {
                    faults.get(code).get('objects').forEach(function (object) {
                        if (!objectMap[object.id]) {
                            objectMap[object.id] = object;
                            countMap[object.id] = 0;
                        }
                        countMap[object.id] += 1;
                    });
                });

                faults.filter(function (fault) {
                    return _.contains(allCodes, fault.get('code'));
                }).forEach(function (fault) {
                    fault.get('objects').map(function (object) {
                        return object.id;
                    }).forEach(function (id) {
                        if (!objectMap[id]) {
                            return;
                        }
                        if (!codesMap[id]) {
                            codesMap[id] = [];
                        }
                        codesMap[id].push([fault.get('code'),
                            activeCodes.includes(fault.get('code')) ? "ActiveCode" : "PassiveCode"]);
                    });
                });

                var faultObjects = {};
                faultObjects.commonObjects = _.keys(objectMap).filter(function (id) {
                    return countMap[id] === activeCodes.length;
                }).map(function (id) {
                    var clone = _.clone(objectMap[id]);
                    clone.codes = codesMap[id] || [];
                    return clone;
                });
                faultObjects.renderObjects = _.keys(objectMap).filter(function (id) {
                    return objectMap[id].type == "Wire"
                            || objectMap[id].type == "Net"
                            || objectMap[id].type == "Shield";
                }).map(function (id) {
                    return objectMap[id];
                });
                return faultObjects;
            };
        }
)