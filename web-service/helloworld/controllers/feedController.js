var User = require('../models/user');
const feed = require('../models/feed');
const got = require('got');
var parseString = require('xml2js').parseString;
var rssParser = require('./rss-parser');
const kafkaProducer = require('./kafkaClient');
var moment = require('moment');

function getRandomInt(min, max) {
  return Math.floor(Math.random() * (max - min)) + min;
}

exports.AddSource = async (source_url) => {
  const encodedUrl = encodeURIComponent(source_url);
  const url = 'http://validator.w3.org/feed/check.cgi?url=' + encodedUrl + '&output=soap12';
  
  try{

    const resWml = await got(url);
    
    if(resWml.statusCode == 200)
    {
      parseString(resWml.body, async function(err, result) {
        const rss_is_valid = result['env:Envelope']['env:Body'][0]['m:feedvalidationresponse'][0]['m:validity'][0];
        
        if(rss_is_valid)
        {
          rss_uri = result['env:Envelope']['env:Body'][0]['m:feedvalidationresponse'][0]['m:uri'][0];
          let feed = await rssParser.parser.parseURL(rss_uri);
          console.log("Composing JSON: \nTitle: " + feed.title + "\nDescription: " + feed.description + "\nlink: " + feed.link);
          var payload = "{\"type\": \"sourceNew\"," +
          "\"Url\": \"" + rss_uri + "\"," +
          "\"Link\": \"" + feed.link + "\"," +
          "\"Title\": \"" + feed.title + "\"," +
          "\"Description\": \"" + feed.description + "\"," +
          "\"DateOfCreation\": \"" + Date.now() + "\"" +
          "}";

          console.log(payload);
          var keyRand = (getRandomInt(1, 5)-1).toString();
          console.log("key: " + keyRand);
          const responses = await kafkaProducer.kafka_producer.send({
            topic: "source-actions-topic",
            messages: [{
              partition: keyRand, //esta es la importante
              key: keyRand, //aunque el connector coge esta para saber dónde mirar
              value: payload
            }]
          });

          console.log('Published message', { responses })
          
          //RETURN POST ANSWER TO CLIENT
        }
        
      });
      
    }
    
  }catch (error) {
    console.log("HA PETAO: " + error);
    console.log(error.response.body);
  } 

}

exports.AddAdmin = async (username, composedId) => {
  console.log("username: " + username + "ComposedID: " + composedId);

  if (typeof composedId !== 'undefined' && composedId !== null && typeof username !== 'undefined' && username !== null) {
    console.log('Entra!!!!!!!!!!!!!!');
    var payload = "{\"type\": \"composedAddAdmin\"," +
          "\"composedId\": \"" + composedId + "\"," +
          "\"username\": \"" + username + "\"," +
          "\"timestamp\": " + Date.now() + 
          "}";
    console.log(payload);

    const responses = await kafkaProducer.kafka_producer.send({
      topic: "composed-followers-topic",
      messages: [{
        key: composedId,
        value: payload
      }]
    });

    console.log('Published message', { responses })
  }

}

exports.DelAdmin = async (username, composedId) => {
  console.log("username: " + username + "ComposedID: " + composedId);

  if (typeof composedId !== 'undefined' && composedId !== null && typeof username !== 'undefined' && username !== null) {
    console.log('Entra!!!!!!!!!!!!!!');
    var payload = "{\"type\": \"composedDelAdmin\"," +
          "\"composedId\": \"" + composedId + "\"," +
          "\"username\": \"" + username + "\"," +
          "\"timestamp\": " + Date.now() + 
          "}";

    console.log(payload);

    const responses = await kafkaProducer.kafka_producer.send({
      topic: "composed-followers-topic",
      messages: [{
        key: composedId,
        value: payload
      }]
    });

    console.log('Published message', { responses })
  }

}

exports.FollowSource = async (composedId, sourceId) => {
  console.log("ComposedID: " + composedId + ", SourceId: " + sourceId);
  if (typeof sourceId !== 'undefined' && sourceId !== null && typeof composedId !== 'undefined' && composedId !== null) {
    console.log('Entra!!!!!!!!!!!!!!');

    var payload = "{\"type\": \"sourceFollowedByComposed\"," +
          "\"composedId\": \"" + composedId + "\"," +
          "\"sourceId\": \"" + sourceId + "\"," +
          "\"timestamp\": " + Date.now() + 
          "}";

    const responses = await kafkaProducer.kafka_producer.send({
      topic: "composed-followers-topic",
      messages: [{
        key: composedId,
        value: payload
      }]
    });

    console.log('Published message', { responses })
  }else {
    console.log('No entra!!!!!!!!!!!!!!');
  }

  
}

exports.UnfollowSource = async (composedId, sourceId) => {
  console.log("ComposedID: " + composedId + ", SourceId: " + sourceId);
  if (typeof sourceId !== 'undefined' && sourceId !== null && typeof composedId !== 'undefined' && composedId !== null) {
    console.log('Entra!!!!!!!!!!!!!!');

    var payload = "{\"type\": \"sourceUnfollowedByComposed\"," +
          "\"composedId\": \"" + composedId + "\"," +
          "\"sourceId\": \"" + sourceId + "\"," +
          "\"timestamp\": " + Date.now() + 
          "}";

    const responses = await kafkaProducer.kafka_producer.send({
      topic: "composed-followers-topic",
      messages: [{
        key: composedId,
        value: payload
      }]
    });

    console.log('Published message', { responses })
  }else {
    console.log('No entra!!!!!!!!!!!!!!');
  }
}

exports.StartHomeFeeds = async (req, res) => {

  try{
    console.log("**** LA LISTA DE FEEDS DE MI PROPIEDAD: " + req.user.myfeeds);
    console.log("feed 1 = "+req.user.feed1)
    console.log("feed 2 = "+req.user.feed2)
    console.log("feed 3 = "+req.user.feed3)
    Promise.all(
      [feed.getComposedInfo(req.user.feed1),
        feed.getComposedInfo(req.user.feed2),
        feed.getComposedInfo(req.user.feed3),
        feed.getLastTenMessages(req.user.feed1),
        feed.getLastTenMessages(req.user.feed2),
        feed.getLastTenMessages(req.user.feed3),
        feed.getFollowingInfos(req.user.feed1),
        feed.getFollowingInfos(req.user.feed2),
        feed.getFollowingInfos(req.user.feed3),
        feed.getMyFeedsInfo(req.user.myfeeds),
        feed.getUserAdminList(req.user.username)
      ]
        ).then(( [feed1Info, feed2Info, feed3Info, resultado1, resultado2, resultado3, feed1FollowingInfo, feed2FollowingInfo,
           feed3FollowingInfo, myFeedsInfo, user_admin_list]) => {
          //console.log("FeedInfo1: " + feed1Info.Name + " FeedInfo2: " + feed2Info.Name + " FeedInfo3: " + feed3Info.Name);
          
          console.log("***** Tamano de list following=" + feed1FollowingInfo.length)
          
          feed1FollowingInfo.forEach(element => {
            console.log("***** Info following: " + element);
          });

          
          res.cookie('feed1', req.user.feed1);
          res.cookie('feed2', req.user.feed2);
          res.cookie('feed3', req.user.feed3);
          for(let i=0; i < resultado1.length; i++)
          {
            resultado1[i][1].date = moment(new Date( parseInt(resultado1[i][1].date, 10))).fromNow();
            var j=0;
            while(feed1FollowingInfo[j].id != resultado1[i][1].sourceId)
              j++;
            
            resultado1[i][1].sourceTitle = feed1FollowingInfo[j].Title;
            resultado1[i][1].sourceLink = feed1FollowingInfo[j].Link; 
          }
          
          for(let i=0; i < resultado2.length; i++)
          {
            resultado2[i][1].date = moment(new Date( parseInt(resultado2[i][1].date, 10))).fromNow();
            var j=0;
            while(feed2FollowingInfo[j].id != resultado2[i][1].sourceId)
              j++;
            
            resultado2[i][1].sourceTitle = feed2FollowingInfo[j].Title; 
            resultado2[i][1].sourceLink = feed2FollowingInfo[j].Link; 
          }

          for(let i=0; i < resultado3.length; i++)
          {
            resultado3[i][1].date = moment(new Date( parseInt(resultado3[i][1].date, 10))).fromNow();
            var j=0;
            while(feed3FollowingInfo[j].id != resultado3[i][1].sourceId)
              j++;
            
            resultado3[i][1].sourceTitle = feed3FollowingInfo[j].Title;
            resultado3[i][1].sourceLink = feed3FollowingInfo[j].Link; 
          }

          res.render('home', {infoFeed1: feed1Info, infoFeed2: feed2Info, infoFeed3: feed3Info, listMsg1: resultado1, listMsg2: resultado2,
             listMsg3: resultado3, listFollowedSources: feed1FollowingInfo, listMyFeeds: myFeedsInfo});
      });
     
  } catch(errorReason){
    console.log(errorReason);
  }
}

exports.CreateComposed = async (iFeed, name, desc, tkn) => {
  

  User.findByToken(tkn, async (err, user) => {
    if (err) throw err;
    if (!user) return res.json({ isAuth: false, error: true })

    if(user.nfeeds < 10)
    {
      composed_id = await feed.postComposed(name, desc, user.username);
      user.myfeeds.push(composed_id);
      user.nfeeds = user.nfeeds + 1;
      switch (iFeed) {
        case "newFeed1":
          user.feed1 = composed_id;
          break;
        case "newFeed2":
          user.feed2 = composed_id;
          break;  
        case "newFeed3":
          user.feed3 = composed_id;
          break;
        default:
          console.log("Feed id does not exists");
          break;
      }

      user.save(function(err, user) {
        if (err) return console.error(err);
        console.log("User feed created correctly!");
        
      });
    }
    else{
      console.log("user already has 10 feeds");
    }
});

}

exports.UpdateFeed = async (req, res) => {
    
    composed_redis_client.exists("ComposedFeed:" + req.body.feed_id, function (err, reply) {
        if(err)
          console.log.apply(err);
        else{
          if(reply==1)
          {
            User.findByIdAndUpdate({ _id: req.user._id }, { feed1: req.body.feed_id }, (err) => {
                if (err) return res.json({ success: false, err })
                return res.status(200).send({ success: true, message: 'Successfully added feed!' });
            })
          }else{
            return res.status(404).send({ success: false, message: 'Feed not found!' });
          }
        }
      });
}