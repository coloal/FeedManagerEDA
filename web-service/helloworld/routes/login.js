var express = require('express');
var router = express.Router();

var user_controller = require('../controllers/userController');

router.get('/user/detail', user_controller.user_detail);

module.exports = router;