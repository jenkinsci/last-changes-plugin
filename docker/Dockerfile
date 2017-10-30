FROM  jenkins/jenkins:2.73.2
COPY plugins.txt /usr/share/jenkins/plugins.txt
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"
RUN /usr/local/bin/plugins.sh /usr/share/jenkins/plugins.txt
