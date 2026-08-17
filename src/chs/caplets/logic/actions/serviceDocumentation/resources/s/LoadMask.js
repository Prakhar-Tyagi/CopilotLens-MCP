var LoadingMaskCreator = function ()
{
    return {
        addLoadMask: function (svgContainer)
        {
            this.removeSVGMask();
            this.removeLoadMask();
            this.showLoadingRing(svgContainer);
        },
        showLoadingRing: function (svgContainer) {
            var container = $('#' + svgContainer);
            if (!Utils.notNull($(container.parent())) || !Utils.notNull($(container.parent()).offset())) {
                return;
            }
            var height = $(container.parent()).height();
            var width = $(container.parent()).width();
            var left = $(container.parent()).offset().left;
            var top = $(container.parent()).offset().top;
            var loadMaskDiv = $('<div id="LoadMask" class="LoadMask">');
            loadMaskDiv.css('top', top + 'px');
            loadMaskDiv.height(height);
            loadMaskDiv.width(width);
            $(container.parent()).append(loadMaskDiv);
        },
        addLoadingRingOnPackageOpen: function (){
            $("#home-screen").hide();
            var container = $('#eualContainer');
            container.show();
            if (!Utils.notNull($(container.parent())) || !Utils.notNull($(container.parent()).offset())) {
                return;
            }
            this.removeLoadMask();
            var loadMaskDiv=$('<div id="LoadMask" class="LoadMask" style="height:300px;padding-left:0"></div>');
            $(container).append(loadMaskDiv);
        },
        removeLoadingRingAfterPackageLoad: function () {
            $('#eualContainer').hide();
            LoadMask.removeLoadMask();
        },

        LoadSVGMask: function (svgContainer) {
            var container = $(svgContainer);
            this.removeSVGMask();
            this.removeLoadMask();
            if (!Utils.notNull($(container.parent())) || !Utils.notNull($(container.parent()).offset())) {
                return;
            }
            var height = $(container).height() - $("#splitter1>.toolbar").height() -
                    $(mentor.publisher.contentPanel.horizontalSeparator).height();
            var width = $(container).width();
            var left = $(mentor.publisher.contentPanel.containerSelector).offset().left + $(".iesdResizeBar").width();
            var top = $(mentor.publisher.contentPanel.containerSelector).offset().top +
                    $("#splitter1>.toolbar").height();
            var loadMaskDiv = $('<div id="LoadSVGMask" class="LoadMask">');
            loadMaskDiv.css('left', left + 'px');
            loadMaskDiv.css('top', top + 'px').css('background-color', "#ffffff").css("opacity", "1");
            loadMaskDiv.height(height);
            loadMaskDiv.width(width);
            $("body").append(loadMaskDiv);
        },

        removeSVGMask: function ()
        {
            $('#LoadSVGMask').remove();
        },

        removeLoadMask: function ()
        {
            $('#LoadMask').remove();
        },

        addErrorMaskClass: function ()
        {
            $('#LoadMask').removeClass();
            $('#LoadMask').addClass('LoadErrorMask');
        }
    };

};

var LoadMask = LoadingMaskCreator();
function doNothingAndStopEventPropagation(event)
{
    event.stopPropagation();
    return;
};
var AlertForErrors = function ()
{

    this.createMessageBody = function ()
    {
        $('#popup_container').remove();
        this.alertMsg = $('<div id="popup_container">' +
                //                          '<h1 id="popup_title"></h1>' +
                '<div id="popup_content">' +
                '<div id="popup_message"></div>' +
                '</div>' +
                '</div>');
        $('#detail').on("click", function ()
        {
            $('#popup_container').remove();
        });

        $('#navigation').on("click", function ()
        {
            $('#popup_container').remove();
        });

        $('.renderConnectivityBtn').on("click", doNothingAndStopEventPropagation);

        $('#applicationArea').append(this.alertMsg);
    }

    this.init = function (message, cssClass)
    {
        if (typeof(popup) != "undefined") {
            //hide all other popups if visible
            popup.hidePopup();
        }

        this.createMessageBody();

        //        $('#popup_title').html(msgHeader);
        $('#popup_message').html(message);
        $('#popup_content').addClass(cssClass);

        var topVal = ($(window).height() / 2) - 50;
        var leftVal = ($(window).width() / 2) - 200;
        $('#popup_container').css('top', topVal + "px");
        $('#popup_container').css('left', leftVal + "px");

    };

    this.showMessageWithLoadingImage = function (message, cssClass)
    {
        this.createMessageBody();
        $('#popup_message').addClass(cssClass);
        $('#popup_message').html(message);
        $('#popup_content').addClass(cssClass);
        var topVal = ($(window).height() / 2) - 50;
        var leftVal = ($(window).width() / 2) - 200;
        $('#popup_container').css('top', ($(window).height() / 2) - 50);
        $('#popup_container').css('left', ($(window).width() / 2) - 200);
    }

    this.removeAlertMsg = function ()
    {
        /**
         * we need to remove the ealier assigned event, otherwise signal tracer does not work
         */
        $('.renderConnectivityBtn').off("click", doNothingAndStopEventPropagation);
        $('#popup_container').remove();
    };

};

var alertMsg = new AlertForErrors();