#! /usr/bin/env bash
#*******************************************************************************
# Copyright (c) 2019 Eclipse Foundation and others.
# This program and the accompanying materials are made available
# under the terms of the Eclipse Public License 2.0
# which is available at http://www.eclipse.org/legal/epl-v20.html
# SPDX-License-Identifier: EPL-2.0
#*******************************************************************************

# Bash strict-mode
set -o errexit
set -o nounset
set -o pipefail

ADOPTOPENJDK_BASENAME="${ADOPTOPENJDK_BASENAME:-adoptopenjdk}"
ADOPTOPENJDK_VERSION="${ADOPTOPENJDK_VERSION:-latest}"
ADOPTOPENJDK_TYPE="${ADOPTOPENJDK_TYPE:-jdk}"
ADOPTOPENJDK_HEAP_SIZE="${ADOPTOPENJDK_HEAP_SIZE:-normal}"
ADOPTOPENJDK_TARGET="${ADOPTOPENJDK_TARGET:-$(pwd)/.${ADOPTOPENJDK_BASENAME}}"

# shellcheck source=download.sh
. "$(dirname "$(readlink -f "${0}")")/download.sh"

os() {
  local os="UNKNOWN:$(uname -s)"
  case "$(uname -s)" in
      Linux*)     os="linux";;
      Darwin*)    os="mac";;
      CYGWIN*)    os="windows";;
      MINGW*)     os="windows";;
  esac
  echo "${os}"
}

arch() {
  local arch
  arch="UNKNOWN:$(uname -m)"
  case "$(uname -m)" in
      x86_64*)    arch="x64";;
      amd64*)     arch="x64";;
      i386*)      arch="x32";;
      i686*)      arch="x32";;
  esac
  echo "${arch}"
}

install() {
  local version="${1}"
  local vm="${2}"
  local release="${3:-${ADOPTOPENJDK_VERSION}}"
  local type="${4:-${ADOPTOPENJDK_TYPE}}"
  local heap_size="${5:-${ADOPTOPENJDK_HEAP_SIZE}}"
  
  local adoptopenjdk_file
  adoptopenjdk_file="${version}_${vm}-${type}-$(os)_$(arch)-${heap_size}-${release}"

  mkdir -p "${ADOPTOPENJDK_TARGET}"
  local code=0
  code=$(download_withcache "https://api.adoptopenjdk.net/v2/info/releases/${version}?openjdk_impl=${vm}&os=$(os)&arch=$(arch)&release=${release}&type=${type}&heap_size=${heap_size}" "${ADOPTOPENJDK_TARGET}/${adoptopenjdk_file}.json")

  if [[ ${code} -lt 400 ]]; then
    local url
    url="$(jq -r '.binaries[].binary_link' < "${ADOPTOPENJDK_TARGET}/${adoptopenjdk_file}.json")"
    local binary_name
    binary_name="$(jq -r '.binaries[].binary_name' < "${ADOPTOPENJDK_TARGET}/${adoptopenjdk_file}.json")"
    code=$(download_withcache "${url}" "${ADOPTOPENJDK_TARGET}/${binary_name}")

    if [[ "${code}" == "200" ]] || [[ ! -d "${ADOPTOPENJDK_TARGET}/${adoptopenjdk_file}" ]]; then
      rm -rf "${ADOPTOPENJDK_TARGET:?}/${adoptopenjdk_file}"
      log_debug "expanding archive %s" "${ADOPTOPENJDK_TARGET}/${binary_name}"
      expand "${ADOPTOPENJDK_TARGET}/${binary_name}" "${ADOPTOPENJDK_TARGET}/${adoptopenjdk_file}"
    fi
  else
    return 2
  fi
}

java_home() {
  local version="${1}"
  local vm="${2}"
  local release="${3:-${ADOPTOPENJDK_VERSION}}"
  local type="${4:-${ADOPTOPENJDK_TYPE}}"
  local heap_size="${5:-${ADOPTOPENJDK_HEAP_SIZE}}"

  local adoptopenjdk_file
  adoptopenjdk_file="${version}_${vm}-${type}-$(os)_$(arch)-${heap_size}-${release}"

  if [[ "$(os)" == "mac" ]]; then
    echo "${ADOPTOPENJDK_TARGET}/${adoptopenjdk_file}/Contents/Home"
  else
    echo "${ADOPTOPENJDK_TARGET}/${adoptopenjdk_file}"
  fi
}

clean() {
  rm -rf "${ADOPTOPENJDK_TARGET}"
}

if [[ -n "${1+x}" ]]; then
  $1 "${@:2}"
fi