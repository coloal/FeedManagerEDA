#! /usr/bin/env node

console.log('This script populates some users')
// Get arguments passed on command line
var userArgs = process.argv.slice(2);
/*
if (!userArgs[0].startsWith('mongodb')) {
    console.log('ERROR: You need to specify a valid mongodb URL as the first argument');
    return
}
*/
var async = require('async')
var User = require('./models/user')


var mongoose = require('mongoose');
var mongoDB = userArgs[0];
mongoose.connect(mongoDB, {useNewUrlParser: true, useUnifiedTopology: true});
mongoose.Promise = global.Promise;
var db = mongoose.connection;
db.on('error', console.error.bind(console, 'MongoDB connection error:'));

var users = []

function userCreate(username, date_of_creation, password, cb) {
  userdetail = {username:username , password: password }
  if (date_of_creation != false) userdetail.date_of_creation = date_of_creation
  
  var user = new User(userdetail);
       
  user.save(function (err) {
    if (err) {
      cb(err, null)
      return
    }
    console.log('New User: ' + user);
    users.push(user)
    cb(null, user)
  }  );
}

function createUsers(cb) {
  async.series([
      function(callback) {
        userCreate('coloal', '1234', '1973-06-06', callback);
      },
      function(callback) {
        userCreate('pcaba', '4321', '1932-11-8', callback);
      },
      function(callback) {
        userCreate('fv', '1234', '1920-01-02', callback);
      },
      ],
      // optional callback
      cb);
}

async.series([
  createUsers
],
// Optional callback
function(err, results) {
    if (err) {
        console.log('FINAL ERR: '+err);
    }
    else {
        console.log('UserInstances: '+users);
        
    }
    // All done, disconnect from database
    mongoose.connection.close();
});