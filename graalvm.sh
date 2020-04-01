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

GRAALVM_BASENAME="${GRAALVM_BASENAME:-graalvm}"
GRAALVM_VERSION="${GRAALVM_VERSION:-latest}"
GRAALVM_TARGET="${GRAALVM_TARGET:-$(pwd)/.${GRAALVM_BASENAME}}"
GRAALVM_EDITION="${GRAALVM_EDITION:-ce}"
GRAALVM_TARGET_JDK="${GRAALVM_TARGET_JDK:-java11}"

GRAALVM_REPO_OWNER="graalvm"
GRAALVM_REPO="graalvm-ce-builds"

# shellcheck source=download.sh
. "$(dirname "$(readlink -f "${0}")")/download.sh"

platform() {
  local platform
  platform="UNKNOWN:$(uname -s)"
  case "$(uname -s)" in
      Linux*)     platform="linux-amd64";;
      Darwin*)    platform="darwin-amd64";;
      CYGWIN*)    platform="windows-amd64";;
      MINGW*)     platform="windows-amd64";;
  esac
  echo "${platform}"
}

platform_regex() {
  local platform
  platform="UNKNOWN:$(uname -s)"
  case "$(uname -s)" in
      Linux*)     platform="linux-amd64";;
      Darwin*)    platform="(macos|darwin)-amd64";;
      CYGWIN*)    platform="windows-amd64";;
      MINGW*)     platform="windows-amd64";;
  esac
  echo "${platform}"
}

github_releaseinfo() {
  local owner="${1}"
  local repo="${2}"
  local versiontag="${3:-latest}"
  local releaseinfo="${4:-"$(pwd)/${owner}_${repo}_${versiontag}.releaseinfo"}"

  local code=0
  if [[ "${versiontag}" = "latest" ]]; then
    code=$(download_withcache "https://api.github.com/repos/${owner}/${repo}/releases/${versiontag}" "${releaseinfo}")
  else
    code=$(download_withcache "https://api.github.com/repos/${owner}/${repo}/releases/tags/${versiontag}" "${releaseinfo}")
  fi

  if [[ -f "${releaseinfo}" ]]; then
    cat "${releaseinfo}"
  else
    >&2 echo "ERROR: unable to retrieve release info for '${owner}/${repo}' version '${versiontag}'"
    return 1
  fi
}

install_ca_bundle() {
  local versiontag="${1:-${GRAALVM_VERSION}}"

  local code_mk_ca_bundle
  code_mk_ca_bundle="$(download_withcache "https://raw.githubusercontent.com/curl/curl/master/lib/mk-ca-bundle.pl" "${GRAALVM_TARGET}/mk-ca-bundle.pl")"
  local certdata_code
  certdata_code="$(download_withcache "https://github.com/mozilla/gecko-dev/raw/master/security/nss/lib/ckfw/builtins/certdata.txt" "${GRAALVM_TARGET}/certdata.txt")"
  local code_keyutil
  code_keyutil="$(download_withcache "https://github.com/use-sparingly/keyutil/releases/download/0.4.0/keyutil-0.4.0.jar" "${GRAALVM_TARGET}/keyutil-0.4.0.jar")"
  
  if [[ "${code_mk_ca_bundle}" -ge "400" ]] || [[ "${certdata_code}" -ge "400" ]] || [[ "${code_keyutil}" -ge "400" ]] ; then
    log_error "there has been error while retrieving certificates data files."
    return 1
  fi

  if [[ "${code_mk_ca_bundle}" -eq "200" ]] || [[ "${certdata_code}" -eq "200" ]] || [[ "${code_keyutil}" -eq "200" ]] ; then
    # at lest one of the file has been updated
    log_debug "generating cacert"
    pushd "${GRAALVM_TARGET}" &> /dev/null
    $(perl_bin) mk-ca-bundle.pl -n > ca-bundle.crt
    $(java_bin "${versiontag}") -jar keyutil-0.4.0.jar --import --new-keystore trustStore.jks --password changeit --force-new-overwrite --import-pem-file ca-bundle.crt
    cp -f trustStore.jks "$(graalvm_home "${versiontag}")/lib/security/cacerts"
    popd &> /dev/null
    log_debug "cacerts file for setup %s has been updated" "$(realpath --relative-to="$(pwd)" "$(graalvm_home "${versiontag}")")"
  fi
}

install() {
  local versiontag="${1:-${GRAALVM_VERSION}}"
  local code
  code="$(download_withcache "$(graalvm_url "${versiontag}")" "$(graalvm_file "${versiontag}")")"

  if [[ "${code}" -eq "200" ]]; then
    rm -rf "$(graalvm_folder "${versiontag}")" && mkdir -p "$(graalvm_folder "${versiontag}")"
    log_debug "expanding archive %s" "$(graalvm_file "${versiontag}")"
    expand "$(graalvm_file "${versiontag}")" "$(graalvm_folder "${versiontag}")" 
  elif [[ "${code}" -ge "400" ]]; then
    log_error "Error downloading GraalVM, code = ${code}"
    return 1
  fi

  install_ca_bundle "${versiontag}"

  if [[ ! -x "$(graalvm_home "${versiontag}")/bin/native-image" ]]; then
    "$(graalvm_home "${versiontag}")/bin/gu" install native-image
  fi
}

perl_bin() {
  if [[ "$(platform)" =~ "windows" ]]; then
    echo "perl.exe"
  else
    echo "perl"
  fi
}

java_bin() {
  local versiontag="${1:-${GRAALVM_VERSION}}"
  if [[ "$(platform)" =~ "windows" ]]; then
    echo "$(graalvm_home "${versiontag}")/bin/javaw.exe"
  else
    echo "$(graalvm_home "${versiontag}")/bin/java"
  fi
}

clean() {
  rm -rf "${GRAALVM_TARGET}"
}

graalvm_actualversion() {
  local versiontag="${1:-${GRAALVM_VERSION}}"
  graalvm_releaseinfo "${versiontag}" | jq -r '.tag_name'
}

graalvm_file() {
  local versiontag="${1:-${GRAALVM_VERSION}}"
  if [[ "$(platform)" =~ "windows" ]]; then
    echo "${GRAALVM_TARGET}/${GRAALVM_BASENAME}-${GRAALVM_EDITION}-${GRAALVM_TARGET_JDK}-$(platform)-$(graalvm_actualversion "${versiontag}").zip"
  else
    echo "${GRAALVM_TARGET}/${GRAALVM_BASENAME}-${GRAALVM_EDITION}-${GRAALVM_TARGET_JDK}-$(platform)-$(graalvm_actualversion "${versiontag}").tar.gz"
  fi
}

graalvm_folder() {
  local versiontag="${1:-${GRAALVM_VERSION}}"
  echo "${GRAALVM_TARGET}/${GRAALVM_BASENAME}-${GRAALVM_EDITION}-${GRAALVM_TARGET_JDK}-$(platform)-$(graalvm_actualversion "${versiontag}")"
}

graalvm_releaseinfo() {
  local versiontag="${1:-${GRAALVM_VERSION}}"
  github_releaseinfo "${GRAALVM_REPO_OWNER}" "${GRAALVM_REPO}" "${versiontag}" "${GRAALVM_TARGET}/${GRAALVM_BASENAME}-${GRAALVM_EDITION}-${GRAALVM_TARGET_JDK}-$(platform)-${versiontag}.releaseinfo"
}

graalvm_url() {
  local versiontag="${1:-${GRAALVM_VERSION}}"
  graalvm_releaseinfo "${versiontag}" | jq -r '.assets[] | select(.name | test("graalvm.*'"${GRAALVM_TARGET_JDK}"'.*'"$(platform_regex)"'")) | .browser_download_url'
}

graalvm_home_suffix() {
  if [[ "$(platform)" =~ "darwin" ]]; then
    echo "/Contents/Home"
  fi
}

graalvm_home() {
  local versiontag="${1:-${GRAALVM_VERSION}}"
  echo "$(graalvm_folder "${versiontag}")$(graalvm_home_suffix)"
}

sunec() {
  local sunec=""
  case "$(platform)" in
      linux*)     sunec="jre/lib/amd64/libsunec.so";;
      darwin*)    sunec="jre/lib/libsunec.dylib";;
      windows*)   sunec="jre/bin/sunec.dll";;
  esac

  echo "$(graalvm_home)/${sunec}"
}

if [[ -n "${1+x}" ]]; then
  "${1}" "${@:2}"
fi