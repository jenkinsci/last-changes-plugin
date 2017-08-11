#!/bin/sh
docker run -it --name jenkins-last-changes -p 8080:8080 -v /var/jenkins_home rmpestano/jenkins-last-changes