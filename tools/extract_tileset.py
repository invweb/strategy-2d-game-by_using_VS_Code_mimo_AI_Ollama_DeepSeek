#!/usr/bin/env python3
"""Extract representative tiles from terrain.png and create a new tileset.

This script analyzes terrain.png (640x1024, 64px tiles) and extracts
representative tiles for each terrain type, then creates a new tileset
compatible with the game (48x48 tiles).
"""

import os
from PIL import Image, ImageDraw, ImageFilter

TILE_SIZE = 48  # Game tile size
OUTPUT_SIZE = 48  # Output tile size
TERRAINS = ["plains", "forest", "mountain", "hills", "water", "desert", "swamp", "snow"]

def analyze_tile(tile):
    """Analyze a tile and return its average color and classification."""
    pixels = list(tile.getdata())
    avg_r = sum(p[0] for p in pixels) / len(pixels)
    avg_g = sum(p[1] for p in pixels) / len(pixels)
    avg_b = sum(p[2] for p in pixels) / len(pixels)
    
    # Classify terrain based on color
    if avg_g > 180:
        return "plains", (avg_r, avg_g, avg_b)
    elif avg_g > 140:
        return "forest", (avg_r, avg_g, avg_b)
    elif avg_g > 100 and avg_b < 80:
        return "desert", (avg_r, avg_g, avg_b)
    elif avg_g < 60 and avg_b > 100:
        return "water", (avg_r, avg_g, avg_b)
    elif avg_r < 100 and avg_g < 100:
        return "mountain", (avg_r, avg_g, avg_b)
    elif avg_g < 80 and avg_r < 100:
        return "swamp", (avg_r, avg_g, avg_b)
    elif avg_r > 220 and avg_g > 220:
        return "snow", (avg_r, avg_g, avg_b)
    else:
        return "hills", (avg_r, avg_g, avg_b)

def resize_tile(tile, target_size):
    """Resize a tile to target size using high-quality resampling."""
    return tile.resize((target_size, target_size), Image.Resampling.LANCZOS)

def main():
    input_path = "/tmp/terrain.png"
    output_dir = "/Users/vasiliikarpenko/Projects/Strategy/desktop/assets"
    analysis_dir = "/tmp/tileset_analysis"
    
    # Load the source image
    img = Image.open(input_path)
    print(f"Loaded {input_path}: {img.size}")
    
    # Analyze all tiles and find representatives
    tile_size = 64
    cols = img.size[0] // tile_size
    rows = img.size[1] // tile_size
    
    os.makedirs(analysis_dir, exist_ok=True)
    
    # Store representative tile for each terrain type
    terrain_tiles = {}
    
    print(f"\nAnalyzing {cols}x{rows} = {cols*rows} tiles...")
    print("Finding representative tiles for each terrain type:")
    
    for row in range(rows):
        for col in range(cols):
            tile = img.crop((col*tile_size, row*tile_size, (col+1)*tile_size, (row+1)*tile_size))
            terrain_type, avg_color = analyze_tile(tile)
            
            # Keep track of tiles for each terrain type
            if terrain_type not in terrain_tiles:
                terrain_tiles[terrain_type] = []
            terrain_tiles[terrain_type].append((tile, avg_color, row, col))
    
    # Print found terrains
    for terrain, tiles in sorted(terrain_tiles.items()):
        print(f"  {terrain}: {len(tiles)} tiles found")
        
        # Save first tile as analysis sample
        sample_tile, _, row, col = tiles[0]
        sample_resized = sample_tile.resize((OUTPUT_SIZE, OUTPUT_SIZE), Image.Resampling.LANCZOS)
        sample_resized.save(os.path.join(analysis_dir, f"{terrain}_sample.png"))
    
    # Create output tileset
    os.makedirs(output_dir, exist_ok=True)
    output_path = os.path.join(output_dir, "tileset.png")
    
    # Create a new image for the tileset
    # 8 terrain types in rows, each with OUTPUT_SIZE x OUTPUT_SIZE
    tileset = Image.new("RGBA", (OUTPUT_SIZE, len(TERRAINS) * OUTPUT_SIZE), (0, 0, 0, 0))
    
    for ri, terrain in enumerate(TERRAINS):
        if terrain in terrain_tiles:
            # Use the first tile we found for this terrain type
            sample_tile, _, _, _ = terrain_tiles[terrain][0]
            # Resize to game tile size
            resized = sample_tile.resize((OUTPUT_SIZE, OUTPUT_SIZE), Image.Resampling.LANCZOS)
            tileset.paste(resized, (0, ri * OUTPUT_SIZE))
            print(f"  Added {terrain} (row {ri})")
        else:
            print(f"  WARNING: No tiles found for {terrain}, using placeholder")
            # Create placeholder
            placeholder = Image.new("RGBA", (OUTPUT_SIZE, OUTPUT_SIZE), (128, 128, 128, 255))
            d = ImageDraw.Draw(placeholder)
            d.text((5, 5), terrain[:4], fill=(255, 255, 255, 255))
            tileset.paste(placeholder, (0, ri * OUTPUT_SIZE))
    
    # Save the tileset
    tileset.save(output_path)
    print(f"\nSaved tileset to {output_path} ({tileset.size[0]}x{tileset.size[1]})")
    
    for ri, t in enumerate(TERRAINS):
        print(f"  row {ri}: {t}")
    
    print(f"\nAnalysis samples saved to {analysis_dir}/")

if __name__ == "__main__":
    main()
