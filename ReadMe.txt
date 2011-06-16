~~~~~~~~~ Java Gearman Service ~~~~~~~~~

java-gearman-service provides a complete implementation of the gearman framework in 
java, including the client, worker, and server. It provides a generic application 
framework to farm out work to other machines or processes that are better suited to 
do the work. It allows you to do work in parallel, to load balance processing, and
to call functions between languages. It can be used in a variety of applications, 
from high-availability web sites to the transport of database replication events. In 
other words, it is the nervous system for how distributed processing communicates.


~~~~~~~~~ 0.3 ~~~~~~~~~
Issues Fixed:
* Issue 6: An exception thrown due to some class renames in newer java 7 builds
* Issue 7: An exception thrown when gearman packets over 1024 bytes are sent

~~~~~~~~~ 0.2 ~~~~~~~~~
Issues Fixed:

* Issue 1: The client receives data in the callback channels concurrently. Depending on how the processing threads are scheduled, it's possible some GearmanPackets will be processed out of order.
* Notes: Packets are now received in order. Blocking and waiting on subsequent packets will now deadlock. Given that packets are processed serially, blocking will not all the user to receive the subsequent packets.

* Issue 4: GearmanClient deadlock
* Notes: Fixed.

~~~~~~~~~ 0.1 ~~~~~~~~~

Initial Release