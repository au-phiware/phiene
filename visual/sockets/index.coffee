sockets = exports ? this
fs = require 'fs'
Lazy = require 'lazy'

sockets.index = (socket) ->
  console.log 'Client Connected'

  buffer = []
  paused = false
  closed = false
  stream = fs.createReadStream 'pipe.log'
  emitter = setInterval emit = ->
    if buffer.length > 0
      data = buffer.shift()
      name = data.op
      delete data.op
      socket.emit name, data
    else if paused and not closed
      stream.resume()
      paused = false
    else if closed and emitter
      clearInterval emitter
  , 150
  stream.on 'end', ->
    closed = true

  lazy = new Lazy stream
  lazy = lazy.lines
      .map(String)
      .map (line) ->
        data = {}
        [data.tick, data.thread, cat, data.op, rest...] = line.split ':'
        data.tick = parseInt data.tick

        switch cat
          when 'Pipe'
            data.in   = rest[0]
            data.out  = rest[2]
            data.proc = rest[1]
            data.op = 'join'

          when 'ArrayCloseableBlockingQueue'
            data.data = rest[1]
            data.pipe = rest[0]

          when 'IN'
            i = (rest[1].replace /^.*[.]/, '').split '@'
            data.data = uid:i[1], type: i[0]
            data.pipe = rest[0]

          when 'OUT'
            switch data.op
              when 'take'
                i = (rest[1].replace /^.*[.]/, '').split '@'
                data.data = [{uid:i[1], type: i[0]}]
              when 'drain'
                data.data = rest[1].split(/^\[|, |\]$/)[1...-1].map (i) ->
                  i = (i.replace /^.*[.]/, '').split '@'
                  {uid:i[1], type: i[0]}
            data.pipe = rest[0]

        buffer.push data
        if not paused and not closed
          stream.pause()
          paused = true

        return data

  socket.on 'pause', ->
    unless paused
      stream.pause()
      paused = true
    if emitter
      clearInterval emitter
      emitter = 0

  socket.on 'resume', ->
    if paused
      stream.resume()
      paused = false
    unless emitter or closed
      emitter = setInterval emit, 150

  socket.on 'disconnect', ->
    if emitter
      clearInterval emitter
      emitter = 0
    console.log 'Client Disconnected.'
