onmessage = function (e)
{
    var payload = e.data;
    logMessage(payload);
    var method = payload[0];
    var options = payload[1];
    logMessage(payload);
    getDataLoader(method)(payload, options, postMessage);
};

function send(data)
{
    postMessage(data);
}
