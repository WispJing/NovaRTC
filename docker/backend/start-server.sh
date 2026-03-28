#!/bin/sh
set -eu

MAX_NODE_ID=31

mod_32() {
  value="$1"
  echo $((value % (MAX_NODE_ID + 1)))
}

resolve_numeric_suffix() {
  value="$1"
  suffix=$(printf '%s' "$value" | sed -n 's/.*\([0-9][0-9]*\)$/\1/p')
  if [ -n "$suffix" ]; then
    echo "$suffix"
    return 0
  fi
  return 1
}

hash_to_node_id() {
  value="$1"
  checksum=$(printf '%s' "$value" | cksum | awk '{print $1}')
  mod_32 "$checksum"
}

resolve_worker_id() {
  if [ -n "${IM_WORKERID:-}" ]; then
    echo "$IM_WORKERID"
    return 0
  fi

  if [ -n "${HOSTNAME:-}" ] && resolve_numeric_suffix "$HOSTNAME" >/dev/null 2>&1; then
    suffix=$(resolve_numeric_suffix "$HOSTNAME")
    mod_32 "$suffix"
    return 0
  fi

  if [ -n "${HOSTNAME:-}" ]; then
    hash_to_node_id "$HOSTNAME"
    return 0
  fi

  echo 0
}

resolve_datacenter_id() {
  if [ -n "${IM_DATACENTERID:-}" ]; then
    echo "$IM_DATACENTERID"
    return 0
  fi

  if [ -n "${COMPOSE_PROJECT_NAME:-}" ]; then
    hash_to_node_id "$COMPOSE_PROJECT_NAME"
    return 0
  fi

  echo 1
}

WORKER_ID=$(resolve_worker_id)
DATACENTER_ID=$(resolve_datacenter_id)

if [ "$WORKER_ID" -lt 0 ] || [ "$WORKER_ID" -gt "$MAX_NODE_ID" ]; then
  echo "workerId out of range: $WORKER_ID" >&2
  exit 1
fi

if [ "$DATACENTER_ID" -lt 0 ] || [ "$DATACENTER_ID" -gt "$MAX_NODE_ID" ]; then
  echo "datacenterId out of range: $DATACENTER_ID" >&2
  exit 1
fi

echo "Starting NovaRTC with workerId=$WORKER_ID datacenterId=$DATACENTER_ID hostname=${HOSTNAME:-unknown}"

exec java ${JAVA_OPTS:-} -jar /opt/novartc/app.jar \
  --spring.config.additional-location=file:/opt/novartc/config/ \
  --im.workerId="$WORKER_ID" \
  --im.datacenterId="$DATACENTER_ID"
