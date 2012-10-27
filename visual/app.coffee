# Setup Dependencies
express = require 'express'
app = express()
http = require 'http'
server = http.createServer app
io = require 'socket.io'
io = io.listen server
stylus = require 'stylus'
nib = require 'nib'
routes = require './routes'
sockets = require './sockets'
path = require 'path'
join = path.join

css_compile = (styl, file) ->
  stylus styl,
    filename: path
    paths: [
      join __dirname, 'public', 'styles'
      join __dirname, 'public', 'vendor', 'styles'
    ]
    compress: true
  .import('nib')
  .use nib()

#app.configure 'development', ->
css_compile = (styl, file) ->
  stylus styl,
    filename: path
    paths: [
      join __dirname, 'public', 'styles'
      join __dirname, 'public', 'vendor', 'styles'
    ]
    compress: false
    warn: true
    #linenos: true
    #firebug: true
  .import('nib')
  .use nib()

# Setup Express
app.configure ->
  app.set 'port', process.env.PORT or 3000
  app.set 'views', join __dirname, 'views'
  app.set 'view engine', 'jade'
  app.set 'view options', layout: false
  #app.use express.favicon()
  #app.use express.logger 'dev'
  app.use express.bodyParser()
  app.use express.methodOverride()
  #app.use express.cookieParser 'your secret here'
  #app.use express.session()
  app.use app.router
  app.use stylus.middleware
    src: join __dirname, 'public'
    compile: css_compile

app.configure 'development', ->
  app.use express.logger 'dev'
  app.use express.errorHandler dumpExceptions: true, showStack: true

app.use express.static join __dirname, 'public'

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

port = app.get 'port'
server.listen port, ->
  console.log "Express #{app.get 'env'} server listening on port #{port}"
