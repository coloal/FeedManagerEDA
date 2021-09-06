var mongoose = require('mongoose');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const SALT = 10;
const SECRETE = "POJENWRGEKJGNKEJWNGKJFN";
var Schema = mongoose.Schema;

var UserSchema = new Schema(
  {
    username: { type: String, unique: true, sparse: true, index: true, required: true, minLength: 1, maxLength: 20 },
    password: { type: String, required: true, minLength: 5, maxLength: 100 },
    token: { type: String },
    feed1: { type: String},
    feed2: { type: String},
    feed3: { type: String},
    myfeeds: { type: Array },
    nfeeds: { type: Number, min: 0, max: 9 }
  }
);

//saving user data
UserSchema.pre('save', function (next) {
  var user = this;
  
  if (user.isModified('password')) {//checking if password field is available and modified

    bcrypt.genSalt(SALT, function (err, salt) {
    
      if (err) return next(err)
        bcrypt.hash(user.password, salt, function (err, hash) {
          if (err) return next(err)
            user.password = hash;
          next();
        });

    });
  } else {
    next();
  }

});

//for comparing the users entered password with database duing login 
UserSchema.methods.comparePassword = function (candidatePassword, callBack) {
  console.log("Actual password=" + this.password + "Candidate password=" + candidatePassword);
  bcrypt.compare(candidatePassword, this.password, function (err, isMatch) {
    if (err) return callBack(err);
      callBack(null, isMatch);
  });
}

//for generating token when loggedin
UserSchema.methods.generateToken = function (callBack) {
  var user = this;
  var token = jwt.sign(user._id.toHexString(), SECRETE);
  user.token = token;
  user.save(function (err, user) {
    if (err) return callBack(err)
      callBack(null, user)
  });
};

/*
//for generating token when loggedin
UserSchema.statics.changeFeedViewport = function (composed_id, ifeed, callBack) {
  var user = this;
  
  switch(ifeed){
    case 1:
       console.log('Entra en 1 con composed_id='+composed_id);
       user.feed1 = composed_id;
       break;
    case 2:
       console.log('Entra en 2 con composed_id='+composed_id);
       user.feed2 = composed_id;
       break;
    case 3:
       console.log('Entra en 3 con composed_id='+composed_id);
       user.feed3 = composed_id;
       break;
    default:
       console.log('default case used');
       break;
  }

  user.save(function (err, user) {
    if (err) return callBack(err)
      callBack(null, user)
  });
};
*/
//validating token for auth routes middleware
UserSchema.statics.findByToken = function (token, callBack) {
  var user = this;
  jwt.verify(token, SECRETE, function (err, decode) {//this decode must give user_id if token is valid .ie decode=user_id

    user.findOne({ '_id': decode, 'token': token }, function (err, user) {
      if (err) return callBack(err);
        callBack(null, user);
    });

  });
};

//Export model
module.exports = mongoose.model('User', UserSchema);