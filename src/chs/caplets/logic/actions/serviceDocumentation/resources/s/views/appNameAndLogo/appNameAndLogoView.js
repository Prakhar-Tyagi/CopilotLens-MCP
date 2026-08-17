/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */
define(["backbone"], function (Backbone) {
    var BuildView = Backbone.View.extend({

        updateApplicationNameAndLogo: function (viewObject) {
            var logoName = mentor.publisher.clientType.replace(/ /g, "");
            var fullPathToImage = "images/" + logoName + ".png";
            var fullPathToIcon = "images/" + logoName + "Icon.png";
            var applicationName = mentor.publisher.constants.clientTypeToNameMap[mentor.publisher.clientType];
            viewObject.$(".ApplicationNameInHeader").text(applicationName);
            viewObject.$(".ApplicationLogoInHeader").attr("src", fullPathToImage);
            $(".applicationIcon").attr("href", fullPathToIcon);
        }
    });
    return new BuildView();
});


