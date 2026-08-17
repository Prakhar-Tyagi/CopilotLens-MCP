/* globals $ */

function filterReportDOM(dom, shouldFilterOptionExpression)
{
	$("tr:has(td[data-accumulatecontent='qty'])", $(dom)).each(adjustQuantityColumn);
	$(".clickable-column", $(dom)).closest("tr").each(filterClickableColumn);
	$(".clickable-span", $(dom)).closest("tr").each(filterClickableSpan);
	$(".clickable-multivalued", $(dom)).closest("tr").each(filterClickableMultivalued);
	
	function filterClickableColumn()
	{
		var filtered = false;
        
        $("td[class='clickable-column'] span", $(this)).each(function ()
        {
			filtered = shouldFilterElement(this);
			if (filtered) {
				return false;
			}
        });	
        
        if (filtered) {
            $(this).remove();
        }
	}
	
	function filterClickableSpan()
	{
		var filtered = false;
        
        $("span[class='clickable-span']", $(this)).each(function ()
        {
			filtered = shouldFilterElement(this);
			if (filtered) {
				return false;
			}
        });	
        
        if (filtered) {
            $(this).attr('style', 'display:none');
        }
	}
	
	function filterClickableMultivalued()
	{
		var filteredObjectCount = 0, 
            spanElements = $("span", this), 
            totalObjectCount = spanElements.length;
            
        if (totalObjectCount == 0) {
			return;
		}
		
		spanElements.each(function ()
		{
			if (shouldFilterElement(this)) {
				filteredObjectCount = filteredObjectCount + 1;
			}
		});

		if (totalObjectCount === filteredObjectCount) {
			$(this).attr('style', 'display:none');
		}
	}
	
	function adjustQuantityColumn()
    {
		var nextRow = $(this).next(), 
			currentRow = $(this), 
			totalElements, 
			remainingElements;
		/**
			* for AssemblyBOM Reports content is nested in next row
			*/
		if ($(nextRow).attr("class") === "nestedTableRow") {
			totalElements = -1;
			$("tr:has(td[data-accumulatecontent='qty'])", nextRow).each(function ()
			{
				if (totalElements === -1) {
					totalElements = 0;
				}
				remainingElements = filterQuantityColumn(this);
				if (remainingElements >= 0) {
					totalElements = remainingElements;
				}
				else {
					totalElements = -1;
				}
			});
			//all elements got filtered
			if (totalElements === 0) {
				$(nextRow).attr('style', 'display:none');
				$(currentRow).attr('style', 'display:none');
			}
		}
		else {
			filterQuantityColumn(this);
		}
    }
	
	function filterQuantityColumn(rowElement)
    {
        var filteredObjectCount = 0,
            spanElements = $("td>span", rowElement), 
            totalObjectCount = spanElements.length;
            
        if (totalObjectCount === 0) {
			return -1;
        }
        
        spanElements.each(function ()
        {
            var switchOff = shouldFilterElement(this);
            if (switchOff) {
                filteredObjectCount = filteredObjectCount + 1;
            }
        });
        
        if (filteredObjectCount !== 0 && (filteredObjectCount === totalObjectCount)) {
            $(rowElement).attr('style', 'display:none');
        }
		else {
			$("td[data-accumulatecontent='qty']", rowElement).each(function ()
			{
				var originalCount = $(this).html().trim();
				$(this).html((originalCount - filteredObjectCount));
			});
		}
        
        return (totalObjectCount - filteredObjectCount);
    };
	
	function shouldFilterElement(element) 
	{
		var optionExpression = parseOptionExpression();
		return shouldFilterOptionExpression(optionExpression);
		
		function parseOptionExpression()
		{
			var optionExpression = '', 
					spanid, 
					components;
					
			spanid = $(element).attr('id');
			if (typeof (spanid) !== "undefined" && spanid !== null && spanid.trim() !== "") {
				components = spanid.split('$');
				if (components.length === 2) {
					optionExpression = Utils.stripX(components[1]);
					$(element).attr('id', (typeof (components[0]) !== "undefined" ? components[0].replace("$", "") : ""));

				}
			}
			
			return optionExpression;
		}
	}
}