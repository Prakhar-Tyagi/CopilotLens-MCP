/*global define, mentor*/
define("jt3DModel", ['underscore', "backbone", "base3DModel", "currentPackage"],
        function (_, Backbone, Base3DModel, selectedPackage) {
            "use strict";
            var JT3DModel = Base3DModel.extend({
                defaults: {
                    map: {}
                },
                initialize: function () {
                    selectedPackage.on("change:id", this.resetModel.bind(this));
                },
                loadSystemPaths: function (oPsid, name, systemPaths) {
                    var _data = {}, syss = [], optExprs = [];
                    _.foldl(systemPaths, this.parseSysPathData, syss)
                    _.each(syss, function (sys) {
                        optExprs.push(sys.optionExpression);
                    });
                    _data.systems = syss;
                    _data.optionExpression = optExprs;
                    this.get('map')[name + "_" + oPsid] = _data;
                },
                resetModel: function () {
                    this.set('map', {});
                },
                loadDataForJTPart: function (oPsid, partName, modelName) {
                    var data, systemPaths, that = this, loadedData;
                    loadedData = this.get("map")[partName + "_" + oPsid];
                    if (loadedData) {
                        return this.filterData(loadedData);
                    }
                    $.ajax({
                        url: Utils.prepareFilePath("./" + modelName + "/psidMap.json"),
                        dataType: 'json',
                        async: false,
                        data: data,
                        success: function (data) {
                            systemPaths = data["psidMap"][oPsid];
                            that.loadSystemPaths(oPsid, partName, systemPaths);
                        }
                    });
                    return this.filterData(this.get('map')[partName + "_" + oPsid]);
                }
            }), jtModel;
            jtModel = new JT3DModel();
            return {
                getSystemsForJTPart: function (oPsid, partName, modelName) {
                    return jtModel.loadDataForJTPart(oPsid, partName, modelName);
                }
            }
        });