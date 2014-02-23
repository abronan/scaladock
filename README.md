#Scaladock

**scaladock** is a simple [Scala](http://www.scala-lang.org) client for the [Docker](http://www.docker.io) Container Engine Remote API.

Consider this as an experimental client. It is still under development and it needs a lot of improvements and cleanup.

##Getting Started

###Connection

#####Connect to a remote instance using Http

	val connection = new DockerConnection("172.16.212.98", "4243")

###Create a new Container and perform actions on it

You can create a new Container using the connection you defined in the last step calling `create`:

	val container = connection.create(
      Some(Config(
        Image = "busybox",
    	Tty = true,
    	Cmd = Array("date")
      ))
	)
	
	container match {
	  case Success(c) => {
		  c.start()
		  // do stuff here
	  }
	  case Failure(e) => new Throwable("Failed to create container")
	}

You can then perform many actions on the container:

	container.start()
	
	container.stop()
	
	container.restart()
	
	container.kill
	
	container.inspect
	...

###Attach to a container

	c.attach()

Do not forget to `start()` the container first or the attach command will output nothing. If attach succeeds, this will print stdout onto the REPL and output to a file named `<container_id>.log`. You can also explicitely specify where the log file will be by passing the path to attach.

	c.attach("/home/user/logfile.log")

To detach from the container, use:

	c.detach

###Contributing

1. Fork it

2. Create your feature branch (git checkout -b my-new-feature)

3. Commit your changes (git commit -am 'Add some feature')

4. Push to the branch (git push origin my-new-feature)

5. Create new Pull Request