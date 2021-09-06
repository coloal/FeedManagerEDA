var User = require('../models/user');
var mongoose = require('mongoose');

exports.RegisterUser = async (req, res) => {
    
    const user = new User(req.body);
    user.nfeeds = 0;
    await user.save((err, doc) => {
        if (err) {
            return res.status(422).json({errors:err})
        } else {
            const userData = {
                username: doc.username, 
                feed1: doc.feed1,
                feed2: doc.feed2,
                feed3: doc.feed3,
                nfeeds: 0
            }
            return res.status(200).json(
                {
                    success: true,
                    message: 'Successfully Signed Up',
                    userData
                }
            )
        }
    });
}

exports.LoginUser = (req, res) => {
    console.log(JSON.stringify(req.body));
    console.log("Username: " + req.body.username);
    User.findOne({ 'username': req.body.username }, (err, user) => {
        if (!user) {
            return res.status(404).json({ success: false, message: 'User username not found!' });
        } else {
            user.comparePassword(req.body.password, (err, isMatch) => {
                console.log(isMatch);
                //isMatch is eaither true or false
                if (!isMatch) {
                    return res.status(400).json({ success: false, message: 'Wrong Password!' });
                } else {
                    user.generateToken((err, user) => {
                        if (err) {
                            return res.status(400).send({ err });
                        } else {
                            //saving token to cookie
                            res.cookie('authToken', user.token) //.status(200).json( {success: true, message: 'Successfully Logged In!', userData: data} )
                            console.log("Feed1: " + user.feed1);
                            console.log("Feed1: " + user.feed2);
                            console.log("Feed1: " + user.feed3);
                            /*
                            res.cookie('feed1', user.feed1);
                            res.cookie('feed2', user.feed2);
                            res.cookie('feed3', user.feed3);
                            */
                            res.redirect('/feeds')
                        }
                    });
                }
            });
        }
    });
}

exports.LogoutUser = (req, res) => {
    User.findByIdAndUpdate({ _id: req.user._id }, { token: '' }, (err) => {
        if (err) return res.json({ success: false, err })
        return res.status(200).send({ success: true, message: 'Successfully Logged Out!' });
    })
}

//get authenticated user details
exports.getUserDetails = (req, res) => {
    return res.status(200).json( { isAuthenticated: true, username: req.user.username, feed1: req.user.feed1 } );
}