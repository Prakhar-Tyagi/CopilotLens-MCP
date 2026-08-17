define([], function ()
{
    return function ()
    {
        return {
            initializeDataLoader: function ()
            {
                this.dataLoader.postMessage(["init", Utils.prepareFilePath(this.url)]);
            },
            onerror: function (e)
            {
                if (window.console) {
                    window.console.log(e);
                }
            },
            initialize: function (config, workerScript)
            {
                config = config || {};

                if (config.url && workerScript && !this.dataLoader) {
                    var WorkerCons = config.Worker || Worker;
                    this.url = Utils.prepareFilePath(config.url);
                    this.dataLoader = new WorkerCons(workerScript);
                    this.dataLoader.onerror = config.onerror || this.error;
                    this.dataLoader.onmessage = config.onmessage || this.onmessage.bind(this);
                }
                if (this.dataLoader) {
                    this.url = config.url.replace("\\", "/");
                    this.initializeDataLoader()
                }
            },

            execute: function (config)
            {
                config = config || {};
                if (config.method) {
                    this.inprogress = true;
                    this.parameters = {};
                    this.parameters.method = config.method;
                    this.parameters.success = config.success;
                    this.parameters.error = config.error;
                    this.dataLoader.postMessage([this.parameters.method, config.parameters])
                }
            },
            reset: function ()
            {
                if (!this.inprogress) {
                    this.dataLoader.postMessage(["reset"]);
                    this.initializeDataLoader();
                }
            },
            onmessage: function (e)
            {
                var payload = e.data || [];
                this.inprogress = false;
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
})