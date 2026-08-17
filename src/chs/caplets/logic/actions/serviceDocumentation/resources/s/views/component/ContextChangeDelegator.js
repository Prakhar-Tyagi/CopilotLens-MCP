define(["currentPackage"], function (currentPackage)
{
    return function ()
    {
        return {
            deletate: null,
            initialize: function ()
            {
                currentPackage.on("change:id", this.packageChanged, this);
                //this is for both VIN and config filtering
                currentPackage.on("change:vin", this.optionFilterApplied, this);
                currentPackage.on("change:searchText", this.searchTextApplied, this);
                currentPackage.on("change:language", this.languageChanged, this);
                currentPackage.on("collapseAll", this.collapseAll, this);
                currentPackage.on("expandAll", this.expandAll, this);
                this.componentDidInitialize();
            },
            packageChanged: function ()
            {
                if (this.deletate && this.deletate.packageChanged) {
                    this.deletate.packageChanged();
                }

            },
            languageChanged: function ()
            {
                if (this.deletate && this.deletate.languageChanged) {
                    this.deletate.languageChanged();
                }

            },
            optionFilterApplied: function ()
            {
                if (this.deletate && this.deletate.optionFilterApplied) {
                    this.deletate.optionFilterApplied();
                }
            },

            searchTextApplied: function ()
            {
                if (this.deletate && this.deletate.searchTextApplied) {
                    this.deletate.searchTextApplied();
                }
            },
            componentDidInitialize: function ()
            {
                if (this.deletate && this.deletate.componentDidInitialize) {
                    this.deletate.componentDidInitialize();
                }
            }
        };

    }

});

