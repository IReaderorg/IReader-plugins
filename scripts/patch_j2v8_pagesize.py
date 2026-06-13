#!/usr/bin/env python3
"""
Patch J2V8 native libraries for Android 15+ (16KB page size) compatibility.

Android 15+ enforces 16KB page size. The linker checks:
  1. p_align >= page_size for PT_LOAD segments
  2. (p_vaddr % page_size) == (p_offset % page_size) for PT_LOAD segments

This script uses LIEF to properly adjust ELF segment alignment and congruence.
"""
import sys
import os
import io
import zipfile
import urllib.request
import tempfile

try:
    import lief
except ImportError:
    print("ERROR: LIEF is required. Install with: pip install lief")
    sys.exit(1)

J2V8_VERSION = os.environ.get("J2V8_VERSION", "6.2.1")
J2V8_AAR_URL = f"https://repo1.maven.org/maven2/com/eclipsesource/j2v8/j2v8/{J2V8_VERSION}/j2v8-{J2V8_VERSION}.aar"
TARGET_PAGE_SIZE = 16384
ABIS = ["arm64-v8a", "armeabi-v7a", "x86", "x86_64"]


def download_aar(url, cache_dir):
    aar_path = os.path.join(cache_dir, f"j2v8-{J2V8_VERSION}.aar")
    if os.path.exists(aar_path):
        print(f"Using cached AAR: {aar_path}")
        return aar_path
    print(f"Downloading AAR from {url}...")
    data = urllib.request.urlopen(url).read()
    with open(aar_path, "wb") as f:
        f.write(data)
    print(f"Downloaded {len(data)} bytes")
    return aar_path


def patch_elf_for_16kb_pages(so_data, abi):
    """Patch ELF to use 16KB page alignment using LIEF."""
    binary = lief.ELF.parse(list(so_data))
    if binary is None:
        print(f"  ERROR: Could not parse ELF for {abi}")
        return None

    load_segs = [s for s in binary.segments if s.type == lief.ELF.Segment.TYPE.LOAD]
    if not load_segs:
        print(f"  WARNING: No PT_LOAD segments for {abi}")
        return so_data

    # Check if already patched
    all_ok = True
    for seg in load_segs:
        if seg.alignment < TARGET_PAGE_SIZE:
            all_ok = False
            break
        if (seg.virtual_address % TARGET_PAGE_SIZE) != (seg.file_offset % TARGET_PAGE_SIZE):
            all_ok = False
            break

    if all_ok:
        print(f"  {abi}: Already compatible with 16KB pages")
        return so_data

    # Fix each PT_LOAD segment
    for seg in load_segs:
        # Set alignment to 16384
        seg.alignment = TARGET_PAGE_SIZE

        # Fix congruence: vaddr % page_size == offset % page_size
        offset_mod = seg.file_offset % TARGET_PAGE_SIZE
        vaddr_mod = seg.virtual_address % TARGET_PAGE_SIZE
        if vaddr_mod != offset_mod:
            diff = (offset_mod - vaddr_mod) % TARGET_PAGE_SIZE
            seg.virtual_address += diff
            seg.physical_address += diff

    # Write
    builder = lief.ELF.Binary.write(binary)
    result = bytes(builder)

    # Verify
    patched = lief.ELF.parse(list(result))
    patched_load = [s for s in patched.segments if s.type == lief.ELF.Segment.TYPE.LOAD]
    for seg in patched_load:
        vmod = seg.virtual_address % TARGET_PAGE_SIZE
        omod = seg.file_offset % TARGET_PAGE_SIZE
        ok = (seg.alignment >= TARGET_PAGE_SIZE) and (vmod == omod)
        if not ok:
            print(f"  WARNING: {abi} verification failed after patch!")
            return None

    print(f"  {abi}: Patched ({len(so_data)} -> {len(result)} bytes)")
    return result


def main():
    output_dir = sys.argv[1] if len(sys.argv) > 1 else "plugins/engines/j2v8-engine/src/main/jniLibs"
    cache_dir = sys.argv[2] if len(sys.argv) > 2 else "build/download-cache"

    os.makedirs(cache_dir, exist_ok=True)

    aar_path = download_aar(J2V8_AAR_URL, cache_dir)

    patched_count = 0
    with zipfile.ZipFile(aar_path) as z:
        for abi in ABIS:
            entry_name = f"jni/{abi}/libj2v8.so"
            try:
                so_data = z.read(entry_name)
                print(f"\nPatching {abi} ({len(so_data)} bytes)...")
                patched = patch_elf_for_16kb_pages(so_data, abi)
                if patched is not None:
                    out_path = os.path.join(output_dir, abi, "libj2v8.so")
                    os.makedirs(os.path.dirname(out_path), exist_ok=True)
                    with open(out_path, "wb") as f:
                        f.write(patched)
                    patched_count += 1
            except KeyError:
                print(f"  {abi} not found in AAR, skipping")

    print(f"\nPatched {patched_count}/{len(ABIS)} ABIs for 16KB page alignment")
    return 0 if patched_count > 0 else 1


if __name__ == "__main__":
    sys.exit(main())
