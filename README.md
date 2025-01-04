# AzuVotifier ![build](https://img.shields.io/github/actions/workflow/status/azurejelly/azuvotifier/build.yml)
A NuVotifier fork with Redis forwarding support and a standalone server implementation. From the original README:
> NuVotifier is a secure alternative to using the original Votifier project.
> NuVotifier will work in place of Votifier - any vote listener that supports
> Votifier will also support NuVotifier.

## Useful resources
- [Setup Guide](https://github.com/NuVotifier/NuVotifier/wiki/Setup-Guide)
- [Troubleshooting Guide](https://github.com/NuVotifier/NuVotifier/wiki/Troubleshooting-Guide)
- [Developer Information](https://github.com/NuVotifier/NuVotifier/wiki/Developer-Documentation)

## Running
You can get the latest release directly from [GitHub](https://github.com/azurejelly/azuvotifier/releases) or [Modrinth](https://modrinth.com/plugin/azuvotifier).
Then, follow the instructions for your server software or the standalone version:

### Bukkit, BungeeCord and Velocity
Drag and drop the downloaded JAR into your `plugins/` folder. You should've downloaded the JAR that has your server software in its name.
If you've done everything right, it should work out of the box. If it doesn't, feel free to ask for help [here](https://github.com/azurejelly/azuvotifier/issues).

### Standalone
Open up the terminal, go into the directory the previously downloaded JAR is at, and then run it like this:
```shell
$ java -Xms512M -Xmx512M -jar nuvotifier-standalone.jar
```

You can also use command line arguments to configure some settings, such as the hostname:
```shell
$ java -Xms512M -Xmx512M -jar nuvotifier-standalone.jar --host 127.0.0.1 --config /etc/nuvotifier/
```

To get a full list of options, run:
```shell
$ java -jar nuvotifier-standalone.jar --help
```

### Standalone with Docker
A Docker image for the standalone NuVotifier implementation is available at [Docker Hub](https://hub.docker.com/r/azurejelly/standalone-nuvotifier). To pull it, run:
```shell
$ docker pull azurejelly/standalone-nuvotifier:latest   # for the latest stable release
$ docker pull azurejelly/standalone-nuvotifier:unstable # for the latest commit on master
```

You can then run the image using a command like:
```shell
$ docker run -p 8192:8192 \
    -v /etc/nuvotifier:/app/config \
    --restart unless-stopped \
    --name nuvotifier \
    azurejelly/standalone-nuvotifier:latest \
    --port 8192
```

This will:
- Expose port 8192 on the host machine;
- Map `/etc/nuvotifier` (host) to `/app/config` (container) using bind mounts;
- Restart the container automatically unless stopped;
- Name the container `nuvotifier`;
- Use the `azurejelly/standalone-nuvotifier:latest` image;
- And pass `--port 8192` as a command line argument to NuVotifier.
  - Not required as `8192/tcp` is already the default port, but helps to show that you can pass arguments such as `--port` or `--config`.

If you want to use Docker Compose, an example [`docker-compose.yml`](https://github.com/azurejelly/azuvotifier/blob/master/docker-compose.yml) file is available on the repository.

# License
NuVotifier is GNU GPLv3 licensed. This project's license can be viewed [here](LICENSE).
