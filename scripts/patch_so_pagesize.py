#!/usr/bin/env python3
"""Minimal ELF patcher - only changes p_align in-place, no binary rewrite."""
import struct, sys, os, glob

TARGET_ALIGN = 16384
PT_LOAD = 1

def patch_p_align(path):
    with open(path, 'r+b') as f:
        magic = f.read(4)
        if magic != b'\x7fELF':
            return False
        f.seek(4)
        elf_class = f.read(1)[0]  # 1=32bit, 2=64bit
        is_64 = elf_class == 2
        data_byte = f.read(1)[0]  # 1=LE, 2=BE
        endian = '<' if data_byte == 1 else '>'
        
        if is_64:
            f.seek(0x20)
            phoff = struct.unpack(f'{endian}Q', f.read(8))[0]
            f.seek(0x36)
            phentsize = struct.unpack(f'{endian}H', f.read(2))[0]
            f.seek(0x38)
            phnum = struct.unpack(f'{endian}H', f.read(2))[0]
            p_align_off = 0x30
            p_type_off = 0x00
        else:
            f.seek(0x1C)
            phoff = struct.unpack(f'{endian}I', f.read(4))[0]
            f.seek(0x2A)
            phentsize = struct.unpack(f'{endian}H', f.read(2))[0]
            f.seek(0x2C)
            phnum = struct.unpack(f'{endian}H', f.read(2))[0]
            p_align_off = 0x1C
            p_type_off = 0x00
        
        patched = 0
        for i in range(phnum):
            entry = phoff + i * phentsize
            f.seek(entry + p_type_off)
            p_type = struct.unpack(f'{endian}I', f.read(4))[0]
            if p_type != PT_LOAD:
                continue
            f.seek(entry + p_align_off)
            fmt = f'{endian}Q' if is_64 else f'{endian}I'
            old_align = struct.unpack(fmt, f.read(struct.calcsize(fmt)))[0]
            if old_align > 0 and old_align < TARGET_ALIGN:
                f.seek(entry + p_align_off)
                f.write(struct.pack(fmt, TARGET_ALIGN))
                patched += 1
                print(f'  PT_LOAD[{i}]: p_align {old_align} -> {TARGET_ALIGN}')
            else:
                print(f'  PT_LOAD[{i}]: p_align {old_align} (no change needed)')
    return patched > 0

for pattern in sys.argv[1:]:
    for path in glob.glob(pattern, recursive=True):
        if path.endswith('.so') and os.path.getsize(path) > 1000:
            print(f'{path}:')
            ok = patch_p_align(path)
            print(f'  Result: {"patched" if ok else "no change/error"}')
