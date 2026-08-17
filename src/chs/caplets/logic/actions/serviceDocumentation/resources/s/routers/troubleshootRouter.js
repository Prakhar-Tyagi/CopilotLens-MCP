/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

define(["fileDisplayHandler", "componentRouter"], function (fileDisplayHandler, componentRouter) {
    return extend(componentRouter, {
        fileDisplayHandler: fileDisplayHandler,

        openTroubleshootPanel: function (faults, options) {
            var activeCodes = (options.parameters && options.parameters.activeFaultCodes) || [];
            var passiveCodes = (options.parameters && options.parameters.passiveFaultCodes) || [];

            if (activeCodes.length === 0 && passiveCodes.length === 0) {
                alert("No codes provided to troubleshoot.");
                return;
            }
            activeCodes = typeof activeCodes == "string" ? activeCodes.split(',') : activeCodes;
            passiveCodes = typeof passiveCodes == "string" ? passiveCodes.split(',') : passiveCodes;
            var allCodes = activeCodes.concat(passiveCodes);
            var hasInvalidCodes = allCodes.every(function (code) {
                return !faults.get(code);
            })

            if (hasInvalidCodes) {
                alert("Invalid codes provided to troubleshoot.");
                return;
            }

            fileDisplayHandler.display({
                id: "Common Fault Codes",
                type: mentor.publisher.contentType.TROUBLESHOOT,
                passiveCodes: passiveCodes,
                activeCodes: activeCodes,
            });
        },

        openComponent: function (options) {
            require(["collections/faults"], function (faults) {
                // TODO: fetch option hash success does not work because section collection does not handle this.
                // We are using Async=false for most collections
                faults.fetch();
                this.openTroubleshootPanel(faults || [], options);
            }.bind(this));
        }
    });
});