define(["ListView",
            "utilities/pagination",
            "filters/documentContentBasedFilter",
            "currentPackage"],
        function (ListView,
                pagination,
                documentContentBasedFilter,
                currentPackage)
        {
            return function (data)
            {
                return ListView(data).extend({
                    paginationDelegate: pagination(),
                    documentContentBasedFilter: documentContentBasedFilter,
                    currentPackage: currentPackage,
                    getDataForTemplate: function (options)
                    {
                        return {
                            page: this.paginationDelegate.page,
                            totalPages: this.paginationDelegate.getTotalPages(),
                            title: this.getTitle(),
                            items: this.getData().getModels(),
                            expand: options.expand,
                            totalItems: this.getData().totalObjects
                        };
                    },
                    useIndexedSearch: function ()
                    {
                        if (!data.usePlainSearch) {
                            return this.documentContentBasedFilter.areIndexesGenerated(
                                    this.currentPackage.get("id"));
                        }
                    },
                    setSearchIndexes: function (dataArr)
                    {
                        if (this.useIndexedSearch()) {
                            var indexes = this.documentContentBasedFilter.fetchSearchIndexes(
                                    data.searchModel.get("searchText"));
                            if (indexes) {
                                dataArr["indexes"] = indexes[this.type];
                            }
                        }
                    },
                    dataFetchFailed: function (config)
                    {
                        if (config.error) {
                            config.error();
                        }
                    },
                    dataDidFetch: function (data, config, message, loadedObjects)
                    {
                        data.afterDataLoad(loadedObjects);
                        this.paginationDelegate.totalObjects = loadedObjects.size;
                        if (config.success) {
                            config.success(this.getDataForTemplate(message));
                        }
                        this.inprogress = false;
                    },
                    fetchData: function (config)
                    {
                        var that = this;
                        if (!that.inprogress) {
                            that.inprogress = true;
                            var message = {
                                header: true,
                                expand: this.expanded
                            };
                            var range = this.paginationDelegate.getStartAndEnd();
                            message.start = range[0];
                            message.end = range[1];
                            message.method = "getItems";
                            message.options = mentor.publisher.filter && mentor.publisher.filter.vinOptions;
                            message.searchText = data.searchModel.get("searchText");
                            message.isIndexEnabled = this.useIndexedSearch();
                            this.setSearchIndexes(message);
                            var onsuccess = this.dataDidFetch.bind(this, data, config, message);
                            var onerror = this.dataFetchFailed.bind(this, config);
                            data.fetchData(message, {
                                success: onsuccess,
                                error: onerror
                            });
                        }
                    },
                    showNextPage: function (event)
                    {
                        this.paginationDelegate.page = this.paginationDelegate.page + 1;
                        this.paginate(event);
                    },

                    showPreviousPage: function (event)
                    {
                        this.paginationDelegate.page = this.paginationDelegate.page - 1;
                        this.paginate(event);
                    },
                    paginate: function (event)
                    {
                        this.reRender();
                        event.stopPropagation();
                    },
                    resetView: function (config)
                    {
                        config = config || {};
                        if (data && data.resetData) {
                            data.resetData(config);
                        }
                        this.paginationDelegate.reset();
                    }

                });
            }

        });