/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
require(["views/appNameAndLogo/appNameAndLogoView"], function (appNameAndLogoView) {
    describe("appNameAndLogoTest", function () {
        function testApplicationNameAndLogoUpdate(applicationName, textDisplayed)
        {
            mentor.publisher.clientType = applicationName;
            appNameAndLogoView.updateApplicationNameAndLogo(appNameAndLogoView);
            expect(appNameAndLogoView.$('.ApplicationNameInHeader').html()).toBe(textDisplayed);
            expect(appNameAndLogoView.$('.ApplicationLogoInHeader').attr("src")).toBe(
                    "images/" + applicationName + ".png");
        }

        it("should update the logo and title for different applications on render", function () {
            var parent$ = $(
                    '<div><div class="ApplicationNameInHeader"></div><img src="" class="ApplicationLogoInHeader"></div>');
            appNameAndLogoView.$el = parent$;
            testApplicationNameAndLogoUpdate("CapitalServiceExplorer", "Capital Service Explorer");
            testApplicationNameAndLogoUpdate("CapitalSmartFlows", "Capital Smart Flows");
            testApplicationNameAndLogoUpdate("CapitalDesignExplorer", "Capital Design Explorer");
            testApplicationNameAndLogoUpdate("CapitalChangeExplorer", "Capital Change Explorer");
        });
    });

});
