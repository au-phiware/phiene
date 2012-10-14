root = exports ? this

# GET home page.
root.index = (req, res) ->
  res.render 'index', {
    title: 'Phiware Genetic Algorithm'
    description: 'A visualisation of individuals piped through the evolutionary processes.'
    author: 'Corin Lawson. Phiware.'
  }

# A Route for Creating a 500 Error (Useful to keep around)
root.error = (req, res) ->
  throw new Error 'This is a 500 Error'

# The 404 Route (ALWAYS Keep this as the last route)
root.notfound = (req, res) ->
  throw new NotFound

NotFound = (msg) ->
  this.name = 'NotFound'
  Error.call this, msg
  Error.captureStackTrace this, arguments.callee

