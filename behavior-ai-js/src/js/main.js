$(document).ready(function() {
    $('*').on("click mousedown mouseup focus blur keydown change",function(e){
        console.log(e);
    });
});