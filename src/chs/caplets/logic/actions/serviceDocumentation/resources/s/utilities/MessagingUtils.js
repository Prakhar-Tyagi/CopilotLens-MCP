
function showError(errorMessage) {
    require(["text!templates/ErrorTemplate.html"], function(template){
        var renderedTemplate = _.template(template)({
            errorMessage: errorMessage
        });

        $("body").html(renderedTemplate);
    });
}
