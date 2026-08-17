/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global define*/

define("LocalConfigurations", ["underscore", "currentPackage"], function (_, currentPackage)
{
    "use strict";
    var localStorage = window.localStorage;
    if (!localStorage) {
        throw new Error("module cannot be used without localstorage support!!!");
    }

    var LocalConfigs = function ()
    {
    };

    function getKey()
    {
        return currentPackage.get("id");
    }

    function findByName(configName, configurationList)
    {
        return _.find(configurationList, function (thisConfig) {
            return thisConfig.name.toLowerCase() === configName.trim().toLowerCase();
        }, this);
    }

    LocalConfigs.prototype.getConfigs = function ()
    {
        try {
            var raw = localStorage.getItem(getKey());
            var parsed = JSON.parse(raw);
            if (!parsed) {
                parsed = [];
            }
            return parsed;
        }
        catch (exception) {
            return [];
        }
    }

    LocalConfigs.prototype.saveConfig = function (configuration)
    {
        if (configuration) {
            var configs = this.getConfigs();
            if (configs && configs instanceof Array) {
                configs.push(configuration);
            }
            localStorage.setItem(getKey(), JSON.stringify(configs));
        }
    }

    LocalConfigs.prototype.deleteConfig = function(configName)
    {
        configName = "" + configName;
        if (configName.trim()) {
            var configs = this.getConfigs();
            var configToRemove = findByName.call(this, configName, configs);

            if (configToRemove) {
                var configListToSave = _.without(configs, configToRemove);
                localStorage.setItem(getKey(), JSON.stringify(configListToSave));
                return true;
            }
            else {
                return false;
            }
        }
        return false;
    }

    return new LocalConfigs();
});
