#
# A sample of image generator only using debootstrap
#
# NOTE: sudo is temporally used, so NOPASSWD required in sudoers
# TODO: Support multiarch packages and cross image generation
#

# packages in non local apt repositories (Debian mirrors)
DEB_RDEPENDS = "openssh-server"

# packages in the local apt repository provided by individual recipes
RDEPENDS = "hello foo"
do_rootfs[rdeptask] = "do_deploy_deb"
# TODO: If multiple repositories provide the same package,
# the highest version is selected even if the local repository
# includes the package that user intentionally customized.
INSTALL_PKGS = "${DEB_RDEPENDS} ${RDEPENDS}"

ROOTFS = "${WORKDIR}/rootfs"

ROOTFS_APT_REPO_DIR = "/apt"
ROOTFS_SOURCES_LIST = "/etc/apt/sources.list.d/local.list"

SUDO = "sudo -E http_proxy=${http_proxy}"
CHROOT = "${SUDO} chroot ${ROOTFS}"

ROOTFS_SIZE ?= "500000"

# Image type: ext2, ext3, ext4, img
IMAGE_SUFFIX ?= "ext3"
FSTYPE ?= "ext3"

# Change these if you want default mkfs behavior (i.e. create minimal inode number)
EXTRA_IMAGECMD ?= "-i 4096"

# TODO: drop root privilege using fakeroot/fakechroot
do_rootfs[dirs] = "${WORKDIR}"
do_rootfs() {
	if [ -d ${ROOTFS} ]; then
		bbnote "Cleaning old rootfs directory"
		${SUDO} rm -r ${ROOTFS}
	fi
	bbnote "Running debootstrap"
	${SUDO} debootstrap ${DEBIAN_CODENAME} ${ROOTFS} ${DEBIAN_REPO}

	bbnote "Copying local apt repository to rootfs"
	${SUDO} cp -r ${APT_REPO_DIR} ${ROOTFS}/${ROOTFS_APT_REPO_DIR}
	# TODO: create Release file (now ignored by trusted=yes)
	bbnote "Registering local apt repository to apt in rootfs"
	${CHROOT} sh -c "echo \"deb [trusted=yes] file://${ROOTFS_APT_REPO_DIR} ${DEBIAN_CODENAME} main\" \
		> ${ROOTFS_SOURCES_LIST}"
	${CHROOT} apt update

	bbnote "Upgrading packages available in local apt repository"
	${CHROOT} apt full-upgrade -y

	bbnote "Installing required packages"
	${CHROOT} apt install -y ${INSTALL_PKGS}

	# TODO: Run postinst commands

	# deploy tarball
	mkdir -p ${DEPLOY_DIR}
	bbnote "Packing rootfs"
	cd ${ROOTFS}
	${SUDO} tar czf ${DEPLOY_DIR}/${PN}-${MACHINE}.tar.gz .

	# TODO: generate the final rootfs image (ext4, tarball, etc.)
}
addtask rootfs before do_build

# Generate the final rootfs image (ext2, ext3, ext4, sdimg)
do_image () {
        # If generating an empty image the size of the sparse block should be large
        # enough to allocate an ext4 filesystem using 4096 bytes per inode, this is
        # about 60K, so dd needs a minimum count of 60, with bs=1024 (bytes per IO)
        COUNT=0
        MIN_COUNT=60
        if [ ${ROOTFS_SIZE} < $MIN_COUNT ]; then
                COUNT=$MIN_COUNT
        fi

	# Create a system image
	dd if=/dev/zero of=${DEPLOY_DIR}/${PN}-${MACHINE}.${IMAGE_SUFFIX} bs=1024 seek=${ROOTFS_SIZE} count=$COUNT

	# Create a file system and install rootfs to the image
	${SUDO} mkfs.${FSTYPE} -F ${EXTRA_IMAGECMD} ${DEPLOY_DIR}/${PN}-${MACHINE}.${IMAGE_SUFFIX} -d ${ROOTFS}
}

addtask image after do_rootfs before do_build
