import sys

L = ("/home/ubuntu/Dokument/Github/Watermelon-Wine/watermelon-wine-os/"
     "layers/meta-embedded-apps/recipes-apps")
NEW = "a9de957a5902c54186e9d4b44b1562f0b6beb951"
SHORT = "a9de957a59"
OLD = "bec4a0b11fc77fb5bfe9584f012296333718e84b"
OSHORT = "bec4a0b11f"


def sub(path, pairs):
    with open(path) as f:
        t = f.read()
    for old, new in pairs:
        if t.count(old) != 1:
            sys.stderr.write("count of block in %s: %d\n%s\n" % (path, t.count(old), old))
            raise SystemExit(1)
        t = t.replace(old, new)
    with open(path, "w") as f:
        f.write(t)
    print("patched", path)


sub(L + "/opennow/opennow_git.bb", [
("""# The pin is the released v1.0.4 tag, not a nightly: v1.0.4 is
# bec4a0b11fc77fb5bfe9584f012296333718e84b. It is the tip of upstream's main
# branch, and its second parent is dev's 1ce8a38c0f450afcc1706213dc2f0327955813ef
# ("Merge pull request #1433 from OpenCloudGaming/dev", 8 Nov 2026). The fetch
# therefore moves to branch=main: v1.0.4 is not reachable from dev, so keeping
# the old branch=dev here would leave the fetcher unable to resolve this SRCREV
# at all, which bitbake reports as a missing pin rather than a compile error.""",
 """# The pin is the released v1.0.5 tag, not a nightly: v1.0.5 is
# a9de957a5902c54186e9d4b44b1562f0b6beb951, published 12 Nov 2026 and still the
# tip of upstream's main branch, its merge commit being "Merge pull request
# #1444 from OpenCloudGaming/dev" of 11 Nov 2026. The fetch therefore keeps
# branch=main: the tag sits on main and is resolvable from it, and the one way
# this recipe could break itself is a SRCREV main does not carry, which bitbake
# reports as a missing pin rather than as a build failure. v1.3.5 is a newer tag
# but is not on main, so it is not what the released line pins: the bump here is
# 1.0.4 to 1.0.5. What actually moved between the two pins, measured by diffing
# the two release trees on the build host, is native/opennow-core (cloudmatch.rs,
# gfn.rs, gfn/routing_tests.rs, main.rs), opennow-qt (CMakeLists, its cmake
# pieces, its qml and its packaging/icons) and docs/core-protocol.md.
# native/opennow-streamer is byte-identical on the two tags, which is what lets
# the opennowlite recipe next door keep its vendored tree as it is.""",
 ),
('SRCREV = "%s"' % OLD, 'SRCREV = "%s"' % NEW),
("""# layer-path.patch draws both directly and nothing else: no streaming, video,
# audio or input patch comes back, and that is the whole series here.""",
 """# layer-path.patch draws both directly and nothing else: no streaming, video,
# audio or input patch comes back, and that is the whole series here. The series
# is still valid at the new pin: `patch -p1 -d <v1.0.5 checkout> --dry-run`
# answers rc=0 for the one patch against DesktopSignInScreen.qml, so the hunks
# are the same lines on both tags. Whether v1.0.5 still dies under the seat's
# Vulkan driver is a seat-side question, not a reason to drop the patch: the
# card stays out of the layer pass either way.""",
 ),
("""# The version has to sort ABOVE what the boards already carry (1.0.4+git1+b3fe71026d
#-r0.74 from the old nightly pin), which is why the release segment is spelled out:
# do_packagedata's version-going-backwards QA check compares segment by segment, and
# bare "1.0.4+git0+bec4a0b11f" loses against "1.0.4+git1+..." on the commit count.
# +release+ also says what this pin is: the released v1.0.4 tag, not a nightly.
PV = "1.0.4+release+git${SRCPV}"
# The released tag reports 1.0.4 stable (the nightly pin before it reported
# 1.0.4-nightly.1515.1). PKGR stays pinned instead of the PR-server counter,
# which resets to r0.0 whenever the recipe changes and would then install an
# older version string over what the boards already carry.
PKGR = "r0.76\"""",
 """# The version has to sort ABOVE what the boards already carry (1.0.4+git1+b3fe71026d
#-r0.74 from the old nightly pin, and 1.0.4+release+git0+bec4a0b11f-r0.76 from the
# pin this one replaces), which is why the release segment is spelled out:
# do_packagedata's version-going-backwards QA check compares segment by segment,
# and bare "1.0.5+git0+a9de957a59" loses against a "+git1+..." commit count.
# +release+ also says what this pin is: the released v1.0.5 tag, not a nightly.
PV = "1.0.5+release+git${SRCPV}"
# The released tag reports 1.0.5 stable (the pin before it reported 1.0.4 stable and
# the nightly before that reported 1.0.4-nightly.1515.1). PKGR stays pinned instead of
# the PR-server counter, which resets to r0.0 whenever the recipe changes and would
# then install an older version string over what the boards already carry; it moves by
# one alongside the bump so the new deb installs over the installed 1.0.4 one.
PKGR = "r0.77\""""),
])

sub(L + "/opennowlite/opennowlite_git.bb", [
("streamer of the released upstream OpenNOW v1.0.4 is used only to carry the \\",
 "streamer of the released upstream OpenNOW v1.0.5 is used only to carry the \\"),
("""# is the reference tree of upstream's OpenNOW v1.0.4 = bec4a0b11fc77fb5bfe9584f012296333718e84b
# (the tag on refs/heads/main, not on dev, which is the same commit the opennow
# recipe pins and fetches) after this recipe's one patch, plus the further edits
# the development session that owned that tree made on top of it. So the pin is
# recorded here instead of fetched, and the patch stays in files/ as documentation
# of the delta against pristine upstream rather than as something to apply: its
# hunks are already folded into files/native, and a second application fails.""",
 """# is the reference tree of upstream's OpenNOW v1.0.5 = a9de957a5902c54186e9d4b44b1562f0b6beb951
# (the tag on refs/heads/main and the tip of main itself, which is the same commit
# the opennow recipe pins and fetches) after this recipe's one patch, plus the further
# edits the development session that owned that tree made on top of it. So the pin is
# recorded here instead of fetched, and the patch stays in files/ as documentation
# of the delta against pristine upstream rather than as something to apply: its
# hunks are already folded into files/native, and a second application fails. That
# v1.0.5 wording is measured, not relabelled: diffing the opennow-streamer subtree of
# the v1.0.4 and the v1.0.5 release trees on the build host answers with no differing
# file, so the tree this recipe vendors is the v1.0.5 streamer tree already, and the
# v1.0.5 bump moves the version string and nothing in the source.""",
 ),
("""# The vendored tree descends from that tag, so the version keeps saying so. It
# is written out rather than computed from ${SRCPV}, because there is no longer
# a fetched commit to compute a tip from, and it still has to sort above the
# nightly string the boards carry (1.0.4+nightly.1515.1+git0+b3fe71026d-r2.11):
# do_packagedata's version-going-backwards QA check compares segment by segment,
# and "git0..." loses against "nightly...".
PV = "1.0.4+release+git0+bec4a0b11f\"""",
 """# The vendored tree descends from that tag, so the version keeps saying so. It
# is written out rather than computed from ${SRCPV}, because there is no longer
# a fetched commit to compute a tip from, and it still has to sort above the
# nightly string the boards carry (1.0.4+nightly.1515.1+git0+b3fe71026d-r2.11):
# do_packagedata's version-going-backwards QA check compares segment by segment,
# and "git0..." loses against "nightly...".
PV = "1.0.5+release+git0+%s\"""" % SHORT),
("""# PR-server counter resets to r0.0 whenever the recipe changes; pin it so the
# deb always installs over what the boards already carry, which is exactly
# 1.0.4+release+git0+bec4a0b11f-r2.12. The source moved into this layer without
# moving the counter: a deb of the same version installs over the installed one,
# and the round that re-measures the rebuilt client on a seat owns the bump, so
# a seat still reporting r2.12 is not evidence about which source it runs.""",
 """# PR-server counter resets to r0.0 whenever the recipe changes; pin it so the
# deb always installs over what the boards already carry, which was exactly
# 1.0.4+release+git0+bec4a0b11f-r2.12. The source moved into this layer without
# moving the counter, and the v1.0.4 to v1.0.5 bump does not move it either:
# nothing in the vendored streamer tree changed between the two tags, so r2.12
# is still what says which source the client runs, and the round that re-measures
# the rebuilt client on a seat owns moving it.""",
 ),
])
