FROM dockerfile/java
MAINTAINER paul.seddon@gmail.com
ADD target/docker/files /
WORKDIR /opt/docker
RUN ["chown", "-R", "daemon", "."]
USER daemon
ENTRYPOINT ["bin/aws-dashboard"]
CMD []
