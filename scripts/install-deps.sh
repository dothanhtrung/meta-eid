#!/bin/sh

DEP_PKGS="python3 python3-debian sbuild dh-make reprepro schroot debootstrap fakeroot"
HOST_TMP_DIR="/tmp"

warn() {
	echo "WARNING: ${@}"
}

die() {
	echo "ERROR: ${@}"
	exit 1
}

if [ "$(whoami)" != "root" ]; then
	die "Please run this script as root"
fi

if [ ! -r /etc/os-release ]; then
	warn "/etc/os-release not found, failed to check the host distro"
fi
OS_ID=$(grep "^ID=" /etc/os-release | sed "s@^ID=\(.*\)@\1@")
if [ "${OS_ID}" != "debian" ]; then
	warn "\"${OS_ID}\" is not a tested distro, \
some dependencies might not be satisfied"
fi

apt-get install ${DEP_PKGS} || die "failed to install dependent packages"

#download and install qemu-user-static and binfmt-support jessie packages because default version on buster could not work normally.
wget ftp.us.debian.org/debian/pool/main/b/binfmt-support/binfmt-support_2.1.5-1_amd64.deb -P ${HOST_TMP_DIR}
wget http://security.debian.org/debian-security/pool/updates/main/q/qemu/qemu-user-static_2.1+dfsg-12+deb8u11_amd64.deb -P ${HOST_TMP_DIR}

dpkg -i ${HOST_TMP_DIR}/binfmt-support_2.1.5-1_amd64.deb
dpkg -i ${HOST_TMP_DIR}/qemu-user-static_2.1+dfsg-12+deb8u11_amd64.deb

#remove downloaded package after installing completed
rm  ${HOST_TMP_DIR}/binfmt-support_2.1.5-1_amd64.deb
rm  ${HOST_TMP_DIR}/qemu-user-static_2.1+dfsg-12+deb8u11_amd64.deb
