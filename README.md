<img src="assets/icon.png" width="64" align="right">

# MSMP World

A server-side Fabric mod that extends the [Minecraft Server Management Protocol](https://minecraft.wiki/w/Minecraft_Server_Management_Protocol) (MSMP) by providing additional functions for getting and setting world data.

This mod is designed for tooling, dashboards, automation systems, external monitoring tools, and integrations that need structured access to world information without relying on command parsing or RCON hacks.


## Installation

1. Download the mod `.jar` and place it in your server's `mods/` folder.
2. Enable the Management Server in `server.properties`:
   ```properties
   management-server-enabled=true
   ```
3. Start the server. The Management Server will listen on `localhost:25576` by default.


## Configuration

On first start, the mod generates a configuration file at `<server_root_dir>/config/msmp/world/config.yml`, where you can tune the notification system for the tracked events that use threshold/interval-based polling:

```yaml
# Main configuration file for MSMP World.

# ...
```

Lower interval values mean more frequent checks (and potentially more notifications), at the cost of slightly more server load. Setting a delta to `0.0` triggers a notification on any change, however small.


## RPC Methods

The mod currently provides the following MSMP RPC methods. All of these methods are also automatically discoverable through the standard `rpc.discover` MSMP endpoint.

| Method                            | Description                                                                                   |
| --------------------------------- | --------------------------------------------------------------------------------------------- |



## RPC Notifications

The mod also provides the following MSMP RPC notifications that clients can subscribe to:

| Method                                    | Description                                                          |
| ----------------------------------------- | -------------------------------------------------------------------- |


> If you want more methods or notifications for other purposes, please [open an issue](https://github.com/MinecraftPlayground/msmp-world-mod/issues/new?template=new_method_or_notification_suggesetion.yml)


## License

[LGPL-3.0](LICENSE)
