#!/bin/bash

set -xe

IMAGENAME=eid-image
CNAME=eid
META_EID_DIR=$(dirname $(readlink -f "$0"))

E="docker exec -u1000 $CNAME /bin/bash -c "

docker run \
	--env http_proxy="$http_proxy" \
	--env https_proxy="$https_proxy" \
	--env no_proxy="$no_proxy" \
	--workdir /home/eid \
	--cap-add SYS_ADMIN \
	-v $META_EID_DIR:/home/eid/poky/meta-eid:ro \
	-u1000 \
	--rm \
	-i \
	-d \
	--name $CNAME \
	$IMAGENAME

# git proxy
test -n "$http_proxy" && $E "git config --global http.proxy $http_proxy"
test -n "$https_proxy" && $E "git config --global https.proxy $https_proxy"
test -n "$no_proxy" && $E "git config --global core.noproxy $no_proxy"

# rebuild chroot
$E "sudo rm -rf /etc/schroot/chroot.d/buster-amd64-* /home/eid/build/buster-amd64-*"
$E "source ./poky/meta-eid/setup.sh; sudo ../poky/meta-eid/scripts/setup-sbuild.sh"

# delete existing configuration
$E "sudo rm -rf /home/eid/build/conf"

BITBAKE_TARGETS="hello localfiles foo"
for bb in $BITBAKE_TARGETS; do
	$E "source ./poky/meta-eid/setup.sh; USER=eid bitbake $bb"
done
