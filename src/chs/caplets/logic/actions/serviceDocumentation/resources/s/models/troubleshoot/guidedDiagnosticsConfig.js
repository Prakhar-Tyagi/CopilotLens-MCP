/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

define(
        ['currentPackage'],
        function (currentPackage) {
            'use strict';

            var config = undefined;

            function fetchConfig()
            {
                config = undefined;
                $.ajax({
                    async: false,
                    url: Utils.prepareFilePath(mentor.publisher.project.getId() + "/spotlight_api.json"),
                    contentType: "application/json; charset=utf-8",
                    success: function (api) {
                        config = {};
                        config.headers = api.headers;
                        config.headers.SpotlightURL = api.url;
                        config.params = api.params;
                        config.supportsMultipleCodes = api.supportsMultipleCodes;
                    },
                    dataType: "json"
                });
            }

            currentPackage.on('change:id', fetchConfig, this);

            fetchConfig();

            return function () {
                return config;
            }
        }
)