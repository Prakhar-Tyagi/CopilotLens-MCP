/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Backbone, mentor*/
define(["currentPackage",
            "views/designobjects/loader/webworker",
            "views/designobjects/loader/mainthread"],
        function (currentPackage,
                dataLoaderUsingWorker,
                dataLoaderMainThread)
        {
            "use strict";
            var designObjModel = Backbone.Model.extend();
            return function (config)
            {
                config = config || {};
                return Backbone.Collection.extend({
                    Model: designObjModel,
                    currentPackage: currentPackage,
                    searchModel: config.searchModel || currentPackage,
                    dataLoader: "",
                    mainthread: config.mainthread || dataLoaderMainThread,
                    worker: config.worker || dataLoaderUsingWorker,
                    url: "",
                    type: "",
                    usePlainSearch: config.usePlainSearch,
                    inProgress: false,
                    requestQueue: [],
                    getModels: function ()
                    {
                        return this.models;
                    },

                    doFetchData: function(parameters, callback) {
                        this.params = parameters;
                        this.callback = callback;

                        this.inProgress = true;
                        this.dataLoader.execute({
                            method: parameters.method,
                            parameters: this.params,
                            success: this.ondataload.bind(this),
                            error: this.onRequestFail.bind(this)
                        });
                    },

                    fetchData: function (parameters, callback)
                    {
                        // fetch data only if no existing call is in Progress
                        if (!this.inProgress) {
                            this.doFetchData(parameters, callback);
                        }
                        // queue the call otherwise
                        else {
                            this.requestQueue.push({
                                params: parameters,
                                cb: callback
                            });
                        }
                    },
                    filter: function (items)
                    {
                        return items;
                    },
                    afterDataLoad: function (loadedObjects)
                    {
                        this.totalObjects = loadedObjects.size;
                        this.reset(this.filter(loadedObjects.items), {silent: true});
                    },

                    handlePendingRequests: function () {
                        if (this.requestQueue.length > 0) {
                            var nextReq = this.requestQueue.shift();
                            this.doFetchData(nextReq.params, nextReq.cb);
                        }
                    },

                    ondataload: function (data)
                    {
                        this.options = data;
                        if (this.callback && this.callback.success) {
                            this.callback.success(data);
                        }
                        this.inProgress = false;
                        this.handlePendingRequests();
                    },

                    onRequestFail: function() {
                        if (this.callback && this.callback.error) {
                            this.callback.error();
                        }
                        this.inProgress = false;
                        this.handlePendingRequests();
                    },

                    workerScript: "s/worker.js",
                    getDataURL: function ()
                    {
                        if (config.getUrl) {
                            return config.getUrl();
                        }
                        return this.currentPackage.get("id") + "/" + this.type + ".json";
                    },
                    useWorkerToFetchDataInASeparateThread: function (params)
                    {
                        params = params || {};
                        var href = params.href || window.location.href;
                        //for file:// urls, dont use web workers
                        //following check is to know it URL is http based
                        var pat = /^https?:\/\//i;
                        if (pat.test(href)) {
                            return !config.useSameThreadToLoad && window.Worker;
                        }
                        else {
                            return false;
                        }
                    },
                    initDataLoader: function ()
                    {
                        this.dataLoader.initialize({url: this.getDataURL()}, this.workerScript);
                    },
                    initialize: function ()
                    {
                        if (this.useWorkerToFetchDataInASeparateThread()) {
                            this.dataLoader = this.worker && this.worker() || dataLoaderUsingWorker();
                        }
                        else {
                            this.dataLoader = this.mainthread && this.mainthread() || dataLoaderMainThread();
                        }
                        this.initDataLoader();
                    },
                    resetData: function (config)
                    {
                        config = config || {};
                        if (config.resetData) {
                            this.dataLoader.reset(config);
                        }

                        this.initDataLoader();
                    }
                });
            };

        });
