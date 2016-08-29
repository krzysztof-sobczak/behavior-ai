$(document).ready(function() {
    pushTrackingData();
});

function getTrackingId()
{
    var trackingCookieName = 'behavior-ai-session';
    var trackingCookieValue = getCookie(trackingCookieName);
    if(trackingCookieValue == null) {
        trackingCookieValue = uid();
        setCookie(trackingCookieName, trackingCookieValue, 30);
    }
    return trackingCookieValue;
}

function pushTrackingData()
{
    var trackingId = getTrackingId();
    var data = {
        "tracking_id": trackingId,
        "path": window.location.pathname,
        "agent": navigator.userAgent,
        "timestamp": (new Date()).toISOString()
    };

    var dataJson = JSON.stringify(data);
    console.log('Sending: ' + dataJson);

    var settings = {
      "async": true,
      "crossDomain": true,
      "url": "http://localhost:8080/",
      "method": "PUT",
      "headers": {
        "content-type": "application/json"
      },
      "processData": false,
      "data": dataJson
    }

    $.ajax(settings).done(function (response) {
        console.log('Tracking data pushed:' + response);
    });
}

function uid(){
  return randomChars(8) +
    '-' + randomChars(4) +
    '-' + randomChars(4) +
    '-' + randomChars(4) +
    '-' + randomChars(12)
}

function randomChars(length){
    return Math.random().toString(16).slice(-length);
}

function setCookie(name,value,minutes) {
    if (minutes) {
        var date = new Date();
        date.setTime(date.getTime()+(minutes*60*1000));
        var expires = "; expires="+date.toGMTString();
    }
    else var expires = "";
    document.cookie = name+"="+value+expires+"; path=/";
}

function getCookie(name)
{
    var nameEQ = name + "=";
    var ca = document.cookie.split(';');
    for(var i=0;i < ca.length;i++) {
        var c = ca[i];
        while (c.charAt(0)==' ') c = c.substring(1,c.length);
        if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
    }
    return null;
}
