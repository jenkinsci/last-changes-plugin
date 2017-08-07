FROM jenkins:2.60.1
COPY plugins.txt /usr/share/jenkins/plugins.txt
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"
RUN /usr/local/bin/plugins.sh /usr/share/jenkins/plugins.txt