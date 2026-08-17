/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor, Utils*/
define(
    ["preferences", "currentPackage"],
    function (preferences, currentPackage) {
        "use strict";

        var getLocalizedApplicationTitle,
            getLocalizedPackageTitle,
            setDocumentTitle,
            updateTitle,
            updateTitleForPackageScreen,
            updateTitleForPackageSelectionScreen;

        getLocalizedApplicationTitle = function () {
            /** @namespace mentor.publisher.languageTranslator */
            var applicationName = mentor.publisher.clientType;
            applicationName = mentor.publisher.constants.clientTypeToNameMap[mentor.publisher.clientType];
            return applicationName
        };
        mentor.publisher.getLocalizedApplicationTitle = getLocalizedApplicationTitle;

        getLocalizedPackageTitle = function () {
            return Utils.translate(currentPackage.get("title"));
        };

        updateTitleForPackageScreen = function () {
            setDocumentTitle(getLocalizedPackageTitle() + " | " + getLocalizedApplicationTitle());
        };

        updateTitleForPackageSelectionScreen = function () {
            setDocumentTitle(getLocalizedApplicationTitle());
        };

        setDocumentTitle = function (title) {
            if (opener && opener.mentor) {
                return;
            }

            document.title = title;
        };

        updateTitle = updateTitleForPackageSelectionScreen;

        preferences.on("change:language", function () {
            updateTitle();
        });

        return {

            startUpdatingTitleForPackageScreen: function () {
                updateTitle = updateTitleForPackageScreen;
                updateTitle();
            },

            startUpdateTitleForPackageSelectionScreen: function () {
                updateTitle = updateTitleForPackageSelectionScreen;
                updateTitle();
            }

        };
    }
);