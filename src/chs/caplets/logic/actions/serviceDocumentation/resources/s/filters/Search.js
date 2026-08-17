function Search()
{
    "use strict";
    return function ()
    {
        return {
            //for UT
            indexSearchEnabled: false,
            //for UT
            setIndexBasedSearch: function (indexBasedSearch)
            {
                this.indexBasedSearch = indexBasedSearch;
            },
            //for UT
            setAttrBasedSearch: function (attrBasedSearch)
            {
                this.attrBasedSearch = attrBasedSearch;
            },
            getFilter: function (isEnabled)
            {
                var indexBasedSearch = this.indexBasedSearch;
                var attrBasedSearch = this.attrBasedSearch;
                if (isEnabled) {
                    return indexBasedSearch;
                }
                else {
                    return attrBasedSearch;
                }
            }
        };

    };
}