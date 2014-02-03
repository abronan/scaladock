#Scaladock

**scaladock** is a simple [Scala](http://www.scala-lang.org) client for the [Docker](http://www.docker.io) Container Engine Remote API.


##Getting Started

###Installation

####Using SBT

You can import the project using SBT:

	libraryDependencies += "com.scaladock" % "scaladock" % "0.2.3-SNAPSHOT"
	
####Using Maven

You can also import the project using classical Maven definition in `pom.xml`:

	<dependency>
		<groupId>com.scaladock</groupId>
		<artifactId>scaladock</artifactId>
		<version>0.2.3-SNAPSHOT</version>
	</dependency>


###Connection

#####Connect to local instance through local unix socket

	val connection = new DockerConnection()

#####Connect to a remote instance using Http

	val connection = new DockerConnection("172.16.212.98", "4243")

###Create a new Container and perform actions on it

You can create a new Container using the connection you defined in the last step calling `create`:

	val container = connection.create(
  	  Some(Config(
    	Tty = true,
    	Cmd = Array("date")
  	  ))
	)
	
	container match {
		case Success(c) => {
			c.start
			// do stuff here
		}
		case Failure(e) => new Throwable("Failed to create container")
	}
	
You can then perform many actions on the container:

	container.start()
	
	container.stop()
	
	container.restart
	
	container.kill
	
	container.inspect
	...

###Attach to a container

	c.attach()
	
Do not forget to `start()` the container first or the attach command will output nothing. If attach succeeds, this will print stdout onto the REPL and output to a file named `<container_id>.log`. You can also explicitely specify where the log file will be by passing the path to attach.

	c.attach("/home/user/logfile.log")

To detach from the container, use:

	c.detach


###Migrate Containers through Hosts

You just need to use the `>>` (shift) method. The following snippet demonstrates how this is done:

	val host = new DockerConnection()
	val remote = new DockerConnection("172.16.212.98") with SSH
	val registry = new DockerRegistry("localhost.localdomain", "5000")
	val container = host.create(
      Some(Config(
        Tty = true,
      	Cmd = Array("date")
      ))
  	).get
	val newContainer = container >> remote using registry
	new.kill // Kills the newly migrated container
	
`>>` is blocking while `>>>` is non blocking and forces you to handle the success of a `Future`.
	
Under the hood, the migration pass through the following steps:

* Commit an Image from the container filesystem changes
* Push the Image onto a private Registry (if you declare nothing it is pushed on the public index using your credentials if you are authenticated)
* Pull the image from the destination connection
* Run a new container with this image, restoring the same Configuration on launch (but you can also pass a new configuration, see advanced usage)

You could even migrate a full set of containers passing a `List[Container]` to the `>>>` method. In this case it might take some time so you need to control the success of the operation with a `Future` handled in its own execution context (`Actor`) to not block further processing. You should note that calling migrate on a Container blocks operations on it until `>>>` finishes its work of migration.
	

###Contributing

1. Fork it

2. Create your feature branch (git checkout -b my-new-feature)

3. Commit your changes (git commit -am 'Add some feature')

4. Push to the branch (git push origin my-new-feature)

5. Create new Pull Request