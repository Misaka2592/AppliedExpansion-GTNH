#!/usr/bin/env bash

set -euo pipefail

SMOKE_ROOT="${SMOKE_ROOT:?SMOKE_ROOT must be set}"
CLIENT_DIR="$SMOKE_ROOT/client"
CLIENT_MC_DIR="$CLIENT_DIR/.minecraft"
SERVER_DIR="$SMOKE_ROOT/server"
RUN_DIR="$SMOKE_ROOT/run"
SERVER_LOG="$RUN_DIR/server.log"
CLIENT_LOG="$RUN_DIR/client.log"
MAIN_MENU_MARKER="$RUN_DIR/.mainmenu.headlessnh"
SERVER_LOADED_MARKER="$RUN_DIR/.serverloaded.headlessnh"

server_process=""
client_process=""
xvfb_process=""

print_failure_logs() {
    printf '%s\n' '--- server.log (last 200 lines) ---'
    tail -n 200 "$SERVER_LOG" 2>/dev/null || true
    printf '%s\n' '--- client.log (last 200 lines) ---'
    tail -n 200 "$CLIENT_LOG" 2>/dev/null || true
}

stop_group() {
    local process_id="$1"
    local signal="$2"
    kill -"$signal" -- "-$process_id" 2>/dev/null || kill -"$signal" "$process_id" 2>/dev/null || true
}

wait_for_exit() {
    local process_id="$1"
    local timeout="$2"
    local waited=0
    while kill -0 "$process_id" 2>/dev/null && ((waited < timeout)); do
        sleep 1
        waited=$((waited + 1))
    done
    ! kill -0 "$process_id" 2>/dev/null
}

cleanup() {
    local result=$?
    trap - EXIT

    if [[ -n "$client_process" ]] && kill -0 "$client_process" 2>/dev/null; then
        stop_group "$client_process" TERM
        wait_for_exit "$client_process" 15 || stop_group "$client_process" KILL
    fi

    if [[ -n "$server_process" ]] && kill -0 "$server_process" 2>/dev/null; then
        printf 'stop\n' >&3 || true
        wait_for_exit "$server_process" 60 || stop_group "$server_process" KILL
    fi

    exec 3>&- 2>/dev/null || true

    if [[ -n "$xvfb_process" ]] && kill -0 "$xvfb_process" 2>/dev/null; then
        kill -TERM "$xvfb_process" 2>/dev/null || true
    fi

    if ((result != 0)); then
        print_failure_logs
    fi
    exit "$result"
}
trap cleanup EXIT

set_server_property() {
    local key="$1"
    local value="$2"
    local properties="$SERVER_DIR/server.properties"
    if grep -q "^${key}=" "$properties"; then
        sed -i "s/^${key}=.*/${key}=${value}/" "$properties"
    else
        printf '%s=%s\n' "$key" "$value" >> "$properties"
    fi
}

wait_for_server() {
    local timeout=600
    local waited=0
    while ((waited < timeout)); do
        if grep -Eq 'Done.*For help, type' "$SERVER_LOG" 2>/dev/null; then
            return 0
        fi
        if ! kill -0 "$server_process" 2>/dev/null; then
            printf 'Server exited before becoming ready.\n' >&2
            return 1
        fi
        sleep 5
        waited=$((waited + 5))
    done
    printf 'Server did not become ready within %s seconds.\n' "$timeout" >&2
    return 1
}

wait_for_client_connection() {
    local timeout=720
    local waited=0
    while ((waited < timeout)); do
        if [[ -e "$SERVER_LOADED_MARKER" ]]; then
            return 0
        fi
        if ! kill -0 "$client_process" 2>/dev/null; then
            printf 'Client exited before connecting to the server.\n' >&2
            return 1
        fi
        if find "$CLIENT_MC_DIR/crash-reports" -maxdepth 1 -type f -name 'crash-*.txt' -print -quit 2>/dev/null \
            | grep -q .; then
            printf 'Client crash report detected before connection.\n' >&2
            return 1
        fi
        sleep 5
        waited=$((waited + 5))
    done
    printf 'Client did not connect within %s seconds.\n' "$timeout" >&2
    return 1
}

mkdir -p "$RUN_DIR"
export XDG_DATA_HOME="$RUN_DIR/xdg-data"
mkdir -p "$XDG_DATA_HOME"
test -f "$CLIENT_DIR/launch.env"
test -f "$CLIENT_DIR/launch.argv"
test -f "$SERVER_DIR/java9args.txt"
test -f "$SERVER_DIR/lwjgl3ify-forgePatches.jar"
test -f "$CLIENT_MC_DIR/mods/appliedexpansion.jar"

if find "$SERVER_DIR/mods" -maxdepth 1 -type f -iname '*appliedexpansion*' -print -quit | grep -q .; then
    printf 'AppliedExpansion must not be installed on the smoke-test server.\n' >&2
    exit 1
fi

printf 'eula=true\n' > "$SERVER_DIR/eula.txt"
set_server_property online-mode false
set_server_property white-list false

server_input="$RUN_DIR/server.stdin"
mkfifo "$server_input"
exec 3<> "$server_input"
(
    cd "$SERVER_DIR"
    exec setsid java \
        -Xms1G \
        -Xmx2500m \
        -Dfml.readTimeout=45 \
        @java9args.txt \
        -jar lwjgl3ify-forgePatches.jar \
        nogui < "$server_input"
) > "$SERVER_LOG" 2>&1 &
server_process=$!

wait_for_server
: > "$RUN_DIR/server.ready"

export DISPLAY=:99
Xvfb "$DISPLAY" -screen 0 854x480x24 -nolisten tcp > "$RUN_DIR/xvfb.log" 2>&1 &
xvfb_process=$!
for _ in $(seq 1 60); do
    [[ -S /tmp/.X11-unix/X99 ]] && break
    kill -0 "$xvfb_process" 2>/dev/null || exit 1
    sleep 0.5
done
test -S /tmp/.X11-unix/X99

printf 'maxFps:10\n' >> "$CLIENT_MC_DIR/options.txt"
(
    cd "$CLIENT_MC_DIR"
    exec setsid bash -c \
        'mapfile -t argv < "$2"; env $(grep -v "^[[:space:]]*#" "$1" | xargs) "${argv[0]}" "${argv[@]:1}"' \
        _ "$CLIENT_DIR/launch.env" "$CLIENT_DIR/launch.argv"
) > "$CLIENT_LOG" 2>&1 &
client_process=$!

wait_for_client_connection
test -e "$MAIN_MENU_MARKER"

printf 'PASS: client reached the main menu and connected to a server without AppliedExpansion installed.\n'
