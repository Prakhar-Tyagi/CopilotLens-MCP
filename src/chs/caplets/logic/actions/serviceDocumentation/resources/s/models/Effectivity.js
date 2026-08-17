/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
define([
            "jquery",
            "backbone"
        ],
        function ($, Backbone) {

            var EffectivityModel = Backbone.Model.extend({
                initialize: function () {
                    this.isEffectivityProj = this.isEffectivityPackage();
                },

                isEffectivityPackage: function(){
                    var xmlData = mentor.publisher.xmlLoader.loadGlobalFile("unzipped/data/packages.xml", false, true,
                            "xml");
                    return $('subpackage', xmlData.data).length > 0;
                }
            });

            return new EffectivityModel();
        }
);