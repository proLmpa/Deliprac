# Stops and removes the local monitoring containers.
docker stop prometheus alertmanager grafana 2>$null
docker rm   prometheus alertmanager grafana 2>$null
docker network rm monitoring 2>$null
