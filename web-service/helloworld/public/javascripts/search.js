var timer
var timerAdmins
var timeInterval = 250;
var $inputSearch = $('#search')

var authToken = document.cookie.replace(/(?:(?:^|.*;\s*)authToken\s*\=\s*([^;]*).*$)|^.*$/, "$1");
var feed1 = document.cookie.replace(/(?:(?:^|.*;\s*)feed1\s*\=\s*([^;]*).*$)|^.*$/, "$1");
var feed2 = document.cookie.replace(/(?:(?:^|.*;\s*)feed2\s*\=\s*([^;]*).*$)|^.*$/, "$1");
var feed3 = document.cookie.replace(/(?:(?:^|.*;\s*)feed3\s*\=\s*([^;]*).*$)|^.*$/, "$1");
var current_feed_id = feed1
var source_id;
var current_search_feed;
var current_your_feed;

if($(".feed1Title").length > 0)
    $(".feed1Title").text($('#feed1Title').text());

$inputSearch.on('keydown', function () {
    clearTimeout(timer);
});

$inputSearch.on('keyup', function () {
    clearTimeout(timer);
    timer = setTimeout(endedTyping, timeInterval);
});

function endedTyping () {
    socket.emit('Search', $("input#search").val())
}

$(window).click(function() {
    $('#searchResult').removeClass("transition visible");
    $('#searchUserResults').removeClass("transition visible");
});
  
$('#menucontainer').click(function(event){
    event.stopPropagation();
});

$(document).on("click", ".item.addSource", function(){

    if (typeof source_id !== 'undefined' && source_id !== null) {
        socket.emit('addSource2Composed', source_id, $(this).attr('id'));
    }

});

$(document).on("click", ".item.viewFeed", function(){
    console.log("Feed id: " + current_search_feed);
    console.log("Id html element: " + $(this).attr('id'));
    if (current_search_feed !== null) {
        switch($(this).attr('id')){
            case 'composed1':
                console.log('change viewport feed, current_search_feed' + current_search_feed);
                socket.emit('change viewport feed', authToken, current_search_feed, 1);
                //feed1 = current_search_feed;
                
                //console.log("*Feed1 ahora: " + feed1);
                
                //enviar evento que cambie la BD
                break;
            case 'composed2':
                socket.emit('change viewport feed', authToken, current_search_feed, 2);
                
                //feed2 = current_search_feed;
                // console.log("*Feed2 ahora: " + feed2);
                
                //enviar evento que cambie la BD
                break; 
            case 'composed3':
                socket.emit('change viewport feed', authToken, current_search_feed, 3);
                
                //feed3 = current_search_feed;
                // console.log("*Feed3 ahora: " + feed3);
        
                //enviar evento que cambie la BD
                break;
            default:
                console.log('default case')
                break;
        }
        
    }

});

$(document).on("click", ".result.source", function(){
    //console.log($(this).attr('id'));
    $('#s1').empty();
    console.log($(this).attr('id'));
    source_id = $(this).attr('id');
    socket.emit('selected source result', $(this).attr('id'), $(this).children().eq(1).text(), $(this).children().eq(2).text(), $('#feed1Title').text(), 
    $('#feed2Title').text(), $('#feed3Title').text() , 'Composed:'+feed1, 'Composed:'+feed2, 'Composed:'+feed3);
});

$(document).on("click", ".result.composed", function(){
    //console.log($(this).attr('id'));
    $('#s1').empty();
    composed_id = $(this).attr('id');
    console.log("(selected composed result)El feed seleccionado es " + composed_id);
    socket.emit('selected composed result', composed_id, $(this).children().eq(1).text(), $(this).children().eq(2).text(), $('#feed1Title').text(), 
    $('#feed2Title').text(), $('#feed3Title').text() , 'Composed:'+feed1, 'Composed:'+feed2, 'Composed:'+feed3);
});

socket.on('user feed viewport updated', function() {
    location.reload();
});

socket.on('search result', function(composed_result, source_result) {

    //if( $('.result').length )
    $('.result').remove();

    $('#searchResult').addClass( "transition visible" );
    //console.log("client recieved searchResult!!!: " + composed_result.length);
    for (let i = 0; i < source_result.length && i < 5; i++) {
        //console.log(composed_result[i]);
        $('#searchResult').append(getMenuItemSource(source_result[i]));
    }
    
    for (let i = 0; i < composed_result.length && i < 5; i++) {
        //console.log(composed_result[i]);
        $('#searchResult').append(getMenuItemComposed(composed_result[i]));
    }
    
});

socket.on('selected search item result', function(html_element, composed_id) {
    //console.log("El html que me ha mandao el server: " + html_element);
    console.log("(respuesta selected search item result) composed_id = " + composed_id);
    current_search_feed = composed_id
    console.log("(respuesta selected search item result) current_search_feed = " + current_search_feed);
    $('#selectedItemResultContainer').append(html_element);
});

function getMenuItemSource(result)
{
    return '<div id=\"' + result.id + '\" class=\"result source\"> <div class="ui horizontal label"> Source </div> <div>' + result.Title + '</div> <div> ' + result.Description + '</div></div>'
}



function getMenuItemComposed(result)
{
    return '<div id=\"' + result.id + '\"  class=\"result composed\">  <div class="ui horizontal label"> Feed </div> <div>' + result.Name + '</div> <div>' + result.Description + '</div></div>'
}


// -------------------------- user search inside edit admin list ---------------------------------------------
$('.editAdminList.item').click( function(){
    current_your_feed = $(this).parent().parent().attr('id');
    console.log('current your feed has changed to: ' + current_your_feed);
});


var $inputSearchUser = $('#searchuser')

$inputSearchUser.on('keydown', function () {
    clearTimeout(timerAdmins);
});

$inputSearchUser.on('keyup', function () {
    clearTimeout(timerAdmins);
    timerAdmins = setTimeout(endedTypingUser, timeInterval);
});

function endedTypingUser () {
    // console.log('Entra');
    // console.log('Has escrito: ' + $inputSearchUser.val());
    socket.emit('Search user', $inputSearchUser.val());
}

socket.on('result user search', function(html_results) {
    // console.log('entra result user search' + html_results)
    $('.result').remove();
    $('#searchUserResults').addClass( "transition visible" );
    $('#searchUserResults').append(html_results);
})

$(document).on("click", ".addAdmin", function(){
    username = $(this).parent().parent().children().eq(0).children().eq(0).text();
    console.log("username selected: " + username);
    console.log("composed_id sent: " + current_your_feed);
    socket.emit('add user to feed admins', username, current_your_feed, authToken);
});