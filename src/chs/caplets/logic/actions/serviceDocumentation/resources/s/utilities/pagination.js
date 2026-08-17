define([], function ()
{
    return function (config)
    {
        config = config || {};
        return {

            itemsPerPage: config.objectsPerPage || 50,
            page: 1,
            totalObject: 50,

            getTotalPages: function ()
            {
                var length, totalLength;
                length = this.totalObjects;
                totalLength = length <= this.itemsPerPage ? 1 :
                        (length % this.itemsPerPage === 0 ? length / this.itemsPerPage :
                        (length / this.itemsPerPage) + 1);
                return parseInt(totalLength, 10);
            },

            getStartAndEnd: function ()
            {
                if (!this.totalObjects) {
                    return [0, this.itemsPerPage];
                }
                var totalItems = this.totalObjects, totalPages = this.getTotalPages(), start, end;
                start = (this.page - 1) * this.itemsPerPage;
                if (this.page === totalPages) {
                    end = totalItems;
                }
                else if (this.page < totalPages) {
                    end = this.page * this.itemsPerPage;
                }
                else {
                    return [];
                }
                return [start, end];
            },
            reset: function ()
            {
                this.page = 1;
                this.totalObjects = '';
            }
        };
    };
});