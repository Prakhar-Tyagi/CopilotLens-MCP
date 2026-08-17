/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

define(
        ['jquery'],
        function ($) {
            "use strict";

            return function (codes, faultSignals) {
                var projectId = mentor.publisher.project.getId();
                var files = _.chain(codes)
                        .map(function (code) {
                            return faultSignals.findById(code);
                        })
                        .filter(function (model) {
                            return model != undefined;
                        })
                        .map(function (model) {
                            return projectId + "/Signals/" + model.get('signalFile');
                        })
                        .uniq()
                        .value();
                var preferences = mentor.publisher.dataLoader.getProjectPreferences();

                var url = "rendereddiagram?files=" + files.join(",");
                url = url + "&options=" + getCurrentConfigurationData();
                url = url + "&hookupConnectOntoMulticore=" + preferences.hookupConnectOntoMulticore;
                url = url + "&hookupConnectOntoOverbraid=" + preferences.hookupConnectOntoOverbraid;
                url = url + "&rand=" + (Math.random());

                return $.ajax({
                    url: Utils.prepareFilePath(url),
                    dataType: (Utils.is_msie()) ? "text" : "html"
                });
            };
        }
);