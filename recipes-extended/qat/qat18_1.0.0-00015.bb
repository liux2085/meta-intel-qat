DESCRIPTION = "Intel(r) QuickAssist Technology API"
HOMEPAGE = "https://01.org/packet-processing/intel%C2%AE-quickassist-technology-drivers-and-patches"

#Dual BSD and GPLv2 License
LICENSE = "BSD & GPLv2"
LIC_FILES_CHKSUM = "\
                    file://${COMMON_LICENSE_DIR}/GPL-2.0;md5=801f80980d171dd6425610833a22dbe6 \
                    file://${COMMON_LICENSE_DIR}/BSD;md5=3775480a712fc46a69647678acb234cb \
                    "
DEPENDS += "boost udev zlib openssl yasm-native"
PROVIDES += "virtual/qat"

TARGET_CC_ARCH += "${LDFLAGS}"

SRC_URI = "https://01.org/sites/default/files/downloads/qat18.l.1.0.0-00015.tar.gz;subdir=qat18 \
           file://qat16_2.6.0-65-qat-override-CC-LD-AR-only-when-it-is-not-define.patch  \
           file://0001-update-KDIR-for-cross-compilation.patch \
           file://0002-Added-include-dir-path.patch \
           file://0003-qat-add-install-target-and-add-folder.patch \
          "

do_fetch[depends] += "virtual/kernel:do_shared_workdir"

SRC_URI[md5sum] = "1da7b12f6628d60d92a049399353b715"
SRC_URI[sha256sum] = "67fe1ec94cfd0af09ec7df4e671c3a61bd76d3231dd877e8eed5164e9d3d0244"

COMPATIBLE_MACHINE = "null"
COMPATIBLE_HOST_x86-x32 = 'null'
COMPATIBLE_HOST_libc-musl_class-target = 'null'

S = "${WORKDIR}/qat18"
ICP_TOOLS = "accelcomp"
SAMPLE_CODE_DIR = "${S}/quickassist/lookaside/access_layer/src/sample_code"
export INSTALL_MOD_PATH = "${D}"
export ICP_ROOT = "${S}"
export ICP_ENV_DIR = "${S}/quickassist/build_system/build_files/env_files"
export ICP_BUILDSYSTEM_PATH = "${S}/quickassist/build_system"
export ICP_TOOLS_TARGET = "${ICP_TOOLS}"
export FUNC_PATH = "${ICP_ROOT}/quickassist/lookaside/access_layer/src/sample_code/functional"
export INSTALL_FW_PATH = "${D}${base_libdir}/firmware"
export KERNEL_SOURCE_ROOT = "${STAGING_KERNEL_DIR}"
export ICP_BUILD_OUTPUT = "${D}"
export DEST_LIBDIR = "${libdir}"
export DEST_BINDIR = "${bindir}"
export QAT_KERNEL_VER = "${KERNEL_VERSION}"
export SAMPLE_BUILD_OUTPUT = "${D}"
export INSTALL_MOD_DIR = "${D}${base_libdir}/modules/${KERNEL_VERSION}"
export KERNEL_BUILDDIR = "${STAGING_KERNEL_BUILDDIR}"
export SC_EPOLL_DISABLED = "1"
export WITH_UPSTREAM = "1"
export WITH_CMDRV = "1"
export KERNEL_SOURCE_DIR = "${ICP_ROOT}/quickassist/qat/"
export ICP_NO_CLEAN = "1"
export ICP_QDM_IOMMU = "1"

inherit module
inherit update-rc.d
INITSCRIPT_NAME = "qat_service"

PARALLEL_MAKE = ""

EXTRA_OEMAKE_append = " CFLAGS+='-fgnu89-inline -fPIC'"
EXTRA_OEMAKE = "-e MAKEFLAGS="

do_compile () {
  export LD="${LD} --hash-style=gnu"
  export MACHINE="${TARGET_ARCH}"
  export SYSROOT="${STAGING_DIR_TARGET}"

  cd ${S}/quickassist/qat
  oe_runmake
  oe_runmake 'modules_install'

  cd ${S}/quickassist
  oe_runmake

  cd ${S}/quickassist/utilities/adf_ctl
  oe_runmake

  cd ${S}/quickassist/utilities/libusdm_drv
  oe_runmake

  cd ${S}/quickassist/lookaside/access_layer/src/qat_direct/src/
  oe_runmake

  #build the whole sample code: per_user only
  cd ${SAMPLE_CODE_DIR}
  oe_runmake 'perf_user'
}

do_install() {
  export MACHINE="${TARGET_ARCH}"

  cd ${S}/quickassist
  oe_runmake install

  cd ${S}/quickassist/qat
  oe_runmake modules_install

  install -d ${D}${sysconfdir}/udev/rules.d
  install -d ${D}${sbindir}
  install -d ${D}${sysconfdir}/conf_files
  install -d ${D}${prefix}/src/qat
  install -d ${D}${includedir}
  install -d ${D}${includedir}/dc
  install -d ${D}${includedir}/lac

  echo 'KERNEL=="qat_adf_ctl" MODE="0660" GROUP="qat"' > ${D}/etc/udev/rules.d/00-qat.rules
  echo 'KERNEL=="qat_dev_processes" MODE="0660" GROUP="qat"' >> ${D}/etc/udev/rules.d/00-qat.rules
  echo 'KERNEL=="usdm_drv" MODE="0660" GROUP="qat"' >> ${D}/etc/udev/rules.d/00-qat.rules
  echo 'KERNEL=="uio*" MODE="0660" GROUP="qat"' >> ${D}/etc/udev/rules.d/00-qat.rules
  echo 'KERNEL=="hugepages" MODE="0660" GROUP="qat"' >> ${D}/etc/udev/rules.d/00-qat.rules

  mkdir -p ${D}${base_libdir}

  install -D -m 0755 ${S}/quickassist/lookaside/access_layer/src/build/linux_2.6/user_space/libqat_s.so ${D}${base_libdir}
  install -D -m 0755 ${S}/quickassist/lookaside/access_layer/src/build/linux_2.6/user_space/libqat.a ${D}${base_libdir}
  install -D -m 0755 ${S}/quickassist/utilities/osal/src/build/linux_2.6/user_space/libosal_s.so ${D}${base_libdir}
  install -D -m 0755 ${S}/quickassist/utilities/osal/src/build/linux_2.6/user_space/libosal.a ${D}${base_libdir}
  install -D -m 0755 ${S}/quickassist/lookaside/access_layer/src/qat_direct/src/build/linux_2.6/user_space/libadf.a ${D}${base_libdir}/libadf.a
  install -D -m 0755 ${S}/quickassist/utilities/libusdm_drv/libusdm_drv_s.so ${D}${base_libdir}
  install -D -m 0755 ${S}/quickassist/utilities/libusdm_drv/libusdm_drv.a ${D}${base_libdir}
  install -D -m 0750 ${S}/quickassist/utilities/adf_ctl/adf_ctl ${D}${sbindir}

  install -D -m 640 ${S}/quickassist/utilities/adf_ctl/conf_files/*  ${D}${sysconfdir}/conf_files

  install -m 0755 ${S}/quickassist/qat/fw/qat_d15xx.bin  ${D}${nonarch_base_libdir}/firmware
  install -m 0755 ${S}/quickassist/qat/fw/qat_d15xx_mmp.bin  ${D}${nonarch_base_libdir}/firmware

  install -m 0755 ${S}/quickassist/qat/fw/qat_c4xxx.bin  ${D}${nonarch_base_libdir}/firmware
  install -m 0755 ${S}/quickassist/qat/fw/qat_c4xxx_mmp.bin  ${D}${nonarch_base_libdir}/firmware

  install -m 640 ${S}/quickassist/include/*.h  ${D}${includedir}
  install -m 640 ${S}/quickassist/include/dc/*.h  ${D}${includedir}/dc/
  install -m 640 ${S}/quickassist/include/lac/*.h  ${D}${includedir}/lac/
  install -m 640 ${S}/quickassist/lookaside/access_layer/include/*.h  ${D}${includedir}
  install -m 640 ${S}/quickassist/utilities/libusdm_drv/*.h  ${D}${includedir}

  install -m 0755 ${S}/quickassist/lookaside/access_layer/src/sample_code/performance/compression/calgary  ${D}${nonarch_base_libdir}/firmware
  install -m 0755 ${S}/quickassist/lookaside/access_layer/src/sample_code/performance/compression/calgary32  ${D}${nonarch_base_libdir}/firmware
  install -m 0755 ${S}/quickassist/lookaside/access_layer/src/sample_code/performance/compression/canterbury  ${D}${nonarch_base_libdir}/firmware

  #install qat source
  cp ${DL_DIR}/qat18.l.${PV}.tar.gz ${D}${prefix}/src/qat/
}

PACKAGES += "${PN}-app"

FILES_${PN}-dev = "${includedir}/ \
                   ${nonarch_base_libdir}/*.a \
                   "

FILES_${PN} += "\
                ${libdir}/ \
                ${nonarch_base_libdir}/firmware \
                ${sysconfdir}/ \
                ${sbindir}/ \
                ${base_libdir}/*.so \
                ${prefix}/src/qat \
                "

FILES_${PN}-dbg += "${sysconfdir}/init.d/.debug/ \
                    "

FILES_${PN}-app += "${bindir}/* \
                    ${prefix}/qat \
                    "
