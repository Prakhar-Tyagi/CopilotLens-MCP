/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global mentor, Utils, $,packageModel, Msg, document, Constants, ConfigurationFilterPopup, XRefFilterConfigurationFilterPopup, VehicleConfigObject, xmlDataLoader, objectFactoryImpl*/
mentor.publisher.configurationsManager = (function ()
{
    "use strict";
    var setSelectedOptions,
        selectedOptions,
        dynamicNavigationActive,
        filterOptionsForContentArea,
        hidePopup,
        isVehicleConfigXMLPresent,
        vinFilterInstance = mentor.publisher.optionFilterPanel,
        vehicleConfigObject,
        filterType,
        enableConfigFilter,
        setTextInStaticMode,
        lastVehicleConfigObject,
        sortConfigurations,
        isSubSet,
        swap,
        configurationBuilderPopup,
        reset;

    reset = function ()
    {
        selectedOptions = '';
        dynamicNavigationActive = false;
        filterOptionsForContentArea = '';
        vehicleConfigObject = null;
        lastVehicleConfigObject = null;
    };

    enableConfigFilter = function (enable)
    {
        if (enable) {
            $('#configButton').show();
        }
        else {
            $('#configButton').hide();
        }
    };

    hidePopup = function ()
    {
    };

    isVehicleConfigXMLPresent = function ()
    {
        var vehicleConfigUrl = (Utils.prepareFilePath(mentor.publisher.project.getId() + '/vehicleconfig.xml'));
        var isVehicleConfigurationPresent;
        $.ajax({
            url: vehicleConfigUrl,
            success: function ()
            {
                isVehicleConfigurationPresent = true;
            },
            error: function ()
            {
                isVehicleConfigurationPresent = false;
            }, dataType: (Utils.is_msie()) ? "text" : "xml", async: false
        });

        return isVehicleConfigurationPresent;
    };

    setSelectedOptions = function (items)
    {
        // TODO: make changes to handle only options through config filter.
        dynamicNavigationActive = false;
        var publisherConstants = mentor.publisher.constants;
        if (filterType === publisherConstants.TypeNOFilter) {//empty will be passed for resetting
            selectedOptions = '';
            if (vinFilterInstance) {
                vinFilterInstance.setTextExternally && vinFilterInstance.setTextExternally('');
            }
            filterOptionsForContentArea = '';
        }
        else if (filterType === publisherConstants.TypeVINFilter) {
            //passed argument will be options
            selectedOptions = items.substring(items.indexOf(publisherConstants.colonSeparator) +
                publisherConstants.colonSeparator.length);
            vinFilterInstance.setTextExternally(items);
            filterOptionsForContentArea = selectedOptions;
        }
        else if (filterType === publisherConstants.TypeConfigurationFilter) {
            //passed argument will be mathcing config array
            if (items.length === 1) {
                var options = items[0].value;
                selectedOptions = options;
                vinFilterInstance.setTextExternally &&
                vinFilterInstance.setTextExternally(items[0].name + publisherConstants.colonSeparator + options);
                filterOptionsForContentArea = selectedOptions;
            }
            else if (items.length > 1) {
                //ONLY FOR TYPE CONFIGURTAION AND VIN DYNAMIC FILTER CASE THIS WILL BE ARRAY{Please do not use it for anything else}
                selectedOptions = items;
                sortConfigurations(selectedOptions);
                dynamicNavigationActive = true;
            }
        }
    };

    setTextInStaticMode = function (items)
    {
        if (items.length === 1) {
            var options = items[0].value;
            selectedOptions = options;
            vinFilterInstance.setTextExternally(items[0].name + mentor.publisher.constants.colonSeparator +
                options);
            filterOptionsForContentArea = selectedOptions;
        }
    };

    /*
     Sort the configurations in the order from subset next superset and so on
     */
    sortConfigurations = function (configs)
    {
        var k, min, l;
        for (k = 0; k < configs.length - 1; k = k + 1) {
            min = k;
            for (l = k + 1; l < configs.length; l = l + 1) {
                if (isSubSet(configs[l], configs[min])) {
                    min = l;
                }
            }
            swap(k, min, configs);
        }
    };

    swap = function (one, two, array)
    {
        var temp;
        temp = array[one];
        array[one] = array[two];
        array[two] = temp;
    };

    /*
     is x configuration subset of y configuration?
     */
    isSubSet = function (x, y)
    {
        var xoptions = x.value, yoptions = y.value, xarray, yarray, j, subset = false;
        xarray = xoptions.split(',');
        yarray = yoptions.split(',');
        for (j = 0; j < xarray.length; j = j + 1) {
            if (yarray.indexOf(xarray[j]) !== -1 || yarray.indexOf(' ' + xarray[j]) !== -1) {
                subset = true;
            }
            else {
                subset = false;
                break;
            }

        }
        return subset;
    };

    return {
        setDynamicNavigationActive: function (active)
        {
            dynamicNavigationActive = active;
        },

        isDynamicNavigationActive: function ()
        {
            return dynamicNavigationActive;
        },

        /*
         This should only be used when it is known that we are in static mode,
         to set the text in the text box
         */
        setTextInStaticMode: setTextInStaticMode,

        getVINFilterInstance: function ()
        {
            return vinFilterInstance;
        },

        getSelectedOptions: function ()
        {
            return selectedOptions || "";
        },

        setSelectedOptions: setSelectedOptions,
        getVehicleConfigObject: function ()
        {
            if (!vehicleConfigObject) {
                vehicleConfigObject = new VehicleConfigObject();
            }
            return vehicleConfigObject;
        },
        setVehicleConfigObject: function (object)
        {
            vehicleConfigObject = object;
            if (object) {
                vehicleConfigObject.isDynamic = mentor.publisher.configurationsManager.isDynamicNavigationActive();
            }

        },
        setProjectFilteringType: function (type)
        {
            filterType = type;
        },
        getProjectFilteringType: function ()
        {
            if (!filterType) {
                filterType = mentor.publisher.constants.TypeNOFilter;
            }
            return filterType;
        },
        setLastVehicleConfigObject: function (obj)
        {
            lastVehicleConfigObject = obj;
            if (lastVehicleConfigObject) {
                lastVehicleConfigObject.isDynamic = mentor.publisher.configurationsManager.isDynamicNavigationActive();
            }

        },
        getLastVehicleConfigObject: function ()
        {
            return lastVehicleConfigObject;
        },
        loadVehicleConfiguration: xmlDataLoader(objectFactoryImpl()).loadVehicleConfigObject,
        getFilterOptionsForContentArea: function ()
        {
            return filterOptionsForContentArea;
        },
        hidePopup: hidePopup,
        hideOrShowConfigBuilderButton: isVehicleConfigXMLPresent,
        reset: reset
    };
}());