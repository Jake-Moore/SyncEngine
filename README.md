# SyncEngine
A data storage and synchronization library for Spigot plugins.  
Inspired by Jonah Seguin's [Payload](https://github.com/jonahseguin/Payload) project.

See the [Wiki](https://github.com/Jake-Moore/SyncEngine/wiki) for documentation.

## Project Description
SyncEngine utilizes [Morphia](https://github.com/MorphiaOrg/morphia) to automatically convert Java objects to JSON documents. These documents can be stored in local files or on a MongoDB server.

SyncEngine is also designed to support distributed minecraft servers, also known as 'instanced' servers (the architecture where multiple minecraft servers are joined as one gamemode to provide more resources and better performance).  

SyncEngine handles data synchronization between servers and transitioning data as players move around the network. This provides a seamless experience for players and developers.
