FROM redislabs/redisearch:latest AS imgmodule
FROM docker.io/bitnami/minideb:buster
LABEL maintainer "Bitnami <containers@bitnami.com>"

ENV HOME="/" \
    OS_ARCH="amd64" \
    OS_FLAVOUR="debian-10" \
    OS_NAME="linux"

COPY prebuildfs /
# Install required system packages and dependencies
RUN install_packages acl ca-certificates curl gzip libc6 libssl1.1 procps tar
RUN . /opt/bitnami/scripts/libcomponent.sh && component_unpack "wait-for-port" "1.0.0-3" --checksum 7521d9a4f9e4e182bf32977e234026caa7b03759799868335bccb1edd8f8fd12
RUN . /opt/bitnami/scripts/libcomponent.sh && component_unpack "redis" "6.2.4-0" --checksum 58126d2082f8d9b76bd1a932972492b69d729d7e0f130b9ae3e359107448eba2
RUN . /opt/bitnami/scripts/libcomponent.sh && component_unpack "gosu" "1.13.0-0" --checksum fd7257c2736164d02832dbf72e2c1ed9d875bf3e32f0988520796bc503330129
RUN chmod g+rwX /opt/bitnami
RUN ln -s /opt/bitnami/scripts/redis/entrypoint.sh /entrypoint.sh
RUN ln -s /opt/bitnami/scripts/redis/run.sh /run.sh

COPY rootfs /
RUN /opt/bitnami/scripts/redis/postunpack.sh
ENV BITNAMI_APP_NAME="redis" \
    BITNAMI_IMAGE_VERSION="6.2.4-debian-10-r3" \
    PATH="/opt/bitnami/common/bin:/opt/bitnami/redis/bin:$PATH"

RUN mkdir /opt/bitnami/redis/modules
COPY --from=imgmodule /usr/lib/redis/modules/redisearch.so /opt/bitnami/redis/modules
#RUN sed -i '/User-supplied/ a loadmodule /opt/bitnami/redis/modules/redisearch.so' /opt/bitnami/redis/etc/redis.conf
#RUN echo "loadmodule /opt/bitnami/redis/modules/redisearch.so" >> /opt/bitnami/redis/etc/redis.conf

EXPOSE 6379

USER 1001
ENTRYPOINT [ "/opt/bitnami/scripts/redis/entrypoint.sh" ]
CMD [ "/opt/bitnami/scripts/redis/run.sh" ]