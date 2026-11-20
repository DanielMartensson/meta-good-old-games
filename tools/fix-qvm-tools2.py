import sys

p = sys.argv[1] + "/cmake/utils/qvm_tools.cmake"
s = open(p).read()

old = """getenv(BUILD_CC QVM_HOST_CC)
getenv(BUILD_CPPFLAGS QVM_HOST_CPPFLAGS)
getenv(BUILD_CFLAGS QVM_HOST_CFLAGS)
getenv(BUILD_LDFLAGS QVM_HOST_LDFLAGS)
string(STRIP "${QVM_HOST_CC}" QVM_HOST_CC)"""

new = """set(QVM_HOST_CC "$ENV{BUILD_CC}")
set(QVM_HOST_CPPFLAGS "$ENV{BUILD_CPPFLAGS}")
set(QVM_HOST_CFLAGS "$ENV{BUILD_CFLAGS}")
set(QVM_HOST_LDFLAGS "$ENV{BUILD_LDFLAGS}")
string(STRIP "${QVM_HOST_CC}" QVM_HOST_CC)"""

assert s.count(old) == 1, ("anchor", s.count(old))
s = s.replace(old, new)
open(p, "w").write(s)
print("fixed", p)
