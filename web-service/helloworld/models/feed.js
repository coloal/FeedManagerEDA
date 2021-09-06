//Set up redis connection
const ioredis = require("ioredis");
const { response } = require("express");

// const  source_redis_client = new ioredis({host: '127.0.0.1', port: '6380', password: process.env.SOURCE_PASSWORD});
const  source_redis_client = new ioredis({host: 'source-release-redis-master.redis.svc.cluster.local', port: '6379', password: process.env.SOURCE_PASSWORD});

// const  composed_redis_client = new ioredis({host: '127.0.0.1', port: '6379', password: process.env.COMPOSED_PASSWORD});
const  composed_redis_client = new ioredis({host: 'composed-release-redis-master.redis.svc.cluster.local', port: '6379', password: process.env.COMPOSED_PASSWORD});

composed_redis_client.on('connect', function() {
    console.log('connected');
});

async function getComposed(searchText) {
    var passed2redisearch = "\"" + searchText + "\"";
    var passed2redisearch = searchText;
    //console.log("++++++++++++++EL COMANDO PASADO A REDISEARCH: " + passed2redisearch);
    answer = await composed_redis_client.call('ft.search', 'composed-index', passed2redisearch);

    // console.log("Raw: " + answer);
    nAnswers = answer[0];
    
    res = answer.slice(1);
    
    arrResult = [];
    //console.log("-----------------------------");
    ///console.log("After slice: " + res);
    for(var i=0; i<nAnswers; i++){
        arrOneRes = res[2*i+1];
        //console.log("-----------------------------");
        //console.log("arrOneRes= " + arrOneRes);
        //console.log("-----------------------------");
        composed = { id: res[2*i] };
        for(var j=0 ; j<=arrOneRes.length-2 ; j+=2)
            composed[arrOneRes[j]] = arrOneRes[j+1]; 

        //console.log(composed);
        arrResult.push(composed);
    }
    //console.log("-----------------------------");
    //console.log(arrResult);
    
    return arrResult;
}

async function getSource(searchText) {
    
    var passed2redisearch = searchText;
    //console.log("++++++++++++++EL COMANDO PASADO A REDISEARCH: " + passed2redisearch);
    answer = await source_redis_client.call('ft.search', 'source-index', passed2redisearch);

    //console.log("Raw: " + answer);

    nAnswers = answer[0];
    res = answer.slice(1);

    arrResult = [];

    for(var i=0; i<nAnswers; i++){

        arrOneRes = res[2*i+1];
        
        //source = { id: res[2*i] }; ya viene desde redis en el value
        source = {};
        for(var j=0 ; j<=arrOneRes.length-2 ; j+=2)
            source[arrOneRes[j]] = arrOneRes[j+1]; 
            
        source['id'] = "Source:" + source['id'];

        //console.log(source);
        arrResult.push(source);
       
    }

    return arrResult;
}

async function getComposed(searchText) {
    var passed2redisearch = "\"" + searchText + "\"";
    var passed2redisearch = searchText;
    //console.log("++++++++++++++EL COMANDO PASADO A REDISEARCH: " + passed2redisearch);
    answer = await composed_redis_client.call('ft.search', 'composed-index', passed2redisearch);

    // console.log("Raw: " + answer);
    nAnswers = answer[0];
    
    res = answer.slice(1);
    
    arrResult = [];
    //console.log("-----------------------------");
    ///console.log("After slice: " + res);
    for(var i=0; i<nAnswers; i++){
        arrOneRes = res[2*i+1];
        //console.log("-----------------------------");
        //console.log("arrOneRes= " + arrOneRes);
        //console.log("-----------------------------");
        composed = { id: res[2*i] };
        for(var j=0 ; j<=arrOneRes.length-2 ; j+=2)
            composed[arrOneRes[j]] = arrOneRes[j+1]; 

        //console.log(composed);
        arrResult.push(composed);
    }
    //console.log("-----------------------------");
    //console.log(arrResult);
    
    return arrResult;
}

async function postComposed(name_, description_, owner) {
    newComposedId = await composed_redis_client.incr("composed:id:");
    valid = await composed_redis_client.hmset("Composed:" + newComposedId, {
        Name: name_,
        Description: description_,
        nPosts: '0',
        nFollowing: '0',
        owner: owner,
        id: newComposedId
    })

    valid2 = await composed_redis_client.sadd("AdminsList:" + newComposedId, owner);

    return newComposedId;
}

async function getMyFeedsInfo(arrComposedIds) {
   
    var arrAsyncComposedInfos = [];
    arrComposedIds.forEach( (composed_id) => arrAsyncComposedInfos.push( getComposedInfo(composed_id) ) );
    
    var arrMyFeedsInfo = await Promise.all( arrAsyncComposedInfos );

    return arrMyFeedsInfo;
}

async function getComposedInfo(composedId) {
    
    return composed_redis_client.hgetall("Composed:" + composedId);
}

async function getSourceInfo(sourceId) {
    return source_redis_client.hgetall("Source:" + sourceId);
}

async function getTenSourceMessages(sourceId) {
    return source_redis_client.zrevrange("SourceFeed:" + sourceId, 0, 10);
}

async function getTenMessages(composedId) {
    return composed_redis_client.zrevrange("ComposedFeed:" + composedId, 0, 10);
};

async function getMessage(id) {
    return source_redis_client.hgetall("Item:" + id);
}

async function getMessages(arrIds) {
    const pipeline = source_redis_client.pipeline();
    arrIds.forEach( key => {
        pipeline.hgetall("Item:"+key);
    });
        
    return pipeline.exec();
}

async function getLastTenSourceMessages(sourceId) {
    try{
        const itemIds = await getTenSourceMessages(sourceId);
        console.log(itemIds);
        return getMessages(itemIds);
    } catch(errorReason){
        console.log(errorReason);
    }    
}

/*
async function getLastTenMessages(composedId) {
    try{
        // console.log("Entra en getLastTenMessages con composed_id: " + composedId);
        const itemIds = await getTenMessages(composedId);

        arrMessages = await getMessages(itemIds);
        for(let i=0; i < arrMessages.length; i++)
        {
            sourceInfo = await getSourceInfo(arrMessages[i][1].sourceId);
            arrMessages[i][1].sourceTitle = sourceInfo.Title;
            // console.log(arrMessages[i][1]);
        }

        // console.log(itemIds);
        return arrMessages;
    } catch(errorReason){
        console.log(errorReason);
    }    
};
*/

async function getLastTenMessages(composedId) {
    try{
        // console.log("Entra en getLastTenMessages con composed_id: " + composedId);
        const itemIds = await getTenMessages(composedId);
        // console.log(itemIds);
        return getMessages(itemIds);
        /*
        arrMessages = await getMessages(itemIds);
         sourceInfos = await getFollowingInfos(composedId)
        for (let index = 0; index < sourceInfos.length; index++) {
            console.log(sourceInfos);
        }
        return arrMessages;
        */
    } catch(errorReason){
        console.log(errorReason);
    }    
};

async function getFollowingList(composedId) { 
    return composed_redis_client.zrevrange("ComposedFollowing:" + composedId, 0, -1);
}

async function getFollowingInfos(composedId) {
    const composedFollowingList = await getFollowingList(composedId)
    var composedFollowingInfo = []
    //console.log("++++++ Tamano lista ids " + composedFollowingList.length);
    
    for (let index = 0; index < composedFollowingList.length; index++) {
        const sourceId = composedFollowingList[index];
        const sourceInfo = await getSourceInfo(sourceId)
        //console.log(sourceInfo)
        composedFollowingInfo.push(sourceInfo)
    }
    //console.log("Tamano array (((((( " + composedFollowingInfo.length)
    return composedFollowingInfo;
}

async function getUserAdminList(username) {
    try{
        listAdmins = await composed_redis_client.smembers('UserAdminList:' + username);
    } catch(errorReason){
        console.log(errorReason);
    }    
    return listAdmins;
}

async function getAdminsList(composed_id) {
    try{
        listAdmins = await composed_redis_client.smembers('AdminsList:' + composed_id);
    } catch(errorReason){
        console.log(errorReason);
    }    
    return listAdmins;
}

module.exports.getUserAdminList = getUserAdminList;
module.exports.getMessage = getMessage;
module.exports.getAdminsList = getAdminsList;
module.exports.getMyFeedsInfo = getMyFeedsInfo;
module.exports.getFollowingInfos = getFollowingInfos;
module.exports.getSourceInfo = getSourceInfo;
module.exports.getFollowingList = getFollowingList;
module.exports.getLastTenSourceMessages = getLastTenSourceMessages;
module.exports.getSource = getSource;
module.exports.getComposed = getComposed;
module.exports.postComposed = postComposed;
module.exports.getComposedInfo = getComposedInfo;
module.exports.getTenMessages = getTenMessages;
module.exports.getMessages = getMessages;
module.exports.getLastTenMessages = getLastTenMessages;