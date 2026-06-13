#!/usr/bin/env python3
"""Patch ELF .so files for Android 15+ (16KB page size) using LIEF."""
import sys, os, glob

try:
    import lief
except ImportError:
    print("pip install lief required"); sys.exit(1)

TARGET = 16384

def patch_file(path):
    binary = lief.ELF.parse(path)
    if binary is None: return False
    for seg in binary.segments:
        if seg.type == lief.ELF.Segment.TYPE.LOAD:
            seg.alignment = TARGET
            vmod = seg.virtual_address % TARGET
            omod = seg.file_offset % TARGET
            if vmod != omod:
                diff = (omod - vmod) % TARGET
                seg.virtual_address += diff
                seg.physical_address += diff
    binary.write(path)
    # Verify
    v = lief.ELF.parse(path)
    for seg in v.segments:
        if seg.type == lief.ELF.Segment.TYPE.LOAD:
            if seg.alignment < TARGET or (seg.virtual_address % TARGET) != (seg.file_offset % TARGET):
                return False
    return True

for pattern in sys.argv[1:]:
    for path in glob.glob(pattern, recursive=True):
        if path.endswith('.so') and os.path.getsize(path) > 1000:
            ok = patch_file(path)
            print(f"{'OK' if ok else 'FAIL'}: {path}")
