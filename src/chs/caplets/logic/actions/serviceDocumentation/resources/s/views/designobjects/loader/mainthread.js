define([], function ()
{
    return function (config)
    {
        config = config || {};
        var dataLoaderFactory = config.dataLoaderFactory || createDataLoader;
        return {
            dataLoader: dataLoaderFactory(),
            ajax: function (url, callback, async)
            {

                if (url) {
                    $.ajax({
                        url: url,
                        async: async,
                        success: function (data, textStatus, XMLHttpRequest)
                        {
                            setTimeout(function ()
                            {
                                callback(XMLHttpRequest.responseText);
                            }, 100);
                            
                        },
                        error: function (XMLHttpRequest, textStatus, errorThrown)
                        {
                            //alert("failed to load url " + url);
                        },
                        dataType: "json"
                    });
                }
            },
            initializeDataLoader: function ()
            {
                this.dataLoader.ajax = this.ajax;
                this.dataLoader["init"](["init", Utils.prepareFilePath(this.url)]);
            },
            initialize: function (config)
            {
                config = config || {};
                if (config.url) {
                    this.url = Utils.prepareFilePath(config.url)
                    this.initializeDataLoader();
                }
            },

            dataLoadDidFinish: function (timeout, payload)
            {
                setTimeout(this.onmessage.bind(this, payload), timeout);
            },

            executeMethodOndataLoader: function (config)
            {
                var timeout = config.timeout || 100;
                var methodName = this.parameters.method;
                var methodArguments = config.parameters;
                var dataLoadDidFinish = this.dataLoadDidFinish.bind(this, timeout)
                this.dataLoader[methodName](methodName, methodArguments, dataLoadDidFinish);
            },
            execute: function (config)
            {
                config = config || {};
                if (config.method) {
                    this.parameters = {};
                    this.parameters.method = config.method;
                    this.parameters.success = config.success;
                    this.parameters.error = config.error;
                    if (this.parameters.method) {
                        this.executeMethodOndataLoader(config);
                    }
                }
            },
            reset: function ()
            {
                this.dataLoader["reset"]();
                this.initializeDataLoader();
            },
            onmessage: function (e)
            {
                var payload = e;
                if (payload.length > 0 && payload[0] === this.parameters.method) {
                    var data = payload[1];
                    if (this.parameters && this.parameters.success) {
                        this.parameters.success(data);
                    }
                }
                else {
                    if (this.parameters && this.parameters.error) {
                        this.parameters.error();
                    }
                }
            }
        }
    }
});