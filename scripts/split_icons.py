#!/usr/bin/env python3
"""
Split a 4x4 grid image into individual plugin icons.
"""
from PIL import Image
import os

# Source image
SOURCE_IMAGE = "../repo/Gemini_Generated_Image_9378yo9378yo9378.png"

# Plugin folders in order (left to right, top to bottom in the grid)
PLUGINS = [
    # Row 1
    "gradio-edge-tts",
    "gradio-persian-edge-tts", 
    "gradio-persian-chatterbox",
    "gradio-persian-piper",
    # Row 2
    "gradio-xtts-v2",
    "gradio-persian-xtts",
    "gradio-bark-tts",
    "gradio-parler-tts",
    # Row 3
    "gradio-style-tts-2",
    "gradio-tortoise-tts",
    "gradio-silero-tts",
    "gradio-openvoice",
    # Row 4
    "gradio-fish-speech",
    None,  # Empty slot
    None,  # Empty slot
    None,  # Empty slot
]

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    source_path = os.path.join(script_dir, SOURCE_IMAGE)
    
    # Open the grid image
    img = Image.open(source_path)
    width, height = img.size
    print(f"Source image size: {width}x{height}")
    
    # Calculate cell size (4x4 grid)
    cell_width = width // 4
    cell_height = height // 4
    print(f"Cell size: {cell_width}x{cell_height}")
    
    # Extract each icon
    for idx, plugin_name in enumerate(PLUGINS):
        if plugin_name is None:
            continue
            
        row = idx // 4
        col = idx % 4
        
        # Calculate crop box
        left = col * cell_width
        top = row * cell_height
        right = left + cell_width
        bottom = top + cell_height
        
        # Crop the icon
        icon = img.crop((left, top, right, bottom))
        
        # Create assets folder if needed
        assets_dir = os.path.join(script_dir, "..", "plugins", "tts", plugin_name, "assets")
        os.makedirs(assets_dir, exist_ok=True)
        
        # Save the icon
        icon_path = os.path.join(assets_dir, "icon.png")
        icon.save(icon_path, "PNG")
        print(f"Saved: {plugin_name}/assets/icon.png")

if __name__ == "__main__":
    main()
