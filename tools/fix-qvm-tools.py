import sys

p = sys.argv[1] + "/cmake/utils/qvm_tools.cmake"
s = open(p).read()

old = """if(CMAKE_BUILD_TYPE)
    set(BUILD_TYPE_ARG -DCMAKE_BUILD_TYPE=${CMAKE_BUILD_TYPE})
endif()

set(Q3RCC ${TOOLS_DIR}/$<CONFIG>/q3rcc${HOST_EXECUTABLE_SUFFIX})"""

new = """if(CMAKE_BUILD_TYPE)
    set(BUILD_TYPE_ARG -DCMAKE_BUILD_TYPE=${CMAKE_BUILD_TYPE})
endif()

# The five programs this sub-project builds are run by the build machine, not
# by the machine this tree is being compiled for: cmake/tools/CMakeLists.txt:83
# pipes lburg into the C file q3rcc is linked with at :88, and the add_qvm
# custom commands below run q3lcc, q3rcc, q3cpp and q3asm over the game
# sources to make the .qvm files. That is what ${HOST_EXECUTABLE_SUFFIX} at
# :15-:18 already says they are. Left alone, though, ExternalProject hands
# this sub-build the parent cache compiler, flags, sysroot and toolchain file,
# so the sub-build links binaries for the target: configuring and linking
# q3asm and lburg succeed, and then the first thing that has to run, lburg,
# stops the build with "Exec format error" because the host cannot exec a
# target binary. Clearing those settings is what asks for the sub-build to
# be a build of the build machine, which is exactly how upstream builds
# these tools for its own release artifacts (.github/workflows/build.yml:67-
# :71 and :185 are a bare cmake, no cross compiler at all). What they emit is
# Q3VM bytecode, an interpreter format that code/qcommon/vm_interpreted.c
# reads wherever the game runs, so the .qvm files are the same bytes no
# matter which machine compiled them.
#
# A cross-building environment such as a distribution build system says which
# compiler the build machine is with BUILD_CC and gives BUILD_CPPFLAGS and
# BUILD_CFLAGS for the flags its own tools get; outside one, BUILD_CC is empty
# and cmake falls back to its own compiler probe, which is what a plain
# upstream build gets.
getenv(BUILD_CC QVM_HOST_CC)
getenv(BUILD_CPPFLAGS QVM_HOST_CPPFLAGS)
getenv(BUILD_CFLAGS QVM_HOST_CFLAGS)
getenv(BUILD_LDFLAGS QVM_HOST_LDFLAGS)
string(STRIP "${QVM_HOST_CC}" QVM_HOST_CC)
set(QVM_HOST_C_ARGS
    "-DCMAKE_C_COMPILER=${QVM_HOST_CC}"
    "-DCMAKE_C_FLAGS=${QVM_HOST_CPPFLAGS} ${QVM_HOST_CFLAGS}"
    "-DCMAKE_C_LINK_FLAGS=${QVM_HOST_LDFLAGS}"
    "-DCMAKE_SYSROOT="
    "-DCMAKE_TOOLCHAIN_FILE=")

set(Q3RCC ${TOOLS_DIR}/$<CONFIG>/q3rcc${HOST_EXECUTABLE_SUFFIX})"""

assert s.count(old) == 1, ("first anchor", s.count(old))
s = s.replace(old, new)

old2 = """        -DCMAKE_MINIMUM_REQUIRED_VERSION=${CMAKE_MINIMUM_REQUIRED_VERSION}
        ${BUILD_TYPE_ARG}
    BUILD_ALWAYS TRUE"""
new2 = """        -DCMAKE_MINIMUM_REQUIRED_VERSION=${CMAKE_MINIMUM_REQUIRED_VERSION}
        ${BUILD_TYPE_ARG}
        ${QVM_HOST_C_ARGS}
    BUILD_ALWAYS TRUE"""
assert s.count(old2) == 1, ("second anchor", s.count(old2))
s = s.replace(old2, new2)

open(p, "w").write(s)
print("patched", p)
