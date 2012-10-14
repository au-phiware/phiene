sockets = exports ? this
fs = require 'fs'
lazy = require 'lazy'

sockets.index = (socket) ->
  console.log 'Client Connected'

  stream = new lazy fs.createReadStream 'pipe.log'
  stream.lines.map(String)
    .map (line) ->
      data = {}
      [data.tick, data.thread, cat, op, rest...] = line.split ':'
      data.tick = parseInt data.tick
      data.name = "#{cat}:#{op}"

      switch cat
        when 'Pipe'
          data.data =
            in:   rest[0]
            out:  rest[2]
            proc: rest[1]

        when 'ArrayCloseableBlockingQueue'
          data.data =
            pipe: rest[0]
            size: rest[1]

        when 'IN'
          data.data =
            pipe: rest[0]
            data: rest[1]

        when 'OUT'
          switch op
            when 'take'
              data.data = [rest[1]]
            when 'drain'
              data.data = rest[1].split(/^\[|, |\]$/)[1...-1]
          data.pipe = rest[0]

      return data

    .forEach (data) ->
      name = data.name
      delete data.name
      socket.emit name, data

  socket.on 'disconnect', ->
    console.log 'Client Disconnected.'
