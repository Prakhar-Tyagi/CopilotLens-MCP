/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

(function () {

    var loadSource = function (file, onSourceLoad) {
        console.log(file.fileSrc);
        if (!file.fileSrc.match(/^\/test\//)) {
            return false;
        }

        var onSuccess = function () {
            onSourceLoad({
                file : file,
                success : true,
                message : ''
            });
        };

        var onError = function (err) {
            var msg = err.requireType;
            if (err.requireModules) {
                msg += ': ' + err.requireModules.join(', ');
            }
            onSourceLoad({
                file : file,
                success : false,
                message : msg
            });
        };

        require([file.fileSrc], onSuccess, onError);

        return true;
    };

    jstestdriver.pluginRegistrar.register({
        name : 'AMDLoaderPlugin',
        loadSource : loadSource
    });

})();
