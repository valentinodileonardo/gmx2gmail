#!/bin/bash

filename=$(find . -name 'g2g_*.tar.gz' -type f)

# remove old stuff is exists
echo "Removing old container ..."
podman kill g2g
podman stop g2g
podman rm g2g

podman load -i $filename

#run container with previously created image 
podman run --restart=unless-stopped --name g2g --hostname g2g -d g2g java -jar /home/jboss/binaries/g2g.jar

podman logs -f g2g