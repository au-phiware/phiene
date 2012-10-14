# Setup Dependencies
express = require 'express'
app = express()
http = require 'http'
server = http.createServer app
io = require 'socket.io'
io = io.listen server
routes = require './routes'
sockets = require './sockets'
port =  process.env.PORT || 3000

# Setup Express
app.configure ->
  app.set 'views', __dirname + '/views'
  app.set 'view engine', 'jade'
  app.set 'view options', layout: false
  #app.use express.favicon()
  #app.use express.logger 'dev'
  app.use express.bodyParser()
  app.use express.methodOverride()
  #app.use express.cookieParser 'your secret here'
  #app.use express.session()
  app.use app.router
  app.use express.static __dirname + '/public'

# Setup the errors
app.configure 'development', ->
  app.use express.errorHandler
###
app.configure function {
app.error function err, req, res, next{
  if  err instanceof NotFound {
    res.render '404.jade', { locals: {
               title : 'Not Found'
             , description: ''
             , author: ''
             #, analyticssiteid: 'XXXXXXX'
    }, status: 404 }
  } else {
    res.render '500.jade', { locals: {
               title : 'Internal Server Error'
             , description: ''
             , author: ''
             #, analyticssiteid: 'XXXXXXX'
             , error: err
    }, status: 500 }
  }
}
###
# Setup Socket.IO
io.sockets.on 'connection', sockets.index

# Setup routes
app.get '/', routes.index
app.get '/500', routes.error
#app.get '/*', routes.notfound

server.listen port
console.log 'Listening on http://localhost:' + port
