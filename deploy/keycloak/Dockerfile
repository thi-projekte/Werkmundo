# Eigener Keycloak für Werkmundo mit eingebautem Realm "werkmundo".
# Build-Kontext: deploy/keycloak
#   docker build -t werkmundo-keycloak deploy/keycloak
#
# Beim ersten Start wird werkmundo-realm.json importiert. Existiert der Realm
# schon in der Datenbank, wird der Import übersprungen; spätere Änderungen also
# in der Admin-Konsole machen (oder Realm löschen und neu importieren).

FROM quay.io/keycloak/keycloak:26.2 AS builder
ENV KC_DB=postgres \
    KC_HEALTH_ENABLED=true
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:26.2
COPY --from=builder /opt/keycloak/ /opt/keycloak/
COPY werkmundo-realm.json /opt/keycloak/data/import/werkmundo-realm.json
ENV KC_DB=postgres \
    KC_HEALTH_ENABLED=true
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized", "--import-realm"]
