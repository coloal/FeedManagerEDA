
var createError = require('http-errors');
var express = require('express');
var path = require('path');
var cookieParser = require('cookie-parser');
var logger = require('morgan');

var indexRouter = require('./routes/index');

var app = express();

//Set up mongoose connection
var mongoose = require('mongoose');
const { isObject } = require('util');

// var mongoDB = 'mongodb://coloal:pass1234@127.0.0.1:27017/users';
var mongoDB = "mongodb://" + process.env.DATABASE_USER + ":" + process.env.DATABASE_PASSWORD + "@" + process.env.DATABASE_HOST + "/" + process.env.DATABASE_NAME;

mongoose.connect(mongoDB, { useNewUrlParser: true , useUnifiedTopology: true});
mongoose.set('useCreateIndex', true);
var db = mongoose.connection;
db.on('error', console.error.bind(console, 'MongoDB connection error:'));

// view engine setup
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'jade');

app.use(logger('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'public')));

const { auth } = require('./middleware/auth')
const { RegisterUser, LoginUser, LogoutUser,getUserDetails } = require('./controllers/authController');
const { StartHomeFeeds,UpdateFeed } = require('./controllers/feedController');
const { RESERVED_EVENTS } = require('socket.io/dist/socket');

app.post('/users/register',RegisterUser);
app.post('/users/login', LoginUser);
app.get('/users/auth', auth, getUserDetails);
app.get('/users/logout', auth, LogoutUser);

app.get('/', (req, res) => { res.render('login') });
app.get('/register', (req, res) => { res.render('register')} );

app.get('/feeds', auth , StartHomeFeeds);

app.get('/addFeed', (req, res) => {res.render('addFeed')} );
app.post('/feeds/addFeed', auth, UpdateFeed); 

app.get('/semantic', (req, res) => {res.render('prueba')} );
// catch 404 and forward to error handler
app.use(function(req, res, next) {
  next(createError(404));
});

// error handler
app.use(function(err, req, res, next) {
  // set locals, only providing error in development
  res.locals.message = err.message;
  res.locals.error = req.app.get('env') === 'development' ? err : {};

  // render the error page
  res.status(err.status || 500);
  res.render('error');
});

module.exports = app;