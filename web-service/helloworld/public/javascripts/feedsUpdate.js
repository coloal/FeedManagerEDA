var socket = io('/feeds');
var cookieToken = document.cookie.replace(/(?:(?:^|.*;\s*)authToken\s*\=\s*([^;]*).*$)|^.*$/, "$1");


//console.log("Feeds: " + feed1 + " " + feed2 + " " + feed3 + " ");
socket.on('connect', function() {
    socket.emit('join', cookieToken);
});

socket.on('feed message', function(msg, feed_id) {
    // console.log('está entrando!!!!!!!!!!!!!!!!!!!!');
    // $('#f1').prepend('<div class="event"> <div class="ui fluid centered card"> <div class="content"> <div class="summary"> <a class="user"> ElPais </a> <div class="date"> 1 hour ago </div> <h3 class="extra text">' + msg + '</h3><p>Está previsto que se realice una nueva operación de reflotamiento este domingo a media tarde </p> <div class="meta"><a class="like">Ir</a> <a class="like">Contagiar</a> <a class="like">Like</a> </div> </div> </div> </div> </div>');
    console.log('New message with feed_id: ' + feed_id)
    console.log('feed1 id = ' + feed1);
    console.log('feed2 id = ' + feed2);
    console.log('feed3 id = ' + feed3);
    if(feed_id == feed1)
        $('#f1').prepend(msg);
    if(feed_id == feed2)
        $('#f2').prepend(msg);
    if(feed_id == feed3)
        $('#f3').prepend(msg);
        
});

$("#registerSource").submit( function(event) {
    event.preventDefault();
    socket.emit('RegisterSource', $("input#source_uri").val());
});

$("#addSource2Composed").submit( function(event) {
    event.preventDefault();
    socket.emit('AddSource2Composed');
});

$("#left-column").on('click', '.stopFollowingSource', function() {
    source_id = $(this).attr('id');
    console.log("current composed feed id: " + current_feed_id)
    socket.emit('stopFollowingSource', current_feed_id, source_id);
});

$(".newFeed").submit( function(event) {
    event.preventDefault();
    socket.emit('CreateFeed', $(this).attr('id'), $(this).find('input[name="title"]').val(), $(this).find('input[name="description"]').val(), cookieToken);
    $(this).addClass("loading");
});

$(".feedbutton").click( function() {
    btn_feedid = $(this).attr('id');
    switch(btn_feedid){
        case 'btnfeed1':
            socket.emit('selected feed', feed1);
            current_feed_id = feed1;
            break;
        case 'btnfeed2':
            socket.emit('selected feed', feed2);
            current_feed_id = feed2;
            break;
        case 'btnfeed3':
            socket.emit('selected feed', feed3);
            current_feed_id = feed3;
            break;
    }
});



socket.on('answer selected feed', function(html_element, new_selected_feedID) {
    
    //console.log("El html que me ha mandao el server: " + html_element);

    $('#feedinfo').remove();
    $('#description').remove();
    $('#feedfollows').remove();
    
    $('#left-column').prepend(html_element);
})

socket.on('FeedCreated', function(newFeed) {
    
    console.log(newFeed);
    window.location.replace(window.location.href);

});

// Modal:

//now attach event?
$('document').ready(function(){
    
    $('.editAdminList.item').click( function() {
        $('.ui.special.modal').modal();
        $('.ui.special.modal').modal('show');
    
        composed_id = $(this).parents().eq(1).attr('id');
        socket.emit('request composed admin list', composed_id);
    });

});


socket.on('response composed admin list', function(html_admin_list) {
        $('.ui.relaxed.divided.list').empty();
        $('.ui.relaxed.divided.list').prepend(html_admin_list);
    }
);

/*
$('.ui.label.deleteAdmin').click( function() {
    console.log('El id: ' + current_feed_id);
    console.log('El username: ' + $(this).siblings('.ui.label.deleteAdmin').first().text());
});
*/

$(".ui.special.modal").on('click', '.ui.label.deleteAdmin', function() {
    var username = $(this).parent().parent().children().eq(1).text();

    console.log('Username: ' + username);
    console.log('Current feed id: ' + current_your_feed);
    console.log('authToken: ' + authToken);
    socket.emit('request delete admin', username, current_your_feed, authToken);

});