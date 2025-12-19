#!/usr/bin/env python3
"""
Split a grid image of icons into individual plugin icons.

Usage: python split_icon_grid.py <grid_image_path> [rows] [cols]

Example: python split_icon_grid.py Gemini_Generated_Image.png 4 4
"""

import sys
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    import os
    print("Installing pillow...")
    os.system("pip install pillow")
    from PIL import Image

# Plugin order - adjust this to match your grid image layout (left-to-right, top-to-bottom)
PLUGIN_ORDER = [
    # Row 1
    ("themes", "nordic-frost"),
    ("themes", "sakura-night"),
    ("themes", "cyber-neon"),
    ("themes", "arctic-aurora"),
    # Row 2
    ("themes", "desert-sand"),
    ("themes", "vintage-sepia"),
    ("themes", "mint-fresh"),
    ("themes", "coffee-bean"),
    # Row 3
    ("themes", "royal-velvet"),
    ("themes", "slate-gray"),
    ("themes", "coral-reef"),
    ("engines", "graalvm-engine"),
    # Row 4
    ("engines", "j2v8-engine"),
    ("engines", "quickjs-engine"),
    ("tts", "piper-tts"),
    None,  # Empty slot
]


def split_grid(image_path: str, rows: int = 4, cols: int = 4):
    """Split grid image into individual icons."""
    
    img = Image.open(image_path)
    width, height = img.size
    
    cell_width = width // cols
    cell_height = height // rows
    
    print(f"Image size: {width}x{height}")
    print(f"Grid: {rows}x{cols}")
    print(f"Cell size: {cell_width}x{cell_height}")
    print()
    
    script_dir = Path(__file__).parent.parent
    plugins_dir = script_dir / "plugins"
    
    idx = 0
    for row in range(rows):
        for col in range(cols):
            if idx >= len(PLUGIN_ORDER):
                break
                
            plugin_info = PLUGIN_ORDER[idx]
            idx += 1
            
            if plugin_info is None:
                continue
            
            category, plugin_id = plugin_info
            
            # Calculate crop box
            left = col * cell_width
            top = row * cell_height
            right = left + cell_width
            bottom = top + cell_height
            
            # Crop the icon
            icon = img.crop((left, top, right, bottom))
            
            # Resize to 512x512 for consistency
            icon = icon.resize((512, 512), Image.Resampling.LANCZOS)
            
            # Save to plugin assets
            assets_dir = plugins_dir / category / plugin_id / "assets"
            assets_dir.mkdir(parents=True, exist_ok=True)
            
            output_path = assets_dir / "icon.png"
            icon.save(output_path, "PNG")
            
            print(f"✓ {category}/{plugin_id} -> {output_path}")
    
    print()
    print(f"Done! Split {idx} icons.")
    print("Run './gradlew repo' to copy icons to the repository.")


def main():
    if len(sys.argv) < 2:
        print("Usage: python split_icon_grid.py <grid_image_path> [rows] [cols]")
        print("Example: python split_icon_grid.py grid.png 4 4")
        print()
        print("Default grid: 4x4 (16 icons)")
        print("Edit PLUGIN_ORDER in the script to match your grid layout.")
        sys.exit(1)
    
    image_path = sys.argv[1]
    rows = int(sys.argv[2]) if len(sys.argv) > 2 else 4
    cols = int(sys.argv[3]) if len(sys.argv) > 3 else 4
    
    if not Path(image_path).exists():
        print(f"Error: Image not found: {image_path}")
        sys.exit(1)
    
    split_grid(image_path, rows, cols)


if __name__ == "__main__":
    main()
