/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global define, Backbone, xmlDataLoader, objectFactoryImpl, mentor*/
define(["jquery", "underscore", "backbone", "models/SubPackage"],
        function ($, _, Backbone, SubPackage) {

            var SubPackages = Backbone.Collection.extend({
                model: SubPackage,
                url: "unzipped/data/packages.xml",

                fetch: function (options) {
                    _.extend(options, {
                        dataType : "text"
                    });
                    this.projectId = options.projectId;
                    return Backbone.Collection.prototype.fetch.call(this, options);
                },

                parse : function (data, options) {
                    this.projectId = options.projectId;
                    var doc = $.parseXML(data);
                    var packageElements = $('subpackage', doc);
                    var filteredPackageElements = _.filter(packageElements, function (element) {
                        return !this.projectId || $(element).parent().attr('projectId') === this.projectId;
                    }, this);

                    return _.map(filteredPackageElements, function (element) {
                        return {
                            id : $(element).attr('id'),
                            idAttribute: $(element).attr('id') + ':' + $(element).attr('range'),
                            name : $(element).parent().attr('name'), //+ ' (' + $(element).attr('range') + ')'
                            effectivityRange : $(element).attr('range'),
                            projectId: $(element).parent().attr('projectId'),
                            prefix: $(element).attr('prefix'),
                            start: $(element).attr('start'),
                            end: $(element).attr('end'),
                            parentPackageId: $(element).parent().attr('id'),
                            description: $(element).parent().attr('description').replace(/\\n/g,'\n').replace(/&nbsp;/g,'    ',)
                        };
                    });
                },

                toJSON: function(){
                    var subPackageArray = this.models
                    return {
                        packages: subPackageArray,
                        isEffectivityPackage: true
                    } ;
                },

                comparator: function(subPackage1, subPackage2){

                    function compareValues(value1, value2){
                        if( !isNaN(value1) ){
                            value1 = parseInt(value1, 10);
                            value2 = parseInt(value2, 10);
                        }
                        return value1 < value2 ? -1 : value1 > value2 ? 1 : 0;
                    }

                    var comparisonResult = 0;
                    var comparisonList = ['name', 'prefix', 'start'];

                    while(comparisonResult === 0 && comparisonList.length > 0){
                        var property = comparisonList.shift();
                        var value1 = subPackage1.get(property) && subPackage1.get(property).toLowerCase();
                        var value2 = subPackage1.get(property) && subPackage2.get(property).toLowerCase();

                        comparisonResult = compareValues(value1, value2);
                    }

                    return comparisonResult;
                }
            });

            return SubPackages;
        }
);