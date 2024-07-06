&nbsp;
> <a href="https://github.com/Jake-Moore/SyncEngine/releases/latest"> <img alt="Latest Release" src="https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/Jake-Moore/ef2ddc2a6021074d1b5d032aafa1c849/raw/version.json" /></a>
>
> The GitHub release may be different from latest on the Maven repository.

# SyncEngine
A data storage and synchronization library for Spigot plugins.  
Inspired by Jonah Seguin's [Payload](https://github.com/jonahseguin/Payload) project.


## Project Description
SyncEngine utilizes [MongoJack](https://github.com/mongojack/mongojack) for MongoDB management, and [Jackson](https://github.com/FasterXML/jackson) to automatically convert Java objects to JSON documents.  
These libraries allow SyncEngine to provide several storage options, including a file-based storage method requiring no external services.

SyncEngine is also designed to support distributed minecraft servers, also known as 'instanced' servers (the architecture where multiple minecraft servers are joined as one gamemode to provide more resources and better performance).  
SyncEngine handles data synchronization between servers and transitioning data as players move around the network. This provides a seamless experience for players and developers.

## Wiki
See the [Wiki](https://github.com/Jake-Moore/SyncEngine/wiki) for documentation.  

## Developers
See [DEVELOPERS.md](https://github.com/Jake-Moore/SyncEngine/blob/main/DEVELOPERS.md) for dependency details.  

Using SyncEngine requires a base understanding of Jackson object mapping. That project provides several [guides](https://github.com/FasterXML/jackson-docs) for developers.
