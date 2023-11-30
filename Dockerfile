FROM registry.access.redhat.com/ubi8/openjdk-8

LABEL name="g2g" \
      vendor="" \
      maintainer="Valentino di Leonardo" \
      version="1.0" \
      summary="" \
      description="" 


USER jboss

#copy content
COPY binaries /home/jboss/binaries

RUN ls -ls /home/
RUN ls -ls /home/jboss
RUN ls -la /home/jboss/binaries/ 

WORKDIR /home/jboss/binaries

CMD ["java","-jar", "/home/jboss/binaries/g2g.jar"]